package com.agpeya.app.data

import android.content.Context
import android.net.Uri
import com.agpeya.app.model.Bookmark
import com.agpeya.app.model.HabitsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Export and restore the things the user builds up and can't get back: the
 * streak/habit history, bookmarks, and highlights.
 *
 * The app is offline by design, so a backup is a file the user writes wherever
 * they like through the system file picker — nothing is uploaded anywhere.
 */
object BackupRepository {

    /** Bumped only if the shape changes incompatibly; readers tolerate older files. */
    private const val VERSION = 1

    @Serializable
    data class Backup(
        val version: Int = VERSION,
        /** Whatever the exporting app called itself, for a human reading the file. */
        val app: String = "Sinq",
        /** ISO date the backup was taken. */
        val created: String = "",
        val habits: HabitsState = HabitsState(),
        val bookmarks: List<Bookmark> = emptyList(),
        val highlights: Map<String, String> = emptyMap(),
    )

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    /** Everything worth keeping, as a JSON document. */
    suspend fun export(context: Context, today: String): String = withContext(Dispatchers.IO) {
        val backup = Backup(
            created = today,
            habits = HabitsRepository.current(context),
            bookmarks = UserDataRepository.bookmarks(context).first(),
            highlights = HighlightRepository.highlights(context).first(),
        )
        json.encodeToString(Backup.serializer(), backup)
    }

    /** Write the backup to a picker-provided [uri]. */
    suspend fun writeTo(context: Context, uri: Uri, today: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = export(context, today)
                context.contentResolver.openOutputStream(uri)?.use { it.write(body.toByteArray()) }
                    ?: return@runCatching false
                true
            }.getOrDefault(false)
        }

    /** What a restore would change, so the user can be told before it happens. */
    data class Summary(
        val version: Int,
        val created: String,
        val days: Int,
        val bookmarks: Int,
        val highlights: Int,
        /** Days present in the file that the device does not already have. */
        val newDays: Int,
        /** Bookmarks in the file that the device does not already have. */
        val newBookmarks: Int,
    )

    /**
     * Read and validate a backup without applying it, reporting both what the
     * file holds and how much of it is actually new here — a restore that would
     * change nothing is worth saying so before the user commits to it.
     *
     * Returns null when the file is unreadable, is not a Sinq backup, or was
     * written by a newer format than this build understands.
     */
    suspend fun peek(context: Context, uri: Uri): Summary? = withContext(Dispatchers.IO) {
        val b = parse(context, uri) ?: return@withContext null
        if (b.version < 1 || b.version > VERSION) return@withContext null

        val localHabits = HabitsRepository.current(context)
        val localBookmarks = UserDataRepository.bookmarks(context).first()
        val have = localBookmarks.mapTo(mutableSetOf()) { it.hourId to it.sectionId }

        Summary(
            version = b.version,
            created = b.created,
            days = b.habits.records.size,
            bookmarks = b.bookmarks.size,
            highlights = b.highlights.size,
            newDays = b.habits.records.keys.count { day ->
                (b.habits.records[day] ?: emptySet()) - (localHabits.records[day] ?: emptySet()) != emptySet<String>()
            },
            newBookmarks = b.bookmarks.count { (it.hourId to it.sectionId) !in have },
        )
    }

    /**
     * Restore a backup, merging rather than replacing: a day marked done in
     * either the file or the device stays done, and existing bookmarks and
     * highlights survive. Restoring an old backup can therefore never erase
     * progress made since it was taken.
     */
    suspend fun restore(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val backup = parse(context, uri) ?: return@withContext false
        // Refuse a file from a newer format rather than half-applying it.
        if (backup.version < 1 || backup.version > VERSION) return@withContext false
        runCatching {
            HabitsRepository.merge(context, backup.habits)
            UserDataRepository.mergeBookmarks(context, backup.bookmarks)
            HighlightRepository.merge(context, backup.highlights)
            true
        }.getOrDefault(false)
    }

    private fun parse(context: Context, uri: Uri): Backup? = runCatching {
        val raw = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().decodeToString()
        } ?: return null
        json.decodeFromString(Backup.serializer(), raw)
    }.getOrNull()
}
