package com.agpeya.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap

/**
 * The bundled patristic commentary on the New Testament and the Psalter,
 * built by tools/build_fathers.py from the public-domain volumes named in
 * assets/content/fathers/index.json.
 *
 * The Fathers comment on passages, not on single verses, so a book's entries
 * are anchored to wherever its source pinned a lemma and [forVerse] walks back
 * to the nearest anchor at or before the verse asked for. A whole-chapter
 * anchor is written as verse 0, which is how the Psalter is carried: Augustine
 * expounds a psalm as one discourse, and the Ge'ez and Hebrew numberings
 * disagree about which line is verse one, so pinning him to a verse would be
 * false precision.
 */
object FathersRepository {

    private const val TAG = "FathersRepository"
    private const val DIR = "content/fathers"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var indexCache: Index? = null
    private val bookCache = ConcurrentHashMap<String, Book>()

    /** Drop the rebuildable caches under memory pressure (see [CacheTrimmer]). */
    fun trimCaches() {
        bookCache.clear()
    }

    /** One extract, with the work and the Father it is credited to. */
    data class Passage(
        val work: String,
        val author: String,
        val text: String,
    )

    /** What [forVerse] found, and the anchor it actually landed on. */
    data class Commentary(
        val chapter: Int,
        val verse: Int,
        val passages: List<Passage>,
    ) {
        /** True when the anchor covers the whole chapter rather than one verse. */
        val wholeChapter: Boolean get() = verse == 0
    }

    @Serializable
    private data class Work(val title: String = "", val author: String = "")

    @Serializable
    private data class BookMeta(val book: String = "", val file: String = "")

    @Serializable
    private data class Index(
        val schemaVersion: Int = 0,
        val works: List<Work> = emptyList(),
        val authors: List<String> = emptyList(),
        val books: List<BookMeta> = emptyList(),
    )

    /** A book's anchors, sorted so [forVerse] can walk back to the nearest one. */
    private class Book(val anchors: List<Anchor>)

    private class Anchor(
        val chapter: Int,
        val verse: Int,
        val passages: List<Passage>,
    )

    private suspend fun index(context: Context): Index =
        indexCache ?: withContext(Dispatchers.IO) {
            val raw = read(context, "index.json")
            val parsed = raw?.let {
                runCatching { json.decodeFromString<Index>(it) }
                    .onFailure { e -> Log.e(TAG, "Failed to parse the fathers index", e) }
                    .getOrNull()
            } ?: Index()
            indexCache = parsed
            parsed
        }

    /** Whether anything is bundled for this book. Cheap: reads only the index. */
    suspend fun covers(context: Context, book: String): Boolean =
        index(context).books.any { it.book == book }

    /**
     * The commentary nearest to [verse], or null when the book is not covered
     * or nothing in it reaches that far.
     */
    suspend fun forVerse(context: Context, book: String, chapter: Int, verse: Int): Commentary? {
        if (chapter < 1) return null
        val loaded = load(context, book) ?: return null
        // The nearest anchor at or before the verse, then the chapter's own.
        val hit = loaded.anchors.lastOrNull { it.chapter == chapter && it.verse in 1..verse }
            ?: loaded.anchors.firstOrNull { it.chapter == chapter && it.verse == 0 }
            ?: return null
        return Commentary(hit.chapter, hit.verse, hit.passages)
    }

    private suspend fun load(context: Context, book: String): Book? {
        bookCache[book]?.let { return it }
        val meta = index(context).books.find { it.book == book } ?: return null
        val idx = index(context)
        return withContext(Dispatchers.IO) {
            val raw = read(context, meta.file) ?: return@withContext null
            val parsed = runCatching { parse(raw, idx) }
                .onFailure { e -> Log.e(TAG, "Failed to parse ${meta.file}", e) }
                .getOrNull() ?: return@withContext null
            bookCache[book] = parsed
            parsed
        }
    }

    /**
     * Entries arrive as {"chapter:verse": [[work, author, text], ...]}. They are
     * read by hand rather than through a generated serializer because the keys
     * are the data and the rows are positional, which keeps the asset small.
     */
    private fun parse(raw: String, idx: Index): Book {
        val root = json.parseToJsonElement(raw) as JsonObject
        val entries = root["entries"] as? JsonObject ?: return Book(emptyList())
        val anchors = ArrayList<Anchor>(entries.size)
        for ((key, value) in entries) {
            val colon = key.indexOf(':')
            if (colon <= 0) continue
            val chapter = key.substring(0, colon).toIntOrNull() ?: continue
            val verse = key.substring(colon + 1).toIntOrNull() ?: continue
            val rows = (value as? JsonArray) ?: continue
            val passages = rows.mapNotNull { row ->
                val cells = (row as? JsonArray)?.takeIf { it.size >= 3 } ?: return@mapNotNull null
                val work = cells[0].jsonPrimitive.content.toIntOrNull() ?: return@mapNotNull null
                val author = cells[1].jsonPrimitive.content.toIntOrNull() ?: return@mapNotNull null
                Passage(
                    work = idx.works.getOrNull(work)?.title.orEmpty(),
                    author = idx.authors.getOrNull(author).orEmpty(),
                    text = cells[2].jsonPrimitive.content,
                )
            }
            if (passages.isNotEmpty()) anchors.add(Anchor(chapter, verse, passages))
        }
        anchors.sortWith(compareBy({ it.chapter }, { it.verse }))
        return Book(anchors)
    }

    private fun read(context: Context, name: String): String? = runCatching {
        context.applicationContext.assets.open("$DIR/$name").readBytes().decodeToString()
    }.onFailure { Log.e(TAG, "Failed to read $DIR/$name", it) }.getOrNull()
}
