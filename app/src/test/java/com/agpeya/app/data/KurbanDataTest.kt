package com.agpeya.app.data

import com.agpeya.app.model.KurbanContent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the bundled ቁርባን preparation content: rules and prayers are whole. */
class KurbanDataTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val file: File =
        listOf(
            "src/main/assets/content/kurban/kurban.json",
            "app/src/main/assets/content/kurban/kurban.json",
        ).map(::File).first { it.isFile }

    private val content: KurbanContent by lazy { json.decodeFromString(file.readText()) }

    @Test
    fun `decodes with checklist and prayers on both sides`() {
        assertTrue(content.checklist.isNotEmpty())
        assertTrue(content.prePrayers.isNotEmpty())
        assertTrue(content.postPrayers.isNotEmpty())
    }

    @Test
    fun `ids are unique across the checklist and prayers`() {
        val ids = content.checklist.map { it.id } +
            (content.prePrayers + content.postPrayers).map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.isNotBlank() })
    }

    @Test
    fun `every rule and prayer carries its text`() {
        for (item in content.checklist) {
            assertTrue("${item.id} title", item.title.isNotBlank())
            assertTrue("${item.id} detail", item.detail.isNotBlank())
        }
        for (prayer in content.prePrayers + content.postPrayers) {
            assertTrue("${prayer.id} title", prayer.title.isNotBlank())
            assertTrue("${prayer.id} body", prayer.body.isNotBlank())
        }
    }
}
