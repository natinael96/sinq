package com.agpeya.app.ui.common

import org.junit.Assert.*
import org.junit.Test

class ReadingProgressTest {
    @Test fun `opening or restoring never completes without interaction`() {
        assertFalse(reachedReadingHalfway(1.0, false))
        assertFalse(reachedReadingHalfway(0.499, true))
        assertTrue(reachedReadingHalfway(0.5, true))
        assertFalse(reachedReadingHalfway(Double.NaN, true))
        assertFalse(reachedReadingHalfway(Double.POSITIVE_INFINITY, true))
    }
    @Test fun `empty content and controls cannot complete`() {
        assertEquals(0.0, readingFraction(emptyList(), emptyList(), 500), 0.0)
        assertEquals(0.0, readingFraction(listOf(0), listOf(ReadingViewportItem(0, 0, 100)), 500), 0.0)
        assertEquals(0.0, readingFraction(listOf(0, 100), listOf(ReadingViewportItem(0, 0, 100)), 100), 0.0)
    }
    @Test fun `a tall section requires reaching halfway inside it`() {
        val visible = listOf(ReadingViewportItem(0, 0, 2000))
        assertEquals(0.25, readingFraction(listOf(100), visible, 500), 0.001)
        assertEquals(0.5, readingFraction(listOf(100), visible, 1000), 0.001)
    }
    @Test fun `unequal sections are weighted by text rather than section count`() {
        assertEquals(0.45, readingFraction(listOf(900, 100), listOf(ReadingViewportItem(0, 0, 1000)), 500), 0.001)
        assertEquals(0.95, readingFraction(listOf(900, 100), listOf(ReadingViewportItem(1, 0, 1000)), 500), 0.001)
    }
    @Test fun `footer only counts after content and fraction stays bounded`() {
        assertEquals(1.0, readingFraction(listOf(100), listOf(ReadingViewportItem(1, 0, 100)), 500), 0.0)
        assertEquals(1.0, readingFraction(listOf(100), listOf(ReadingViewportItem(0, -500, 100)), 500), 0.0)
    }
    @Test fun `swiped pages use the same text weights`() {
        assertEquals(0.45, pageReadingFraction(listOf(900, 100), 0, 0.5), 0.001)
        assertEquals(0.95, pageReadingFraction(listOf(900, 100), 1, 0.5), 0.001)
        assertEquals(0.0, pageReadingFraction(listOf(100), 2, 1.0), 0.0)
    }
}
