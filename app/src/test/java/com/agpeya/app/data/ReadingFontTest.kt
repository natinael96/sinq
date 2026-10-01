package com.agpeya.app.data

import com.agpeya.app.ui.settings.FONT_CHOICES
import com.agpeya.app.ui.settings.fontLabel
import com.agpeya.app.ui.theme.readingFontFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReadingFontTest {

    @Test
    fun `reading font enum contains all five fonts`() {
        val fonts = ReadingFont.values()
        assertEquals(5, fonts.size)
        assertTrue(fonts.contains(ReadingFont.ABBA_GARIMA))
        assertEquals(ReadingFont.ABBA_GARIMA, ReadingFont.valueOf("ABBA_GARIMA"))
    }

    @Test
    fun `font choices in settings contains all reading font options`() {
        val mappedFonts = FONT_CHOICES.map { it.first }.toSet()
        assertEquals(ReadingFont.values().toSet(), mappedFonts)
        val abbaGarimaEntry = FONT_CHOICES.find { it.first == ReadingFont.ABBA_GARIMA }
        assertNotNull(abbaGarimaEntry)
        assertEquals("Abba Garima", abbaGarimaEntry?.second)
    }

    @Test
    fun `fontLabel produces expected display strings`() {
        assertEquals("Abba Garima", fontLabel(ReadingFont.ABBA_GARIMA))
        assertEquals("Abyssinica SIL", fontLabel(ReadingFont.ABYSSINICA))
        assertEquals("Ethiopic Abay Light", fontLabel(ReadingFont.ABAY_LIGHT))
        assertEquals("Bela Bereka", fontLabel(ReadingFont.BELA_BEREKA))
        assertEquals("Zemenay", fontLabel(ReadingFont.ZEMENAY))
    }

    @Test
    fun `readingFontFamily maps all enum values`() {
        for (font in ReadingFont.values()) {
            val family = readingFontFamily(font)
            assertNotNull(family)
        }
    }

    @Test
    fun `abba garima font asset and license exist`() {
        val fontFile = File("src/main/res/font/abba_garima.ttf")
        assertTrue("Font file must exist", fontFile.exists())
        assertTrue("Font file must be non-empty", fontFile.length() > 0)

        val licenseFile = File("../docs/fonts/Abba_Garima-license.txt")
        assertTrue("License file must exist", licenseFile.exists())
        assertTrue("License file must be non-empty", licenseFile.length() > 0)
    }
}
