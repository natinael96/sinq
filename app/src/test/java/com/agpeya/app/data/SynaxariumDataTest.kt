package com.agpeya.app.data

import com.agpeya.app.model.SynaxariumManifest
import com.agpeya.app.model.SynaxariumMonth
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the bundled ስንክሳር: every month decodes and the totals hold. */
class SynaxariumDataTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val dir: File? =
        listOf("src/main/assets/content/sinksar", "app/src/main/assets/content/sinksar")
            .map(::File).firstOrNull { it.isDirectory }

    @Test
    fun `all 13 months of both editions decode with the expected totals`() {
        org.junit.Assume.assumeTrue(dir != null && File(dir, "manifest.json").exists())
        val manifestFile = File(dir!!, "manifest.json")
        val manifest: SynaxariumManifest =
            json.decodeFromString(manifestFile.readText())
        assertEquals(13, manifest.months.size)
        assertEquals(listOf("am", "ge"), manifest.editions.map { it.code })

        var days = 0
        var entries = 0
        for (m in 1..13) {
            val month: SynaxariumMonth = json.decodeFromString(File(dir, "am-$m.json").readText())
            assertEquals(m, month.month)
            days += month.days.size
            entries += month.days.sumOf { it.entries.size }
            assertTrue("month $m has days", month.days.isNotEmpty())
            // The manifest drives the month list; if it disagrees with the file
            // a month advertises a day the reader cannot open.
            assertEquals(
                "manifest day count for month $m",
                manifest.months.first { it.month == m }.days,
                month.days.size,
            )
        }
        // The whole fixed-calendar book: twelve months of thirty days plus all
        // six of ጳጉሜን — the synaxarium keeps the leap day whatever the year does.
        assertEquals(366, days)
        assertEquals(1242, entries)

        // The Ge'ez edition covers the same year and is aligned with the Amharic edition.
        var geezDays = 0
        var geezEntries = 0
        for (m in 1..13) {
            val month: SynaxariumMonth = json.decodeFromString(File(dir, "ge-$m.json").readText())
            geezDays += month.days.size
            geezEntries += month.days.sumOf { it.entries.size }
        }
        assertEquals(366, geezDays)
        assertEquals(1242, geezEntries)
    }

    /**
     * Bookmarks point at an entry id, so two entries sharing one in the same day
     * would make a bookmark on either light up on both.
     */
    @Test
    fun `entry ids are unique within their day`() {
        org.junit.Assume.assumeTrue(dir != null && File(dir, "manifest.json").exists())
        for (edition in listOf("am", "ge")) {
            for (m in 1..13) {
                val month: SynaxariumMonth =
                    json.decodeFromString(File(dir!!, "$edition-$m.json").readText())
                for (day in month.days) {
                    val ids = day.entries.map { it.id }
                    assertEquals(
                        "$edition month $m day ${day.day} repeats an entry id",
                        ids.distinct().size,
                        ids.size,
                    )
                }
            }
        }
    }

    /**
     * A month must cover 1..n with no gap and no repeat: a duplicate shadows a
     * day, and a hole leaves the reader on that date with nothing at all.
     */
    @Test
    fun `every month covers its days exactly once`() {
        org.junit.Assume.assumeTrue(dir != null && File(dir, "manifest.json").exists())
        for (m in 1..13) {
            val month: SynaxariumMonth = json.decodeFromString(File(dir!!, "am-$m.json").readText())
            val dayNumbers = month.days.map { it.day }
            assertEquals("month $m has duplicate days", dayNumbers.distinct().size, dayNumbers.size)
            assertEquals(
                "month $m skips a day",
                (1..dayNumbers.size).toList(),
                dayNumbers.sorted(),
            )
        }
    }

    @Test
    fun `Meskerem 1 has commemorations with text`() {
        org.junit.Assume.assumeTrue(dir != null && File(dir, "manifest.json").exists())
        val meskerem: SynaxariumMonth = json.decodeFromString(File(dir!!, "am-1.json").readText())
        val day1 = meskerem.days.first { it.day == 1 }
        assertTrue(day1.entries.isNotEmpty())
        assertTrue("first entry has narrative text", day1.entries.first().text.length > 50)
        assertTrue("the day carries its own heading", day1.header.startsWith("ስንክሳር ዘወርኀ") || day1.header.startsWith("ስንክሳር፡ዘወርኀ"))
    }

    @Test
    fun `arke hymns carry traditional liturgical punctuation with semicolons and periods`() {
        org.junit.Assume.assumeTrue(dir != null && File(dir, "manifest.json").exists())
        val meskerem: SynaxariumMonth = json.decodeFromString(File(dir!!, "am-1.json").readText())
        val arke = meskerem.days.first().entries.first().arke
        org.junit.Assert.assertNotNull("Meskerem 1 entry 0 has arke", arke)
        assertTrue("Arke carries traditional semicolon line breaks", arke!!.contains("፤"))
        assertTrue("Arke ends with full stop", arke.endsWith("።"))
    }

    @Test
    fun `synaxarium narrative paragraphs preserve Ethiopic manuscript wordspace and punctuation`() {
        org.junit.Assume.assumeTrue(dir != null && File(dir, "manifest.json").exists())
        val meskeremAm: SynaxariumMonth = json.decodeFromString(File(dir!!, "am-1.json").readText())
        val meskeremGe: SynaxariumMonth = json.decodeFromString(File(dir!!, "ge-1.json").readText())
        val amText = meskeremAm.days.first().entries.first().paragraphs.first().text
        val geText = meskeremGe.days.first().entries.first().paragraphs.first().text
        assertTrue("Amharic text preserves wordspace ፡", amText.contains("፡"))
        assertTrue("Amharic text preserves period ።", amText.contains("።"))
        assertTrue("Ge'ez text preserves wordspace ፡", geText.contains("፡"))
        assertTrue("Ge'ez text preserves period ።", geText.contains("።"))
    }
}
