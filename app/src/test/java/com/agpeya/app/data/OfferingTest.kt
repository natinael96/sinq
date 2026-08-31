package com.agpeya.app.data

import com.agpeya.app.model.HabitSchedule
import com.agpeya.app.model.TitheEntry
import com.agpeya.app.model.TitheEntryKind
import com.agpeya.app.model.Vow
import com.agpeya.app.model.VowFulfilment
import com.agpeya.app.ui.common.EthiopianDate
import com.agpeya.app.ui.settings.formatCents
import com.agpeya.app.ui.settings.parseAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Locks the አስራት reckoning, the ስዕለት lifecycle, and the two new feast-anchored
 * cadences — the parts of the offering feature where being wrong means telling
 * someone they owe the wrong amount, or reminding them on the wrong day.
 */
class OfferingTest {

    private fun income(amount: Long, date: String) =
        TitheEntry(id = "i-$date-$amount", kind = TitheEntryKind.INCOME, amount = amount, date = date)

    private fun given(amount: Long, date: String) =
        TitheEntry(id = "g-$date-$amount", kind = TitheEntryKind.GIVEN, amount = amount, date = date)

    // ── The reckoning ────────────────────────────────────────────────────────

    @Test
    fun `tithe due is the chosen share of income`() {
        val entries = listOf(income(500_000, "2026-08-01"), income(250_000, "2026-08-10"))
        val r = OfferingRepository.reckon(entries, 10)
        assertEquals(750_000L, r.income)
        assertEquals(75_000L, r.due)
        assertEquals(0L, r.given)
        assertEquals(75_000L, r.owed)
        assertFalse(r.settled)
    }

    @Test
    fun `giving beyond the tithe reports a surplus rather than clamping to zero`() {
        val r = OfferingRepository.reckon(listOf(income(100_000, "2026-08-01"), given(50_000, "2026-08-02")), 10)
        assertEquals(10_000L, r.due)
        assertEquals(-40_000L, r.owed)
        assertTrue(r.settled)
    }

    @Test
    fun `a fraction of a coin rounds down, never inventing a debt`() {
        // 5 santim at 10% is half a santim: owed is 0, not 1.
        assertEquals(0L, OfferingRepository.reckon(listOf(income(5, "2026-08-01")), 10).due)
        assertEquals(1L, OfferingRepository.reckon(listOf(income(15, "2026-08-01")), 10).due)
    }

    @Test
    fun `a non-tenth share is honoured`() {
        assertEquals(20_000L, OfferingRepository.reckon(listOf(income(100_000, "2026-08-01")), 20).due)
    }

    @Test
    fun `entries are filtered by the Ethiopian month, not the Gregorian one`() {
        // 2026-09-10 is መስከረም (month 1); 2026-09-05 is still ጳጉሜ/ነሐሴ of the
        // year before, so a Gregorian-month filter would wrongly pair them.
        val a = LocalDate.of(2026, 9, 10)
        val b = LocalDate.of(2026, 9, 5)
        val ethA = EthiopianDate.from(a)
        val entries = listOf(income(100, a.toString()), income(200, b.toString()))
        val inMonth = OfferingRepository.inEthiopianMonth(entries, ethA.year, ethA.month)
        assertEquals(1, inMonth.size)
        assertEquals(100L, inMonth.single().amount)
    }

    @Test
    fun `an unparseable stored date is skipped rather than crashing the ledger`() {
        val entries = listOf(TitheEntry("x", TitheEntryKind.INCOME, 100, "not-a-date"))
        assertTrue(OfferingRepository.inEthiopianYear(entries, 2018).isEmpty())
    }

    // ── Money at the edges ───────────────────────────────────────────────────

    @Test
    fun `amounts parse the way people type them`() {
        assertEquals(150_000L, parseAmount("1500"))
        assertEquals(150_050L, parseAmount("1500.50"))
        assertEquals(150_000L, parseAmount("1,500"))
        assertEquals(150_000L, parseAmount(" 1 500 "))
        // A lone comma before one or two digits is a decimal mark.
        assertEquals(150_050L, parseAmount("1500,50"))
        assertEquals(50L, parseAmount("0.5"))
        // Extra places truncate down — never record more than was entered.
        assertEquals(19L, parseAmount("0.199"))
    }

    @Test
    fun `nonsense does not parse into a silent zero`() {
        assertNull(parseAmount(""))
        assertNull(parseAmount("abc"))
        assertNull(parseAmount("-100"))
        assertNull(parseAmount("1.2.3"))
    }

    @Test
    fun `formatting round-trips through parsing`() {
        val cents = 1_234_567L
        assertEquals(cents, parseAmount(formatCents(cents, "").trim()))
    }

    // ── Vows ─────────────────────────────────────────────────────────────────

    @Test
    fun `a vow with an amount is settled only once it is paid up`() {
        val vow = Vow(id = "v", pledged = 100_000)
        assertFalse(vow.settled)
        val part = vow.copy(fulfilments = listOf(VowFulfilment("f1", "2026-08-01", 40_000)))
        assertEquals(60_000L, part.remaining)
        assertFalse(part.settled)
        val full = part.copy(fulfilments = part.fulfilments + VowFulfilment("f2", "2026-08-02", 60_000))
        assertTrue(full.settled)
        assertEquals(0L, full.remaining)
    }

    @Test
    fun `a vow without an amount is settled by any record at all`() {
        val vow = Vow(id = "v")
        assertFalse(vow.settled)
        assertTrue(vow.copy(fulfilments = listOf(VowFulfilment("f", "2026-08-01"))).settled)
    }

