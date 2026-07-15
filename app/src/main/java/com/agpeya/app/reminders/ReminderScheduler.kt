package com.agpeya.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.agpeya.app.MainActivity
import com.agpeya.app.data.ModesRepository
import com.agpeya.app.model.ReminderEntry
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Derives the alarm schedule from the active mode (PLAN.md §8.3–8.4).
 * Chain pattern: each alarm fires once; AlarmReceiver schedules the entry's
 * next occurrence. Uses setAlarmClock for true alarm-clock behaviour: exact,
 * doze-exempt, and allowed without the SCHEDULE_EXACT_ALARM permission.
 */
object ReminderScheduler {

    const val EXTRA_ENTRY_ID = "entryId"
    const val EXTRA_HOUR_ID = "hourId"
    const val EXTRA_HOUR_NAME = "hourName"
    const val EXTRA_SNOOZE = "snooze"
    const val SNOOZE_MINUTES = 10

    /** Schedule a one-shot alarm [SNOOZE_MINUTES] from now for the same hour. */
    fun snooze(context: Context, hourId: String, hourName: String) {
        val at = LocalDateTime.now().plusMinutes(SNOOZE_MINUTES.toLong())
        val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.agpeya.app.SNOOZE"
            data = android.net.Uri.parse("agpeya://snooze/$hourId")
            putExtra(EXTRA_ENTRY_ID, "snooze_$hourId")
            putExtra(EXTRA_HOUR_ID, hourId)
            putExtra(EXTRA_HOUR_NAME, hourName)
            putExtra(EXTRA_SNOOZE, true)
        }
        val pi = PendingIntent.getBroadcast(
            context, "snooze_$hourId".hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val showPi = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val am = context.getSystemService(AlarmManager::class.java)
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showPi), pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /** Next fire time for an entry, strictly after [now]. */
    fun nextOccurrence(entry: ReminderEntry, now: LocalDateTime): LocalDateTime? {
        if (entry.days.isEmpty()) return null
        for (offset in 0..7L) {
            val date = now.toLocalDate().plusDays(offset)
            if (date.dayOfWeek.value !in entry.days) continue
            val candidate = date.atTime(entry.hour, entry.minute)
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }

    /** Cancel everything previously scheduled, then schedule the active mode's enabled entries. */
    suspend fun rescheduleAll(context: Context, hourNames: Map<String, String>) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(AlarmManager::class.java)

        for (id in ModesRepository.scheduledIds(app)) {
            pendingIntent(app, id, null, null, create = false)?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }

        val state = ModesRepository.current(app)
        val active = state.activeMode ?: return ModesRepository.setScheduledIds(app, emptySet())
        val now = LocalDateTime.now()
        val scheduled = mutableSetOf<String>()

        for (entry in active.entries) {
            if (!entry.enabled) continue
            // Hidden hours don't ring: hourNames is built from the visible hours.
            if (entry.hourId !in hourNames) continue
            val at = nextOccurrence(entry, now) ?: continue
            scheduleAt(app, alarmManager, entry, hourNames[entry.hourId] ?: entry.hourId, at)
            scheduled += entry.id
        }
        ModesRepository.setScheduledIds(app, scheduled)
    }

    /** Schedule one entry's next occurrence — used by the chain after a fire. */
    fun scheduleNext(context: Context, entry: ReminderEntry, hourName: String) {
        val at = nextOccurrence(entry, LocalDateTime.now()) ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        scheduleAt(context, alarmManager, entry, hourName, at)
    }

    private fun scheduleAt(
        context: Context,
        alarmManager: AlarmManager,
        entry: ReminderEntry,
        hourName: String,
        at: LocalDateTime,
    ) {
        val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = pendingIntent(context, entry.id, entry.hourId, hourName, create = true)!!
        val showPi = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // Prefer an exact alarm-clock (USE_EXACT_ALARM is declared, normally auto-granted).
        // Fall back to an inexact alarm if a device/policy ever refuses, so we never crash.
        try {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showPi), pi)
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun pendingIntent(
        context: Context,
        entryId: String,
        hourId: String?,
        hourName: String?,
        create: Boolean,
    ): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.agpeya.app.REMINDER"
            data = android.net.Uri.parse("agpeya://reminder/$entryId")
            putExtra(EXTRA_ENTRY_ID, entryId)
            if (hourId != null) putExtra(EXTRA_HOUR_ID, hourId)
            if (hourName != null) putExtra(EXTRA_HOUR_NAME, hourName)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(context, entryId.hashCode(), intent, flags)
    }
}
