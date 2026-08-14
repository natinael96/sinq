package com.agpeya.app.data

import com.agpeya.app.model.SeatatContent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the bundled ሰዓታት (generated from tools/seatat-source.json by
 * tools/build_seatat.py): the file decodes, every section is addressable
 * (unique id, a title), and every line keeps the paired-text contract —
 * at least one half present, the Amharic never a stray copy of the Ge'ez.
 */
class SeatatDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val contentDir: File =
        listOf("src/main/assets/content", "app/src/main/assets/content")
            .map(::File).first { it.isDirectory }

    private fun content(): SeatatContent =
        json.decodeFromString(File(contentDir, "seatat/seatat.json").readText())

    @Test
    fun `seatat decodes as the full night-and-dawn office`() {
        val c = content()
        // መቅድም + the 42 sections of the printed book, in printed order.
        assertEquals(43, c.sections.size)
        assertEquals(c.sections.size, c.sections.map { it.id }.distinct().size)
        assertEquals("meqdim", c.sections.first().id)
        assertEquals("introductory_prayer", c.sections[1].id)
        assertEquals("concluding_doxology_and_colophon", c.sections.last().id)
        for (s in c.sections) {
            assertTrue("${s.id} has a title", s.titleGe.isNotBlank() || s.titleAm.isNotBlank())
            assertTrue("${s.id} has lines", s.lines.isNotEmpty())
        }
    }

    @Test
    fun `every line carries text and an honest pairing`() {
        var geezLines = 0
        for (s in content().sections) {
            s.lines.forEachIndexed { i, line ->
                assertTrue("${s.id}[$i] has text", line.ge.isNotBlank() || line.am.isNotBlank())
                if (line.ge.isNotBlank()) geezLines++
                // The one-to-one pairing lives in the line itself: an Amharic
                // half, when present with Ge'ez, may not be a stray copy.
                if (line.ge.isNotBlank() && line.am.isNotBlank()) {
                    assertTrue("${s.id}[$i] am differs from ge", line.am != line.ge)
                }
            }
        }
        // The book is Ge'ez-first: only the መቅድም is Amharic-only prose.
        assertTrue("most lines are Ge'ez-first ($geezLines)", geezLines >= 190)
    }

    @Test
    fun `typo-review flags only ever trail a word`() {
        // A `*` marks a word as needing review; it renders red in the reader
        // and is stripped from shares/search. It must never open a line.
        for (s in content().sections) {
            s.lines.forEach { line ->
                for (text in listOf(line.ge, line.am)) {
                    assertTrue("flag never starts a line", !text.startsWith("*"))
                }
            }
        }
    }
}
