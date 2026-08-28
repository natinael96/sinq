package com.agpeya.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GitsaweWidgetDayTest {
    private val day = LocalDate.of(2026, 8, 28)

    @Test fun `shows today before evening`() {
        assertEquals(day, gitsaweWidgetDate(LocalDateTime.of(2026, 8, 28, 18, 59)))
    }

    @Test fun `shows tomorrow from seven in the evening`() {
        assertEquals(day.plusDays(1), gitsaweWidgetDate(LocalDateTime.of(2026, 8, 28, 19, 0)))
    }
}
