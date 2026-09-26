package com.agpeya.app.habits

import com.agpeya.app.data.BahreHasab
import com.agpeya.app.data.FastingCalendar
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.data.PrayerJourney
import com.agpeya.app.model.HabitsState
import com.agpeya.app.ui.common.EthiopianDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The prayer-day metric: distinct days one of the hours was prayed, inside the
 * current period. The properties that matter are the ones a streak lacks — the
 * count survives missed days, never resets, and counts a day once however much
 * it holds. What "prayed" means is a property in its own right: an hour, not
 * any mark at all, so ቤት's candle and ጉዞ's count cannot disagree.
 */
class PrayerJourneyTest {

    // ሐምሌ 11, 2018 EC — mid Ethiopian month, outside every fast period.
    private val today = LocalDate.of(2026, 7, 18)
    private fun d(offset: Long) = today.plusDays(offset).toString()

    private fun ecMonthStart(date: LocalDate): LocalDate =
        EthiopianDate.from(date).let { EthiopianDate(it.year, it.month, 1).toGregorian() }

    @Test
    fun `daily checklist includes Synaxarium church and prostration`() {
        val ids = HabitsRepository.orderedHabitIds(HabitsState(), includeHidden = false)
        assertTrue("sinksar" in ids)
        assertTrue("church" in ids)
        assertTrue("prostrate" in ids)
    }

    // ---- daysPrayedBetween ----

    @Test
    fun `empty history counts zero`() {
        assertEquals(0, PrayerJourney.daysPrayedBetween(emptyMap(), today.minusDays(30), today))
    }

    @Test
    fun `counts distinct days not events`() {
        // Three prayers on one day and one on another: two days, not four.
        val records = mapOf(
            d(0) to setOf("hour_morning", "hour_vespers", "bible"),
            d(-3) to setOf("hour_compline"),
        )
        assertEquals(2, PrayerJourney.daysPrayedBetween(records, today.minusDays(30), today))
    }

    @Test
    fun `a gap does not reset the count`() {
        val records = mapOf(d(0) to setOf("hour_x"), d(-1) to setOf("hour_x"), d(-9) to setOf("hour_x"))
        assertEquals(3, PrayerJourney.daysPrayedBetween(records, today.minusDays(30), today))
    }

    @Test
    fun `range is inclusive on both ends and excludes the rest`() {
        val records = mapOf(
            d(0) to setOf("hour_x"),
            d(-5) to setOf("hour_x"),
            d(-6) to setOf("hour_x"),   // before the range
            d(1) to setOf("hour_x"),    // after the range
        )
        assertEquals(2, PrayerJourney.daysPrayedBetween(records, today.minusDays(5), today))
    }

    @Test
    fun `empty sets and malformed keys are ignored`() {
        val records = mapOf(
            d(0) to emptySet<String>(),
            "not-a-date" to setOf("hour_x"),
            d(-1) to setOf("hour_x"),
        )
        assertEquals(1, PrayerJourney.daysPrayedBetween(records, today.minusDays(30), today))
    }

    // ---- summarize: the month period ----

    @Test
    fun `month period counts only the current Ethiopian month`() {
        val monthStart = ecMonthStart(today)
        val records = mapOf(
            monthStart.toString() to setOf("hour_x"),
            monthStart.minusDays(1).toString() to setOf("hour_x"), // previous EC month
            today.toString() to setOf("hour_x"),
        )
        val s = PrayerJourney.summarize(records, today)
        assertNull(s.fast)
        assertEquals(2, s.daysPrayed)
    }

    @Test
    fun `first day of an Ethiopian month starts the count fresh`() {
        // ጥቅምት 1, 2018 EC — a month boundary that sits outside every fast
        // (ሐምሌ 1 would not: it falls inside ጾመ ሐዋርያት, whose period then wins).
        val monthStart = EthiopianDate(2018, 2, 1).toGregorian()
        val records = mapOf(
            monthStart.minusDays(1).toString() to setOf("hour_x"), // መስከረም 30
            monthStart.minusDays(2).toString() to setOf("hour_x"),
        )
        val s = PrayerJourney.summarize(records, monthStart)
        assertNull(s.fast)
        assertEquals(0, s.daysPrayed)
    }

    @Test
    fun `never prayed is zero and not a return`() {
        val s = PrayerJourney.summarize(emptyMap(), today)
        assertFalse(s.prayedToday)
        assertFalse(s.returning)
        assertEquals(0, s.daysPrayed)
    }

    @Test
    fun `prayed today lights the candle`() {
        val s = PrayerJourney.summarize(mapOf(d(0) to setOf("hour_x")), today)
        assertTrue(s.prayedToday)
        assertFalse(s.returning)
        assertEquals(1, s.daysPrayed)
    }

    @Test
    fun `unmarked today after a prayed yesterday is an ordinary morning`() {
        val s = PrayerJourney.summarize(mapOf(d(-1) to setOf("hour_x")), today)
        assertFalse(s.prayedToday)
        assertFalse(s.returning)
    }

    @Test
    fun `one whole missed day reads as a return`() {
        val s = PrayerJourney.summarize(mapOf(d(-2) to setOf("hour_x")), today)
        assertTrue(s.returning)
        assertEquals(1, s.daysPrayed) // the record two days ago still counts
    }

