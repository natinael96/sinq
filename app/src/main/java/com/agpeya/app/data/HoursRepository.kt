package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.model.CustomHour
import com.agpeya.app.model.Hour
import com.agpeya.app.model.HoursConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.hoursDataStore by preferencesDataStore(name = "hours")

/**
 * Lets the user edit the hour list: create custom hours, rename any hour,
 * reorder them, hide them, and delete custom ones. The built-in hours still
 * come from bundled assets; this layer overlays user changes on top.
 */
object HoursRepository {

    private val KEY = stringPreferencesKey("hours_config_json")
    private val json = Json { ignoreUnknownKeys = true }

    private fun decode(raw: String?): HoursConfig =
        raw?.let { runCatching { json.decodeFromString<HoursConfig>(it) }.getOrNull() } ?: HoursConfig()

    fun config(context: Context): Flow<HoursConfig> =
        context.hoursDataStore.data.map { decode(it[KEY]) }

    suspend fun current(context: Context): HoursConfig = config(context).first()

    private suspend fun update(context: Context, transform: (HoursConfig) -> HoursConfig) {
        context.hoursDataStore.edit { prefs ->
            prefs[KEY] = json.encodeToString(HoursConfig.serializer(), transform(decode(prefs[KEY])))
        }
    }

    suspend fun addCustomHour(context: Context, name: String): CustomHour {
        val hour = CustomHour(id = "custom_${UUID.randomUUID()}", name = name.ifBlank { "New hour" })
        update(context) { it.copy(custom = it.custom + hour) }
        return hour
    }

    suspend fun renameHour(context: Context, id: String, name: String) =
        update(context) { it.copy(names = it.names + (id to name)) }

    suspend fun deleteCustomHour(context: Context, id: String) = update(context) {
        it.copy(
            custom = it.custom.filterNot { c -> c.id == id },
            order = it.order - id,
            hidden = it.hidden - id,
            names = it.names - id,
        )
    }

    suspend fun setHidden(context: Context, id: String, hidden: Boolean) = update(context) {
        it.copy(hidden = if (hidden) it.hidden + id else it.hidden - id)
    }

    suspend fun setOrder(context: Context, ids: List<String>) =
        update(context) { it.copy(order = ids) }

    /** Merge built-in + custom hours, applying name overrides, order and hidden. */
    fun merge(builtIn: List<Hour>, config: HoursConfig, includeHidden: Boolean): List<Hour> {
        val customHours = config.custom.map {
            Hour(it.id, 0, config.names[it.id] ?: it.name, "", "", emptyList())
        }
        val named = builtIn.map { it.copy(name = config.names[it.id] ?: it.name) }
        val all = named + customHours
        val ordered = if (config.order.isEmpty()) all else {
            val byId = all.associateBy { it.id }
            config.order.mapNotNull { byId[it] } + all.filter { it.id !in config.order }
        }
        return if (includeHidden) ordered else ordered.filterNot { it.id in config.hidden }
    }

    /** Resolve one hour by id — a custom hour (empty base sections) or a renamed built-in. */
    suspend fun hourById(context: Context, id: String): Hour? {
        val config = current(context)
        config.custom.find { it.id == id }?.let {
            return Hour(id, 0, config.names[id] ?: it.name, "", "", emptyList())
        }
        val h = ContentRepository.hour(context, id) ?: return null
        return h.copy(name = config.names[id] ?: h.name)
    }

    /** Visible, ordered hours (built-in + custom) for Home, reminders, etc. */
    suspend fun visibleHours(context: Context): List<Hour> =
        merge(ContentRepository.hours(context), current(context), includeHidden = false)
}
