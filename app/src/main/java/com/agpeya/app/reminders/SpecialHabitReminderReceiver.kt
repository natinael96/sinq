package com.agpeya.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.agpeya.app.MainActivity
import com.agpeya.app.R
import com.agpeya.app.data.OfferingRepository
import com.agpeya.app.data.PenanceRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.Penance
import com.agpeya.app.model.ScheduledReminder
import com.agpeya.app.model.Vow
import com.agpeya.app.stringsFor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * One entry of an intention reminder (ምጽዋት, ንስሐ, አስራት or ስዕለት) fired on its
 * due day. Re-arms THAT entry for its next due day, then — unless it was
 * disabled, removed, or (for a one-time vow) already kept meanwhile — posts a
 * notification.
 *
 * There is still no "Done" action here, for any of the four. ምጽዋት and ንስሐ are
 * not the app's to know at all; አስራት and ስዕለት ARE recorded, but recording an
 * amount is not something to guess from a notification tap, so the tap opens
 * the page where the real figure can be entered. The entry's label, when set,
 * rides in the notification title so several reminders read apart in the tray.
 */
class SpecialHabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habit = SpecialHabit.entries.firstOrNull { it.action == intent.action } ?: return
        val entryId = intent.getStringExtra(SpecialHabitReminderScheduler.EXTRA_ENTRY_ID) ?: return
        val pending = goAsync()
        Thread {
            try {
                runBlocking {
                    val list: List<ScheduledReminder> = when (habit) {
                        SpecialHabit.ALMS -> SettingsRepository.almsReminders(context).first()
                        SpecialHabit.REPENTANCE -> SettingsRepository.repentanceReminders(context).first()
                        SpecialHabit.TITHE -> SettingsRepository.titheReminders(context).first()
                        SpecialHabit.VOW -> OfferingRepository.vows(context).first()
                        SpecialHabit.PENANCE -> PenanceRepository.penances(context).first()
                    }
                    val entry = list.firstOrNull { it.id == entryId } ?: return@runBlocking
                    // A one-time ስዕለት that has been kept stops here: no
                    // notification, and no re-arming to ask again next year.
                    // A finished ቀኖና likewise.
                    if (entry is Vow && !entry.remindsStill) return@runBlocking
                    if (entry is Penance && !entry.remindsStill) return@runBlocking
                    if (!entry.enabled) return@runBlocking
                    // Chain: always re-arm this entry for its next due day,
                    // then fall silent if the quiet window covers right now.
                    SpecialHabitReminderScheduler.scheduleNext(context, habit, entry)
                    if (SettingsRepository.inQuietHoursNow(context)) return@runBlocking

                    val s = stringsFor(SettingsRepository.language(context).first())
                    val (defaultTitle, defaultBody, channelName) = when (habit) {
                        SpecialHabit.ALMS ->
                            Triple(s.almsReminderTitle, s.almsReminderBody, s.almsChannelName)
                        SpecialHabit.REPENTANCE ->
                            Triple(s.repentReminderTitle, s.repentReminderBody, s.repentChannelName)
                        SpecialHabit.TITHE ->
                            Triple(s.titheReminderTitle, s.titheReminderBody, s.titheChannelName)
                        SpecialHabit.VOW ->
                            Triple(s.vowReminderTitle, s.vowReminderBody, s.vowChannelName)
                        SpecialHabit.PENANCE ->
                            Triple(s.penanceReminderTitle, s.penanceReminderBody, s.penanceChannelName)
                    }
                    // A vow that named an amount says what is still owed on it;
                    // one that promised prayer or fasting has no figure to give.
                    val body = if (entry is Vow && entry.remaining > 0) {
                        val currency = OfferingRepository.currency(context).first()
                        s.vowReminderOwing(formatAmount(entry.remaining, currency.ifBlank { s.currencyDefault }))
                    } else {
                        defaultBody
                    }
                    // A ቀኖና is confessional: its label never rides in the tray,
                    // where a lock screen would show it to anyone who glances.
                    val title = if (habit == SpecialHabit.PENANCE) defaultTitle
                    else entry.label.ifBlank { defaultTitle }
                    ensureChannel(context, habit, channelName)
                    // Tap request code and notification id both derive from the
                    // entry id, so entries never overwrite each other's tray
                    // notification or share a PendingIntent.
                    val tapCode = "${habit.action}:tap:$entryId".hashCode()
                    val tap = PendingIntent.getActivity(
                        context,
                        tapCode,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            // አስራት, ስዕለት and ቀኖና land on their own page, where
                            // the record can actually be made; ምጽዋት and ንስሐ
                            // have nothing to record and just open the app.
                            if (habit == SpecialHabit.TITHE || habit == SpecialHabit.VOW ||
                                habit == SpecialHabit.PENANCE
                            ) {
                                putExtra(MainActivity.EXTRA_OPEN_OFFERING, habit.name)
                            }
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                    val notification = NotificationCompat.Builder(context, habit.channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(tap)
                        .build()
                    context.getSystemService(NotificationManager::class.java)
                        .notify("${habit.action}:$entryId".hashCode(), notification)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    /** "1,500.00 ብር" — the same shape the ledger shows. */
    private fun formatAmount(cents: Long, currency: String): String =
        "%,.2f %s".format(cents / 100.0, currency).trim()

    private fun ensureChannel(context: Context, habit: SpecialHabit, name: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        // Channels only exist from Oreo; below it there is nothing to create.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            nm.getNotificationChannel(habit.channelId) == null
        ) {
            nm.createNotificationChannel(
                NotificationChannel(habit.channelId, name, NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }
}
