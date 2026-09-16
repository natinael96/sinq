package com.agpeya.app.widget

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalTime

class PrayerClockTest {
    @Test fun `every hour changes exactly at the website boundary`() {
        PrayerClock.hours.forEach { hour ->
            assertEquals(hour, PrayerClock.current(LocalTime.of(hour.hour, 0)))
            assertNotEquals(hour, PrayerClock.current(LocalTime.of(hour.hour, 0).minusMinutes(1)))
        }
    }
    @Test fun `midnight lasts until six and resets the elapsed arc`() {
        assertEquals("midnight", PrayerClock.current(LocalTime.of(5, 59)).id)
        assertEquals("compline", PrayerClock.current(LocalTime.of(23, 59)).id)
        assertEquals(0f, PrayerClock.fraction(LocalTime.MIDNIGHT), 0f)
        assertEquals(0.5f, PrayerClock.fraction(LocalTime.NOON), 0f)
        assertTrue(PrayerClock.fraction(LocalTime.of(23, 59)) < 1f)
    }
    @Test fun `all seven destinations have unique reader ids and numerals`() {
        assertEquals(7, PrayerClock.hours.map { it.id }.toSet().size)
        assertEquals(7, PrayerClock.hours.map { it.number }.toSet().size)
    }
}
