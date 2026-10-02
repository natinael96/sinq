package com.agpeya.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingProgressBarTest {

    private fun calculateListProgress(
        firstIndex: Int,
        firstOffset: Int,
        firstSize: Int,
        totalItems: Int,
        visibleCount: Int,
        headerOffset: Int = 0,
        isAtEnd: Boolean = false,
    ): Float {
        if (totalItems <= 1 || visibleCount.coerceAtLeast(0) == 0) return 0f
        if (visibleCount >= totalItems && !isAtEnd && firstIndex == 0 && firstOffset == 0) return 0f
        if (isAtEnd) return 1f
        val remaining = (totalItems - visibleCount).coerceAtLeast(1)
        val base = (firstIndex - headerOffset).coerceAtLeast(0).toFloat() / remaining
        val offset = if (firstSize > 0) (firstOffset.toFloat() / firstSize) / remaining else 0f
        return (base + offset).coerceIn(0f, 1f)
    }

    @Test
    fun `starts at zero on first item`() {
        val progress = calculateListProgress(
            firstIndex = 0,
            firstOffset = 0,
            firstSize = 100,
            totalItems = 10,
            visibleCount = 3,
        )
        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun `interpolates smoothly during scroll within an item`() {
        val progressHalfway = calculateListProgress(
            firstIndex = 0,
            firstOffset = 50,
            firstSize = 100,
            totalItems = 8,
            visibleCount = 1,
        )
        // remaining = 7, base = 0, offset = 0.5 / 7 = 0.0714
        assertEquals(0.0714f, progressHalfway, 0.001f)
    }

    @Test
    fun `reaches one hundred percent when at end`() {
        val progress = calculateListProgress(
            firstIndex = 7,
            firstOffset = 0,
            firstSize = 100,
            totalItems = 10,
            visibleCount = 3,
            isAtEnd = true,
        )
        assertEquals(1.0f, progress, 0.001f)
    }

    @Test
    fun `accounts for header offset`() {
        val progress = calculateListProgress(
            firstIndex = 1,
            firstOffset = 0,
            firstSize = 100,
            totalItems = 10,
            visibleCount = 3,
            headerOffset = 1,
        )
        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun `returns zero when single item fits viewport`() {
        val progress = calculateListProgress(
            firstIndex = 0,
            firstOffset = 0,
            firstSize = 100,
            totalItems = 1,
            visibleCount = 1,
        )
        assertEquals(0f, progress, 0.001f)
    }
}
