package com.agpeya.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.HabitSchedule
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The two scheduled intentions — ምጽዋት and ንስሐ. Deliberately NOT habits: the
 * app reminds and then looks away. Nothing is recorded, streaked, or shown as
 * done — alms and repentance are between the person and God, not the app
 * ("let not your left hand know what your right hand is doing").
 *
 * Each carries its own ids so the pair can share one scheduler and one
 * receiver without colliding: request codes, notification ids and channels
 * must all be unique app-wide (see the comment in StreakReminderReceiver —
 * extras don't distinguish PendingIntents, only request codes do). Tap codes
 * 5/6 because 0–3 are taken by the alarm, ግጻዌ, streak and widget intents.
 */
enum class SpecialHabit(
    val action: String,
    val requestCode: Int,
    val tapRequestCode: Int,
    val notificationId: Int,
    val channelId: String,
) {
    ALMS("com.agpeya.app.ALMS_REMINDER", 9300, 5, 7400, "alms_reminders"),
    REPENTANCE("com.agpeya.app.REPENTANCE_REMINDER", 9400, 6, 7500, "repentance_reminders"),
}

/**
 * Schedules the ምጽዋት and ንስሐ nudges (gentle notifications, not ringing
 * alarms). Unlike the nightly streak nudge, these do not fire daily: the next
 * alarm is armed for the next DUE day of the habit's [HabitSchedule], at the
 * user's chosen time. The receiver re-arms on fire (chain pattern), and
 * boot/update/time changes plus app launch re-arm too.
 */
object SpecialHabitReminderScheduler {

    /** Re-arm or cancel to match the current setting; call after any toggle. */
    fun sync(context: Context, habit: SpecialHabit, enabled: Boolean) {
        if (enabled) schedule(context, habit) else cancel(context, habit)
    }

    fun schedule(context: Context, habit: SpecialHabit) {
        val minute = when (habit) {
            SpecialHabit.ALMS -> SettingsRepository.almsReminderTimeBlocking(context)
            SpecialHabit.REPENTANCE -> SettingsRepository.repentanceReminderTimeBlocking(context)
        }
        val sched = scheduleOf(context, habit)
        val now = LocalDateTime.now()
        val time = LocalTime.of(minute / 60, minute % 60)
        // Next due day whose firing time hasn't passed yet.
        var dueDate = sched.nextDueOnOrAfter(now.toLocalDate()) ?: return
        if (!dueDate.atTime(time).isAfter(now)) {
            dueDate = sched.nextDueOnOrAfter(dueDate.plusDays(1)) ?: return
        }
        val triggerAt = dueDate.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context, habit)
        // Exact + doze-exempt for the same reason as the streak nudge: inexact
        // alarms get throttled once the app goes App-Standby "rare", and a
        // weekly reminder that lands a day late is worse than none.
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context, habit: SpecialHabit) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, habit))
    }

    fun scheduleOf(context: Context, habit: SpecialHabit): HabitSchedule = when (habit) {
        SpecialHabit.ALMS -> SettingsRepository.almsScheduleBlocking(context)
        SpecialHabit.REPENTANCE -> SettingsRepository.repentanceScheduleBlocking(context)
    }

    private fun pendingIntent(context: Context, habit: SpecialHabit): PendingIntent {
        val intent = Intent(context, SpecialHabitReminderReceiver::class.java)
            .setAction(habit.action)
        return PendingIntent.getBroadcast(
            context,
            habit.requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
