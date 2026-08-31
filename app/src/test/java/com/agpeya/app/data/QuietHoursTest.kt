package com.agpeya.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the quiet-hours window, whose wrap-around case is easy to get wrong. */
class QuietHoursTest {

    private fun at(h: Int, m: Int = 0) = h * 60 + m

    @Test
    fun `a window that wraps midnight covers both sides of it`() {
        val q = QuietHours(enabled = true, startMinute = at(22), endMinute = at(6))
        assertTrue("22:00 is the first silent minute", q.covers(at(22)))
        assertTrue(q.covers(at(23, 30)))
        assertTrue("just after midnight is still inside", q.covers(at(0, 1)))
        assertTrue(q.covers(at(5, 59)))
        assertFalse("the end is exclusive", q.covers(at(6)))
        assertFalse(q.covers(at(12)))
        assertFalse(q.covers(at(21, 59)))
    }

    @Test
    fun `a daytime window does not wrap`() {
        val q = QuietHours(enabled = true, startMinute = at(9), endMinute = at(17))
        assertTrue(q.covers(at(12)))
        assertFalse(q.covers(at(8, 59)))
        assertFalse(q.covers(at(17)))
        assertFalse("midnight is outside a daytime window", q.covers(at(0)))
    }

    @Test
    fun `disabled covers nothing`() {
        val q = QuietHours(enabled = false, startMinute = at(22), endMinute = at(6))
        assertFalse(q.covers(at(23)))
        assertFalse(q.covers(at(2)))
    }

    @Test
    fun `the shipped quiet window swallows none of the shipped reminders`() {
        // Every reminder now falls silent inside the window, so the defaults
        // colliding would mean a fresh install where switching quiet hours on
        // quietly turns several reminders off. 06:00 in particular is the exact
        // boundary: the ግጻዌ nudge survives only because the end is exclusive.
        val q = QuietHours(enabled = true, startMinute = at(22), endMinute = at(6))
        val gitsawe = com.agpeya.app.reminders.GitsaweReminderScheduler.REMINDER_TIME
        assertFalse(
            "the morning ግጻዌ reminder sits on the boundary",
            q.covers(gitsawe.hour * 60 + gitsawe.minute),
        )
        assertFalse(
            "the nightly nudge",
            q.covers(SettingsRepository.DEFAULT_STREAK_REMINDER_MIN),
        )
        assertFalse("ምጽዋት", q.covers(SettingsRepository.DEFAULT_ALMS_REMINDER_MIN))
        assertFalse("አስራት", q.covers(SettingsRepository.DEFAULT_TITHE_REMINDER_MIN))
        assertFalse("ንስሐ", q.covers(SettingsRepository.DEFAULT_REPENTANCE_REMINDER_MIN))
    }

    @Test
    fun `a zero-length window silences nothing`() {
        // Otherwise start == end could be read as "all day", silencing everything.
        val q = QuietHours(enabled = true, startMinute = at(8), endMinute = at(8))
        assertFalse(q.covers(at(8)))
        assertFalse(q.covers(at(20)))
    }
}
