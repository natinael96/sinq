package com.agpeya.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.io.File
import com.agpeya.app.model.BahreHasabReference
import kotlinx.serialization.json.Json
import com.agpeya.app.ui.common.EthiopianDate

/**
 * Verifies the Ethiopian computus against independently-known dates: Fasika must
 * equal the recorded Orthodox Easter for each year, and the classical Bahre
 * Hasab numbers must match a worked example (2015 E.C.).
 */
class BahreHasabTest {

    private fun printedDate(value: String): Pair<Int, Int> {
        val month = mapOf("ጥር" to 5, "የካ" to 6, "መጋ" to 7, "ሚያ" to 8, "ግን" to 9, "ሰኔ" to 10)
        val parts = value.split(" ")
        val digits = mapOf('፩' to 1, '፪' to 2, '፫' to 3, '፬' to 4, '፭' to 5, '፮' to 6, '፯' to 7, '፰' to 8, '፱' to 9, '፲' to 10, '፳' to 20, '፴' to 30)
        return (month[parts[0]] ?: error("unknown printed month $value")) to
            parts[1].sumOf { digits[it] ?: error("unknown printed day $value") }
    }

    private fun ethiopianMonthDay(date: LocalDate): Pair<Int, Int> =
        EthiopianDate.from(date).let { it.month to it.day }

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
        assertEquals(
            BahreHasab.SeasonWindow("tnsae", 12),
            BahreHasab.movableSeasonOn(BahreHasab.fasika(e).plusDays(77)),
        )
        // A high-summer date is in no movable season.
        assertEquals(null, BahreHasab.movableSeasonOn(LocalDate.of(2026, 8, 1)))
    }

    @Test
    fun `master Part 2 weekdays resolve without collapsing adjacent days`() {
        val nineveh = BahreHasab.nineveh(2018)
        assertEquals(
            BahreHasab.WeekdayWindow("neneweTsom", 1, 1),
            BahreHasab.movableWeekdayOn(nineveh),
        )
        assertEquals(
            BahreHasab.WeekdayWindow("neneweTsom", 1, 3),
            BahreHasab.movableWeekdayOn(nineveh.plusDays(2)),
        )
        assertEquals(
            BahreHasab.WeekdayWindow("heraclius", 1, 1),
            BahreHasab.movableWeekdayOn(nineveh.plusDays(7)),
        )
        assertEquals(
            BahreHasab.WeekdayWindow("abiyTsom", 1, 1),
            BahreHasab.movableWeekdayOn(nineveh.plusDays(14)),
        )
        assertEquals(
            BahreHasab.WeekdayWindow("abiyTsom", 6, 6),
            BahreHasab.movableWeekdayOn(nineveh.plusDays(54)),
        )
        assertEquals(null, BahreHasab.movableWeekdayOn(nineveh.plusDays(13)))
    }

    @Test
    fun `Part 5 printed table validates computus dates for 2001 through 2015 EC`() {
        val dir = listOf("src/main/assets/content/gitsawe", "app/src/main/assets/content/gitsawe")
            .map(::File).first { it.isDirectory }
        val table = Json.decodeFromString(
            BahreHasabReference.serializer(),
            File(dir, "bahre-hasab-reference.json").readText(),
        )
        val differences = mutableListOf<String>()
        table.rows.forEachIndexed { index, row ->
            val year = 2001 + index
            val checks = listOf(
                6 to BahreHasab.nineveh(year),
                7 to BahreHasab.greatLentStart(year),
                8 to BahreHasab.debreZeit(year),
                9 to BahreHasab.hosanna(year),
                10 to BahreHasab.siklet(year),
                11 to BahreHasab.fasika(year),
                12 to BahreHasab.rikbeKahnat(year),
                13 to BahreHasab.ascension(year),
                14 to BahreHasab.pentecost(year),
                15 to BahreHasab.apostlesFast(year),
            )
            checks.forEach { (column, actual) ->
                val printed = printedDate(row.values[column])
                val computed = ethiopianMonthDay(actual)
                if (printed != computed) differences += "$year:${table.columns[column]}:$printed:$computed"
            }
        }
        // The sole divergence in 150 checked feast dates is printed in the
        // source itself: 2004 EC gives Nineveh as Tir 27 while its own Fasika
        // row and the standard 69-day offset resolve to Tir 28.
        assertEquals(listOf("2004:ጾመ ነነዌ:(5, 27):(5, 28)"), differences)
    }
}
