package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.ScriptureBook
import com.agpeya.app.model.ScriptureBookMeta
import com.agpeya.app.model.ScriptureManifest
import com.agpeya.app.model.ScriptureVerse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads the bundled New Testament (assets/content/scripture/) and resolves the
 * ግጻዌ (Gitsawe) book titles to it. The manifest is small and loaded once; each
 * book is loaded lazily and cached, so a reading opens only the one book it needs.
 */
object ScriptureRepository {

    private const val TAG = "ScriptureRepository"
    private const val DIR = "content/scripture"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var manifestCache: List<ScriptureBookMeta>? = null
    private val bookCache = ConcurrentHashMap<String, ScriptureBook>()

    /** The 27 NT books (metadata only). */
    suspend fun books(context: Context): List<ScriptureBookMeta> =
        manifestCache ?: withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.applicationContext.assets
                    .open("$DIR/nt-manifest.json").readBytes().decodeToString()
                json.decodeFromString<ScriptureManifest>(raw).books
            }.onFailure { Log.e(TAG, "Failed to load nt-manifest.json", it) }
                // Cache only success, matching book() below.
                .getOrNull()?.also { manifestCache = it } ?: emptyList()
        }

    /** Load one book by its key (e.g. "luke"), cached after first read. */
    suspend fun book(context: Context, key: String): ScriptureBook? =
        bookCache[key] ?: withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.applicationContext.assets
                    .open("$DIR/$key.json").readBytes().decodeToString()
                json.decodeFromString<ScriptureBook>(raw)
            }.onFailure { Log.e(TAG, "Failed to load book $key", it) }
                .getOrNull()?.also { bookCache[key] = it }
        }

    /**
     * Resolve a Gitsawe reference to its verses. [bookTitle] is the raw lectionary
     * title (any of its many spellings); [start]/[end] are verse numbers ([end]
     * null = single verse, both null = whole chapter). Returns null only when the
     * book or chapter genuinely doesn't exist — a valid chapter always yields a
     * non-empty page.
     *
     * The verse range is clamped to what the chapter actually has: some ~3% of
     * lectionary references over-cite (LXX/Masoretic verse-count differences, or
     * plain source errors like a verse past the chapter's end). Rather than show
     * an empty page, we clamp into range and, if the citation lands entirely
     * outside the chapter, fall back to the whole chapter.
     */
    suspend fun passage(
        context: Context,
        bookTitle: String,
        chapter: Int,
        start: Int? = null,
        end: Int? = null,
    ): List<ScriptureVerse>? {
        val key = resolveBookKey(bookTitle) ?: return null
        val book = book(context, key) ?: return null
        val ch = book.chapters.find { it.chapter == chapter } ?: return null
        if (start == null) return ch.verses
        val maxN = ch.verses.maxOfOrNull { it.n } ?: return ch.verses
        val lo = start.coerceAtMost(maxN)
        val hi = (end ?: lo).coerceIn(lo, maxN)
        return ch.verses.filter { it.n in lo..hi }.ifEmpty { ch.verses }
    }

    // ---- Gitsawe title resolution -------------------------------------------

    /**
     * Map a raw ግጻዌ book title to a bundled NT book key, or null for non-NT
     * references (Psalms route to the Psalter instead). Matches by keyword +
     * ordinal so it tolerates the data's spelling variants, typos (ማርዎስ→Mark,
     * ያዕቆን→James), double spaces, and the ፩/፪/፫ vs 1ኛ/2ኛ ordinal forms.
     */
    fun resolveBookKey(rawTitle: String): String? {
        val t = rawTitle.replace(" ", "")
        val ord = ordinal(t)
        fun has(vararg xs: String) = xs.any { it in t }
        return when {
            has("ራዕይ") -> "revelation"
            has("ማቴዎስ") -> "matthew"
            has("ማርቆስ", "ማርዎስ") -> "mark"
            has("ሉቃስ") -> "luke"
            has("ሐዋርያት") -> "acts"                 // ግብረ ሐዋርያት or የሐዋርያት ሥራ
            has("ሮሜ") -> "romans"
            has("ቆሮንቶስ", "ቈሮንቶስ") -> if ((ord ?: 1) == 1) "1-corinthians" else "2-corinthians"
            has("ገላትያ") -> "galatians"
            has("ኤፌሶን") -> "ephesians"
            has("ፊልጵስዩስ", "ፊልጲስዮስ") -> "philippians"
            has("ቈላስይስ", "ቆላስይስ") -> "colossians"
            has("ተሰሎንቄ") -> if ((ord ?: 1) == 1) "1-thessalonians" else "2-thessalonians"
            has("ጢሞቴዎስ") -> if ((ord ?: 1) == 1) "1-timothy" else "2-timothy"
            has("ቲቶ") -> "titus"
            has("ፊልሞና") -> "philemon"
            has("ዕብራውያን") -> "hebrews"
            has("ጴጥሮስ") -> if ((ord ?: 1) == 1) "1-peter" else "2-peter"
            has("ያዕቆብ", "ያዕቆን") -> "james"
            has("ይሁዳ") -> "jude"
            has("ዮሐንስ") -> when {
                has("ወንጌል") -> "john"
                ord == 2 -> "2-john"
                ord == 3 -> "3-john"
                else -> "1-john"
            }
            else -> null                            // Psalms (መዝሙረ ዳዊት) and anything unknown
        }
    }

    /** First ordinal marker in a book title: ፫/3, ፪/2, ፩/1 — or null if none. */
    private fun ordinal(t: String): Int? = when {
        t.any { it == '፫' || it == '3' } -> 3
        t.any { it == '፪' || it == '2' } -> 2
        t.any { it == '፩' || it == '1' } -> 1
        else -> null
    }
}
