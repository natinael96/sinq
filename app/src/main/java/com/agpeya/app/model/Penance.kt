package com.agpeya.app.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * The shape a ቀኖና usually takes: bows, days of fasting, alms, prayers, or
 * whatever else a ንስሐ አባት sees fit to give.
 */
@Serializable
enum class PenanceKind { PROSTRATIONS, FASTING_DAYS, ALMS, PRAYERS, OTHER }

/** One session recorded against a [Penance] — ፳ ስግደት of the ፵ given. */
@Serializable
data class PenanceProgress(
    val id: String,
    val date: String,
    val amount: Int = 0,
    val note: String = "",
) {
    val localDate: LocalDate? get() = runCatching { LocalDate.parse(date) }.getOrNull()
}

/**
 * A ቀኖና — the rule of penance a ንስሐ አባት assigns after confession.
 *
 * Like a ስዕለት it is a debt with a measure ("፵ ስግደት", "ሦስት ቀን ጾም"), so it is
 * tracked the same way: a quota, records against it, and reminders that stop
 * once it is discharged. Unlike a vow it is never standing — a penance is
 * given once and finished once, so there is no [Vow.oneTime] switch; settling
 * IS the end.
 *
 * And unlike a vow it is confessional material. It lives in its own store that
 * backup never touches, its reminders never carry its label, and its screen
 * sits behind the journal lock. What was confessed, and what was given for it,
 * stays between the person, their ንስሐ አባት, and God.
 */
@Serializable
data class Penance(
    override val id: String,
    override val label: String = "",
    override val schedule: HabitSchedule = HabitSchedule(),
    override val minute: Int = 6 * 60,
    override val enabled: Boolean = true,
    val kind: PenanceKind = PenanceKind.OTHER,
    /** The measure given — ፵ ስግደት, 3 days; 0 for a rule with no count. */
    val quota: Int = 0,
    /** ISO-8601 Gregorian date the ቀኖና was received. */
    val assignedDate: String = "",
    val progress: List<PenanceProgress> = emptyList(),
) : ScheduledReminder {

    /** Everything performed so far. */
    val done: Int get() = progress.sumOf { it.amount }

    /** What is still owed on a counted penance; 0 once it is met. */
    val remaining: Int get() = (quota - done).coerceAtLeast(0)

    /**
     * True once the penance is discharged. A counted one settles when the count
     * is met; an unmeasured one is settled by any record at all — there is
     * nothing else to measure it against.
     */
    val settled: Boolean
        get() = if (quota > 0) done >= quota else progress.isNotEmpty()

    /** A penance asks until it is finished, and then never again. */
    val remindsStill: Boolean get() = enabled && !settled
}
