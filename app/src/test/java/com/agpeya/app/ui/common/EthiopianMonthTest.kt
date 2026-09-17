package com.agpeya.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Thirteen months, twelve of thirty days, and ጳጉሜን.
 *
 * The picker offers a grid of days, so it has to know how many there are: five
 * in ጳጉሜን, six in the year before a Gregorian leap year.
 */
class EthiopianMonthTest {

    @Test
    fun `the twelve months are all thirty days`() {
        (1..12).forEach { assertEquals(30, monthLength(2018, it)) }
    }

    @Test
    fun `ጳጉሜን is five days, and six before a leap year`() {
        assertEquals(5, monthLength(2018, 13))
        assertEquals(6, monthLength(2019, 13))   // 2019 % 4 == 3
        assertEquals(5, monthLength(2020, 13))
    }

    @Test
    fun `the sixth day of ጳጉሜን exists exactly when the picker offers it`() {
        // The calendar itself is the check: መስከረም ፩ of the next year is the day
        // after the last day of ጳጉሜን.
        (2015..2025).forEach { year ->
            val newYear = EthiopianDate(year + 1, 1, 1).toGregorian()
            val last = EthiopianDate(year, 13, monthLength(year, 13)).toGregorian()
            assertEquals("ጳጉሜን of $year", newYear.minusDays(1), last)
        }
    }

    @Test
    fun `a chosen Ethiopian day round-trips to the Gregorian one`() {
        val d = EthiopianDate(2018, 13, 2).toGregorian()
        assertEquals(EthiopianDate(2018, 13, 2), EthiopianDate.from(d))
        assertEquals(d, LocalDate.ofEpochDay(d.toEpochDay()))
    }
    @Test
    fun `Meskerem starts in its actual weekday column`() {
        assertEquals(LocalDate.of(2026, 9, 11), EthiopianDate(2019, 1, 1).toGregorian())
        assertEquals(listOf(null, null, null, null, 1, 2, 3), ethiopianMonthCells(2019, 1).take(7))
    }

    @Test
    fun `leap Pagume includes day six in the next week when needed`() {
        val cells = ethiopianMonthCells(2015, 13)
        assertEquals(listOf(null, null, 1, 2, 3, 4, 5, 6, null, null, null, null, null, null), cells)
        assertEquals((1..5).toList(), ethiopianMonthCells(2018, 13).filterNotNull())
    }

    @Test
    fun `month navigation wraps through the thirteenth month`() {
        assertEquals(2019 to 1, shiftedEthiopianMonth(2018, 13, 1))
        assertEquals(2018 to 13, shiftedEthiopianMonth(2019, 1, -1))
        assertEquals(2019 to 13, shiftedEthiopianMonth(2018, 13, 13))
    }

    @Test
    fun `every selectable day occupies the correct weekday across common and leap years`() {
        for (year in 2015..2025) for (month in 1..13) {
            val cells = ethiopianMonthCells(year, month)
            assertEquals(0, cells.size % 7)
            assertEquals((1..monthLength(year, month)).toList(), cells.filterNotNull())
            cells.forEachIndexed { index, day ->
                if (day != null) assertEquals(
                    EthiopianDate(year, month, day).toGregorian().dayOfWeek.value - 1, index % 7)
            }
        }
    }

}
