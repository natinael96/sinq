package com.agpeya.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.agpeya.app.MainActivity
import com.agpeya.app.R
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.GitsaweReading
import com.agpeya.app.model.GitsaweService
import com.agpeya.app.model.VerseRef
import com.agpeya.app.reminders.GitsaweReminderScheduler
import com.agpeya.app.stringsFor
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.reading.geezNumeral
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * የዕለቱ ግጻዌ — a home-screen widget showing today's ምስባክ and ወንጌል at a glance.
 * Tapping it opens the ግጻዌ screen.
 *
 * Content is read off the main thread in [onUpdate]'s worker and pushed as plain
 * [RemoteViews]; the widget renders in the launcher's process, so it uses system
 * text views (no Compose, no bundled fonts). It refreshes when the date rolls
 * over — see the manifest intent filter — rather than on a polling interval.
 */
class GitsaweWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // The date rolled over (or the clock/timezone moved, or we just booted):
        // re-render so the widget never shows yesterday's readings.
        if (intent.action in REFRESH_ACTIONS) {
            val manager = AppWidgetManager.getInstance(context)
            refresh(context, manager, manager.getAppWidgetIds(componentOf(context)))
        }
    }

    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val app = context.applicationContext
        val pending = goAsync()
        Thread {
            try {
                val views = runCatching { buildViews(app) }.getOrElse { return@Thread }
                ids.forEach { manager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun buildViews(context: Context): RemoteViews = runBlocking {
        val s = stringsFor(SettingsRepository.language(context).first())
        val today = LocalDate.now()
        val readings = GitsaweRepository.readingsFor(context, today)

        val views = RemoteViews(context.packageName, R.layout.widget_gitsawe)
        views.setTextViewText(R.id.widget_kicker, s.gitsaweTitle)
        views.setTextViewText(R.id.widget_date, formatEthiopian(today, s))

        // Prefer the ቅዳሴ (liturgy) service, falling back to ነግህ (matins).
        val entry = readings.daily
        val service = entry?.kidassie ?: entry?.negh
        val title = entry?.title?.takeIf { it.isNotBlank() }
            ?: readings.feasts.firstOrNull()?.amharicName
            ?: s.gitsaweTitle
        views.setTextViewText(R.id.widget_title, title)

        val rows = service?.let { pickRows(it) }.orEmpty()
        if (rows.isEmpty()) {
            views.setViewVisibility(R.id.widget_row_1, View.GONE)
            views.setViewVisibility(R.id.widget_row_2, View.GONE)
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            views.setTextViewText(R.id.widget_empty, s.noGitsaweToday)
        } else {
            views.setViewVisibility(R.id.widget_empty, View.GONE)
            bindRow(views, 0, rows.getOrNull(0), R.id.widget_row_1, R.id.widget_role_1, R.id.widget_ref_1)
            bindRow(views, 1, rows.getOrNull(1), R.id.widget_row_2, R.id.widget_role_2, R.id.widget_ref_2)
        }

        // Whole widget taps through to the ግጻዌ screen.
        views.setOnClickPendingIntent(R.id.widget_root, openGitsawe(context))
        views
    }

    private fun bindRow(
        views: RemoteViews,
        index: Int,
        row: Pair<String, String>?,
        rowId: Int,
        roleId: Int,
        refId: Int,
    ) {
        if (row == null) {
            views.setViewVisibility(rowId, View.GONE)
            return
        }
        views.setViewVisibility(rowId, View.VISIBLE)
        views.setTextViewText(roleId, row.first)
        views.setTextViewText(refId, row.second)
    }

    /** The two most useful lines for a glance: ምስባክ and ወንጌል, when present. */
    private fun pickRows(service: GitsaweService): List<Pair<String, String>> =
        listOfNotNull(
            service.msbak.firstOrNull()?.let { "ምስባክ" to summarize(it) },
            service.wengel.firstOrNull()?.let { "ወንጌል" to summarize(it) },
        )

    private fun summarize(reading: GitsaweReading): String =
        reading.verse?.let { verseRef(it) }?.takeIf { it.isNotBlank() }
            ?: reading.text?.amharic
            ?: reading.text?.geez
            ?: ""

    /** "መዝሙረ ዳዊት ፷፬፥፲፩–፲፪" — book with Ge'ez chapter:verse(range). */
    private fun verseRef(v: VerseRef): String = buildString {
        v.bookTitle?.let { append(it); append(" ") }
        v.chapter?.let { append(geezNumeral(it)) }
        v.start?.let { append("፥"); append(geezNumeral(it)) }
        v.end?.let { append("–"); append(geezNumeral(it)) }
    }.trim()

    private fun openGitsawe(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(GitsaweReminderScheduler.EXTRA_OPEN_GITSAWE, true)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        /** Distinct from the reminder PendingIntents (0, 1, 2) — see the audit. */
        private const val REQUEST_CODE = 3

        private val REFRESH_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCALE_CHANGED,
        )

        private fun componentOf(context: Context) =
            ComponentName(context, GitsaweWidgetProvider::class.java)

        /** Ask every placed widget to re-render (e.g. after the language changes). */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(componentOf(context))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, GitsaweWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                },
            )
        }
    }
}
