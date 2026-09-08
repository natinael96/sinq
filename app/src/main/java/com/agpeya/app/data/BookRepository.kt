package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.Book
import com.agpeya.app.model.BookIndex
import com.agpeya.app.model.BookMeta
import com.agpeya.app.search.AmharicSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * The ሌሎች መጻሕፍት shelf.
 *
 * Nine megabytes of scanned church books, so nothing loads eagerly: the index
 * is small and cached for the session, and a book is read only when it is
 * opened. The two most recently opened books stay in memory, which is what
 * paging back and forth between a ማኅሌት part and its full hymn costs.
 */
object BookRepository {

    private const val TAG = "BookRepository"
    private const val DIR = "content/books"
    private const val KEEP = 2

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var indexCache: BookIndex? = null

    private val recent = object : LinkedHashMap<String, Book>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Book>?) = size > KEEP
    }

    suspend fun index(context: Context): BookIndex =
        indexCache ?: withContext(Dispatchers.IO) {
            read("$DIR/index.json", context)?.let { runCatching { json.decodeFromString<BookIndex>(it) }
                .onFailure { e -> Log.e(TAG, "Failed to parse the book index", e) }.getOrNull() }
                // Cache only success, so a transient read failure doesn't stick
                // as an empty shelf for the rest of the process.
                ?.also { indexCache = it } ?: BookIndex()
        }

    /** Every book on every shelf, in shelf order — used by search and the joins. */
    suspend fun all(context: Context): List<BookMeta> =
        index(context).shelves.flatMap { it.books }

    suspend fun meta(context: Context, id: String): BookMeta? =
        all(context).find { it.id == id }

    suspend fun book(context: Context, id: String): Book? {
        if (id.isBlank()) return null
        synchronized(recent) { recent[id] }?.let { return it }
        return withContext(Dispatchers.IO) {
            read("$DIR/$id.json", context)
                ?.let { runCatching { json.decodeFromString<Book>(it) }
                    .onFailure { e -> Log.e(TAG, "Failed to parse book $id", e) }.getOrNull() }
                ?.also { synchronized(recent) { recent[id] = it } }
        }
    }

    /**
     * The book a ማኅሌት part names, if the shelf has it.
     *
     * A part called መልክአ ሥላሴ is an excerpt of the book of that name, so the
     * part heading becomes a door into the whole hymn. Matched on the folded
     * key the generator wrote, because the scans spell መልክአ and መልክዐ both ways.
     */
    suspend fun byPartName(context: Context, partName: String): BookMeta? {
        val key = fold(partName)
        if (key.isBlank()) return null
        return all(context).find { it.key == key }
    }

    /** The app's own homophone folding — tools/build_books.py mirrors it exactly. */
    internal fun fold(raw: String): String =
        AmharicSearch.fold(raw).replace(Regex("\\s+"), " ").trim()

    private fun read(path: String, context: Context): String? = runCatching {
        context.applicationContext.assets.open(path).readBytes().decodeToString()
    }.onFailure { Log.e(TAG, "Failed to read $path", it) }.getOrNull()
}
