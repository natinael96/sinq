package com.sinq.app.ui.library

import com.sinq.app.ui.strings.AmharicStrings
import com.sinq.app.ui.strings.EnglishStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LibraryStringsTest {

    @Test
    fun `three distinct language modes enforce strict English, bilingual English-first, and strict Amharic`() {
        val ethiopicRegex = Regex("[\\u1200-\\u137F]")

        // 1. Strict English: zero Ethiopic characters in liturgical titles
        assertEquals("Rite of Mahlet", EnglishStrings.mahletTitle)
        assertEquals("Psalter", EnglishStrings.psalterTitle)
        assertEquals("Other Books", EnglishStrings.booksTitle)
        assertEquals("Synaxarium", EnglishStrings.synaxariumTitle)
        assertEquals("Today's Lectionary", EnglishStrings.gitsaweTitle)
        assertEquals("Daily Prayer", EnglishStrings.zewotrTselot)
        assertEquals("Praises of Mary", EnglishStrings.wudaseMariam)
        assertEquals("Text Size", EnglishStrings.fontSizeLabel)
        assertEquals("Typeface", EnglishStrings.fontFamilyLabel)
        assertEquals("Size 18", EnglishStrings.fontStepLabel(18))
        assertEquals("Eng+Amh", EnglishStrings.langBilingual)
        assertFalse(EnglishStrings.mahletTitle.contains(ethiopicRegex))
        assertFalse(EnglishStrings.psalterTitle.contains(ethiopicRegex))
        assertFalse(EnglishStrings.booksTitle.contains(ethiopicRegex))
        assertFalse(EnglishStrings.synaxariumTitle.contains(ethiopicRegex))
        assertFalse(EnglishStrings.fontSizeLabel.contains(ethiopicRegex))
        assertFalse(EnglishStrings.fontFamilyLabel.contains(ethiopicRegex))

        // 2. Bilingual (Amh+Eng): English first with Amharic
        assertEquals("Rite of Mahlet (ሥርዓተ ማኅሌት)", com.sinq.app.ui.strings.BilingualStrings.mahletTitle)
        assertEquals("Psalter (መዝሙረ ዳዊት)", com.sinq.app.ui.strings.BilingualStrings.psalterTitle)
        assertEquals("Other Books (ሌሎች መጻሕፍት)", com.sinq.app.ui.strings.BilingualStrings.booksTitle)
        assertEquals("Synaxarium (ስንክሳር)", com.sinq.app.ui.strings.BilingualStrings.synaxariumTitle)
        assertEquals("Today's Lectionary (የዕለቱ ግጻዌ)", com.sinq.app.ui.strings.BilingualStrings.gitsaweTitle)
        assertEquals("Daily Prayer (ዘወትር ጸሎት)", com.sinq.app.ui.strings.BilingualStrings.zewotrTselot)
        assertEquals("Praises of Mary (ውዳሴ ማርያም)", com.sinq.app.ui.strings.BilingualStrings.wudaseMariam)
        assertEquals("Text Size (የፊደል መጠን)", com.sinq.app.ui.strings.BilingualStrings.fontSizeLabel)
        assertEquals("Typeface (የፊደል ቅርጽ)", com.sinq.app.ui.strings.BilingualStrings.fontFamilyLabel)
        assertEquals("Size 18 (፲፰)", com.sinq.app.ui.strings.BilingualStrings.fontStepLabel(18))
        org.junit.Assert.assertTrue(com.sinq.app.ui.strings.BilingualStrings.psalterTitle.startsWith("Psalter"))
        org.junit.Assert.assertTrue(com.sinq.app.ui.strings.BilingualStrings.psalterTitle.contains(ethiopicRegex))

        // 3. Strict Amharic
        assertEquals("ሥርዓተ ማኅሌት", AmharicStrings.mahletTitle)
        assertEquals("መዝሙረ ዳዊት", AmharicStrings.psalterTitle)
        assertEquals("ሌሎች መጻሕፍት", AmharicStrings.booksTitle)
        assertEquals("ስንክሳር", AmharicStrings.synaxariumTitle)
        assertEquals("የዕለቱ ግጻዌ", AmharicStrings.gitsaweTitle)
        assertEquals("ዘወትር ጸሎት", AmharicStrings.zewotrTselot)
        assertEquals("ውዳሴ ማርያም", AmharicStrings.wudaseMariam)
        assertEquals("የፊደል መጠን", AmharicStrings.fontSizeLabel)
        assertEquals("የፊደል ቅርጽ", AmharicStrings.fontFamilyLabel)
        assertEquals("መጠን ፲፰", AmharicStrings.fontStepLabel(18))
    }
}
