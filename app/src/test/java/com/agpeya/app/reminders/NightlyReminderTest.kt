package com.agpeya.app.reminders

import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.model.HabitSchedule
import com.agpeya.app.model.HabitsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NightlyReminderTest {

    @Test
    fun `marking prayers never removes the nightly checklist`() {
        val pending = pendingNightlyHabitIds(
            setOf("hour_morning", "hour_vespers", "hour_compline"),
        )
        assertEquals(listOf("sinksar", "church", "prostrate"), pending)
    }

    @Test
    fun `only the matching checklist item is removed`() {
        assertEquals(
            listOf("sinksar", "prostrate"),
            pendingNightlyHabitIds(setOf("church", "hour_morning")),
        )
    }

    @Test
    fun `completed checklist remains safe and empty`() {
        assertTrue(pendingNightlyHabitIds(setOf("sinksar", "church", "prostrate")).isEmpty())
    }

    // ---- cadence ----

    // A Wednesday and a Thursday, so a Wednesday-and-Friday rule is due on one
    // and not the other.
    private val wednesday = LocalDate.of(2026, 9, 9)
    private val thursday = LocalDate.of(2026, 9, 10)

    private val wedAndFri = HabitsState(
        schedules = mapOf(
            "prostrate" to HabitSchedule(kind = HabitSchedule.Kind.WEEKLY, days = setOf(3, 5)),
        ),
    )

    @Test
    fun `a habit not asked for tonight is not outstanding tonight`() {
        assertEquals(
            listOf("sinksar", "church"),
            pendingNightlyHabitIds(emptySet(), wedAndFri, thursday),
        )
    }

    @Test
    fun `the same habit is outstanding on a day it is asked for`() {
        assertEquals(
            listOf("sinksar", "church", "prostrate"),
            pendingNightlyHabitIds(emptySet(), wedAndFri, wednesday),
        )
    }

    @Test
    fun `a habit with no cadence is asked for every day`() {
        assertEquals(
            listOf("sinksar", "church", "prostrate"),
            pendingNightlyHabitIds(emptySet(), HabitsState(), thursday),
        )
    }

    @Test
    fun `kept of due counts only what the day asked for`() {
        val done = wedAndFri.copy(records = mapOf(thursday.toString() to setOf("church")))
        // Thursday asks for ስንክሳር, ቤተ ክርስቲያን, የዕለት ንባብ and ዳዊት — not ስግደት.
        val (kept, due) = HabitsRepository.keptOfDue(done, thursday)
        assertEquals(1, kept)
        assertEquals(HabitsRepository.BUILT_IN_IDS.size - 1, due)
        assertFalse("prostrate" in HabitsRepository.dueHabitIds(done, thursday))
    }
}
