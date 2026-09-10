package com.agpeya.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The keys the ማኅሌት's movable feasts are appointed by, checked against the
 * computus they rest on and against one date the world agrees on: Orthodox
 * Easter of 2024, 5 May, which is ትንሣኤ of 2016 ዓ.ም.
 */
class MahletComputusTest {

    private val fasika2016 = LocalDate.of(2024, 5, 5)

    @Test
    fun `the computus places Fasika 2016 on 5 May 2024`() {
        assertEquals(fasika2016, BahreHasab.fasika(2016))
    }

    @Test
    fun `the Paschal keys fall on the days the books mean`() {
        assertTrue("fasika" in MahletComputus.on(fasika2016))
        assertTrue("kedamSiur" in MahletComputus.on(fasika2016.minusDays(1)))
        assertTrue("siklet" in MahletComputus.on(fasika2016.minusDays(2)))
        assertTrue("hosanna" in MahletComputus.on(fasika2016.minusDays(7)))
        assertTrue("dagmTinsae" in MahletComputus.on(fasika2016.plusDays(7)))
        assertTrue("erget" in MahletComputus.on(fasika2016.plusDays(39)))
        assertTrue("peraklitos" in MahletComputus.on(fasika2016.plusDays(49)))
        // Holy Week's prayer is for Monday through Thursday, not the days that
        // have an order of their own.
        for (d in 1L..4L) assertTrue("himamat" in MahletComputus.on(fasika2016.minusDays(7 - d)))
        assertTrue("himamat" !in MahletComputus.on(fasika2016.minusDays(2)))
        assertTrue("himamat" !in MahletComputus.on(fasika2016.minusDays(7)))
    }

    @Test
    fun `an ordinary day is appointed nothing`() {
        assertTrue(MahletComputus.on(fasika2016.plusDays(100)).isEmpty())
    }

    @Test
    fun `dateOf agrees with on for every Paschal key`() {
        for (key in listOf("hosanna", "siklet", "kedamSiur", "fasika", "dagmTinsae", "erget", "peraklitos")) {
            val d = MahletComputus.dateOf(key, 2016)
            assertTrue("$key has no date", d != null)
            assertTrue("$key: dateOf is not a day on() appoints", key in MahletComputus.on(d!!))
        }
    }

    /** ስብከት, ብርሃን, ኖላዊ are Sundays, and the ጽጌ weeks are Sundays, by construction. */
    @Test
    fun `the fixed-anchored keys are appointed only on Sundays`() {
        for (key in listOf("sibket", "birhan", "nolawi") + (1..6).map { "tsige$it" }) {
            val d = MahletComputus.dateOf(key, 2016)
            assertTrue("$key has no date", d != null)
            assertEquals("$key is not a Sunday", DayOfWeek.SUNDAY, d!!.dayOfWeek)
            assertTrue("$key: dateOf is not a day on() appoints", key in MahletComputus.on(d))
        }
    }

    /**
     * The first Sunday of ዘመነ ጽጌ is the one on or after መስከረም ፳፮, and two
     * years in seven it is ጥቅምት ፩ or ፪. A window that only looked at the
     * last five days of መስከረም returned null for those years, and the list
     * showed no date for the ጽጌ orders.
     */
    @Test
    fun `every year has a first Sunday of tsige`() {
        for (year in 2010..2040) {
            val first = MahletComputus.dateOf("tsige1", year)
            assertNotNull("no ጽጌ ፩ in $year", first)
            assertTrue("tsige1" in MahletComputus.on(first!!))
            assertNotNull("no ጽጌ ፭ in $year", MahletComputus.dateOf("tsige5", year))
        }
    }
}
