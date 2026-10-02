package com.agpeya.app.widget

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.agpeya.app.MainActivity
import com.agpeya.app.R
import com.agpeya.app.data.DailyQuoteRepository
import com.agpeya.app.data.Language
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.reminders.DailyQuoteReceiver
import com.agpeya.app.stringsFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * የአበው ምክር home-screen and lock-screen widget.
 * Refreshes daily at midnight and on date/time/timezone/locale broadcasts.
 */
class DailyQuoteWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    ?: AppWidgetManager.getInstance(context).getAppWidgetIds(componentOf(context))
                refreshAsync(context, ids)
            }
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED -> {
                val id = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID,
                )
                if (id != AppWidgetManager.INVALID_APPWIDGET_ID) refreshAsync(context, intArrayOf(id))
            }
            in REFRESH_ACTIONS -> {
                refreshAsync(context, AppWidgetManager.getInstance(context).getAppWidgetIds(componentOf(context)))
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(dayRefreshIntent(context))
        super.onDisabled(context)
    }

    private fun refreshAsync(context: Context, ids: IntArray) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        Thread {
            try {
                val manager = AppWidgetManager.getInstance(context)
                ids.forEach { id ->
                    manager.updateAppWidget(id, render(context, manager, id))
                }
                scheduleDayRefresh(context)
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun render(context: Context, manager: AppWidgetManager, id: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_daily_quote)
        val today = LocalDate.now()
        val quote = DailyQuoteRepository.quoteForBlocking(context, today)
        val lang = runCatching {
            runBlocking { SettingsRepository.language(context).first() }
        }.getOrDefault(Language.SYSTEM)
        val s = stringsFor(lang)

        val author = if (lang == Language.ENGLISH) {
            quote.authorEn.ifBlank { quote.authorAm }
        } else {
            quote.authorAm.ifBlank { quote.authorEn }
        }

        views.setTextViewText(R.id.widget_quote_tag, "☩ ${s.dailyQuoteChannelName}")
        views.setTextViewText(R.id.widget_quote_author, author)
        views.setTextViewText(R.id.widget_quote_body, "«${quote.quote}»")

        views.setOnClickPendingIntent(R.id.widget_quote_root, openApp(context))
        return views
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE,
        Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_SKIP_INTRO, true)
            putExtra(DailyQuoteReceiver.EXTRA_OPEN_DAILY_QUOTE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    @SuppressLint("MissingPermission")
    private fun scheduleDayRefresh(context: Context) {
        val next = LocalDateTime.now().toLocalDate().plusDays(1).atStartOfDay().plusSeconds(5)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = dayRefreshIntent(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun dayRefreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REFRESH_REQUEST_CODE,
        Intent(context, DailyQuoteWidgetProvider::class.java).setAction(ACTION_DAY_REFRESH),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        private const val ACTION_DAY_REFRESH = "com.agpeya.app.widget.DAILY_QUOTE_DAY_REFRESH"
        private const val REQUEST_CODE = 9601
        private const val REFRESH_REQUEST_CODE = 9602

        private val REFRESH_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_DAY_REFRESH,
        )

        fun componentOf(context: Context) = ComponentName(context, DailyQuoteWidgetProvider::class.java)

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(componentOf(context))
            if (ids.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(context, DailyQuoteWidgetProvider::class.java)
                        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
                )
            }
        }
    }
}
