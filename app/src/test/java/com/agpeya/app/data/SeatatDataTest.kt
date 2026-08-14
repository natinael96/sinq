package com.agpeya.app.data

import com.agpeya.app.model.SeatatContent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the bundled ሰዓታት: the file decodes, every section is addressable
 * (unique id, chip label, Ge'ez title), and every line keeps the Ge'ez-first
 * contract — Ge'ez never blank, the paired Amharic attached to its own line.
 */
class SeatatDataTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val contentDir: File =
        listOf("src/main/assets/content", "app/src/main/assets/content")
            .map(::File).first { it.isDirectory }

    private fun content(): SeatatContent =
        json.decodeFromString(File(contentDir, "seatat/seatat.json").readText())

    @Test
    fun `seatat decodes with the four hours`() {
        val c = content()
        assertEquals(listOf("negh", "ketr", "serk", "lelit"), c.sections.map { it.id })
        assertEquals(c.sections.size, c.sections.map { it.id }.distinct().size)
        for (s in c.sections) {
            assertTrue("${s.id} has a chip label", s.label.isNotBlank())
            assertTrue("${s.id} has a Ge'ez title", s.titleGe.isNotBlank())
            assertTrue("${s.id} has lines", s.lines.isNotEmpty())
        }
    }

    @Test
    fun `every line is Ge'ez-first with its own translation`() {
        for (s in content().sections) {
            s.lines.forEachIndexed { i, line ->
                assertTrue("${s.id}[$i] Ge'ez never blank", line.ge.isNotBlank())
                // The one-to-one pairing lives in the line itself: an Amharic
                // half, when present, may not be a stray copy of the Ge'ez.
                if (line.am.isNotBlank()) {
                    assertTrue("${s.id}[$i] am differs from ge", line.am != line.ge)
                }
            }
        }
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
