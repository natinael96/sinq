package com.agpeya.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * What the bundle actually carries, against what the reader was showing.
 *
 * The Amharic edition prints headings above verses and cross references under
 * them; the parser read neither, so 22,905 verses' worth of references and
 * every descriptive heading were dropped at load and could not be turned on.
 */
class ScriptureParseTest {

    private val booksDir: File =
        listOf("src/main/assets/content/bible/am-1980/books", "app/src/main/assets/content/bible/am-1980/books")
            .map(::File).first { it.isDirectory }

    private val json = Json { ignoreUnknownKeys = true }

    private fun chapters(file: String) =
        json.parseToJsonElement(File(booksDir, file).readText())
            .jsonObject["chapters"]!!.jsonArray

    @Test
    fun `the edition carries cross references the reader can show`() {
        val luke10 = chapters("61-luke.json").first { it.jsonObject["n"]!!.jsonPrimitive.content == "10" }
        val withRefs = luke10.jsonObject["verses"]!!.jsonArray.count {
            !it.jsonObject["refs"]?.jsonArray.isNullOrEmpty()
        }
        assertTrue("ሉቃስ ፲ carries no references at all", withRefs > 0)
    }

    @Test
    fun `a chapter's own number is not a heading worth printing`() {
        // Every chapter opens with a "ምዕራፍ N" heading. The page already says
        // which chapter it is, so those are the ones the parser drops.
        val kinds = chapters("61-luke.json").flatMap { ch ->
            ch.jsonObject["headings"]?.jsonArray.orEmpty().map {
                it.jsonObject["kind"]!!.jsonPrimitive.content
            }
        }
        assertTrue(kinds.isNotEmpty())
        assertEquals(setOf("major"), kinds.toSet())
    }

    @Test
    fun `descriptive headings exist and are the ones worth keeping`() {
        // ሲኖዶስ opens with a prefatory note rather than with verse one.
        val descriptive = booksDir.listFiles().orEmpty().sumOf { file ->
            runCatching {
                chapters(file.name).sumOf { ch ->
                    ch.jsonObject["headings"]?.jsonArray.orEmpty().count {
                        it.jsonObject["kind"]?.jsonPrimitive?.content != "major"
                    }
                }
            }.getOrDefault(0)
        }
        assertTrue("no non-chapter headings in the whole bundle", descriptive > 0)
    }
}
