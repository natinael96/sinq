package com.agpeya.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import com.agpeya.app.reminders.ReadingReminderScheduler
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Everything that will actually reach you on a given day, in clock order.
 *
 * Six notification channels and two lists of custom reminders arm themselves
 * independently, each behind its own switch on its own row, and no screen has
 * ever shown the result. Quiet hours can cover a reminder slot, so this view
 * shows which configured notifications will stay silent.
 *
 * This derives the day rather than storing it: nothing here arms, cancels or
 * changes an alarm, so it can be wrong about the future only in the way the
 * schedule itself can — a mode switched later, a plan started tomorrow.
 */
object DaySchedule {

    enum class Kind { HOUR, GITSAWE, READING, NIGHTLY, BREATH, GIVING }

    /**
     * One thing that will fire. [minute] is null for the breath prayer, which
     * picks its own moment inside a window rather than keeping an appointment.
     */
    data class Entry(
        val kind: Kind,
        val label: String,
        val minute: Int?,
        /** True when quiet hours cover it, so it is armed but will stay silent. */
        val silenced: Boolean = false,
        /** False for something armed that today's cadence does not call for. */
        val today: Boolean = true,
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observe(context: Context, date: LocalDate): Flow<List<Entry>> {
        val changes: List<Flow<Any>> = listOf(
            SettingsRepository.quietHours(context), SettingsRepository.streakReminder(context),
            SettingsRepository.streakReminderTime(context), SettingsRepository.gitsaweReminder(context),
            SettingsRepository.gitsaweReminderTime(context),
            SettingsRepository.breathReminder(context), SettingsRepository.readingReminder(context),
            SettingsRepository.almsReminders(context), SettingsRepository.repentanceReminders(context),
            SettingsRepository.titheReminders(context), OfferingRepository.vows(context),
            PenanceRepository.penances(context), ReadingPlanRepository.state(context),
            ModesRepository.state(context), HoursRepository.config(context),
        )
        return combine(changes) { Unit }.mapLatest { forDay(context, date) }
    }

    suspend fun forDay(context: Context, date: LocalDate = LocalDate.now()): List<Entry> {
        val quiet = SettingsRepository.quietHours(context).first()
        fun silenced(minute: Int) = quiet.enabled && quiet.covers(minute)

        val out = mutableListOf<Entry>()

        // The hours, from the active mode. Entries are off until enabled, so an
        // untouched install shows none of them — which is the truth.
        val modes = runCatching { ModesRepository.current(context) }.getOrNull()
        val names = runCatching { HoursRepository.visibleHours(context).associate { it.id to it.name } }
            .getOrDefault(emptyMap())
        modes?.activeMode?.entries.orEmpty()
            .filter { it.enabled && it.hourId in names }
            .forEach { e ->
                val minute = e.hour * 60 + e.minute
                out += Entry(
                    kind = Kind.HOUR,
                    label = names[e.hourId] ?: e.hourId,
                    minute = minute,
                    silenced = silenced(minute),
                    today = e.days.contains(date.dayOfWeek.isoDayOfWeek()),
                )
            }

        if (SettingsRepository.gitsaweReminder(context).first()) {
            val minute = SettingsRepository.gitsaweReminderTime(context).first()
            out += Entry(Kind.GITSAWE, "", minute, silenced(minute))
        }

        if (SettingsRepository.readingReminder(context).first()) {
            val pending = runCatching {
                ReadingPlanRepository.unreadToday(
                    ReadingPlanRepository.content(context), ReadingPlanRepository.current(context), date,
                ).isNotEmpty()
            }.getOrDefault(false)
            ReadingReminderScheduler.REMINDER_TIMES.forEach { time ->
                val minute = time.hour * 60 + time.minute
                out += Entry(Kind.READING, "", minute, silenced(minute), today = pending)
            }
        }

        if (SettingsRepository.streakReminder(context).first()) {
            val minute = SettingsRepository.streakReminderTime(context).first()
            out += Entry(Kind.NIGHTLY, "", minute, silenced(minute))
        }

        if (SettingsRepository.breathReminder(context).first()) {
            out += Entry(Kind.BREATH, "", null)
        }

        listOf(
            SettingsRepository.almsReminders(context).first(),
            SettingsRepository.repentanceReminders(context).first(),
            SettingsRepository.titheReminders(context).first(),
        ).flatten().filter { it.enabled }.forEach { r ->
            out += Entry(
                kind = Kind.GIVING,
                label = r.label,
                minute = r.minute,
                silenced = silenced(r.minute),
                today = r.schedule.isDueOn(date),
            )
        }

        OfferingRepository.vows(context).first().filter { it.remindsStill }.forEach { vow ->
            out += Entry(Kind.GIVING, vow.label, vow.minute, silenced(vow.minute), vow.schedule.isDueOn(date))
        }
        PenanceRepository.penances(context).first().filter { it.remindsStill }.forEach { penance ->
            // Private labels must not appear in a public schedule summary.
            out += Entry(Kind.GIVING, "", penance.minute, silenced(penance.minute), penance.schedule.isDueOn(date))
        }

        // Clock order, with the breath prayer last: it has no appointment to keep.
        return out.sortedWith(compareBy({ it.minute == null }, { it.minute ?: 0 }))
    }

    /** ISO day-of-week, matching how [com.agpeya.app.model.ReminderEntry] stores days. */
    private fun DayOfWeek.isoDayOfWeek(): Int = value
}
