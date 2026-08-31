package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.model.HourLayout
import com.agpeya.app.model.Section
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.layoutDataStore by preferencesDataStore(name = "layouts")

/** Per-hour show/hide + reorder customization (the personal "prayer plan"). */
object LayoutRepository {

    private val KEY = stringPreferencesKey("layouts_json")
    private val json = Json { ignoreUnknownKeys = true }
    private val mapSerializer = MapSerializer(String.serializer(), HourLayout.serializer())

    private fun decode(raw: String?): Map<String, HourLayout> =
        raw?.let { runCatching { json.decodeFromString(mapSerializer, it) }.getOrNull() } ?: emptyMap()

    fun layout(context: Context, hourId: String): Flow<HourLayout> =
        context.layoutDataStore.data.map { decode(it[KEY])[hourId] ?: HourLayout() }

    suspend fun current(context: Context): Map<String, HourLayout> =
        context.layoutDataStore.data.map { decode(it[KEY]) }.first()

    /** Existing device customizations win; missing hour layouts come from the backup. */
    suspend fun merge(context: Context, restored: Map<String, HourLayout>) {
        if (restored.isEmpty()) return
        // A hand-edited backup can repeat a psalm number or section id; the
        // duplicates would render twice and collide as lazy-list keys.
        val safe = restored.mapValues { (_, layout) ->
            layout.copy(order = layout.order.distinct(), added = layout.added.distinct())
        }
        context.layoutDataStore.edit { prefs ->
            val current = decode(prefs[KEY])
            prefs[KEY] = json.encodeToString(mapSerializer, safe + current)
        }
    }

    suspend fun setOrder(context: Context, hourId: String, orderedIds: List<String>) {
        update(context, hourId) { it.copy(order = orderedIds) }
    }

    suspend fun toggleHidden(context: Context, hourId: String, sectionId: String) {
        update(context, hourId) {
            it.copy(hidden = if (sectionId in it.hidden) it.hidden - sectionId else it.hidden + sectionId)
        }
    }

    suspend fun addPsalm(context: Context, hourId: String, number: Int) {
        update(context, hourId) {
            if (number in it.added) it else it.copy(added = it.added + number)
        }
    }

    suspend fun removePsalm(context: Context, hourId: String, number: Int) {
        update(context, hourId) {
            it.copy(added = it.added - number, hidden = it.hidden - "ps_$number", order = it.order - "ps_$number")
        }
    }

    suspend fun reset(context: Context, hourId: String) {
        context.layoutDataStore.edit { prefs ->
            val map = decode(prefs[KEY]) - hourId
            prefs[KEY] = json.encodeToString(mapSerializer, map)
        }
    }

    private suspend fun update(context: Context, hourId: String, transform: (HourLayout) -> HourLayout) {
        context.layoutDataStore.edit { prefs ->
            val map = decode(prefs[KEY])
            prefs[KEY] = json.encodeToString(mapSerializer, map + (hourId to transform(map[hourId] ?: HourLayout())))
        }
    }
}

/** Pure helpers for applying an [HourLayout] to an hour's sections. */
object PrayerLayout {

    /**
     * Combine an hour's own sections with the user-added psalms, in the user's
     * order (anything not explicitly ordered is appended). Hidden included.
     */
    fun ordered(sections: List<Section>, addedPsalms: List<Section>, layout: HourLayout): List<Section> {
        // distinctBy: ids double as lazy-list keys downstream, and an added
        // psalm that duplicates one already in the hour must not crash there.
        val all = (sections + addedPsalms).distinctBy { it.id }
        if (layout.order.isEmpty()) return all
        val byId = all.associateBy { it.id }
        val inOrder = layout.order.mapNotNull { byId[it] }
        val rest = all.filter { it.id !in layout.order }
        return inOrder + rest
    }

    /** Sections to actually display: combined, ordered, and with hidden ones removed. */
    fun visible(sections: List<Section>, addedPsalms: List<Section>, layout: HourLayout): List<Section> =
        ordered(sections, addedPsalms, layout).filter { it.id !in layout.hidden }
}
