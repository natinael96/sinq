package com.agpeya.app.data

import com.agpeya.app.ui.common.EthiopianDate
import java.time.LocalDate

/**
 * The prayer-day metric behind Home's Today card and the Journey screen — one
 * source of truth so the two can never disagree.
 *
 * Deliberately not a streak: it counts *distinct days with prayer* inside the
 * current period (the running fast when one is underway, the Ethiopian month
 * otherwise). A missed day changes nothing but that day, and the count stays
 * true however many days were missed. Returning after a gap is just today's
 * candle waiting to be lit.
 *
 * Prayer here means one of the hours — a `hour_<id>` record. ጉዞ counted any
 * record at all, so ቤት's candle and ጉዞ's own "prayed today" disagreed on a day
 * when only ስንክሳር had been read: one word cannot mean two things on two
 * screens. Everything else kept that day is counted too, but as keeping, not
 * as prayer.
 */
object PrayerJourney {

    /** The id prefix every prayed hour is recorded under. */
    const val HOUR_PREFIX = "hour_"

    data class Summary(
        /** An hour prayed today — whether today's candle is lit. */
        val prayedToday: Boolean,
        /** Distinct days prayed from the period's start through today. */
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

    /** Whether a day's records include one of the hours. */
    fun prayed(ids: Set<String>): Boolean = ids.any { it.startsWith(HOUR_PREFIX) }

    /**
     * Distinct days in [start]..[endInclusive] an hour was prayed. Records are
     * keyed by day, so several hours on one day count once; malformed keys
     * (e.g. from a hand-edited backup) are ignored.
     */
    fun daysPrayedBetween(
        records: Map<String, Set<String>>,
        start: LocalDate,
        endInclusive: LocalDate,
    ): Int = records.count { (key, ids) ->
        if (!prayed(ids)) return@count false
        val day = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@count false
        !day.isBefore(start) && !day.isAfter(endInclusive)
    }

    fun summarize(records: Map<String, Set<String>>, today: LocalDate): Summary {
        val prayedToday = prayed(records[today.toString()] ?: emptySet())
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
            .filter { prayed(it.value) }
            .mapNotNull { runCatching { LocalDate.parse(it.key) }.getOrNull() }
            .filter { !it.isAfter(today) }
            .maxOrNull() ?: return false // never prayed: nothing to return from
        return last.isBefore(today.minusDays(1))
    }
}
