package com.agpeya.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.agpeya.app.model.JournalEntry
import com.agpeya.app.model.JournalKind
import kotlinx.coroutines.flow.Flow

/**
 * The journal's SQLite store.
 *
 * This is the only part of Sinq that uses a database. Everything else the app
 * keeps — habits, bookmarks, the አስራት ledger — is a bounded JSON document in a
 * Preferences DataStore, which is the right shape for those: small, read whole,
 * written whole. A journal is not that. It grows without bound, is read a day
 * at a time, and is written on every save, so the JSON-blob pattern would mean
 * re-serialising years of prose to append a sentence.
 *
 * There is deliberately no full-text index. The journal is browsed by day, not
 * queried — see [JournalDao].
 */
@Database(entities = [JournalEntry::class], version = 1, exportSchema = true)
@TypeConverters(JournalConverters::class)
abstract class JournalDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao

    companion object {
        private const val NAME = "journal"

        @Volatile
        private var instance: JournalDatabase? = null

        fun get(context: Context): JournalDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    NAME,
                ).build().also { instance = it }
            }
    }
}

class JournalConverters {
    /**
     * The kind is stored by NAME, not ordinal, so reordering or inserting a
     * kind later cannot silently reclassify existing entries — turning someone's
     * reflection into a confession draft would be a nasty way to learn that
     * lesson. An unrecognised name reads back as a plain reflection.
     */
    @TypeConverter
    fun toKind(value: String): JournalKind =
        runCatching { JournalKind.valueOf(value) }.getOrDefault(JournalKind.REFLECTION)

    @TypeConverter
    fun fromKind(kind: JournalKind): String = kind.name
}

/**
 * Reading the journal is always a browse: this day, this Ethiopian month, or —
 * only for an export — all of it. There is no search, by choice: a journal one
 * can query is a journal one edits for the record rather than writes honestly.
 */
@Dao
interface JournalDao {

    /** Everything written on one day, newest first within the day. */
    @Query("SELECT * FROM journal_entries WHERE date = :date ORDER BY createdAt DESC")
    fun onDate(date: String): Flow<List<JournalEntry>>

    /**
     * One Ethiopian month, for the month view. Matched on the stored ግእዝ parts
     * rather than a Gregorian date range, so a month never loses its edges to
     * the two calendars disagreeing about where it starts.
     */
    @Query(
        """
        SELECT * FROM journal_entries
        WHERE ctx_ethYear = :year AND ctx_ethMonth = :month
        ORDER BY date DESC, createdAt DESC
        """,
    )
    fun inEthiopianMonth(year: Int, month: Int): Flow<List<JournalEntry>>

    /** The days in a month that carry anything, so the month view can mark them. */
    @Query(
        """
        SELECT DISTINCT ctx_ethDay FROM journal_entries
        WHERE ctx_ethYear = :year AND ctx_ethMonth = :month
        """,
    )
    fun writtenDaysIn(year: Int, month: Int): Flow<List<Int>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun byId(id: String): JournalEntry?

    /** Every entry, for the export only. Never bound to a screen. */
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    suspend fun all(): List<JournalEntry>

    @Query("SELECT COUNT(*) FROM journal_entries")
    fun count(): Flow<Int>

    /** How many ንስሐ drafts wait unconfessed — a number, never their text. */
    @Query("SELECT COUNT(*) FROM journal_entries WHERE kind = 'CONFESSION_DRAFT'")
    fun draftCount(): Flow<Int>

    @Upsert
    suspend fun upsert(entry: JournalEntry)

    @Upsert
    suspend fun upsertAll(entries: List<JournalEntry>)

    @Delete
    suspend fun delete(entry: JournalEntry)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Discharge every ንስሐ draft at once — the "confessed" action. A hard
     * delete, not a flag: an archived confession is not a confession.
     */
    @Query("DELETE FROM journal_entries WHERE kind = 'CONFESSION_DRAFT'")
    suspend fun deleteAllDrafts()
}
