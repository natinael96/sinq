package com.agpeya.app.data

import android.content.Context
import com.agpeya.app.reminders.GitsaweReminderScheduler
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Everything that will actually reach you on a given day, in clock order.
 *
 * Six notification channels and two lists of custom reminders arm themselves
 * independently, each behind its own switch on its own row, and no screen has
 * ever shown the result. You could set the reading nudge for 05:00, have quiet
 * hours running to 06:00, and nothing anywhere would say the two disagree.
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
            .filter { it.enabled }
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
            val minute = GitsaweReminderScheduler.REMINDER_TIME.hour * 60 +
                GitsaweReminderScheduler.REMINDER_TIME.minute
            out += Entry(Kind.GITSAWE, "", minute, silenced(minute))
        }

        if (SettingsRepository.readingReminder(context).first()) {
            val minute = SettingsRepository.readingReminderTime(context).first()
            // It stays silent until a plan is begun, so say so rather than
            // promising a notification that will not come.
            val started = runCatching { ReadingPlanRepository.current(context).activePlanId.isNotBlank() }
                .getOrDefault(false)
            out += Entry(Kind.READING, "", minute, silenced(minute), today = started)
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

        // Clock order, with the breath prayer last: it has no appointment to keep.
        return out.sortedWith(compareBy({ it.minute == null }, { it.minute ?: 0 }))
    }

    /** ISO day-of-week, matching how [com.agpeya.app.model.ReminderEntry] stores days. */
    private fun DayOfWeek.isoDayOfWeek(): Int = value
}
