package com.sinq.app.data

import androidx.room.Entity

/**
 * Daily habit / canonical hour completion log in SQLite Room database.
 * Keyed by the Ethiopian calendar date and habit / hour ID.
 */
@Entity(
    tableName = "habit_logs",
    primaryKeys = ["year", "month", "day", "habitId"],
)
data class HabitLogEntity(
    val year: Int,
    val month: Int,
    val day: Int,
    val habitId: String,
    val completed: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
)
