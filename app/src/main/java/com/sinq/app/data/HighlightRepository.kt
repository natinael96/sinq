package com.sinq.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.highlightDataStore by preferencesDataStore(name = "highlights")

/**
 * Verse-level highlights stored in SQLite Room via [HighlightDao].
 * Prayer sections use "sectionId:verseNumber"; content with multiple editions
 * prefixes that identity with its edition namespace (e.g. "scripture:am-1980:gen:1:1").
 * Removing a highlight drops the entry from the table.
 */
object HighlightRepository {

    val COLOR_KEYS = listOf("yellow", "green", "blue", "pink")
    const val AMHARIC_PSALTER_NAMESPACE = "am-1980"
    const val GEEZ_PSALTER_NAMESPACE = "gez-1980"

    private val KEY = stringPreferencesKey("highlights_json")
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), String.serializer())

    @Volatile
    private var migratedFromDataStore = false

    private fun dao(context: Context) = JournalDatabase.get(context).highlightDao()

    fun verseKey(sectionId: String, verseNumber: Int, namespace: String? = null): String =
        listOfNotNull(namespace, sectionId, verseNumber.toString()).joinToString(":")

    private fun decode(raw: String?): Map<String, String> =
        raw?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyMap()

    private suspend fun ensureMigrated(context: Context) {
        if (migratedFromDataStore) return
        synchronized(this) {
            if (migratedFromDataStore) return
        }
        val raw = runCatching { context.highlightDataStore.data.first()[KEY] }.getOrNull()
        if (!raw.isNullOrBlank()) {
            val legacy = migrateLegacyPsalterMap(decode(raw))
            if (legacy.isNotEmpty()) {
                val entities = legacy.map { (k, v) -> HighlightEntity(k, v) }
                dao(context).upsertAll(entities)
            }
        }
        migratedFromDataStore = true
    }

    /** Reactive stream of all active highlights. */
    fun highlights(context: Context): Flow<Map<String, String>> =
        dao(context).all().map { list ->
            list.associate { it.verseKey to it.colorToken }
        }

    /** Fast indexed query for highlights matching a chapter / book prefix. */
    fun forPrefix(context: Context, prefix: String): Flow<Map<String, String>> =
        dao(context).forPrefix(prefix).map { list ->
            list.associate { it.verseKey to it.colorToken }
        }

    /**
     * Old builds stored Psalter highlights as `ps_N:verse`. Migrate legacy keys.
     */
    suspend fun migrateLegacyPsalterKeys(context: Context) {
        ensureMigrated(context)
        val all = dao(context).all().first()
        val legacy = all.filter { isLegacyPsalterKey(it.verseKey) }
        if (legacy.isNotEmpty()) {
            val updated = legacy.map {
                HighlightEntity("$AMHARIC_PSALTER_NAMESPACE:${it.verseKey}", it.colorToken, it.timestamp)
            }
            dao(context).deleteAll(legacy.map { it.verseKey })
            dao(context).upsertAll(updated)
        }
    }

    /** Merge restored highlights in; an existing colour on a verse wins. */
    suspend fun merge(context: Context, restored: Map<String, String>) {
        if (restored.isEmpty()) return
        ensureMigrated(context)
        val migrated = migrateLegacyPsalterMap(restored)
        val existing = dao(context).all().first().associate { it.verseKey to it.colorToken }
        val toInsert = mutableListOf<HighlightEntity>()
        migrated.forEach { (k, v) ->
            if (v in COLOR_KEYS && !existing.containsKey(k)) {
                toInsert.add(HighlightEntity(k, v))
            }
        }
        if (toInsert.isNotEmpty()) {
            dao(context).upsertAll(toInsert)
        }
    }

    /** Replace all highlights with restored values. */
    suspend fun replaceAll(context: Context, restored: Map<String, String>) {
        ensureMigrated(context)
        dao(context).clear()
        val migrated = migrateLegacyPsalterMap(restored)
        val toInsert = migrated.filter { it.value in COLOR_KEYS }.map { (k, v) -> HighlightEntity(k, v) }
        if (toInsert.isNotEmpty()) {
            dao(context).upsertAll(toInsert)
        }
    }

    /** Set [colorKey] for a verse, or pass null to clear it. */
    suspend fun setHighlight(context: Context, verseKey: String, colorKey: String?) {
        setHighlights(context, listOf(verseKey), colorKey)
    }

    /** Apply or clear an entire selected range in one atomic database operation. */
    suspend fun setHighlights(context: Context, verseKeys: Collection<String>, colorKey: String?) {
        if (verseKeys.isEmpty() || colorKey != null && colorKey !in COLOR_KEYS) return
        ensureMigrated(context)
        if (colorKey == null) {
            dao(context).deleteAll(verseKeys)
        } else {
            val entities = verseKeys.map { HighlightEntity(it, colorKey) }
            dao(context).upsertAll(entities)
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
