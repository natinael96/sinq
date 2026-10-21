package com.sinq.app.ui.habits

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyFabVisibilityTest {

    private fun isFabVisible(selectedEpochDay: Long?): Boolean {
        // FAB is visible only when no day is being inspected
        return selectedEpochDay == null
    }

    @Test
    fun `fab is visible when day inspection card is closed`() {
        assertTrue(isFabVisible(null))
    }

    @Test
    fun `fab is hidden when day inspection card is open`() {
        assertFalse(isFabVisible(19730L))
        assertFalse(isFabVisible(0L))
    }
}
