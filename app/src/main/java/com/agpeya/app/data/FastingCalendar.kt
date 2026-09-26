package com.agpeya.app.data

import com.agpeya.app.ui.common.EthiopianDate
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * አጽዋማት — the fasts of the Ethiopian Orthodox Tewahedo church year.
 *
 * The movable fasts hang off Fasika via [BahreHasab]; the fixed ones sit on
 * Ethiopian calendar dates. The two weekly fasts (ረቡዕ and ዓርብ) are a rule rather
 * than a period, so they're reported separately by [isWeeklyFastDay].
 */
object FastingCalendar {

    /** One fast: its span, and whether the church counts it as obligatory for all. */
    data class Fast(
        val key: String,
        val nameAm: String,
        val nameEn: String,
        val start: LocalDate,
        /** Inclusive last day of the fast. */
        val end: LocalDate,
        val movable: Boolean,
    ) {
        fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)
        val days: Int get() = (end.toEpochDay() - start.toEpochDay()).toInt() + 1
    }

    /**
     * Every fast of the Ethiopian year [ethYear], in calendar order.
     *
     * Note on lengths: ጾመ ሐዋርያት runs from the day after Pentecost to ሐምሌ ፬
     * (the eve of Peter and Paul), so its length varies with Fasika — it is
     * computed from both ends rather than assumed. ጾመ ድህነት (the Wednesday/Friday
     * rule) is not listed here; see [isWeeklyFastDay].
     */
    fun fastsOf(ethYear: Int): List<Fast> {
        val nineveh = BahreHasab.nineveh(ethYear)
        val fasika = BahreHasab.fasika(ethYear)

        // Inclusive fixed endpoint: ሐምሌ ፬. Do not extend this to the feast day.
        val apostlesStart = BahreHasab.apostlesFast(ethYear)
        val apostlesEnd = EthiopianDate(ethYear, 11, 4).toGregorian()

        return listOf(
            Fast(
                key = "nineveh",
                nameAm = "ጾመ ነነዌ",
                nameEn = "Fast of Nineveh",
                start = nineveh,
                end = nineveh.plusDays(2),          // Monday–Wednesday
                movable = true,
            ),
            Fast(
                key = "abiy",
                nameAm = "ዐቢይ ጾም",
                nameEn = "Great Lent",
                start = BahreHasab.greatLentStart(ethYear),
                end = fasika.minusDays(1),          // through Holy Saturday
                movable = true,
            ),
            Fast(
                key = "hawaryat",
                nameAm = "ጾመ ሐዋርያት",
                nameEn = "Fast of the Apostles",
                start = apostlesStart,
                end = apostlesEnd,
                movable = true,
            ),
            Fast(
                key = "filseta",
                nameAm = "ጾመ ፍልሰታ",
                nameEn = "Fast of the Assumption",
                start = EthiopianDate(ethYear, 12, 1).toGregorian(),   // ነሐሴ ፩
                end = EthiopianDate(ethYear, 12, 15).toGregorian(),    // ነሐሴ ፲፭
                movable = false,
            ),
            Fast(
                key = "nebiyat",
                nameAm = "ጾመ ነቢያት",
                nameEn = "Fast of the Prophets (Advent)",
                start = EthiopianDate(ethYear, 3, 15).toGregorian(),   // ኅዳር ፲፭
                end = EthiopianDate(ethYear, 4, 28).toGregorian(),     // ታኅሣሥ ፳፰, eve of ልደት
                movable = false,
            ),
            Fast(
                key = "gahad",
                nameAm = "ጾመ ገሃድ",
                nameEn = "Fast of the Eve",
                start = EthiopianDate(ethYear, 4, 28).toGregorian(),
                end = EthiopianDate(ethYear, 4, 28).toGregorian(),
                movable = false,
            ),
        ).sortedBy { it.start }
    }

    /** The fast covering [date], if any. */
    fun fastOn(date: LocalDate): Fast? {
        val year = EthiopianDate.from(date).year
        // A fast can straddle the new year, so check the neighbouring years too.
        return (year - 1..year + 1)
            .flatMap { runCatching { fastsOf(it) }.getOrDefault(emptyList()) }
            .firstOrNull { it.contains(date) }
    }

    /**
     * ረቡዕ and ዓርብ are fasting days year-round, except between Fasika and
     * Pentecost (the ኃምሳ, when the church does not fast).
     */
    fun isWeeklyFastDay(date: LocalDate): Boolean {
        if (date.dayOfWeek != DayOfWeek.WEDNESDAY && date.dayOfWeek != DayOfWeek.FRIDAY) return false
        val year = EthiopianDate.from(date).year
        val fasika = runCatching { BahreHasab.fasika(year) }.getOrNull() ?: return true
        val pentecost = runCatching { BahreHasab.pentecost(year) }.getOrNull() ?: return true
        val inPaschalSeason = !date.isBefore(fasika) && !date.isAfter(pentecost)
        return !inPaschalSeason
    }
}
