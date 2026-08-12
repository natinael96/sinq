package com.agpeya.app.data

import com.agpeya.app.model.Hour
import com.agpeya.app.model.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

/** Guards prayer-hour stepping, progress counting, and the resume rule. */
class PrayerScheduleTest {

    private fun section(id: String) = Section(id = id, type = "psalm", title = id, verses = emptyList())
    private fun hour(id: String, vararg sectionIds: String) = Hour(
        id = id,
        orderIndex = 0,
        name = id,
        transliteration = id,
        timeHint = "",
        sections = sectionIds.map(::section),
    )

    private val hours = listOf(
        hour("morning", "a", "b", "c"),
        hour("terce", "d", "e"),
        hour("sext", "f"),
    )

    @Test
    fun `stepping follows the user's order, not the clock`() {
        assertEquals("morning", PrayerSchedule.previous(hours, "terce")?.id)
        assertEquals("sext", PrayerSchedule.next(hours, "terce")?.id)
    }

    @Test
    fun `the ends of the list have no neighbour`() {
        assertNull(PrayerSchedule.previous(hours, "morning"))
        assertNull(PrayerSchedule.next(hours, "sext"))
    }

    @Test
    fun `an unknown hour steps nowhere`() {
        assertNull(PrayerSchedule.previous(hours, "nope"))
        assertNull(PrayerSchedule.next(hours, "nope"))
    }

    @Test
    fun `the current hour is only reported when it is visible`() {
        assertEquals("morning", PrayerSchedule.currentHourId(hours, LocalTime.of(6, 0)))
        // 21:00 suggests compline, which this user has hidden.
        assertNull(PrayerSchedule.currentHourId(hours, LocalTime.of(21, 0)))
    }

    @Test
    fun `progress counts only sections that are actually in the hour`() {
        val ids = listOf("a", "b", "c")
        // "zz" is stale — from a section since hidden — and must not inflate the count.
        assertEquals(2 to 3, PrayerSchedule.progressOf(ids, setOf("a", "c", "zz")))
    }

    private fun progress(last: String?, done: Map<String, Set<String>>) =
        PrayerProgressRepository.DayProgress(date = "2026-08-10", done = done, lastHourId = last)

    private val idsOf: (String) -> List<String> =
        { id -> hours.find { it.id == id }?.sections?.map { it.id } ?: emptyList() }

    @Test
    fun `a part-read hour is resumable`() {
        assertEquals("morning", PrayerSchedule.resumable(progress("morning", mapOf("morning" to setOf("a"))), idsOf))
    }

    @Test
    fun `a finished hour is not resumable`() {
        assertNull(PrayerSchedule.resumable(progress("morning", mapOf("morning" to setOf("a", "b", "c"))), idsOf))
    }

    @Test
    fun `an untouched hour is not resumable`() {
        assertNull(PrayerSchedule.resumable(progress("morning", emptyMap()), idsOf))
        assertNull(PrayerSchedule.resumable(progress(null, emptyMap()), idsOf))
    }

    @Test
    fun `stale marks from hidden sections cannot make an hour look finished`() {
        // Three marks, but only one belongs to the hour — still unfinished.
        val done = mapOf("morning" to setOf("a", "old1", "old2"))
        assertEquals("morning", PrayerSchedule.resumable(progress("morning", done), idsOf))
    }
}
