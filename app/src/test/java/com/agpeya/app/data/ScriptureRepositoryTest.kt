package com.agpeya.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ScriptureRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val contentDir: File =
        listOf("src/main/assets/content", "app/src/main/assets/content")
            .map(::File).first { it.isDirectory }

    private val englishDir = File(contentDir, "bible/en-nkjv/books")

    @Test
    fun `English canon keys are recognized`() {
        assertTrue(ScriptureRepository.hasEnglish("genesis"))
        assertTrue(ScriptureRepository.hasEnglish("psalms"))
        assertTrue(ScriptureRepository.hasEnglish("matthew"))
        assertTrue(ScriptureRepository.hasEnglish("revelation"))
        assertFalse(ScriptureRepository.hasEnglish("tobit"))
        assertFalse(ScriptureRepository.hasEnglish("yodit"))
    }

    @Test
    fun `English NKJV bundled books exist and decode`() {
        assertTrue("en-nkjv books directory exists", englishDir.isDirectory)
        val files = englishDir.listFiles { _, name -> name.endsWith(".json") }
        assertEquals("66 canonical books bundled in en-nkjv", 66, files?.size)

        val genesis = File(englishDir, "01-genesis.json")
        assertTrue(genesis.exists())
        val genJson = json.parseToJsonElement(genesis.readText()).jsonObject
        assertEquals("genesis", genJson["book"]?.jsonPrimitive?.content)
        val genChapters = genJson["chapters"]!!.jsonArray
        assertEquals(50, genChapters.size)

        val psalms = File(englishDir, "19-psalms.json")
        assertTrue(psalms.exists())
        val psJson = json.parseToJsonElement(psalms.readText()).jsonObject
        val psChapters = psJson["chapters"]!!.jsonArray
        assertEquals(150, psChapters.size)
    }
}
