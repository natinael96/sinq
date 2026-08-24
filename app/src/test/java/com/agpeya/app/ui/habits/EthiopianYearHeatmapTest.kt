package com.agpeya.app.ui.habits

import com.agpeya.app.ui.common.EthiopianDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EthiopianYearHeatmapTest {

    @Test
    fun `first supported year starts at the app epoch rather than Meskerem`() {
        val today = EthiopianDate(2018, 12, 10).toGregorian()

        val range = journeyYearSelectableRange(2018, today)

        assertEquals(APP_EPOCH_EC.toGregorian(), range?.start)
        assertEquals(today, range?.endInclusive)
    }

    @Test
    fun `current year ends at today`() {
        val today = EthiopianDate(2019, 4, 12).toGregorian()

        val range = journeyYearSelectableRange(2019, today)

        assertEquals(EthiopianDate(2019, 1, 1).toGregorian(), range?.start)
        assertEquals(today, range?.endInclusive)
    }

    @Test
    fun `year before the app epoch has no selectable days`() {
        val today = EthiopianDate(2018, 12, 10).toGregorian()

        assertNull(journeyYearSelectableRange(2017, today))
    }
}
