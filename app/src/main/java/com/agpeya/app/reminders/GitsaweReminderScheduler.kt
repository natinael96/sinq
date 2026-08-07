package com.agpeya.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules the morning nudge with today's ግጻዌ reading heading. Fires once around
 * [REMINDER_TIME]; the receiver re-arms the next day (chain pattern), and
 * boot/update/time changes and app launch re-arm it too.
 */
object GitsaweReminderScheduler {

    const val ACTION_GITSAWE_REMINDER = "com.agpeya.app.GITSAWE_REMINDER"
    const val EXTRA_OPEN_GITSAWE = "openGitsawe"
    private const val REQUEST_CODE = 9200
    private val REMINDER_TIME: LocalTime = LocalTime.of(6, 0)

    /** Re-arm or cancel to match the current setting; call after any toggle. */
    fun sync(context: Context, enabled: Boolean) {
        if (enabled) schedule(context) else cancel(context)
    }

    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(REMINDER_TIME)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context)
        // Exact + doze-exempt so the morning nudge lands on time (see the streak
        // scheduler note); fall back to inexact if a device ever refuses.
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
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
