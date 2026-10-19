package com.sinq.app.ui.common

import com.sinq.app.data.Language
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerDisplayTest {

    @Test
    fun `hourName returns appropriate formatting for all three language modes`() {
        // 1. Strict English
        assertEquals("Morning Prayer", PrayerDisplay.hourName("morning", mode = Language.ENGLISH, compact = false))
        assertEquals("Morning", PrayerDisplay.hourName("morning", mode = Language.ENGLISH, compact = true))
        assertEquals("Terce", PrayerDisplay.hourName("terce", mode = Language.ENGLISH, compact = false))

        // 2. Bilingual (English first with Amharic)
        assertEquals("Morning Prayer · ጸሎተ ነግህ", PrayerDisplay.hourName("morning", mode = Language.AMH_ENG, compact = false))
        assertEquals("Morning (ነግህ)", PrayerDisplay.hourName("morning", mode = Language.AMH_ENG, compact = true))
        assertEquals("Terce · ጸሎተ ሠለስት", PrayerDisplay.hourName("terce", mode = Language.AMH_ENG, compact = false))
        assertEquals("Terce (ሠለስት)", PrayerDisplay.hourName("terce", mode = Language.AMH_ENG, compact = true))

        // 3. Strict Amharic
        assertEquals("ጸሎተ ነግህ", PrayerDisplay.hourName("morning", mode = Language.AMHARIC, compact = false))
        assertEquals("ነግህ", PrayerDisplay.hourName("morning", mode = Language.AMHARIC, compact = true))
        assertEquals("ጸሎተ ሠለስት", PrayerDisplay.hourName("terce", mode = Language.AMHARIC, compact = false))
    }

    @Test
    fun `fastName returns appropriate formatting for all three language modes`() {
        // Weekly fast
        assertEquals("Wednesday Fast", PrayerDisplay.fastName(null, null, isWeeklyWednesday = true, isWeeklyFriday = false, mode = Language.ENGLISH))
        assertEquals("Wednesday Fast (ጾመ ድህነት · ረቡዕ)", PrayerDisplay.fastName(null, null, isWeeklyWednesday = true, isWeeklyFriday = false, mode = Language.AMH_ENG))
        assertEquals("ጾመ ድህነት (ረቡዕ)", PrayerDisplay.fastName(null, null, isWeeklyWednesday = true, isWeeklyFriday = false, mode = Language.AMHARIC))

        // Seasonal fast
        val am = "ጾመ ነቢያት"
        val en = "Fast of the Prophets"
        assertEquals(en, PrayerDisplay.fastName(am, en, isWeeklyWednesday = false, isWeeklyFriday = false, mode = Language.ENGLISH))
        assertEquals("$en ($am)", PrayerDisplay.fastName(am, en, isWeeklyWednesday = false, isWeeklyFriday = false, mode = Language.AMH_ENG))
        assertEquals(am, PrayerDisplay.fastName(am, en, isWeeklyWednesday = false, isWeeklyFriday = false, mode = Language.AMHARIC))
    }
}
