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
                    runBlocking {
                        val names = runCatching {
                            HoursRepository.visibleHours(context).associate { it.id to it.name }
                        }.getOrDefault(emptyMap())
                        ReminderScheduler.rescheduleAll(context, names)
                        // Re-arm the nightly nudge too, if it's enabled.
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
                            SpecialHabitReminderScheduler.sync(
                                context,
                                SpecialHabit.ALMS,
                                SettingsRepository.almsReminder(context).first(),
                            )
                        }
                        runCatching {
                            SpecialHabitReminderScheduler.sync(
                                context,
                                SpecialHabit.REPENTANCE,
                                SettingsRepository.repentanceReminder(context).first(),
                            )
                        }
                        runCatching {
                            BreathPrayerScheduler.sync(
                                context,
                                SettingsRepository.breathReminder(context).first(),
                            )
                        }
                    }
                    pending.finish()
                }.start()
            }
        }
    }
}
