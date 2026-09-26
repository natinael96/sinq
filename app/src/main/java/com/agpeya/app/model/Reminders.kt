package com.agpeya.app.model

import kotlinx.serialization.Serializable

/**
 * One reminder: a prayer hour, a time, and the weekdays it fires on.
 * [days] uses ISO day numbers (Monday=1 .. Sunday=7); all seven = daily.
 */
@Serializable
data class ReminderEntry(
    val id: String,
    val hourId: String,
    val hour: Int,
    val minute: Int,
    val days: Set<Int> = ALL_DAYS,
    val enabled: Boolean = false,
) {
    companion object {
        val ALL_DAYS: Set<Int> = (1..7).toSet()
    }
}

/**
 * A named prayer schedule. Exactly one mode is active at a time; only the
 * active mode's enabled entries schedule alarms (PLAN.md decision D12).
 * The built-in Agpeya mode cannot be deleted; entries can be re-timed and
 * toggled but not added/removed.
 */
@Serializable
data class PrayerMode(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean = false,
    val entries: List<ReminderEntry> = emptyList(),
)

@Serializable
data class ModesState(
    val activeModeId: String,
    val modes: List<PrayerMode>,
) {
    val activeMode: PrayerMode?
        get() = modes.find { it.id == activeModeId }
}
