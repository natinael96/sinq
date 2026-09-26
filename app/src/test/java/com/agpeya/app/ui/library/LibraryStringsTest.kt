package com.agpeya.app.ui.library

import com.agpeya.app.ui.strings.AmharicStrings
import com.agpeya.app.ui.strings.EnglishStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LibraryStringsTest {

    @Test
    fun `library supporting copy follows the selected language`() {
        assertEquals("ሰኞ–እሑድ", AmharicStrings.wudaseScheduleSubtitle)
        assertEquals("የዕለት ጸሎቶች", AmharicStrings.zewotrSubtitle)
        assertFalse(EnglishStrings.wudaseScheduleSubtitle.contains(Regex("[\\u1200-\\u137F]")))
        assertFalse(EnglishStrings.zewotrSubtitle.contains(Regex("[\\u1200-\\u137F]")))
    }
}
