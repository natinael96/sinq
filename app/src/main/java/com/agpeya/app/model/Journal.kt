package com.agpeya.app.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * What a journal entry is for. The kind is not a category the app reasons
 * about — it decides how the entry is *treated*, which is a different thing.
 */
enum class JournalKind {
    /** An ordinary dated reflection. */
    REFLECTION,

    /** Written from a psalm, a ግጻዌ reading, or a ስንክሳር day, and links back to it. */
    PASSAGE,

    /**
     * Preparation for ንስሐ. Built to be destroyed: the "confessed" action
     * deletes it outright rather than archiving it, because absolution means
     * the record is gone.
     *
     * A draft never leaves the device — not in an export, not encrypted, not
     * at all. Everything else in the journal can be carried to a new phone; a
     * confession draft is the one thing whose only correct destination is
     * deletion.
     */
    CONFESSION_DRAFT,
}

/**
 * The Church's day, captured when the entry was written.
 *
 * This is the whole reason a journal belongs in *this* app rather than any
 * notes app: three lines written on a hard evening read back years later as
 * ጾመ ፍልሰታ, ቀን ፲፪, ቅዱስ ሚካኤል. It is snapshotted rather than resolved on
 * demand, so re-reading an old entry never depends on content that may have
 * been re-extracted, corrected, or renumbered since it was written.
 *
 * Everything but the date is nullable: the bundled ግጻዌ covers 301 of ~365
 * days, and an entry written on an uncovered day is not a broken entry.
 */
@Serializable
data class DayContext(
    val ethYear: Int,
    val ethMonth: Int,
    val ethDay: Int,
    /** The ወርኀዊ commemoration of this day of the month, e.g. ቅዱስ ሚካኤል. */
    val monthlyFeast: String? = null,
    /** A fast in effect, by its Amharic name. */
    val fast: String? = null,
    /** The day's ግጻዌ heading, if the lectionary covers it. */
    val gitsawe: String? = null,
)

/**
 * One journal entry.
 *
 * Stored in SQLite (via Room) rather than the JSON-blob-in-DataStore pattern
 * the rest of the app uses, because a journal is the one thing here that grows
 * without bound: re-serialising years of long-form text on every keystroke-save
 * would be the wrong shape entirely.
 *
 * The body is plaintext on disk. That is a deliberate, recorded decision — the
 * journal is gated behind a passphrase and relies on Android's file-based
 * encryption for the file itself; see [com.agpeya.app.data.JournalLock].
 */
@Serializable
@Entity(
    tableName = "journal_entries",
    indices = [Index("date"), Index("kind")],
)
data class JournalEntry(
    @PrimaryKey val id: String,
    /** ISO-8601 Gregorian; every surface renders it in the ግእዝ calendar. */
    val date: String,
    val kind: JournalKind = JournalKind.REFLECTION,
    val body: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    @Embedded(prefix = "ctx_") val context: DayContext,
    /**
     * Where the entry was written from, as a nav route the app can replay —
     * the same strings bookmarks already store. Null for a free reflection.
     */
    val anchorRoute: String? = null,
    val anchorLabel: String? = null,
) {
    val localDate: LocalDate? get() = runCatching { LocalDate.parse(date) }.getOrNull()

    /** A draft is discharged by confessing it, not by keeping it. */
    val isDraft: Boolean get() = kind == JournalKind.CONFESSION_DRAFT

    /** ንስሐ drafts are never written to an export, encrypted or otherwise. */
    val exportable: Boolean get() = kind != JournalKind.CONFESSION_DRAFT

    /**
     * The first line, for a list row. Falls back to nothing rather than to a
     * placeholder — an untitled entry is a normal thing, not an error.
     */
    val preview: String
        get() = body.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
}
