package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.model.Habit
import com.agpeya.app.model.HabitSchedule
import com.agpeya.app.model.HabitsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.UUID

private val Context.habitsDataStore by preferencesDataStore(name = "habits")

/**
 * Habit tracking: daily check-off records plus a customizable habit list
 * (defaults + user-created). Prayer-day/heatmap math is exposed as pure
 * functions (no Context) so it can be unit-tested. Mirrors HoursRepository's
 * shape. The period-level metric (this month / this fast) lives in
 * [PrayerJourney], which derives from these same records.
 */
object HabitsRepository {

    /**
     * Built-in habit ids; their display names come from Strings (localized).
     * Prayer is NOT a single habit — each prayer hour is tracked individually
     * with a "hour_<hourId>" record id (see [hourHabitId]).
     */
    /** The day's portion of the Psalter, recorded when the reader finishes it. */
    const val DAWIT = "dawit"

    val BUILT_IN_IDS = listOf("sinksar", "church", "prostrate", "bible", DAWIT)

    fun hourHabitId(hourId: String): String = "hour_$hourId"

    private val KEY = stringPreferencesKey("habits_json")
    private val json = Json { ignoreUnknownKeys = true }

    private fun decode(raw: String?): HabitsState =
        raw?.let { runCatching { json.decodeFromString<HabitsState>(it) }.getOrNull() } ?: HabitsState()

    fun state(context: Context): Flow<HabitsState> =
        context.habitsDataStore.data.map { decode(it[KEY]) }

    suspend fun current(context: Context): HabitsState = state(context).first()

    private suspend fun update(context: Context, transform: (HabitsState) -> HabitsState) {
        context.habitsDataStore.edit { prefs ->
            prefs[KEY] = json.encodeToString(HabitsState.serializer(), transform(decode(prefs[KEY])))
        }
    }

    /**
     * Merge a restored [HabitsState] into the device's. Records union per day,
     * so a day marked done in either place stays done and restoring an old
     * backup can't erase newer progress. Custom habits are added if missing.
     */
    suspend fun merge(context: Context, restored: HabitsState) = update(context) { current ->
        val records = current.records.toMutableMap()
        restored.records.forEach { (day, ids) ->
            records[day] = (records[day] ?: emptySet()) + ids
        }
        val knownIds = current.custom.mapTo(mutableSetOf()) { it.id }
        current.copy(
            custom = current.custom + restored.custom.filter { it.id !in knownIds },
            order = current.order.ifEmpty { restored.order },
            hidden = current.hidden + restored.hidden,
            names = restored.names + current.names,   // device naming wins
            schedules = restored.schedules + current.schedules,  // device cadence wins
            records = records,
        )
    }

    /** Toggle a habit's completion for a given day key ("yyyy-MM-dd"). */
    suspend fun toggle(context: Context, date: String, habitId: String) = update(context) {
        val done = it.records[date] ?: emptySet()
        val next = if (habitId in done) done - habitId else done + habitId
        val records = if (next.isEmpty()) it.records - date else it.records + (date to next)
        it.copy(records = records)
    }

    /** Idempotently mark a habit done — used by the "Done?" notification action. */
    suspend fun markDone(context: Context, date: String, habitId: String) = update(context) {
        val done = it.records[date] ?: emptySet()
        it.copy(records = it.records + (date to done + habitId))
    }

    suspend fun addCustomHabit(context: Context, name: String): Habit {
        val habit = Habit(id = "custom_${UUID.randomUUID()}", name = name.ifBlank { "New habit" })
        update(context) { it.copy(custom = it.custom + habit) }
        return habit
    }

    suspend fun renameHabit(context: Context, id: String, name: String) =
        update(context) { it.copy(names = it.names + (id to name)) }

    suspend fun deleteCustomHabit(context: Context, id: String) = update(context) {
        it.copy(
            custom = it.custom.filterNot { c -> c.id == id },
            order = it.order - id,
            hidden = it.hidden - id,
            names = it.names - id,
        )
    }

