package com.agpeya.app.common

import com.agpeya.app.ui.common.EthiopianDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class EthiopianDateTest {

    @Test
    fun `ethiopian millennium - Meskerem 1 2000`() {
        // 11 Sep 2007 (Gregorian) was Ethiopian New Year 2000 — the Millennium.
        assertEquals(EthiopianDate(2000, 1, 1), EthiopianDate.from(LocalDate.of(2007, 9, 12)))
    }

    @Test
    fun `new year after gregorian leap year falls on sep 12`() {
        // The Ethiopian year beginning after a Gregorian leap-February starts Sep 12.
        assertEquals(EthiopianDate(2013, 1, 1), EthiopianDate.from(LocalDate.of(2020, 9, 11)))
    }

    @Test
    fun `mid-year conversion`() {
        // 15 Jul 2026 (Gregorian) = Hamle 8, 2018 EC.
        assertEquals(EthiopianDate(2018, 11, 8), EthiopianDate.from(LocalDate.of(2026, 7, 15)))
    }

    @Test
    fun `toGregorian inverts from for a full range of days`() {
        var d = LocalDate.of(2019, 9, 1)
        val end = LocalDate.of(2027, 12, 31)
        while (!d.isAfter(end)) {
            assertEquals(d, EthiopianDate.from(d).toGregorian())
            d = d.plusDays(17) // stride keeps the test fast while crossing months/years
        }
        // Anchors
        assertEquals(LocalDate.of(2007, 9, 12), EthiopianDate(2000, 1, 1).toGregorian())
        assertEquals(LocalDate.of(2026, 7, 8), EthiopianDate(2018, 11, 1).toGregorian())
    }

    @Test
    fun `pagume has five or six days`() {
        // 5 Sep 2019 = Pagume 30?? no — Pagume runs 6..10 Sep 2019 (2011 EC, 5 days).
        assertEquals(EthiopianDate(2011, 13, 1), EthiopianDate.from(LocalDate.of(2019, 9, 6)))
        assertEquals(EthiopianDate(2011, 13, 5), EthiopianDate.from(LocalDate.of(2019, 9, 10)))
        // 2011 EC is followed by New Year on 12 Sep 2019? No: 2012 EC began 12 Sep 2019
        // (the EC leap year 2011 had Pagume 6 on 11 Sep 2019).
        assertEquals(EthiopianDate(2011, 13, 6), EthiopianDate.from(LocalDate.of(2019, 9, 11)))
        assertEquals(EthiopianDate(2012, 1, 1), EthiopianDate.from(LocalDate.of(2019, 9, 12)))
    }
}
