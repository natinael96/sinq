package com.sinq.app.reminders

import com.sinq.app.data.SettingsRepository
import com.sinq.app.ui.strings.AmharicStrings
import com.sinq.app.ui.strings.EnglishStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRetentionTest {

    @Test
    fun `default notification timeout is 2 hours (120 minutes)`() {
        assertEquals(120, SettingsRepository.DEFAULT_NOTIFICATION_TIMEOUT_MINUTES)
        assertTrue(SettingsRepository.NOTIFICATION_TIMEOUT_CHOICES.contains(120))
        assertTrue(SettingsRepository.NOTIFICATION_TIMEOUT_CHOICES.contains(0))
        assertTrue(SettingsRepository.NOTIFICATION_TIMEOUT_CHOICES.contains(30))
        assertTrue(SettingsRepository.NOTIFICATION_TIMEOUT_CHOICES.contains(60))
    }

    @Test
    fun `notification timeout options produce distinct human-readable labels`() {
        // Amharic labels
        assertEquals("30 ደቂቃ", AmharicStrings.notificationTimeoutOption(30))
        assertEquals("1 ሰዓት", AmharicStrings.notificationTimeoutOption(60))
        assertEquals("2 ሰዓት (የተመረጠ)", AmharicStrings.notificationTimeoutOption(120))
        assertEquals("4 ሰዓት", AmharicStrings.notificationTimeoutOption(240))
        assertEquals("6 ሰዓት", AmharicStrings.notificationTimeoutOption(360))
        assertEquals("እስኪነሳ ድረስ (አይጥፋ)", AmharicStrings.notificationTimeoutOption(0))

        // English labels
        assertEquals("30 minutes", EnglishStrings.notificationTimeoutOption(30))
        assertEquals("1 hour", EnglishStrings.notificationTimeoutOption(60))
        assertEquals("2 hours (Recommended)", EnglishStrings.notificationTimeoutOption(120))
        assertEquals("4 hours", EnglishStrings.notificationTimeoutOption(240))
        assertEquals("6 hours", EnglishStrings.notificationTimeoutOption(360))
        assertEquals("Keep until dismissed", EnglishStrings.notificationTimeoutOption(0))
    }

    @Test
    fun `daily quote time title is distinct and formatted`() {
        assertEquals("የማሳሰቢያ ሰዓት (በቀን አንድ ጊዜ)", AmharicStrings.dailyQuoteTimeTitle)
        assertEquals("Daily Reminder Time", EnglishStrings.dailyQuoteTimeTitle)
    }

    @Test
    fun `English mode preserves non-scripture liturgical books in traditional Amharic`() {
        assertEquals("ባሕረ ሐሳብ", EnglishStrings.bahreHasabTitle)
        assertEquals("ስንክሳር", EnglishStrings.synaxariumTitle)
        assertEquals("ሥርዓተ ማኅሌት", EnglishStrings.mahletTitle)
        assertEquals("ሌሎች መጻሕፍት", EnglishStrings.booksTitle)
        assertEquals("ውዳሴ ማርያም", EnglishStrings.wudaseMariam)
        assertEquals("Psalter", EnglishStrings.psalterTitle)
    }
}
