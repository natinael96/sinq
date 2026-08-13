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
 */
@Serializable
data class HabitSchedule(
    val kind: Kind = Kind.WEEKLY,
    val days: Set<Int> = setOf(7),
    val anchor: String = "",
    val monthDay: Int = 21,
) {
    enum class Kind { WEEKLY, EVERY_OTHER_DAY, MONTHLY }

    fun isDueOn(date: LocalDate): Boolean = when (kind) {
        Kind.WEEKLY -> date.dayOfWeek.value in days
        Kind.EVERY_OTHER_DAY -> {
            // A missing/broken anchor falls back to epoch-day parity, which is
            // still a valid every-other-day rhythm rather than a crash.
            val base = runCatching { LocalDate.parse(anchor) }.getOrNull()?.toEpochDay() ?: 0L
            (date.toEpochDay() - base).mod(2L) == 0L
        }
        Kind.MONTHLY -> EthiopianDate.from(date).day == monthDay.coerceIn(1, 30)
    }

    /**
     * The first due day on or after [from], or null when the schedule can
     * never fire (a WEEKLY with no days). The scan bound covers the longest
     * possible gap: Ethiopian day 30 across ጳጉሜ is 35–36 days.
     */
    fun nextDueOnOrAfter(from: LocalDate): LocalDate? {
        for (i in 0..SCAN_DAYS) {
            val d = from.plusDays(i.toLong())
            if (isDueOn(d)) return d
        }
        return null
    }

    companion object {
        private const val SCAN_DAYS = 40

        /** ምጽዋት: once a week on Sunday until the user says otherwise. */
        val DEFAULT_ALMS = HabitSchedule(kind = Kind.WEEKLY, days = setOf(7))

        /** ንስሐ: Saturday — the evening before Sunday's ቅዳሴ and communion. */
        val DEFAULT_REPENTANCE = HabitSchedule(kind = Kind.WEEKLY, days = setOf(6))
    }
}
