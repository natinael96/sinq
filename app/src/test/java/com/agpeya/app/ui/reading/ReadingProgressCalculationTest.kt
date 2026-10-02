package com.agpeya.app.ui.reading

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingProgressCalculationTest {

    private fun calculateProgress(currentIndex: Int, totalSections: Int): Float {
        if (totalSections <= 1) return 0f
        return (currentIndex.toFloat() / (totalSections - 1)).coerceIn(0f, 1f)
    }

    @Test
    fun `single section reports zero progress`() {
        assertEquals(0f, calculateProgress(0, 1), 0.001f)
    }

    @Test
    fun `empty sections reports zero progress`() {
        assertEquals(0f, calculateProgress(0, 0), 0.001f)
    }

    @Test
    fun `first section of multi-section office starts at zero`() {
        assertEquals(0f, calculateProgress(0, 10), 0.001f)
    }

    @Test
    fun `halfway section reports fifty percent`() {
        assertEquals(0.5f, calculateProgress(4, 9), 0.001f)
    }

    @Test
    fun `final section reaches one hundred percent`() {
        assertEquals(1.0f, calculateProgress(9, 10), 0.001f)
    }

    @Test
    fun `out of bounds index is safely clamped`() {
        assertEquals(1.0f, calculateProgress(15, 10), 0.001f)
        assertEquals(0f, calculateProgress(-2, 10), 0.001f)
    }
}
