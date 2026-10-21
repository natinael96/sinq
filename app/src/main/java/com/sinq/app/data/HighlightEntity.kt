package com.sinq.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Verse-level highlight entity in SQLite Room database.
 * Keyed by the deterministic content-addressed contract:
 * "scripture:<edition>:<book>:<chapter>:<verse>" or "<edition>:<sectionId>:<verseNumber>".
 */
@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey val verseKey: String,
    val colorToken: String,
    val timestamp: Long = System.currentTimeMillis(),
)
