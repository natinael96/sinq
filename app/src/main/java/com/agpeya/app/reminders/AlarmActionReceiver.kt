package com.agpeya.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles every way a ringing alarm can end.
 *
 * The "done?" follow-up is posted explicitly on each path rather than left to
 * the notification's delete intent: that intent only fires when the *user*
 * clears a notification, so an app-side cancel() — which is what Dismiss,
 * tapping Open, and the timeout all do — would silently skip it.
 *
 * Snooze is the one ending that asks nothing: the prayer was postponed, not
 * answered.
 */
class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val hourId = intent.getStringExtra(ReminderScheduler.EXTRA_HOUR_ID)
            ?.takeIf { it.isNotBlank() } ?: return
        when (intent.action) {
            AlarmRinger.ACTION_SNOOZE -> {
                val hourName = intent.getStringExtra(ReminderScheduler.EXTRA_HOUR_NAME) ?: hourId
                val count = intent.getIntExtra(ReminderScheduler.EXTRA_SNOOZE_COUNT, 1)
                AlarmRinger.cancelTimeout(context)
                AlarmRinger.stop(context)
                // Reads the snooze length from DataStore, so off the main thread.
                val pending = goAsync()
                Thread {
                    try {
                        val backAt = ReminderScheduler.snooze(context, hourId, hourName, count)
                        // Snoozing used to be silent, which left no way to tell a
                        // postponed hour from one that never rang.
                        AlarmRinger.postSnoozeNotice(context, hourId, hourName, backAt, count)
                    } finally {
                        pending.finish()
                    }
                }.start()
            }
            // Dismissed, opened, rung out, or swiped away: all of them are the
            // alarm ending, and all of them are worth asking about.
            AlarmRinger.ACTION_DISMISS,
            AlarmRinger.ACTION_TIMEOUT,
            AlarmRinger.ACTION_REMOVED,
            -> {
                AlarmRinger.cancelTimeout(context)
                AlarmRinger.stop(context)
                // The prompt reads the language from DataStore — off the main
                // thread, with goAsync holding the receiver open for it.
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
