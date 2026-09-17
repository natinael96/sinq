package com.agpeya.app.data

import com.agpeya.app.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ReadingProgressAuditTest {
    private val today = LocalDate.of(2026, 9, 16)
    private val day = PlanDay(1, listOf(PlanReading("genesis", 1, 3)))

    @Test fun `one chapter cannot complete a three chapter assignment`() {
        val state = ReadingPlanRepository.toggledChapters(ReadingPlanState(), "annual", listOf("genesis:1"), today)
        assertFalse(ReadingPlanRepository.isRead(state, "annual", day))
        assertEquals(setOf("genesis:1"), state.readFor("annual"))
    }
    @Test fun `completion is independent for overlapping plans`() {
        val first = ReadingPlanRepository.toggledChapters(ReadingPlanState(), "annual", listOf("genesis:1", "genesis:2", "genesis:3"), today)
        assertTrue(ReadingPlanRepository.isRead(first, "annual", day))
        assertFalse(ReadingPlanRepository.isRead(first, "other", day))
    }
    @Test fun `undo makes assignment unfinished without erasing lifetime reading`() {
        val complete = ReadingPlanRepository.toggledChapters(ReadingPlanState(), "annual", listOf("genesis:1", "genesis:2", "genesis:3"), today)
        val undone = ReadingPlanRepository.toggledChapters(complete, "annual", listOf("genesis:2"), today)
        assertFalse(ReadingPlanRepository.isRead(undone, "annual", day))
        assertEquals(complete.readChapters, undone.readChapters)
    }
    @Test fun `restart resets only selected cycle and preserves lifetime map`() {
        val original = ReadingPlanState(
            active = listOf(ActivePlan("annual", "2025-09-01"), ActivePlan("psalter", "2026-09-01")),
            read = mapOf("annual" to setOf("genesis:1"), "psalter" to setOf("psalms:10")),
            readChapters = setOf("genesis:1", "psalms:10"),
            completedDays = mapOf("annual" to setOf(1)),
        )
        val restarted = ReadingPlanRepository.restarted(original, "annual", today)
        assertTrue(restarted.readFor("annual").isEmpty())
        assertEquals(original.readFor("psalter"), restarted.readFor("psalter"))
        assertEquals(original.readChapters, restarted.readChapters)
        assertFalse(restarted.completedDays.containsKey("annual"))
        assertEquals(today.toString(), restarted.plansKept.first { it.planId == "annual" }.startedOn)
    }
    @Test fun `automatic chapter completion is idempotent and does not finish the whole assignment`() {
        val first = ReadingPlanRepository.markedChapter(ReadingPlanState(), "genesis:1", listOf("annual"), today)
        val second = ReadingPlanRepository.markedChapter(first, "genesis:1", listOf("annual"), today)
        assertEquals(first, second)
        assertFalse(ReadingPlanRepository.isRead(second, "annual", day))
        assertEquals(setOf("genesis:1"), second.readChapters)
        assertEquals(today.toString(), second.lastReadOn)
    }
    @Test fun `automatic completion updates matching plans and preserves unrelated plans`() {
        val before = ReadingPlanState(read = mapOf("other" to setOf("psalms:1")))
        val after = ReadingPlanRepository.markedChapter(before, "genesis:1", listOf("annual", "short", "annual"), today)
        assertEquals(setOf("genesis:1"), after.readFor("annual"))
        assertEquals(setOf("genesis:1"), after.readFor("short"))
        assertEquals(before.readFor("other"), after.readFor("other"))
    }
    @Test fun `unplanned reading records lifetime progress without inventing a plan`() {
        val after = ReadingPlanRepository.markedChapter(ReadingPlanState(), "genesis:1", emptyList(), today)
        assertTrue(after.read.isEmpty())
        assertEquals(setOf("genesis:1"), after.readChapters)
    }
}
