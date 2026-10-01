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

    /**
     * Replaces the static prayer-book Gospel in [sections] with the dynamic Gospel
     * appointed for [date] in the ግጻዌ (Gitsawe) lectionary.
     *
     * Falls back silently to the original sections if lectionary data or scripture
     * verses cannot be resolved, maintaining offline-first reliability.
     */
    suspend fun withDynamicGospel(
        context: Context,
        hourId: String,
        sections: List<com.agpeya.app.model.Section>,
        date: java.time.LocalDate = java.time.LocalDate.now(),
    ): List<com.agpeya.app.model.Section> {
        val gospelIndex = sections.indexOfFirst { it.type == "gospel" }
        if (gospelIndex < 0) return sections

        val dynamicGospel = resolveDynamicGospel(context, hourId, sections[gospelIndex], date)
            ?: return sections

        val result = ArrayList<com.agpeya.app.model.Section>(sections.size)
        var replaced = false
        for (section in sections) {
            if (section.type == "gospel") {
                if (!replaced) {
                    result.add(dynamicGospel.copy(id = section.id, part = section.part))
                    replaced = true
                } else if (hourId == "midnight") {
                    result.add(section)
                }
            } else {
                result.add(section)
            }
        }
        return result
    }

    private suspend fun resolveDynamicGospel(
        context: Context,
        hourId: String,
        template: com.agpeya.app.model.Section,
        date: java.time.LocalDate,
    ): com.agpeya.app.model.Section? {
        val readings = runCatching { GitsaweRepository.readingsFor(context, date) }.getOrNull()
            ?: return null

        val wengelReading = pickWengel(readings, hourId) ?: return null
        val verse = wengelReading.verse ?: return null
        val bookTitle = verse.bookTitle ?: return null
        val bookKey = ScriptureRepository.resolveBookKey(bookTitle) ?: return null
        val chapter = verse.chapter ?: return null
        val book = ScriptureRepository.book(context, bookKey) ?: return null
        val citedVerses = ScriptureRepository.passage(context, bookKey, chapter, verse.start, verse.end)
        if (citedVerses.isNullOrEmpty()) return null

        val lo = citedVerses.first().n
        val hi = citedVerses.last().n
        val amharicRef = buildString {
            append(com.agpeya.app.ui.reading.geezNumeral(chapter))
            append("፥")
            append(com.agpeya.app.ui.reading.geezNumeral(lo))
            if (hi != lo) {
                append("–")
                append(com.agpeya.app.ui.reading.geezNumeral(hi))
            }
        }
        val subtitle = "${book.nameAm} $amharicRef"
        val reference = "${book.nameEn} $chapter:$lo-$hi"

        return template.copy(
            title = "የዕለቱ ወንጌል",
            subtitle = subtitle,
            reference = reference,
            firstVerse = lo,
            verses = citedVerses.map { it.text },
        )
    }

    private fun pickWengel(
        readings: DayReadings,
        hourId: String,
    ): com.agpeya.app.model.GitsaweReading? {
        val (preferOffice, fallbackOffice) = when (hourId) {
            "morning" -> ({ s: com.agpeya.app.model.GitsaweServices -> s.negh } to { s: com.agpeya.app.model.GitsaweServices -> s.kidassie })
            "vespers", "compline", "veil" -> ({ s: com.agpeya.app.model.GitsaweServices -> s.serk } to { s: com.agpeya.app.model.GitsaweServices -> s.kidassie })
            else -> ({ s: com.agpeya.app.model.GitsaweServices -> s.kidassie } to { s: com.agpeya.app.model.GitsaweServices -> s.negh })
        }

        // 1. Seasonal prefer
        readings.seasonal.firstNotNullOfOrNull { preferOffice(it)?.wengel?.firstOrNull() }?.let { return it }
        // 2. Daily prefer
        readings.daily?.let { preferOffice(it)?.wengel?.firstOrNull() }?.let { return it }
        // 3. Seasonal fallback
        readings.seasonal.firstNotNullOfOrNull { fallbackOffice(it)?.wengel?.firstOrNull() }?.let { return it }
        // 4. Daily fallback
        readings.daily?.let { fallbackOffice(it)?.wengel?.firstOrNull() }?.let { return it }
        // 5. Any wengel in seasonal
        readings.seasonal.firstNotNullOfOrNull { s ->
            listOfNotNull(s.kidassie, s.negh, s.serk).firstNotNullOfOrNull { it.wengel.firstOrNull() }
        }?.let { return it }
        // 6. Any wengel in daily
        readings.daily?.let { d ->
            listOfNotNull(d.kidassie, d.negh, d.serk).firstNotNullOfOrNull { it.wengel.firstOrNull() }
        }?.let { return it }

        return null
    }
}
