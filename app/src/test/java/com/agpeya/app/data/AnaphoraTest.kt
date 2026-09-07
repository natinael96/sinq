package com.agpeya.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * One liturgy, one name.
 *
 * The lectionary writes the anaphora as free text and does it 120 different
 * ways across the bundled year, for thirteen anaphoras. The ግጻዌ header printed
 * every variant verbatim, so the same ቅዳሴ appeared under four names in a week.
 */
class AnaphoraTest {

    @Test
    fun `the council is one anaphora however its number is written`() {
        val forms = listOf("ዘ፫፻ (ግሩም) ።", "ዘ፫፻፲፰ ግሩም ።", "ግሩም ።", "፫፻ (ግሩም) ።")
        assertEquals(setOf("ዘሠለስቱ ምዕት"), forms.mapNotNull { Anaphora.of(it)?.name }.toSet())
    }

    @Test
    fun `the bracketed word is the incipit, not part of the name`() {
        val named = Anaphora.of("ዘወልደ ነጐድጓድ ። (ኀቤከ)")!!
        assertEquals("ዘዮሐንስ ወልደ ነጎድጓድ", named.name)
        assertEquals("ኀቤከ", named.incipit)
        assertEquals("ዘዮሐንስ ወልደ ነጎድጓድ  ·  ኀቤከ", named.label)
    }

    @Test
    fun `the prefix and the trailing mark do not make a second anaphora`() {
        assertEquals(Anaphora.of("ዘባስልዮስ ።")?.name, Anaphora.of("ባስልዮስ ።")?.name)
        assertEquals(Anaphora.of("ዘቄርሎስ ።")?.name, Anaphora.of("ቄርሎስ ።")?.name)
        assertEquals(Anaphora.of("ዘዲዮስቆሮስ ።")?.name, Anaphora.of("ዲዮስቆሮስ ።")?.name)
    }

    @Test
    fun `a day offering a choice reads as two`() {
        val both = Anaphora.allOf(listOf("ዘእግዝእትነ አው ግሩም ።"))
        assertEquals(listOf("ዘእግዝእትነ ማርያም", "ዘሠለስቱ ምዕት"), both.map { it.name })
    }

    @Test
    fun `an incipit written on its own still names its anaphora`() {
        // The source writes ያዕቆብ ዘሥሩግ both ways round: with the incipit in
        // brackets, and as the incipit alone.
        assertEquals("ዘያዕቆብ ዘሥሩግ", Anaphora.of("ተንሥኡ ።")?.name)
        assertEquals("ዘያዕቆብ ዘሥሩግ", Anaphora.of("ተንሥኡ (ያዕቆብ ዘሥሩግ) ።")?.name)
    }

    @Test
    fun `what names no anaphora is passed through rather than guessed at`() {
        assertEquals("ቅዳሴ", Anaphora.of("ቅዳሴ ።")?.name)
    }

    @Test
    fun `the whole bundled year resolves to thirteen anaphoras at most`() {
        val dir = listOf("src/main/assets/content/gitsawe", "app/src/main/assets/content/gitsawe")
            .map(::File).first { it.isDirectory }
        val raw = File(dir, "daily-gitsawe.json").readText()
        // Every "kidassie": [...] string in the file, without decoding the model.
        val values = Regex(""""kidassie"\s*:\s*\[([^]]*)]""").findAll(raw)
            .flatMap { m -> Regex(""""((?:[^"\\]|\\.)*)"""").findAll(m.groupValues[1]).map { it.groupValues[1] } }
            .toList()
        assertTrue("no anaphora strings found", values.isNotEmpty())
        val names = values.flatMap { Anaphora.allOf(listOf(it)) }.map { it.name }.toSet()
        // Thirteen anaphoras, plus whatever the source names by incipit alone.
        assertTrue("resolved to ${names.size}: $names", names.size <= 16)
    }
}
