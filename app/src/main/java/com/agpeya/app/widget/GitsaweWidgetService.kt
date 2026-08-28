package com.agpeya.app.widget

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
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
import java.time.LocalDateTime

/**
 * Feeds the widget's collection its single current card: today by day, tomorrow
 * from 19:00 so the evening glance helps prepare for the coming liturgical day.
 */
class GitsaweWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        GitsaweWidgetFactory(applicationContext)
}

private class GitsaweWidgetFactory(
    private val context: Context,
) : RemoteViewsService.RemoteViewsFactory {

    /** One card, fully resolved off the UI thread in [onDataSetChanged]. */
    private data class Page(
        val header: String,
        val title: String,
        val rows: List<Pair<String, String>>,
        val kidase: String?,
        val emptyText: String,
        /** The day genuinely has something to read (not just a fallback title). */
        val hasContent: Boolean,
        val ctaText: String,
        val epochDay: Long,
    )

    private var pages: List<Page> = emptyList()

    override fun onCreate() = Unit

    override fun onDestroy() = Unit

    // Called on a binder thread — blocking reads are fine (and expected) here.
    override fun onDataSetChanged() {
        pages = runCatching { buildPages() }.getOrDefault(emptyList())
    }

    /** One honest card: today's by day, tomorrow's after the evening rollover. */
    private fun buildPages(): List<Page> = runBlocking {
        val s = stringsFor(SettingsRepository.language(context).first())
        val now = LocalDateTime.now()
        val date = gitsaweWidgetDate(now)
        val label = if (date == now.toLocalDate()) s.todayLabel else s.tomorrowLabel
        listOf(pageFor(date, "$label  ·  ${formatEthiopian(date, s)}", s))
    }

    private suspend fun pageFor(date: LocalDate, header: String, s: com.agpeya.app.ui.strings.Strings): Page {
        val readings = GitsaweRepository.readingsFor(context, date)
        // Prefer the ቅዳሴ (liturgy) service, falling back to ነግህ (matins).
        val entry = readings.daily
        val service = entry?.kidassie ?: entry?.negh
        val realTitle = entry?.title?.takeIf { it.isNotBlank() }
            ?: readings.feasts.firstOrNull()?.amharicName
        val rows = service?.let { pickRows(it) }.orEmpty()
        return Page(
            header = header,
            title = realTitle ?: s.gitsaweTitle,
            rows = rows,
            kidase = service?.kidassie?.firstOrNull { it.isNotBlank() }?.trim(),
            emptyText = s.noGitsaweToday,
            hasContent = rows.isNotEmpty() || realTitle != null,
            ctaText = "${s.readGitsawe} →",
            epochDay = date.toEpochDay(),
        )
    }

    override fun getCount(): Int = pages.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_gitsawe_page)
        val page = pages.getOrNull(position) ?: return views
        views.setTextViewText(R.id.widget_date, page.header)
        views.setTextViewText(R.id.widget_title, page.title)
        if (page.rows.isEmpty()) {
            views.setViewVisibility(R.id.widget_row_1, View.GONE)
            views.setViewVisibility(R.id.widget_row_2, View.GONE)
            views.setViewVisibility(R.id.widget_rule, View.GONE)
            views.setViewVisibility(R.id.widget_kidase, View.GONE)
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            views.setTextViewText(R.id.widget_empty, page.emptyText)
        } else {
            views.setViewVisibility(R.id.widget_empty, View.GONE)
            views.setViewVisibility(R.id.widget_rule, View.VISIBLE)
            bindRow(views, page.rows.getOrNull(0), R.id.widget_row_1, R.id.widget_role_1, R.id.widget_ref_1)
            bindRow(views, page.rows.getOrNull(1), R.id.widget_row_2, R.id.widget_role_2, R.id.widget_ref_2)
            // The day's ቅዳሴ, one quiet line — a continuation, not a third row.
            if (page.kidase == null) {
                views.setViewVisibility(R.id.widget_kidase, View.GONE)
            } else {
                views.setViewVisibility(R.id.widget_kidase, View.VISIBLE)
                views.setTextViewText(R.id.widget_kidase, "ቅዳሴ · ${page.kidase}")
            }
        }
        // The primary action line — the whole card opens the app, this names it.
        if (page.hasContent) {
            views.setViewVisibility(R.id.widget_cta, View.VISIBLE)
            views.setTextViewText(R.id.widget_cta, page.ctaText)
        } else {
            views.setViewVisibility(R.id.widget_cta, View.GONE)
        }
        views.setViewVisibility(R.id.widget_dots, View.GONE)
        // Fills the provider's PendingIntent template: any card opens the app's
        // ግጻዌ screen (which has its own change-day control for going further).
        views.setOnClickFillInIntent(
            R.id.widget_page_root,
            Intent()
                .putExtra(GitsaweReminderScheduler.EXTRA_OPEN_GITSAWE, true)
                .putExtra(GitsaweWidgetProvider.EXTRA_GITSAWE_EPOCH_DAY, page.epochDay),
        )
        return views
    }

    private fun bindRow(
        views: RemoteViews,
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
            ?: reading.citation
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

    override fun getLoadingView(): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_gitsawe_page)

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
