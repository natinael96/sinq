package com.agpeya.app.ui.common

import com.agpeya.app.ui.strings.Strings
import java.time.LocalDate

/**
 * Ethiopian (Amete Mihret) calendar date. Conversion uses the standard
 * Beyene–Kudlek Julian-day algorithm: the Ethiopian epoch corresponds to
 * JDN 1723856, years cycle in 1461-day groups of four (three 365-day years
 * plus one 366-day year, Pagume gaining its 6th day).
 */
data class EthiopianDate(val year: Int, val month: Int, val day: Int) {

    /** Inverse conversion (verified round-trip in unit tests). */
    fun toGregorian(): LocalDate {
        val jdn = ETHIOPIC_EPOCH + 365L * year + year / 4 + 30L * (month - 1) + (day - 1)
        return LocalDate.ofEpochDay(jdn - 2440588L)
    }

    companion object {
        private const val ETHIOPIC_EPOCH = 1723856L

        fun from(date: LocalDate): EthiopianDate {
            val jdn = date.toEpochDay() + 2440588L // epoch day -> Julian Day Number
            val r = ((jdn - ETHIOPIC_EPOCH) % 1461).toInt()
            val n = r % 365 + 365 * (r / 1460)
            val year = 4 * ((jdn - ETHIOPIC_EPOCH) / 1461) + r / 365 - r / 1460
            val month = n / 30 + 1
            val day = n % 30 + 1
            return EthiopianDate(year.toInt(), month, day)
        }
    }
}

/** e.g. "ረቡዕ፣ ሐምሌ 8 2018 ዓ.ም" — weekday, Ethiopian month + day, year, era. */
fun formatEthiopian(date: LocalDate, s: Strings): String {
    val e = EthiopianDate.from(date)
    val weekday = s.weekdayNames[date.dayOfWeek.value - 1]
    val month = s.ethMonths[e.month - 1]
    return "$weekday፣ $month ${e.day} ${e.year} ${s.eraSuffix}"
}

/** Short form for compact spots: "ሐምሌ 8". */
fun formatEthiopianShort(date: LocalDate, s: Strings): String {
    val e = EthiopianDate.from(date)
    return "${s.ethMonths[e.month - 1]} ${e.day}"
}
