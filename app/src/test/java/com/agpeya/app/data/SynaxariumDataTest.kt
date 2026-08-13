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
    private val dir: File =
        listOf("src/main/assets/content/sinksar", "app/src/main/assets/content/sinksar")
            .map(::File).first { it.isDirectory }

    @Test
    fun `all 13 months decode with the expected totals`() {
        val manifest: SynaxariumManifest =
            json.decodeFromString(File(dir, "manifest.json").readText())
        assertEquals(13, manifest.months.size)

        var days = 0
        var entries = 0
        for (m in 1..13) {
            val month: SynaxariumMonth = json.decodeFromString(File(dir, "$m.json").readText())
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
        assertEquals(2308, entries)
    }

    /**
     * A month must cover 1..n with no gap and no repeat: a duplicate shadows a
     * day, and a hole leaves the reader on that date with nothing at all.
     */
    @Test
    fun `every month covers its days exactly once`() {
        for (m in 1..13) {
            val month: SynaxariumMonth = json.decodeFromString(File(dir, "$m.json").readText())
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
        val meskerem: SynaxariumMonth = json.decodeFromString(File(dir, "1.json").readText())
        val day1 = meskerem.days.first { it.day == 1 }
        assertTrue(day1.entries.isNotEmpty())
        assertTrue("first entry has narrative text", day1.entries.first().text.length > 50)
    }
}
