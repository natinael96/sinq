package com.agpeya.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.agpeya.app.data.ModesRepository
import com.agpeya.app.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

/**
 * የመሃል ጸሎት — the in-between prayer. Once a day, at a moment nobody planned,
 * a one-line nudge to pray one of the short prayers the tradition keeps for
 * exactly these moments. Praying, not reading: the notification carries the
 * whole prayer; there is nothing to open.
 *
 * The moment is drawn at random between the active mode's configured Morning
 * prayer time and [LATEST_HOUR]. Later prayer times and completion records do
 * not affect it. When the day's window has passed, tomorrow is armed instead.
 *
 * Re-armed from app launch, boot/time changes, Morning-time or active-mode
 * edits, the Settings toggle, and the receiver itself after firing. The
 * receiver marks the day fired, so a day never gets two nudges; scheduling then
 * chains into a fresh window tomorrow even if the app is not opened again.
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

    internal const val MIN_FROM_NOW_MIN = 15L
    private const val DEFAULT_MORNING_HOUR = 6

    /** No nudges after 21:00 — the evening belongs to the nightly reminder. */
    internal const val LATEST_HOUR = 21

    /** Re-arm or cancel to match the current setting; call after any toggle. */
    fun sync(context: Context, enabled: Boolean) {
        if (enabled) schedule(context) else cancel(context)
    }

    fun schedule(context: Context) {
        if (!SettingsRepository.breathReminderBlocking(context)) return cancel(context)
        val now = LocalDateTime.now()
        val firedToday = SettingsRepository.breathLastFiredDayBlocking(context) ==
            now.toLocalDate().toString()
        val window = breathPrayerWindow(now, morningMinute(context), firedToday)
        armRandom(context, window.lower, window.upper)
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
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    // Dispatchers.IO: schedule() is reached from the main thread (launch-time
    // self-heal, Settings toggles), and plain runBlocking would decode the
    // modes JSON on that thread. Same shape as the SettingsRepository helpers.
    private fun morningMinute(context: Context): Int =
        runCatching {
            runBlocking(Dispatchers.IO) {
                ModesRepository.current(context).activeMode?.entries
                    ?.firstOrNull { it.hourId == "morning" }
                    ?.let { it.hour * 60 + it.minute }
            }
        }.getOrNull() ?: DEFAULT_MORNING_HOUR * 60

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

internal data class BreathPrayerWindow(
    val lower: LocalDateTime,
    val upper: LocalDateTime,
)

/** Pure window calculation kept separate from Android scheduling for regression tests. */
internal fun breathPrayerWindow(
    now: LocalDateTime,
    configuredMorningMinute: Int,
    firedToday: Boolean,
): BreathPrayerWindow {
    val today = now.toLocalDate()
    val todayUpper = today.atTime(BreathPrayerScheduler.LATEST_HOUR, 0)
    val mustUseTomorrow = firedToday ||
        !todayUpper.isAfter(now.plusMinutes(BreathPrayerScheduler.MIN_FROM_NOW_MIN))
    val date = if (mustUseTomorrow) today.plusDays(1) else today

    val minute = configuredMorningMinute.coerceIn(0, 23 * 60 + 59)
    val configuredStart = date.atTime(LocalTime.of(minute / 60, minute % 60))
    val upper = date.atTime(BreathPrayerScheduler.LATEST_HOUR, 0)
    val morning = if (configuredStart.isBefore(upper)) {
        configuredStart
    } else {
        date.atTime(6, 0)
    }
    val earliestFromNow = now.plusMinutes(BreathPrayerScheduler.MIN_FROM_NOW_MIN)
    val lower = if (date == today && earliestFromNow.isAfter(morning)) {
        earliestFromNow
    } else {
        morning
    }
    return BreathPrayerWindow(lower, upper)
}
