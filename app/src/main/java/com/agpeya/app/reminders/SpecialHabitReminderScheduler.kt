package com.agpeya.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.SpecialReminder
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The two scheduled intentions — ምጽዋት and ንስሐ. Deliberately NOT habits: the
 * app reminds and then looks away. Nothing is recorded, streaked, or shown as
 * done — alms and repentance are between the person and God, not the app
 * ("let not your left hand know what your right hand is doing").
 *
 * Each intention now holds a LIST of reminders (see [SpecialReminder]); a
 * person can keep several, each with its own label, cadence and time. The enum
 * carries only what is shared per intention: the broadcast [action] (used by
 * the receiver to tell alms from repentance) and the notification [channelId].
 * Per-entry PendingIntent request codes and notification ids are derived from
 * the entry id so siblings never collide.
 */
enum class SpecialHabit(
    val action: String,
    val channelId: String,
) {
    ALMS("com.agpeya.app.ALMS_REMINDER", "alms_reminders"),
    REPENTANCE("com.agpeya.app.REPENTANCE_REMINDER", "repentance_reminders"),
}

/**
 * Schedules the ምጽዋት and ንስሐ nudges (gentle notifications, not ringing
 * alarms). Each enabled entry is armed for its next DUE day at its chosen time;
 * the receiver re-arms that entry on fire (chain pattern), and boot / update /
 * time changes plus app launch re-arm the whole list.
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
            if (!entry.enabled) continue
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

    /** Re-arm one entry for its next due day — used by the chain after a fire. */
    fun scheduleNext(context: Context, habit: SpecialHabit, entry: SpecialReminder) {
        val at = nextOccurrence(entry, LocalDateTime.now()) ?: return
        val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context, habit, entry.id)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    /** Next due day whose firing time is still ahead of [now], or null. */
    fun nextOccurrence(entry: SpecialReminder, now: LocalDateTime): LocalDateTime? {
        val time = LocalTime.of(entry.minute / 60, entry.minute % 60)
        var dueDate = entry.schedule.nextDueOnOrAfter(now.toLocalDate()) ?: return null
        if (!dueDate.atTime(time).isAfter(now)) {
            dueDate = entry.schedule.nextDueOnOrAfter(dueDate.plusDays(1)) ?: return null
        }
        return dueDate.atTime(time)
    }

    fun remindersOf(context: Context, habit: SpecialHabit): List<SpecialReminder> = when (habit) {
        SpecialHabit.ALMS -> SettingsRepository.almsRemindersBlocking(context)
        SpecialHabit.REPENTANCE -> SettingsRepository.repentanceRemindersBlocking(context)
    }

    private fun scheduledIdsKey(habit: SpecialHabit) = when (habit) {
        SpecialHabit.ALMS -> SettingsRepository.KEY_ALMS_SCHEDULED_IDS_PUBLIC
        SpecialHabit.REPENTANCE -> SettingsRepository.KEY_REPENTANCE_SCHEDULED_IDS_PUBLIC
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
