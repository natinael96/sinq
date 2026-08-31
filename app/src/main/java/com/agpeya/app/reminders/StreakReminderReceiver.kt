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
import com.agpeya.app.data.FastingCalendar
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.stringsFor
import com.agpeya.app.ui.strings.Strings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * The nightly nudge fired. Re-arms tomorrow, then (unless turned off) posts an
 * invitation to close the day with prayer; its tap opens the Journey screen —
 * the same shape as the morning ግጻዌ nudge.
 *
 * The wording never depends on history — no count carried, nothing "at stake",
 * nothing to lose — so the notification is equally true on a first night, a
 * hundredth night, or the night someone comes back after a month away. It does
 * know the Church's day: a feast is named, a fast colours the invitation.
 *
 * It fires whether or not something was already logged today. It used to skip
 * itself on any logged day, which read as the reminder being broken: the nights
 * it stayed silent were exactly the nights the user was paying attention.
 */
class StreakReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                runBlocking {
                    if (!SettingsRepository.streakReminder(context).first()) return@runBlocking
                    // Chain: always re-arm for tomorrow while enabled — even
                    // when tonight's nudge is about to be silenced.
                    StreakReminderScheduler.schedule(context)
                    if (SettingsRepository.inQuietHoursNow(context)) return@runBlocking

                    val s = stringsFor(SettingsRepository.language(context).first())
                    // Completion never suppresses this notification. Instead,
                    // it adds the core checklist items that still need a mark.
                    val body = reminderBody(context, s)
                    ensureChannel(context, s.nightReminderChannel)
                    // Request code must differ from ReminderScheduler's alarm-clock
                    // show intent (code 0): extras don't distinguish PendingIntents,
                    // so sharing the code lets each overwrite the other's extras.
                    val tap = PendingIntent.getActivity(
                        context,
                        2,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(StreakReminderScheduler.EXTRA_OPEN_STREAK, true)
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(s.nightReminderTitle)
                        .setContentText(body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(tap)
                        .build()
                    context.getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, notification)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    /**
     * The invitation, coloured by the Church's day where the calendar knows it:
     * a feast is named first, then a running fast (or the ረቡዕ/ዓርብ rule), then
     * the plain evening wording. Each lookup fails soft to the next.
     */
    private suspend fun liturgicalBody(context: Context, s: Strings): String {
        val today = LocalDate.now()
        runCatching { GitsaweRepository.readingsFor(context, today).feasts.firstOrNull()?.amharicName }
            .getOrNull()?.let { return s.nightReminderFeastBody(it) }
        val fasting = runCatching {
            FastingCalendar.fastOn(today) != null || FastingCalendar.isWeeklyFastDay(today)
        }.getOrDefault(false)
        return if (fasting) s.nightReminderFastBody else s.nightReminderBody
    }

    private suspend fun reminderBody(context: Context, s: Strings): String {
        val base = liturgicalBody(context, s)
        val done = runCatching {
            HabitsRepository.current(context).records[LocalDate.now().toString()].orEmpty()
        }.getOrDefault(emptySet())
        val pending = pendingNightlyHabitIds(done).map { id ->
            when (id) {
                "sinksar" -> s.habitSynaxarium
                "church" -> s.habitChurch
                "prostrate" -> s.habitProstrate
                else -> id
            }
        }
        return if (pending.isEmpty()) base else "$base\n${s.nightReminderPending(pending.joinToString(" · "))}"
    }

    private fun ensureChannel(context: Context, name: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "streak_reminders"
        private const val NOTIFICATION_ID = 7200
    }
}

/** These remain worth reviewing even after one or every prayer hour is marked. */
internal fun pendingNightlyHabitIds(done: Set<String>): List<String> =
    listOf("sinksar", "church", "prostrate").filterNot(done::contains)
