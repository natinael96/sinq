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
    data class Summary(val days: Int, val bookmarks: Int, val highlights: Int)

    /** Read and validate a backup without applying it. */
    suspend fun peek(context: Context, uri: Uri): Summary? = withContext(Dispatchers.IO) {
        parse(context, uri)?.let {
            Summary(it.habits.records.size, it.bookmarks.size, it.highlights.size)
        }
    }

    /**
     * Restore a backup, merging rather than replacing: a day marked done in
     * either the file or the device stays done, and existing bookmarks and
     * highlights survive. Restoring an old backup can therefore never erase
     * progress made since it was taken.
     */
    suspend fun restore(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val backup = parse(context, uri) ?: return@withContext false
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
