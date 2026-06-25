package com.agpeya.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.agpeya.app.data.ModesRepository
import kotlinx.coroutines.runBlocking

/**
 * Fires when a reminder alarm goes off: starts the ringing [AlarmService], then
 * schedules the entry's next occurrence (chain pattern). Skips silently if the
 * entry was meanwhile disabled, deleted, or its mode deactivated.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val entryId = intent.getStringExtra(ReminderScheduler.EXTRA_ENTRY_ID) ?: return
        val hourId = intent.getStringExtra(ReminderScheduler.EXTRA_HOUR_ID) ?: return
        val hourName = intent.getStringExtra(ReminderScheduler.EXTRA_HOUR_NAME) ?: hourId

        // A snoozed alarm fires once, unconditionally — no active-entry check, no chaining.
        if (intent.getBooleanExtra(ReminderScheduler.EXTRA_SNOOZE, false)) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmService::class.java).apply {
                    putExtra(ReminderScheduler.EXTRA_HOUR_ID, hourId)
                    putExtra(ReminderScheduler.EXTRA_HOUR_NAME, hourName)
                },
            )
            return
        }

        val stillActive = runBlocking {
            val entry = ModesRepository.current(context).activeMode
                ?.entries?.find { it.id == entryId && it.enabled }
            if (entry != null) {
                ReminderScheduler.scheduleNext(context, entry, hourName)
                true
            } else {
                ModesRepository.setScheduledIds(context, ModesRepository.scheduledIds(context) - entryId)
                false
            }
        }
        if (!stillActive) return

        ContextCompat.startForegroundService(
            context,
            Intent(context, AlarmService::class.java).apply {
                putExtra(ReminderScheduler.EXTRA_HOUR_ID, hourId)
                putExtra(ReminderScheduler.EXTRA_HOUR_NAME, hourName)
            },
        )
    }
}
