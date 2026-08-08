package com.agpeya.app.data

import com.agpeya.app.model.WudaseContent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the bundled ውዳሴ ማርያም: the asset decodes and every section is whole. */
class WudaseDataTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val file: File =
        listOf("src/main/assets/content/wudase/wudase.json", "app/src/main/assets/content/wudase/wudase.json")
            .map(::File).first { it.isFile }

    private val content: WudaseContent by lazy { json.decodeFromString(file.readText()) }

    @Test
    fun `decodes with all ten sections`() {
        assertEquals(10, content.sections.size)
        assertTrue(content.attribution.isNotBlank())
        // The daily prayer opens the book, then the seven weekday portions in order.
        assertEquals("daily", content.sections.first().id)
        assertEquals((1..7).toList(), content.sections.filter { it.weekday in 1..7 }.map { it.weekday })
    }

    @Test
    fun `every section has titles and stanzas in both languages`() {
        for (s in content.sections) {
            assertTrue("${s.id} titleAm", s.titleAm.isNotBlank())
            assertTrue("${s.id} titleGe", s.titleGe.isNotBlank())
            assertTrue("${s.id} am stanzas", s.am.isNotEmpty())
            assertTrue("${s.id} ge stanzas", s.ge.isNotEmpty())
        }
    }

    @Test
    fun `stanzas are clean of source escape artifacts`() {
        for (s in content.sections) {
            for (stanza in s.am + s.ge) {
                assertTrue("${s.id} has raw backslash", '\\' !in stanza)
                assertTrue("${s.id} has tab/newline", '\t' !in stanza && '\n' !in stanza)
            }
        }
    }

    @Test
    fun `monday opens with the first numbered stanza in both languages`() {
        val monday = content.sections.first { it.id == "monday" }
        assertTrue(monday.am.first().startsWith("፩."))
        assertTrue(monday.ge.first().startsWith("፩."))
    }
}
