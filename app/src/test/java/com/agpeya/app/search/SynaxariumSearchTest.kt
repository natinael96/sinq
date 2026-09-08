package com.agpeya.app.search

import com.agpeya.app.model.SynaxariumMonth
import java.io.File
import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** End-to-end search guards over every bundled ስንክሳር entry. */
class SynaxariumSearchTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val contentDir: File =
        listOf("src/main/assets/content", "app/src/main/assets/content")
            .map(::File).first { it.isDirectory }
    private val labels = AmharicSearch.Labels(
        psalter = "መዝሙር",
        psalterGeez = "መዝሙር · ግዕዝ",
        scripture = "መጽሐፍ ቅዱስ",
        synaxarium = "ስንክሳር",
        wudase = "ውዳሴ ማርያም",
    )

    // 2015 E.C. has Pagume 6, so every valid bundled day receives a route.
    private val docs by lazy {
        (1..13).flatMap { month ->
            val data = json.decodeFromString<SynaxariumMonth>(
                File(contentDir, "sinksar/am-$month.json").readText(),
            )
            AmharicSearch.synaxariumDocs(month, 2015, data.days)
        }
    }

    private fun search(query: String) = AmharicSearch.searchDocs(
        docs = docs,
        needle = AmharicSearch.fold(query.trim()),
        matchLen = query.trim().length,
        labels = labels,
    )

    @Test
    fun `every Sinkisar entry has a unique result identity and lands on its own entry`() {
        assertTrue(docs.isNotEmpty())
        assertEquals(docs.size, docs.map { it.targetId }.toSet().size)
        docs.forEachIndexed { index, doc ->
            assertEquals(AmharicSearch.Source.SYNAXARIUM, doc.source)
            // "synaxarium/<epochDay>?entry=<ordinal>" — the day to open and the
            // commemoration inside it, both of which MainActivity must accept.
            val (day, entry) = doc.route.removePrefix("synaxarium/").split("?entry=")
            LocalDate.ofEpochDay(day.toLong())
            assertTrue("entry ordinal missing at $index", entry.toInt() >= 0)
            // The ordinal is the one the identity records.
            assertEquals(doc.targetId.substringAfterLast('-'), entry)
        }
    }

    @Test
    fun `normal Amharic and Geez phrases find renderable Sinkisar results`() {
        for (query in listOf("ቅዱስ", "ሰላም ለ")) {
            val results = search(query)
            assertTrue("no results for $query", results.isNotEmpty())
            results.forEach { result ->
                assertEquals(AmharicSearch.Source.SYNAXARIUM, result.source)
                assertTrue(result.snippetMatchStart >= 0)
                assertTrue(result.snippetMatchStart + result.snippetMatchLen <= result.snippet.length)
                assertTrue(result.route?.startsWith("synaxarium/") == true)
            }
        }
    }

    @Test
    fun `empty and absent searches return no Sinkisar results`() {
        assertTrue(search("").isEmpty())
        assertTrue(search("definitely-not-in-the-synaxarium-987654321").isEmpty())
    }

    @Test
    fun `special Ethiopic punctuation is safe to search and highlight`() {
        val results = search("፡፡")
        assertTrue(results.isNotEmpty())
        results.forEach { result ->
            assertTrue(result.snippetMatchStart + result.snippetMatchLen <= result.snippet.length)
        }
    }
}
