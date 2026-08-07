package com.agpeya.app.data

import com.agpeya.app.ui.common.EthiopianDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * ባሕረ ሓሳብ — the Ethiopian computus. Locates the movable fasts and feasts of the
 * church year (which depend on Fasika/Easter) so seasonal ግጻዌ readings can be
 * matched to a calendar date.
 *
 * Fasika follows the Alexandrian paschal cycle — identical to Orthodox (Julian)
 * Easter — so it is computed with the Julian computus and shifted to the
 * Gregorian calendar. Every movable feast is then a fixed offset from ጾመ ነነዌ
 * (the Nineveh fast), per the classical Tewsak table. The classical intermediate
 * numbers (Wenber/Abekte/Metqi) are exposed too; all are locked by unit tests
 * against known Easter dates.
 */
object BahreHasab {

    // ---- Classical values ----------------------------------------------------

    /** Years since creation: the Ethiopian year + 5500. */
    fun ameteAlem(ethYear: Int): Int = ethYear + 5500

    /** Evangelist of the year: 1 Matthew, 2 Mark, 3 Luke, 0 John. */
    fun evangelist(ethYear: Int): Int = ameteAlem(ethYear) % 4

    /** Wenber — seeds Abekte and Metqi (Medeb − 1, wrapping 0 → 18). */
    fun wenber(ethYear: Int): Int {
        val medeb = ameteAlem(ethYear) % 19
        return if (medeb == 0) 18 else medeb - 1
    }

    /** Abekte — the solar/lunar difference. */
    fun abekte(ethYear: Int): Int = (wenber(ethYear) * 11) % 30

    /** Metqi — used to seat the paschal cycle (Abekte + Metqi = 30). */
    fun metqi(ethYear: Int): Int = (wenber(ethYear) * 19) % 30

    // ---- Movable feasts (Gregorian dates) -----------------------------------

    /**
     * Fasika (Easter) for an Ethiopian year. Ethiopian year E's Fasika falls in
     * ሚያዝያ (month 8), i.e. April/May of Gregorian year E + 8.
     */
    fun fasika(ethYear: Int): LocalDate = orthodoxEaster(ethYear + 8)

    /** ጾመ ነነዌ — the Nineveh fast (Monday), 69 days before Fasika. */
    fun nineveh(ethYear: Int): LocalDate = fasika(ethYear).minusDays(NINEVEH_TO_FASIKA)

    fun greatLentStart(ethYear: Int): LocalDate = nineveh(ethYear).plusDays(14)   // ዐቢይ ጾም
    fun debreZeit(ethYear: Int): LocalDate = nineveh(ethYear).plusDays(41)
    fun hosanna(ethYear: Int): LocalDate = nineveh(ethYear).plusDays(62)           // Palm Sunday
    fun siklet(ethYear: Int): LocalDate = nineveh(ethYear).plusDays(67)            // Good Friday
    fun rikbeKahnat(ethYear: Int): LocalDate = nineveh(ethYear).plusDays(93)
    fun ascension(ethYear: Int): LocalDate = nineveh(ethYear).plusDays(108)        // ዕርገት, Fasika + 39
    fun pentecost(ethYear: Int): LocalDate = nineveh(ethYear).plusDays(118)        // ጰራቅሊጦስ, Fasika + 49
    fun apostlesFast(ethYear: Int): LocalDate = nineveh(ethYear).plusDays(119)     // ጾመ ሐዋርያት

    // ---- Season resolution ---------------------------------------------------

    /** A movable season and, where it has weekly readings, the week within it. */
    data class SeasonWindow(val season: String, val week: Int?)

    /**
     * Which movable season a date falls in, matching the `season` keys used by
     * the seasonal ግጻዌ data — or null if the date isn't within a movable window.
     * Covers the Nineveh fast, the eight Sundays of Great Lent (ዘወረደ … ሆሳዕና),
     * Holy Thursday, and the Sundays of the Resurrection season through Pentecost.
     */
    fun movableSeasonOn(date: LocalDate): SeasonWindow? {
        val year = EthiopianDate.from(date).year
        val off = ChronoUnit.DAYS.between(nineveh(year), date).toInt()
        return when {
            off in 0..2 -> SeasonWindow("neneweTsom", 1)                 // Nineveh fast, Mon–Wed
            off in 13..62 && (off - 13) % 7 in 0..6 && off <= 62 ->      // Great Lent Sundays 1..8
                SeasonWindow("abiyTsom", (off - 13) / 7 + 1)
            off == 66 -> SeasonWindow("holy_thursday", null)             // ዘጸሎተ ሐሙስ
            off == 108 -> SeasonWindow("erget", null)                    // ዕርገት (Ascension)
            off in 69..117 && (off - 69) % 7 == 0 ->                     // Resurrection Sundays
                SeasonWindow("tnsae", (off - 69) / 7 + 1)
            else -> null
        }
    }

    // ---- Internal ------------------------------------------------------------

    private const val NINEVEH_TO_FASIKA = 69L

    /**
     * Orthodox (Julian-computus) Easter for a Gregorian year, returned as a
     * Gregorian date. Meeus's Julian algorithm gives the Julian-calendar date;
     * adding the year's Julian→Gregorian offset shifts it to the Gregorian one.
     */
    private fun orthodoxEaster(year: Int): LocalDate {
        val a = year % 4
        val b = year % 7
        val c = year % 19
        val d = (19 * c + 15) % 30
        val e = (2 * a + 4 * b - d + 34) % 7
        val n = d + e + 114
        val month = n / 31          // 3 = March, 4 = April (Julian)
        val day = n % 31 + 1
        val julianOffset = year / 100 - year / 400 - 2
        return LocalDate.of(year, month, day).plusDays(julianOffset.toLong())
    }
}
