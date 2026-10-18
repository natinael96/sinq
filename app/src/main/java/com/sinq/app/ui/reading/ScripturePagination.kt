package com.sinq.app.ui.reading

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.sinq.app.model.ScriptureVerse

/**
 * A page in Paged Leaf mode containing a slice of verses.
 */
data class ScripturePage(
    val pageIndex: Int,
    val verses: List<ScriptureVerse>,
    val startVerse: Int,
    val endVerse: Int,
) {
    fun containsVerse(verseNum: Int): Boolean = verseNum in startVerse..endVerse
}

object ScripturePagination {
    /**
     * Paginates a list of verses based on font size, line spacing, and container dimensions.
     * Higher font size -> fewer characters per page -> more pages.
     * Lower font size -> more characters per page -> fewer pages.
     */
    fun paginate(
        verses: List<ScriptureVerse>,
        fontSizeSp: Int,
        pageHeightDp: Int = 600,
        pageWidthDp: Int = 360,
    ): List<ScripturePage> {
        if (verses.isEmpty()) return emptyList()

        val safeFont = fontSizeSp.coerceIn(10, 48)
        val charsPerLine = ((pageWidthDp * 1.5f) / safeFont).coerceIn(16f, 80f)
        val linesPerPage = (pageHeightDp / (safeFont * 1.6f)).coerceIn(6f, 40f)
        val maxCharsPerPage = (charsPerLine * linesPerPage).toInt().coerceAtLeast(120)

        val pages = mutableListOf<ScripturePage>()
        var currentVerses = mutableListOf<ScriptureVerse>()
        var currentChars = 0

        for (verse in verses) {
            val verseLen = verse.text.length + 8 // accommodate numeral prefix and spacing
            if (currentVerses.isNotEmpty() && (currentChars + verseLen > maxCharsPerPage)) {
                pages.add(
                    ScripturePage(
                        pageIndex = pages.size,
                        verses = currentVerses.toList(),
                        startVerse = currentVerses.first().n,
                        endVerse = currentVerses.last().n,
                    )
                )
                currentVerses = mutableListOf(verse)
                currentChars = verseLen
            } else {
                currentVerses.add(verse)
                currentChars += verseLen
            }
        }
        if (currentVerses.isNotEmpty()) {
            pages.add(
                ScripturePage(
                    pageIndex = pages.size,
                    verses = currentVerses.toList(),
                    startVerse = currentVerses.first().n,
                    endVerse = currentVerses.last().n,
                )
            )
        }
        return pages
    }

    /**
     * Resolves which page index contains the target verse number.
     * If the target verse is not found, returns the nearest preceding page.
     */
    fun findPageIndexForVerse(pages: List<ScripturePage>, verseNum: Int): Int {
        if (pages.isEmpty()) return 0
        val exact = pages.indexOfFirst { it.containsVerse(verseNum) }
        if (exact >= 0) return exact
        val closest = pages.indexOfLast { it.startVerse <= verseNum }
        return if (closest >= 0) closest else 0
    }

    /**
     * Given a verse key like "scripture:am-1980:gen:1:12", extracts the verse number.
     */
    fun parseVerseNumber(verseKey: String?): Int? {
        if (verseKey.isNullOrBlank()) return null
        return verseKey.substringAfterLast(':', "").toIntOrNull()
    }
}

/**
 * Tap zones:
 * - left 20% -> previous page
 * - right 20% -> next page
 * - middle 60% -> toggle chrome
 */
fun Modifier.readerEdgeTaps(
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onToggleChrome: () -> Unit,
): Modifier = this.pointerInput(Unit) {
    detectTapGestures { offset ->
        val xRatio = offset.x / size.width
        when {
            xRatio < 0.20f -> onPreviousPage()
            xRatio > 0.80f -> onNextPage()
            else -> onToggleChrome()
        }
    }
}
