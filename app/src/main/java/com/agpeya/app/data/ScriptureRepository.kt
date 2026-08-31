package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.ScriptureBook
import com.agpeya.app.model.ScriptureBookMeta
import com.agpeya.app.model.ScriptureVerse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Unified offline scripture source. Bible books and default Psalms come from
 * Amharic 1980; Psalms alone can be switched to Ge'ez 1980.
 */
object ScriptureRepository {

    private const val TAG = "ScriptureRepository"
    private const val DIR = "content/bible"
    const val BIBLE_EDITION = "am-1980"
    const val PSALMS_AMHARIC_EDITION = "am-1980"
    const val PSALMS_GEEZ_EDITION = "gez-1980"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var manifestCache: List<ScriptureBookMeta>? = null

    /**
     * Recently read books only — a whole parsed book is hundreds of kilobytes,
     * and holding all 26 NT books (as an unbounded map once did) costs tens of
     * megabytes for chapters nobody is reading. LruCache is synchronized.
     */
    private val bookCache = android.util.LruCache<String, ScriptureBook>(8)
    private val psalmCache = ConcurrentHashMap<Boolean, List<com.agpeya.app.model.Section>>()

    /** Drop the rebuildable caches under memory pressure (see [CacheTrimmer]). */
    fun trimCaches() {
        bookCache.evictAll()
        psalmCache.clear()
    }

    /** All Bible books except Psalms, which has its own translation selector. */
    suspend fun books(context: Context): List<ScriptureBookMeta> =
        manifestCache ?: withContext(Dispatchers.IO) {
            runCatching {
                val assets = context.applicationContext.assets
                val canon = json.parseToJsonElement(
                    assets.open("$DIR/canon.json").readBytes().decodeToString()
                ).jsonArray.associateBy { it.jsonObject["id"]!!.jsonPrimitive.content }
                val meta = json.parseToJsonElement(
                    assets.open("$DIR/$BIBLE_EDITION/meta.json").readBytes().decodeToString()
                ).jsonObject
                meta["books"]!!.jsonArray.mapNotNull { node ->
                    val b = node.jsonObject
                    val id = b["id"]!!.jsonPrimitive.content
                    if (id == "PSA") return@mapNotNull null
                    val canonical = canon[id]?.jsonObject
                    ScriptureBookMeta(
                        number = b["order"]!!.jsonPrimitive.int,
                        key = slug(b["file"]!!.jsonPrimitive.content),
                        nameAm = b["name"]!!.jsonPrimitive.content,
                        nameEn = canonical?.get("name_en")?.jsonPrimitive?.content ?: id,
                        chapters = b["chapters"]!!.jsonPrimitive.int,
                        testament = canonical?.get("testament")?.jsonPrimitive?.content ?: "old",
                        section = canonical?.get("section")?.jsonPrimitive?.content ?: "",
                    )
                }
            }.onFailure { Log.e(TAG, "Failed to load unified Bible catalog", it) }
                .getOrNull()?.also { manifestCache = it } ?: emptyList()
        }

    /**
     * Load one book by its key (e.g. "luke"), cached after first read.
     * [cache] false reads without populating the cache — the search indexer
     * walks every book once and must not evict what the reader is using.
     */
    suspend fun book(context: Context, key: String, cache: Boolean = true): ScriptureBook? =
        bookCache.get(key) ?: withContext(Dispatchers.IO) {
            CacheTrimmer.ensureRegistered(context)
            runCatching {
                val meta = books(context.applicationContext).first { it.key == key }
                val raw = context.applicationContext.assets
                    .open("$DIR/$BIBLE_EDITION/books/${meta.number.toString().padStart(2, '0')}-$key.json")
                    .readBytes().decodeToString()
                parseBook(json.parseToJsonElement(raw).jsonObject, meta)
            }.onFailure { Log.e(TAG, "Failed to load book $key", it) }
                .getOrNull()?.also { if (cache) bookCache.put(key, it) }
        }