    @Test
    fun `a long gap is still just a return with the record intact`() {
        val monthStart = ecMonthStart(today)
        val gapDays = (monthStart.toEpochDay()..today.minusDays(10).toEpochDay())
            .associate { LocalDate.ofEpochDay(it).toString() to setOf("hour_x") }
        val s = PrayerJourney.summarize(gapDays, today)
        assertTrue(s.returning)
        assertEquals(gapDays.size, s.daysPrayed)
    }

    @Test
    fun `praying today ends the return state`() {
        val s = PrayerJourney.summarize(mapOf(d(-10) to setOf("hour_x"), d(0) to setOf("hour_x")), today)
        assertFalse(s.returning)
        assertTrue(s.prayedToday)
    }

    // ---- summarize: the fast period ----

    @Test
    fun `inside a fast the period is the fast`() {
        // ዐቢይ ጾም of the current Ethiopian year, taken from BahreHasab itself.
        val lentStart = BahreHasab.greatLentStart(EthiopianDate.from(today).year)
        val day18 = lentStart.plusDays(17)
        val records = mapOf(
            lentStart.toString() to setOf("hour_x"),
            lentStart.plusDays(3).toString() to setOf("hour_x"),
            lentStart.minusDays(1).toString() to setOf("hour_x"), // before the fast
            day18.toString() to setOf("hour_x"),
        )
        val s = PrayerJourney.summarize(records, day18)
        assertEquals("abiy", s.fast?.key)
        assertEquals(18, s.fastDay)
        assertEquals(3, s.daysPrayed)
        assertTrue(s.prayedToday)
    }

    @Test
    fun `first day of a fast is day one`() {
        val ethYear = EthiopianDate.from(today).year
        val lentStart = BahreHasab.greatLentStart(ethYear)
        val s = PrayerJourney.summarize(emptyMap(), lentStart)
        assertEquals("abiy", s.fast?.key)
        assertEquals(1, s.fastDay)
        assertEquals(0, s.daysPrayed)
    }

    @Test
    fun `the day after a fast ends falls back to the month period`() {
        // ዐቢይ ጾም ends the eve of ትንሣኤ; Fasika itself is in no fast period.
        val fasika = BahreHasab.fasika(EthiopianDate.from(today).year)
        assertNull(FastingCalendar.fastOn(fasika))
        val s = PrayerJourney.summarize(emptyMap(), fasika)
        assertNull(s.fast)
        assertNull(s.fastDay)
    }

    // ---- per-habit distinct-day counts (HabitsRepository) ----

    @Test
    fun `habit days count distinct days for that habit only`() {
        val records = mapOf(
            d(0) to setOf("bible", "church"),
            d(-1) to setOf("bible"),
            d(-4) to setOf("church"),
        )
        assertEquals(2, HabitsRepository.habitDaysBetween(records, "bible", today.minusDays(30), today))
        assertEquals(2, HabitsRepository.habitDaysBetween(records, "church", today.minusDays(30), today))
        assertEquals(0, HabitsRepository.habitDaysBetween(records, "prostrate", today.minusDays(30), today))
    }

    @Test
    fun `prayer days aggregate any hour on a day as one day`() {
        val records = mapOf(
            d(0) to setOf("hour_morning", "hour_vespers"),
            d(-1) to setOf("bible"),           // no hour: not a prayer day
            d(-2) to setOf("hour_compline"),
        )
        assertEquals(2, PrayerJourney.daysPrayedBetween(records, today.minusDays(30), today))
    }

    @Test
    fun `a day of keeping without an hour is not a day of prayer`() {
        // ስንክሳር read, church attended, the plan's chapters done — kept, and
        // counted as keeping, but the candle is not lit by them.
        val records = mapOf(d(0) to setOf("sinksar", "church", "bible"))
        assertFalse(PrayerJourney.prayed(records.getValue(d(0))))
        val s = PrayerJourney.summarize(records, today)
        assertFalse(s.prayedToday)
        assertEquals(0, s.daysPrayed)
        assertEquals(3, HabitsRepository.dayCount(records, today))
    }

    // ---- heatmap level ----

    @Test
    fun `heatmap level counts what was kept, not a share of what could be`() {
        // The scale is absolute, so adding a habit never dims a past day.
        assertEquals(0, HabitsRepository.level(0))
        assertEquals(1, HabitsRepository.level(1))
        assertEquals(2, HabitsRepository.level(2))
        assertEquals(2, HabitsRepository.level(3))
        assertEquals(3, HabitsRepository.level(4))
        assertEquals(3, HabitsRepository.level(6))
        assertEquals(4, HabitsRepository.level(HabitsRepository.FULL_DAY))
        assertEquals(4, HabitsRepository.level(20))
    }

    @Test
    fun `the seven hours alone are a full day`() {
        val hours = (1..HabitsRepository.FULL_DAY).map { HabitsRepository.hourHabitId("h$it") }
        assertEquals(4, HabitsRepository.level(hours.size))
    }

    @Test
    fun `day count reflects number of habits`() {
        val records = mapOf(d(0) to setOf("prayer", "bible", "church"))
        assertEquals(3, HabitsRepository.dayCount(records, today))
        assertEquals(0, HabitsRepository.dayCount(records, today.minusDays(1)))
    }
}
