package com.agpeya.app.data

import android.content.Context
import com.agpeya.app.ui.common.Passage
import com.agpeya.app.ui.reading.geezNumeral

/**
 * ምልክቶቼ — every mark the reader has left, resolved back into passages.
 *
 * Nothing here is stored. A highlight is four colours against a verse key and a
 * bookmark is a route; the citation and the two lines of text under it are read
 * from the bundle each time the list is built, so a mark can never go stale
 * against a corrected text, and the marks themselves stay as small as they are.
 *
 * The keys come from three readers and have three shapes, which is a fact of
 * where they were written rather than a design:
 *
 *  - `scripture:am-1980:luke:10:38` — the Bible reader
 *  - `am-1980:ps_50:3` — the Psalter, keyed by edition so a ግዕዝ highlight
 *    never lands on the Amharic verse
 *  - `morning_ps1:3` — a verse inside an hour, keyed by the hour's own section
 */
object MarksRepository {

    /** One mark, ready to draw: what it is, what it says, and where it opens. */
    data class Mark(
        /** The Church's name for the passage — "የሉቃስ ወንጌል ፲፥፴፰–፴፱". */
        val citation: String,
        /** Up to two lines of the text itself, read from the bundle. */
        val snippet: String,
        /** Where tapping it goes; null when the passage can no longer be found. */
        val route: String?,
        /** The highlight colour, for a highlight; null for a bookmark. */
        val colorKey: String? = null,
        /** The heading it files under: the book, the Psalter, the hour. */
        val group: String,
        /** Every key this mark covers, so removing it removes the whole run. */
        val keys: List<String> = emptyList(),
    )

    /** A verse key taken apart. */
    private sealed interface Ref {
        data class Scripture(val edition: String, val book: String, val chapter: Int, val verse: Int) : Ref
        data class Psalm(val edition: String, val psalm: Int, val verse: Int) : Ref
        data class Hour(val sectionId: String, val verse: Int) : Ref
    }

    private fun parse(key: String): Ref? {
        val parts = key.split(':')
        val verse = parts.lastOrNull()?.toIntOrNull() ?: return null
        return when {
            parts.size == 5 && parts[0] == "scripture" ->
                parts[3].toIntOrNull()?.let { Ref.Scripture(parts[1], parts[2], it, verse) }
            parts.size == 3 && parts[1].startsWith("ps_") ->
                parts[1].removePrefix("ps_").toIntOrNull()?.let { Ref.Psalm(parts[0], it, verse) }
            parts.size == 2 -> Ref.Hour(parts[0], verse)
            else -> null
        }
    }

    /** What a run of verses is keyed under, ignoring the verse itself. */
    private fun sectionOf(key: String): String = key.substringBeforeLast(':')

    /**
     * The highlights, as marks.
     *
     * Consecutive verses of one section sharing a colour collapse into a single
     * run: someone who painted ሉቃስ ፲፥፴፰–፵፪ made one mark, and a list that shows
     * it as five is a list of the storage rather than of what they did.
     */
    suspend fun highlights(
        context: Context,
        highlights: Map<String, String>,
        psalmName: String,
        psalterTitle: String,
    ): List<Mark> {
        if (highlights.isEmpty()) return emptyList()
        val bookNames = runCatching { ScriptureRepository.bookNames(context) }.getOrDefault(emptyMap())
        val hourSections: Map<String, HourSection> =
            runCatching { hourSections(context) }.getOrDefault(emptyMap())

        // Group by section and colour, then split each group into runs of
        // consecutive verses.
        return highlights.entries
            .mapNotNull { (key, color) ->
                val ref = parse(key) ?: return@mapNotNull null
                Triple(key, color, ref)
            }
            .groupBy { (key, color, _) -> sectionOf(key) to color }
            .flatMap { (sectionColor, entries) ->
                val (_, color) = sectionColor
                val sorted = entries.sortedBy { verseOf(it.third) }
                runs(sorted.map { verseOf(it.third) }).mapNotNull { run ->
                    val keys = sorted.filter { verseOf(it.third) in run }.map { it.first }
                    val ref = sorted.first { verseOf(it.third) == run.first }.third
                    mark(context, ref, run, color, keys, bookNames, hourSections, psalmName, psalterTitle)
                }
            }
            .sortedWith(compareBy({ it.group }, { it.citation }))
    }

