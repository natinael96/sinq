package com.agpeya.app.habits

import com.agpeya.app.data.HabitsRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HabitStreakTest {

    private val today = LocalDate.of(2026, 6, 18)
    private fun d(offset: Long) = today.plusDays(offset).toString()

    @Test
    fun `empty history is zero`() {
        assertEquals(0, HabitsRepository.currentStreak(emptyMap(), "prayer", today))
        assertEquals(0, HabitsRepository.longestStreak(emptyMap(), "prayer"))
        assertEquals(0, HabitsRepository.overallCurrentStreak(emptyMap(), today))
    }

    @Test
    fun `current streak counts today plus preceding days`() {
        val records = mapOf(d(0) to setOf("prayer"), d(-1) to setOf("prayer"), d(-2) to setOf("prayer"))
        assertEquals(3, HabitsRepository.currentStreak(records, "prayer", today))
    }

    @Test
    fun `today not marked still counts through yesterday`() {
        val records = mapOf(d(-1) to setOf("prayer"), d(-2) to setOf("prayer"))
        assertEquals(2, HabitsRepository.currentStreak(records, "prayer", today))
    }

    @Test
    fun `streak breaks on a missed day`() {
        // today + yesterday, then a gap at day -2, then -3
        val records = mapOf(d(0) to setOf("prayer"), d(-1) to setOf("prayer"), d(-3) to setOf("prayer"))
        assertEquals(2, HabitsRepository.currentStreak(records, "prayer", today))
    }

    @Test
    fun `zero when neither today nor yesterday done`() {
        val records = mapOf(d(-2) to setOf("prayer"))
        assertEquals(0, HabitsRepository.currentStreak(records, "prayer", today))
    }

    @Test
    fun `longest streak over a sparse history`() {
        val records = mapOf(
            d(-10) to setOf("prayer"),
            d(-9) to setOf("prayer"),
            d(-8) to setOf("prayer"),
            d(-8 + -100) to setOf("prayer"), // unrelated far date
            d(-2) to setOf("prayer"),
            d(-1) to setOf("prayer"),
        )
        assertEquals(3, HabitsRepository.longestStreak(records, "prayer"))
    }

    @Test
    fun `per-habit isolation`() {
        val records = mapOf(d(0) to setOf("prayer", "bible"), d(-1) to setOf("prayer"))
        assertEquals(2, HabitsRepository.currentStreak(records, "prayer", today))
        assertEquals(1, HabitsRepository.currentStreak(records, "bible", today))
    }

    @Test
    fun `overall streak counts any-habit days`() {
        val records = mapOf(d(0) to setOf("church"), d(-1) to setOf("prayer"), d(-2) to setOf("bible"))
        assertEquals(3, HabitsRepository.overallCurrentStreak(records, today))
    }

    @Test
    fun `day count reflects number of habits`() {
        val records = mapOf(d(0) to setOf("prayer", "bible", "church"))
        assertEquals(3, HabitsRepository.dayCount(records, today))
        assertEquals(0, HabitsRepository.dayCount(records, today.minusDays(1)))
    }
}
