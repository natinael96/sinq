package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.model.PrayerPerson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.prayerListDataStore by preferencesDataStore(name = "prayer_list")

/**
 * The people the user remembers in prayer. A plain ordered list — newest last,
 * so the list reads in the order it was built, like names added to a diptych.
 * Mirrors UserDataRepository's storage shape (one JSON list under one key).
 */
object PrayerListRepository {

    private val KEY = stringPreferencesKey("prayer_list_json")

    // encodeDefaults for the same downgrade-safety reason as UserDataRepository:
    // an entry with an empty note must not lose the field on disk.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val serializer = ListSerializer(PrayerPerson.serializer())

    private fun decode(raw: String?): List<PrayerPerson> =
        raw?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyList()

    fun people(context: Context): Flow<List<PrayerPerson>> =
        context.prayerListDataStore.data.map { decode(it[KEY]) }

    suspend fun current(context: Context): List<PrayerPerson> = people(context).first()

    private suspend fun update(context: Context, transform: (List<PrayerPerson>) -> List<PrayerPerson>) {
        context.prayerListDataStore.edit { prefs ->
            prefs[KEY] = json.encodeToString(serializer, transform(decode(prefs[KEY])))
        }
    }

    suspend fun add(context: Context, name: String, note: String) = update(context) {
        it + PrayerPerson(id = UUID.randomUUID().toString(), name = name.trim(), note = note.trim())
    }

    suspend fun edit(context: Context, id: String, name: String, note: String) = update(context) { list ->
        list.map { if (it.id == id) it.copy(name = name.trim(), note = note.trim()) else it }
    }

    suspend fun remove(context: Context, id: String) = update(context) { list ->
        list.filterNot { it.id == id }
    }

    /** Add restored people that aren't already present, keyed by id. */
    suspend fun merge(context: Context, restored: List<PrayerPerson>) {
        if (restored.isEmpty()) return
        update(context) { current ->
            val have = current.mapTo(mutableSetOf()) { it.id }
            current + restored.filterNot { it.id in have }
        }
    }
}
