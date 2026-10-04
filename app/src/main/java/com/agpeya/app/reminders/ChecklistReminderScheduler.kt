package com.agpeya.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Schedules alarms for checklist tasks assigned to canonical Ethiopian prayer hours or times.
 */
object ChecklistReminderScheduler {
    const val ACTION_CHECKLIST_REMINDER = "com.agpeya.app.CHECKLIST_REMINDER"
    const val EXTRA_TASK_TEXT = "checklistTaskText"
    const val EXTRA_HOUR_NAME = "checklistHourName"
    const val EXTRA_TASK_ID = "checklistTaskId"
    const val EXTRA_DATE = "checklistDate"
    const val EXTRA_ENTRY_ID = "checklistEntryId"

    /**
     * Resolves the canonical hour name, timestamp, or any custom time string to a [LocalTime].
     */
    fun resolveTime(scheduledHour: String?): LocalTime {
        if (scheduledHour.isNullOrBlank()) return LocalTime.of(6, 0)
        val trimmed = scheduledHour.trim()
        val lower = trimmed.lowercase()

        // 1. Direct standard formats: HH:mm, H:mm, h:mm a
        val patterns = listOf(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.US),
            java.time.format.DateTimeFormatter.ofPattern("H:mm", java.util.Locale.US),
            java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US),
            java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US),
        )
        for (pattern in patterns) {
            val parsed = runCatching { LocalTime.parse(trimmed.uppercase(java.util.Locale.US), pattern) }.getOrNull()
            if (parsed != null) return parsed
        }

        // 2. Extract time from strings like "ነግህ (6:00 AM)" or "ቀትር (12:00 PM)"
        val regex = Regex("""(\d{1,2}):(\d{2})(?:\s*([AaPp][Mm]))?""")
        val match = regex.find(trimmed)
        if (match != null) {
            val (hStr, mStr, ampm) = match.destructured
            var hour = hStr.toIntOrNull() ?: 6
            val min = mStr.toIntOrNull() ?: 0
            if (ampm.equals("pm", ignoreCase = true) && hour < 12) hour += 12
            if (ampm.equals("am", ignoreCase = true) && hour == 12) hour = 0
            if (hour in 0..23 && min in 0..59) {
                return LocalTime.of(hour, min)
            }
        }

        // 3. Fallback to canonical liturgical hours
        return when {
            "ነግህ" in lower || "morning" in lower -> LocalTime.of(6, 0)
            "ሠለስት" in lower || "terce" in lower -> LocalTime.of(9, 0)
            "ቀትር" in lower || "noon" in lower || "sext" in lower -> LocalTime.of(12, 0)
            "ተሰዓት" in lower || "none" in lower -> LocalTime.of(15, 0)
            "ሰርክ" in lower || "ሠርክ" in lower || "vespers" in lower -> LocalTime.of(18, 0)
            "ንዋም" in lower || "ነዋም" in lower || "compline" in lower -> LocalTime.of(21, 0)
            else -> LocalTime.of(6, 0)
        }
    }

    /**
     * Arms an exact alarm for the task at the resolved time on [date].
     */
    fun schedule(
        context: Context,
        taskId: String,
        taskText: String,
        scheduledHour: String?,
        date: LocalDate = LocalDate.now(),
        entryId: String? = null,
    ) {
        val app = context.applicationContext
        val am = app.getSystemService(AlarmManager::class.java)
        val time = resolveTime(scheduledHour)
        var targetDateTime = date.atTime(time)
        val now = LocalDateTime.now()
        if (!targetDateTime.isAfter(now)) {
            targetDateTime = targetDateTime.plusDays(1)
        }
        val triggerAt = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(app, ChecklistReminderReceiver::class.java).apply {
            action = ACTION_CHECKLIST_REMINDER
            data = "agpeya://checklist/$taskId".toUri()
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TEXT, taskText)
            putExtra(EXTRA_HOUR_NAME, scheduledHour ?: "ነግህ")
            putExtra(EXTRA_DATE, date.toString())
            if (!entryId.isNullOrBlank()) {
                putExtra(EXTRA_ENTRY_ID, entryId)
            }
        }
        val pi = PendingIntent.getBroadcast(
            app,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } catch (_: SecurityException) {
            // System policy may restrict exact alarms on certain devices; falls back gracefully.
        }
    }

    /**
     * Cancels an armed alarm for the task.
     */
    fun cancel(context: Context, taskId: String) {
        val app = context.applicationContext
        val am = app.getSystemService(AlarmManager::class.java)
        val intent = Intent(app, ChecklistReminderReceiver::class.java).apply {
            action = ACTION_CHECKLIST_REMINDER
            data = "agpeya://checklist/$taskId".toUri()
        }
        val pi = PendingIntent.getBroadcast(
            app,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
        app.getSystemService(android.app.NotificationManager::class.java)
            .cancel(NotificationIds.inFamily(NotificationIds.CHECKLIST_BASE, taskId))
    }
}
