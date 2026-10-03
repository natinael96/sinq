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

    /**
     * A day-to-day customizable checklist of spiritual tasks, devotionals,
     * or vows with an optional reflection note.
     */
    CHECKLIST,
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
     * Safe list preview. Confession text is only displayed inside its editor;
     * never return it to list rows or their accessibility semantics.
     */
    val preview: String
        get() = when {
            isDraft -> ""
            kind == JournalKind.CHECKLIST -> {
                val (items, note) = ChecklistParser.parse(body)
                if (items.isNotEmpty()) {
                    val done = items.count { it.isDone }
                    val total = items.size
                    val firstRemaining = items.firstOrNull { !it.isDone }?.text ?: items.first().text
                    "✓ $done/$total · $firstRemaining"
                } else {
                    note.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
                }
            }
            else -> body.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        }
}

/**
 * A single item in a [JournalKind.CHECKLIST] entry.
 */
@Serializable
data class ChecklistItem(
    val text: String,
    val isDone: Boolean = false,
    val scheduledHour: String? = null,
    val hasReminder: Boolean = false,
)

/**
 * Parses and serializes checklist entries from the entry's markdown body.
 *
 * Uses standard markdown task lists (`- [ ] ` and `- [x] `) with optional
 * `@time:...` and `@alarm` tags so entries remain human-readable when backed up,
 * shared, or switched to another view.
 */
object ChecklistParser {
    private val TASK_PATTERN = Regex("""^-\s*\[([ xX])\]\s*(.*)$""")
    private val TIME_TAG_PATTERN = Regex("""@time:([^\s@]+)""")
    private const val ALARM_TAG = "@alarm"

    fun parse(body: String): Pair<List<ChecklistItem>, String> {
        val items = mutableListOf<ChecklistItem>()
        val noteLines = mutableListOf<String>()
        var inNotes = false

        for (line in body.lines()) {
            val trimmed = line.trim()
            if (!inNotes) {
                val match = TASK_PATTERN.matchEntire(trimmed)
                if (match != null) {
                    val isDone = match.groupValues[1].equals("x", ignoreCase = true)
                    var raw = match.groupValues[2].trim()
                    var scheduledHour: String? = null
                    var hasReminder = false

                    if (raw.contains(ALARM_TAG)) {
                        hasReminder = true
                        raw = raw.replace(ALARM_TAG, "").trim()
                    }
                    val timeMatch = TIME_TAG_PATTERN.find(raw)
                    if (timeMatch != null) {
                        scheduledHour = timeMatch.groupValues[1].replace("_", " ")
                        raw = raw.removeRange(timeMatch.range).trim()
                    }

                    if (raw.isNotEmpty()) {
                        items.add(ChecklistItem(raw, isDone, scheduledHour, hasReminder))
                    }
                    continue
                } else if (items.isNotEmpty()) {
                    inNotes = true
                }
            }
            if (inNotes) {
                noteLines.add(line)
            } else if (trimmed.isNotEmpty()) {
                noteLines.add(line)
                inNotes = true
            }
        }
        return items to noteLines.joinToString("\n").trim()
    }

    fun serialize(items: List<ChecklistItem>, note: String): String {
        val sb = StringBuilder()
        for (item in items) {
            val mark = if (item.isDone) "x" else " "
            sb.append("- [$mark] ").append(item.text.trim())
            if (item.scheduledHour != null) {
                sb.append(" @time:").append(item.scheduledHour.replace(" ", "_"))
            }
            if (item.hasReminder) {
                sb.append(" @alarm")
            }
            sb.append("\n")
        }
        val trimmedNote = note.trim()
        if (trimmedNote.isNotEmpty()) {
            if (items.isNotEmpty()) sb.append("\n")
            sb.append(trimmedNote)
        }
        return sb.toString().trim()
    }
}

