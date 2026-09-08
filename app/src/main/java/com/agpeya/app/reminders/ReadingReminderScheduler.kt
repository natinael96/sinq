package com.agpeya.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.agpeya.app.data.SettingsRepository
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Arms the reading plan's daily nudge (06:30 unless changed). Same chain as the
 * other reminders: the receiver re-arms tomorrow, and boot, update and
 * time changes re-arm too.
 */
object ReadingReminderScheduler {

    const val ACTION_READING_REMINDER = "com.agpeya.app.READING_REMINDER"
    const val EXTRA_OPEN_READING = "openReading"
    private const val REQUEST_CODE = 9300

    /** Re-arm or cancel to match the current setting; call after any toggle. */
    fun sync(context: Context, enabled: Boolean) {
        if (enabled) schedule(context) else cancel(context)
    }

    fun schedule(context: Context) {
        val minute = SettingsRepository.readingReminderTimeBlocking(context)
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(minute / 60, minute % 60))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Doze-exempt, like the nightly nudge: once the app falls into the
        // App-Standby "rare" bucket an inexact alarm is throttled to about once
        // a day, which is exactly the case this reminder exists for.
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReadingReminderReceiver::class.java)
            .setAction(ACTION_READING_REMINDER)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