    /** Psalms as the shared Section model used by prayer and scripture readers. */
    suspend fun psalms(context: Context, geez: Boolean = false): List<com.agpeya.app.model.Section> =
        psalmCache[geez] ?: withContext(Dispatchers.IO) {
            CacheTrimmer.ensureRegistered(context)
            val edition = if (geez) PSALMS_GEEZ_EDITION else PSALMS_AMHARIC_EDITION
            runCatching {
                val raw = context.applicationContext.assets
                    .open("$DIR/$edition/books/19-psalms.json").readBytes().decodeToString()
                json.parseToJsonElement(raw).jsonObject["chapters"]!!.jsonArray
                    .mapNotNull { chapter ->
                        val c = chapter.jsonObject
                        val number = c["n"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                        if (number !in 1..150) return@mapNotNull null
                        val verses = c["verses"]!!.jsonArray.mapNotNull { v ->
                            v.jsonObject["t"]?.jsonPrimitive?.contentOrNull
                        }
                        val headings = c["headings"]?.jsonArray.orEmpty().mapNotNull { hNode ->
                            val h = hNode.jsonObject
                            val before = h["before"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
                            val text = h["text"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                                ?: return@mapNotNull null
                            before to text
                        }.toMap()
                        com.agpeya.app.model.Section(
                            id = "ps_$number",
                            orderIndex = number - 1,
                            type = "psalm",
                            number = number,
                            title = "መዝሙር ${com.agpeya.app.ui.reading.geezNumeral(number)}",
                            firstVerse = 1,
                            verseHeaders = headings,
                            verses = verses,
                        )
                    }
            }.onFailure { Log.e(TAG, "Failed to load Psalms from $edition", it) }
                .getOrDefault(emptyList()).also { if (it.isNotEmpty()) psalmCache[geez] = it }
        }

    private fun slug(file: String): String = file.substringAfter('/').substringAfter('-').removeSuffix(".json")

    private fun parseBook(raw: JsonObject, meta: ScriptureBookMeta): ScriptureBook = ScriptureBook(
        number = meta.number,
        key = meta.key,
        nameAm = meta.nameAm,
        nameEn = meta.nameEn,
        chapters = raw["chapters"]!!.jsonArray.map { node ->
            val c = node.jsonObject
            com.agpeya.app.model.ScriptureChapter(
                chapter = c["n"]!!.jsonPrimitive.int,
                verses = c["verses"]!!.jsonArray.mapIndexedNotNull { index, verseNode ->
                    val v = verseNode.jsonObject
                    val text = v["t"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null
                    ScriptureVerse(v["n"]?.jsonPrimitive?.intOrNull ?: index + 1, text)
                },
            )
        },
    )

    /**
     * Resolve a Gitsawe reference to its verses. [bookKey] is a bundled book
     * key (already resolved via [resolveBookKey]); [start]/[end] are verse
     * numbers. A missing [end] is not an incomplete range: the lectionary's
     * "ከ᎐᎐ ጀምሮ" convention means from [start] to the END of the chapter, so
     * [end] null reads through the chapter's last verse (both null = the whole
     * chapter). Returns null only when the book or chapter genuinely doesn't
     * exist — a valid chapter always yields a non-empty page.
     *
     * The verse range is clamped to what the chapter actually has: some ~3% of
     * lectionary references over-cite (LXX/Masoretic verse-count differences, or
     * plain source errors like a verse past the chapter's end). Rather than show
     * an empty page, we clamp into range and, if the citation lands entirely
     * outside the chapter, fall back to the whole chapter.
     */
    suspend fun passage(
        context: Context,
        bookKey: String,
        chapter: Int,
        start: Int? = null,
        end: Int? = null,
    ): List<ScriptureVerse>? {
        val book = book(context, bookKey) ?: return null
        val ch = book.chapters.find { it.chapter == chapter } ?: return null
        return citedRange(ch.verses, start, end)
    }

    /**
     * The cited slice of a chapter's verses — the pure half of [passage], so
     * the range rules are unit-testable against the real bundled data.
     */
    fun citedRange(verses: List<ScriptureVerse>, start: Int?, end: Int?): List<ScriptureVerse> {
        if (start == null) return verses
        val maxN = verses.maxOfOrNull { it.n } ?: return verses
        val lo = start.coerceAtMost(maxN)
        val hi = (end ?: maxN).coerceIn(lo, maxN)
        return verses.filter { it.n in lo..hi }.ifEmpty { verses }
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
