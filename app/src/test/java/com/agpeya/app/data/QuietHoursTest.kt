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
    fun `a zero-length window silences nothing`() {
        // Otherwise start == end could be read as "all day", silencing everything.
        val q = QuietHours(enabled = true, startMinute = at(8), endMinute = at(8))
        assertFalse(q.covers(at(8)))
        assertFalse(q.covers(at(20)))
    }
}
