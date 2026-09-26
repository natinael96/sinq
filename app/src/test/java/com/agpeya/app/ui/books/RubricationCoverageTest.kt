package com.agpeya.app.ui.books

import com.agpeya.app.ui.gitsawe.SYNAXARIUM_CLOSING_STANZAS
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The rubrication rule, run over the text each reader actually renders.
 *
 * The rule itself is covered by RubricationTest on hand-written lines. This is
 * the other half: proof that on the corpus as shipped it reddens something in
 * every surface, and that the scope each reader passes is the one the words
 * need. Without it a reader can be wired to a scope that quietly matches
 * nothing, and the page looks exactly as it did before.
 */
class RubricationCoverageTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val assets: File =
        listOf("src/main/assets/content", "app/src/main/assets/content")
            .map(::File).first { it.isDirectory }

    /** Every string at [field], anywhere in the file's tree. */
    private fun strings(path: String, field: String): List<String> {
        val out = mutableListOf<String>()
        fun collect(node: JsonElement) {
            when (node) {
                is JsonPrimitive -> if (node.isString) out += node.content
                is JsonArray -> node.forEach(::collect)
                else -> Unit
            }
        }
        fun walk(node: JsonElement) {
            when (node) {
                is JsonObject -> node.forEach { (k, v) ->
                    // The field may hold one string or an array of them —
                    // "text" is a paragraph, "verses" is a psalm.
                    if (k == field) collect(v) else walk(v)
                }
                is JsonArray -> node.forEach(::walk)
                else -> Unit
            }
        }
        walk(json.parseToJsonElement(File(assets, path).readText()))
        return out
    }

    private fun redWords(text: String, scope: Rubrication.Scope): List<String> =
        Rubrication.redRanges(text, scope).map { text.substring(it.first, it.last + 1) }

    private fun reddened(lines: List<String>, scope: Rubrication.Scope): Int =
        lines.count { Rubrication.redRanges(it, scope).isNotEmpty() }

    @Test
    fun `the hours redden the Name and leave the Psalms' people in ink`() {
        // ሰዓታት and the Psalter share one reader, so one scope covers both.
        val verses = strings("morning.json", "verses")
        assertTrue("no verses read", verses.size > 50)
        assertTrue("nothing reddened", reddened(verses, Rubrication.Scope.GENERAL) > 10)

        val davidic = verses.first { it.contains("ዳዊት") || it.contains("ሙሴ") }
        val red = redWords(davidic, Rubrication.Scope.GENERAL)
        assertTrue("a person reddened in psalmody: $red", red.none { it == "ዳዊት" || it == "ሙሴ" })
    }

    @Test
    fun `the ስንክሳር reddens the saint it commemorates`() {
        val file = File(assets, "sinksar/am-1.json")
        org.junit.Assume.assumeTrue(file.exists())
        val paragraphs = strings("sinksar/am-1.json", "text")
        assertTrue("no paragraphs read", paragraphs.size > 50)
        val hits = reddened(paragraphs, Rubrication.Scope.SINKSAR)
        assertTrue("only $hits of ${paragraphs.size} paragraphs reddened", hits > paragraphs.size / 4)

        // The scope is the whole point here: the same prose at GENERAL leaves
        // every saint in ink, which is what the ስንክሳር must not do.
        val aboutSaint = paragraphs.first { p ->
            Rubrication.redRanges(p, Rubrication.Scope.SINKSAR).size >
                Rubrication.redRanges(p, Rubrication.Scope.GENERAL).size
        }
        assertTrue(aboutSaint.isNotBlank())
    }

    @Test
    fun `a ማኅሌት order reddens the Name and the saint`() {
        val verses = strings("mahlet/m1.json", "verse")
        assertTrue("no parts read", verses.size > 20)
        assertTrue("nothing reddened", reddened(verses, Rubrication.Scope.MAHLET) > 5)
    }

    @Test
    fun `the ስንክሳር closing prayer reddens its salam opening`() {
        // This replaced a local three-name list that had no salutation rule at
        // all, so the opening is the part worth pinning.
        val salam = SYNAXARIUM_CLOSING_STANZAS.filter { it.geez.startsWith("ሰላም") }
        assertEquals(2, salam.size)
        for (stanza in salam) {
            val red = redWords(stanza.geez, Rubrication.Scope.SINKSAR)
            assertTrue("salam not reddened: $red", red.first().startsWith("ሰላም"))
        }
        // And the names the old three-name list did catch, still caught.
        val named = SYNAXARIUM_CLOSING_STANZAS.first { it.geez.contains("ኢየሱስ ክርስቶስ") }
        val red = redWords(named.geez, Rubrication.Scope.SINKSAR)
        // Two spans, not one: ኢየሱስ and ክርስቶስ match separately and the space
        // between them is not joined. No visible difference — a space has no
        // colour — but it is why this asserts the words, not the phrase.
        assertTrue("ኢየሱስ not reddened: $red", red.any { it.contains("ኢየሱስ") })
        assertTrue("ክርስቶስ not reddened: $red", red.any { it.contains("ክርስቶስ") })
        assertTrue("ማርያም not reddened: $red", red.any { it.contains("ማርያም") })
    }

    @Test
    fun `GENERAL never reddens a saint, in any scope-free surface`() {
        // The one invariant the whole scope split rests on.
        val samples = listOf(
            "ወተንሥአ ጴጥሮስ ወሖረ ምስለ ዮሐንስ",
            "ሙሴ ወአሮን በካህናቲሁ",
            "ወነበረ ዳዊት ውስተ ቤቱ",
        )
        for (line in samples) {
            assertEquals(emptyList<String>(), redWords(line, Rubrication.Scope.GENERAL))
        }
    }
}
