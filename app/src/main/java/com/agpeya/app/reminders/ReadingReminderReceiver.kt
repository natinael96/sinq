package com.agpeya.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.agpeya.app.MainActivity
import com.agpeya.app.R
import com.agpeya.app.data.ReadingPlanRepository
import com.agpeya.app.data.ScriptureRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.PlanDay
import com.agpeya.app.stringsFor
import com.agpeya.app.ui.strings.Strings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/** Remind at each automatic slot while today's passages remain unfinished. */
internal fun shouldSendReadingReminder(hasUnreadReadings: Boolean, inQuietHours: Boolean): Boolean =
    hasUnreadReadings && !inQuietHours

class ReadingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                runBlocking {
                    if (!SettingsRepository.readingReminder(context).first()) return@runBlocking
                    // Chain: re-arm the next slot first, even when this nudge
                    // is about to be silenced for one of the reasons below.
                    ReadingReminderScheduler.schedule(context)

                    val today = LocalDate.now()
                    val state = runCatching { ReadingPlanRepository.current(context) }.getOrNull()
                        ?: return@runBlocking
                    val content = runCatching { ReadingPlanRepository.content(context) }.getOrNull()
                        ?: return@runBlocking
                    val unread = ReadingPlanRepository.unreadToday(content, state, today)
                    if (!shouldSendReadingReminder(unread.isNotEmpty(), SettingsRepository.inQuietHoursNow(context))) {
                        return@runBlocking
                    }
                    val s = stringsFor(SettingsRepository.language(context).first())
                    notify(context, s.readingReminderTitle, body(context, unread, s), s)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    /**
     * What today's plan actually asks for, named in the Church's own book
     * names — "ኦሪት ዘፍጥረት ፩–፫". A reminder that names the passage is worth
     * opening; one that says "time to read" is not.
     */
    private suspend fun body(context: Context, unread: List<PlanDay>, s: Strings): String {
        val names = runCatching { ScriptureRepository.bookNames(context) }.getOrDefault(emptyMap())
        val passages = unread.joinToString("  ·  ") { describe(it, names) }
        val firstDay = unread.first().d
        return if (passages.isBlank()) s.readingReminderPlain(firstDay)
        else s.readingReminderBody(firstDay, passages)
    }

    private fun describe(day: PlanDay, names: Map<String, String>): String =
        day.r.joinToString("  ·  ") { reading ->
            val book = names[reading.b] ?: reading.b
            val from = com.agpeya.app.ui.reading.geezNumeral(reading.c)
            if (reading.to > reading.c) {
                "$book $from–${com.agpeya.app.ui.reading.geezNumeral(reading.to)}"
            } else {
                "$book $from"
            }
        }

    private fun notify(context: Context, title: String, body: String, s: Strings) {
        ensureChannel(context, s.readingReminderChannel)
        // A request code of its own: extras do not distinguish PendingIntents,
        // so sharing one with another reminder lets each overwrite the other's.
        val tap = PendingIntent.getActivity(
            context,
            5,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ReadingReminderScheduler.EXTRA_OPEN_READING, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(NotificationIds.READING, notification)
    }

    private fun ensureChannel(context: Context, name: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            nm.getNotificationChannel(CHANNEL_ID) == null
        ) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "reading_reminders"
    }
}
