package com.agpeya.app.data

import com.agpeya.app.model.GitsaweEntry
import com.agpeya.app.model.GitsaweServices
import com.agpeya.app.model.MonthlyEntry
import com.agpeya.app.model.SeasonalEntry
import com.agpeya.app.model.VerseRef
import com.agpeya.app.model.readings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Coverage guard: how many of the REAL Gitsawe reading references actually reach
 * a bundled page. Distinguishes a linkable reference (its psalm/chapter exists —
 * the verse range is only scroll emphasis) from a dead one (book/chapter absent).
 *
 * Locks two facts so regressions surface: linkability stays ≥ 99.5%, and the set
 * of genuinely dead references is exactly the 10 known bad citations in the
 * upstream data (impossible chapters like "Hebrews 15" / "Acts 76").
 */
class GitsaweLinkCoverageTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val contentDir: File =
        listOf("src/main/assets/content", "app/src/main/assets/content")
            .map(::File).first { it.isDirectory }

    // Psalm number -> verse count (from the bundled Psalter).
    private val psalmVerses: Map<Int, Int> by lazy {
        val root = json.parseToJsonElement(File(contentDir, "bible/am-1980/books/19-psalms.json").readText()).jsonObject
        root["chapters"]!!.jsonArray.map { it.jsonObject }.filter { it["n"]!!.jsonPrimitive.content.toInt() in 1..150 }.associate { o ->
            o["n"]!!.jsonPrimitive.content.toInt() to o["verses"]!!.jsonArray.size
        }
    }

    // Book key -> set of chapter numbers present (from the bundled NT).
    private val bookChapters: Map<String, Set<Int>> by lazy {
        contentDir.resolve("bible/am-1980/books").listFiles { f -> f.name.endsWith(".json") && !f.name.endsWith("-psalms.json") }!!
            .associate { f ->
                val root = json.parseToJsonElement(f.readText()).jsonObject
                f.name.substringAfter('-').removeSuffix(".json") to root["chapters"]!!.jsonArray.map { it.jsonObject["n"]!!.jsonPrimitive.content.toInt() }.toSet()
            }
    }

    private fun allServices(): List<GitsaweServices> {
        val out = ArrayList<GitsaweServices>()
        out += json.decodeFromString(ListSerializer(GitsaweEntry.serializer()),
            File(contentDir, "gitsawe/daily-gitsawe.json").readText())
        out += json.decodeFromString(ListSerializer(SeasonalEntry.serializer()),
            File(contentDir, "gitsawe/seasonal-gitsawe.json").readText())
        out += json.decodeFromString(ListSerializer(MonthlyEntry.serializer()),
            File(contentDir, "gitsawe/monthly-gitsawe.json").readText())
        return out
    }

    private fun allRefs(): List<VerseRef> =
        allServices().flatMap { it.readings() }.mapNotNull { it.verse }

    /** True when the reference can open a real page (psalm/chapter exists). */
    private fun isLinkable(v: VerseRef): Boolean {
        val title = v.bookTitle ?: return false
        val ch = v.chapter ?: return false
        return if (GitsaweLinks.isPsalms(title)) {
            ch in psalmVerses
        } else {
            val key = ScriptureRepository.resolveBookKey(title) ?: return false
            ch in (bookChapters[key] ?: emptySet())
        }
    }

    @Test
    fun `at least 99 percent of references are linkable`() {
        val refs = allRefs()
        val linkable = refs.count { isLinkable(it) }
        val pct = 100.0 * linkable / refs.size
        assertTrue("only $linkable/${refs.size} (${"%.1f".format(pct)}%) linkable", pct >= 99.5)
    }

    @Test
    fun `dead references are exactly the known-bad upstream citations`() {
        val dead = allRefs()
            .filterNot { isLinkable(it) }
            .map { v -> "${ScriptureRepository.resolveBookKey(v.bookTitle!!) ?: v.bookTitle} ${v.chapter}" }
            .toSortedSet()
        val expected = sortedSetOf(
            "hebrews 15", "acts 76", "2-corinthians 14", "ephesians 8", "ephesians 7",
            "mark 17", "2-john 2", "2-peter 5", "john 24",
        )
        // 'ephesians 7' appears twice in the data but collapses to one distinct citation.
        assertEquals(expected, dead)
    }
}
