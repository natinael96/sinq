package com.agpeya.app.search

import android.content.Context
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.model.Section
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline search over all prayer text — the prayer hours AND the 150-psalm
 * Psalter — tolerant of Amharic homophones.
 *
 * Ge'ez script writes several distinct letters that sound identical, and users
 * spell the same word different ways (ጸሎት / ፀሎት, ሰላም / ሠላም, ሀ / ሐ / ኀ / ኸ,
 * አ / ዐ). [fold] maps each homophone family to one canonical consonant while
 * preserving its vowel order, so a query matches regardless of which form was
 * typed. Both the indexed text and the query are folded before comparison.
 */
object AmharicSearch {

    /** Fold one character to its canonical homophone form, keeping the vowel order. */
    fun foldChar(c: Char): Char = when (val cp = c.code) {
        in 0x1220..0x1227 -> (0x1230 + (cp - 0x1220)).toChar() // ሠ-series -> ሰ-series
        in 0x1210..0x1217 -> (0x1200 + (cp - 0x1210)).toChar() // ሐ-series -> ሀ-series
        in 0x1280..0x1287 -> (0x1200 + (cp - 0x1280)).toChar() // ኀ-series -> ሀ-series
        in 0x12B8..0x12BF -> (0x1200 + (cp - 0x12B8)).toChar() // ኸ-series -> ሀ-series
        in 0x12D0..0x12D7 -> (0x12A0 + (cp - 0x12D0)).toChar() // ዐ-series -> አ-series
        in 0x1340..0x1347 -> (0x1338 + (cp - 0x1340)).toChar() // ፀ-series -> ጸ-series
        else -> c
    }

    fun fold(text: String): String = buildString(text.length) {
        for (c in text) append(foldChar(c))
    }

    /** Where a result came from — lets the UI label it and route the tap. */
    enum class Source { HOUR, PSALTER }

    data class Result(
        val source: Source,
        /** Real hour id for [Source.HOUR]; empty for the Psalter. */
        val targetId: String,
        /** Display label of the origin (hour name, or the Psalter title). */
        val sourceLabel: String,
        /** Section index within the hour, or psalm index within the full psalter. */
        val targetIndex: Int,
        val title: String,
        val snippet: String,
        /** Match offset within [snippet] for highlighting; -1 if not locatable. */
        val snippetMatchStart: Int,
        val snippetMatchLen: Int,
    )

    suspend fun search(context: Context, rawQuery: String, psalterLabel: String): List<Result> {
        val query = rawQuery.trim()
        if (query.length < 2) return emptyList()
        val needle = fold(query)

        return withContext(Dispatchers.Default) {
            val results = mutableListOf<Result>()
            for (hour in ContentRepository.hours(context)) {
                hour.sections.forEachIndexed { index, section ->
                    match(section, needle, query.length)?.let { snip ->
                        results += Result(
                            source = Source.HOUR,
                            targetId = hour.id,
                            sourceLabel = hour.name,
                            targetIndex = index,
                            title = section.title,
                            snippet = snip.text,
                            snippetMatchStart = snip.matchStart,
                            snippetMatchLen = snip.matchLen,
                        )
                    }
                }
            }
            ContentRepository.psalter(context).forEachIndexed { index, psalm ->
                match(psalm, needle, query.length)?.let { snip ->
                    results += Result(
                        source = Source.PSALTER,
                        targetId = "",
                        sourceLabel = psalterLabel,
                        targetIndex = index,
                        title = psalm.title,
                        snippet = snip.text,
                        snippetMatchStart = snip.matchStart,
                        snippetMatchLen = snip.matchLen,
                    )
                }
            }
            results
        }
    }

    /** Text of a section, searchable as one blob (title + subtitle + verses). */
    private fun haystackOf(section: Section): String = buildString {
        append(section.title)
        section.subtitle?.let { append(' '); append(it) }
        section.verses.forEach { append(' '); append(it) }
    }

    /** Fold + find [needle] in a section; returns a highlighted snippet or null. */
    private fun match(section: Section, needle: String, matchLen: Int): Snip? {
        val haystack = haystackOf(section)
        // fold is 1:1 per character, so an index in the folded text is the same
        // index in the original — the highlight lands on the real characters.
        val hit = fold(haystack).indexOf(needle)
        return if (hit >= 0) snippet(haystack, hit, matchLen) else null
    }

    private data class Snip(val text: String, val matchStart: Int, val matchLen: Int)

    private fun snippet(text: String, matchStart: Int, matchLen: Int, radius: Int = 28): Snip {
        val start = (matchStart - radius).coerceAtLeast(0)
        val end = (matchStart + matchLen + radius).coerceAtMost(text.length)
        val prefix = if (start > 0) "…" else ""
        val core = text.substring(start, end).replace('\n', ' ')
        val suffix = if (end < text.length) "…" else ""
        // Offsets stay exact because we don't trim (which would shift them).
        return Snip(prefix + core + suffix, prefix.length + (matchStart - start), matchLen)
    }
}
