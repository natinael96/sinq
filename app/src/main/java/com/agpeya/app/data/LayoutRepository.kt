package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.model.HourLayout
import com.agpeya.app.model.Section
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    suspend fun setOrder(context: Context, hourId: String, orderedIds: List<String>) {
        update(context, hourId) { it.copy(order = orderedIds) }
    }

    suspend fun toggleHidden(context: Context, hourId: String, sectionId: String) {
        update(context, hourId) {
            it.copy(hidden = if (sectionId in it.hidden) it.hidden - sectionId else it.hidden + sectionId)
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

    /** All sections in the user's order (newly added sections appended), hidden included. */
    fun ordered(sections: List<Section>, layout: HourLayout): List<Section> {
        if (layout.order.isEmpty()) return sections
        val byId = sections.associateBy { it.id }
        val inOrder = layout.order.mapNotNull { byId[it] }
        val rest = sections.filter { it.id !in layout.order }
        return inOrder + rest
    }

    /** Sections to actually display: ordered and with hidden ones removed. */
    fun visible(sections: List<Section>, layout: HourLayout): List<Section> =
        ordered(sections, layout).filter { it.id !in layout.hidden }
}
