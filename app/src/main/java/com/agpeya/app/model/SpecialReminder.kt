package com.agpeya.app.model

import kotlinx.serialization.Serializable

/**
 * One entry in a scheduled intention's list (ምጽዋት or ንስሐ). Where the app used
 * to hold a single alms reminder and a single repentance reminder, it now holds
 * a list of each: a person can keep "ለቤተ ክርስቲያን" every Sunday and "ለነዳያን" every
 * other day as separate nudges, each with its own cadence and time.
 *
 * Still deliberately NOT a habit: nothing here is recorded, streaked, or shown
 * as done. The [label] is only for the person's own recognition and rides along
 * in the notification title; blank is fine and falls back to the generic title.
 *
 * [id] is stable for the entry's life so its alarm's PendingIntent (keyed on the
 * id) can be re-armed and cancelled without colliding with its siblings.
 */
@Serializable
data class SpecialReminder(
    val id: String,
    val label: String = "",
    val schedule: HabitSchedule = HabitSchedule(),
    val minute: Int = 9 * 60,
    val enabled: Boolean = true,
)
