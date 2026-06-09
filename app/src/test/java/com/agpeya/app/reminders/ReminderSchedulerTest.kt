package com.agpeya.app.reminders

import com.agpeya.app.model.ReminderEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class ReminderSchedulerTest {

    private fun entry(hour: Int, minute: Int = 0, days: Set<Int> = ReminderEntry.ALL_DAYS) =
        ReminderEntry(id = "e", hourId = "morning", hour = hour, minute = minute, days = days)

    // Wed 2026-06-10 05:00
    private val wedMorning = LocalDateTime.of(2026, 6, 10, 5, 0)

    @Test
    fun `today when time not yet passed`() {
        val next = ReminderScheduler.nextOccurrence(entry(6), wedMorning)
        assertEquals(LocalDateTime.of(2026, 6, 10, 6, 0), next)
    }

    @Test
    fun `tomorrow when time already passed`() {
        val next = ReminderScheduler.nextOccurrence(entry(6), wedMorning.withHour(7))
        assertEquals(LocalDateTime.of(2026, 6, 11, 6, 0), next)
    }

    @Test
    fun `exact now rolls to next day`() {
        val now = LocalDateTime.of(2026, 6, 10, 6, 0)
        val next = ReminderScheduler.nextOccurrence(entry(6), now)
        assertEquals(LocalDateTime.of(2026, 6, 11, 6, 0), next)
    }

    @Test
    fun `weekday set skips to wednesday and friday`() {
        // Wed=3, Fri=5 — classic fasting days
        val fasting = entry(6, days = setOf(3, 5))
        val thuEvening = LocalDateTime.of(2026, 6, 11, 20, 0)
        val next = ReminderScheduler.nextOccurrence(fasting, thuEvening)
        assertEquals(DayOfWeek.FRIDAY, next!!.dayOfWeek)
        assertEquals(LocalDateTime.of(2026, 6, 12, 6, 0), next)
    }

    @Test
    fun `week wraps from saturday to monday`() {
        val monOnly = entry(6, days = setOf(1))
        val satNoon = LocalDateTime.of(2026, 6, 13, 12, 0)
        val next = ReminderScheduler.nextOccurrence(monOnly, satNoon)
        assertEquals(LocalDateTime.of(2026, 6, 15, 6, 0), next)
    }

    @Test
    fun `same weekday after time passed waits a full week`() {
        val wedOnly = entry(6, days = setOf(3))
        val next = ReminderScheduler.nextOccurrence(wedOnly, wedMorning.withHour(9))
        assertEquals(LocalDateTime.of(2026, 6, 17, 6, 0), next)
    }

    @Test
    fun `empty day set yields nothing`() {
        assertNull(ReminderScheduler.nextOccurrence(entry(6, days = emptySet()), wedMorning))
    }

    @Test
    fun `midnight entry schedules at zero hour`() {
        val next = ReminderScheduler.nextOccurrence(entry(0, 0), wedMorning)
        assertEquals(LocalDateTime.of(2026, 6, 11, 0, 0), next)
    }
}
