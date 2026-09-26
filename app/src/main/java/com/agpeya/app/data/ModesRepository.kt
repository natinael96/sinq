package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.model.ModesState
import com.agpeya.app.model.PrayerMode
import com.agpeya.app.model.ReminderEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.modesDataStore by preferencesDataStore(name = "prayer_modes")

/**
 * Persists prayer modes as JSON in DataStore. The alarm schedule is derived
 * state: after any mutation, callers must invoke ReminderScheduler.rescheduleAll.
 */
object ModesRepository {

    const val BUILT_IN_ID = "agpeya_classic"

    private val KEY_STATE = stringPreferencesKey("modes_state_json")
    private val KEY_SCHEDULED = stringSetPreferencesKey("scheduled_entry_ids")

    private val json = Json { ignoreUnknownKeys = true }

    /** Traditional times per hour — the built-in mode's defaults (PLAN.md §2.1). */
    /**
     * The hours of the built-in mode, and whether each one rings out of the box.
     *
     * Every one of these used to ship off, so an app installed to call someone
     * to prayer called them to nothing and never said so. The six day hours are
     * now armed: they are the round the Church keeps, and being called to them
     * is the reason to install this at all.
     *
     * The two night offices are not. ንዋም at midnight and ሌሊት ፱ ሰዓት at ten are
     * kept by those who have taken them up, and an app that wakes a stranger at
     * midnight on the day they install it has misread what it was asked for.
     * Both are one switch away in የጸሎት ማንቂያ ሁነታዎች.
     */
    private val DEFAULT_TIMES = listOf(
        Triple("morning", 6, true),
        Triple("terce", 9, true),
        Triple("sext", 12, true),
        Triple("none", 15, true),
        Triple("vespers", 18, true),
        Triple("compline", 21, true),
        Triple("midnight", 0, false),
        Triple("veil", 22, false),
    )

    const val BUILT_IN_NAME = "ሰዓታት"

    fun builtInMode(): PrayerMode = PrayerMode(
        id = BUILT_IN_ID,
        name = BUILT_IN_NAME,
        isBuiltIn = true,
        entries = DEFAULT_TIMES.map { (hourId, h, on) ->
            ReminderEntry(id = "builtin_$hourId", hourId = hourId, hour = h, minute = 0, enabled = on)
        },
    )

    private fun defaultState() = ModesState(
        activeModeId = BUILT_IN_ID,
        modes = listOf(builtInMode()),
    )

    private fun decode(raw: String?): ModesState = raw
        ?.let { runCatching { json.decodeFromString<ModesState>(it) }.getOrNull() }
        ?: defaultState()

    /** Atomic read-modify-write; concurrent toggle/time edits cannot overwrite each other. */
    private suspend fun update(context: Context, transform: (ModesState) -> ModesState) {
        context.modesDataStore.edit { prefs ->
            prefs[KEY_STATE] = json.encodeToString(
                ModesState.serializer(),
                transform(decode(prefs[KEY_STATE])),
            )
        }
    }

    fun state(context: Context): Flow<ModesState> =
        context.modesDataStore.data.map { prefs ->
            val decoded = decode(prefs[KEY_STATE])
            // Force the built-in mode's display name to the current value, so any
            // older persisted name (e.g. "አግፔያ") is replaced.
            decoded.copy(
                modes = decoded.modes.map {
                    if (it.isBuiltIn) it.copy(name = BUILT_IN_NAME) else it
                },
            )
        }

    suspend fun current(context: Context): ModesState = state(context).first()

