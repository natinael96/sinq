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
 * Verse-level highlights. Prayer sections use "sectionId:verseNumber"; content
 * with multiple editions prefixes that identity with its edition namespace.
 * Removing a highlight drops the entry.
 */
object HighlightRepository {

    val COLOR_KEYS = listOf("yellow", "green", "blue", "pink")
    const val AMHARIC_PSALTER_NAMESPACE = "am-1980"
    const val GEEZ_PSALTER_NAMESPACE = "gez-1980"

    private val KEY = stringPreferencesKey("highlights_json")
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), String.serializer())

    fun verseKey(sectionId: String, verseNumber: Int, namespace: String? = null): String =
        listOfNotNull(namespace, sectionId, verseNumber.toString()).joinToString(":")

    private fun decode(raw: String?): Map<String, String> =
        raw?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyMap()

    fun highlights(context: Context): Flow<Map<String, String>> =
        context.highlightDataStore.data.map { migrateLegacyPsalterMap(decode(it[KEY])) }

    /**
     * Old builds stored Psalter highlights as `ps_N:verse`. Those keys represented
     * the then-default Amharic text, so migrate them into that edition rather than
     * allowing them to leak onto Ge'ez verses with different versification.
     */
    suspend fun migrateLegacyPsalterKeys(context: Context) {
        context.highlightDataStore.edit { prefs ->
            val current = decode(prefs[KEY])
            val migrated = migrateLegacyPsalterMap(current)
            if (migrated != current) prefs[KEY] = json.encodeToString(serializer, migrated)
        }
    }

    /** Merge restored highlights in; an existing colour on a verse wins. */
    suspend fun merge(context: Context, restored: Map<String, String>) {
        if (restored.isEmpty()) return
        context.highlightDataStore.edit { prefs ->
            val map = migrateLegacyPsalterMap(decode(prefs[KEY])).toMutableMap()
            migrateLegacyPsalterMap(restored).forEach { (k, v) ->
                if (v in COLOR_KEYS) map.putIfAbsent(k, v)
            }
            prefs[KEY] = json.encodeToString(serializer, map)
        }
    }

    /** Set [colorKey] for a verse, or pass null to clear it. */
    suspend fun setHighlight(context: Context, verseKey: String, colorKey: String?) {
        setHighlights(context, listOf(verseKey), colorKey)
    }

    /** Apply or clear an entire selected range in one atomic DataStore update. */
    suspend fun setHighlights(context: Context, verseKeys: Collection<String>, colorKey: String?) {
        if (verseKeys.isEmpty() || colorKey != null && colorKey !in COLOR_KEYS) return
        context.highlightDataStore.edit { prefs ->
            val map = migrateLegacyPsalterMap(decode(prefs[KEY])).toMutableMap()
            verseKeys.forEach { verseKey ->
                if (colorKey == null) map.remove(verseKey) else map[verseKey] = colorKey
            }
            prefs[KEY] = json.encodeToString(serializer, map)
        }
    }

    internal fun migrateLegacyPsalterMap(source: Map<String, String>): Map<String, String> {
        if (source.keys.none(::isLegacyPsalterKey)) return source
        val migrated = LinkedHashMap<String, String>(source.size)
        source.forEach { (key, color) ->
            if (!isLegacyPsalterKey(key)) migrated[key] = color
        }
        source.forEach { (key, color) ->
            if (isLegacyPsalterKey(key)) {
                migrated.putIfAbsent("$AMHARIC_PSALTER_NAMESPACE:$key", color)
            }
        }
        return migrated
    }

    private fun isLegacyPsalterKey(key: String): Boolean {
        val section = key.substringBefore(':')
        val verse = key.substringAfter(':', missingDelimiterValue = "")
        return section.startsWith("ps_") && section.removePrefix("ps_").toIntOrNull() != null &&
            verse.toIntOrNull() != null
    }
}
