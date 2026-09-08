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
import android.view.View
import android.widget.RemoteViews
import com.agpeya.app.MainActivity
import com.agpeya.app.R
import com.agpeya.app.data.FastingCalendar
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.GitsaweReading
import com.agpeya.app.model.GitsaweService
import com.agpeya.app.model.VerseRef
import com.agpeya.app.reminders.GitsaweReminderScheduler
import com.agpeya.app.stringsFor
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.reading.geezNumeral
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * የዕለቱ ግጻዌ — a home-screen widget showing today's ምስባክ and ወንጌል at a glance.
 * Tapping it opens the ግጻዌ screen.
 *
 * The provider renders the single card itself (no collection service): content
 * is read on a background thread via [BroadcastReceiver.goAsync], and the number
 * of readings — one to five — is chosen from the height the launcher grants, so
 * a tall card is worth making tall and a short one still says something. It
 * refreshes on date/time/locale changes and re-arms a just-past-midnight
 * alarm so the card never shows yesterday's readings.
 */
class GitsaweWidgetProvider : AppWidgetProvider() {

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

    /** The last widget left the home screen: the daily refresh has nothing to do. */
    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(dayRefreshIntent(context))
        super.onDisabled(context)
    }

    /** Read content and re-render off the main thread; the receiver stays alive for it. */
    private fun refreshAsync(context: Context, ids: IntArray) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        Thread {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val card = runCatching { buildCard(context) }.getOrNull()
                ids.forEach { id ->
                    manager.updateAppWidget(id, render(context, manager, id, card))
                }
                scheduleDayRefresh(context)
            } finally {
                pending.finish()
            }
        }.start()
    }

    /** Today's content, fully resolved. Null on a failed load — the card falls back. */
    private data class Card(
        val header: String,
        val title: String,
        /** Every reading the day has, in liturgical order. Trimmed at render. */
        val rows: List<Pair<String, String>>,
        /** What kind of day it is: the anaphora, the fast, or both. */
        val status: String?,
        val emptyText: String,
        val hasContent: Boolean,
        val epochDay: Long,
    )

    private fun buildCard(context: Context): Card = runBlocking {
        val s = stringsFor(SettingsRepository.language(context).first())
        val date = LocalDate.now()
        val readings = GitsaweRepository.readingsFor(context, date)
        // Prefer the ቅዳሴ (liturgy) service, falling back to ነግህ (matins).
        val entry = readings.daily
        val service = entry?.kidassie ?: entry?.negh
        val realTitle = entry?.title?.takeIf { it.isNotBlank() }
            ?: readings.feasts.firstOrNull()?.amharicName
        val rows = service?.let { allRows(it) }.orEmpty()
        Card(
            // The date now owns its line, so it can be the whole date: it used
            // to share a row with the wordmark and get ellipsized on a 4×2.
            header = formatEthiopian(date, s),
            title = realTitle ?: s.gitsaweTitle,
            rows = rows,
            status = statusLine(s, date, service),
            emptyText = s.noGitsaweToday,
            hasContent = rows.isNotEmpty() || realTitle != null,
            epochDay = date.toEpochDay(),
        )
    }

    private fun render(context: Context, manager: AppWidgetManager, id: Int, card: Card?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_gitsawe)
        // How much fits: the launcher reports granted size in dp via the options.
        val options = manager.getAppWidgetOptions(id)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
        // Readings the height pays for. The 4×2 default is ~110dp, and the two
        // it buys there are the two the widget has always promised; every step
        // up adds a reading rather than an ornament. The old thresholds asked
        // for 128dp before showing a footer the default size could never grant,
        // so two of the card's tiers were unreachable as shipped.
        val maxRows = when {
            minHeight >= 190 -> 5
            minHeight >= 162 -> 4
            minHeight >= 134 -> 3
            minHeight >= 100 -> 2
            minHeight >= 76 -> 1
            else -> 0
        }
        val showStatus = minHeight >= 162
        // A narrow card gives the reference the whole width instead.
        val showRoles = minWidth >= 180

        if (card == null) {
            // Load failure: name the app, invite the tap — never a blank card.
            views.setTextViewText(R.id.widget_date, "")
            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_gitsawe_label))
            views.setViewVisibility(R.id.widget_rule, View.GONE)
            ROW_IDS.forEach { (rowId, _) -> views.setViewVisibility(rowId, View.GONE) }
            views.setViewVisibility(R.id.widget_empty, View.GONE)
            views.setViewVisibility(R.id.widget_status_rule, View.GONE)
            views.setViewVisibility(R.id.widget_status, View.GONE)
            views.setOnClickPendingIntent(R.id.widget_root, openGitsawe(context, LocalDate.now().toEpochDay()))
            return views
        }

        views.setTextViewText(R.id.widget_date, card.header)
        views.setTextViewText(R.id.widget_title, card.title)

        val shown = trimToFit(card.rows, maxRows)
        val hasRoom = maxRows > 0
        if (shown.isEmpty()) {
            ROW_IDS.forEach { (rowId, _) -> views.setViewVisibility(rowId, View.GONE) }
            views.setViewVisibility(R.id.widget_rule, if (hasRoom) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_empty, if (hasRoom) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_empty, card.emptyText)
        } else {
            views.setViewVisibility(R.id.widget_empty, View.GONE)
            views.setViewVisibility(R.id.widget_rule, View.VISIBLE)
            ROW_IDS.forEachIndexed { i, (rowId, ids) ->
                bindRow(views, shown.getOrNull(i), showRoles, rowId, ids.first, ids.second)
            }
        }

        // The day itself — anaphora and fast — under its own hairline.
        val status = card.status?.takeIf { showStatus && shown.isNotEmpty() }
        if (status == null) {
            views.setViewVisibility(R.id.widget_status_rule, View.GONE)
            views.setViewVisibility(R.id.widget_status, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_status_rule, View.VISIBLE)
            views.setViewVisibility(R.id.widget_status, View.VISIBLE)
            views.setTextViewText(R.id.widget_status, status)
        }
        views.setOnClickPendingIntent(R.id.widget_root, openGitsawe(context, card.epochDay))
        return views
    }

    private fun bindRow(
        views: RemoteViews,
        row: Pair<String, String>?,
        showRole: Boolean,
        rowId: Int,
        roleId: Int,
        refId: Int,
    ) {
        if (row == null) {
            views.setViewVisibility(rowId, View.GONE)
            return
        }
        views.setViewVisibility(rowId, View.VISIBLE)
        views.setViewVisibility(roleId, if (showRole) View.VISIBLE else View.GONE)
        views.setTextViewText(roleId, row.first)
        views.setTextViewText(refId, row.second)
    }

    /** Every reading the service carries, in the order the liturgy reads them. */
    private fun allRows(service: GitsaweService): List<Pair<String, String>> =
        ROLES.mapNotNull { (label, pick) ->
            pick(service).firstOrNull()?.let { label to summarize(it) }
                ?.takeIf { it.second.isNotBlank() }
        }

    /**
     * The [max] most useful readings, still in liturgical order.
     *
     * Which ones go when there is no room is a different question from which
     * order they are read in: the ወንጌል is the one reading worth a single line,
     * and the ምስባክ the second — those two are what a glance is for, and what
     * the widget's own picker description promises. The epistles fill a taller
     * card. Dropping is by [ROLE_PRIORITY]; what survives is re-sorted back.
     */
    private fun trimToFit(rows: List<Pair<String, String>>, max: Int): List<Pair<String, String>> {
        if (max <= 0) return emptyList()
        if (rows.size <= max) return rows
        val keep = rows.sortedBy { ROLE_PRIORITY.indexOf(it.first) }.take(max).toSet()
        return rows.filter { it in keep }
    }

    /**
     * What kind of day it is, in one line: the anaphora sung at the ቅዳሴ, and
     * the fast in effect. Both are things a person wants before deciding
     * anything about the day, and neither reached the widget before.
     */
    private fun statusLine(s: Strings, date: LocalDate, service: GitsaweService?): String? {
        val anaphora = service?.kidassie?.firstOrNull { it.isNotBlank() }?.trim()
        val fast = FastingCalendar.fastOn(date)?.let { if (s.isAmharic) it.nameAm else it.nameEn }
            ?: s.fastingWeekly.takeIf { FastingCalendar.isWeeklyFastDay(date) }
        return listOfNotNull(anaphora?.let { "ቅዳሴ $it" }, fast)
            .joinToString("  ·  ")
            .ifBlank { null }
    }

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

    private fun openGitsawe(context: Context, epochDay: Long): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(GitsaweReminderScheduler.EXTRA_OPEN_GITSAWE, true)
            putExtra(EXTRA_GITSAWE_EPOCH_DAY, epochDay)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Refresh just past midnight, when today's readings change. */
    @SuppressLint("MissingPermission") // Guarded by canScheduleExactAlarms; otherwise uses an inexact alarm.
    private fun scheduleDayRefresh(context: Context) {
        val next = LocalDate.now().plusDays(1).atStartOfDay().plusMinutes(1)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = dayRefreshIntent(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        } else {
            // A user can revoke exact-alarm access. The inexact fallback still
            // rolls the card forward without making widget refresh fragile.
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        }
    }

    private fun dayRefreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        DAY_REFRESH_REQUEST_CODE,
        Intent(context, GitsaweWidgetProvider::class.java).setAction(ACTION_DAY_REFRESH),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val EXTRA_GITSAWE_EPOCH_DAY = "gitsaweWidgetEpochDay"
        /** Distinct from the reminder PendingIntents (0-3, 5-11) — see the audit. */
        private const val REQUEST_CODE = 3
        private const val DAY_REFRESH_REQUEST_CODE = 7
        // Value kept from the pre-1.5 evening refresh so an already-armed alarm
        // from before the update still routes here.
        private const val ACTION_DAY_REFRESH = "com.agpeya.app.widget.EVENING_REFRESH"

        private val REFRESH_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCALE_CHANGED,
            ACTION_DAY_REFRESH,
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

