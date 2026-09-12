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

/**
 * The reading plan's daily nudge.
 *
 * Four rules, in order, and each is a reason *not* to send:
 *
 *  1. **No plan, no nudge.** A reminder for something never started is an
 *     advertisement. The setting is on by default and stays silent until a plan
 *     is begun, so starting one is still the reader's own act.
 *  2. **Read already, nothing to say.** Unlike the nightly ጉዞ nudge — which
 *     fires whether or not the day is logged, on purpose — this one has a
 *     single errand, and it is done.
 *  3. **Quiet hours are quiet.**
 *  4. **Stop when ignored.** After a week of nudges that changed nothing, the
 *     app says so once and turns itself off. Duolingo's most-quoted
 *     notification is the one admitting the reminders are not working; it
 *     exists because a notification nobody acts on stops being read at all.
 *
 * What it does *not* do is carry a streak, a count, or anything at stake. The
 * nightly reminder settled that question for this app already: the wording has
 * to be equally true on a first morning, a hundredth, and the morning someone
 * comes back after a month away. So the body names the passage — which is
 * useful — and never what standing it is in.
 */
/** What this morning's nudge should do. */
internal enum class ReadingNudge { SEND, SILENT, STOP }

/**
 * The whole rule, with no Android in it so it can be pinned by a test.
 *
 * Order matters. Quiet hours are checked before the back-off so that a night
 * spent asleep is not counted as a nudge the reader ignored — otherwise a
 * reader whose quiet hours cover the reminder time would be "stopped for not
 * responding" to notifications that were never sent.
 */
internal fun decideReadingNudge(
    keeping: Boolean,
    lastReadOn: String,
    today: String,
    inQuietHours: Boolean,
    unanswered: Int,
    backoff: Int,
): ReadingNudge = when {
    // Nothing started, nothing to be reminded of. The setting is on by
    // default, so this is what keeps it from advertising itself.
    !keeping -> ReadingNudge.SILENT
    // The errand is done. Unlike the nightly ጉዞ nudge, which fires either way
    // on purpose, this one has a single thing to ask about.
    lastReadOn == today -> ReadingNudge.SILENT
    inQuietHours -> ReadingNudge.SILENT
    unanswered >= backoff -> ReadingNudge.STOP
    else -> ReadingNudge.SEND
}

class ReadingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                runBlocking {
                    if (!SettingsRepository.readingReminder(context).first()) return@runBlocking
                    // Chain: re-arm for tomorrow first, even when today's nudge
                    // is about to be silenced for one of the reasons below.
                    ReadingReminderScheduler.schedule(context)

                    val today = LocalDate.now()
                    val state = runCatching { ReadingPlanRepository.current(context) }.getOrNull()
                        ?: return@runBlocking
                    val decision = decideReadingNudge(
                        keeping = state.plansKept.isNotEmpty(),
                        lastReadOn = state.lastReadOn,
                        today = today.toString(),
                        inQuietHours = SettingsRepository.inQuietHoursNow(context),
                        unanswered = SettingsRepository.readingReminderUnanswered(context).first(),
                        backoff = SettingsRepository.READING_REMINDER_BACKOFF,
                    )
                    if (decision == ReadingNudge.SILENT) return@runBlocking

                    val s = stringsFor(SettingsRepository.language(context).first())
                    if (decision == ReadingNudge.STOP) {
                        // Said once, then the reminder turns itself off. The
                        // setting is left where the reader can switch it back on.
                        notify(context, s.readingReminderStoppedTitle, s.readingReminderStoppedBody, s)
                        SettingsRepository.setReadingReminder(context, false)
                        ReadingReminderScheduler.cancel(context)
                        return@runBlocking
                    }

                    val body = body(context, state, s) ?: return@runBlocking
                    notify(context, s.readingReminderTitle, body, s)
                    SettingsRepository.noteReadingReminderSent(context)
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
    private suspend fun body(
        context: Context,
        state: com.agpeya.app.model.ReadingPlanState,
        s: Strings,
    ): String? {
        val content = runCatching { ReadingPlanRepository.content(context) }.getOrNull() ?: return null
        // Every plan being kept, in the order they are kept: a reader keeping
        // the year and the Psalter is owed both names, not whichever came first.
        val names = runCatching { ScriptureRepository.bookNames(context) }.getOrDefault(emptyMap())
        var firstDay = 0
        val parts = state.plansKept.mapNotNull { kept ->
            val plan = content.plans.firstOrNull { it.id == kept.planId } ?: return@mapNotNull null
            val days = ReadingPlanRepository.effectiveDays(plan, state)
            val n = ReadingPlanRepository.dayOn(kept.startedOn, LocalDate.now(), plan.days)
            val day = days.firstOrNull { it.d == n } ?: return@mapNotNull null
            if (ReadingPlanRepository.isRead(state, plan.id, day)) return@mapNotNull null
            if (firstDay == 0) firstDay = n
            describe(day, names).takeIf { it.isNotBlank() }
        }
        if (firstDay == 0) return null
        val passages = parts.joinToString("  ·  ")
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
            .notify(NOTIFICATION_ID, notification)
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
        private const val NOTIFICATION_ID = 7300
    }
}
