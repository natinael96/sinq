package com.sinq.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

@Entity(tableName = "corpus_docs")
data class CorpusDocEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val target_id: String,
    val source_label: String,
    val target_index: Int,
    val title: String,
    val route: String,
    val haystack: String,
    val folded: String,
    val verse_starts: String? = null,
    val verse_numbers: String? = null,
)

@Dao
interface CorpusDao {
    @RawQuery(observedEntities = [CorpusDocEntity::class])
    suspend fun queryDocs(query: SupportSQLiteQuery): List<CorpusDocEntity>

    suspend fun searchFts(query: String, limit: Int = 120): List<CorpusDocEntity> =
        queryDocs(
            SimpleSQLiteQuery(
                "SELECT * FROM corpus_docs WHERE id IN (SELECT docid FROM corpus_fts WHERE corpus_fts MATCH ?) LIMIT ?",
                arrayOf(query, limit),
            ),
        )

    @Query("""
        SELECT * FROM corpus_docs
        WHERE folded LIKE '%' || :query || '%'
        LIMIT :limit
    """)
    suspend fun searchLike(query: String, limit: Int = 120): List<CorpusDocEntity>

    @Query("SELECT * FROM corpus_docs WHERE source = :source LIMIT :limit")
    suspend fun forSource(source: String, limit: Int = 100): List<CorpusDocEntity>

    @Query("SELECT count(*) FROM corpus_docs")
    suspend fun count(): Int
}

@Database(entities = [CorpusDocEntity::class], version = 1, exportSchema = false)
abstract class CorpusDatabase : RoomDatabase() {
    abstract fun corpusDao(): CorpusDao

    companion object {
        private const val NAME = "sinq_corpus.db"

        @Volatile
        private var instance: CorpusDatabase? = null

        fun get(context: Context): CorpusDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CorpusDatabase::class.java,
                    NAME,
                )
                .createFromAsset("database/sinq_corpus.db")
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
            }
    }
}
