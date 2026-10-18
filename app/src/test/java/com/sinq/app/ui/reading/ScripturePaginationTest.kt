package com.sinq.app.ui.reading

import com.sinq.app.data.UserDataRepository
import com.sinq.app.model.ScriptureVerse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScripturePaginationTest {

    private fun sampleVerses(count: Int): List<ScriptureVerse> = (1..count).map { n ->
        ScriptureVerse(
            n = n,
            text = "በመጀመሪያ እግዚአብሔር ሰማይንና ምድርን ፈጠረ። ምድርም ባዶ ነበረች አንዳችም አልነበረባትም፤ ጨለማም በጥልቁ ላይ ነበረ፤ የእግዚአብሔርም መንፈስ በውኃ ላይ ሰፍፎ ነበር። ቁጥር $n",
        )
    }

    @Test
    fun `pagination splits verses into multiple pages`() {
        val verses = sampleVerses(30)
        val pages = ScripturePagination.paginate(verses, fontSizeSp = 18)
        assertTrue("Expected multiple pages for 30 verses", pages.size > 1)
        assertEquals(1, pages.first().startVerse)
        assertEquals(30, pages.last().endVerse)
    }

    @Test
    fun `increasing font size yields more pages and decreasing yields fewer`() {
        val verses = sampleVerses(30)
        val smallFontPages = ScripturePagination.paginate(verses, fontSizeSp = 14)
        val mediumFontPages = ScripturePagination.paginate(verses, fontSizeSp = 20)
        val largeFontPages = ScripturePagination.paginate(verses, fontSizeSp = 28)

        assertTrue(
            "Large font should produce at least as many pages as small font: ${largeFontPages.size} >= ${smallFontPages.size}",
            largeFontPages.size >= smallFontPages.size,
        )
        assertTrue(
            "Medium font should produce at least as many pages as small font: ${mediumFontPages.size} >= ${smallFontPages.size}",
            mediumFontPages.size >= smallFontPages.size,
        )
    }

    @Test
    fun `changing font size mid-chapter keeps the same verse key visible`() {
        val verses = sampleVerses(35)
        val targetVerse = 19

        // 1. Reader starts at font size 18sp
        val pagesAt18 = ScripturePagination.paginate(verses, fontSizeSp = 18)
        val pageIdxAt18 = ScripturePagination.findPageIndexForVerse(pagesAt18, targetVerse)
        assertTrue("Page at 18sp must contain verse 19", pagesAt18[pageIdxAt18].containsVerse(targetVerse))

        // Save position as verse key (content-addressed contract)
        val savedKey = UserDataRepository.scriptureVerseKey("am-1980", "gen", 1, targetVerse)
        assertEquals("scripture:am-1980:gen:1:19", savedKey)

        // 2. User increases font size mid-chapter to 28sp
        val parsedVerse = ScripturePagination.parseVerseNumber(savedKey) ?: 1
        assertEquals(targetVerse, parsedVerse)
        val pagesAt28 = ScripturePagination.paginate(verses, fontSizeSp = 28)
        val pageIdxAt28 = ScripturePagination.findPageIndexForVerse(pagesAt28, parsedVerse)
        assertTrue(
            "After font enlargement, the resolved page must still contain verse $targetVerse",
            pagesAt28[pageIdxAt28].containsVerse(targetVerse),
        )

        // 3. User decreases font size mid-chapter to 12sp
        val pagesAt12 = ScripturePagination.paginate(verses, fontSizeSp = 12)
        val pageIdxAt12 = ScripturePagination.findPageIndexForVerse(pagesAt12, parsedVerse)
        assertTrue(
            "After font reduction, the resolved page must still contain verse $targetVerse",
            pagesAt12[pageIdxAt12].containsVerse(targetVerse),
        )
    }

    @Test
    fun `edge cases - empty list and single verse`() {
        val emptyPages = ScripturePagination.paginate(emptyList(), fontSizeSp = 18)
        assertTrue(emptyPages.isEmpty())
        assertEquals(0, ScripturePagination.findPageIndexForVerse(emptyPages, 5))

        val singleVerse = sampleVerses(1)
        val singlePages = ScripturePagination.paginate(singleVerse, fontSizeSp = 18)
        assertEquals(1, singlePages.size)
        assertEquals(1, singlePages.first().startVerse)
        assertEquals(1, singlePages.first().endVerse)
    }
}
