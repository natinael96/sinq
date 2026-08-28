package com.agpeya.app.ui.library

import com.agpeya.app.data.BahreHasab
import org.junit.Assert.assertEquals
import org.junit.Test

class BahreHasabReferenceScreenTest {
    @Test fun `range includes current year and twenty five future years`() {
        val years = bahreHasabYearsFrom(2018)
        assertEquals(26, years.count())
        assertEquals(2018, years.first)
        assertEquals(2043, years.last)
    }

    @Test fun `calculated year exposes the complete movable calendar`() {
        val year = calculateBahreHasabYear(2018)

        assertEquals(2018, year.year)
        assertEquals(BahreHasab.ameteAlem(2018), year.ameteAlem)
        assertEquals(10, year.observances.size)
        assertEquals(BahreHasab.fasika(2018), year.observances.first { it.first == "ትንሣኤ" }.second)
        assertEquals(BahreHasab.apostlesFast(2018), year.observances.last().second)
    }
}
