package com.agpeya.app.ui.common

import com.agpeya.app.data.BahreHasab
import com.agpeya.app.ui.strings.AmharicStrings
import com.agpeya.app.ui.strings.EnglishStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Guards the paired Ethiopian/Gregorian display and the liturgical season label. */
class DateDisplayTest {

    /** 2026-08-10 is ነሐሴ 4, 2018 EC. */
    private val day = LocalDate.of(2026, 8, 10)

    @Test
    fun `english pairs the church date with a spelled-out Gregorian date`() {
        val text = formatEthiopianWithGregorian(day, EnglishStrings)
        assertTrue(text, text.contains("Aug 10, 2026"))
        assertTrue("church date leads", text.indexOf("·") > 0)
    }

    @Test
    fun `amharic falls back to a numeric Gregorian date`() {
        val text = formatEthiopianWithGregorian(day, AmharicStrings)
        assertTrue(text, text.contains("10/08/2026"))
    }

    @Test
    fun `both languages name every season the calculator can emit`() {
        // Any key movableSeasonOn returns must have a label, or the UI shows nothing.
        val nineveh = BahreHasab.nineveh(2018)
        val keys = (0..130)
            .mapNotNull { BahreHasab.movableSeasonOn(nineveh.plusDays(it.toLong()))?.season }
            .toSet()
        assertTrue("expected some seasons", keys.isNotEmpty())
        for (k in keys) {
            assertNotNull("Amharic label missing for '$k'", AmharicStrings.seasonName(k))
            assertNotNull("English label missing for '$k'", EnglishStrings.seasonName(k))
        }
    }

    @Test
    fun `a day inside Great Lent reports the season with its week`() {
        val lentDay = BahreHasab.greatLentStart(2018).plusDays(8)
        val label = liturgicalSeasonLabel(lentDay, EnglishStrings)
        assertNotNull(label)
        assertTrue(label!!, label.contains("Great Lent") && label.contains("week"))
    }

    @Test
    fun `an ordinary day reports no season`() {
        // Meskerem 10 sits well outside every movable window.
        assertNull(liturgicalSeasonLabel(EthiopianDate(2018, 1, 10).toGregorian(), EnglishStrings))
    }

    @Test
    fun `the short form stays short`() {
        assertEquals("ነሐሴ 4", formatEthiopianShort(day, AmharicStrings))
    }
}
