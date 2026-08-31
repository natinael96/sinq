package com.agpeya.app.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * What every alarm-bearing entry in the app has in common: an identity, a
 * cadence, a time of day, and a switch. [SpecialReminder] (ምጽዋት, ንስሐ, አስራት)
 * and [Vow] (ስዕለት) both satisfy it, so one scheduler arms all four rather than
 * four near-identical copies of the same AlarmManager dance.
 */
interface ScheduledReminder {
    val id: String
    val label: String
    val schedule: HabitSchedule
    val minute: Int
    val enabled: Boolean
}

/**
 * Money, in the smallest unit of whatever currency the person keeps — santim
 * for ብር. Amounts are never floats: a tithe is arithmetic someone checks, and
 * 0.1 + 0.2 quietly failing that check would be worse than useless.
 */
typealias Cents = Long

/** Whether a ledger line is money that came in, or money given away. */
@Serializable
enum class TitheEntryKind { INCOME, GIVEN }

/**
 * One line of the አስራት ledger: income received, or a tithe paid.
 *
 * Income lines exist so the app can say what a tenth of them comes to; given
 * lines are what was actually handed over. Neither is a habit and neither is
 * streaked — the ledger answers "what do I still owe", not "how good was I".
 */
@Serializable
data class TitheEntry(
    val id: String,
    val kind: TitheEntryKind,
    val amount: Cents,
    /** ISO-8601 Gregorian date; the UI shows it in the ግእዝ calendar. */
    val date: String,
    val note: String = "",
) {
    val localDate: LocalDate? get() = runCatching { LocalDate.parse(date) }.getOrNull()
}

/** One payment made toward a [Vow]. */
@Serializable
data class VowFulfilment(
    val id: String,
    val date: String,
    val amount: Cents = 0,
    val note: String = "",
) {
    val localDate: LocalDate? get() = runCatching { LocalDate.parse(date) }.getOrNull()
}

/**
 * A ስዕለት — a vow or pledge, most often promised on a feast day: "ለቅዱስ ገብርኤል,
 * every ፲፱" or "500 ብር on the coming ፋሲካ".
 *
 * Unlike ምጽዋት and ንስሐ, a vow IS tracked, because a vow is a debt a person has
 * chosen to owe and the whole point is knowing whether it has been kept. It
 * carries an optional [pledged] amount (a vow of prayer or fasting has none),
 * the [fulfilments] recorded against it, and a cadence which is usually a feast
 * from the ወርኀዊ or ዓመታዊ calendar.
 *
 * [oneTime] separates a promise kept once — after which the vow is discharged
 * and stops reminding — from a standing one kept at every recurrence.
 */
@Serializable
data class Vow(
    override val id: String,
    override val label: String = "",
    override val schedule: HabitSchedule = HabitSchedule(),
    override val minute: Int = 8 * 60,
    override val enabled: Boolean = true,
    val pledged: Cents = 0,
    val note: String = "",
    val oneTime: Boolean = false,
    val fulfilments: List<VowFulfilment> = emptyList(),
) : ScheduledReminder {

    /** Everything paid toward the vow so far. */
    val given: Cents get() = fulfilments.sumOf { it.amount }

    /** What is still owed on a vow that named an amount; 0 once it is met. */
    val remaining: Cents get() = (pledged - given).coerceAtLeast(0)

    /**
     * True once the vow is discharged. An amount-bearing vow is settled when it
     * is paid up; a vow of prayer or fasting, which names no amount, is settled
     * by any record at all — there is nothing else to measure it against.
     */
    val settled: Boolean
        get() = if (pledged > 0) given >= pledged else fulfilments.isNotEmpty()

    /**
     * A one-time vow stops reminding once it has been kept. A standing vow
     * keeps its rhythm no matter how much has been paid into it.
     */
    val remindsStill: Boolean get() = enabled && !(oneTime && settled)
}
