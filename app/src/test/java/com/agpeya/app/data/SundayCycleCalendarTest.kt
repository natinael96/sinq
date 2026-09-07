package com.agpeya.app.data

import com.agpeya.app.model.SundayCycleEntry
import com.agpeya.app.ui.common.EthiopianDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The fixed-anchored Sunday seasons of Part 3. Dates are given as Ethiopian
 * month/day so each case reads like the rubric it checks.
 */
class SundayCycleCalendarTest {

    private fun eth(year: Int, month: Int, day: Int): LocalDate = EthiopianDate(year, month, day).toGregorian()

    private fun windows(year: Int, month: Int, day: Int) =
        SundayCycleCalendar.windowsOn(eth(year, month, day)).map { it.season to it.week }

    private fun sundaysOf(year: Int, month: Int, from: Int, to: Int): List<LocalDate> =
        (from..to).map { eth(year, month, it) }.filter { it.dayOfWeek == DayOfWeek.SUNDAY }

    @Test
    fun `the Sunday of Pagumen 2018 takes the Pagumen rule`() {
        val date = LocalDate.of(2026, 9, 6)
        assertEquals(EthiopianDate(2018, 13, 1), EthiopianDate.from(date))
        assertEquals(DayOfWeek.SUNDAY, date.dayOfWeek)
        val w = windows(2018, 13, 1)
        assertTrue(w.toString(), ("kremt" to 15) in w)
        assertTrue(w.toString(), ("pagumen" to null) in w)
        assertTrue(w.toString(), ("nuha" to null) in w)
    }

    @Test
    fun `tsige counts six Sundays from Meskerem 26`() {
        assertEquals(listOf("tsige" to 1), windows(2018, 1, 26))
        assertEquals(listOf("tsige" to 1), windows(2018, 1, 30))
        assertEquals(listOf("tsige" to 2), windows(2018, 2, 3))
        assertEquals(listOf("tsige" to 6), windows(2018, 3, 5))
        assertEquals(listOf("astemhro" to 1), windows(2018, 3, 6))
    }

    @Test
    fun `astemhro runs to Tahsas 6 and the three Advent Sundays follow`() {
        assertEquals(listOf("astemhro" to 5), windows(2018, 4, 6))
        assertEquals(listOf("sibket" to null), windows(2018, 4, 7))
        assertEquals(listOf("sibket" to null), windows(2018, 4, 13))
        assertEquals(listOf("birhan" to null), windows(2018, 4, 14))
        assertEquals(listOf("nolawi" to null), windows(2018, 4, 27))
        assertEquals(emptyList<Pair<String, Int?>>(), windows(2018, 4, 28))   // መርዓዊ: a fixed-date rule
    }

    @Test
    fun `ledet is Tahsas 29 except in the year of John`() {
        assertEquals(29, SundayCycleCalendar.ledet(2018).day)
        assertEquals(28, SundayCycleCalendar.ledet(2016).day)   // 7 Jan 2024
        assertEquals(LocalDate.of(2024, 1, 7), SundayCycleCalendar.ledet(2016).toGregorian())
        assertEquals(listOf("lidet" to 0), windows(2018, 4, 29))
        assertEquals(listOf("lidet" to 0), windows(2016, 4, 28))
    }

