package com.agpeya.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.agpeya.app.MainActivity
import com.agpeya.app.R
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.stringsFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * The የመሃል ጸሎት moment arriving. The notification IS the prayer — one of the
 * four short prayers, drawn at random, in full — so it can be prayed from the
 * lock screen without opening anything. Tapping simply opens the app.
 *
 * Nothing is recorded or streaked. After posting, the day is marked fired and
 * tomorrow's moment is armed immediately, so the reminder remains reliable
 * even if the app is not reopened and today's prayers are already marked.
 */
class BreathPrayerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BreathPrayerScheduler.ACTION) return
        val pending = goAsync()
        Thread {
            try {
                runBlocking {
                    if (!SettingsRepository.breathReminder(context).first()) return@runBlocking
                    val today = LocalDate.now().toString()
                    // One atomic DataStore edit decides the winner when two
                    // broadcasts overlap. A separate read followed by a write
                    // allowed both threads to post on the same day.
                    if (!SettingsRepository.claimBreathReminderDay(context, today)) return@runBlocking
                    // If custom quiet hours overlap the selected moment, treat
                    // today as handled and move directly to tomorrow's window.
                    if (SettingsRepository.inQuietHoursNow(context)) {
                        ReminderDispatchGate.locked {
                            if (SettingsRepository.breathReminderBlocking(context)) {
                                BreathPrayerScheduler.schedule(context)
                            }
                        }
                        return@runBlocking
                    }

                    val s = stringsFor(SettingsRepository.language(context).first())
                    val prayer = BreathPrayerScheduler.PRAYERS.random()
                    ensureChannel(context, s.breathChannelName)
                    val tap = PendingIntent.getActivity(
                        context,
                        BreathPrayerScheduler.TAP_REQUEST_CODE,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                    val notification = NotificationCompat.Builder(context, BreathPrayerScheduler.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(s.breathReminderTitle)
                        .setContentText(prayer)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(prayer))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(tap)
                        .build()
                    ReminderDispatchGate.locked {
                        if (SettingsRepository.breathReminderBlocking(context)) {
                            if (!SettingsRepository.inQuietHoursNow(context)) {
                                context.getSystemService(NotificationManager::class.java)
                                    .notify(NotificationIds.BREATH, notification)
                            }
                            BreathPrayerScheduler.schedule(context)
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun ensureChannel(context: Context, name: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        // Channels only exist from Oreo; below it there is nothing to create.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            nm.getNotificationChannel(BreathPrayerScheduler.CHANNEL_ID) == null
        ) {
            nm.createNotificationChannel(
                NotificationChannel(
                    BreathPrayerScheduler.CHANNEL_ID,
                    name,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }
}
