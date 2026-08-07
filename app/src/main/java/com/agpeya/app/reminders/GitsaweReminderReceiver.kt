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
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.stringsFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * The morning ግጻዌ nudge fired. Re-arms tomorrow, then (unless turned off) posts
 * a notification whose body is today's ግጻዌ reading heading and whose tap opens
 * the ግጻዌ screen.
 */
class GitsaweReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                runBlocking {
                    if (!SettingsRepository.gitsaweReminder(context).first()) return@runBlocking
                    // Chain: always re-arm for tomorrow while enabled.
                    GitsaweReminderScheduler.schedule(context)

                    val s = stringsFor(SettingsRepository.language(context).first())
                    val heading = runCatching {
                        GitsaweRepository.dailyFor(context, LocalDate.now())?.title
                    }.getOrNull()?.takeIf { it.isNotBlank() } ?: s.gitsaweReminderBody

                    ensureChannel(context, s.gitsaweChannelName)
                    val tap = PendingIntent.getActivity(
                        context,
                        1,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(GitsaweReminderScheduler.EXTRA_OPEN_GITSAWE, true)
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(s.gitsaweReminderTitle)
                        .setContentText(heading)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(heading))
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
        const val CHANNEL_ID = "gitsawe_reminders"
        private const val NOTIFICATION_ID = 7300
    }
}
