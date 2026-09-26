package com.agpeya.app.data

import com.agpeya.app.model.ExaminationContent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the bundled ንስሐ examination and the confession-draft body builder. */
class NisihaDataTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val file: File =
        listOf(
            "src/main/assets/content/nisiha/examination.json",
            "app/src/main/assets/content/nisiha/examination.json",
        ).map(::File).first { it.isFile }

    private val content: ExaminationContent by lazy { json.decodeFromString(file.readText()) }

    // The examination ships empty on purpose: its text is the Church's to give,
    // and the screen shows "coming soon" until it is written. These tests
    // therefore guard the SHAPE, and hold whether or not content is present —
    // so they keep their value on the day the questions land.

    @Test
    fun `decodes, whether or not the examination has been written yet`() {
        assertTrue(content.contentVersion >= 1)
    }

    @Test
    fun `any section present is unique, titled, and has questions`() {
        assertEquals(content.sections.size, content.sections.map { it.id }.toSet().size)
        for (s in content.sections) {
            assertTrue("${s.id} title", s.title.isNotBlank())
            assertTrue("${s.id} questions", s.questions.isNotEmpty())
            assertTrue("${s.id} blank question", s.questions.all { it.isNotBlank() })
        }
    }

    @Test
    fun `body carries only the sections that were written under`() {
        val sections = listOf(
            com.agpeya.app.model.ExaminationSection(id = "a", title = "አንድ", questions = listOf("?")),
            com.agpeya.app.model.ExaminationSection(id = "b", title = "ሁለት", questions = listOf("?")),
        )
        val notes = mapOf("a" to "የግል ማስታወሻ", "no-such-section" to "x")
        val body = NisihaRepository.buildConfessionBody(sections, notes, "ተመርምሯል")
        assertTrue(body.startsWith("ተመርምሯል"))
        assertTrue("አንድ" in body)
        assertTrue("የግል ማስታወሻ" in body)
        assertFalse("unnoted section leaked", "ሁለት" in body)
    }

    @Test
    fun `body is never blank even with nothing written`() {
        val body = NisihaRepository.buildConfessionBody(content.sections, emptyMap(), "ተመርምሯል")
        assertEquals("ተመርምሯል", body)
    }

    @Test
    fun `a whitespace-only note counts as nothing written`() {
        val sections = listOf(
            com.agpeya.app.model.ExaminationSection(id = "a", title = "አንድ", questions = listOf("?")),
        )
        val notes = mapOf("a" to "   ")
        val body = NisihaRepository.buildConfessionBody(sections, notes, "ተመርምሯል")
        assertEquals("ተመርምሯል", body)
    }
}
