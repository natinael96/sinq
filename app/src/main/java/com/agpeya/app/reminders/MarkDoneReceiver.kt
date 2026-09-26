package com.agpeya.app.reminders

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.agpeya.app.data.HabitsRepository
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * Handles the "Done?" follow-up notification's Yes action: marks the prayed
 * hour on today's record and dismisses the notification.
 */
class MarkDoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val hourId = intent.getStringExtra(ReminderScheduler.EXTRA_HOUR_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            context.getSystemService(NotificationManager::class.java).cancel(notificationId)
        }
        // markDone writes DataStore — do it off the main thread.
        val pending = goAsync()
        Thread {
            try {
                runBlocking {
                    HabitsRepository.markDone(
                        context,
                        LocalDate.now().toString(),
                        HabitsRepository.hourHabitId(hourId),
                    )
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notificationId"
    }
}
