package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val Context.prayerProgressStore by preferencesDataStore(name = "prayer_progress")

/**
 * How far through each prayer hour the user has read *today*.
 *
 * Completion is keyed by **section id**, not position: ids are permanent
 * contracts (guarded by tools/validate_content.py), so reordering or hiding
 * sections never loses a mark, while a position would silently shift.
 *
 * Progress is per-day and resets at midnight — an hour is prayed anew each day,
 * so yesterday's ticks would be misleading. Only [today]'s entry is retained.
 */
object PrayerProgressRepository {

    private val KEY = stringPreferencesKey("progress_v1")

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class DayProgress(
        /** ISO date this progress belongs to; anything else is stale. */
        val date: String = "",
        /** hourId -> the section ids marked read. */
        val done: Map<String, Set<String>> = emptyMap(),
        /** The hour last opened, for "resume". */
        val lastHourId: String? = null,
    )

    private fun decode(raw: String?, today: String): DayProgress {
        val p = raw?.let { runCatching { json.decodeFromString<DayProgress>(it) }.getOrNull() }
            ?: return DayProgress(date = today)
        // A new day wipes the slate rather than showing yesterday's ticks.
        return if (p.date == today) p else DayProgress(date = today)
    }

    fun progress(context: Context, today: String = LocalDate.now().toString()): Flow<DayProgress> =
        context.prayerProgressStore.data.map { decode(it[KEY], today) }

    /** The section ids read today in [hourId]. */
    fun doneIn(context: Context, hourId: String): Flow<Set<String>> =
        progress(context).map { it.done[hourId] ?: emptySet() }

    suspend fun toggleSection(
        context: Context,
        hourId: String,
        sectionId: String,
        today: String = LocalDate.now().toString(),
    ) {
        context.prayerProgressStore.edit { prefs ->
            val p = decode(prefs[KEY], today)
            val current = p.done[hourId] ?: emptySet()
            val next = if (sectionId in current) current - sectionId else current + sectionId
            prefs[KEY] = json.encodeToString(
                DayProgress.serializer(),
                p.copy(done = p.done + (hourId to next)),
            )
        }
    }

    /** Note that [hourId] was opened, so Home can offer to resume it. */
    suspend fun recordOpened(
        context: Context,
        hourId: String,
        today: String = LocalDate.now().toString(),
    ) {
        context.prayerProgressStore.edit { prefs ->
            val p = decode(prefs[KEY], today)
            prefs[KEY] = json.encodeToString(DayProgress.serializer(), p.copy(lastHourId = hourId))
        }
    }

    /** Clear one hour's marks — "start this prayer over". */
    suspend fun clearHour(
        context: Context,
        hourId: String,
        today: String = LocalDate.now().toString(),
    ) {
        context.prayerProgressStore.edit { prefs ->
            val p = decode(prefs[KEY], today)
            prefs[KEY] = json.encodeToString(DayProgress.serializer(), p.copy(done = p.done - hourId))
        }
    }

    suspend fun current(context: Context): DayProgress = progress(context).first()
}
