package com.agpeya.app.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.agpeya.app.data.SettingsRepository
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules the morning nudge with today's ግጻዌ reading heading. Fires once around
 * the time chosen in Settings; the receiver re-arms the next day (chain pattern), and
 * boot/update/time changes and app launch re-arm it too.
 */
object GitsaweReminderScheduler {

    const val ACTION_GITSAWE_REMINDER = "com.agpeya.app.GITSAWE_REMINDER"
    const val EXTRA_OPEN_GITSAWE = "openGitsawe"
    private const val REQUEST_CODE = 9200
    /** The editable preference as a clock time, with 06:00 as its safe default. */
    fun reminderTime(context: Context): LocalTime {
        val minute = SettingsRepository.gitsaweReminderTimeBlocking(context)
        return LocalTime.of(minute / 60, minute % 60)
    }

    /** Re-arm or cancel to match the current setting; call after any toggle. */
    fun sync(context: Context, enabled: Boolean) {
        if (enabled) schedule(context) else cancel(context)
    }

    @SuppressLint("MissingPermission") // Exact scheduling is guarded and has an inexact fallback.
    fun schedule(context: Context) = ReminderDispatchGate.locked {
        if (!SettingsRepository.gitsaweReminderBlocking(context)) return@locked cancel(context)
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(reminderTime(context))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context)
        // Use the exact-alarm access already needed by the prayer clock when it
        // is available; managed devices may revoke it, so always keep a safe
        // inexact fallback rather than losing the reminder entirely.
        try {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
                am.canScheduleExactAlarms()
            ) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context) = ReminderDispatchGate.locked {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
        context.getSystemService(android.app.NotificationManager::class.java)
            .cancel(NotificationIds.GITSAWE)
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GitsaweReminderReceiver::class.java)
            .setAction(ACTION_GITSAWE_REMINDER)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
