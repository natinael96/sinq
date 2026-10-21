package com.sinq.app.data

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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sinq.app.model.JournalEntry
import com.sinq.app.model.JournalKind
import kotlinx.coroutines.flow.Flow

/**
 * Sinq's primary Room SQLite database.
 * Houses unbounded user records:
 *  - Journal entries
 *  - Highlights (content-addressed)
 *  - Habit / Hour completion logs (Ethiopian calendar keyed)
 */
@Database(
    entities = [
        JournalEntry::class,
        HighlightEntity::class,
        HabitLogEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(JournalConverters::class)
abstract class JournalDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao
    abstract fun highlightDao(): HighlightDao
    abstract fun habitLogDao(): HabitLogDao

    companion object {
        private const val NAME = "journal"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `highlights` (
                        `verseKey` TEXT NOT NULL,
                        `colorToken` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`verseKey`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_logs` (
                        `year` INTEGER NOT NULL,
                        `month` INTEGER NOT NULL,
                        `day` INTEGER NOT NULL,
                        `habitId` TEXT NOT NULL,
                        `completed` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`year`, `month`, `day`, `habitId`)
                    )
                    """.trimIndent(),
                )
            }
        }

        @Volatile
        private var instance: JournalDatabase? = null

        fun get(context: Context): JournalDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    NAME,
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
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

    /** Resume the most recently edited confession note without creating duplicates. */
    @Query("SELECT * FROM journal_entries WHERE kind = 'CONFESSION_DRAFT' ORDER BY updatedAt DESC, createdAt DESC, id DESC LIMIT 1")
    suspend fun latestConfessionDraft(): JournalEntry?

    /** Every entry, for the export only. Never bound to a screen. */
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    suspend fun all(): List<JournalEntry>

    /** Entries written from a passage — the third tab of ምልክቶቼ. */
    @Query(
        """
        SELECT * FROM journal_entries
        WHERE anchorRoute IS NOT NULL AND kind != 'CONFESSION_DRAFT'
        ORDER BY date DESC, createdAt DESC
        """,
    )
    fun fromPassages(): Flow<List<JournalEntry>>

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

    /** Delete all non-confession entries for clean restore. */
    @Query("DELETE FROM journal_entries WHERE kind != 'CONFESSION_DRAFT'")
    suspend fun clearExportable()
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights")
    fun all(): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE verseKey LIKE :prefix || '%'")
    fun forPrefix(prefix: String): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE verseKey = :verseKey")
    suspend fun get(verseKey: String): HighlightEntity?

    @Upsert
    suspend fun upsert(entity: HighlightEntity)

    @Upsert
    suspend fun upsertAll(entities: List<HighlightEntity>)

    @Query("DELETE FROM highlights WHERE verseKey = :verseKey")
    suspend fun delete(verseKey: String)

    @Query("DELETE FROM highlights WHERE verseKey IN (:verseKeys)")
    suspend fun deleteAll(verseKeys: Collection<String>)

    @Query("DELETE FROM highlights")
    suspend fun clear()
}

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs")
    fun all(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE year = :year AND month = :month")
    fun forMonth(year: Int, month: Int): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE year = :year AND month = :month AND day = :day")
    fun forDay(year: Int, month: Int, day: Int): Flow<List<HabitLogEntity>>

    @Upsert
    suspend fun upsert(entity: HabitLogEntity)

    @Upsert
    suspend fun upsertAll(entities: List<HabitLogEntity>)

    @Query("DELETE FROM habit_logs WHERE year = :year AND month = :month AND day = :day AND habitId = :habitId")
    suspend fun delete(year: Int, month: Int, day: Int, habitId: String)

    @Query("DELETE FROM habit_logs")
    suspend fun clear()
}
