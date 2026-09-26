package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.Hour
import com.agpeya.app.model.Manifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Loads the bundled prayer content from assets once and caches it in memory
 * (~76k chars total — small enough to hold). Replaced by Room when search
 * and bookmarks arrive (PLAN.md Phase 5).
 */
object ContentRepository {

    private const val TAG = "ContentRepository"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: List<Hour>? = null

    /** All 150 psalms, ordered by number — used by the "add psalm" picker. */
    suspend fun psalter(context: Context): List<com.agpeya.app.model.Section> =
        ScriptureRepository.psalms(context, geez = false)

    suspend fun psalm(context: Context, number: Int): com.agpeya.app.model.Section? =
        psalter(context).find { it.number == number }

    suspend fun hours(context: Context): List<Hour> =
        cache ?: withContext(Dispatchers.IO) {
            val app = context.applicationContext
            // Latin book names are a fact of the extraction, not of the prayer
            // book; they are turned into the Church's own here, once, so that
            // everything downstream — sharing above all — reads in Amharic.
            val namesByEnglish = runCatching {
                ScriptureRepository.books(app)
                    .associate { Citation.englishKey(it.nameEn) to it.nameAm }
            }.getOrDefault(emptyMap())
            load(app).map { hour -> hour.copy(sections = hour.sections.map { it.inAmharic(namesByEnglish) }) }
                .also { cache = it }
        }

    /** A section whose reference reads as the app writes every other reference. */
    private fun com.agpeya.app.model.Section.inAmharic(
        namesByEnglish: Map<String, String>,
    ): com.agpeya.app.model.Section {
        val raw = reference?.takeIf { it.isNotBlank() } ?: return this
        val named = Citation.fromLatin(raw, PSALM_NAME, namesByEnglish) ?: return this
        return copy(reference = named)
    }

    /** How a single psalm is named — the Psalter is not in the book catalogue. */
    private const val PSALM_NAME = "መዝሙር"

    suspend fun hour(context: Context, hourId: String): Hour? =
        hours(context).find { it.id == hourId }

    private fun load(context: Context): List<Hour> = runCatching {
        val assets = context.assets
        val manifest = json.decodeFromString<Manifest>(
            assets.open("content/manifest.json").readBytes().decodeToString()
        )
        // A single corrupt hour file is skipped rather than sinking the whole app.
        manifest.hours.mapNotNull { entry ->
            runCatching {
                json.decodeFromString<Hour>(
                    assets.open("content/${entry.file}").readBytes().decodeToString()
                )
            }.onFailure { Log.e(TAG, "Failed to load hour ${entry.file}", it) }.getOrNull()
        }.sortedBy { it.orderIndex }
    }.onFailure { Log.e(TAG, "Failed to load manifest.json", it) }.getOrDefault(emptyList())

    /** Which hour fits the current time of day — drives the Home suggestion. */
    fun suggestedHourId(hourOfDay: Int): String = when (hourOfDay) {
        in 4..7 -> "morning"
        in 8..10 -> "terce"
        in 11..13 -> "sext"
        in 14..16 -> "none"
        in 17..19 -> "vespers"
        in 20..22 -> "compline"
        else -> "midnight"
    }
}
