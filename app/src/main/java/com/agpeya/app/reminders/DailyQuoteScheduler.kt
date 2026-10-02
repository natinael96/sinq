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
 * Schedules the daily refresh for the lock screen quote at 06:00.
 */
object DailyQuoteScheduler {

    const val ACTION_DAILY_QUOTE = "com.agpeya.app.DAILY_QUOTE"
    private const val REQUEST_CODE = 9600
    private val REFRESH_TIME = LocalTime.of(6, 0)

    fun sync(context: Context, enabled: Boolean) {
        if (enabled) {
            DailyQuoteReceiver.postNow(context)
            schedule(context)
        } else {
            cancel(context)
        }
    }

    @SuppressLint("MissingPermission")
    fun schedule(context: Context) = ReminderDispatchGate.locked {
        if (!SettingsRepository.dailyQuoteLockscreenBlocking(context)) return@locked cancel(context)
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(REFRESH_TIME)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context)
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
        DailyQuoteReceiver.cancel(context)
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyQuoteReceiver::class.java)
            .setAction(ACTION_DAILY_QUOTE)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
