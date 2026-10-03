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

/**
 * Fires when a checklist task reminder alarm triggers.
 * Delivers a high-priority notification respecting user quiet hours.
 */
class ChecklistReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(ChecklistReminderScheduler.EXTRA_TASK_ID) ?: return
        val taskText = intent.getStringExtra(ChecklistReminderScheduler.EXTRA_TASK_TEXT) ?: return
        val hourName = intent.getStringExtra(ChecklistReminderScheduler.EXTRA_HOUR_NAME) ?: "ነግህ"

        val pending = goAsync()
        Thread {
            try {
                val silenced = runBlocking { SettingsRepository.inQuietHoursNow(context) }
                if (silenced) return@Thread

                val lang = runBlocking { SettingsRepository.language(context).first() }
                val s = stringsFor(lang)

                ensureChannel(context, s.checklistChannelName)

                val tap = PendingIntent.getActivity(
                    context,
                    taskId.hashCode(),
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )

                val title = "${s.checklistReminderTitle} · $hourName"
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(taskText)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(tap)
                    .setAutoCancel(true)
                    .build()

                val notifId = NotificationIds.inFamily(NotificationIds.CHECKLIST_BASE, taskId)
                context.getSystemService(NotificationManager::class.java).notify(notifId, notification)
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun ensureChannel(context: Context, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Daily spiritual checklist task reminders"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "checklist_reminders"
    }
}
