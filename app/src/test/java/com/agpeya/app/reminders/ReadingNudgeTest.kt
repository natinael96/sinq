package com.agpeya.app.reminders

import com.agpeya.app.data.ReadingPlanRepository
import com.agpeya.app.model.ActivePlan
import com.agpeya.app.model.PlanDay
import com.agpeya.app.model.PlanReading
import com.agpeya.app.model.ReadingPlan
import com.agpeya.app.model.ReadingPlanContent
import com.agpeya.app.model.ReadingPlanState
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class ReadingNudgeTest {
    private val today = LocalDate.of(2026, 9, 16)
    private val passages = listOf(PlanReading("genesis", 1, 2), PlanReading("matthew", 1, 1))
    private val plan = ReadingPlan(id = "year", days = 2, readings = listOf(
        PlanDay(1, passages), PlanDay(2, listOf(PlanReading("genesis", 3, 3))),
    ))
    private val content = ReadingPlanContent(plans = listOf(plan))
    private val state = ReadingPlanState(active = listOf(ActivePlan("year", today.toString())))

    @Test fun `no plan sends nothing`() {
        assertTrue(ReadingPlanRepository.unreadToday(content, ReadingPlanState(), today).isEmpty())
        assertFalse(shouldSendReadingReminder(false, false))
    }

    @Test fun `partial reading today still needs the remaining passages`() {
        val partial = state.copy(lastReadOn = today.toString(), read = mapOf("year" to setOf("genesis:1", "genesis:2")))
        val unread = ReadingPlanRepository.unreadToday(content, partial, today)
        assertEquals(listOf(PlanDay(1, listOf(passages[1]))), unread)
        assertTrue(shouldSendReadingReminder(unread.isNotEmpty(), false))
    }

    @Test fun `a partially read chapter range is unfinished`() {
        val partial = state.copy(read = mapOf("year" to setOf("genesis:1", "matthew:1")))
        assertEquals(listOf(PlanDay(1, listOf(passages[0]))), ReadingPlanRepository.unreadToday(content, partial, today))
    }

    @Test fun `finishing every passage silences later slots`() {
        val done = state.copy(read = mapOf("year" to setOf("genesis:1", "genesis:2", "matthew:1")))
        assertTrue(ReadingPlanRepository.unreadToday(content, done, today).isEmpty())
        // Completion is chapter-based, even if lastReadOn is absent in an old backup.
        assertEquals("", done.lastReadOn)
    }

    @Test fun `one completed plan does not silence another plan`() {
        val second = ReadingPlan(id = "psalter", days = 1, readings = listOf(PlanDay(1, listOf(PlanReading("psalms", 1, 1)))))
        val both = state.copy(
            active = state.active + ActivePlan("psalter", today.toString()),
            lastReadOn = today.toString(),
            read = mapOf("year" to setOf("genesis:1", "genesis:2", "matthew:1")),
        )
        assertEquals(second.readings, ReadingPlanRepository.unreadToday(ReadingPlanContent(plans = listOf(plan, second)), both, today))
    }

    @Test fun `tomorrow uses its own passages after today is finished`() {
        val done = state.copy(read = mapOf("year" to setOf("genesis:1", "genesis:2", "matthew:1")))
        assertEquals(listOf(plan.readings[1]), ReadingPlanRepository.unreadToday(content, done, today.plusDays(1)))
    }

    @Test fun `quiet hours suppress a slot without disabling future reminders`() {
        assertFalse(shouldSendReadingReminder(true, true))
        assertTrue(shouldSendReadingReminder(true, false))
    }

    @Test fun `old completed plan and unknown plan do not send empty notifications`() {
        val done = state.copy(read = mapOf("year" to setOf("genesis:1", "genesis:2", "matthew:1", "genesis:3")))
        assertTrue(ReadingPlanRepository.unreadToday(content, done, today.plusDays(30)).isEmpty())
        assertTrue(ReadingPlanRepository.unreadToday(content, state.copy(active = listOf(ActivePlan("missing", today.toString()))), today).isEmpty())
    }

    @Test fun `automatic schedule moves through morning afternoon night and tomorrow`() {
        val cases = listOf(
            "2026-09-16T00:00:00+03:00" to "2026-09-16T06:30:00+03:00",
            "2026-09-16T06:29:59+03:00" to "2026-09-16T06:30:00+03:00",
            "2026-09-16T06:30:00+03:00" to "2026-09-16T14:00:00+03:00",
            "2026-09-16T14:00:00+03:00" to "2026-09-16T20:00:00+03:00",
            "2026-09-16T20:00:00+03:00" to "2026-09-17T06:30:00+03:00",
            "2026-12-31T23:59:00+03:00" to "2027-01-01T06:30:00+03:00",
        )
        cases.forEach { (now, expected) ->
            assertEquals(now, ZonedDateTime.parse(expected), ReadingReminderScheduler.nextReminder(ZonedDateTime.parse(now)))
        }
    }

    @Test fun `late delivery skips elapsed slots rather than replaying them`() {
        val now = ZonedDateTime.parse("2026-09-16T15:25:00+03:00")
        assertEquals(ZonedDateTime.parse("2026-09-16T20:00:00+03:00"), ReadingReminderScheduler.nextReminder(now))
    }

    @Test fun `schedule follows local time across daylight saving changes`() {
        listOf(
            "2026-03-07T21:00:00-05:00[America/New_York]" to "2026-03-08T06:30:00-04:00[America/New_York]",
            "2026-10-31T21:00:00-04:00[America/New_York]" to "2026-11-01T06:30:00-05:00[America/New_York]",
        ).forEach { (now, expected) ->
            assertEquals(ZonedDateTime.parse(expected), ReadingReminderScheduler.nextReminder(ZonedDateTime.parse(now)))
        }
    }
}