    /** Restore modes without replacing locally-created modes. A fresh default state adopts the backup fully. */
    suspend fun merge(context: Context, restored: ModesState) {
        // A hand-edited backup can carry impossible times; persisting them
        // would crash the scheduler's LocalTime.of later. Clamp on the way in.
        val safeModes = restored.modes.map { mode ->
            mode.copy(entries = mode.entries.map {
                it.copy(hour = it.hour.coerceIn(0, 23), minute = it.minute.coerceIn(0, 59))
            })
        }
        update(context) { current ->
            if (current == defaultState()) {
                val valid = safeModes.ifEmpty { listOf(builtInMode()) }
                val active = restored.activeModeId
                    .takeIf { id -> valid.any { it.id == id } } ?: BUILT_IN_ID
                ModesState(activeModeId = active, modes = valid)
            } else {
                val have = current.modes.mapTo(mutableSetOf()) { it.id }
                current.copy(
                    modes = current.modes + safeModes.filterNot { it.id in have || it.isBuiltIn },
                )
            }
        }
    }

    suspend fun setActiveMode(context: Context, modeId: String) {
        update(context) { state ->
            if (state.modes.any { it.id == modeId }) state.copy(activeModeId = modeId) else state
        }
    }

    suspend fun addMode(context: Context, name: String, copyFrom: PrayerMode? = null): PrayerMode {
        val mode = PrayerMode(
            id = UUID.randomUUID().toString(),
            name = name,
            entries = copyFrom?.entries?.map {
                it.copy(id = UUID.randomUUID().toString())
            } ?: emptyList(),
        )
        update(context) { it.copy(modes = it.modes + mode) }
        return mode
    }

    suspend fun renameMode(context: Context, modeId: String, name: String) {
        updateMode(context, modeId) { it.copy(name = name) }
    }

    /** Deleting the active mode falls back to the built-in Agpeya mode. */
    suspend fun deleteMode(context: Context, modeId: String) {
        update(context) { state ->
            val mode = state.modes.find { it.id == modeId }
            if (mode == null || mode.isBuiltIn) state
            else {
                val remaining = state.modes.filter { it.id != modeId }
                val active = if (state.activeModeId == modeId) BUILT_IN_ID else state.activeModeId
                ModesState(activeModeId = active, modes = remaining)
            }
        }
    }

    suspend fun resetBuiltIn(context: Context) {
        updateMode(context, BUILT_IN_ID) { builtInMode() }
    }

    suspend fun upsertEntry(context: Context, modeId: String, entry: ReminderEntry) {
        updateMode(context, modeId) { mode ->
            val existing = mode.entries.indexOfFirst { it.id == entry.id }
            val entries = if (existing >= 0) {
                mode.entries.toMutableList().also { it[existing] = entry }
            } else {
                mode.entries + entry
            }
            mode.copy(entries = entries)
        }
    }

    suspend fun deleteEntry(context: Context, modeId: String, entryId: String) {
        updateMode(context, modeId) { mode ->
            if (mode.isBuiltIn) mode
            else mode.copy(entries = mode.entries.filter { it.id != entryId })
        }
    }

    suspend fun findEntry(context: Context, entryId: String): Pair<PrayerMode, ReminderEntry>? {
        val s = current(context)
        for (mode in s.modes) {
            mode.entries.find { it.id == entryId }?.let { return mode to it }
        }
        return null
    }

    private suspend fun updateMode(context: Context, modeId: String, transform: (PrayerMode) -> PrayerMode) {
        update(context) { state ->
            state.copy(modes = state.modes.map { if (it.id == modeId) transform(it) else it })
        }
    }

    // Bookkeeping of which entry ids currently have alarms, so reschedule can cancel stale ones.
    suspend fun scheduledIds(context: Context): Set<String> =
        context.modesDataStore.data.first()[KEY_SCHEDULED] ?: emptySet()

    suspend fun setScheduledIds(context: Context, ids: Set<String>) {
        context.modesDataStore.edit { it[KEY_SCHEDULED] = ids }
    }

    suspend fun removeScheduledId(context: Context, id: String) {
        context.modesDataStore.edit { prefs ->
            prefs[KEY_SCHEDULED] = (prefs[KEY_SCHEDULED] ?: emptySet()) - id
        }
    }
}
