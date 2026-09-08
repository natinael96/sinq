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
 * Reading the edition's printed cross references.
 *
 * The parser has to hold two conventions the printed page takes for granted:
 * a reference with no book of its own belongs to the book before it, and the
 * numbers are Arabic even though every number the app prints is Ge'ez.
 */
class CrossReferenceTest {

    private val names = mapOf(
        "matthew" to "የማቴዎስ ወንጌል",
        "john" to "የዮሐንስ ወንጌል",
        "2-timothy" to "2ኛ ወደ ጢሞቴዎስ",
        "acts" to "የሐዋርያት ሥራ",
    )

    @Test
    fun `a reference with no book belongs to the one before it`() {
        val refs = CrossReference.parse("2ጢሞ 4፥5፤ 4፥17፤ የሐዋ 3፥18።", names)
        assertEquals(3, refs.size)
        assertEquals(listOf("2-timothy", "2-timothy", "acts"), refs.map { it.bookKey })
        assertEquals(listOf(5, 17, 18), refs.map { it.verse })
    }

    @Test
    fun `a reference opens the verse, not the chapter`() {
        val ref = CrossReference.parse("ማቴዎ 9፥37", names).single()
        assertEquals("scripture/matthew/9?start=37&end=37", ref.route)
        assertEquals("የማቴዎስ ወንጌል", ref.bookName)
    }

    @Test
    fun `what cannot be read is dropped rather than guessed at`() {
        assertTrue(CrossReference.parse("", names).isEmpty())
        assertTrue(CrossReference.parse("ወንጌል", names).isEmpty())
        // No book has ever been named, so a bare verse has nothing to attach to.
        assertTrue(CrossReference.parse("4፥17", names).isEmpty())
    }

    // ── against the bundle itself ────────────────────────────────────────────

    private val booksDir: File =
        listOf("src/main/assets/content/bible/am-1980/books", "app/src/main/assets/content/bible/am-1980/books")
            .map(::File).first { it.isDirectory }

    @Test
    fun `every abbreviation the edition uses resolves to a book`() {
        val json = Json { ignoreUnknownKeys = true }
        var atoms = 0
        var parsed = 0
        booksDir.listFiles().orEmpty().sortedBy { it.name }.forEach { file ->
            json.parseToJsonElement(file.readText()).jsonObject["chapters"]!!.jsonArray.forEach { ch ->
                ch.jsonObject["verses"]!!.jsonArray.forEach { v ->
                    v.jsonObject["refs"]?.jsonArray.orEmpty().forEach { r ->
                        val target = r.jsonObject["target"]?.jsonPrimitive?.content.orEmpty()
                        atoms += target.split('፤').count { it.isNotBlank() }
                        parsed += CrossReference.parse(target).size
                    }
                }
            }
        }
        assertTrue("no references found at all", atoms > 50_000)
        // Ranges and comma lists are the remainder; they are dropped on purpose.
        val share = parsed * 100 / atoms
        assertTrue("only $share% of $atoms references parsed", share >= 95)
    }
}
