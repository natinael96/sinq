package com.agpeya.app.data

import com.agpeya.app.model.PlanDay
import com.agpeya.app.model.PlanReading
import com.agpeya.app.model.ReadingPlanState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The plan's day arithmetic. The thing being defended here is that falling
 * behind stays harmless: nothing resets, nothing is lost, and no count ever
 * reports a deficit.
 */
class ReadingPlanTest {

    private val start = LocalDate.of(2026, 9, 4)

    @Test
    fun `day one is the day it was started`() {
        assertEquals(1, ReadingPlanRepository.dayOn(start.toString(), start, 360))
        assertEquals(2, ReadingPlanRepository.dayOn(start.toString(), start.plusDays(1), 360))
        assertEquals(43, ReadingPlanRepository.dayOn(start.toString(), start.plusDays(42), 360))
    }

    @Test
    fun `a plan left running does not run past its end`() {
        assertEquals(360, ReadingPlanRepository.dayOn(start.toString(), start.plusDays(900), 360))
    }

    @Test
    fun `an unparseable start date falls back to day one rather than crashing`() {
        assertEquals(1, ReadingPlanRepository.dayOn("not-a-date", start, 360))
    }

    @Test
    fun `progress counts distinct days read, never a streak`() {
        // Days 1, 2 then a gap then 9: five days later, three days read.
        val st = ReadingPlanState(completedDays = mapOf("annual" to setOf(1, 2, 9)))
        assertEquals(3, ReadingPlanRepository.daysRead(st, "annual"))
    }

    @Test
    fun `a missed day changes nothing but that day`() {
        val before = ReadingPlanState(completedDays = mapOf("annual" to setOf(1, 2, 3)))
        // Day 4 is simply never added. Nothing resets.
        assertEquals(3, ReadingPlanRepository.daysRead(before, "annual"))
        val after = before.copy(completedDays = mapOf("annual" to setOf(1, 2, 3, 5)))
        assertEquals(4, ReadingPlanRepository.daysRead(after, "annual"))
    }

    @Test
    fun `oldest unread finds where catching up would start`() {
        val st = ReadingPlanState(completedDays = mapOf("annual" to setOf(1, 2, 4)))
        assertEquals(3, ReadingPlanRepository.oldestUnread(st, "annual", currentDay = 5))
    }

    @Test
    fun `nothing owed reports nothing`() {
        val st = ReadingPlanState(completedDays = mapOf("annual" to setOf(1, 2, 3)))
        assertNull(ReadingPlanRepository.oldestUnread(st, "annual", currentDay = 3))
    }

    @Test
    fun `an untouched plan owes its first day`() {
        assertEquals(1, ReadingPlanRepository.oldestUnread(ReadingPlanState(), "annual", 1))
    }

    // ── redistribute ─────────────────────────────────────────────────────────

    private fun days(n: Int) = (1..n).map { PlanDay(d = it, r = listOf(PlanReading("genesis", it, it))) }

    @Test
    fun `redistribute drops nothing`() {
        val before = days(10)
        val after = ReadingPlanRepository.redistribute(before, fromDay = 3, remainingDays = 4)
        val kept = after.flatMap { it.r }
        // Days 3..10 is eight readings; all eight survive, in order.
        assertEquals(8, kept.size)
        assertEquals((3..10).toList(), kept.map { it.c })
    }

    @Test
    fun `redistribute fits inside the days that remain`() {
        val after = ReadingPlanRepository.redistribute(days(10), fromDay = 3, remainingDays = 4)
        assertTrue("used ${after.size} of 4 days", after.size <= 4)
        assertEquals(3, after.first().d)
    }

    @Test
    fun `redistribute with one day left gathers everything into it`() {
        val after = ReadingPlanRepository.redistribute(days(10), fromDay = 8, remainingDays = 1)
        assertEquals(1, after.size)
        assertEquals(3, after.single().r.size)
    }

    @Test
    fun `redistribute with nothing left returns nothing`() {
        assertTrue(ReadingPlanRepository.redistribute(days(5), fromDay = 9, remainingDays = 3).isEmpty())
        assertTrue(ReadingPlanRepository.redistribute(days(5), fromDay = 1, remainingDays = 0).isEmpty())
    }

    @Test
    fun `merge unions rather than overwrites`() {
        val a = ReadingPlanState(completedDays = mapOf("annual" to setOf(1, 2)))
        val b = ReadingPlanState(completedDays = mapOf("annual" to setOf(2, 3)))
        val union = (a.readDays("annual") + b.readDays("annual"))
        assertEquals(setOf(1, 2, 3), union)
    }
}
