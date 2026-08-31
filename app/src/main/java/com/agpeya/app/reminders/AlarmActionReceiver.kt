package com.agpeya.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles the ringing notification's controls. Dismiss just takes the
 * notification down; the removal itself ([AlarmRinger.ACTION_REMOVED], the
 * delete intent — fired equally by Dismiss, a swipe, tapping Open, or the
 * timeout) posts the "done?" follow-up. Snooze re-arms the alarm and marks the
 * removal it causes as not-an-answer so no follow-up appears.
 */
class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val hourId = intent.getStringExtra(ReminderScheduler.EXTRA_HOUR_ID) ?: return
        when (intent.action) {
            AlarmRinger.ACTION_SNOOZE -> {
                val hourName = intent.getStringExtra(ReminderScheduler.EXTRA_HOUR_NAME) ?: hourId
                AlarmRinger.suppressFollowUp = true
                AlarmRinger.stop(context)
                ReminderScheduler.snooze(context, hourId, hourName)
            }
            AlarmRinger.ACTION_DISMISS -> AlarmRinger.stop(context)
            AlarmRinger.ACTION_REMOVED -> {
                if (AlarmRinger.suppressFollowUp) {
                    AlarmRinger.suppressFollowUp = false
                    return
                }
                // Language read is a DataStore hit — keep it off the main thread.
                val pending = goAsync()
                Thread {
                    try {
                        AlarmRinger.postDonePrompt(context, hourId)
                    } finally {
                        pending.finish()
                    }
                }.start()
            }
        }
    }
}
