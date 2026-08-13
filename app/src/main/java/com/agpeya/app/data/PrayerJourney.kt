package com.agpeya.app.data

import com.agpeya.app.ui.common.EthiopianDate
import java.time.LocalDate

/**
 * The prayer-day metric behind Home's Today card and the Journey screen — one
 * source of truth so the two can never disagree.
 *
 * Deliberately not a streak: it counts *distinct days with prayer activity*
 * inside the current period (the running fast when one is underway, the
 * Ethiopian month otherwise). A missed day changes nothing but that day, and
 * the count stays true however many days were missed. Returning after a gap is
 * just today's candle waiting to be lit.
 */
object PrayerJourney {

    data class Summary(
        /** Any activity recorded today — whether today's candle is lit. */
        val prayedToday: Boolean,
        /** Distinct days with activity from the period's start through today. */
        val daysPrayed: Int,
        /** The fast the period follows, or null when it is the Ethiopian month. */
        val fast: FastingCalendar.Fast?,
        /** 1-based day of the fast today is, when [fast] is set. */
        val fastDay: Int?,
        /** There is history, but nothing today or yesterday: a quiet welcome
         *  back, never a loss notice. Today still unmarked with yesterday
         *  logged is an ordinary morning, not a return. */
        val returning: Boolean,
    )

    /**
     * Distinct days in [start]..[endInclusive] with at least one record.
     * Records are keyed by day, so several prayers on one day count once;
     * malformed keys (e.g. from a hand-edited backup) are ignored.
     */
    fun daysPrayedBetween(
        records: Map<String, Set<String>>,
        start: LocalDate,
        endInclusive: LocalDate,
    ): Int = records.count { (key, ids) ->
        if (ids.isEmpty()) return@count false
        val day = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@count false
        !day.isBefore(start) && !day.isAfter(endInclusive)
    }

    fun summarize(records: Map<String, Set<String>>, today: LocalDate): Summary {
        val prayedToday = records[today.toString()]?.isNotEmpty() == true
        val fast = runCatching { FastingCalendar.fastOn(today) }.getOrNull()
        val periodStart = fast?.start ?: EthiopianDate.from(today).let {
            EthiopianDate(it.year, it.month, 1).toGregorian()
        }
        return Summary(
            prayedToday = prayedToday,
            daysPrayed = daysPrayedBetween(records, periodStart, today),
            fast = fast,
            fastDay = fast?.let { (today.toEpochDay() - it.start.toEpochDay()).toInt() + 1 },
            returning = !prayedToday && hasGapBeforeToday(records, today),
        )
    }

    private fun hasGapBeforeToday(records: Map<String, Set<String>>, today: LocalDate): Boolean {
        val last = records.asSequence()
            .filter { it.value.isNotEmpty() }
            .mapNotNull { runCatching { LocalDate.parse(it.key) }.getOrNull() }
            .filter { !it.isAfter(today) }
            .maxOrNull() ?: return false // never prayed: nothing to return from
        return last.isBefore(today.minusDays(1))
    }
}
