package com.agpeya.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.agpeya.app.R
import com.agpeya.app.data.AlarmAlert
import com.agpeya.app.data.AlarmSound
import com.agpeya.app.data.Language
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.stringsFor
import com.agpeya.app.ui.strings.Strings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Foreground service that turns a reminder into a real ringing alarm: it plays
 * the device alarm sound on a loop, vibrates, and posts a full-screen-intent
 * notification that launches [AlarmActivity] over the lock screen. It stops
 * when the user dismisses it or after [TIMEOUT_MS].
 */
class AlarmService : Service() {

    @Volatile
    private var player: MediaPlayer? = null

    @Volatile
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable {
        postDonePrompt() // timed out unanswered — still worth asking
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private var activeHourId: String? = null
    private var activeHourName: String = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS) {
            // The alarm was answered — follow up asking whether the prayer happened.
            postDonePrompt()
            stopSelf()
            return START_NOT_STICKY
        }
        val hourId = intent?.getStringExtra(ReminderScheduler.EXTRA_HOUR_ID) ?: "morning"
        val hourName = intent?.getStringExtra(ReminderScheduler.EXTRA_HOUR_NAME) ?: ""

        if (intent?.action == ACTION_SNOOZE) {
            activeHourId = null // snooze re-rings later; no done-prompt yet
            ReminderScheduler.snooze(this, hourId, hourName)
            stopSelf()
            return START_NOT_STICKY
        }

        activeHourId = hourId
        activeHourName = hourName
        ensureChannel()
        // startForeground must be called promptly and can't block on disk, so the
        // provisional notification uses system-locale strings (no DataStore read).
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(hourId, hourName, stringsFor(Language.SYSTEM)),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED else 0,
        )
        handler.postDelayed(autoStop, TIMEOUT_MS)
        // Read prefs off the main thread, then ring and (if the user overrode the
        // language) refine the notification.
        Thread {
            val alert = SettingsRepository.alarmAlertBlocking(this)
            val sound = SettingsRepository.alarmSoundBlocking(this)
            val language = runCatching {
                runBlocking { SettingsRepository.language(this@AlarmService).first() }
            }.getOrDefault(Language.SYSTEM)
            startRinging(alert, sound)
            if (language != Language.SYSTEM) {
                handler.post {
                    if (activeHourId == hourId) {
                        getSystemService(NotificationManager::class.java).notify(
                            NOTIFICATION_ID,
                            buildNotification(hourId, hourName, stringsFor(language)),
                        )
                    }
                }
            }
        }.start()
        // NOT_STICKY: a system restart would replay a null intent and ring "morning" spuriously.
        return START_NOT_STICKY
    }

    /** Quiet notification after the alarm ends: "Done?" with a Yes action that marks the hour. */
    private fun postDonePrompt() {
        val hourId = activeHourId ?: return
        activeHourId = null
        val app = applicationContext
        // Read the language off the main thread; the service may be stopping.
        Thread {
            val s = stringsFor(
                runCatching {
                    runBlocking { SettingsRepository.language(app).first() }
                }.getOrDefault(Language.SYSTEM),
            )
            val notifId = DONE_NOTIFICATION_BASE + hourId.hashCode() % 1000
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
            val notification = NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(s.donePrompt)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .addAction(0, s.yesAction, yes)
                .build()
            app.getSystemService(NotificationManager::class.java).notify(notifId, notification)
        }.start()
    }

    private fun startRinging(alert: AlarmAlert, sound: AlarmSound) {
        val wantSound = alert == AlarmAlert.SOUND_VIBRATE || alert == AlarmAlert.SOUND_ONLY
        val wantVibrate = alert == AlarmAlert.SOUND_VIBRATE || alert == AlarmAlert.VIBRATE_ONLY

        if (wantSound) {
            val type = when (sound) {
                AlarmSound.ALARM -> RingtoneManager.TYPE_ALARM
                AlarmSound.RINGTONE -> RingtoneManager.TYPE_RINGTONE
                AlarmSound.NOTIFICATION -> RingtoneManager.TYPE_NOTIFICATION
            }
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, type)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            runCatching {
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(this@AlarmService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        }

        if (wantVibrate) {
            val vib = if (Build.VERSION.SDK_INT >= 31) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator = vib
            val pattern = longArrayOf(0, 600, 800)
            vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }
    }

    private fun buildNotification(hourId: String, hourName: String, s: Strings): android.app.Notification {
        val fullScreen = PendingIntent.getActivity(
            this,
            1,
            Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(ReminderScheduler.EXTRA_HOUR_ID, hourId)
                putExtra(ReminderScheduler.EXTRA_HOUR_NAME, hourName)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val dismiss = PendingIntent.getService(
            this,
            2,
            Intent(this, AlarmService::class.java).setAction(ACTION_DISMISS),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val snooze = PendingIntent.getService(
            this,
            3,
            Intent(this, AlarmService::class.java).setAction(ACTION_SNOOZE).apply {
                putExtra(ReminderScheduler.EXTRA_HOUR_ID, hourId)
                putExtra(ReminderScheduler.EXTRA_HOUR_NAME, hourName)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        // Android 14+ can revoke full-screen-intent permission; fall back to the
        // heads-up notification (contentIntent still opens the alarm screen).
        val canFullScreen = Build.VERSION.SDK_INT < 34 ||
            getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(s.itsTime)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .apply { if (canFullScreen) setFullScreenIntent(fullScreen, true) }
            .setContentIntent(fullScreen)
            .addAction(0, s.snooze, snooze)
            .addAction(0, s.dismiss, dismiss)
            .build()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "የጸሎት ማንቂያ", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Prayer alarm"
                    setSound(null, null) // sound is played by the service on the alarm stream
                    enableVibration(false)
                }
            )
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        runCatching { player?.stop() }
        player?.release()
        player = null
        vibrator?.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "prayer_alarms"
        const val ACTION_DISMISS = "com.agpeya.app.ALARM_DISMISS"
        const val ACTION_SNOOZE = "com.agpeya.app.ALARM_SNOOZE"
        private const val NOTIFICATION_ID = 7001
        private const val DONE_NOTIFICATION_BASE = 7100
        private const val TIMEOUT_MS = 60_000L

        fun dismiss(context: Context) {
            context.startService(
                Intent(context, AlarmService::class.java).setAction(ACTION_DISMISS)
            )
        }
    }
}
