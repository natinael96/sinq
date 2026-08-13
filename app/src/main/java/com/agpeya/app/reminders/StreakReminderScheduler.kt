package com.agpeya.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules the nightly nudge to close the day with prayer (a gentle
 * notification, not the ringing prayer alarm). Fires once at the user's chosen time (21:30
 * unless changed in Settings); the receiver re-arms the next day (chain
 * pattern), and boot/update/time changes re-arm too. Re-arming after a time
 * change needs no cancel: the PendingIntent is identity-matched, so setExact
 * with the same request code replaces the pending alarm.
 */
object StreakReminderScheduler {

    // Action and extra keep their historical names: the extra rides in
    // already-issued PendingIntents, and renaming buys nothing user-visible.
    const val ACTION_STREAK_REMINDER = "com.agpeya.app.STREAK_REMINDER"
    const val EXTRA_OPEN_STREAK = "openStreak"
    private const val REQUEST_CODE = 9100

    /** Re-arm or cancel to match the current setting; call after any toggle. */
    fun sync(context: Context, enabled: Boolean) {
        if (enabled) schedule(context) else cancel(context)
    }

    fun schedule(context: Context) {
        val minute = com.agpeya.app.data.SettingsRepository.streakReminderTimeBlocking(context)
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(minute / 60, minute % 60))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context)
        // Exact + doze-exempt so the nightly nudge actually lands at 21:30. Plain
        // setAndAllowWhileIdle is inexact and, once the app falls into the
        // App-Standby "rare" bucket (a day or two unopened), gets throttled to
        // ~once a day and deferred to a Doze maintenance window — the nudge then
        // silently misses days at a time. USE_EXACT_ALARM is declared, so exact
        // scheduling is normally granted; fall back to inexact if ever refused.
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
        val intent = Intent(context, StreakReminderReceiver::class.java)
            .setAction(ACTION_STREAK_REMINDER)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
