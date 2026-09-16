package com.agpeya.app.data

import com.agpeya.app.model.HabitSchedule
import com.agpeya.app.ui.common.EthiopianDate
import org.junit.Assert.*
import org.junit.Test

class LeapReminderTest {
    @Test fun `Pagume six finds the next leap day across common years`() {
        val schedule = HabitSchedule(kind = HabitSchedule.Kind.YEARLY, monthNum = 13, monthDay = 6)
        val from = EthiopianDate(2020, 1, 1).toGregorian()
        assertEquals(EthiopianDate(2023, 13, 6).toGregorian(), schedule.nextDueOnOrAfter(from))
        assertFalse(schedule.isDueOn(EthiopianDate(2020, 13, 5).toGregorian()))
    }
}
