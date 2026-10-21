package com.sinq.app.ui.library

import com.sinq.app.ui.strings.AmharicStrings
import com.sinq.app.ui.strings.EnglishStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LibraryStringsTest {

    @Test
    fun `two distinct language modes enforce English and Amharic`() {
        val ethiopicRegex = Regex("[\\u1200-\\u137F]")

        // 1. English: Scripture in English, liturgical books in traditional Amharic
        assertEquals("ሥርዓተ ማኅሌት", EnglishStrings.mahletTitle)
        assertEquals("Psalter", EnglishStrings.psalterTitle)
        assertEquals("ሌሎች መጻሕፍት", EnglishStrings.booksTitle)
        assertEquals("ስንክሳር", EnglishStrings.synaxariumTitle)
        assertEquals("Today's Lectionary", EnglishStrings.gitsaweTitle)
        assertEquals("Daily Prayer", EnglishStrings.zewotrTselot)
        assertEquals("ውዳሴ ማርያም", EnglishStrings.wudaseMariam)
        assertEquals("ባሕረ ሐሳብ", EnglishStrings.bahreHasabTitle)
        assertEquals("Text Size", EnglishStrings.fontSizeLabel)
        assertEquals("Typeface", EnglishStrings.fontFamilyLabel)
        assertEquals("Size 18", EnglishStrings.fontStepLabel(18))
        assertFalse(EnglishStrings.psalterTitle.contains(ethiopicRegex))
        assertFalse(EnglishStrings.fontSizeLabel.contains(ethiopicRegex))
        assertFalse(EnglishStrings.fontFamilyLabel.contains(ethiopicRegex))

        // 2. Amharic
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
