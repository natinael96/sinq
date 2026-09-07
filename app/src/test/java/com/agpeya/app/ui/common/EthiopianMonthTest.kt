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
}
