package com.agpeya.app.ui.common

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveNavigationTest {
    @Test
    fun compactWindowsKeepBottomNavigation() {
        assertFalse(usesNavigationRail(599.dp))
    }

    @Test
    fun mediumAndExpandedWindowsUseRail() {
        assertTrue(usesNavigationRail(600.dp))
        assertTrue(usesNavigationRail(1200.dp))
    }
}
