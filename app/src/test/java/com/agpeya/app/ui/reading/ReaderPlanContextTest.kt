package com.agpeya.app.ui.reading

import com.agpeya.app.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ReaderPlanContextTest {
    private val today = LocalDate.of(2026, 9, 17)
    private val plan = ReadingPlan(id = "year", days = 3, readings = (1..3).map {
        PlanDay(it, listOf(PlanReading("genesis", it, it)))
    })
    private val content = ReadingPlanContent(plans = listOf(plan, plan.copy(id = "other")))
    private val state = ReadingPlanState(active = listOf(
        ActivePlan("year", "2026-09-15"), ActivePlan("other", "2026-09-16"),
    ))

    @Test fun `historical assignment wins over today and other active plans`() {
        val result = readerPlanDays(content, state, today, "year", 1)
        assertEquals(listOf("year" to 1), result.map { it.first.id to it.second.d })
    }
    @Test fun `an untouched plan offers its first day, not the calendar's`() {
        // "year" started two days ago and "other" one, so the calendar says day
        // 3 and day 2. Nothing has been read, so day 1 is what is owed and day 1
        // is what the reader is handed — the plan waits rather than walking on.
        val result = readerPlanDays(content, state, today)
        assertEquals(listOf("year" to 1, "other" to 1), result.map { it.first.id to it.second.d })
    }

    @Test fun `a reader who is up to date gets the calendar day`() {
        val caughtUp = state.copy(read = mapOf(
            "year" to setOf("genesis:1", "genesis:2"),
            "other" to setOf("genesis:1"),
        ))
        val result = readerPlanDays(content, caughtUp, today)
        assertEquals(listOf("year" to 3, "other" to 2), result.map { it.first.id to it.second.d })
    }

    @Test fun `a gap holds the reader on the day that was missed`() {
        // Day 1 read, day 2 skipped, day 3 read ahead. Day 2 is still owed.
        val skipped = state.copy(read = mapOf("year" to setOf("genesis:1", "genesis:3")))
        val result = readerPlanDays(content, skipped, today, "year", 0)
            .ifEmpty { readerPlanDays(content, skipped, today).filter { it.first.id == "year" } }
        assertEquals(listOf("year" to 2), result.map { it.first.id to it.second.d })
    }
    @Test fun `invalid or stopped assignment never becomes today's assignment`() {
        assertTrue(readerPlanDays(content, state, today, "stopped", 1).isEmpty())
        assertTrue(readerPlanDays(content, state, today, "year", 999).isEmpty())
    }
    @Test fun `explicit chapter links preserve plan day and encode plan ids`() {
        assertEquals("scripture/genesis/1?plan=year+one&day=2", planReadingRoute("genesis", 1, "year one", 2))
        assertEquals("scripture/genesis/1", planReadingRoute("genesis", 1))
        assertEquals("psalter?section=4", planReadingRoute("psalms", 5, "year", 2))
    }
}
