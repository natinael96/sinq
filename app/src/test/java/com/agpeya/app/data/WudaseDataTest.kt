package com.agpeya.app.data

import com.agpeya.app.model.WudaseContent
import com.agpeya.app.ui.books.Rubrication
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
    fun `the daily prayer is the supplied Amharic, heading and all`() {
        val daily = content.sections.first { it.id == "daily" }
        // Eleven numbered stanzas with ጸሎተ ሃይማኖት's heading standing between the
        // fifth and the sixth, where the print sets it.
        assertEquals(12, daily.am.size)
        assertEquals("የሃይማኖት መሠረት /ጸሎተ ሃይማኖት/", daily.am[5])
        assertTrue(daily.am.last().startsWith("፲፩."))
        // The portion's own name is printed above the text by the screen, so it
        // must not also open the text.
        assertTrue(daily.am.none { it == daily.titleAm })
    }

    @Test
    fun `no Amharic stanza carries an extraction spacing artefact`() {
        // A space dropped inside a word splits the Name of God in two, which is
        // both wrong to read and invisible to the rubrication rule.
        val artefacts = listOf("እግዚአብሔር ን", "ክርስቶስ ን", "ክርስቶስ ም", "ከ ኢየሱስ", "የ ኢየሱስ")
        for (s in content.sections) {
            for (stanza in s.am) {
                for (bad in artefacts) {
                    assertTrue("${s.id} carries ${'"'}$bad${'"'}", !stanza.contains(bad))
                }
            }
        }
    }

    @Test
    fun `no stanza has swallowed the next one's numeral`() {
        // The supplied Amharic arrived with four stanzas run into the one
        // before them — ቅዳሜ's ፪ inside ፩, and three in አንቀጸ ብርሃን — which left
        // those portions reading a stanza short of their own numbering. Split
        // in the source; this is the guard that they stay split.
        val marker = Regex("\\s[\u1369-\u137C]+[.።፤፣]\\s")
        for (s in content.sections) {
            for (stanza in s.am) {
                val body = stanza.substringAfter(". ", stanza)
                assertTrue("${s.id}: ${stanza.take(40)}", !marker.containsMatchIn(body))
            }
        }
    }

    @Test
    fun `ቅዳሜ and አንቀጸ ብርሃን are whole`() {
        // The two portions the split restored, at the lengths their numbering
        // implies: ፩…፲ and ፩…፲፮.
        assertEquals(10, content.sections.first { it.id == "saturday" }.am.size)
        assertEquals(16, content.sections.first { it.id == "anqetse_birhan" }.am.size)
    }

    @Test
    fun `ይዌድስዋ መላእክት keeps the clause-separated text`() {
        // The supplied ውዳሴ ማርያም file carries this portion too, and it is
        // deliberately not used: the shipped one has the praise clauses split
        // on ፤ as the book prints them.
        val y = content.sections.first { it.id == "yiwedsewa_melaekt" }
        assertEquals(4, y.am.size)
        assertTrue(y.am.any { it.contains("ምስጋና ይገባሻል፤") })
    }

    @Test
    fun `the rubrication rule reddens the Name throughout both editions`() {
        // What the reader actually renders, run over the shipped text: every
        // portion reddens in both editions, and the Name is what reddens.
        for (s in content.sections) {
            for (edition in listOf("am" to s.am, "ge" to s.ge)) {
                val reddened = edition.second.sumOf {
                    Rubrication.redRanges(it, Rubrication.Scope.GENERAL).size
                }
                assertTrue("${s.id} ${edition.first} has no red", reddened > 0)
            }
        }
    }

    @Test
    fun `saints named in passing stay in ink`() {
        // ውዳሴ ማርያም is not a መልክእ, a ስንክሳር entry or a ማኅሌት order, so GENERAL is
        // the scope and the saints it mentions are ordinary narrative. ማርያም is
        // the exception and is meant to be: it is on the divine-name list.
        val stanza = content.sections.first { it.id == "friday" }.am
            .first { it.contains("ገብርኤል") }
        val red = Rubrication.redRanges(stanza, Rubrication.Scope.GENERAL)
            .map { stanza.substring(it.first, it.last + 1) }
        assertTrue("ገብርኤል reddened: $red", red.none { it.contains("ገብርኤል") })
        assertTrue("ማርያም not reddened: $red", red.any { it.contains("ማርያም") })
        assertTrue("እግዚአብሔር not reddened: $red", red.any { it.contains("እግዚአብሔር") })
    }

    @Test
    fun `monday opens with the first numbered stanza in both languages`() {
        val monday = content.sections.first { it.id == "monday" }
        assertTrue(monday.am.first().startsWith("፩."))
        assertTrue(monday.ge.first().startsWith("፩."))
    }
}
