package com.agpeya.app.data

import com.agpeya.app.model.Hour
import java.time.LocalTime

/**
 * Where the user is in the day's cycle of hours: which one is current, what sits
 * either side of it, and whether an hour is still unfinished.
 *
 * The hours are read in the order the user sees them (their customized order),
 * so "next" means the next one down their list — not the next by clock time,
 * which would jump around once an hour is hidden or a custom hour is added.
 */
object PrayerSchedule {

    /** The hour whose time-of-day window contains [now], if it's visible. */
    fun currentHourId(hours: List<Hour>, now: LocalTime = LocalTime.now()): String? {
        val suggested = ContentRepository.suggestedHourId(now.hour)
        return hours.firstOrNull { it.id == suggested }?.id
    }

    /** The hour before [hourId] in the user's order, or null at the start. */
    fun previous(hours: List<Hour>, hourId: String): Hour? {
        val i = hours.indexOfFirst { it.id == hourId }
        return if (i > 0) hours[i - 1] else null
    }

    /** The hour after [hourId] in the user's order, or null at the end. */
    fun next(hours: List<Hour>, hourId: String): Hour? {
        val i = hours.indexOfFirst { it.id == hourId }
        return if (i >= 0 && i < hours.lastIndex) hours[i + 1] else null
    }
}
