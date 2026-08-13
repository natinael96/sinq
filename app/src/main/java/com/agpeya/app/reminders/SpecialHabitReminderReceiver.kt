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

/**
 * A special reminder (ምጽዋት or ንስሐ) fired on its due day. Re-arms for the
 * NEXT due day, then (unless turned off meanwhile) posts a notification whose
 * tap simply opens the app.
 *
 * Deliberately no "Done" action and no record: these are intentions, not
 * habits. The app's part ends at the reminder — whether alms were given or
 * repentance made is not the app's to know.
 */
class SpecialHabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habit = SpecialHabit.entries.firstOrNull { it.action == intent.action } ?: return
        val pending = goAsync()
        Thread {
            try {
                runBlocking {
                    val enabled = when (habit) {
                        SpecialHabit.ALMS -> SettingsRepository.almsReminder(context).first()
                        SpecialHabit.REPENTANCE -> SettingsRepository.repentanceReminder(context).first()
                    }
                    if (!enabled) return@runBlocking
                    // Chain: always re-arm for the next due day while enabled.
                    SpecialHabitReminderScheduler.schedule(context, habit)

                    val s = stringsFor(SettingsRepository.language(context).first())
                    val (title, body, channelName) = when (habit) {
                        SpecialHabit.ALMS ->
                            Triple(s.almsReminderTitle, s.almsReminderBody, s.almsChannelName)
                        SpecialHabit.REPENTANCE ->
                            Triple(s.repentReminderTitle, s.repentReminderBody, s.repentChannelName)
                    }
                    ensureChannel(context, habit, channelName)
                    val tap = PendingIntent.getActivity(
                        context,
                        habit.tapRequestCode,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                    val notification = NotificationCompat.Builder(context, habit.channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(tap)
                        .build()
                    context.getSystemService(NotificationManager::class.java)
                        .notify(habit.notificationId, notification)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun ensureChannel(context: Context, habit: SpecialHabit, name: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(habit.channelId) == null) {
            nm.createNotificationChannel(
                NotificationChannel(habit.channelId, name, NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }
}
