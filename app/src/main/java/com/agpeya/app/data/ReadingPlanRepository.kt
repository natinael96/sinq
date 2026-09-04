package com.agpeya.app.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.model.PlanDay
import com.agpeya.app.model.ReadingPlan
import com.agpeya.app.model.ReadingPlanContent
import com.agpeya.app.model.ReadingPlanState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val Context.readingPlanDataStore by preferencesDataStore(name = "reading_plan")

/**
 * ንባብ — the reading plan: its bundled definition, and what has been read of it.
 *
 * The plan is deliberately not a streak. Progress is the count of distinct days
 * read, exactly as [PrayerJourney] counts prayer — a missed day changes nothing
 * but that day, and nothing here ever resets to zero.
 */
object ReadingPlanRepository {

    private const val TAG = "ReadingPlanRepository"
    private const val PATH = "content/reading/plans.json"

    private val KEY_STATE = stringPreferencesKey("reading_plan_json")

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Volatile
    private var cache: ReadingPlanContent? = null

    // ── the bundled plans ────────────────────────────────────────────────────

    suspend fun content(context: Context): ReadingPlanContent =
        cache ?: withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.applicationContext.assets
                    .open(PATH).readBytes().decodeToString()
                json.decodeFromString<ReadingPlanContent>(raw)
            }.onFailure { Log.e(TAG, "Failed to load reading plans", it) }
                .getOrNull()?.also { cache = it } ?: ReadingPlanContent()
        }

    suspend fun plan(context: Context, id: String): ReadingPlan? =
        content(context).plans.firstOrNull { it.id == id }

    // ── what has been read ───────────────────────────────────────────────────

    fun state(context: Context): Flow<ReadingPlanState> =
        context.readingPlanDataStore.data.map { decode(it[KEY_STATE]) }

    suspend fun current(context: Context): ReadingPlanState = state(context).first()

    private fun decode(raw: String?): ReadingPlanState =
        raw?.let { runCatching { json.decodeFromString<ReadingPlanState>(it) }.getOrNull() }
            ?: ReadingPlanState()

    private suspend fun update(context: Context, transform: (ReadingPlanState) -> ReadingPlanState) {
        context.readingPlanDataStore.edit { prefs ->
            prefs[KEY_STATE] = json.encodeToString(transform(decode(prefs[KEY_STATE])))
        }
    }

    /** Begin a plan today. Any days already read of it are kept. */
    suspend fun start(context: Context, planId: String, today: LocalDate = LocalDate.now()) {
        update(context) { it.copy(activePlanId = planId, startedOn = today.toString()) }
    }

    suspend fun stop(context: Context) {
        update(context) { it.copy(activePlanId = "") }
    }

    /** Idempotent: reading the same day twice is not worth recording twice. */
    suspend fun markDay(context: Context, planId: String, day: Int, today: LocalDate = LocalDate.now()) {
        update(context) { st ->
            val days = st.readDays(planId) + day
            st.copy(
                completedDays = st.completedDays + (planId to days),
                lastReadOn = today.toString(),
            )
        }
    }

    suspend fun unmarkDay(context: Context, planId: String, day: Int) {
        update(context) { st ->
            val days = st.readDays(planId) - day
            st.copy(completedDays = st.completedDays + (planId to days))
        }
    }

    /**
     * Shift day 1 so that [day] becomes today — the "ዛሬ ላይ ቀጥል" and
     * "እስከዚህ አሟላ" verbs, which differ only in which day is chosen.
     */
    suspend fun rebaseTo(context: Context, day: Int, today: LocalDate = LocalDate.now()) {
        update(context) { it.copy(startedOn = today.minusDays((day - 1).toLong()).toString()) }
    }

    /** Merge for backup restore: union, so an old file never erases newer reading. */
    suspend fun merge(context: Context, restored: ReadingPlanState) {
        update(context) { cur ->
            val days = (cur.completedDays.keys + restored.completedDays.keys).associateWith {
                cur.readDays(it) + restored.readDays(it)
            }
            cur.copy(
                activePlanId = cur.activePlanId.ifBlank { restored.activePlanId },
                startedOn = cur.startedOn.ifBlank { restored.startedOn },
                completedDays = days,
                lastReadOn = maxOf(cur.lastReadOn, restored.lastReadOn),
            )
        }
    }

    // ── pure day math (unit-tested; no Context) ──────────────────────────────

    /**
     * Which day of the plan today is — 1 on the day it was started.
     *
     * Clamped to the plan's length so a plan left running for two years shows
     * its last day rather than day 800.
     */
    fun dayOn(startedOn: String, today: LocalDate, planDays: Int): Int {
        val start = runCatching { LocalDate.parse(startedOn) }.getOrNull() ?: return 1
        val n = ChronoUnit.DAYS.between(start, today).toInt() + 1
        return n.coerceIn(1, maxOf(1, planDays))
    }

    /** Days actually read — the only progress number the app shows. */
    fun daysRead(state: ReadingPlanState, planId: String): Int = state.readDays(planId).size

    /**
     * The earliest unread day at or before [currentDay] — where "catch up"
     * would start. Null when nothing is owed, which is the common case.
     */
    fun oldestUnread(state: ReadingPlanState, planId: String, currentDay: Int): Int? {
        val read = state.readDays(planId)
        return (1..currentDay).firstOrNull { it !in read }
    }

    /**
     * Redistribute what is left across the days that remain — the default
     * answer to falling behind, and the only one that never shows a shortfall.
     *
     * Returns the days of the plan re-packed from [fromDay] onward into
     * [remainingDays], preserving order and dropping nothing.
     */
    fun redistribute(days: List<PlanDay>, fromDay: Int, remainingDays: Int): List<PlanDay> {
        if (remainingDays <= 0) return emptyList()
        val tail = days.filter { it.d >= fromDay }.flatMap { it.r }
        if (tail.isEmpty()) return emptyList()
        val per = kotlin.math.ceil(tail.size.toDouble() / remainingDays).toInt().coerceAtLeast(1)
        return tail.chunked(per).mapIndexed { i, r -> PlanDay(d = fromDay + i, r = r) }
    }
}
