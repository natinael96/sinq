package com.agpeya.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class BreathPrayerSchedulerTest {

    private val morningMinute = 6 * 60 + 30

    @Test
    fun `before morning starts at configured morning time`() {
        val window = breathPrayerWindow(
            now = LocalDateTime.of(2026, 8, 24, 5, 0),
            configuredMorningMinute = morningMinute,
            firedToday = false,
        )

        assertEquals(LocalDateTime.of(2026, 8, 24, 6, 30), window.lower)
        assertEquals(LocalDateTime.of(2026, 8, 24, 21, 0), window.upper)
    }

    @Test
    fun `during daytime starts safely after now`() {
        val window = breathPrayerWindow(
            now = LocalDateTime.of(2026, 8, 24, 12, 0),
            configuredMorningMinute = morningMinute,
            firedToday = false,
        )

        assertEquals(LocalDateTime.of(2026, 8, 24, 12, 15), window.lower)
        assertEquals(LocalDateTime.of(2026, 8, 24, 21, 0), window.upper)
    }

    @Test
    fun `after firing schedules tomorrow from morning`() {
        val window = breathPrayerWindow(
            now = LocalDateTime.of(2026, 8, 24, 10, 0),
            configuredMorningMinute = morningMinute,
            firedToday = true,
        )

        assertEquals(LocalDateTime.of(2026, 8, 25, 6, 30), window.lower)
        assertEquals(LocalDateTime.of(2026, 8, 25, 21, 0), window.upper)
    }

    @Test
    fun `too close to night schedules tomorrow`() {
        val window = breathPrayerWindow(
            now = LocalDateTime.of(2026, 8, 24, 20, 50),
            configuredMorningMinute = morningMinute,
            firedToday = false,
        )

        assertEquals(LocalDateTime.of(2026, 8, 25, 6, 30), window.lower)
        assertEquals(LocalDateTime.of(2026, 8, 25, 21, 0), window.upper)
    }

    @Test
    fun `morning configured after night uses safe default`() {
        val window = breathPrayerWindow(
            now = LocalDateTime.of(2026, 8, 24, 5, 0),
            configuredMorningMinute = 22 * 60,
            firedToday = false,
        )

        assertEquals(LocalDateTime.of(2026, 8, 24, 6, 0), window.lower)
        assertEquals(LocalDateTime.of(2026, 8, 24, 21, 0), window.upper)
    }
}
