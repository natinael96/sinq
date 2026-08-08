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
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.stringsFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * The nightly streak nudge fired. Re-arms tomorrow, then (unless the user has
 * already logged something today, or turned the reminder off) posts a gentle
 * notification whose tap opens the Streak screen.
 */
class StreakReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                runBlocking {
                    if (!SettingsRepository.streakReminder(context).first()) return@runBlocking
                    // Chain: always re-arm for tomorrow while enabled.
                    StreakReminderScheduler.schedule(context)

                    // Already engaged today? Skip the nudge — they don't need it.
                    val today = LocalDate.now().toString()
                    val doneToday = HabitsRepository.current(context).records[today] ?: emptySet()
                    if (doneToday.isNotEmpty()) return@runBlocking

                    val s = stringsFor(SettingsRepository.language(context).first())
                    ensureChannel(context, s.streakChannelName)
                    // Request code must differ from ReminderScheduler's alarm-clock
                    // show intent (code 0): extras don't distinguish PendingIntents,
                    // so sharing the code lets each overwrite the other's extras.
                    val tap = PendingIntent.getActivity(
                        context,
                        2,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(StreakReminderScheduler.EXTRA_OPEN_STREAK, true)
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(s.streakReminderTitle)
                        .setContentText(s.streakReminderBody)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(tap)
                        .build()
                    context.getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, notification)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun ensureChannel(context: Context, name: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "streak_reminders"
        private const val NOTIFICATION_ID = 7200
    }
}
