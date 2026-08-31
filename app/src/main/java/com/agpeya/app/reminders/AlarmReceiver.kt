package com.agpeya.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.agpeya.app.data.ModesRepository
import kotlinx.coroutines.runBlocking

/**
 * Fires when a reminder alarm goes off: rings via [AlarmRinger] (an insistent
 * alarm-channel notification — no foreground service), then schedules the
 * entry's next occurrence (chain pattern). Skips silently if the entry was
 * meanwhile disabled, deleted, or its mode deactivated.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val entryId = intent.getStringExtra(ReminderScheduler.EXTRA_ENTRY_ID) ?: return
        val hourId = intent.getStringExtra(ReminderScheduler.EXTRA_HOUR_ID) ?: return
        val hourName = intent.getStringExtra(ReminderScheduler.EXTRA_HOUR_NAME) ?: hourId
        val isSnooze = intent.getBooleanExtra(ReminderScheduler.EXTRA_SNOOZE, false)

        // DataStore reads off the main thread; goAsync keeps the receiver alive for them.
        val pending = goAsync()
        Thread {
            try {
                // A snoozed alarm fires once, unconditionally — no active-entry
                // check, no chaining.
                if (isSnooze) {
                    AlarmRinger.ring(context, hourId, hourName)
                    return@Thread
                }
                val stillActive = runBlocking {
                    val entry = ModesRepository.current(context).activeMode
                        ?.entries?.find { it.id == entryId && it.enabled }
                    if (entry != null) {
                        // Always re-arm, even when silenced: the chain must keep
                        // running or tomorrow's alarm is lost too.
                        ReminderScheduler.scheduleNext(context, entry, hourName)
                        !com.agpeya.app.data.SettingsRepository.inQuietHoursNow(context)
                    } else {
                        ModesRepository.setScheduledIds(
                            context,
                            ModesRepository.scheduledIds(context) - entryId,
                        )
                        false
                    }
                }
                if (stillActive) {
                    AlarmRinger.ring(context, hourId, hourName)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}
