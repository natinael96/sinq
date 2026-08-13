package com.agpeya.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.agpeya.app.MainActivity
import com.agpeya.app.R
import com.agpeya.app.reminders.GitsaweReminderScheduler

/**
 * የዕለቱ ግጻዌ — a home-screen widget showing today's ምስባክ and ወንጌል at a glance,
 * with tomorrow's one swipe away (to prepare — only tomorrow, on purpose).
 * Tapping either card opens the ግጻዌ screen.
 *
 * The stack's cards come from [GitsaweWidgetService]'s factory, which reads
 * content on a binder thread; the provider itself only wires the adapter and
 * the click template. The widget renders in the launcher's process, so it uses
 * system text views (no Compose, no bundled fonts). It refreshes when the date
 * rolls over — see the manifest intent filter — rather than on a polling
 * interval.
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
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_gitsawe)
            // The widget id in the data URI keeps each widget's adapter intent
            // distinct — extras alone don't distinguish cached intents.
            val adapter = Intent(context, GitsaweWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_stack, adapter)
            // MUTABLE, unlike every other PendingIntent in the app: a collection
            // template must accept each card's fill-in intent, and an immutable
            // one silently drops it.
            views.setPendingIntentTemplate(R.id.widget_stack, openGitsaweTemplate(context))
            manager.updateAppWidget(id, views)
        }
        // Make the factory re-read both days' content, not just re-lay-out.
        manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_stack)
    }

    private fun openGitsaweTemplate(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_CODE,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(GitsaweReminderScheduler.EXTRA_OPEN_GITSAWE, true)
        },
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        /** Distinct from the reminder PendingIntents (0, 1, 2, 5, 6) — see the audit. */
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
