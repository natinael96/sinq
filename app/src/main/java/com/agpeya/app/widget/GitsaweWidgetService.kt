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

/**
 * Feeds the widget's StackView its two cards: today's ግጻዌ and tomorrow's —
 * only tomorrow's, deliberately; the widget is for the day at hand and for
 * preparing, not a calendar to page through.
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
    )

    private var pages: List<Page> = emptyList()

    override fun onCreate() = Unit

    override fun onDestroy() = Unit

    // Called on a binder thread — blocking reads are fine (and expected) here.
    override fun onDataSetChanged() {
        pages = runCatching { buildPages() }.getOrDefault(emptyList())
    }

    private fun buildPages(): List<Page> = runBlocking {
        val s = stringsFor(SettingsRepository.language(context).first())
        val today = LocalDate.now()
        listOf(
            pageFor(today, "${s.gitsaweTitle}  ·  ${formatEthiopian(today, s)}", s),
            pageFor(
                today.plusDays(1),
                "${s.tomorrowLabel}  ·  ${formatEthiopian(today.plusDays(1), s)}",
                s,
            ),
        )
    }

    private suspend fun pageFor(date: LocalDate, header: String, s: com.agpeya.app.ui.strings.Strings): Page {
        val readings = GitsaweRepository.readingsFor(context, date)
        // Prefer the ቅዳሴ (liturgy) service, falling back to ነግህ (matins).
        val entry = readings.daily
        val service = entry?.kidassie ?: entry?.negh
        val title = entry?.title?.takeIf { it.isNotBlank() }
            ?: readings.feasts.firstOrNull()?.amharicName
            ?: s.gitsaweTitle
        return Page(
            header = header,
            title = title,
            rows = service?.let { pickRows(it) }.orEmpty(),
            kidase = service?.kidassie?.firstOrNull { it.isNotBlank() }?.trim(),
            emptyText = s.noGitsaweToday,
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
        // Fills the provider's PendingIntent template: any card opens the app's
        // ግጻዌ screen (which has its own change-day control for going further).
        views.setOnClickFillInIntent(
            R.id.widget_page_root,
            Intent().putExtra(GitsaweReminderScheduler.EXTRA_OPEN_GITSAWE, true),
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
