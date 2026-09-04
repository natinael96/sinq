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

    @Test
    fun `decodes with an intro and sections`() {
        assertTrue(content.intro.isNotBlank())
        assertTrue(content.sections.isNotEmpty())
    }

    @Test
    fun `section ids are unique and titled, and none is empty of questions`() {
        assertEquals(content.sections.size, content.sections.map { it.id }.toSet().size)
        for (s in content.sections) {
            assertTrue("${s.id} title", s.title.isNotBlank())
            assertTrue("${s.id} questions", s.questions.isNotEmpty())
            assertTrue("${s.id} blank question", s.questions.all { it.isNotBlank() })
        }
    }

    @Test
    fun `body carries only the sections that were written under`() {
        val notes = mapOf(content.sections.first().id to "የግል ማስታወሻ", "no-such-section" to "x")
        val body = NisihaRepository.buildConfessionBody(content.sections, notes, "ተመርምሯል")
        assertTrue(body.startsWith("ተመርምሯል"))
        assertTrue(content.sections.first().title in body)
        assertTrue("የግል ማስታወሻ" in body)
        for (other in content.sections.drop(1)) {
            assertFalse("${other.id} leaked", other.title in body)
        }
    }

    @Test
    fun `body is never blank even with nothing written`() {
        val body = NisihaRepository.buildConfessionBody(content.sections, emptyMap(), "ተመርምሯል")
        assertEquals("ተመርምሯል", body)
    }

    @Test
    fun `a whitespace-only note counts as nothing written`() {
        val notes = mapOf(content.sections.first().id to "   ")
        val body = NisihaRepository.buildConfessionBody(content.sections, notes, "ተመርምሯል")
        assertEquals("ተመርምሯል", body)
    }
}
