package com.agpeya.app.reminders

import androidx.core.net.toUri
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.agpeya.app.MainActivity
import com.agpeya.app.R
import com.agpeya.app.data.AlarmAlert
import com.agpeya.app.data.AlarmSound
import com.agpeya.app.data.Language
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.stringsFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Rings a prayer alarm as a pure notification — no foreground service. The
 * channel carries the alarm sound (USAGE_ALARM, so it follows the alarm
 * stream) and the vibration pattern; FLAG_INSISTENT loops both until the
 * notification is removed, and [TIMEOUT_MS] removes it on its own.
 *
 * Every ending — Dismiss, tapping Open, a swipe, or the timeout — routes
 * through [AlarmActionReceiver], which posts the quiet "done?" follow-up.
 * Snooze is the exception: it re-arms the alarm and asks nothing.
 *
 * Channels are immutable once created, so each (alert, sound) setting pair
 * gets its own channel variant; stale variants are dropped so the app's
 * notification settings never accumulate more than one alarm channel.
 */
object AlarmRinger {

    private const val CHANNEL_PREFIX = "prayer_alarm_"

    /** Wait, buzz, wait — looped by FLAG_INSISTENT until the alarm is answered. */
    private val VIBRATION_PATTERN = longArrayOf(0, 600, 800)

    private fun wantsSound(alert: AlarmAlert) =
        alert == AlarmAlert.SOUND_VIBRATE || alert == AlarmAlert.SOUND_ONLY

    private fun wantsVibrate(alert: AlarmAlert) =
        alert == AlarmAlert.SOUND_VIBRATE || alert == AlarmAlert.VIBRATE_ONLY

