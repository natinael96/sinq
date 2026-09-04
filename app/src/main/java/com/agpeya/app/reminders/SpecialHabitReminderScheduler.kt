package com.agpeya.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.agpeya.app.data.OfferingRepository
import com.agpeya.app.data.PenanceRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.Penance
import com.agpeya.app.model.ScheduledReminder
import com.agpeya.app.model.Vow
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The scheduled intentions the app nudges: ምጽዋት, ንስሐ, አስራት, ስዕለት and ቀኖና.
 *
 * The first two are deliberately NOT habits — the app reminds and then looks
 * away, recording nothing, because alms and repentance are between the person
 * and God ("let not your left hand know what your right hand is doing").
 *
 * አስራት and ስዕለት are different in kind and are tracked (see
 * [OfferingRepository]): a tithe is a reckoning and a vow is a debt willingly
 * taken on, and neither can be kept by someone who cannot see where they
 * stand. A ቀኖና is a debt too — assigned rather than chosen — and is tracked
 * the same way, but as confessional material (see [PenanceRepository]): its
 * notifications never carry its label. The reminder machinery is identical
 * for all five, so it is shared; only the source of the entries and the
 * notification wording differ.
 *
 * Each intention holds a LIST of entries, each with its own label, cadence and
 * time. The enum carries what is shared per intention: the broadcast [action]
 * (the receiver's way of telling them apart) and the notification [channelId].
 * Per-entry PendingIntent request codes and notification ids derive from the
 * entry id so siblings never collide.
 */
enum class SpecialHabit(
    val action: String,
    val channelId: String,
) {
    ALMS("com.agpeya.app.ALMS_REMINDER", "alms_reminders"),
    REPENTANCE("com.agpeya.app.REPENTANCE_REMINDER", "repentance_reminders"),
    TITHE("com.agpeya.app.TITHE_REMINDER", "tithe_reminders"),
    VOW("com.agpeya.app.VOW_REMINDER", "vow_reminders"),
    PENANCE("com.agpeya.app.PENANCE_REMINDER", "penance_reminders"),
}

/**
 * Schedules the intention nudges (gentle notifications, not ringing alarms).
 * Each armable entry is armed for its next DUE day at its chosen time; the
 * receiver re-arms that entry on fire (chain pattern), and boot / update / time
 * changes plus app launch re-arm every list.
 */
object SpecialHabitReminderScheduler {

    const val EXTRA_ENTRY_ID = "specialEntryId"

    /** Re-arm every enabled entry and cancel any that were removed or disabled. */
    fun sync(context: Context, habit: SpecialHabit) {
        val app = context.applicationContext
        val am = app.getSystemService(AlarmManager::class.java)
        val list = remindersOf(app, habit)
        val idsKey = scheduledIdsKey(habit)

        // Cancel everything armed last time plus everything in the current list,
        // so a re-timed or disabled entry never leaves a stale alarm behind.
        val previous = SettingsRepository.scheduledIds(app, idsKey)
        (previous + list.map { it.id }).forEach { id ->
            am.cancel(pendingIntent(app, habit, id))
        }

        val now = LocalDateTime.now()
        val armed = mutableSetOf<String>()
        for (entry in list) {
            if (!entry.armable()) continue
            val at = nextOccurrence(entry, now) ?: continue
            val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val pi = pendingIntent(app, habit, entry.id)
            // Exact + doze-exempt: inexact alarms get throttled once the app
            // goes App-Standby "rare", and a weekly reminder that lands a day
            // late is worse than none.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            armed += entry.id
        }
        SettingsRepository.setScheduledIds(app, idsKey, armed)
    }

    /**
     * Whether an entry should hold an alarm at all. For most intentions that is
     * simply its switch; a one-time ስዕለት also stops once it has been kept, so
     * a discharged vow does not go on asking to be paid — and a finished ቀኖና
     * likewise.
     */
    private fun ScheduledReminder.armable(): Boolean = when (this) {
        is Vow -> remindsStill
        is Penance -> remindsStill
        else -> enabled
    }

    /** Re-arm one entry for its next due day — used by the chain after a fire. */
    fun scheduleNext(context: Context, habit: SpecialHabit, entry: ScheduledReminder) {
        val at = nextOccurrence(entry, LocalDateTime.now()) ?: return
        val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context, habit, entry.id)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    /** Next due day whose firing time is still ahead of [now], or null. */
    fun nextOccurrence(entry: ScheduledReminder, now: LocalDateTime): LocalDateTime? {
        val time = LocalTime.of(entry.minute / 60, entry.minute % 60)
        var dueDate = entry.schedule.nextDueOnOrAfter(now.toLocalDate()) ?: return null
        if (!dueDate.atTime(time).isAfter(now)) {
            dueDate = entry.schedule.nextDueOnOrAfter(dueDate.plusDays(1)) ?: return null
        }
        return dueDate.atTime(time)
    }

    fun remindersOf(context: Context, habit: SpecialHabit): List<ScheduledReminder> = when (habit) {
        SpecialHabit.ALMS -> SettingsRepository.almsRemindersBlocking(context)
        SpecialHabit.REPENTANCE -> SettingsRepository.repentanceRemindersBlocking(context)
        SpecialHabit.TITHE -> SettingsRepository.titheRemindersBlocking(context)
        SpecialHabit.VOW -> OfferingRepository.vowsBlocking(context)
        SpecialHabit.PENANCE -> PenanceRepository.penancesBlocking(context)
    }

    /** Arm every intention at once — app launch, boot, time change. */
    fun syncAll(context: Context) {
        SpecialHabit.entries.forEach { sync(context, it) }
    }

    private fun scheduledIdsKey(habit: SpecialHabit) = when (habit) {
        SpecialHabit.ALMS -> SettingsRepository.KEY_ALMS_SCHEDULED_IDS_PUBLIC
        SpecialHabit.REPENTANCE -> SettingsRepository.KEY_REPENTANCE_SCHEDULED_IDS_PUBLIC
        SpecialHabit.TITHE -> SettingsRepository.KEY_TITHE_SCHEDULED_IDS_PUBLIC
        SpecialHabit.VOW -> SettingsRepository.KEY_VOW_SCHEDULED_IDS_PUBLIC
        SpecialHabit.PENANCE -> SettingsRepository.KEY_PENANCE_SCHEDULED_IDS_PUBLIC
    }

    private fun requestCode(habit: SpecialHabit, entryId: String): Int =
        "${habit.action}:$entryId".hashCode()

    private fun pendingIntent(context: Context, habit: SpecialHabit, entryId: String): PendingIntent {
        val intent = Intent(context, SpecialHabitReminderReceiver::class.java)
            .setAction(habit.action)
            .setData(android.net.Uri.parse("agpeya://special/${habit.name}/$entryId"))
            .putExtra(EXTRA_ENTRY_ID, entryId)
        return PendingIntent.getBroadcast(
            context,
            requestCode(habit, entryId),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
