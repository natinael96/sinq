package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.highlightDataStore by preferencesDataStore(name = "highlights")

/**
 * Verse-level highlights. A verse is keyed by "sectionId:verseNumber" (globally
 * unique) and mapped to a colour key. Removing a highlight drops the entry.
 */
object HighlightRepository {

    val COLOR_KEYS = listOf("yellow", "green", "blue", "pink")

    private val KEY = stringPreferencesKey("highlights_json")
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), String.serializer())

    fun verseKey(sectionId: String, verseNumber: Int): String = "$sectionId:$verseNumber"

    private fun decode(raw: String?): Map<String, String> =
        raw?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyMap()

    fun highlights(context: Context): Flow<Map<String, String>> =
        context.highlightDataStore.data.map { decode(it[KEY]) }

    /** Merge restored highlights in; an existing colour on a verse wins. */
    suspend fun merge(context: Context, restored: Map<String, String>) {
        if (restored.isEmpty()) return
        context.highlightDataStore.edit { prefs ->
            val map = decode(prefs[KEY]).toMutableMap()
            restored.forEach { (k, v) -> map.putIfAbsent(k, v) }
            prefs[KEY] = json.encodeToString(serializer, map)
        }
    }

    /** Set [colorKey] for a verse, or pass null to clear it. */
    suspend fun setHighlight(context: Context, verseKey: String, colorKey: String?) {
        context.highlightDataStore.edit { prefs ->
            val map = decode(prefs[KEY]).toMutableMap()
            if (colorKey == null) map.remove(verseKey) else map[verseKey] = colorKey
            prefs[KEY] = json.encodeToString(serializer, map)
        }
    }
}
