package com.agpeya.app.ui.settings

import com.agpeya.app.ui.common.GUIDE_URL
import com.agpeya.app.ui.strings.AmharicStrings
import com.agpeya.app.ui.strings.EnglishStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsGuideTest {

    @Test
    fun `guide url points to canonical documentation endpoint`() {
        assertEquals("https://sinq.natinael96.tech/guide.html", GUIDE_URL)
        assertTrue(GUIDE_URL.startsWith("https://"))
    }

    @Test
    fun `user guide strings are properly localized`() {
        assertEquals("የአጠቃቀም መመሪያ", AmharicStrings.userGuide)
        assertEquals("User Guide", EnglishStrings.userGuide)

        assertTrue(AmharicStrings.userGuide.contains(Regex("[\\u1200-\\u137F]")))
        assertFalse(EnglishStrings.userGuide.contains(Regex("[\\u1200-\\u137F]")))

        assertTrue(AmharicStrings.userGuideSubtitle.isNotEmpty())
        assertTrue(EnglishStrings.userGuideSubtitle.isNotEmpty())
    }
}
