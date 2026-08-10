package com.agpeya.app.data

import com.agpeya.app.ui.common.EthiopianDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Guards አጽዋማት: the fasts land where the church calendar puts them. */
class FastingCalendarTest {

    /** 2018 EC — Fasika falls on 2026-04-12 (verified against the computus). */
    private val year = 2018

    @Test
    fun `nineveh is a Monday three days long`() {
        val fast = FastingCalendar.fastsOf(year).first { it.key == "nineveh" }
        assertEquals(java.time.DayOfWeek.MONDAY, fast.start.dayOfWeek)
        assertEquals(3, fast.days)
    }

    @Test
    fun `great lent runs 55 days and ends the day before Fasika`() {
        val fasika = BahreHasab.fasika(year)
        val lent = FastingCalendar.fastsOf(year).first { it.key == "abiy" }
        assertEquals(fasika.minusDays(1), lent.end)
        // ዐቢይ ጾም is the 55-day fast (8 weeks less the Nineveh gap).
        assertEquals(55, lent.days)
    }

    @Test
    fun `filseta is the sixteen days of Nehase`() {
        val f = FastingCalendar.fastsOf(year).first { it.key == "filseta" }
        assertEquals(16, f.days)
        assertEquals(12, EthiopianDate.from(f.start).month)
        assertEquals(1, EthiopianDate.from(f.start).day)
    }

    @Test
    fun `advent fast runs Hidar 15 to the eve of Christmas`() {
        val f = FastingCalendar.fastsOf(year).first { it.key == "nebiyat" }
        val start = EthiopianDate.from(f.start)
        val end = EthiopianDate.from(f.end)
        assertEquals(3, start.month)   // ኅዳር
        assertEquals(15, start.day)
        assertEquals(4, end.month)     // ታኅሣሥ
        assertEquals(28, end.day)      // ልደት falls on ፳፱
        // Those endpoints span 44 inclusive days. The fast is often quoted as
        // "43 days", which counts one endpoint exclusively — the dates, not the
        // headline number, are what this asserts.
        assertEquals(44, f.days)
    }

    @Test
    fun `weekly fast skips the paschal season but holds otherwise`() {
        val fasika = BahreHasab.fasika(year)
        // A Wednesday inside the ኃምሳ is not a fasting day.
        val inSeason = generateSequence(fasika) { it.plusDays(1) }
            .first { it.dayOfWeek == java.time.DayOfWeek.WEDNESDAY }
        assertFalse(FastingCalendar.isWeeklyFastDay(inSeason))
        // A Wednesday well after Pentecost is.
        val after = BahreHasab.pentecost(year).plusDays(30)
            .let { d -> generateSequence(d) { it.plusDays(1) }.first { it.dayOfWeek == java.time.DayOfWeek.WEDNESDAY } }
        assertTrue(FastingCalendar.isWeeklyFastDay(after))
        assertFalse("Sunday is never a fast day", FastingCalendar.isWeeklyFastDay(fasika))
    }

    @Test
    fun `fasts do not overlap and are ordered`() {
        val fasts = FastingCalendar.fastsOf(year)
        assertTrue(fasts.isNotEmpty())
        for (i in 1 until fasts.size) {
            assertTrue(
                "${fasts[i - 1].key} overlaps ${fasts[i].key}",
                !fasts[i].start.isBefore(fasts[i - 1].start),
            )
        }
    }

    @Test
    fun `fastOn finds a day inside Great Lent`() {
        val lent = FastingCalendar.fastsOf(year).first { it.key == "abiy" }
        val mid = lent.start.plusDays(10)
        assertEquals("abiy", FastingCalendar.fastOn(mid)?.key)
        // A day in the paschal season belongs to no fast.
        assertEquals(null, FastingCalendar.fastOn(BahreHasab.fasika(year).plusDays(5))?.key)
    }
}
