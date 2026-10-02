package com.agpeya.app.data

import com.agpeya.app.model.DailyQuote
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Tests for the bundled Desert Fathers daily quotes dataset and multi-year rotation invariants.
 */
class DailyQuoteRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val file: File? =
        listOf("src/main/assets/content/daily_quotes.json", "app/src/main/assets/content/daily_quotes.json")
            .map(::File).firstOrNull { it.isFile }

    @Test
    fun `all 844 daily quotes decode cleanly with valid authors and texts`() {
        assertNotNull("daily_quotes.json must exist in assets", file)
        val raw = file!!.readText()
        val quotes: List<DailyQuote> = json.decodeFromString(raw)

        assertEquals("Must contain all 844 Desert Fathers quotes", 844, quotes.size)

        val seenDays = mutableSetOf<Int>()
        quotes.forEachIndexed { index, quote ->
            val day = index + 1
            assertEquals("Day number should be 1-indexed sequential", day, quote.day)
            assertTrue("Day $day must not be duplicated", seenDays.add(quote.day))
            assertTrue("Day $day must have non-blank quote text", quote.quote.isNotBlank())
            assertTrue("Day $day must have non-blank Amharic author", quote.authorAm.isNotBlank())
            assertTrue("Day $day must have non-blank English author", quote.authorEn.isNotBlank())
            assertTrue("Day $day must have non-blank Ge'ez numeral", quote.dayGeez.isNotBlank())
        }
    }

    @Test
    fun `multi-year rotation provides distinct quotes across consecutive years without repeating`() {
        val totalQuotes = 844

        val y2026Jan1 = DailyQuoteRepository.quoteIndexFor(LocalDate.of(2026, 1, 1), totalQuotes)
        val y2026Jan2 = DailyQuoteRepository.quoteIndexFor(LocalDate.of(2026, 1, 2), totalQuotes)
        val y2027Jan1 = DailyQuoteRepository.quoteIndexFor(LocalDate.of(2027, 1, 1), totalQuotes)
        val y2028Jan1 = DailyQuoteRepository.quoteIndexFor(LocalDate.of(2028, 1, 1), totalQuotes)

        assertEquals(0, y2026Jan1)
        assertEquals(1, y2026Jan2)
        assertEquals(365, y2027Jan1)
        assertEquals(730, y2028Jan1)

        // Consecutive years NEVER display the same quote on the same day
        assertTrue(y2026Jan1 != y2027Jan1)
        assertTrue(y2027Jan1 != y2028Jan1)
        assertTrue(y2026Jan1 != y2028Jan1)

        // Daily progression is contiguous
        for (i in 0 until 1000) {
            val date1 = LocalDate.of(2026, 1, 1).plusDays(i.toLong())
            val date2 = date1.plusDays(1)
            val idx1 = DailyQuoteRepository.quoteIndexFor(date1, totalQuotes)
            val idx2 = DailyQuoteRepository.quoteIndexFor(date2, totalQuotes)
            assertEquals((idx1 + 1) % totalQuotes, idx2)
        }
    }
}
