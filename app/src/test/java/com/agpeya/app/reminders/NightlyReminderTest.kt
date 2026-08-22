package com.agpeya.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