    @Test
    fun `lidet Sundays are counted after the feast and skip the Simeon ordinal`() {
        // 2017: ልደት is Tuesday 7 Jan 2025; Sundays follow on ጥር 4, 11, 18, 25, የካቲት 2, 9.
        assertEquals(DayOfWeek.TUESDAY, eth(2017, 4, 29).dayOfWeek)
        assertEquals(listOf("lidet" to 1), windows(2017, 5, 4))
        assertEquals(listOf("lidet" to 2), windows(2017, 5, 11))
        assertEquals(listOf("lidet" to 5), windows(2017, 6, 2))
        assertEquals(listOf("lidet" to 7), windows(2017, 6, 9))      // sixth Sunday: ፮ተኛ is ስምዖን's
        // ዘወረደ 2017 is 23 Feb 2025 (Nineveh 10 Feb): the series stops the Sunday before.
        val zewerede = BahreHasab.nineveh(2017).plusDays(13)
        assertEquals(LocalDate.of(2025, 2, 23), zewerede)
        assertEquals(DayOfWeek.SUNDAY, zewerede.dayOfWeek)
        assertTrue(SundayCycleCalendar.windowsOn(zewerede).none { it.season == "lidet" })
        // 2018: Lent is early enough that ዘወረደ is the sixth Sunday, so ልደት keeps five.
        assertEquals(listOf("lidet" to 5), windows(2018, 6, 1))
        assertEquals(LocalDate.of(2026, 2, 15), BahreHasab.nineveh(2018).plusDays(13))
        assertTrue(windows(2018, 6, 8).none { it.first == "lidet" })
    }

    @Test
    fun `kremt sub-seasons hand out the printed ordinals in order`() {
        // ዘርዕ ደመና: ሰኔ ፳፬ – ሐምሌ ፲፰ → ፩ ፪ ፬ ፭
        val zere = sundaysOf(2018, 10, 24, 30) + sundaysOf(2018, 11, 1, 18)
        assertEquals(listOf(1, 2, 4, 5).take(zere.size), zere.map { kremtOrdinal(it) })
        // መብረቅ …: ሐምሌ ፲፱ – ነሐሴ ፲፭ → ፯ ፰ ፱ ፲፩
        val mebreq = sundaysOf(2018, 11, 19, 30) + sundaysOf(2018, 12, 1, 15)
        assertEquals(listOf(7, 8, 9, 11).take(mebreq.size), mebreq.map { kremtOrdinal(it) })
        // ዕጐለ ቋዓት: ነሐሴ ፲፮ – ፳፯ → ፲፪ ፲፫
        val egule = sundaysOf(2018, 12, 16, 27)
        assertEquals(listOf(12, 13).take(egule.size), egule.map { kremtOrdinal(it) })
        // ኑኀ ነግህ: ነሐሴ ፳፰ onward → ፲፬
        assertEquals(14, kremtOrdinal(eth(2018, 12, 30)))
    }

    private fun kremtOrdinal(date: LocalDate): Int =
        SundayCycleCalendar.windowsOn(date).first { it.season == "kremt" }.week!!

    @Test
    fun `the feast-conditional kremt ordinals appear only on their dates`() {
        assertTrue(("kremt" to 3) in windows(2018, 11, 5))
        assertTrue(("kremt" to 6) in windows(2018, 11, 19))
        assertTrue(("kremt" to 10) in windows(2018, 12, 6))
        assertTrue(windows(2018, 11, 6).none { it == ("kremt" to 3) })
        assertTrue(("filseta" to 1) in windows(2018, 12, 1))
        assertTrue(("filseta" to 3) in windows(2018, 12, 15))
        assertTrue(windows(2018, 12, 16).none { it.first == "filseta" })
    }

    @Test
    fun `the Sene astemhro sits inside the start of kremt`() {
        assertEquals(listOf("seneAstemhro" to 1), windows(2018, 10, 17))
        val w = windows(2018, 10, 24)
        assertTrue(w.toString(), ("seneAstemhro" to 2) in w)
        assertTrue(w.toString(), ("zere" to null) in w)
        assertTrue(w.toString(), ("kremt" to 1) in w)
    }

    @Test
    fun `every Sunday of the year lands in a Part 3 season or a fixed-date rule`() {
        // The fixed rules (Part 3 rows 1–7, 27, 31, 35 …) cover Meskerem 1–25 and
        // single feast days; everything else must have a season window.
        var date = eth(2018, 1, 1)
        val end = eth(2019, 1, 1)
        while (date.isBefore(end)) {
            if (date.dayOfWeek == DayOfWeek.SUNDAY) {
                val e = EthiopianDate.from(date)
                val fixed = e.month == 1 && e.day <= 25 || e.month == 4 && e.day == 28
                val seasons = GitsaweRepository.seasonWindowsOn(date)
                assertTrue("no window for ${e.month}/${e.day}", fixed || seasons.isNotEmpty())
            }
            date = date.plusDays(1)
        }
    }

