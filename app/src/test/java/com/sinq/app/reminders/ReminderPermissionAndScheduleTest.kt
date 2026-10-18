package com.sinq.app.reminders

import com.sinq.app.ui.strings.AmharicStrings
import com.sinq.app.ui.strings.EnglishStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPermissionAndScheduleTest {

    @Test
    fun `evening reminder wording is non-punitive and never mentions streak`() {
        assertFalse(EnglishStrings.nightReminderTitle.contains("streak", ignoreCase = true))
        assertFalse(EnglishStrings.nightReminderBody.contains("streak", ignoreCase = true))
        assertFalse(EnglishStrings.nightReminderBody.contains("missed", ignoreCase = true))
        assertFalse(EnglishStrings.nightReminderFastBody.contains("streak", ignoreCase = true))

        assertFalse(AmharicStrings.nightReminderTitle.contains("streak", ignoreCase = true))
        assertFalse(AmharicStrings.nightReminderBody.contains("streak", ignoreCase = true))
        assertFalse(AmharicStrings.nightReminderBody.contains("ተቋርጧል", ignoreCase = true)) // not "broken"
        assertFalse(AmharicStrings.nightReminderFastBody.contains("streak", ignoreCase = true))
    }

    @Test
    fun `oem guidance strings are populated for all target manufacturers`() {
        assertTrue(EnglishStrings.oemTecnoTitle.contains("Tecno"))
        assertTrue(EnglishStrings.oemTecnoSteps.contains("Phone Master"))

        assertTrue(EnglishStrings.oemInfinixTitle.contains("Infinix"))
        assertTrue(EnglishStrings.oemInfinixSteps.contains("Phone Master"))

        assertTrue(EnglishStrings.oemXiaomiTitle.contains("Xiaomi"))
        assertTrue(EnglishStrings.oemXiaomiSteps.contains("Autostart"))

        assertTrue(EnglishStrings.oemSamsungTitle.contains("Samsung"))
        assertTrue(EnglishStrings.oemSamsungSteps.contains("Never sleeping apps"))

        assertTrue(EnglishStrings.oemGenericTitle.contains("Stock") || EnglishStrings.oemGenericTitle.contains("Other"))
        assertTrue(EnglishStrings.oemGenericSteps.contains("Unrestricted"))

        assertTrue(EnglishStrings.exactAlarmWarningTitle.isNotEmpty())
        assertTrue(EnglishStrings.grantExactAlarmPermission.isNotEmpty())
    }

    @Test
    fun `amharic oem guidance contains ethiopic characters`() {
        assertTrue(AmharicStrings.oemTecnoSteps.contains(Regex("[\\u1200-\\u137F]")))
        assertTrue(AmharicStrings.oemInfinixSteps.contains(Regex("[\\u1200-\\u137F]")))
        assertTrue(AmharicStrings.oemXiaomiSteps.contains(Regex("[\\u1200-\\u137F]")))
        assertTrue(AmharicStrings.oemSamsungSteps.contains(Regex("[\\u1200-\\u137F]")))
        assertTrue(AmharicStrings.oemGenericSteps.contains(Regex("[\\u1200-\\u137F]")))
        assertTrue(AmharicStrings.exactAlarmWarningBody.contains(Regex("[\\u1200-\\u137F]")))
    }
}
