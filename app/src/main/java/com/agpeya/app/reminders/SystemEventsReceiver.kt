package com.agpeya.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.agpeya.app.data.HoursRepository
import com.agpeya.app.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Rebuilds the alarm schedule after events that invalidate it:
 * device reboot, app update, and clock/timezone changes (PLAN.md §8.4).
 */
class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> {
                val pending = goAsync()
                Thread {
                    try {
                        runBlocking {
                            // getOrNull, not getOrDefault(emptyMap()): an
                            // empty map means every hour is hidden and nothing
                            // should ring, and rescheduleAll obeys it — it
                            // cancels the lot and records that nothing is
                            // armed. A failed read is not that. It used to
                            // arrive as the same empty map and take the whole
                            // schedule down with it, on the one event whose
                            // job is to put the schedule back.
                            val names = runCatching {
                                HoursRepository.visibleHours(context).associate { it.id to it.name }
                            }.getOrNull()
                            if (names != null) {
                                runCatching { ReminderScheduler.rescheduleAll(context, names) }
                            }
                            // Re-arm every independent reminder chain. Completion
                            // records are deliberately not consulted here.
                            runCatching {
                                StreakReminderScheduler.sync(
                                    context,
                                    SettingsRepository.streakReminder(context).first(),
                                )
                            }
                            runCatching {
                                GitsaweReminderScheduler.sync(
                                    context,
                                    SettingsRepository.gitsaweReminder(context).first(),
                                )
                            }
                            runCatching {
                                ReadingReminderScheduler.sync(
                                    context,
                                    SettingsRepository.readingReminder(context).first(),
                                )
                            }
                            runCatching { SpecialHabitReminderScheduler.syncAll(context) }
                            runCatching {
                                BreathPrayerScheduler.sync(
                                    context,
                                    SettingsRepository.breathReminder(context).first(),
                                )
                            }
                        }
                    } finally {
                        pending.finish()
                    }
                }.start()
            }
        }
    }
}