    @Test
    fun `weekdays outside every window carry nothing`() {
        assertNull(SundayCycleCalendar.windowsOn(eth(2018, 1, 10)).firstOrNull())
    }

    private val rows: List<SundayCycleEntry> by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val file = File("src/main/assets/content/gitsawe/sunday-cycle-gitsawe.json")
        json.decodeFromString(ListSerializer(SundayCycleEntry.serializer()), file.readText())
    }

    @Test
    fun `every Sunday of 2018 and 2019 selects at least one printed row`() {
        for (year in listOf(2018, 2019)) {
            var date = eth(year, 1, 1)
            val end = eth(year + 1, 1, 1)
            while (date.isBefore(end)) {
                if (date.dayOfWeek == DayOfWeek.SUNDAY) {
                    val e = EthiopianDate.from(date)
                    val picked = GitsaweRepository.selectSundayCycle(rows, date)
                    assertTrue("no row for $year ${e.month}/${e.day}", picked.isNotEmpty())
                    // ሆሣዕና prints nine rows; nothing else should come close.
                    assertTrue("too many rows for $year ${e.month}/${e.day}: ${picked.map { it.index }}", picked.size <= 9)
                }
                date = date.plusDays(1)
            }
        }
    }

    @Test
    fun `the Sunday of Pagumen 1 2018 selects the Pagumen hymn`() {
        val picked = GitsaweRepository.selectSundayCycle(rows, LocalDate.of(2026, 9, 6))
        assertEquals(listOf(91), picked.map { it.index })
        assertEquals("መዝሙር ከመ እንተ መብረቅ ።", picked.single().mezmur)
    }

    @Test
    fun `Holy Saturday selects its own row although it is not a Sunday`() {
        val saturday = BahreHasab.fasika(2018).minusDays(1)
        assertEquals(DayOfWeek.SATURDAY, saturday.dayOfWeek)
        assertEquals(listOf(56), GitsaweRepository.selectSundayCycle(rows, saturday).map { it.index })
    }

    @Test
    fun `a feast on a Sunday overrides the season row`() {
        // The first year from 2015 in which ጥቅምት ፲፪ (ሚካኤል) falls on a Sunday.
        val michael = (2015..2030).map { eth(it, 2, 12) }.first { it.dayOfWeek == DayOfWeek.SUNDAY }
        assertEquals(listOf(15), GitsaweRepository.selectSundayCycle(rows, michael).map { it.index })
        // The Sunday after takes its ዘመነ ጽጌ ordinal instead (ጥቅምት ፲፱ is the fourth).
        assertEquals(listOf(11), GitsaweRepository.selectSundayCycle(rows, michael.plusDays(7)).map { it.index })
    }

    @Test
    fun `Great Lent outranks a feast that falls on one of its Sundays`() {
        // 15 Feb 2026 is both ስምዖን (የካቲት ፰) and ዘወረደ, the first Sunday of Lent.
        val date = LocalDate.of(2026, 2, 15)
        assertEquals(EthiopianDate(2018, 6, 8), EthiopianDate.from(date))
        assertEquals("abiyTsom", BahreHasab.movableSeasonOn(date)?.season)
        assertEquals(listOf(39, 40), GitsaweRepository.selectSundayCycle(rows, date).map { it.index })
        // In a year where ስምዖን precedes Lent it keeps its own hymn.
        val simeon = (2000..2080).map { eth(it, 6, 8) }
            .first { it.dayOfWeek == DayOfWeek.SUNDAY && BahreHasab.movableSeasonOn(it) == null }
        assertEquals(listOf(35), GitsaweRepository.selectSundayCycle(rows, simeon).map { it.index })
    }
}
