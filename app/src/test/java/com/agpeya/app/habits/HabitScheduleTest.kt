package com.agpeya.app.habits

import com.agpeya.app.model.HabitSchedule
import com.agpeya.app.ui.common.EthiopianDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitScheduleTest {

    // A known Monday, and the Sunday just before it.
    private val monday: LocalDate = LocalDate.of(2026, 8, 10)
    private val sunday: LocalDate = monday.minusDays(1)

    @Test
    fun `weekly is due only on chosen days`() {
        val s = HabitSchedule(kind = HabitSchedule.Kind.WEEKLY, days = setOf(7))
        assertTrue(s.isDueOn(sunday))
        assertFalse(s.isDueOn(monday))
        assertEquals(sunday, s.nextDueOnOrAfter(monday.minusDays(3)))
    }

    @Test
    fun `weekly with no days never fires`() {
        val s = HabitSchedule(kind = HabitSchedule.Kind.WEEKLY, days = emptySet())
        assertNull(s.nextDueOnOrAfter(monday))
    }

    @Test
    fun `every other day alternates from its anchor`() {
        val s = HabitSchedule(kind = HabitSchedule.Kind.EVERY_OTHER_DAY, anchor = monday.toString())
        assertTrue(s.isDueOn(monday))
        assertFalse(s.isDueOn(monday.plusDays(1)))
        assertTrue(s.isDueOn(monday.plusDays(2)))
        // Parity also holds walking backwards past the anchor.
        assertTrue(s.isDueOn(monday.minusDays(2)))
        assertFalse(s.isDueOn(monday.minusDays(1)))
    }

    @Test
    fun `monthly follows the Ethiopian month day`() {
        val s = HabitSchedule(kind = HabitSchedule.Kind.MONTHLY, monthDay = 21)
        val due = EthiopianDate(2018, 11, 21).toGregorian()
        assertTrue(s.isDueOn(due))
        assertFalse(s.isDueOn(due.plusDays(1)))
        // From the day after, the next due day is day 21 of the next month.
        val next = s.nextDueOnOrAfter(due.plusDays(1))!!
        assertEquals(21, EthiopianDate.from(next).day)
        assertEquals(EthiopianDate(2018, 12, 21).toGregorian(), next)
    }

    @Test
    fun `monthly day skips Pagume and lands in the next month`() {
        val s = HabitSchedule(kind = HabitSchedule.Kind.MONTHLY, monthDay = 30)
        // From day 1 of ጳጉሜ (month 13, max 6 days) the next day-30 is መስከረም 30.
        val pagume1 = EthiopianDate(2017, 13, 1).toGregorian()
        val next = s.nextDueOnOrAfter(pagume1)!!
        val eth = EthiopianDate.from(next)
        assertEquals(1, eth.month)
        assertEquals(30, eth.day)
    }
}
