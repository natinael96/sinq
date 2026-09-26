package com.agpeya.app.data

import android.content.Context
import com.agpeya.app.model.Section
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class PrayerLevelData(
    val singlePsalmNumber: Int = 50,
    val hours: Map<String, List<String>>,
)

/** Loads the liturgically curated Psalm progression bundled with the app. */
object PrayerLevelRepository {
    private val json = Json { ignoreUnknownKeys = true }
    @Volatile private var cache: PrayerLevelData? = null

    private suspend fun data(context: Context): PrayerLevelData =
        cache ?: withContext(Dispatchers.IO) {
            runCatching {
                json.decodeFromString<PrayerLevelData>(
                    context.assets.open("content/prayer_levels.json").readBytes().decodeToString(),
                )
            }.getOrDefault(PrayerLevelData(hours = emptyMap())).also { cache = it }
        }

    suspend fun apply(
        context: Context,
        hourId: String,
        sections: List<Section>,
        level: PrayerLevel,
    ): List<Section> {
        if (level == PrayerLevel.FULL) return gospelsLastByPart(sections)
        val configuration = data(context)
        if (level == PrayerLevel.PSALM_50) {
            val existing = sections.firstOrNull {
                it.type == "psalm" && it.id.substringAfterLast("_ps").toIntOrNull() == configuration.singlePsalmNumber
            }
            val psalm = existing ?: ContentRepository.psalm(context, configuration.singlePsalmNumber)
            return gospelsLastByPart(listOfNotNull(psalm) + sections.filter { it.type != "psalm" })
        }

        val requestedCount = if (hourId == "midnight") {
            when (level) {
                PrayerLevel.PSALM_50 -> 1
                PrayerLevel.BEGINNING -> 7
                PrayerLevel.GROWTH -> 14
                PrayerLevel.STEADFAST -> 24
                PrayerLevel.FULL -> Int.MAX_VALUE
            }
        } else {
            when (level) {
                PrayerLevel.PSALM_50 -> 1
                PrayerLevel.BEGINNING -> 3
                PrayerLevel.GROWTH -> 7
                PrayerLevel.STEADFAST -> 10
                PrayerLevel.FULL -> Int.MAX_VALUE
            }
        }
        val selectedPsalmIds = configuration.hours[hourId].orEmpty().take(requestedCount).toSet()
        // Unknown/user-added Psalms stay untouched; configured Agpeya Psalms follow the level.
        val configuredIds = configuration.hours[hourId].orEmpty().toSet()
        return gospelsLastByPart(
            sections.filter { it.type != "psalm" || it.id !in configuredIds || it.id in selectedPsalmIds },
        )
    }

    /** Keep Midnight's watches intact while making each watch's Gospel its conclusion. */
    private fun gospelsLastByPart(sections: List<Section>): List<Section> =
        sections.groupBy { it.part }.values.flatMap { part ->
            part.filter { it.type != "gospel" } + part.filter { it.type == "gospel" }
        }
}