    /** The chosen ringtone, falling back to the notification tone. */
    private fun soundUri(context: Context, sound: AlarmSound): Uri? {
        val type = when (sound) {
            AlarmSound.ALARM -> RingtoneManager.TYPE_ALARM
            AlarmSound.RINGTONE -> RingtoneManager.TYPE_RINGTONE
            AlarmSound.NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
        }
        return RingtoneManager.getActualDefaultRingtoneUri(context, type)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
    private const val FOLLOWUP_CHANNEL_ID = "prayer_alarm_followup"
    private const val LEGACY_CHANNEL_ID = "prayer_alarms"
    private const val TIMEOUT_MS = 60_000L

    // Request codes 8-12: distinct from the reminder PendingIntents (0-3, 5-7).
    private const val OPEN_REQUEST_CODE = 8
    private const val SNOOZE_REQUEST_CODE = 9
    private const val DISMISS_REQUEST_CODE = 10
    private const val DELETE_REQUEST_CODE = 11
    private const val TIMEOUT_REQUEST_CODE = 12

    const val ACTION_SNOOZE = "com.agpeya.app.ALARM_SNOOZE"
    const val ACTION_DISMISS = "com.agpeya.app.ALARM_DISMISS"
    const val ACTION_REMOVED = "com.agpeya.app.ALARM_REMOVED"
    const val ACTION_TIMEOUT = "com.agpeya.app.ALARM_TIMEOUT"
    const val EXTRA_ALARM_SESSION = "alarmSession"

    /**
     * Post the ringing notification. Call from a background thread — it reads
     * settings. [snoozeCount] is how many times this hour has already been
     * postponed; past [SettingsRepository.MAX_SNOOZES] the alarm stops offering
     * to postpone it again.
     */
    fun ring(context: Context, hourId: String, hourName: String, snoozeCount: Int = 0) {
        val app = context.applicationContext
        val session = java.util.UUID.randomUUID().toString()
        AlarmEndSessions.begin(session)
        val alert = SettingsRepository.alarmAlertBlocking(app)
        val sound = SettingsRepository.alarmSoundBlocking(app)
        val language = runCatching {
            runBlocking { SettingsRepository.language(app).first() }
        }.getOrDefault(Language.SYSTEM)
        val s = stringsFor(language)

        val nm = app.getSystemService(NotificationManager::class.java)
        val channelId = ensureChannel(app, nm, alert, sound)

        fun activityPi(requestCode: Int) = PendingIntent.getActivity(
            app,
            requestCode,
            Intent(app, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                data = "agpeya://alarm/open/$hourId".toUri()
                putExtra(ReminderScheduler.EXTRA_HOUR_ID, hourId)
                putExtra(EXTRA_ALARM_SESSION, session)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        fun broadcastPi(requestCode: Int, action: String) = PendingIntent.getBroadcast(
            app,
            requestCode,
            Intent(app, AlarmActionReceiver::class.java).apply {
                this.action = action
                data = "agpeya://alarm/${action.substringAfterLast('.')}/$hourId".toUri()
                putExtra(ReminderScheduler.EXTRA_HOUR_ID, hourId)
                putExtra(ReminderScheduler.EXTRA_HOUR_NAME, hourName)
                putExtra(ReminderScheduler.EXTRA_SNOOZE_COUNT, snoozeCount + 1)
                putExtra(EXTRA_ALARM_SESSION, session)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val open = activityPi(OPEN_REQUEST_CODE)
        val notification = NotificationCompat.Builder(app, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            // "ሰርክ ደርሷል", not "ጊዜው ደርሷል": the hour itself is the summons.
            .setContentTitle(if (hourName.isNotBlank()) s.hourArrived(hourName) else s.itsTime)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setDeleteIntent(broadcastPi(DELETE_REQUEST_CODE, ACTION_REMOVED))
            .setTimeoutAfter(TIMEOUT_MS)
            .addAction(0, s.openShort, open)
            .also { b ->
                // An hour that can be postponed for ever is an hour that is
                // never prayed, so the third አሳድር is the last one offered.
                if (snoozeCount < SettingsRepository.MAX_SNOOZES) {
                    b.addAction(0, s.snooze, broadcastPi(SNOOZE_REQUEST_CODE, ACTION_SNOOZE))
                }
            }
            .addAction(0, s.dismiss, broadcastPi(DISMISS_REQUEST_CODE, ACTION_DISMISS))
            // The count is worth showing: it is the difference between an alarm
            // that has just arrived and one that has been put off twice already.
            .also { b ->
                if (snoozeCount > 0) b.setContentText(s.snoozedTimes(snoozeCount))
            }
            // Below Oreo there is no channel to carry them, and this carried
            // nothing either: on Android 6 and 7 — which the app supports and
            // which [ensureChannel] returns early for — the prayer alarm posted
            // with no sound and no vibration at all, and FLAG_INSISTENT dutifully
            // looped the silence. The alert setting reaches those phones here.
            .also { b ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return@also
                if (wantsSound(alert)) {
                    @Suppress("DEPRECATION")
                    b.setSound(soundUri(app, sound), android.media.AudioManager.STREAM_ALARM)
                }
                // An explicit empty pattern, not merely no pattern: without one
                // a phone set to vibrate-for-everything can still buzz on a
                // notification the reader asked to be silent.
                b.setVibrate(if (wantsVibrate(alert)) VIBRATION_PATTERN else longArrayOf(0))
            }
            .build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        nm.notify(NotificationIds.ALARM, notification)
        // setTimeoutAfter removes the notification but tells us nothing, so the
        // unanswered alarm still has to ask "done?" — that needs our own alarm.
        scheduleTimeout(app, hourId, session)
    }

    /** Fires [ACTION_TIMEOUT] once the alarm has rung itself out unanswered. */
    @SuppressLint("MissingPermission") // Guarded by canScheduleExactAlarms.
    private fun scheduleTimeout(context: Context, hourId: String, session: String) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val at = System.currentTimeMillis() + TIMEOUT_MS
        val pi = timeoutIntent(context, hourId, session)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    /**
     * Drop this ring's pending timeout so an answered alarm is never asked
     * about twice. Looks the intent up with NO_CREATE rather than rebuilding
     * it: matching ignores extras, so UPDATE_CURRENT here would blank the hour
     * id the scheduled alarm still carries.
     *
     * Keyed by session, like [timeoutIntent] — cancelling by action alone would
     * take down whichever ring's timeout happened to hold the slot.
     */
    fun cancelTimeout(context: Context, session: String) {
        val app = context.applicationContext
        val pending = PendingIntent.getBroadcast(
            app,
            TIMEOUT_REQUEST_CODE,
            Intent(app, AlarmActionReceiver::class.java)
                .setAction(ACTION_TIMEOUT)
                .setData(timeoutData(session)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        ) ?: return
        app.getSystemService(AlarmManager::class.java).cancel(pending)
        pending.cancel()
    }

    /**
     * The session in the data URI, which is what makes one ring's timeout
     * distinct from another's.
     *
     * PendingIntent identity is the request code plus `Intent.filterEquals`,
     * and extras are no part of either. With the action alone to tell them
     * apart, every ring shared one timeout: a second alarm within the minute
     * re-armed that one PendingIntent and the first ring's timeout was gone
     * with it, so the first hour was never asked whether it had been prayed.
     */
    private fun timeoutData(session: String): Uri = "agpeya://alarm/timeout/$session".toUri()

    private fun timeoutIntent(context: Context, hourId: String, session: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            TIMEOUT_REQUEST_CODE,
            Intent(context, AlarmActionReceiver::class.java).apply {
                action = ACTION_TIMEOUT
                data = timeoutData(session)
                // Outside the match criteria (extras are ignored there) but
                // carried so the receiver knows which hour rang.
                putExtra(ReminderScheduler.EXTRA_HOUR_ID, hourId)
                putExtra(EXTRA_ALARM_SESSION, session)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /**
     * Take the ringing notification down. This does NOT post the follow-up:
     * an app-side cancel() never fires the delete intent (only a user clearing
     * the notification does), so every path that ends an alarm asks for the
     * "done?" prompt explicitly.
     */
    fun stop(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NotificationIds.ALARM)
    }

    /**
     * The alarm was answered from inside the app (the user tapped Open). Routed
     * through the receiver so the settings read stays off the main thread.
     */
    fun answered(context: Context, hourId: String, session: String?) {
        context.sendBroadcast(
            Intent(context, AlarmActionReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra(ReminderScheduler.EXTRA_HOUR_ID, hourId)
                session?.let { putExtra(EXTRA_ALARM_SESSION, it) }
            },
        )
    }

    /**
     * A quiet line confirming a postponed hour and when it returns.
     *
     * Snoozing used to do its work in silence: the notification vanished and
     * nothing said whether the alarm had been put off or lost. It clears itself
     * as the alarm comes back.
     */
    fun postSnoozeNotice(
        context: Context,
        hourId: String,
        hourName: String,
        backAt: java.time.LocalDateTime,
        count: Int,
    ) {
        val app = context.applicationContext
        val s = stringsFor(
            runCatching {
                runBlocking { SettingsRepository.language(app).first() }
            }.getOrDefault(Language.SYSTEM),
        )
        val nm = app.getSystemService(NotificationManager::class.java)
        ensureFollowupChannel(app, nm)
        val at = backAt.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        val notification = NotificationCompat.Builder(app, FOLLOWUP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (hourName.isNotBlank()) s.snoozedUntil(hourName, at) else s.snoozedUntilPlain(at))
            .apply { if (count >= SettingsRepository.MAX_SNOOZES) setContentText(s.snoozeLastOne) }
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setAutoCancel(true)
            .setTimeoutAfter(
                (java.time.Duration.between(java.time.LocalDateTime.now(), backAt).toMillis())
                    .coerceAtLeast(1_000L),
            )
            .build()
        nm.notify(NotificationIds.inFamily(NotificationIds.SNOOZE_BASE, hourId), notification)
    }

    /** The silent channel both quiet follow-ups share. */
    private fun ensureFollowupChannel(app: Context, nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (nm.getNotificationChannel(FOLLOWUP_CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(FOLLOWUP_CHANNEL_ID, "የጸሎት ማንቂያ ክትትል", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Prayer alarm follow-up"
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    /** Quiet follow-up after the alarm ends: "Done?" with a Yes that marks the hour. */
    fun postDonePrompt(context: Context, hourId: String) {
        val app = context.applicationContext
        val s = stringsFor(
            runCatching {
                runBlocking { SettingsRepository.language(app).first() }
            }.getOrDefault(Language.SYSTEM),
        )
        val nm = app.getSystemService(NotificationManager::class.java)
        ensureFollowupChannel(app, nm)
        val notifId = NotificationIds.inFamily(NotificationIds.DONE_BASE, hourId)
        val yes = PendingIntent.getBroadcast(
            app,
            notifId,
            Intent(app, MarkDoneReceiver::class.java).apply {
                putExtra(ReminderScheduler.EXTRA_HOUR_ID, hourId)
                putExtra(MarkDoneReceiver.EXTRA_NOTIFICATION_ID, notifId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // Discreet on purpose — like the alarm itself, no prayer wording.
        val notification = NotificationCompat.Builder(app, FOLLOWUP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(s.donePrompt)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, s.yesAction, yes)
            .build()
        nm.notify(notifId, notification)
    }

    /**
     * Create (once) the channel for this settings pair, dropping stale variants.
     *
     * Pre-Oreo there are no channels at all: the returned id is still used as
     * the builder's channel name (harmless and ignored there), and the alarm's
     * sound and vibration come from the notification itself instead.
     */
    private fun ensureChannel(
        context: Context,
        nm: NotificationManager,
        alert: AlarmAlert,
        sound: AlarmSound,
    ): String {
        val id = CHANNEL_PREFIX + "${alert.name}_${sound.name}".lowercase()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return id
        // One alarm channel at a time: remove the pre-notification-alarm channel
        // and any variant left over from an earlier settings choice.
        nm.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        nm.notificationChannels.forEach { existing ->
            if (existing.id.startsWith(CHANNEL_PREFIX) &&
                existing.id != id && existing.id != FOLLOWUP_CHANNEL_ID
            ) {
                nm.deleteNotificationChannel(existing.id)
            }
        }
        if (nm.getNotificationChannel(id) != null) return id

        val channel = NotificationChannel(id, "የጸሎት ማንቂያ", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Prayer alarm"
            if (wantsSound(alert)) {
                setSound(
                    soundUri(context, sound),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            } else {
                setSound(null, null)
            }
            enableVibration(wantsVibrate(alert))
            if (wantsVibrate(alert)) vibrationPattern = VIBRATION_PATTERN
        }
        nm.createNotificationChannel(channel)
        return id
    }
}