/**
 * The five readings of a ቅዳሴ, in the order the liturgy reads them and under the
 * names the text goes by rather than whose turn it is to read it — the same
 * list, and the same reasoning, as `ROLE_LABELS` on the ግጻዌ screen. ነግህ and
 * ሠርክ carry only the psalm and the Gospel; the roles they lack contribute
 * nothing. The names stay Amharic in every language, as they do on the screen.
 */
private val ROLES: List<Pair<String, (GitsaweService) -> List<GitsaweReading>>> = listOf(
    "ጳውሎስ" to { s: GitsaweService -> s.firstDeacon },
    "ሐዋርያ" to { s: GitsaweService -> s.secondDeacon },
    "ግብረ ሐዋርያት" to { s: GitsaweService -> s.secondKahn },
    "ምስባክ" to { s: GitsaweService -> s.msbak },
    "ወንጌል" to { s: GitsaweService -> s.wengel },
)

/** Which readings survive a short card, most worth keeping first. */
private val ROLE_PRIORITY = listOf("ወንጌል", "ምስባክ", "ጳውሎስ", "ሐዋርያ", "ግብረ ሐዋርያት")

/** row → (role, reference), in the layout's own order. */
private val ROW_IDS = listOf(
    R.id.widget_row_1 to (R.id.widget_role_1 to R.id.widget_ref_1),
    R.id.widget_row_2 to (R.id.widget_role_2 to R.id.widget_ref_2),
    R.id.widget_row_3 to (R.id.widget_role_3 to R.id.widget_ref_3),
    R.id.widget_row_4 to (R.id.widget_role_4 to R.id.widget_ref_4),
    R.id.widget_row_5 to (R.id.widget_role_5 to R.id.widget_ref_5),
)