    private fun verseOf(ref: Ref): Int = when (ref) {
        is Ref.Scripture -> ref.verse
        is Ref.Psalm -> ref.verse
        is Ref.Hour -> ref.verse
    }

    /** Consecutive numbers, split into runs. */
    internal fun runs(numbers: List<Int>): List<IntRange> {
        if (numbers.isEmpty()) return emptyList()
        val sorted = numbers.distinct().sorted()
        val out = mutableListOf<IntRange>()
        var start = sorted.first()
        var previous = start
        sorted.drop(1).forEach { n ->
            if (n != previous + 1) {
                out += start..previous
                start = n
            }
            previous = n
        }
        out += start..previous
        return out
    }

    private suspend fun mark(
        context: Context,
        ref: Ref,
        run: IntRange,
        color: String,
        keys: List<String>,
        bookNames: Map<String, String>,
        hourSections: Map<String, HourSection>,
        psalmName: String,
        psalterTitle: String,
    ): Mark? = when (ref) {
        is Ref.Scripture -> {
            val name = bookNames[ref.book] ?: ref.book
            val verses = runCatching {
                ScriptureRepository.book(context, ref.book)
                    ?.chapters?.firstOrNull { it.chapter == ref.chapter }
                    ?.verses.orEmpty()
                    .filter { it.n in run }
                    .map { it.text }
            }.getOrDefault(emptyList())
            Mark(
                citation = Citation.of(name, ref.chapter, run.first, run.last),
                snippet = verses.joinToString(" "),
                route = "scripture/${ref.book}/${ref.chapter}?start=${run.first}&end=${run.last}",
                colorKey = color,
                group = name,
                keys = keys,
            )
        }
        is Ref.Psalm -> {
            val verses = runCatching {
                ScriptureRepository.psalms(context, ref.edition == HighlightRepository.GEEZ_PSALTER_NAMESPACE)
                    .firstOrNull { it.number == ref.psalm }
                    ?.let { section -> run.mapNotNull { section.verses.getOrNull(it - section.firstVerse) } }
                    .orEmpty()
            }.getOrDefault(emptyList())
            Mark(
                citation = Citation.of(psalmName, ref.psalm, run.first, run.last),
                snippet = verses.joinToString(" "),
                route = "psalter?section=${ref.psalm - 1}&start=${run.first}&end=${run.last}",
                colorKey = color,
                group = psalterTitle,
                keys = keys,
            )
        }
        is Ref.Hour -> {
            val found = hourSections[ref.sectionId] ?: return null
            val section = found.section
            val verses = run.mapNotNull { section.verses.getOrNull(it - section.firstVerse) }
            Mark(
                citation = listOfNotNull(
                    section.reference?.takeIf { it.isNotBlank() && it != section.title } ?: section.title,
                    "${geezNumeral(run.first)}" +
                        if (run.last != run.first) "–${geezNumeral(run.last)}" else "",
                ).joinToString("፥"),
                snippet = verses.joinToString(" "),
                route = "reading/${found.hourId}?sectionId=${android.net.Uri.encode(section.id)}",
                colorKey = color,
                group = found.hourName,
                keys = keys,
            )
        }
    }

    /** Where a verse highlighted inside an hour actually lives. */
    private data class HourSection(
        val hourId: String,
        val hourName: String,
        val section: com.agpeya.app.model.Section,
    )

    /** sectionId → the hour and section it belongs to. */
    private suspend fun hourSections(context: Context): Map<String, HourSection> =
        ContentRepository.hours(context).flatMap { hour ->
            hour.sections.map { it.id to HourSection(hour.id, hour.name, it) }
        }.toMap()

    /** A mark as the export writes it. */
    fun asPassage(mark: Mark): Passage =
        Passage(verses = listOf(null to mark.snippet), citation = mark.citation)
}
