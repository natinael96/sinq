package com.agpeya.app.widget

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.agpeya.app.MainActivity
import com.agpeya.app.R
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

/** The website's day dial, drawn locally. TextClock keeps the digital time live in the launcher. */
class PrayerClockWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context)
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = refresh(context)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in REFRESH_ACTIONS) refresh(context) else super.onReceive(context, intent)
    }
    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))
    }

    private fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        if (manager.getAppWidgetIds(ComponentName(context, javaClass)).isEmpty()) return
        val pending = goAsync()
        Thread {
            try {
                // Re-read after starting the worker: removal must not leave a repeating alarm.
                val ids = manager.getAppWidgetIds(ComponentName(context, javaClass))
                if (ids.isEmpty()) return@Thread
                val time = LocalTime.now()
                val current = PrayerClock.current(time)
                ids.forEach { id ->
                    val options = manager.getAppWidgetOptions(id)
                    val dp = minOf(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250),
                        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 250) - 40).coerceIn(120, 400)
                    val pixels = (dp * context.resources.displayMetrics.density).toInt().coerceIn(240, 720)
                    val views = RemoteViews(context.packageName, R.layout.widget_prayer_clock)
                    views.setImageViewBitmap(R.id.prayer_clock_dial, drawDial(context, time, pixels))
                    views.setContentDescription(R.id.prayer_clock_dial,
                        "${current.amharic} · ${current.english} · %02d:00".format(current.hour))
                    views.setOnClickPendingIntent(R.id.prayer_clock_root, PendingIntent.getActivity(context, 0,
                        Intent(context, MainActivity::class.java).apply {
                            action = ACTION_OPEN
                            putExtra(MainActivity.EXTRA_SKIP_INTRO, true)
                            putExtra(EXTRA_OPEN_CLOCK, true)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                    manager.updateAppWidget(id, views)
                }
                if (manager.getAppWidgetIds(ComponentName(context, javaClass)).isNotEmpty()) schedule(context)
            } catch (error: Exception) {
                Log.w("PrayerClockWidget", "Could not refresh clock", error)
            } finally { pending.finish() }
        }.start()
    }

    /** Non-wakeup refresh: the dial never wakes a sleeping phone or starts a foreground service. */
    @SuppressLint("ScheduleExactAlarm")
    private fun schedule(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val nextMinute = (System.currentTimeMillis() / 60_000 + 1) * 60_000
        val operation = refreshIntent(context)
        if (Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()) {
            try { alarm.setExact(AlarmManager.RTC, nextMinute, operation); return }
            catch (_: SecurityException) { /* Access may have been revoked since the check. */ }
        }
        alarm.set(AlarmManager.RTC, nextMinute, operation)
    }

    private fun refreshIntent(context: Context) = PendingIntent.getBroadcast(context, 0,
        Intent(context, PrayerClockWidgetProvider::class.java).setAction(ACTION_REFRESH),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun drawDial(context: Context, time: LocalTime, pixels: Int): android.graphics.Bitmap {
        val bitmap = createBitmap(pixels, pixels)
        val canvas = Canvas(bitmap)
        canvas.scale(pixels / 300f, pixels / 300f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val font = runCatching { ResourcesCompat.getFont(context, R.font.abyssinica_sil) }.getOrNull() ?: Typeface.DEFAULT
        val gold = 0xFFE4BC5A.toInt()
        val green = 0xFF10382F.toInt()
        fun line(color: Int, width: Float) { paint.color = color; paint.strokeWidth = width; paint.style = Paint.Style.STROKE }
        fun text(value: String, y: Float, size: Float, color: Int, face: Typeface = Typeface.DEFAULT) {
            paint.style = Paint.Style.FILL; paint.color = color; paint.textSize = size; paint.typeface = face
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(value, 150f, y, paint)
        }
        line(0x25FFFFFF, 1.5f)
        canvas.drawCircle(150f, 150f, 112f, paint)
        for (i in 0..7) {
            val a = i * Math.PI / 4 - Math.PI / 2
            canvas.drawLine(150f + cos(a).toFloat() * 100, 150f + sin(a).toFloat() * 100,
                150f + cos(a).toFloat() * 112, 150f + sin(a).toFloat() * 112, paint)
        }
        val fraction = PrayerClock.fraction(time)
        line(gold, 2.5f)
        canvas.drawArc(RectF(38f, 38f, 262f, 262f), -90f, fraction * 360f, false, paint)
        val angle = fraction * Math.PI * 2 - Math.PI / 2
        line(gold, 1.5f)
        canvas.drawLine(150f, 150f, 150f + cos(angle).toFloat() * 98, 150f + sin(angle).toFloat() * 98, paint)
        val current = PrayerClock.current(time)
        PrayerClock.hours.forEach { hour ->
            val a = hour.hour / 24.0 * Math.PI * 2 - Math.PI / 2
            val x = 150f + cos(a).toFloat() * 112
            val y = 150f + sin(a).toFloat() * 112
            paint.style = Paint.Style.FILL; paint.color = if (hour == current) gold else green
            canvas.drawCircle(x, y, 18f, paint)
            line(if (hour == current) gold else 0xFF567363.toInt(), 1f)
            canvas.drawCircle(x, y, 18f, paint)
            paint.style = Paint.Style.FILL; paint.color = if (hour == current) green else 0xFFF6F2E6.toInt()
            paint.typeface = font; paint.textSize = 18f; paint.textAlign = Paint.Align.CENTER
            canvas.drawText(hour.number, x, y - (paint.ascent() + paint.descent()) / 2, paint)
        }
        // Opaque center keeps the hand from crossing the readout, as on the website.
        paint.style = Paint.Style.FILL; paint.color = green
        canvas.drawRoundRect(RectF(77f, 108f, 223f, 199f), 10f, 10f, paint)
        text("NOW · አሁን", 123f, 10f, 0xFF8FA99C.toInt(), font)
        text(current.amharic, 151f, 18f, 0xFFF6F2E6.toInt(), font)
        text(current.english, 172f, 12f, 0xFFB4C9BF.toInt())
        text("%02d:00".format(current.hour), 192f, 12f, gold)
        return bitmap
    }

    companion object {
        const val EXTRA_OPEN_CLOCK = "openPrayerClock"
        private const val ACTION_OPEN = "com.agpeya.app.widget.OPEN_PRAYER_CLOCK"
        private const val ACTION_REFRESH = "com.agpeya.app.widget.REFRESH_PRAYER_CLOCK"
        private val REFRESH_ACTIONS = setOf(ACTION_REFRESH, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED, Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
    }
}
