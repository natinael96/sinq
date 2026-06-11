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

    private val KEY_BOOKMARKS = stringPreferencesKey("bookmarks_json")
    private val KEY_RECENTS = stringPreferencesKey("recents_json")
    private val KEY_PROGRESS = stringPreferencesKey("progress_json")

    private val json = Json { ignoreUnknownKeys = true }
    private val bookmarkListSerializer = ListSerializer(Bookmark.serializer())
    private val stringListSerializer = ListSerializer(String.serializer())

    // ---- Bookmarks ----

    fun bookmarks(context: Context): Flow<List<Bookmark>> =
        context.userDataStore.data.map { prefs ->
            prefs[KEY_BOOKMARKS]?.let {
                runCatching { json.decodeFromString(bookmarkListSerializer, it) }.getOrNull()
            } ?: emptyList()
        }

    suspend fun toggleBookmark(context: Context, bookmark: Bookmark) {
        context.userDataStore.edit { prefs ->
            val current = prefs[KEY_BOOKMARKS]?.let {
                runCatching { json.decodeFromString(bookmarkListSerializer, it) }.getOrNull()
            } ?: emptyList()
            val next = if (current.any { it.sectionId == bookmark.sectionId }) {
                current.filterNot { it.sectionId == bookmark.sectionId }
            } else {
                current + bookmark
            }
            prefs[KEY_BOOKMARKS] = json.encodeToString(bookmarkListSerializer, next)
        }
    }

    suspend fun removeBookmark(context: Context, sectionId: String) {
        context.userDataStore.edit { prefs ->
            val current = prefs[KEY_BOOKMARKS]?.let {
                runCatching { json.decodeFromString(bookmarkListSerializer, it) }.getOrNull()
            } ?: emptyList()
            prefs[KEY_BOOKMARKS] =
                json.encodeToString(bookmarkListSerializer, current.filterNot { it.sectionId == sectionId })
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
