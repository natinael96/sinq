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

    /**
     * The book and chapter holding the hymn a ግጻዌ incipit names.
     *
     * The lectionary only ever gives a መዝሙር by its opening words — a printed
     * ግጻዌ names the chant, and the chant itself lives in another book. That
     * other book is on the shelf now, so the incipit can be a door instead of
     * a dead end.
     *
     * The incipit is written with an ordinal and the word መዝሙር in front of it
     * ("፯ተኛ መዝሙር ትብሎ መርዓት ።"), neither of which is part of the hymn.
     */
    suspend fun byIncipit(context: Context, incipit: String): BookLocation? {
        val needle = fold(INCIPIT_PREFIX.replace(incipit, "")).trim(' ', '\u1362', '\u1361')
        if (needle.length < 8) return null
        val probe = needle.take(14)
        for (meta in all(context).filter { it.shelf == CHANT_SHELF }) {
            val book = book(context, meta.id) ?: continue
            for (chapter in book.chapters) {
                if (probe in fold(chapter.blocks.joinToString(" ") { it.text })) {
                    return BookLocation(meta, chapter.number)
                }
            }
        }
        return null
    }

    /**
     * A book on the shelf and where in it a lookup landed.
     *
     * [block] is the index of the stanza within that chapter, or -1 when only
     * the chapter is known. A መልክእ is a single chapter of forty-odd stanzas,
     * so for those the chapter alone is the top of the book and says nothing.
     */
    data class BookLocation(val book: BookMeta, val chapter: Int, val block: Int = -1)

    /**
     * The stanza a ማኅሌት part is an excerpt of.
     *
     * [byPartName] finds the book the part is named after; this goes one step
     * further and finds the stanza itself, so tapping a movement of the ማኅሌት
     * opens the hymn at the verse being sung rather than at its first page —
     * which, in a hymn of forty stanzas, is forty scrolls from the answer.
     *
     * Matched on the folded opening of the part's own text. Null only when the
     * book is not on the shelf at all; when the book is there but the stanza
     * cannot be found in it — the ማኅሌት and the scan do not always spell a line
     * the same way — this still answers with the book and a [BookLocation.block]
     * of -1, which opens it at the top. Losing the link because the excerpt
     * could not be located would be worse than landing a scroll away from it.
     */
    suspend fun byPartVerse(context: Context, partName: String, verse: String): BookLocation? {
        val meta = byPartName(context, partName) ?: return null
        val top = BookLocation(meta, 0, -1)
        val needle = fold(verse).trim(' ', '\u1362', '\u1361')
        if (needle.length < 8) return top
        val probe = needle.take(14)
        val book = book(context, meta.id) ?: return top
        for (chapter in book.chapters) {
            val i = chapter.blocks.indexOfFirst { probe in fold(it.text) }
            if (i >= 0) return BookLocation(meta, chapter.number, i)
        }
        return top
    }

    /** "፯ተኛ መዝሙር " — an ordinal and the name of the genre, not of the hymn. */
    private val INCIPIT_PREFIX = Regex("^\\s*(?:[\u1369-\u137C0-9]+\\s*(?:\u1270\u129B|\u129B)?\\s*)?(?:\u1218\u12DD\u1219\u122D\\s*)?")

    /** የዜማ መጻሕፍት — the only shelf a chant is on. */
    private const val CHANT_SHELF = "zema"

    /** The app's own homophone folding — tools/build_books.py mirrors it exactly. */
    internal fun fold(raw: String): String =
        AmharicSearch.fold(raw).replace(Regex("\\s+"), " ").trim()

    private fun read(path: String, context: Context): String? = runCatching {
        context.applicationContext.assets.open(path).readBytes().decodeToString()
    }.onFailure { Log.e(TAG, "Failed to read $path", it) }.getOrNull()
}
