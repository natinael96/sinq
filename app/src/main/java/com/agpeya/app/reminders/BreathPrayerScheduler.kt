package com.agpeya.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.agpeya.app.data.ModesRepository
import com.agpeya.app.data.SettingsRepository
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

/**
 * የመሃል ጸሎት — the in-between prayer. Once a day, at a moment nobody planned,
 * a one-line nudge to pray one of the short prayers the tradition keeps for
 * exactly these moments. Praying, not reading: the notification carries the
 * whole prayer; there is nothing to open.
 *
 * The moment is drawn at random from the day's in-between window:
 *   - no earlier than [MIN_AFTER_PRAYER_MIN] minutes after the last recorded
 *     prayer hour (and never bang on "now" — [MIN_FROM_NOW_MIN] gives air),
 *   - no later than [BEFORE_NEXT_PRAYER_MIN] minute before the next scheduled
 *     prayer reminder, and never into the night ([LATEST_HOUR]).
 * If the prayers sit too close together right now, nothing is armed — the next
 * recorded prayer reopens the window with a fresh roll.
 *
 * Re-armed from every place the window can change: a prayer being recorded
 * (JourneyScreen, the Done notification), app launch, boot/time changes, the
 * Settings toggle, and the receiver itself after firing. The receiver marks
 * the day fired, so a day never gets two nudges; scheduling then chains into a
 * fresh window tomorrow even if the app is not opened again.
 */
object BreathPrayerScheduler {

    const val ACTION = "com.agpeya.app.BREATH_PRAYER"
    const val CHANNEL_ID = "breath_reminders"
    const val NOTIFICATION_ID = 7600

    /** Tap code 7 — 0–3 are the alarm/ግጻዌ/streak/widget intents, 5/6 ምጽዋት/ንስሐ. */
    const val TAP_REQUEST_CODE = 7
    private const val REQUEST_CODE = 9500

    /**
     * The one-breath prayers, drawn at random at fire time. Content, not
     * chrome: like every prayer text in the app they stay untranslated.
     * እግዚኦ and Kyrie as the liturgy repeats them, the Jesus Prayer, and the
     * ሰላም ለኪ greeting of the Theotokos.
     */
    val PRAYERS = listOf(
        "እግዚኦ መሐረነ ክርስቶስ።",
        "ኪርያላይሶን፥ ኪርያላይሶን፥ ኪርያላይሶን።",
        "ጌታ ኢየሱስ ክርስቶስ የእግዚአብሔር ልጅ ሆይ፥ እኔን ኃጢአተኛውን ማረኝ።",
        "ሰላም ለኪ ማርያም ምልዕተ ጸጋ፥ እግዚአብሔር ምስሌኪ።",
    )

    private const val MIN_AFTER_PRAYER_MIN = 10L
    private const val MIN_FROM_NOW_MIN = 15L
    private const val BEFORE_NEXT_PRAYER_MIN = 1L
    private const val NEXT_DAY_EARLIEST_HOUR = 8

    /** No nudges after 21:00 — the evening belongs to the nightly reminder. */
    private const val LATEST_HOUR = 21

    /** Re-arm or cancel to match the current setting; call after any toggle. */
    fun sync(context: Context, enabled: Boolean) {
        if (enabled) schedule(context) else cancel(context)
    }

    /** A prayer hour was just recorded: move the anchor, re-roll the moment. */
    suspend fun onPrayerRecorded(context: Context) {
        SettingsRepository.setLastPrayerRecordedAt(context, System.currentTimeMillis())
        schedule(context)
    }

    fun schedule(context: Context) {
        if (!SettingsRepository.breathReminderBlocking(context)) return cancel(context)
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        // Once today's nudge has fired, keep the chain alive by arming a fresh
        // daytime window tomorrow. Prayer completion must never silence it.
        if (SettingsRepository.breathLastFiredDayBlocking(context) == today.toString()) {
            val lower = today.plusDays(1).atTime(NEXT_DAY_EARLIEST_HOUR, 0)
            var upper = today.plusDays(1).atTime(LATEST_HOUR, 0)
            nextPrayerTime(context, lower)?.let { next ->
                if (next.toLocalDate() == lower.toLocalDate()) {
                    val cap = next.minusMinutes(BEFORE_NEXT_PRAYER_MIN)
                    if (cap.isAfter(lower) && cap.isBefore(upper)) upper = cap
                }
            }
            return armRandom(context, lower, upper)
        }

        // Lower bound: a while from now, and clear of the last recorded prayer.
        var lower = now.plusMinutes(MIN_FROM_NOW_MIN)
        val lastPrayed = SettingsRepository.lastPrayerRecordedAtBlocking(context)
            .takeIf { it > 0 }
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
        if (lastPrayed != null && lastPrayed.toLocalDate() == today) {
            val clear = lastPrayed.plusMinutes(MIN_AFTER_PRAYER_MIN)
            if (clear.isAfter(lower)) lower = clear
        }

        // Upper bound: short of the next scheduled prayer, and of the night.
        var upper = today.atTime(LATEST_HOUR, 0)
        nextPrayerTime(context, now)?.let { next ->
            if (next.toLocalDate() == today) {
                val cap = next.minusMinutes(BEFORE_NEXT_PRAYER_MIN)
                if (cap.isBefore(upper)) upper = cap
            }
        }

        // No room between the prayers right now — recording the next prayer
        // (or tomorrow's launch) rolls again.
        if (!upper.isAfter(lower)) return cancel(context)

        armRandom(context, lower, upper)
    }

    private fun armRandom(context: Context, lower: LocalDateTime, upper: LocalDateTime) {
        if (!upper.isAfter(lower)) return cancel(context)
        val span = java.time.Duration.between(lower, upper).seconds
        val at = lower.plusSeconds(Random.nextLong(span + 1))
        val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService(AlarmManager::class.java)
        // Exact + doze-exempt like the other nudges: an inexact alarm in the
        // App-Standby "rare" bucket drifts hours — which here means into the
        // very prayer this nudge is supposed to stay clear of.
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
        }
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    /** The next enabled reminder of the active mode, strictly after [now]. */
    private fun nextPrayerTime(context: Context, now: LocalDateTime): LocalDateTime? =
        runCatching {
            runBlocking {
                ModesRepository.current(context).activeMode?.entries.orEmpty()
                    .filter { it.enabled }
                    .mapNotNull { ReminderScheduler.nextOccurrence(it, now) }
                    .minOrNull()
            }
        }.getOrNull()

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, BreathPrayerReceiver::class.java).setAction(ACTION)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
