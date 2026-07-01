package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.model.Habit
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
 * (defaults + user-created). Streak/heatmap math is exposed as pure functions
 * (no Context) so it can be unit-tested. Mirrors HoursRepository's shape.
 */
object HabitsRepository {

    /**
     * Built-in habit ids; their display names come from Strings (localized).
     * Prayer is NOT a single habit — each prayer hour is tracked individually
     * with a "hour_<hourId>" record id (see [hourHabitId]).
     */
    val BUILT_IN_IDS = listOf("church", "prostrate", "bible")

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

    /** Built-in + custom habit ids in the user's order, optionally including hidden. */
    fun orderedHabitIds(state: HabitsState, includeHidden: Boolean): List<String> {
        val all = BUILT_IN_IDS + state.custom.map { it.id }
        val ordered = if (state.order.isEmpty()) all
        else state.order.filter { it in all } + all.filterNot { it in state.order }
        return if (includeHidden) ordered else ordered.filterNot { it in state.hidden }
    }

    // ---- Pure streak / heatmap math (no Context; unit-tested) ----

    private fun done(records: Map<String, Set<String>>, date: LocalDate): Set<String> =
        records[date.toString()] ?: emptySet()

    fun dayCount(records: Map<String, Set<String>>, date: LocalDate): Int = done(records, date).size

    /**
     * Consecutive completed days ending today — or yesterday if today isn't yet
     * marked, so the streak doesn't visibly reset mid-day. Breaks on a gap.
     */
    fun currentStreak(records: Map<String, Set<String>>, habitId: String, today: LocalDate): Int {
        var cursor = if (habitId in done(records, today)) today else today.minusDays(1)
        var count = 0
        while (habitId in done(records, cursor)) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    /** Longest run of consecutive calendar days the habit was completed, ever. */
    fun longestStreak(records: Map<String, Set<String>>, habitId: String): Int {
        val dates = records.filterValues { habitId in it }.keys
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .sorted()
        if (dates.isEmpty()) return 0
        var best = 1
        var run = 1
        for (i in 1 until dates.size) {
            if (dates[i] == dates[i - 1].plusDays(1)) run++ else run = 1
            if (run > best) best = run
        }
        return best
    }

    /** Consecutive days with at least one habit completed (same day-tolerant rule). */
    fun overallCurrentStreak(records: Map<String, Set<String>>, today: LocalDate): Int {
        var cursor = if (done(records, today).isNotEmpty()) today else today.minusDays(1)
        var count = 0
        while (done(records, cursor).isNotEmpty()) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }
}