    suspend fun setHidden(context: Context, id: String, hidden: Boolean) = update(context) {
        it.copy(hidden = if (hidden) it.hidden + id else it.hidden - id)
    }

    suspend fun setOrder(context: Context, ids: List<String>) =
        update(context) { it.copy(order = ids) }

    /** Set a habit's cadence, or pass null to return it to every day. */
    suspend fun setSchedule(context: Context, id: String, schedule: HabitSchedule?) =
        update(context) {
            it.copy(schedules = if (schedule == null) it.schedules - id else it.schedules + (id to schedule))
        }

    /** Built-in + custom habit ids in the user's order, optionally including hidden. */
    fun orderedHabitIds(state: HabitsState, includeHidden: Boolean): List<String> {
        val all = BUILT_IN_IDS + state.custom.map { it.id }
        val ordered = if (state.order.isEmpty()) all
        else state.order.filter { it in all } + all.filterNot { it in state.order }
        return if (includeHidden) ordered else ordered.filterNot { it in state.hidden }
    }

    // ---- Cadence (pure; unit-tested) ----

    /**
     * Whether a habit is asked for on [date]. A habit with no schedule is a
     * daily one — that is what every habit was before cadences existed, so an
     * untouched state behaves exactly as it did.
     */
    fun isDue(state: HabitsState, habitId: String, date: LocalDate): Boolean =
        state.schedules[habitId]?.isDueOn(date) ?: true

    /** The habits asked for on [date], in the user's own order. */
    fun dueHabitIds(state: HabitsState, date: LocalDate, includeHidden: Boolean = false): List<String> =
        orderedHabitIds(state, includeHidden).filter { isDue(state, it, date) }

    /**
     * How much of what was asked for on [date] was kept — the honest pair for
     * a checklist that is not the same length every day. A day that asks for
     * nothing reads as 0 of 0, never as a failure.
     */
    fun keptOfDue(state: HabitsState, date: LocalDate): Pair<Int, Int> {
        val due = dueHabitIds(state, date)
        val done = state.records[date.toString()] ?: emptySet()
        return due.count { it in done } to due.size
    }

    // ---- Pure prayer-day / heatmap math (no Context; unit-tested) ----

    private fun done(records: Map<String, Set<String>>, date: LocalDate): Set<String> =
        records[date.toString()] ?: emptySet()

    fun dayCount(records: Map<String, Set<String>>, date: LocalDate): Int = done(records, date).size

    /**
     * Heatmap intensity level 0..4, from how many things were kept that day.
     *
     * It used to be a fraction of everything trackable, which made the top of
     * the scale a function of how many habits the user had added: with the
     * seven hours and five habits showing, level 4 wanted nine kept in one day
     * and a full week of morning prayer never darkened past level 1. Adding a
     * habit is not meant to dim the past. So the scale is absolute — the
     * fullest day this counts is [FULL_DAY], about the seven hours or any
     * mixture that size, and one thing kept is always level 1.
     */
    fun level(count: Int): Int = when {
        count <= 0 -> 0
        count >= FULL_DAY -> 4
        count >= 4 -> 3
        count >= 2 -> 2
        else -> 1
    }

    /** What a full day of keeping looks like: the seven hours, or as much. */
    const val FULL_DAY = 7

    /**
     * Distinct days in [start]..[endInclusive] a habit was checked off. Days,
     * not events: a day is a day however many times it was marked. Gaps in
     * between change nothing — the count never resets.
     */
    fun habitDaysBetween(
        records: Map<String, Set<String>>,
        habitId: String,
        start: LocalDate,
        endInclusive: LocalDate,
    ): Int = daysMatching(records, start, endInclusive) { habitId in it }

    private fun daysMatching(
        records: Map<String, Set<String>>,
        start: LocalDate,
        endInclusive: LocalDate,
        predicate: (Set<String>) -> Boolean,
    ): Int = records.count { (key, ids) ->
        if (!predicate(ids)) return@count false
        val day = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@count false
        !day.isBefore(start) && !day.isAfter(endInclusive)
    }
}
