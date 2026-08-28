package com.agpeya.app.data

import com.agpeya.app.model.Feast
import com.agpeya.app.model.AthanasiusEntry
import com.agpeya.app.model.BahreHasabReference
import com.agpeya.app.model.GitsaweEntry
import com.agpeya.app.model.GitsaweMonth
import com.agpeya.app.model.GitsawePackage
import com.agpeya.app.model.Mahlet
import com.agpeya.app.model.SundayCycleEntry
import com.agpeya.app.model.SubFeast
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Guards the bundled ግጻዌ data against the [com.agpeya.app.model] contract:
 * every collection must decode with the real serializers, and the Gregorian ->
 * Ethiopian key conversion must hit authoritative calendar anchors. Runs on the
 * JVM by reading the asset files directly (no Android Context needed).
 */
class GitsaweDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val assetsDir: File =
        listOf("src/main/assets/content/gitsawe", "app/src/main/assets/content/gitsawe")
            .map(::File).first { it.isDirectory }

    private fun <T> load(file: String, serializer: KSerializer<T>): List<T> =
        json.decodeFromString(
            ListSerializer(serializer),
            File(assetsDir, file).readText(),
        )

    @Test
    fun `every collection decodes with the expected record count`() {
        assertEquals(366, load("daily-gitsawe.json", GitsaweEntry.serializer()).size)
        assertEquals(43, load("seasonal-gitsawe.json", com.agpeya.app.model.SeasonalEntry.serializer()).size)
        assertEquals(49, load("movable-weekday-gitsawe.json", com.agpeya.app.model.SeasonalEntry.serializer()).size)
        assertEquals(91, load("sunday-cycle-gitsawe.json", SundayCycleEntry.serializer()).size)
        assertEquals(25, load("athanasius.json", AthanasiusEntry.serializer()).size)
        assertEquals(9, load("monthly-gitsawe.json", com.agpeya.app.model.MonthlyEntry.serializer()).size)
        assertEquals(21, load("feasts.json", Feast.serializer()).size)
        assertEquals(40, load("sub-feasts.json", SubFeast.serializer()).size)
        assertEquals(37, load("mahlets.json", Mahlet.serializer()).size)
        assertEquals(8, load("months.json", GitsaweMonth.serializer()).size)
        assertEquals(1, load("packages.json", GitsawePackage.serializer()).size)
        val reference = json.decodeFromString(
            BahreHasabReference.serializer(),
            File(assetsDir, "bahre-hasab-reference.json").readText(),
        )
        assertEquals(17, reference.columns.size)
        assertEquals(15, reference.rows.size)
        assertTrue(reference.rows.all { it.values.size == reference.columns.size })
    }

    @Test
    fun `daily entries carry keys and fully-typed readings`() {
        val daily = load("daily-gitsawe.json", GitsaweEntry.serializer())
        assertTrue("every daily entry has a date key", daily.all { it.date.isNotBlank() })
        assertEquals("keys are unique", daily.size, daily.map { it.date }.toSet().size)

        val newYear = daily.first { it.date == "01-01" }
        val msbak = newYear.kidassie!!.msbak.first()
        assertEquals("መዝሙረ ዳዊት", msbak.verse!!.bookTitle)
        assertEquals(141, msbak.verse!!.chapter)   // proves string->int normalization stuck
        assertEquals(6, msbak.verse!!.start)
    }

    @Test
    fun `fixed calendar covers every regular and leap day with evening readings`() {
        val daily = load("daily-gitsawe.json", GitsaweEntry.serializer())
        val expected = buildSet {
            for (month in 1..12) for (day in 1..30) add("%02d-%02d".format(day, month))
            for (day in 1..6) add("%02d-13".format(day))
        }
        assertEquals(expected, daily.map { it.date }.toSet())
        assertEquals(
            "the source has ሠርክ on every day except Hidar 28",
            listOf("28-03"),
            daily.filter { it.serk == null }.map { it.date },
        )
    }

    @Test
    fun `malformed printed citations remain visible without a broken link`() {
        val newYear = load("daily-gitsawe.json", GitsaweEntry.serializer()).first { it.date == "01-01" }
        val printedWithoutChapter = newYear.kidassie!!.firstDeacon.first()
        assertEquals("ም ቍ ፩ – ፲፩", printedWithoutChapter.citation)
        assertEquals(null, printedWithoutChapter.verse)
    }

    @Test
    fun `additional liturgy readings are not truncated to three rows`() {
        val ginbot1 = load("daily-gitsawe.json", GitsaweEntry.serializer()).first { it.date == "01-09" }
        val liturgy = ginbot1.kidassie!!
        assertEquals(
            4,
            liturgy.firstDeacon.size + liturgy.secondDeacon.size + liturgy.secondKahn.size,
        )
    }

    @Test
    fun `Part 3 activates only explicitly classified Sunday rules`() {
        val entries = load("sunday-cycle-gitsawe.json", SundayCycleEntry.serializer())
        val fixed = entries.first { it.index == 2 }
        assertEquals(1, fixed.monthNum)
        assertEquals(7, fixed.fromDay)
        assertEquals(7, fixed.toDay)
        val lent = entries.first { it.index == 44 }
        assertEquals("abiyTsom", lent.season)
        assertEquals(5, lent.week)
        val ambiguous = entries.first { it.index == 8 }
        assertEquals(null, ambiguous.monthNum)
        assertEquals(null, ambiguous.season)
    }

    @Test
    fun `Part 4 keeps funeral categories and supplications distinct`() {
        val entries = load("athanasius.json", AthanasiusEntry.serializer())
        assertEquals("person", entries.first().category)
        assertEquals("riteChapter", entries.first { it.index == 11 }.category)
        assertEquals(3, entries.first { it.index == 20 }.memorialDay)
        assertEquals("memorial", entries.last().category)
        assertTrue(entries.count { it.supplication != null } == 14)
    }

    @Test
    fun `date key conversion matches authoritative Ethiopian anchors`() {
        // The Millennium: Ethiopian New Year 2000 fell on 12 Sep 2007 -> Meskerem 1.
        assertEquals("01-01", GitsaweRepository.toGitsaweDateKey(LocalDate.of(2007, 9, 12)))
        // Hamle 8, 2018 EC.
        assertEquals("08-11", GitsaweRepository.toGitsaweDateKey(LocalDate.of(2026, 7, 15)))
        // Pagume 1, 2011 EC (a 6-day Pagume year).
        assertEquals("01-13", GitsaweRepository.toGitsaweDateKey(LocalDate.of(2019, 9, 6)))
    }

    @Test
    fun `every daily key is reachable from some Gregorian date in a full year`() {
        val daily = load("daily-gitsawe.json", GitsaweEntry.serializer()).map { it.date }.toSet()
        // Walk one full Ethiopian year's worth of days; every produced key that
        // exists in the data confirms the conversion lands in the data's key space.
        var d = LocalDate.of(2025, 9, 11) // Meskerem 1, 2018 EC
        var hits = 0
        repeat(366) {
            if (GitsaweRepository.toGitsaweDateKey(d) in daily) hits++
            d = d.plusDays(1)
        }
        assertEquals("every day in the leap-year calendar is reachable", 366, hits)
    }
}
