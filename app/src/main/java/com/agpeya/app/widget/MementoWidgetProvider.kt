package com.agpeya.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import com.agpeya.app.MainActivity
import com.agpeya.app.R
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.stringsFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

/**
 * ሞትን አስብ — the memento mori widget.
 *
 * One phrase and nothing else: no date, no reading, no state. The splash opens
 * on *memento mori* because it frames why one prays at all, and a widget that
 * kept the words in view is the same thought left standing on the home screen.
 * Deliberately without a refresh: something that updated would be a different
 * thing entirely, and the point of this one is that it does not change.
 *
 * The Amharic is drawn to a bitmap rather than set on a TextView. A widget
 * layout can only name a font resource from API 26, and below that the face
 * would be silently dropped for sans — on exactly the Android 6 and 7 phones
 * the minSdk floor was lowered to reach. Rendering it here with the Typeface
 * gives every device the same lettering.
 */
class MementoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, render(context, manager, it)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context, MementoWidgetProvider::class.java),
                )
                onUpdate(context, manager, ids)
            }
            else -> super.onReceive(context, intent)
        }
    }

    private fun render(context: Context, manager: AppWidgetManager, id: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_memento)
        val options = manager.getAppWidgetOptions(id)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)

        val gloss = runCatching {
            runBlocking { stringsFor(SettingsRepository.language(context).first()).mementoMoriGloss }
        }.getOrDefault(context.getString(R.string.widget_memento_label))

        // The phrase is the widget, so it takes the height it is given: a third
        // of the card, bounded so a 4x4 does not turn it into a poster and a
        // slim strip still leaves it legible.
        val sizeSp = (minHeight * 0.30f).coerceIn(20f, 44f)
        views.setImageViewBitmap(
            R.id.memento_amharic,
            lettering(context, gloss, sizeSp, (minWidth - 32).coerceAtLeast(80)),
        )

        // Under about two cells there is room for the phrase and nothing else.
        val roomForLatin = minHeight >= 96
        views.setViewVisibility(R.id.memento_rule, if (roomForLatin) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.memento_latin, if (roomForLatin) View.VISIBLE else View.GONE)

        views.setOnClickPendingIntent(R.id.memento_root, openApp(context))
        return views
    }

    /**
     * The phrase, drawn in ዋልድባ.
     *
     * Looked up by name rather than as R.font.waldba so the app builds and runs
     * without the file present, falling back to the display face it already
     * ships. Drop `waldba.ttf` into res/font and this picks it up with no code
     * change — see the licence note in docs/fonts.
     */
    private fun lettering(context: Context, text: String, sizeSp: Float, maxWidthDp: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GOLD
            textSize = sizeSp * density
            typeface = displayTypeface(context)
        }
        // Shrink to fit rather than clip: ሞትን አስብ is short, but a translation
        // of it need not be, and a widget cannot scroll.
        val maxWidthPx = maxWidthDp * density
        var measured = paint.measureText(text)
        if (measured > maxWidthPx && measured > 0f) {
            paint.textSize *= maxWidthPx / measured
            measured = paint.measureText(text)
        }
        val metrics = paint.fontMetrics
        val height = (metrics.bottom - metrics.top).coerceAtLeast(1f)
        val bitmap = Bitmap.createBitmap(
            measured.roundToInt().coerceAtLeast(1),
            height.roundToInt(),
            Bitmap.Config.ARGB_8888,
        )
        Canvas(bitmap).drawText(text, 0f, -metrics.top, paint)
        return bitmap
    }

    private fun displayTypeface(context: Context): Typeface {
        val res = context.resources
        val id = res.getIdentifier("waldba", "font", context.packageName)
            .takeIf { it != 0 }
            ?: R.font.bela_bereka
        return runCatching { ResourcesCompat.getFont(context, id) }
            .onFailure { Log.d(TAG, "display font unavailable, using default: ${it.message}") }
            .getOrNull() ?: Typeface.DEFAULT
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val TAG = "MementoWidget"
        /** The widget gold, matching widget_gitsawe's own. */
        const val GOLD = 0xFFE4BC5A.toInt()
        /** Distinct from the reminder and ግጻዌ PendingIntents (0-3, 5-11). */
        const val REQUEST_CODE = 12
    }
}
