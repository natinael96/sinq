package com.sinq.app.data

import android.content.Context
import com.sinq.app.search.AmharicSearch
import com.sinq.app.ui.reading.geezNumeral
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Builds the offline search corpus from bundled assets on first launch.
 *
 * Populates [CorpusDatabase]'s `corpus_docs` and `corpus_fts` tables in a single
 * atomic transaction, eliminating the need to bundle a pre-built SQLite file (~4.5MB).
 */
object CorpusIndexer {

    private val json = Json { ignoreUnknownKeys = true }

    fun interface AssetLoader {
        fun open(path: String): InputStream
    }

    suspend fun populate(context: Context, database: CorpusDatabase) = withContext(Dispatchers.IO) {
        val loader = AssetLoader { path -> context.assets.open(path) }
        populate(loader, database)
    }

    fun populate(loader: AssetLoader, database: CorpusDatabase) {
        val docs = indexAll(loader)
        val db = database.openHelper.writableDatabase

        db.beginTransaction()
        try {
            val stmt = db.compileStatement(
                """
                INSERT INTO corpus_docs (
                    source, target_id, source_label, target_index, title, route,
                    haystack, folded, verse_starts, verse_numbers
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
            )

            for (doc in docs) {
                stmt.bindString(1, doc.source)
                stmt.bindString(2, doc.target_id)
                stmt.bindString(3, doc.source_label)
                stmt.bindLong(4, doc.target_index.toLong())
                stmt.bindString(5, doc.title)
                stmt.bindString(6, doc.route)
                stmt.bindString(7, doc.haystack)
                stmt.bindString(8, doc.folded)
                if (doc.verse_starts != null) stmt.bindString(9, doc.verse_starts) else stmt.bindNull(9)
                if (doc.verse_numbers != null) stmt.bindString(10, doc.verse_numbers) else stmt.bindNull(10)
                stmt.executeInsert()
                stmt.clearBindings()
            }

            db.execSQL(
                """
                INSERT INTO corpus_fts (docid, title, folded)
                SELECT id, title, folded FROM corpus_docs;
                """.trimIndent(),
            )
            db.execSQL("INSERT INTO corpus_fts(corpus_fts) VALUES('optimize');")

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun indexAll(loader: AssetLoader): List<CorpusDocEntity> {
        val docs = ArrayList<CorpusDocEntity>(3100)

        // 1. Scripture (Amharic 1980)
        val metaText = runCatching {
            loader.open("content/bible/am-1980/meta.json").bufferedReader().use { it.readText() }
        }.getOrNull()

        if (metaText != null) {
            val metaObj = json.parseToJsonElement(metaText).jsonObject
            val books = metaObj["books"]?.jsonArray.orEmpty()
            for (bEl in books) {
                val bObj = bEl.jsonObject
                val fileRel = bObj["file"]?.jsonPrimitive?.content ?: continue
                val nameAm = bObj["name"]?.jsonPrimitive?.content ?: ""
                val key = (bObj["usfm_id"]?.jsonPrimitive?.content ?: bObj["id"]?.jsonPrimitive?.content ?: "").lowercase()
                val bookText = runCatching {
                    loader.open("content/bible/am-1980/$fileRel").bufferedReader().use { it.readText() }
                }.getOrNull() ?: continue
                val bookObj = json.parseToJsonElement(bookText).jsonObject
                val chapters = bookObj["chapters"]?.jsonArray.orEmpty()
                for (chEl in chapters) {
                    val chObj = chEl.jsonObject
                    val chapterNum = chObj["chapter"]?.jsonPrimitive?.int ?: 1
                    val verses = chObj["verses"]?.jsonArray.orEmpty()
                    val starts = ArrayList<Int>(verses.size)
                    val numbers = ArrayList<Int>(verses.size)
                    val textParts = ArrayList<String>(verses.size)
                    var curLen = 0
                    for ((i, vEl) in verses.withIndex()) {
                        val vObj = vEl.jsonObject
                        val vText = vObj["t"]?.jsonPrimitive?.contentOrNull
                            ?: vObj["text"]?.jsonPrimitive?.contentOrNull
                            ?: ""
                        val n = vObj["n"]?.jsonPrimitive?.int ?: (i + 1)
                        if (i > 0) curLen += 1
                        starts.add(curLen)
                        numbers.add(n)
                        textParts.add(vText)
                        curLen += vText.length
                    }
                    val hay = textParts.joinToString(" ")
                    val title = "$nameAm ${geezNumeral(chapterNum)}"
                    val route = "scripture/$key/$chapterNum"
                    docs.add(
                        CorpusDocEntity(
                            source = "SCRIPTURE",
                            target_id = key,
                            source_label = "መጽሐፍ ቅዱስ",
                            target_index = chapterNum,
                            title = title,
                            route = route,
                            haystack = hay,
                            folded = AmharicSearch.fold(hay).ifBlank { hay },
                            verse_starts = starts.toString(),
                            verse_numbers = numbers.toString(),
                        ),
                    )
                }
            }
        }

        // 2. Synaxarium (Amharic)
        for (m in 1..13) {
            val mText = runCatching {
                loader.open("content/sinksar/am-$m.json").bufferedReader().use { it.readText() }
            }.getOrNull() ?: continue
            val mObj = json.parseToJsonElement(mText).jsonObject
            val days = mObj["days"]?.jsonArray.orEmpty()
            for (dayEl in days) {
                val dayObj = dayEl.jsonObject
                val dayNum = dayObj["day"]?.jsonPrimitive?.int ?: 1
                val entries = dayObj["entries"]?.jsonArray.orEmpty()
                for ((entryIdx, entryEl) in entries.withIndex()) {
                    val entryObj = entryEl.jsonObject
                    val paragraphs = entryObj["paragraphs"]?.jsonArray.orEmpty()
                    val paraTexts = paragraphs.mapNotNull { pEl ->
                        pEl.jsonObject["text"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                    }
                    val firstPara = paraTexts.firstOrNull().orEmpty()
                    val heading = if ("።" in firstPara) firstPara.substringBefore("።").trim() else firstPara.take(100).trim()
                    val body = paraTexts.joinToString(" ")
                    val arke = entryObj["arke"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val hay = "$body $arke".trim()
                    val targetId = "$m-$dayNum-$entryIdx"
                    val route = "synaxarium/$m/$dayNum?entry=$entryIdx"
                    val f = AmharicSearch.fold(hay).ifBlank { hay }
                    docs.add(
                        CorpusDocEntity(
                            source = "SYNAXARIUM",
                            target_id = targetId,
                            source_label = "ስንክሳር",
                            target_index = dayNum,
                            title = heading,
                            route = route,
                            haystack = hay,
                            folded = f,
                            verse_starts = "[]",
                            verse_numbers = "[]",
                        ),
                    )
                }
            }
        }

        // 3. Books
        val bIndexText = runCatching {
            loader.open("content/books/index.json").bufferedReader().use { it.readText() }
        }.getOrNull()
        if (bIndexText != null) {
            val bIndexObj = json.parseToJsonElement(bIndexText).jsonObject
            val shelves = bIndexObj["shelves"]?.jsonArray.orEmpty()
            for (shelfEl in shelves) {
                val shelfObj = shelfEl.jsonObject
                val books = shelfObj["books"]?.jsonArray.orEmpty()
                for (bMetaEl in books) {
                    val bMetaObj = bMetaEl.jsonObject
                    val bId = bMetaObj["id"]?.jsonPrimitive?.content ?: continue
                    val bFileText = runCatching {
                        loader.open("content/books/$bId.json").bufferedReader().use { it.readText() }
                    }.getOrNull() ?: continue
                    val bData = json.parseToJsonElement(bFileText).jsonObject
                    val bTitle = bData["title"]?.jsonPrimitive?.content ?: ""
                    val chapters = bData["chapters"]?.jsonArray.orEmpty()
                    for (chEl in chapters) {
                        val chObj = chEl.jsonObject
                        val chNum = chObj["number"]?.jsonPrimitive?.int ?: 1
                        val chTitle = chObj["title"]?.jsonPrimitive?.content.orEmpty()
                        val blocks = chObj["blocks"]?.jsonArray.orEmpty()
                        val hay = blocks.mapNotNull { b ->
                            b.jsonObject["text"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                        }.joinToString(" ").trim()
                        if (hay.isBlank()) continue
                        val title = if (chTitle.isNotBlank() && chapters.size > 1) "$bTitle · $chTitle" else bTitle
                        val route = "book/$bId?ch=$chNum"
                        docs.add(
                            CorpusDocEntity(
                                source = "BOOK",
                                target_id = bId,
                                source_label = "መጻሕፍት",
                                target_index = chNum,
                                title = title,
                                route = route,
                                haystack = hay,
                                folded = AmharicSearch.fold(hay).ifBlank { hay },
                                verse_starts = "[]",
                                verse_numbers = "[]",
                            ),
                        )
                    }
                }
            }
        }

        // 4. Wudase
        val wText = runCatching {
            loader.open("content/wudase/wudase.json").bufferedReader().use { it.readText() }
        }.getOrNull()
        if (wText != null) {
            val wData = json.parseToJsonElement(wText).jsonObject
            val sections = wData["sections"]?.jsonArray.orEmpty()
            for (secEl in sections) {
                val secObj = secEl.jsonObject
                val sId = secObj["id"]?.jsonPrimitive?.content ?: ""
                val title = secObj["titleAm"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: secObj["label"]?.jsonPrimitive?.content ?: ""
                val amTexts = secObj["am"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }
                val geTexts = secObj["ge"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }
                val hay = (amTexts + geTexts).joinToString(" ").trim()
                val route = "wudase?sec=$sId"
                docs.add(
                    CorpusDocEntity(
                        source = "WUDASE",
                        target_id = sId,
                        source_label = "ውዳሴ ማርያም",
                        target_index = 0,
                        title = title,
                        route = route,
                        haystack = hay,
                        folded = AmharicSearch.fold(hay).ifBlank { hay },
                        verse_starts = "[]",
                        verse_numbers = "[]",
                    ),
                )
            }
        }

        // 5. Hours
        val hoursFiles = listOf(
            "morning.json", "terce.json", "sext.json", "none.json",
            "vespers.json", "compline.json", "veil.json", "midnight.json",
        )
        for (hName in hoursFiles) {
            val hText = runCatching {
                loader.open("content/$hName").bufferedReader().use { it.readText() }
            }.getOrNull() ?: continue
            val hData = json.parseToJsonElement(hText).jsonObject
            val hId = hData["id"]?.jsonPrimitive?.content ?: hName.substringBefore(".json")
            val hLabel = hData["name"]?.jsonPrimitive?.content ?: hId
            val sections = hData["sections"]?.jsonArray.orEmpty()
            for ((idx, secEl) in sections.withIndex()) {
                val secObj = secEl.jsonObject
                val secTitle = secObj["title"]?.jsonPrimitive?.content ?: ""
                val secSub = secObj["subtitle"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val verses = secObj["verses"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }
                val hay = "$secTitle $secSub ${verses.joinToString(" ")}".trim()
                val route = "prayer/$hId?sec=$idx"
                docs.add(
                    CorpusDocEntity(
                        source = "HOUR",
                        target_id = hId,
                        source_label = hLabel,
                        target_index = idx,
                        title = secTitle,
                        route = route,
                        haystack = hay,
                        folded = AmharicSearch.fold(hay).ifBlank { hay },
                        verse_starts = "[]",
                        verse_numbers = "[]",
                    ),
                )
            }
        }

        // 6. Psalter
        val psalterConfigs = listOf(
            Triple(false, "am-1980", "መዝሙረ ዳዊት"),
            Triple(true, "gez", "መዝሙረ ዳዊት (ግዕዝ)"),
        )
        for ((geez, subDir, label) in psalterConfigs) {
            val targetId = if (geez) "geez" else ""
            val psText = runCatching {
                loader.open("content/bible/$subDir/books/19-psalms.json").bufferedReader().use { it.readText() }
            }.getOrNull() ?: continue
            val psData = json.parseToJsonElement(psText).jsonObject
            val chapters = psData["chapters"]?.jsonArray.orEmpty()
            for ((idx, chEl) in chapters.withIndex()) {
                val chObj = chEl.jsonObject
                val chNum = chObj["chapter"]?.jsonPrimitive?.int ?: (idx + 1)
                val verses = chObj["verses"]?.jsonArray.orEmpty().mapNotNull {
                    it.jsonObject["t"]?.jsonPrimitive?.contentOrNull
                        ?: it.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                }
                val hay = verses.joinToString(" ").trim()
                val title = "$label ${geezNumeral(chNum)}"
                val route = "psalter?section=$idx" + (if (geez) "&geez=true" else "")
                docs.add(
                    CorpusDocEntity(
                        source = "PSALTER",
                        target_id = targetId,
                        source_label = label,
                        target_index = idx,
                        title = title,
                        route = route,
                        haystack = hay,
                        folded = AmharicSearch.fold(hay).ifBlank { hay },
                        verse_starts = "[]",
                        verse_numbers = "[]",
                    ),
                )
            }
        }

        return docs
    }
}
