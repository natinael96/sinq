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

/**
 * Ethiopian date with the Gregorian one alongside — "ሐምሌ 8 · Aug 10, 2026".
 *
 * The church date is what the app is organised around, so it leads; the
 * Gregorian date follows for anyone reconciling with a phone calendar.
 */
fun formatEthiopianWithGregorian(date: LocalDate, s: Strings): String =
    "${formatEthiopianShort(date, s)} · ${formatGregorianShort(date, s)}"

/** "Aug 10, 2026" in English; "10/08/2026" under Amharic, where the month
 *  abbreviations would be Gregorian names in Ethiopic script and read oddly. */
fun formatGregorianShort(date: LocalDate, s: Strings): String =
    if (s.usesGregorianMonthNames) {
        "${s.gregorianMonths[date.monthValue - 1]} ${date.dayOfMonth}, ${date.year}"
    } else {
        "%02d/%02d/%d".format(date.dayOfMonth, date.monthValue, date.year)
    }

/**
 * The movable liturgical season [date] falls in, ready to display — or null on
 * a day outside any of them. The window itself comes from [BahreHasab]; this
 * only names it.
 *
 * Note: which days of a season's week the readings apply to is still under
 * liturgical review (docs/LITURGICAL_REVIEW.md). The week NUMBER shown here is
 * plain arithmetic off the season's start and is not affected by that question.
 */
fun liturgicalSeasonLabel(date: LocalDate, s: Strings): String? {
    val window = runCatching { com.agpeya.app.data.BahreHasab.movableSeasonOn(date) }.getOrNull()
        ?: return null
    val name = s.seasonName(window.season) ?: return null
    val week = window.week ?: return name
    return s.seasonWithWeek(name, week)
}
