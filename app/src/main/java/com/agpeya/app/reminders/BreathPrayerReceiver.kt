package com.agpeya.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.agpeya.app.MainActivity
import com.agpeya.app.R
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.stringsFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime

/**
 * The የመሃል ጸሎት moment arriving. The notification IS the prayer — one of the
 * four short prayers, drawn at random, in full — so it can be prayed from the
 * lock screen without opening anything. Tapping simply opens the app.
 *
 * Nothing is recorded or streaked, and nothing re-arms here: the day is marked
 * fired, and tomorrow's moment is rolled by the next app launch or recorded
 * prayer, whose timing shapes tomorrow's window anyway.
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
                    // Once a day, however many times the schedule was recomputed.
                    if (SettingsRepository.breathLastFiredDayBlocking(context) == today) return@runBlocking
                    // Quiet hours: stay silent and leave the day unmarked, so a
                    // later trigger may still find a daytime window.
                    val nowMinute = LocalTime.now().let { it.hour * 60 + it.minute }
                    if (SettingsRepository.quietHoursBlocking(context).covers(nowMinute)) return@runBlocking

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
                    context.getSystemService(NotificationManager::class.java)
                        .notify(BreathPrayerScheduler.NOTIFICATION_ID, notification)
                    SettingsRepository.setBreathLastFiredDay(context, today)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun ensureChannel(context: Context, name: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(BreathPrayerScheduler.CHANNEL_ID) == null) {
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
