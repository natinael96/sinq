package com.agpeya.app.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.model.PlanDay
import com.agpeya.app.model.ReadingPlan
import com.agpeya.app.model.Redistribution
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

    /** ጉዞ's daily-reading habit, kept by reading a day of the plan. */
    private const val BIBLE_HABIT = "bible"

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

    /**
     * Mark a day's chapters read. Idempotent — reading the same day twice is
     * not worth recording twice, and a chapter this day shares with another is
     * simply read.
     *
     * A day of the plan read is also the day's Bible reading kept, so it
     * writes ጉዞ's `bible` habit for the day it was marked on — the ledger and
     * the plan were two records of one act, and the reader had to keep both.
     */
    suspend fun markDay(context: Context, day: PlanDay, today: LocalDate = LocalDate.now()) {
        update(context) { st ->
            st.copy(
                readChapters = st.readChapters + chaptersOf(day),
                lastReadOn = today.toString(),
            )
        }
        HabitsRepository.markDone(context, today.toString(), BIBLE_HABIT)
    }

    /**
     * Unmark a day. Chapters this day shares with another already-read day are
     * kept: they have still been read, and taking them back would leave that
     * other day looking unread.
     */
    suspend fun unmarkDay(context: Context, plan: ReadingPlan, day: PlanDay) {
        update(context) { st ->
            val elsewhere = effectiveDays(plan, st)
                .filter { it.d != day.d && isRead(st, it) }
                .flatMapTo(mutableSetOf()) { chaptersOf(it) }
            st.copy(readChapters = st.readChapters - (chaptersOf(day).toSet() - elsewhere))
        }
    }

    /**
     * Fold the old day-number ledger into chapters, and drop a repacking that
     * a new bundle has renumbered out from under. Runs at launch, before any
     * screen reads the state; a no-op on every launch after the first.
     */
    suspend fun reconcile(context: Context, content: ReadingPlanContent) = update(context) { st ->
        // A state that has never been stamped is not stale — it was written
        // against the bundle it is being read on. Only a version that changed
        // under a reader renumbers the days.
        val stale = st.contentVersion != 0 && st.contentVersion != content.contentVersion
        if (st.completedDays.isEmpty() && !stale && st.contentVersion == content.contentVersion) {
            return@update st
        }
        val chapters = st.readChapters.toMutableSet()
        st.completedDays.forEach { (planId, days) ->
            val plan = content.plans.firstOrNull { it.id == planId } ?: return@forEach
            effectiveDays(plan, st).forEach { d -> if (d.d in days) chapters += chaptersOf(d) }
        }
        st.copy(
            readChapters = chapters,
            completedDays = emptyMap(),
            redistributed = if (stale) emptyMap() else st.redistributed,
            contentVersion = content.contentVersion,
        )
    }

    /**
     * Shift day 1 so that [day] becomes today — "finish later, same daily
     * reading". Nothing is skipped; the plan's last day simply moves out.
     */
    suspend fun rebaseTo(context: Context, day: Int, today: LocalDate = LocalDate.now()) {
        update(context) { it.copy(startedOn = today.minusDays((day - 1).toLong()).toString()) }
    }

    /**
     * The other answer to falling behind: keep the finish date and let the days
     * grow. Everything from [fromDay] is repacked across the days from [onDay]
     * to the plan's last — see [effectiveDays], which recomputes the packing.
     */
    suspend fun redistributeFrom(context: Context, planId: String, fromDay: Int, onDay: Int) {
        update(context) {
            it.copy(redistributed = it.redistributed + (planId to Redistribution(fromDay, onDay)))
        }
    }

    /**
     * The plan's days as they stand for this reader: the bundled days until the
     * repacking took effect, then the repacked tail. Days before it keep the
     * readings they were given — that is what those days were, and they are
     * still marked unread — while everything owed moves into the days ahead.
     */
    fun effectiveDays(plan: ReadingPlan, state: ReadingPlanState): List<PlanDay> {
        val r = state.redistributed[plan.id] ?: return plan.readings
        if (!r.inForce) return plan.readings
        return plan.readings.filter { it.d < r.on } +
            redistribute(plan.readings, r.from, plan.days - r.on + 1, startDay = r.on)
    }

    /**
     * Merge for backup restore: union, so an old file never erases newer
     * reading. A backup written by an older build carries day numbers instead
     * of chapters; they land in [ReadingPlanState.completedDays] and the next
     * [reconcile] folds them in.
     */
    suspend fun merge(context: Context, restored: ReadingPlanState) {
        update(context) { cur ->
            val days = (cur.completedDays.keys + restored.completedDays.keys).associateWith {
                cur.readDays(it) + restored.readDays(it)
            }
            cur.copy(
                activePlanId = cur.activePlanId.ifBlank { restored.activePlanId },
                startedOn = cur.startedOn.ifBlank { restored.startedOn },
                completedDays = days,
                readChapters = cur.readChapters + restored.readChapters,
                lastReadOn = maxOf(cur.lastReadOn, restored.lastReadOn),
                // The device's own repacking wins: it matches the dates it is living under.
                redistributed = restored.redistributed + cur.redistributed,
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

    /** How a chapter is recorded: the book's slug and the chapter number. */
    fun chapterKey(slug: String, chapter: Int): String = "$slug:$chapter"

    /** Every chapter a day of the plan asks for. */
    fun chaptersOf(day: PlanDay): List<String> =
        day.r.flatMap { r -> r.chapters.map { chapterKey(r.b, it) } }

    /** A day is read when every chapter it asks for has been. */
    fun isRead(state: ReadingPlanState, day: PlanDay): Boolean =
        day.r.isNotEmpty() && chaptersOf(day).all { it in state.readChapters }

    /** Which of [days] are read, by day number. */
    fun readDayNumbers(state: ReadingPlanState, days: List<PlanDay>): Set<Int> =
        days.filterTo(mutableListOf()) { isRead(state, it) }.mapTo(mutableSetOf()) { it.d }

    /** Days actually read — the only progress number the app shows. */
    fun daysRead(state: ReadingPlanState, days: List<PlanDay>): Int = days.count { isRead(state, it) }

    /**
     * The earliest unread day at or before [currentDay] — where "catch up"
     * would start. Null when nothing is owed, which is the common case.
     *
     * A repacking resets the floor: once what was owed has been folded into the
     * days ahead, the days it came from are no longer behind.
     */
    fun oldestUnread(
        state: ReadingPlanState,
        planId: String,
        days: List<PlanDay>,
        currentDay: Int,
    ): Int? {
        val floor = state.redistributed[planId]?.takeIf { it.inForce }?.on ?: 1
        val byNumber = days.associateBy { it.d }
        return (floor..currentDay).firstOrNull { n ->
            val day = byNumber[n] ?: return@firstOrNull false
            !isRead(state, day)
        }
    }

    /**
     * Redistribute what is left across the days that remain — the default
     * answer to falling behind, and the only one that never shows a shortfall.
     *
     * Returns the days of the plan re-packed from [fromDay] onward into
     * [remainingDays], preserving order and dropping nothing.
     */
    fun redistribute(
        days: List<PlanDay>,
        fromDay: Int,
        remainingDays: Int,
        /** Day number the repacked run starts on; the same day it came from unless
         *  the reader has fallen behind and it is being folded into today. */
        startDay: Int = fromDay,
    ): List<PlanDay> {
        if (remainingDays <= 0) return emptyList()
        val tail = days.filter { it.d >= fromDay }.flatMap { it.r }
        if (tail.isEmpty()) return emptyList()
        val per = kotlin.math.ceil(tail.size.toDouble() / remainingDays).toInt().coerceAtLeast(1)
        return tail.chunked(per).mapIndexed { i, r -> PlanDay(d = startDay + i, r = r) }
    }
}
