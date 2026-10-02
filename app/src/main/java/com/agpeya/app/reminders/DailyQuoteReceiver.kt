package com.agpeya.app.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.agpeya.app.MainActivity
import com.agpeya.app.R
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.stringsFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Handles posting and refreshing the daily quote notification on the lock screen.
 */
class DailyQuoteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                runBlocking {
                    if (!SettingsRepository.dailyQuoteLockscreen(context).first()) return@runBlocking
                    DailyQuoteScheduler.schedule(context)
                    postNow(context)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        const val CHANNEL_ID = "daily_quote"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(NotificationManager::class.java)
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    val s = stringsFor(runBlocking { SettingsRepository.language(context).first() })
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        s.dailyQuoteChannelName,
                        NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        setShowBadge(false)
                    }
                    nm.createNotificationChannel(channel)
                }
            }
        }

        fun postNow(context: Context) {
            ensureChannel(context)
            val s = stringsFor(runBlocking { SettingsRepository.language(context).first() })
            // ponytail: sample quote for now; quote list will replace this when provided
            val quote = s.dailyQuoteSample

            val tap = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(s.dailyQuoteTitle)
                .setContentText(quote)
                .setStyle(NotificationCompat.BigTextStyle().bigText(quote))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(tap)
                .build()

            ReminderDispatchGate.locked {
                if (SettingsRepository.dailyQuoteLockscreenBlocking(context)) {
                    context.getSystemService(NotificationManager::class.java)
                        .notify(NotificationIds.DAILY_QUOTE, notification)
                }
            }
        }

        fun cancel(context: Context) {
            context.getSystemService(NotificationManager::class.java)
                .cancel(NotificationIds.DAILY_QUOTE)
        }

        /**
         * Checks whether lock screen notifications are allowed for this channel/app.
         */
        fun isLockscreenVisible(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(NotificationManager::class.java)
                if (!nm.areNotificationsEnabled()) return false
                ensureChannel(context)
                val channel = nm.getNotificationChannel(CHANNEL_ID) ?: return true
                if (channel.importance == NotificationManager.IMPORTANCE_NONE) return false
                if (channel.lockscreenVisibility == Notification.VISIBILITY_SECRET) return false
            }
            try {
                val globalLock = Settings.Secure.getInt(
                    context.contentResolver,
                    "lock_screen_show_notifications",
                    1,
                )
                if (globalLock == 0) return false
            } catch (_: Exception) {
            }
            return true
        }

        /**
         * Opens notification settings directly targeting this channel or app.
         */
        fun openChannelSettings(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                runCatching { context.startActivity(intent) }.onFailure {
                    val appIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    runCatching { context.startActivity(appIntent) }
                }
            } else {
                val appIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                runCatching { context.startActivity(appIntent) }
            }
        }
    }
}
