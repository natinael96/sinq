package com.sinq.app.ui.intro

import com.sinq.app.ui.strings.AmharicStrings
import com.sinq.app.ui.strings.EnglishStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFlowTest {

    @Test
    fun `three onboarding steps are localized in english and amharic`() {
        // Step 1: Language
        assertTrue(EnglishStrings.onboardingStepLanguageTitle.isNotEmpty())
        assertTrue(AmharicStrings.onboardingStepLanguageTitle.contains(Regex("[\\u1200-\\u137F]")))

        // Step 2: Typography
        assertTrue(EnglishStrings.onboardingStepTypographyTitle.isNotEmpty())
        assertTrue(AmharicStrings.onboardingStepTypographyTitle.contains(Regex("[\\u1200-\\u137F]")))

        // Step 3: Reminders
        assertTrue(EnglishStrings.onboardingStepRemindersTitle.isNotEmpty())
        assertTrue(AmharicStrings.onboardingStepRemindersTitle.contains(Regex("[\\u1200-\\u137F]")))

        // Reminder toggle wording
        assertTrue(EnglishStrings.onboardingReminderOptIn.isNotEmpty())
        assertTrue(EnglishStrings.onboardingReminderOptInDesc.contains("Off by default", ignoreCase = true))
        assertTrue(AmharicStrings.onboardingReminderOptIn.contains(Regex("[\\u1200-\\u137F]")))
    }

    @Test
    fun `navigation actions are clearly defined`() {
        assertEquals("Next", EnglishStrings.next)
        assertEquals("Skip", EnglishStrings.skip)
        assertEquals("Get started", EnglishStrings.getStarted)

        assertEquals("ቀጥል", AmharicStrings.next)
        assertEquals("ዝለል", AmharicStrings.skip)
        assertEquals("ጀምር", AmharicStrings.getStarted)
    }
}
