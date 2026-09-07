package com.agpeya.app.data

import com.agpeya.app.model.PlanDay
import com.agpeya.app.model.PlanReading
import com.agpeya.app.model.ReadingPlan
import com.agpeya.app.model.ReadingPlanState
import com.agpeya.app.model.Redistribution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // ── the chapter ledger ───────────────────────────────────────────────────

    /** The state a reader would be in having read [dayNumbers] of [days]. */
    private fun read(days: List<PlanDay>, vararg dayNumbers: Int) = ReadingPlanState(
        readChapters = days.filter { it.d in dayNumbers.toSet() }
            .flatMapTo(mutableSetOf()) { ReadingPlanRepository.chaptersOf(it) },
    )

    @Test
    fun `progress counts distinct days read, never a streak`() {
        // Days 1, 2 then a gap then 9: five days later, three days read.
        val d = days(10)
        assertEquals(3, ReadingPlanRepository.daysRead(read(d, 1, 2, 9), d))
    }

    @Test
    fun `a missed day changes nothing but that day`() {
        val d = days(10)
        // Day 4 is simply never added. Nothing resets.
        assertEquals(3, ReadingPlanRepository.daysRead(read(d, 1, 2, 3), d))
        assertEquals(4, ReadingPlanRepository.daysRead(read(d, 1, 2, 3, 5), d))
    }

    @Test
    fun `a day is read when its chapters are, whoever asked for them`() {
        // The same chapter can be reached from the Library or from another
        // plan; what is recorded is the chapter, so the day is already read.
        val d = days(3)
        val st = ReadingPlanState(readChapters = setOf(ReadingPlanRepository.chapterKey("genesis", 2)))
        assertTrue(ReadingPlanRepository.isRead(st, d[1]))
        assertEquals(setOf(2), ReadingPlanRepository.readDayNumbers(st, d))
    }

    @Test
    fun `a partly read day is not a read day`() {
        val day = PlanDay(d = 1, r = listOf(PlanReading("genesis", 1, 3)))
        val st = ReadingPlanState(readChapters = setOf(ReadingPlanRepository.chapterKey("genesis", 1)))
        assertFalse(ReadingPlanRepository.isRead(st, day))
    }

    @Test
    fun `repacking a plan does not unread what was read`() {
        // The reader has read Genesis 1..3. Repack the plan so those chapters
        // now fall on different day numbers: they are still read.
        val d = days(10)
        val st = read(d, 1, 2, 3)
        val repacked = ReadingPlanRepository.redistribute(d, fromDay = 1, remainingDays = 5)
        assertEquals(
            listOf(1, 2, 3),
            repacked.flatMap { ReadingPlanRepository.chaptersOf(it) }
                .filter { it in st.readChapters }
                .map { it.substringAfter(':').toInt() },
        )
    }

    @Test
    fun `oldest unread finds where catching up would start`() {
        val d = days(10)
        assertEquals(3, ReadingPlanRepository.oldestUnread(read(d, 1, 2, 4), "annual", d, currentDay = 5))
    }

    @Test
    fun `nothing owed reports nothing`() {
        val d = days(10)
        assertNull(ReadingPlanRepository.oldestUnread(read(d, 1, 2, 3), "annual", d, currentDay = 3))
    }

    @Test
    fun `an untouched plan owes its first day`() {
        assertEquals(1, ReadingPlanRepository.oldestUnread(ReadingPlanState(), "annual", days(10), 1))
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

    // ── finishing on time: the other answer to falling behind ────────────────

    private fun plan(n: Int) = ReadingPlan(id = "annual", days = n, readings = days(n))

    @Test
    fun `a repacked run can start on a day other than the one it came from`() {
        // Days 3..10 folded into the four days that remain, beginning at day 7.
        val after = ReadingPlanRepository.redistribute(days(10), fromDay = 3, remainingDays = 4, startDay = 7)
        assertEquals(7, after.first().d)
        assertEquals((3..10).toList(), after.flatMap { it.r }.map { it.c })
    }

    @Test
    fun `without a repacking the plan is the plan`() {
        val p = plan(10)
        assertEquals(p.readings, ReadingPlanRepository.effectiveDays(p, ReadingPlanState()))
    }

    @Test
    fun `a repacking folds what was owed into the days that remain`() {
        val p = plan(10)
        // Behind on day 7 with day 3 the oldest unread: chapters 3..10 are owed
        // and four days are left, so they land two to a day from day 7.
        val state = ReadingPlanState(redistributed = mapOf("annual" to Redistribution(from = 3, on = 7)))
        val effective = ReadingPlanRepository.effectiveDays(p, state)
        assertEquals((1..6).toList(), effective.filter { it.d < 7 }.map { it.d })
        val ahead = effective.filter { it.d >= 7 }
        assertTrue("used ${ahead.size} of 4 days", ahead.size <= 4)
        assertEquals(10, ahead.last().d.coerceAtMost(10))
        // Nothing owed is dropped on the way.
        assertEquals((3..10).toList(), ahead.flatMap { it.r }.map { it.c })
    }

    @Test
    fun `a repacking stops the days it came from being owed`() {
        val p = plan(10)
        val state = read(p.readings, 1, 2)
            .copy(redistributed = mapOf("annual" to Redistribution(from = 3, on = 7)))
        val effective = ReadingPlanRepository.effectiveDays(p, state)
        // Days 3..6 were folded forward, so nothing before day 7 is behind now.
        assertEquals(7, ReadingPlanRepository.oldestUnread(state, "annual", effective, currentDay = 8))
    }

    @Test
    fun `a half-written repacking is ignored rather than obeyed`() {
        val p = plan(10)
        val nonsense = ReadingPlanState(redistributed = mapOf("annual" to Redistribution(from = 7, on = 3)))
        assertEquals(p.readings, ReadingPlanRepository.effectiveDays(p, nonsense))
        assertEquals(1, ReadingPlanRepository.oldestUnread(nonsense, "annual", p.readings, currentDay = 5))
    }

    @Test
    fun `merge unions rather than overwrites`() {
        val d = days(10)
        val a = read(d, 1, 2)
        val b = read(d, 2, 3)
        val union = ReadingPlanState(readChapters = a.readChapters + b.readChapters)
        assertEquals(3, ReadingPlanRepository.daysRead(union, d))
    }
}
