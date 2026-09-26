package com.agpeya.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.agpeya.app.data.SettingsRepository
import java.time.ZonedDateTime
import java.time.LocalTime

/** Automatic local-time reading reminders; each delivery arms the next slot. */
object ReadingReminderScheduler {

    const val ACTION_READING_REMINDER = "com.agpeya.app.READING_REMINDER"
    const val EXTRA_OPEN_READING = "openReading"
    private const val REQUEST_CODE = 9300

    /** Re-arm or cancel to match the current setting; call after any toggle. */
    fun sync(context: Context, enabled: Boolean) {
        if (enabled) schedule(context) else cancel(context)
    }

    val REMINDER_TIMES: List<LocalTime> = listOf(
        LocalTime.of(6, 30),
        LocalTime.of(14, 0),
        LocalTime.of(20, 0),
    )

    /** Strictly future: never replay earlier slots after launch or a clock change. */
    internal fun nextReminder(now: ZonedDateTime): ZonedDateTime =
        (0L..1L).flatMap { offset ->
            REMINDER_TIMES.map { time -> now.toLocalDate().plusDays(offset).atTime(time).atZone(now.zone) }
        }.first { it.isAfter(now) }

    fun schedule(context: Context) = ReminderDispatchGate.locked {
        if (!SettingsRepository.readingReminderBlocking(context)) return@locked cancel(context)
        val triggerAt = nextReminder(ZonedDateTime.now()).toInstant().toEpochMilli()
        // Doze-exempt, like the nightly nudge. Android may still delay delivery;
        // the receiver always schedules the next future slot, not a catch-up burst.
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
    }

    fun cancel(context: Context) = ReminderDispatchGate.locked {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
        context.getSystemService(android.app.NotificationManager::class.java)
            .cancel(NotificationIds.READING)
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
