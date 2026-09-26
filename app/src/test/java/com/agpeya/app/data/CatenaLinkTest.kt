package com.agpeya.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Holds the Catena mapping to the bundled Bible.
 *
 * The two things that can silently rot are the book list — a book added to the
 * canon quietly getting no link — and the Psalter table, which is derived from
 * the bundled psalms and would be wrong if that file were ever renumbered.
 */
class CatenaLinkTest {

    private fun find(vararg rel: String): File =
        rel.map(::File).firstOrNull { it.isFile }
            ?: rel.map { File("app/$it") }.first { it.isFile }

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `the psalm table matches the numbers the bundled Psalter carries`() {
        // Every heading of the form መዝሙር ፶ (፶፩) names the Masoretic number in
        // the parentheses. Where the Psalter says one thing and the table says
        // another, the Psalter wins and this fails.
        val psalms = json.parseToJsonElement(
            find("src/main/assets/content/bible/am-1980/books/19-psalms.json").readText(),
        ).jsonObject["chapters"]!!.jsonArray

        val pattern = Regex("""መዝሙር\s+([፩-፼]+)\s*\(\s*([፩-፼]+)\s*\)""")
        var checked = 0
        for (chapter in psalms) {
            val n = chapter.jsonObject["n"]!!.jsonPrimitive.content.toInt()
            val headings = chapter.jsonObject["headings"] as? JsonArray ?: continue
            val alt = headings.firstNotNullOfOrNull { h ->
                pattern.find((h as JsonObject)["text"]?.jsonPrimitive?.content.orEmpty())
                    ?.groupValues?.get(2)?.let(::geezNumber)
            } ?: continue
            checked++
            assertEquals("psalm $n", alt, CatenaLink.masoreticPsalm(n))
        }
        assertEquals("the Psalter should carry 140 alternate numbers", 140, checked)
    }

    @Test
    fun `the ten psalms without an alternate number are the ones that need none`() {
        // ፩–፰ agree in both numberings; ፻፲፭ and ፻፵፯ are the second half of a
        // psalm the Masoretic text joins to the one before, so they share its
        // number rather than having one of their own.
        for (n in 1..8) assertEquals(n, CatenaLink.masoreticPsalm(n))
        assertEquals(CatenaLink.masoreticPsalm(114), CatenaLink.masoreticPsalm(115))
        assertEquals(CatenaLink.masoreticPsalm(146), CatenaLink.masoreticPsalm(147))
    }

    @Test
    fun `the Psalter link is shifted and nothing else is`() {
        // The shepherd psalm: ፳፪ here, 23 there. Getting this one wrong sends a
        // reader to "why hast thou forsaken me".
        assertEquals("https://catenabible.com/ps/23/1", CatenaLink.url("psalms", 22, 1))
        assertEquals("https://catenabible.com/mt/5/3", CatenaLink.url("matthew", 5, 3))
        assertEquals("https://catenabible.com/gn/1/1", CatenaLink.url("genesis", 1, 1))
        // Jeremiah follows the Masoretic order in this Bible, so it is not shifted.
        assertEquals("https://catenabible.com/jer/31/31", CatenaLink.url("jeremiah", 31, 31))
    }

    @Test
    fun `books Catena does not carry get no link rather than a wrong one`() {
        for (id in listOf("enoch", "kufale", "3-maccabees", "ezra-kalie", "susanna",
                          "teref-daniel", "esther-greek", "seleste-dekik", "josippon",
                          "1-clement", "didascalia", "tizaz", "abtilis", "gitsew",
                          "sirate-tsion", "1-covenant", "2-covenant")) {
            assertNull("$id should have no Catena page", CatenaLink.url(id, 1, 1))
            assertTrue("$id should not report coverage", !CatenaLink.covers(id))
        }
    }

    @Test
    fun `every book the app bundles either maps or is knowingly excluded`() {
        val canon = json.parseToJsonElement(
            find("src/main/assets/content/bible/canon.json").readText(),
        ) as JsonArray
        val meta = json.parseToJsonElement(
            find("src/main/assets/content/bible/am-1980/meta.json").readText(),
        ).jsonObject["books"]!!.jsonArray
        val bundled = meta.map { it.jsonObject["id"]!!.jsonPrimitive.content }.toSet()

        val mapped = canon.count { c ->
            val o = c.jsonObject
            o["id"]!!.jsonPrimitive.content in bundled &&
                CatenaLink.covers(o["slug"]!!.jsonPrimitive.content)
        }
        assertEquals("books mapped to Catena", 76, mapped)
        // Every mapped book must produce a URL for its own first verse.
        canon.forEach { c ->
            val o = c.jsonObject
            val slug = o["slug"]!!.jsonPrimitive.content
            if (o["id"]!!.jsonPrimitive.content in bundled && CatenaLink.covers(slug)) {
                assertNotNull(CatenaLink.url(slug, 1, 1))
            }
        }
    }

    private fun geezNumber(s: String): Int {
        var total = 0
        var run = 0
        for (c in s) {
            val v = when (c) {
                '፩' -> 1; '፪' -> 2; '፫' -> 3; '፬' -> 4; '፭' -> 5
                '፮' -> 6; '፯' -> 7; '፰' -> 8; '፱' -> 9
                '፲' -> 10; '፳' -> 20; '፴' -> 30; '፵' -> 40; '፶' -> 50
                '፷' -> 60; '፸' -> 70; '፹' -> 80; '፺' -> 90; '፻' -> 100
                else -> 0
            }
            if (v == 100) { run = (if (run == 0) 1 else run) * 100; total += run; run = 0 } else run += v
        }
        return total + run
    }
}