    @Test
    fun `a one-time vow stops reminding once kept, a standing one does not`() {
        val kept = listOf(VowFulfilment("f", "2026-08-01", 100_000))
        val once = Vow(id = "a", pledged = 100_000, oneTime = true, fulfilments = kept)
        assertFalse(once.remindsStill)
        val standing = once.copy(oneTime = false)
        assertTrue(standing.remindsStill)
        // Switching a vow off silences it whether or not it has been kept.
        assertFalse(standing.copy(enabled = false).remindsStill)
    }

    @Test
    fun `overpaying a vow never reports a negative remainder`() {
        val vow = Vow(id = "v", pledged = 10_000, fulfilments = listOf(VowFulfilment("f", "2026-08-01", 25_000)))
        assertEquals(0L, vow.remaining)
        assertTrue(vow.settled)
    }

    // ── The new cadences ─────────────────────────────────────────────────────

    @Test
    fun `a yearly schedule falls on its Ethiopian month and day`() {
        val meskel = HabitSchedule(kind = HabitSchedule.Kind.YEARLY, monthNum = 1, monthDay = 17)
        val due = meskel.nextDueOnOrAfter(LocalDate.of(2026, 1, 1))
        val eth = EthiopianDate.from(due!!)
        assertEquals(1, eth.month)
        assertEquals(17, eth.day)
        assertTrue(meskel.isDueOn(due))
        assertFalse(meskel.isDueOn(due.plusDays(1)))
    }

    @Test
    fun `a yearly schedule recurs, and the gap is about a year`() {
        val schedule = HabitSchedule(kind = HabitSchedule.Kind.YEARLY, monthNum = 5, monthDay = 11)
        val first = schedule.nextDueOnOrAfter(LocalDate.of(2026, 1, 1))!!
        val second = schedule.nextDueOnOrAfter(first.plusDays(1))!!
        val gap = second.toEpochDay() - first.toEpochDay()
        assertTrue("gap was $gap days", gap in 360..371)
    }

    @Test
    fun `a feast schedule tracks the movable date rather than a stored one`() {
        val schedule = HabitSchedule(kind = HabitSchedule.Kind.FEAST, feastKey = "fasika")
        val first = schedule.nextDueOnOrAfter(LocalDate.of(2026, 1, 1))!!
        // Fasika is a Sunday, every year, wherever the computus puts it.
        assertEquals(java.time.DayOfWeek.SUNDAY, first.dayOfWeek)
        val second = schedule.nextDueOnOrAfter(first.plusDays(1))!!
        assertEquals(java.time.DayOfWeek.SUNDAY, second.dayOfWeek)
        // Two consecutive Fasikas are a year apart give or take the five weeks
        // the date is free to move in.
        val gap = second.toEpochDay() - first.toEpochDay()
        assertTrue("gap was $gap days", gap in 330..400)
    }

    @Test
    fun `a fixed feast key resolves to its Ethiopian date`() {
        val date = HolidayCalendar.dateOf("meskel", 2019)!!
        val eth = EthiopianDate.from(date)
        assertEquals(2019, eth.year)
        assertEquals(1, eth.month)
        assertEquals(17, eth.day)
    }

    @Test
    fun `every annual feast resolves for a run of years`() {
        for (year in 2015..2025) {
            for (holiday in HolidayCalendar.annual) {
                assertTrue(
                    "${holiday.key} did not resolve for $year",
                    HolidayCalendar.dateOf(holiday.key, year) != null,
                )
            }
        }
    }

    @Test
    fun `an unknown feast key never fires and never loops forever`() {
        val schedule = HabitSchedule(kind = HabitSchedule.Kind.FEAST, feastKey = "no-such-feast")
        assertNull(schedule.nextDueOnOrAfter(LocalDate.of(2026, 1, 1)))
        assertFalse(schedule.isDueOn(LocalDate.of(2026, 4, 12)))
    }

    // ── The bundled monthly calendar ─────────────────────────────────────────

    @Test
    fun `the monthly holiday asset covers all thirty days with agreeing sources`() {
        val raw = File("src/main/assets/content/holidays/monthly.json").readText()
        val holidays = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            .decodeFromString<List<com.agpeya.app.model.MonthlyHoliday>>(raw)
        assertEquals(30, holidays.size)
        assertEquals((1..30).toList(), holidays.map { it.day })
        holidays.forEach {
            assertTrue("day ${it.day} has no name", it.primary.isNotBlank())
            // Derived from the ስንክሳር's own ወርኀዊ በዓላት blocks; a day backed by
            // only a handful of months would mean the extraction had drifted.
            assertTrue("day ${it.day} agreed on by only ${it.months} months", it.months >= 9)
        }
        // Spot-check the days people most often anchor a ስዕለት to.
        assertTrue(holidays.first { it.day == 12 }.primary.contains("ሚካኤል"))
        assertTrue(holidays.first { it.day == 19 }.primary.contains("ገብርኤል"))
        assertTrue(holidays.first { it.day == 21 }.primary.contains("ማርያም"))
        // ቀን ፳፫ leads with ዳዊት in the sources but is kept as ጊዮርጊስ's day —
        // the reason `also` is not truncated to a top-N slice.
        val twentyThird = holidays.first { it.day == 23 }
        assertTrue((listOf(twentyThird.primary) + twentyThird.also).any { it.contains("ጊዮርጊስ") })
    }
}
