package com.agpeya.app.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where a search result goes, and how much of a corpus it can reach.
 *
 * A chapter is one document, so a match used to open the chapter unscrolled;
 * and a per-source cap of forty stopped the search itself in bundled order, so
 * a search for a common word could not reach past the front of the Bible.
 */
class SearchLandingTest {

    private val labels = AmharicSearch.Labels(
        psalter = "መዝሙር",
        scripture = "መጽሐፍ ቅዱስ",
        synaxarium = "ስንክሳር",
        wudase = "ውዳሴ ማርያም",
    )

    /** One chapter, built the way the indexer builds it. */
    private fun chapter(book: String, number: Int, verses: List<Pair<Int, String>>): AmharicSearch.Doc {
        val text = StringBuilder()
        val starts = IntArray(verses.size)
        val numbers = IntArray(verses.size)
        verses.forEachIndexed { i, (n, t) ->
            if (i > 0) text.append(' ')
            starts[i] = text.length
            numbers[i] = n
            text.append(t)
        }
        val hay = text.toString()
        return AmharicSearch.Doc(
            source = AmharicSearch.Source.SCRIPTURE,
            targetId = book,
            targetIndex = number,
            title = "$book $number",
            route = "scripture/$book/$number",
            haystack = hay,
            folded = AmharicSearch.fold(hay),
            verseStarts = starts,
            verseNumbers = numbers,
        )
    }

    private val luke10 = chapter(
        "luke", 10,
        listOf(
            1 to "ከዚህም በኋላ ጌታ ሌላዎቹን ሰብዓ ሾመ",
            38 to "ወደ ቤትም ሲገቡ ማርታ የምትባል አንዲት ሴት በቤቷ ተቀበለችው",
            39 to "ማርያም የምትባል እኅትም ነበረቻት",
        ),
    )

    private fun search(docs: List<AmharicSearch.Doc>, query: String) =
        AmharicSearch.searchDocs(docs, AmharicSearch.fold(query), query.length, labels)

    @Test
    fun `a match opens the verse it is in, not the top of the chapter`() {
        assertEquals("scripture/luke/10?start=38", search(listOf(luke10), "ማርታ").single().route)
        assertEquals("scripture/luke/10?start=39", search(listOf(luke10), "ማርያም").single().route)
    }

    @Test
    fun `a match in the first verse still names it`() {
        assertEquals("scripture/luke/10?start=1", search(listOf(luke10), "ሰብዓ").single().route)
    }

    @Test
    fun `a document with no verses keeps its own route`() {
        val day = AmharicSearch.synaxariumDocs(
            month = 1,
            ethYear = 2015,
            days = emptyList(),
        )
        assertTrue(day.isEmpty())
        val plain = AmharicSearch.Doc(
            source = AmharicSearch.Source.WUDASE,
            targetId = "monday",
            targetIndex = 0,
            title = "ውዳሴ ማርያም ዘሰኞ",
            route = "wudase?sec=monday",
            haystack = "ሰላም ለኪ",
            folded = AmharicSearch.fold("ሰላም ለኪ"),
        )
        assertEquals("wudase?sec=monday", search(listOf(plain), "ሰላም").single().route)
    }

    @Test
    fun `every match is returned, however common the word`() {
        // Sixty chapters all carrying the word: the fortieth is not the last.
        val many = (1..60).map { n ->
            chapter("genesis", n, listOf(1 to "እግዚአብሔር ሰማይንና ምድርን ፈጠረ"))
        }
        assertEquals(60, search(many, "እግዚአብሔር").size)
    }

    @Test
    fun `the homophone fold still decides what matches`() {
        val hits = search(listOf(luke10), "ማርታ")
        assertEquals(1, hits.size)
        assertTrue(hits.single().snippetMatchStart >= 0)
    }
}
