package com.agpeya.app.data

import com.agpeya.app.model.GitsaweEntry
import com.agpeya.app.model.GitsaweServices
import com.agpeya.app.model.MonthlyEntry
import com.agpeya.app.model.ScriptureBook
import com.agpeya.app.model.ScriptureChapter
import com.agpeya.app.model.ScriptureVerse
import com.agpeya.app.model.SeasonalEntry
import com.agpeya.app.model.VerseRef
import com.agpeya.app.model.readings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * End-to-end guard for the NT integration: every bundled book decodes, every
 * ግጻዌ book-title variant in the real data resolves (to a psalm or an NT book),
 * and the two worked examples (Luke 4:17-23, 2 Cor 6:1-11) land on real text.
 */
class ScriptureDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val contentDir: File =
        listOf("src/main/assets/content", "app/src/main/assets/content")
            .map(::File).first { it.isDirectory }

    private val bibleDir = File(contentDir, "bible/am-1980")
    private val metas by lazy {
        json.parseToJsonElement(File(bibleDir, "meta.json").readText()).jsonObject["books"]!!.jsonArray
            .map { it.jsonObject }.filter { it["id"]!!.jsonPrimitive.content != "PSA" }
    }

    private fun book(key: String): ScriptureBook {
        val meta = metas.first { it["file"]!!.jsonPrimitive.content.endsWith("-$key.json") }
        val root = json.parseToJsonElement(File(bibleDir, meta["file"]!!.jsonPrimitive.content).readText()).jsonObject
        return ScriptureBook(
            number = meta["order"]!!.jsonPrimitive.content.toInt(), key = key,
            nameAm = meta["name"]!!.jsonPrimitive.content, nameEn = key,
            chapters = root["chapters"]!!.jsonArray.map { cNode ->
                val c = cNode.jsonObject
                ScriptureChapter(c["n"]!!.jsonPrimitive.content.toInt(), c["verses"]!!.jsonArray.mapIndexedNotNull { i, vNode ->
                    val v = vNode.jsonObject
                    val text = v["t"]?.jsonPrimitive?.content ?: return@mapIndexedNotNull null
                    ScriptureVerse(v["n"]?.jsonPrimitive?.content?.toIntOrNull() ?: i + 1, text)
                })
            },
        )
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

    private fun gitsaweTitles(): List<String> =
        allServices().flatMap { it.readings() }.mapNotNull { it.verse?.bookTitle }

    @Test
    fun `Amharic 1980 Bible books decode`() {
        assertEquals(92, metas.size) // 93 source books minus Psalms (owned by its reader)
        for (meta in metas) {
            val key = meta["file"]!!.jsonPrimitive.content.substringAfter('-').removeSuffix(".json")
            val b = book(key)
            assertEquals(meta["chapters"]!!.jsonPrimitive.content.toInt(), b.chapters.size)
            assertTrue("$key has verses", b.chapters.all { it.verses.isNotEmpty() })
        }
    }

    @Test
    fun `every Gitsawe book title resolves to a psalm or an NT book`() {
        val unresolved = gitsaweTitles().toSet().filter { title ->
            !GitsaweLinks.isPsalms(title) && ScriptureRepository.resolveBookKey(title) == null
        }
        assertTrue("these titles resolve to nothing: $unresolved", unresolved.isEmpty())
    }

    @Test
    fun `title variants and typos map to the right book`() {
        val cases = mapOf(
            "ግብረ ሐዋርያት" to "acts",
            "የሐዋርያት ሥራ" to "acts",
            "የማርዎስ ወንጌል" to "mark",          // typo in source
            "ወደ ቆሮንቶስ ሰዎች ፪" to "2-corinthians",
            "፪ና ጴጥሮስ" to "2-peter",
            "፩ኛ ዮሐንስ" to "1-john",
            "የዮሐንስ ራዕይ" to "revelation",
            "የያዕቆብ መልእክት" to "james",
        )
        for ((title, key) in cases) assertEquals(title, key, ScriptureRepository.resolveBookKey(title))
        // Psalms are NOT an NT book — they route to the Psalter.
        assertEquals(null, ScriptureRepository.resolveBookKey("መዝሙረ ዳዊት"))
        assertTrue(GitsaweLinks.isPsalms("መዝሙረ ዳዊት"))
    }

    @Test
    fun `Luke 4 17-23 resolves to real verses`() {
        val luke = book("luke")
        val ch4 = luke.chapters.first { it.chapter == 4 }
        val passage = ch4.verses.filter { it.n in 17..23 }
        assertEquals(7, passage.size)
        assertTrue("v17 mentions Isaiah's scroll", passage.first().text.contains("ኢሳይያስ"))
    }

    @Test
    fun `2 Cor 6 1-11 resolves to real verses`() {
        val second = book("2-corinthians")
        val ch6 = second.chapters.first { it.chapter == 6 }
        val passage = ch6.verses.filter { it.n in 1..11 }
        assertEquals(11, passage.size)
    }

    @Test
    fun `reading targets route psalms and gospels correctly`() {
        // Day-1 msbak: Psalm 64:11-12 -> Psalter section 63.
        val psalm = GitsaweLinks.target(VerseRef(bookTitle = "መዝሙረ ዳዊት", chapter = 64, start = 11, end = 12))
        assertEquals(ReadingTarget.Psalm(64, 63, 11, 12), psalm)
        assertEquals("psalter?section=63&start=11&end=12", GitsaweLinks.chapterRoute(psalm!!))
        assertEquals("psalter", GitsaweLinks.bookRoute(psalm))
        assertEquals(
            "gitsawePassage?psalm=64&start=11&end=12",
            GitsaweLinks.passageRoute(psalm),
        )

        // Day-1 negh gospel: Luke 4:17-23 -> NT passage.
        val gospel = GitsaweLinks.target(VerseRef(bookTitle = "የሉቃስ ወንጌል", chapter = 4, start = 17, end = 23))
        assertEquals(ReadingTarget.NtPassage("luke", 4, 17, 23), gospel)
        assertEquals("scripture/luke/4?start=17&end=23", GitsaweLinks.chapterRoute(gospel!!))
        assertEquals("scripture/luke/1", GitsaweLinks.bookRoute(gospel))
        assertEquals(
            "gitsawePassage?book=luke&chapter=4&start=17&end=23",
            GitsaweLinks.passageRoute(gospel),
        )
    }

    @Test
    fun `a citation range resolves exactly, and open-ended runs to the chapter's end`() {
        val ch4 = book("luke").chapters.first { it.chapter == 4 }
        val lastN = ch4.verses.last().n

        // Explicit start and end: exactly that range.
        val ranged = ScriptureRepository.citedRange(ch4.verses, 17, 23)
        assertEquals(17, ranged.first().n)
        assertEquals(23, ranged.last().n)

        // Start with no end is not incomplete: it reads to the chapter's end.
        val openEnded = ScriptureRepository.citedRange(ch4.verses, 17, null)
        assertEquals(17, openEnded.first().n)
        assertEquals(lastN, openEnded.last().n)

        // No start at all: the whole chapter.
        assertEquals(ch4.verses.size, ScriptureRepository.citedRange(ch4.verses, null, null).size)

        // A citation past the chapter's real end still shows what exists.
        val overCited = ScriptureRepository.citedRange(ch4.verses, lastN + 5, null)
        assertTrue(overCited.isNotEmpty())
    }

    @Test
    fun `all Gitsawe psalm references fall within the bundled 150`() {
        val bad = allServices().flatMap { it.readings() }
            .mapNotNull { it.verse }
            .filter { it.bookTitle != null && GitsaweLinks.isPsalms(it.bookTitle!!) }
            .mapNotNull { it.chapter }
            .filter { it !in 1..150 }
        assertTrue("psalm numbers outside 1..150: $bad", bad.isEmpty())
    }
}
