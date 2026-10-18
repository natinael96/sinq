package com.sinq.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sinq.app.model.CustomHour
import com.sinq.app.model.Hour
import com.sinq.app.model.HoursConfig
import com.sinq.app.model.Section
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

    /** ሌሊት 9 ሰዓት (veil) ships hidden; users opt in via Manage Hours. */
    // Nothing is hidden to begin with. ሌሊት ፱ ሰዓት used to be, which made the
    // built-in mode's 22:00 reminder unreachable: a hidden hour is filtered out
    // of the mode editor and out of the scheduler alike, so the entry was
    // stored, counted, and impossible to turn on from the page that listed it.
    private val DEFAULT = HoursConfig()

    private fun decode(raw: String?): HoursConfig =
        raw?.let { runCatching { json.decodeFromString<HoursConfig>(it) }.getOrNull() } ?: DEFAULT

    fun config(context: Context): Flow<HoursConfig> =
        context.hoursDataStore.data.map { decode(it[KEY]) }

    suspend fun current(context: Context): HoursConfig = config(context).first()

    suspend fun merge(context: Context, restored: HoursConfig) {
        update(context) { current ->
            if (current == DEFAULT) restored else {
                val have = current.custom.mapTo(mutableSetOf()) { it.id }
                current.copy(
                    custom = current.custom + restored.custom.filterNot { it.id in have },
                    order = (current.order + restored.order).distinct(),
                    hidden = current.hidden + restored.hidden,
                    names = restored.names + current.names,
                )
            }
        }
    }

    suspend fun replaceAll(context: Context, restored: HoursConfig) {
        update(context) { restored }
    }

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

    /**
     * Dynamically injects a Seasonal Liturgical Banner & Rubric at the top of the canonical
     * hour based on the active liturgical season computed via [BahreHasab]:
     *  - 50 Days of Joy (በዓለ ሃምሳ) & Sabbaths: Prostration omission rubric & Paschal responsory.
     *  - Great Lent (ዓቢይ ጾም), Nineveh, & Fast Weekdays: Prostration rubric & Prayer of St. Ephrem.
     */
    fun withSeasonalAdaptation(
        context: Context,
        hourId: String,
        sections: List<Section>,
        date: java.time.LocalDate = java.time.LocalDate.now(),
    ): List<Section> {
        val ethYear = com.sinq.app.ui.common.EthiopianDate.from(date).year
        val fasika = BahreHasab.fasika(ethYear)
        val pentecost = BahreHasab.pentecost(ethYear)
        val is50DaysOfJoy = !date.isBefore(fasika) && !date.isAfter(pentecost)
        val isSabbath = date.dayOfWeek == java.time.DayOfWeek.SATURDAY || date.dayOfWeek == java.time.DayOfWeek.SUNDAY

        val greatLentStart = BahreHasab.greatLentStart(ethYear)
        val isGreatLent = !date.isBefore(greatLentStart) && !date.isAfter(fasika.minusDays(1))
        val nineveh = BahreHasab.nineveh(ethYear)
        val isNineveh = !date.isBefore(nineveh) && !date.isAfter(nineveh.plusDays(2))
        val isFastWeekday = (date.dayOfWeek == java.time.DayOfWeek.WEDNESDAY || date.dayOfWeek == java.time.DayOfWeek.FRIDAY) && !is50DaysOfJoy

        val banner = when {
            is50DaysOfJoy || isSabbath -> com.sinq.app.model.Section(
                id = "seasonal_resurrection_banner",
                orderIndex = -10,
                type = "rubric",
                title = "የትንሣኤ ሥርዓትና የዘመነ በዓለ ሃምሳ ጸሎት",
                subtitle = "በበዓለ ሃምሳ እና በሰንበታት የአክብሮት እንጂ የንስሐ ስግደት አይሰገድም",
                verses = listOf(
                    "ክርስቶስ ተንሥአ እምሙታን፤ በዐቢይ ኃይል ወሥልጣን፤ አሰሮ ለሰይጣን፤ አግአዞ ለአዳም፤ ሰላም፤ እምይእዜሰ፤ ኮነ፤ ፍሥሐ፤ ወሰላም።",
                    "አማን በአማን ተንሥአ ክርስቶስ እምሙታን፤ በሥልጣኑ ወበኃይሉ፤ ተንሥአ ክርስቶስ እምሙታን።",
                ),
            )
            isGreatLent || isNineveh || isFastWeekday -> com.sinq.app.model.Section(
                id = "seasonal_lent_ephrem_banner",
                orderIndex = -10,
                type = "prayer",
                title = "የስግደት ሥርዓትና የቅዱስ ኤፍሬም ጸሎት",
                subtitle = "በአጽዋማት ወራት የሚሰገድ ስግደት እና ጸሎተ ቅዱስ ኤፍሬም",
                verses = listOf(
                    "አቤቱ የሕይወቴ ጌታና ገዢ ሆይ፤ የስንፍናን፣ የልብ ድካምን፣ የሥልጣን መውደድን፣ እና የከንቱ ንግግርን መንፈስ አታምጣብኝ። (ስግደት)",
                    "የንጽሕናን፣ የትሕትናን፣ የትዕግሥትንና የፍቅርን መንፈስ ግን ለእኔ ለባሪያህ ስጠኝ። (ስግደት)",
                    "አቤቱ ንጉሥ ሆይ፤ የወንድሜን በደል ሳልመረምር የራሴን ኃጢአት አውቅ ዘንድ አድለኝ፤ ለዘላለሙ የተባረክህ ነህና፤ አሜን። (ስግደት)",
                ),
            )
            else -> null
        }

        return if (banner != null) listOf(banner) + sections else sections
    }
}
