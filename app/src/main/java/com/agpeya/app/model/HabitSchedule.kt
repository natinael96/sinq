package com.agpeya.app.model

import com.agpeya.app.ui.common.EthiopianDate
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * When a scheduled intention (ምጽዋት, ንስሐ) is due — reminders that, unlike the
 * daily habits, recur on a cadence the user chooses. Nothing about them is
 * recorded; the schedule only drives the reminder alarms:
 *
 * - [Kind.WEEKLY]: on chosen ISO weekdays (Monday=1..Sunday=7). One day is
 *   "once a week"; three days is "three times a week".
 * - [Kind.EVERY_OTHER_DAY]: every second day, counted from [anchor] (the day
 *   the option was chosen, so it is due immediately and then alternates).
 * - [Kind.MONTHLY]: on day [monthDay] of the ETHIOPIAN month — monthly
 *   observance follows the ግእዝ calendar here, like everything else in the
 *   app. Months 1..12 are 30 days, so any monthDay always lands; ጳጉሜ has no
 *   day above 6, so the due day simply skips to the next month.
 * - [Kind.YEARLY]: once a year, on Ethiopian month [monthNum] day [monthDay] —
 *   a ስዕለት kept on a saint's annual feast, or on any date of one's own.
 * - [Kind.FEAST]: on a named feast from [HolidayCalendar], [feastKey]. The only
 *   cadence whose date moves: ፋሲካ and everything hanging off it are resolved
 *   through the ባሕረ ሓሳብ each year rather than stored.
 */
@Serializable
data class HabitSchedule(
    val kind: Kind = Kind.WEEKLY,
    val days: Set<Int> = setOf(7),
    val anchor: String = "",
    val monthDay: Int = 21,
    /** Ethiopian month 1..13, for [Kind.YEARLY]. */
    val monthNum: Int = 1,
    /** A [HolidayCalendar] annual feast key, for [Kind.FEAST]. */
    val feastKey: String = "",
) {
    enum class Kind { WEEKLY, EVERY_OTHER_DAY, MONTHLY, YEARLY, FEAST }

    fun isDueOn(date: LocalDate): Boolean = when (kind) {
        Kind.WEEKLY -> date.dayOfWeek.value in days
        Kind.EVERY_OTHER_DAY -> {
            // A missing/broken anchor falls back to epoch-day parity, which is
            // still a valid every-other-day rhythm rather than a crash.
            val base = runCatching { LocalDate.parse(anchor) }.getOrNull()?.toEpochDay() ?: 0L
            (date.toEpochDay() - base).mod(2L) == 0L
        }
        Kind.MONTHLY -> EthiopianDate.from(date).day == monthDay.coerceIn(1, 30)
        Kind.YEARLY -> {
            val e = EthiopianDate.from(date)
            e.month == monthNum.coerceIn(1, 13) && e.day == monthDay.coerceIn(1, 30)
        }
        Kind.FEAST -> feastKey.isNotBlank() &&
            com.agpeya.app.data.HolidayCalendar.isFeastOn(feastKey, date)
    }

    /**
     * The first due day on or after [from], or null when the schedule can
     * never fire (a WEEKLY with no days, a FEAST whose key no longer exists).
     *
     * The scan bound depends on the cadence. Weekly and monthly gaps are short
     * — Ethiopian day 30 across ጳጉሜ is 35–36 days — but a yearly feast can be
     * a full year away, and ፋሲካ moves by five weeks between years, so the
     * annual cadences scan a year and a half rather than assuming a fixed gap.
     */
    fun nextDueOnOrAfter(from: LocalDate): LocalDate? {
        val bound = when (kind) {
            Kind.YEARLY, Kind.FEAST -> ANNUAL_SCAN_DAYS
            else -> SCAN_DAYS
        }
        for (i in 0..bound) {
            val d = from.plusDays(i.toLong())
            if (isDueOn(d)) return d
        }
        return null
    }

    companion object {
        private const val SCAN_DAYS = 40
        private const val ANNUAL_SCAN_DAYS = 550

        /** ምጽዋት: once a week on Sunday until the user says otherwise. */
        val DEFAULT_ALMS = HabitSchedule(kind = Kind.WEEKLY, days = setOf(7))

        /** ንስሐ: Saturday — the evening before Sunday's ቅዳሴ and communion. */
        val DEFAULT_REPENTANCE = HabitSchedule(kind = Kind.WEEKLY, days = setOf(6))
    }
}
