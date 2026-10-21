package com.sinq.app.ui.common

import com.sinq.app.data.Language
import org.junit.Assert.assertEquals
import org.junit.Test

class PrayerDisplayTest {

    @Test
    fun `hourName returns appropriate formatting for language modes`() {
        // 1. English
        assertEquals("Morning Prayer", PrayerDisplay.hourName("morning", mode = Language.ENGLISH, compact = false))
        assertEquals("Morning", PrayerDisplay.hourName("morning", mode = Language.ENGLISH, compact = true))
        assertEquals("Terce", PrayerDisplay.hourName("terce", mode = Language.ENGLISH, compact = false))

        // 2. Amharic
        assertEquals("ጸሎተ ነግህ", PrayerDisplay.hourName("morning", mode = Language.AMHARIC, compact = false))
        assertEquals("ነግህ", PrayerDisplay.hourName("morning", mode = Language.AMHARIC, compact = true))
        assertEquals("ጸሎተ ሠለስት", PrayerDisplay.hourName("terce", mode = Language.AMHARIC, compact = false))
    }

    @Test
    fun `fastName returns appropriate formatting for language modes`() {
        // Weekly fast
        assertEquals("Wednesday Fast", PrayerDisplay.fastName(null, null, isWeeklyWednesday = true, isWeeklyFriday = false, mode = Language.ENGLISH))
        assertEquals("ጾመ ድህነት (ረቡዕ)", PrayerDisplay.fastName(null, null, isWeeklyWednesday = true, isWeeklyFriday = false, mode = Language.AMHARIC))

        // Seasonal fast
        val am = "ጾመ ነቢያት"
        val en = "Fast of the Prophets"
        assertEquals(en, PrayerDisplay.fastName(am, en, isWeeklyWednesday = false, isWeeklyFriday = false, mode = Language.ENGLISH))
        assertEquals(am, PrayerDisplay.fastName(am, en, isWeeklyWednesday = false, isWeeklyFriday = false, mode = Language.AMHARIC))
    }
}
