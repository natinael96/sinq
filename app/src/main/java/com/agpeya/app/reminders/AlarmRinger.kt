package com.agpeya.app.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
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
 * Any removal — Dismiss, tapping Open, a swipe, or the timeout — fires the
 * delete intent, which posts the quiet "done?" follow-up. Snooze suppresses
 * that one follow-up and re-arms the alarm instead.
 *
 * Channels are immutable once created, so each (alert, sound) setting pair
 * gets its own channel variant; stale variants are dropped so the app's
 * notification settings never accumulate more than one alarm channel.
 */
object AlarmRinger {

    private const val CHANNEL_PREFIX = "prayer_alarm_"
    private const val FOLLOWUP_CHANNEL_ID = "prayer_alarm_followup"
    private const val LEGACY_CHANNEL_ID = "prayer_alarms"
    private const val NOTIFICATION_ID = 7001
    private const val DONE_NOTIFICATION_BASE = 7100
    private const val TIMEOUT_MS = 60_000L

    // Request codes 8-11: distinct from the reminder PendingIntents (0-3, 5-7).
    private const val OPEN_REQUEST_CODE = 8
    private const val SNOOZE_REQUEST_CODE = 9
    private const val DISMISS_REQUEST_CODE = 10
    private const val DELETE_REQUEST_CODE = 11

    const val ACTION_SNOOZE = "com.agpeya.app.ALARM_SNOOZE"
    const val ACTION_DISMISS = "com.agpeya.app.ALARM_DISMISS"
    const val ACTION_REMOVED = "com.agpeya.app.ALARM_REMOVED"

    /** Set right before a snooze cancels the notification, so the resulting
     *  removal doesn't ask "done?" about a prayer that was only postponed. */
    @Volatile
    var suppressFollowUp: Boolean = false

    /** Post the ringing notification. Call from a background thread — it reads settings. */
    fun ring(context: Context, hourId: String, hourName: String) {
        val app = context.applicationContext
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
                data = Uri.parse("agpeya://alarm/open/$hourId")
                putExtra(ReminderScheduler.EXTRA_HOUR_ID, hourId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        fun broadcastPi(requestCode: Int, action: String) = PendingIntent.getBroadcast(
            app,
            requestCode,
            Intent(app, AlarmActionReceiver::class.java).apply {
                this.action = action
                data = Uri.parse("agpeya://alarm/${action.substringAfterLast('.')}/$hourId")
                putExtra(ReminderScheduler.EXTRA_HOUR_ID, hourId)
                putExtra(ReminderScheduler.EXTRA_HOUR_NAME, hourName)
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
            .addAction(0, s.snooze, broadcastPi(SNOOZE_REQUEST_CODE, ACTION_SNOOZE))
            .addAction(0, s.dismiss, broadcastPi(DISMISS_REQUEST_CODE, ACTION_DISMISS))
            .build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        nm.notify(NOTIFICATION_ID, notification)
    }

    /** Take the ringing notification down (its removal posts the follow-up). */
    fun stop(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
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
        if (nm.getNotificationChannel(FOLLOWUP_CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(FOLLOWUP_CHANNEL_ID, "የጸሎት ማንቂያ ክትትል", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Prayer alarm follow-up"
                    setSound(null, null)
                    enableVibration(false)
                },
            )
        }
        val notifId = DONE_NOTIFICATION_BASE + Math.floorMod(hourId.hashCode(), 1000)
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

    /** Create (once) the channel for this settings pair, dropping stale variants. */
    private fun ensureChannel(
        context: Context,
        nm: NotificationManager,
        alert: AlarmAlert,
        sound: AlarmSound,
    ): String {
        val id = CHANNEL_PREFIX + "${alert.name}_${sound.name}".lowercase()
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

        val wantSound = alert == AlarmAlert.SOUND_VIBRATE || alert == AlarmAlert.SOUND_ONLY
        val wantVibrate = alert == AlarmAlert.SOUND_VIBRATE || alert == AlarmAlert.VIBRATE_ONLY
        val channel = NotificationChannel(id, "የጸሎት ማንቂያ", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Prayer alarm"
            if (wantSound) {
                val type = when (sound) {
                    AlarmSound.ALARM -> RingtoneManager.TYPE_ALARM
                    AlarmSound.RINGTONE -> RingtoneManager.TYPE_RINGTONE
                    AlarmSound.NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
                }
                setSound(
                    RingtoneManager.getActualDefaultRingtoneUri(context, type)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            } else {
                setSound(null, null)
            }
            enableVibration(wantVibrate)
            if (wantVibrate) vibrationPattern = longArrayOf(0, 600, 800)
        }
        nm.createNotificationChannel(channel)
        return id
    }
}
