package com.agpeya.app.reminders

import com.agpeya.app.model.HabitSchedule
import com.agpeya.app.model.Penance
import com.agpeya.app.model.PenanceKind
import com.agpeya.app.model.PenanceProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDateTime

/** A ቀኖና rides the shared intention scheduler; this locks how it is timed. */
class PenanceReminderTest {

    private fun penance(minute: Int = 6 * 60) = Penance(
        id = "k1",
        kind = PenanceKind.PROSTRATIONS,
        quota = 40,
        // The screen's default: every day until it is finished.
        schedule = HabitSchedule(kind = HabitSchedule.Kind.WEEKLY, days = (1..7).toSet()),
        minute = minute,
    )

    @Test
    fun `a daily penance due later today fires today`() {
        val now = LocalDateTime.of(2026, 9, 3, 5, 0)
        val at = SpecialHabitReminderScheduler.nextOccurrence(penance(), now)
        assertNotNull(at)
        assertEquals(now.toLocalDate(), at!!.toLocalDate())
        assertEquals(6, at.hour)
    }

    @Test
    fun `a daily penance whose time has passed fires tomorrow`() {
        val now = LocalDateTime.of(2026, 9, 3, 7, 0)
        val at = SpecialHabitReminderScheduler.nextOccurrence(penance(), now)
        assertEquals(now.toLocalDate().plusDays(1), at!!.toLocalDate())
    }

    @Test
    fun `a settled penance no longer asks`() {
        val settled = penance().copy(
            progress = listOf(PenanceProgress(id = "p1", date = "2026-09-01", amount = 40)),
        )
        // The scheduler skips entries whose remindsStill is false; the receiver
        // checks the same flag before posting. This is that flag.
        assertFalse(settled.remindsStill)
    }
}
