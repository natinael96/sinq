package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.model.Bookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.userDataStore by preferencesDataStore(name = "user_data")

/** Bookmarks (section-level), recently opened hours, and per-hour scroll memory. */
object UserDataRepository {

    private const val MAX_RECENTS = 4
    private const val MAX_RECENT_SEARCHES = 8

    private val KEY_BOOKMARKS = stringPreferencesKey("bookmarks_json")
    private val KEY_RECENTS = stringPreferencesKey("recents_json")
    private val KEY_PROGRESS = stringPreferencesKey("progress_json")
    private val KEY_RECENT_SEARCHES = stringPreferencesKey("recent_searches_json")

    // encodeDefaults keeps every field in the stored JSON (a bookmark with
    // sectionIndex 0 would otherwise omit it, which older app versions — whose
    // model has no default — fail to decode, wiping the list on downgrade).
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val bookmarkListSerializer = ListSerializer(Bookmark.serializer())
    private val stringListSerializer = ListSerializer(String.serializer())

    // ---- Bookmarks ----

    fun bookmarks(context: Context): Flow<List<Bookmark>> =
        context.userDataStore.data.map { prefs ->
            prefs[KEY_BOOKMARKS]?.let {
                runCatching { json.decodeFromString(bookmarkListSerializer, it) }.getOrNull()
            } ?: emptyList()
        }

    // Identity is the (hourId, sectionId) pair: sectionId alone is not unique —
    // a psalm added into an hour shares its "ps_N" id with the Psalter's copy,
    // and matching by id only made one toggle delete the other's bookmark.
    suspend fun toggleBookmark(context: Context, bookmark: Bookmark) {
        context.userDataStore.edit { prefs ->
            val current = prefs[KEY_BOOKMARKS]?.let {
                runCatching { json.decodeFromString(bookmarkListSerializer, it) }.getOrNull()
            } ?: emptyList()
            val next = if (current.any { it.hourId == bookmark.hourId && it.sectionId == bookmark.sectionId }) {
                current.filterNot { it.hourId == bookmark.hourId && it.sectionId == bookmark.sectionId }
            } else {
                current + bookmark
            }
            prefs[KEY_BOOKMARKS] = json.encodeToString(bookmarkListSerializer, next)
        }
    }

    suspend fun removeBookmark(context: Context, hourId: String, sectionId: String) {
        context.userDataStore.edit { prefs ->
            val current = prefs[KEY_BOOKMARKS]?.let {
                runCatching { json.decodeFromString(bookmarkListSerializer, it) }.getOrNull()
            } ?: emptyList()
            prefs[KEY_BOOKMARKS] = json.encodeToString(
                bookmarkListSerializer,
                current.filterNot { it.hourId == hourId && it.sectionId == sectionId },
            )
        }
    }

    // ---- Recents ----

    fun recents(context: Context): Flow<List<String>> =
        context.userDataStore.data.map { prefs ->
            prefs[KEY_RECENTS]?.let {
                runCatching { json.decodeFromString(stringListSerializer, it) }.getOrNull()
            } ?: emptyList()
        }

    suspend fun recordRecent(context: Context, hourId: String) {
        context.userDataStore.edit { prefs ->
            val current = prefs[KEY_RECENTS]?.let {
                runCatching { json.decodeFromString(stringListSerializer, it) }.getOrNull()
            } ?: emptyList()
            val next = (listOf(hourId) + current.filterNot { it == hourId }).take(MAX_RECENTS)
            prefs[KEY_RECENTS] = json.encodeToString(stringListSerializer, next)
        }
    }

    // ---- Recent searches (most recent first) ----

    fun recentSearches(context: Context): Flow<List<String>> =
        context.userDataStore.data.map { prefs ->
            prefs[KEY_RECENT_SEARCHES]?.let {
                runCatching { json.decodeFromString(stringListSerializer, it) }.getOrNull()
            } ?: emptyList()
        }

    suspend fun addRecentSearch(context: Context, query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        context.userDataStore.edit { prefs ->
            val current = prefs[KEY_RECENT_SEARCHES]?.let {
                runCatching { json.decodeFromString(stringListSerializer, it) }.getOrNull()
            } ?: emptyList()
            val next = (listOf(q) + current.filterNot { it.equals(q, ignoreCase = true) })
                .take(MAX_RECENT_SEARCHES)
            prefs[KEY_RECENT_SEARCHES] = json.encodeToString(stringListSerializer, next)
        }
    }

    suspend fun clearRecentSearches(context: Context) {
        context.userDataStore.edit { it.remove(KEY_RECENT_SEARCHES) }
    }

    // ---- Scroll memory (hourId -> section index) ----

    suspend fun savedPosition(context: Context, hourId: String): Int {
        val map = context.userDataStore.data.first()[KEY_PROGRESS]?.let {
            runCatching { json.decodeFromString<Map<String, Int>>(it) }.getOrNull()
        } ?: emptyMap()
        return map[hourId] ?: 0
    }

    suspend fun savePosition(context: Context, hourId: String, index: Int) {
        context.userDataStore.edit { prefs ->
            val map = prefs[KEY_PROGRESS]?.let {
                runCatching { json.decodeFromString<Map<String, Int>>(it) }.getOrNull()
            } ?: emptyMap()
            prefs[KEY_PROGRESS] = json.encodeToString(map + (hourId to index))
        }
    }
}
