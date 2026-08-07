package com.agpeya.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Verifies the Ethiopian computus against independently-known dates: Fasika must
 * equal the recorded Orthodox Easter for each year, and the classical Bahre
 * Hasab numbers must match a worked example (2015 E.C.).
 */
class BahreHasabTest {

    @Test
    fun `classical values match the 2015 EC worked example`() {
        assertEquals(7515, BahreHasab.ameteAlem(2015))
        assertEquals(9, BahreHasab.wenber(2015))
        assertEquals(9, BahreHasab.abekte(2015))
        assertEquals(21, BahreHasab.metqi(2015))
        assertEquals(3, BahreHasab.evangelist(2015))   // Luke
    }

    @Test
    fun `abekte plus metqi is a multiple of 30`() {
        // (w*11 + w*19) mod 30 = (w*30) mod 30 = 0, so the sum is 30 — or 0 when
        // Wenber is 0 (e.g. 2006 / 2025 E.C.), where both terms vanish.
        for (e in 2000..2030) {
            assertEquals("year $e", 0, (BahreHasab.abekte(e) + BahreHasab.metqi(e)) % 30)
        }
    }

    @Test
    fun `Fasika equals the known Orthodox Easter dates`() {
        // Ethiopian year E → Gregorian Easter (E + 8), from published Orthodox Easter.
        assertEquals(LocalDate.of(2023, 4, 16), BahreHasab.fasika(2015))
        assertEquals(LocalDate.of(2024, 5, 5), BahreHasab.fasika(2016))
        assertEquals(LocalDate.of(2025, 4, 20), BahreHasab.fasika(2017))
        assertEquals(LocalDate.of(2026, 4, 12), BahreHasab.fasika(2018))
    }

    @Test
    fun `Fasika is a Sunday and Nineveh the Monday 69 days before`() {
        for (e in 2010..2025) {
            val fasika = BahreHasab.fasika(e)
            val nineveh = BahreHasab.nineveh(e)
            assertEquals("Fasika $e is Sunday", DayOfWeek.SUNDAY, fasika.dayOfWeek)
            assertEquals("Nineveh $e is Monday", DayOfWeek.MONDAY, nineveh.dayOfWeek)
            assertEquals(fasika, nineveh.plusDays(69))
        }
    }

    @Test
    fun `movable seasons resolve to the right window`() {
        val e = 2018
        assertEquals(
            BahreHasab.SeasonWindow("neneweTsom", 1),
            BahreHasab.movableSeasonOn(BahreHasab.nineveh(e)),
        )
        // Hosanna (Palm Sunday) is the 8th Sunday of Great Lent.
        assertEquals(
            BahreHasab.SeasonWindow("abiyTsom", 8),
            BahreHasab.movableSeasonOn(BahreHasab.hosanna(e)),
        )
        // First Lent Sunday, ዘወረደ, 13 days after Nineveh.
        assertEquals(
            BahreHasab.SeasonWindow("abiyTsom", 1),
            BahreHasab.movableSeasonOn(BahreHasab.nineveh(e).plusDays(13)),
        )
        // Fasika opens the Resurrection season.
        assertEquals(
            BahreHasab.SeasonWindow("tnsae", 1),
            BahreHasab.movableSeasonOn(BahreHasab.fasika(e)),
        )
        assertEquals(
            BahreHasab.SeasonWindow("erget", null),
            BahreHasab.movableSeasonOn(BahreHasab.ascension(e)),
        )
        // A high-summer date is in no movable season.
        assertEquals(null, BahreHasab.movableSeasonOn(LocalDate.of(2026, 8, 1)))
    }
}
