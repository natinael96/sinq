package com.agpeya.app.search

import android.content.Context
import com.agpeya.app.data.ContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline search over all prayer text, tolerant of Amharic homophones.
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

    data class Result(
        val hourId: String,
        val hourName: String,
        val sectionIndex: Int,
        val title: String,
        val snippet: String,
    )

    suspend fun search(context: Context, rawQuery: String): List<Result> {
        val query = rawQuery.trim()
        if (query.length < 2) return emptyList()
        val needle = fold(query)

        return withContext(Dispatchers.Default) {
            val results = mutableListOf<Result>()
            for (hour in ContentRepository.hours(context)) {
                hour.sections.forEachIndexed { index, section ->
                    val haystack = buildString {
                        append(section.title)
                        section.subtitle?.let { append(' '); append(it) }
                        section.verses.forEach { append(' '); append(it) }
                    }
                    val hit = fold(haystack).indexOf(needle)
                    if (hit >= 0) {
                        results += Result(
                            hourId = hour.id,
                            hourName = hour.name,
                            sectionIndex = index,
                            title = section.title,
                            snippet = snippet(haystack, hit, query.length),
                        )
                    }
                }
            }
            results
        }
    }

    private fun snippet(text: String, matchStart: Int, matchLen: Int, radius: Int = 28): String {
        val start = (matchStart - radius).coerceAtLeast(0)
        val end = (matchStart + matchLen + radius).coerceAtMost(text.length)
        val core = text.substring(start, end).replace('\n', ' ').trim()
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        return "$prefix$core$suffix"
    }
}
