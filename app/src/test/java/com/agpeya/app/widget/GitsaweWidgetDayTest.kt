package com.agpeya.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The evening rollover, which is the whole of the widget that can be reasoned
 * about without a home screen.
 */
class GitsaweWidgetDayTest {

    private val mon = LocalDate.of(2026, 9, 21)
    private val tue = LocalDate.of(2026, 9, 22)
    private val evening = 20 * 60

    private fun at(day: LocalDate, hour: Int, minute: Int = 0) =
        LocalDateTime.of(day, java.time.LocalTime.of(hour, minute))

    @Test
    fun `before the hour the card is today`() {
        assertEquals(mon, widgetDayFor(at(mon, 6), evening))
        assertEquals(mon, widgetDayFor(at(mon, 19, 59), evening))
    }

    @Test
    fun `on the hour the card turns to tomorrow`() {
        assertEquals(tue, widgetDayFor(at(mon, 20), evening))
        assertEquals(tue, widgetDayFor(at(mon, 23, 59), evening))
    }

    @Test
    fun `midnight does not move the card`() {
        // The one that is easy to get wrong. At 23:00 Monday the card reads
        // Tuesday because it rolled forward; at 00:30 Tuesday it must still
        // read Tuesday, now as today. The reader sees nothing change, which is
        // the point — the date on the card is continuous across midnight.
        assertEquals(tue, widgetDayFor(at(mon, 23), evening))
        assertEquals(tue, widgetDayFor(at(tue, 0, 30), evening))
    }

    @Test
    fun `switched off, the card follows the calendar`() {
        assertEquals(mon, widgetDayFor(at(mon, 6), null))
        assertEquals(mon, widgetDayFor(at(mon, 23, 59), null))
    }

    @Test
    fun `the next refresh is the rollover, never midnight`() {
        // Waking at midnight would redraw an identical card, so the alarm is
        // armed for the only moment the card actually changes.
        assertEquals(at(mon, 20), nextWidgetRefresh(at(mon, 6), evening))
        assertEquals(at(tue, 20), nextWidgetRefresh(at(mon, 20), evening))
        assertEquals(at(tue, 20), nextWidgetRefresh(at(mon, 23, 59), evening))
        assertEquals(at(tue, 20), nextWidgetRefresh(at(tue, 0, 30), evening))
    }

    @Test
    fun `switched off, the refresh is just past midnight`() {
        assertEquals(at(tue, 0, 1), nextWidgetRefresh(at(mon, 15), null))
    }

    @Test
    fun `every refresh is in the future`() {
        // A trigger time in the past fires immediately and re-arms in a loop.
        for (hour in 0..23) for (minute in listOf(0, 1, 30, 59)) {
            val now = at(mon, hour, minute)
            listOf(null, 0, evening, 23 * 60 + 59).forEach { rollover ->
                val next = nextWidgetRefresh(now, rollover)
                assert(next.isAfter(now)) { "rollover=$rollover at $now gave $next" }
            }
        }
    }

    @Test
    fun `a rollover of midnight keeps the card a day ahead`() {
        // Strange to ask for and legitimate: it is what preparing a day early
        // looks like. It must at least be consistent.
        assertEquals(tue, widgetDayFor(at(mon, 0), 0))
        assertEquals(tue, widgetDayFor(at(mon, 12), 0))
        assertEquals(tue, widgetDayFor(at(mon, 23, 59), 0))
    }
}
