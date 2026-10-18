package com.sinq.app.search

import android.content.Context
import com.sinq.app.data.ContentRepository
import com.sinq.app.data.ScriptureRepository
import com.sinq.app.data.SynaxariumRepository
import com.sinq.app.data.WudaseRepository
import com.sinq.app.model.Section
import com.sinq.app.model.SynaxariumDay
import com.sinq.app.ui.common.EthiopianDate
import com.sinq.app.ui.reading.geezNumeral
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline search over all prayer text — the prayer hours AND the 150-psalm
 * Psalter — tolerant of Amharic homophones.
 *
 * Ge'ez script writes several distinct letters that sound identical, and users
 * spell the same word different ways (ጸሎት / ፀሎት, ሰላም / ሠላም, ሀ / ሐ / ኀ / ኸ,
 * አ / ዐ). [fold] maps each homophone family to one canonical consonant while
 * preserving its vowel order, so a query matches regardless of which form was
 * typed. Both the indexed text and the query are folded before comparison.
 */
object AmharicSearch {

    /**
     * Canonical fold mapping for Ethiopic script, organized by phonetic family.
     *
     * Each family maps archaic, dialectal, and interchangeable homophone characters
     * to a single canonical equivalent while preserving vocalic order.
     */
    val ETHIOPIC_FOLD_TABLE: Map<Char, String> = buildMap {
        // -------------------------------------------------------------------------
        // 1. HA FAMILY (ሀ, ሐ, ኀ, ኸ) - Laryngeal & Velar Fricatives
        // Canonical base: ሀ (0x1200) series. Includes 1st & 4th order mergers.
        // -------------------------------------------------------------------------
        put('ሐ', "ሀ"); put('ሑ', "ሁ"); put('ሒ', "ሂ"); put('ሓ', "ሀ"); put('ሔ', "ሄ"); put('ሕ', "ህ"); put('ሖ', "ሆ"); put('ሗ', "ኋ")
        put('ኀ', "ሀ"); put('ኁ', "ሁ"); put('ኂ', "ሂ"); put('ኃ', "ሀ"); put('ኄ', "ሄ"); put('ኅ', "ህ"); put('ኆ', "ሆ"); put('ኇ', "ኋ")
        put('ኸ', "ሀ"); put('ኹ', "ሁ"); put('ኺ', "ሂ"); put('ኻ', "ሀ"); put('ኼ', "ሄ"); put('ኽ', "ህ"); put('ኾ', "ሆ")
        put('ሃ', "ሀ") // 4th order merger to 1st order

        // -------------------------------------------------------------------------
        // 2. SE FAMILY (ሰ, ሠ) - Alveolar Sibilants
        // Canonical base: ሰ (0x1230) series. ሠ-series folds to ሰ-series.
        // -------------------------------------------------------------------------
        put('ሠ', "ሰ"); put('ሡ', "ሱ"); put('ሢ', "ሲ"); put('ሣ', "ሳ"); put('ሤ', "ሴ"); put('ሥ', "ስ"); put('ሦ', "ሶ"); put('ሧ', "ሷ")

        // -------------------------------------------------------------------------
        // 3. GLOTTAL / A FAMILY (አ, ዐ) - Glottals & Pharyngeals
        // Canonical base: አ (0x12A0) series. ዐ-series folds to አ-series. Includes 1st & 4th order mergers.
        // -------------------------------------------------------------------------
        put('ዐ', "አ"); put('ዑ', "ኡ"); put('ዒ', "ኢ"); put('ዓ', "አ"); put('ዔ', "ኤ"); put('ዕ', "እ"); put('ዖ', "ኦ")
        put('ኣ', "አ") // 4th order merger
        put('ኧ', "አ")

        // -------------------------------------------------------------------------
        // 4. TSE FAMILY (ጸ, ፀ) - Ejective Affricates
        // Canonical base: ጸ (0x1338) series. ፀ-series folds to ጸ-series.
        // -------------------------------------------------------------------------
        put('ፀ', "ጸ"); put('ፁ', "ጹ"); put('ፂ', "ጺ"); put('ፃ', "ጻ"); put('ፄ', "ጼ"); put('ፅ', "ጽ"); put('ፆ', "ጾ"); put('ፇ', "ጿ")

        // -------------------------------------------------------------------------
        // 5. LABIALIZED VELARS (ዲቃላ ፊደላት)
        // Diphthong labialized forms folded to standard vocalic counterparts.
        // -------------------------------------------------------------------------
        put('ኰ', "ኮ"); put('ኲ', "ኪ"); put('ኴ', "ኬ"); put('ኵ', "ክ")
        put('ጐ', "ጎ"); put('ጒ', "ጊ"); put('ጔ', "ጌ"); put('ጕ', "ግ")
        put('ቈ', "ቆ"); put('ቊ', "ቂ"); put('ቌ', "ቄ"); put('ቍ', "ቅ")
        put('ኈ', "ሆ"); put('ኊ', "ሂ"); put('ኌ', "ሄ"); put('ኍ', "ህ")
        put('ኋ', "ዃ")

        // -------------------------------------------------------------------------
        // 6. ETHIOPIC PUNCTUATION & WORD SEPARATORS
        // Wordspace folds to ASCII space; delimiters & sentence terminators stripped.
        // -------------------------------------------------------------------------
        put('፡', " ") // Ethiopic word separator
        put('።', "")  // Full stop (Arat Neteb)
        put('፣', "")  // Comma (Netela Serez)
        put('፤', "")  // Semicolon (Dereb Serez)
        put('፥', "")  // Colon (Yeqetir Neteb)
        put('፦', "")  // Preface colon (Teqatu)
        put('፧', "")  // Question mark (Yemiteyeq Qirts)
        put('፨', "")  // Paragraph mark (Hulete Neteb)
        put('፠', "")  // Section mark
    }

    /** Fold one character to its canonical homophone form, keeping the vowel order. */
    fun foldChar(c: Char): String = ETHIOPIC_FOLD_TABLE[c] ?: c.toString()

    /** Normalize Ge'ez numerals to Arabic numerals so searching "መዝሙር 23" or "መዝሙር ፳፫" matches identically. */
    fun normalizeNumerals(text: String): String {
        val regex = Regex("[፩-፼]+")
        return regex.replace(text) { matchResult ->
            com.sinq.app.ui.reading.parseGeezNumeral(matchResult.value)?.toString() ?: matchResult.value
        }
    }

    fun fold(text: String): String {
        val normalized = normalizeNumerals(text)
        val sb = StringBuilder(normalized.length)
        for (c in normalized) {
            sb.append(foldChar(c))
        }
        return sb.toString().replace(Regex("[ \\t]+"), " ").trim()
    }

    /** Where a result came from — lets the UI label it and route the tap. */
    enum class Source { HOUR, PSALTER, SCRIPTURE, SYNAXARIUM, WUDASE, BOOK }

    /** Quality of the match: EXACT orthography vs FOLDED homophone match. */
    enum class MatchQuality { EXACT, FOLDED }

    data class Result(
        val source: Source,
        /** Real hour id for [Source.HOUR]; empty for the Psalter. */
        val targetId: String,
        /** Display label of the origin (hour name, or the Psalter title). */
        val sourceLabel: String,
        /** Section index within the hour, or psalm index within the full psalter. */
        val targetIndex: Int,
        val title: String,
        val snippet: String,
        /** Match offset within [snippet] for highlighting; -1 if not locatable. */
        val snippetMatchStart: Int,
        val snippetMatchLen: Int,
        /** A ready navigation route. Sources added after the original two carry
         *  one instead of being decoded from [targetId]/[targetIndex]. */
        val route: String? = null,
        /**
         * How well the hit answers the query — see [scoreOf]. Corpus order was
         * the only order before, so a search for ጸሎት led with whatever the
         * bundle happened to list first.
         */
        val score: Int = 0,
        /**
         * Match quality: [MatchQuality.EXACT] if the original query text matched
         * without folding homophones, or [MatchQuality.FOLDED] if matched via homophones.
         */
        val matchQuality: MatchQuality = MatchQuality.EXACT,
    )

    /**
     * How good a hit is, high to low.
     *
     * A match in the title is what someone searching a book name wants; a match
     * at the start of a word is a word, and a match inside one is a coincidence
     * of spelling that is still worth showing, last. An earlier hit in a long
     * document beats a later one, which only breaks ties.
     */
    internal fun scoreOf(
        title: String,
        haystack: String,
        needle: String,
        hit: Int,
        isExact: Boolean = false,
    ): Int {
        val inTitle = fold(title).contains(needle)
        val boundary = hit == 0 || !haystack[hit - 1].isLetter()
        val base = when {
            inTitle && boundary -> 400
            inTitle -> 300
            boundary -> 200
            else -> 100
        }
        val exactBonus = if (isExact) 1000 else 0
        return exactBonus + base + (50 - hit / 40).coerceIn(0, 50)
    }

    /**
     * One searchable unit of a bundled corpus, with its haystack pre-folded.
     *
     * The scripture, ስንክሳር and ውዳሴ ማርያም corpora together are megabytes of text;
     * folding them on every keystroke would allocate all of it each time. They're
     * folded once into [Doc]s and kept until memory pressure ([trimCaches]).
     *
     * [haystack] and [folded] are both kept deliberately: matching needs the
     * folded form, but folding is lossy (ሠ→ሰ, ሐ→ሀ …), so snippets must be cut
     * from the original or the user would see the wrong letters. The parsed
     * book models behind them are NOT retained — see [buildIndex].
     */
    internal class Doc(
        val source: Source,
        val targetId: String,
        val targetIndex: Int,
        val title: String,
        val route: String,
        val haystack: String,
        val folded: String,
        /**
         * Where each verse begins in [haystack], and which verse that is. A
         * match is an offset; these turn it into the verse to land on. Empty
         * for a document with no verse structure.
         */
        val verseStarts: IntArray = IntArray(0),
        val verseNumbers: IntArray = IntArray(0),
    )

    /** The verse a hit at [offset] falls inside, or null without verse structure. */
    private fun Doc.verseAt(offset: Int): Int? {
        if (verseStarts.isEmpty()) return null
        var found = 0
        for (i in verseStarts.indices) {
            if (verseStarts[i] <= offset) found = i else break
        }
        return verseNumbers.getOrNull(found)
    }

    /** Where a tap should land: the verse the match is in, not just its chapter. */
    private fun Doc.routeTo(offset: Int): String =
        verseAt(offset)?.let { "$route?start=$it" } ?: route

    @Volatile private var docIndex: List<Doc>? = null

    /** Drop the rebuildable index under memory pressure (see [CacheTrimmer]). */
    fun trimCaches() {
        docIndex = null
    }

    /** Build (once) the folded index over the bundled corpora. */
    private suspend fun index(context: Context): List<Doc> =
        docIndex ?: buildIndex(context).also {
            docIndex = it
            com.sinq.app.data.CacheTrimmer.ensureRegistered(context)
        }

    private suspend fun buildIndex(context: Context): List<Doc> {
        val docs = mutableListOf<Doc>()

        // cache = false: indexing walks every NT book once; letting those loads
        // fill ScriptureRepository's cache would pin megabytes of parsed books
        // (and evict the ones actually being read) for a one-shot pass.
        for (meta in ScriptureRepository.books(context)) {
            val book = ScriptureRepository.book(context, meta.key, cache = false) ?: continue
            for (chapter in book.chapters) {
                val text = StringBuilder()
                val starts = IntArray(chapter.verses.size)
                val numbers = IntArray(chapter.verses.size)
                chapter.verses.forEachIndexed { i, verse ->
                    if (i > 0) text.append(' ')
                    starts[i] = text.length
                    numbers[i] = verse.n
                    text.append(verse.text)
                }
                val hay = text.toString()
                docs += Doc(
                    source = Source.SCRIPTURE,
                    targetId = meta.key,
                    targetIndex = chapter.chapter,
                    title = "${book.nameAm} ${geezNumeral(chapter.chapter)}",
                    route = "scripture/${meta.key}/${chapter.chapter}",
                    haystack = hay,
                    folded = fold(hay),
                    verseStarts = starts,
                    verseNumbers = numbers,
                )
            }
        }

        // The ስንክሳር is keyed by Ethiopian month/day; the route resolves to the
        // Gregorian date that day falls on in the current Ethiopian year.
        val ethYear = EthiopianDate.from(LocalDate.now()).year
        for (month in 1..13) {
            // The Amharic edition only. Indexing both would double the ስንክሳር's
            // weight in memory to find the same commemorations twice.
            docs += synaxariumDocs(month, ethYear, SynaxariumRepository.month(context, month))
        }

        // The ሌሎች መጻሕፍት shelf, a document per chapter rather than per book:
        // ሥርዓተ ቅዳሴ is 230k characters, and a hit that lands on "the book" makes
        // the reader hunt through twenty-three chapters for the word.
        for (meta in com.sinq.app.data.BookRepository.all(context)) {
            val book = com.sinq.app.data.BookRepository.book(context, meta.id) ?: continue
            for (chapter in book.chapters) {
                val hay = chapter.blocks.joinToString(" ") { it.text }
                if (hay.isBlank()) continue
                docs += Doc(
                    source = Source.BOOK,
                    targetId = meta.id,
                    targetIndex = chapter.number,
                    title = if (book.chapters.size > 1 && chapter.title.isNotBlank())
                        "${book.title} · ${chapter.title}" else book.title,
                    route = "book/${meta.id}?ch=${chapter.number}",
                    haystack = hay,
                    folded = fold(hay),
                )
            }
        }

        for (section in WudaseRepository.load(context).sections) {
            val hay = (section.am + section.ge).joinToString(" ")
            docs += Doc(
                source = Source.WUDASE,
                targetId = section.id,
                targetIndex = 0,
                title = section.titleAm.ifBlank { section.label },
                route = "wudase?sec=${section.id}",
                haystack = hay,
                folded = fold(hay),
            )
        }

        return docs
    }

    /**
     * Build one month of Synaxarium search documents. A day commonly has several
     * commemorations, so the entry ordinal is part of [Doc.targetId]. Without it,
     * two matches from the same day receive the same Compose lazy-list key and
     * crash while the result list is being laid out.
     */
    internal fun synaxariumDocs(month: Int, ethYear: Int, days: List<SynaxariumDay>): List<Doc> =
        buildList {
            for (day in days) {
                val epochDay = runCatching {
                    EthiopianDate(ethYear, month, day.day).toGregorian().toEpochDay()
                }.getOrNull() ?: continue
                day.entries.forEachIndexed { entryIndex, entry ->
                    // The hymn is searched with the life it closes: a reader
                    // looking for a line of an አርኬ should find the saint it
                    // belongs to, not a result with no name on it.
                    val hay = entry.text + " " + entry.arke.orEmpty()
                    add(
                        Doc(
                            source = Source.SYNAXARIUM,
                            targetId = "$month-${day.day}-$entryIndex",
                            targetIndex = day.day,
                            title = entry.heading,
                            // The entry the hit is in, not just the day: a
                            // twelve-entry ስንክሳር landed at the top and left the
                            // reader to find it.
                            route = "synaxarium/$epochDay?entry=$entryIndex",
                            haystack = hay,
                            folded = fold(hay),
                        ),
                    )
                }
            }
        }

    /**
     * Pre-warm search database ahead of first keystroke.
     */
    suspend fun warm(context: Context) {
        withContext(Dispatchers.IO) {
            runCatching {
                com.sinq.app.data.CorpusDatabase.get(context).corpusDao().count()
            }
        }
    }

    suspend fun search(context: Context, rawQuery: String, labels: Labels): List<Result> {
        val query = rawQuery.trim()
        if (query.length < 2) return emptyList()
        val needle = fold(query)

        return withContext(Dispatchers.Default) {
            val dbResults = runCatching {
                val db = com.sinq.app.data.CorpusDatabase.get(context)
                val ftsQuery = needle.split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { "$it*" }
                val ftsHits = runCatching { db.corpusDao().searchFts(ftsQuery) }.getOrDefault(emptyList())
                val likeHits = db.corpusDao().searchLike(needle)
                val allHits = (ftsHits + likeHits).distinctBy { it.id }
                if (allHits.isEmpty()) return@runCatching emptyList<Result>()

                allHits.mapNotNull { doc ->
                    val hit = doc.folded.indexOf(needle)
                    if (hit < 0) return@mapNotNull null
                    val snip = snippet(doc.haystack, hit, query.length)
                    val sourceEnum = runCatching { Source.valueOf(doc.source) }.getOrDefault(Source.SCRIPTURE)
                    val label = when (sourceEnum) {
                        Source.HOUR -> doc.source_label
                        Source.PSALTER -> doc.source_label
                        Source.SCRIPTURE -> labels.scripture
                        Source.SYNAXARIUM -> labels.synaxarium
                        Source.WUDASE -> labels.wudase
                        Source.BOOK -> doc.source_label
                    }
                    val isExact = query.isNotBlank() && (doc.title.contains(query) || doc.haystack.contains(query))
                    val quality = if (isExact) MatchQuality.EXACT else MatchQuality.FOLDED
                    Result(
                        source = sourceEnum,
                        targetId = doc.target_id,
                        sourceLabel = label,
                        targetIndex = doc.target_index,
                        title = doc.title.ifBlank { label },
                        snippet = snip.text,
                        snippetMatchStart = snip.matchStart,
                        snippetMatchLen = snip.matchLen,
                        route = doc.route,
                        score = scoreOf(doc.title, doc.haystack, needle, hit, isExact = isExact),
                        matchQuality = quality,
                    )
                }
            }.getOrNull()

            if (!dbResults.isNullOrEmpty()) {
                return@withContext dbResults.sortedByDescending { it.score }
            }

            // Fallback for tests or environments without asset database
            val results = mutableListOf<Result>()
            for (hour in ContentRepository.hours(context)) {
                hour.sections.forEachIndexed { index, section ->
                    match(section, needle, query.length)?.let { snip ->
                        val hay = haystackOf(section)
                        val isExact = query.isNotBlank() && (section.title.contains(query) || hay.contains(query))
                        val quality = if (isExact) MatchQuality.EXACT else MatchQuality.FOLDED
                        results += Result(
                            source = Source.HOUR,
                            targetId = hour.id,
                            sourceLabel = hour.name,
                            targetIndex = index,
                            title = section.title,
                            snippet = snip.text,
                            snippetMatchStart = snip.matchStart,
                            snippetMatchLen = snip.matchLen,
                            score = scoreOf(section.title, hay, needle, snip.matchStart, isExact = isExact),
                            matchQuality = quality,
                        )
                    }
                }
            }
            listOf(false to labels.psalter, true to labels.psalterGeez).forEach { (geez, label) ->
                val psalms = runCatching {
                    if (geez) ScriptureRepository.psalms(context, geez = true)
                    else ContentRepository.psalter(context)
                }.getOrDefault(emptyList())
                psalms.forEachIndexed { index, psalm ->
                    match(psalm, needle, query.length)?.let { snip ->
                        val hay = haystackOf(psalm)
                        val isExact = query.isNotBlank() && (psalm.title.contains(query) || hay.contains(query))
                        val quality = if (isExact) MatchQuality.EXACT else MatchQuality.FOLDED
                        results += Result(
                            source = Source.PSALTER,
                            targetId = if (geez) "geez" else "",
                            sourceLabel = label,
                            targetIndex = index,
                            title = psalm.title,
                            snippet = snip.text,
                            snippetMatchStart = snip.matchStart,
                            snippetMatchLen = snip.matchLen,
                            score = scoreOf(psalm.title, hay, needle, snip.matchStart, isExact = isExact),
                            matchQuality = quality,
                        )
                    }
                }
            }
            results += searchBundled(context, query, needle, query.length, labels)
            results.sortedByDescending { it.score }
        }
    }

    /**
     * A typed reference — "መዝሙር 23", "ሉቃስ 10", "ps 23" — resolved to the page
     * it names, so the user jumps straight there instead of scanning matches.
     * Returned ahead of the text results.
     */
    suspend fun referenceMatch(context: Context, rawQuery: String, labels: Labels): Result? {
        val q = rawQuery.trim()
        if (q.isEmpty()) return null

        val (titlePart, number) = parseReference(q) ?: return null
        if (looksLikePsalm(titlePart) && number in 1..150) {
            return Result(
                source = Source.PSALTER,
                targetId = "",
                sourceLabel = labels.psalter,
                targetIndex = number - 1,
                title = "${labels.psalter} ${geezNumeral(number)}",
                snippet = "",
                snippetMatchStart = -1,
                snippetMatchLen = 0,
                route = "psalter?section=${number - 1}",
            )
        }

        if (titlePart.isEmpty()) return null
        val key = ScriptureRepository.resolveBookKey(titlePart) ?: return null
        val book = ScriptureRepository.book(context, key) ?: return null
        // Silently clamping would send the user somewhere they didn't ask for.
        if (number !in 1..book.chapters.size) return null
        return Result(
            source = Source.SCRIPTURE,
            targetId = key,
            sourceLabel = labels.scripture,
            targetIndex = number,
            title = "${book.nameAm} ${geezNumeral(number)}",
            snippet = "",
            snippetMatchStart = -1,
            snippetMatchLen = 0,
            route = "scripture/$key/$number",
        )
    }

    /**
     * Split "ሉቃስ 10" into its book part and trailing number; null if it isn't
     * shaped like a reference at all. Pure, so it is unit-testable.
     *
     * The number may be written either way. Every number this app prints is a
     * Ge'ez numeral, so "መዝሙር ፶" is someone typing back what they just read,
     * and it used to find nothing.
     */
    internal fun parseReference(raw: String): Pair<String, Int>? {
        val m = Regex("^(.*?)[\\s:፥]*([\\d፩-፼]{1,6})\\s*$").find(raw.trim()) ?: return null
        val digits = m.groupValues[2]
        val number = digits.toIntOrNull()
            ?: com.sinq.app.ui.reading.parseGeezNumeral(digits)
            ?: return null
        return m.groupValues[1].trim() to number
    }

    /** A bare number, or one prefixed with a psalm word, names a psalm. */
    internal fun looksLikePsalm(titlePart: String): Boolean {
        if (titlePart.isEmpty()) return true
        return listOf("መዝሙር", "መዝ", "ps", "psalm", "ምዕራፍ")
            .any { titlePart.equals(it, true) || titlePart.startsWith(it, true) }
    }

    /** Display labels for the corpora, passed in so search stays UI-agnostic. */
    data class Labels(
        val psalter: String,
        val psalterGeez: String,
        val scripture: String,
        val synaxarium: String,
        val wudase: String,
    )

    /** One pass over the pre-folded index, capped per source. */
    private suspend fun searchBundled(
        context: Context,
        rawQuery: String,
        needle: String,
        matchLen: Int,
        labels: Labels,
    ): List<Result> {
        return searchDocs(index(context), needle, matchLen, labels, rawQuery = rawQuery)
    }

    /**
     * Pure search pass, separated so the complete bundled Synaxarium can be
     * regression-tested without an Android Context.
     *
     * Every match is returned. Deciding how many of them to draw is the screen's
     * job: capping here meant a corpus was searched in bundled order and cut off
     * at a fixed count, so a search for ኢየሱስ stopped inside Mark and the rest of
     * the New Testament could not be reached at all.
     */
    internal fun searchDocs(
        docs: List<Doc>,
        needle: String,
        matchLen: Int,
        labels: Labels,
        rawQuery: String = needle,
    ): List<Result> {
        if (needle.isEmpty()) return emptyList()
        val out = mutableListOf<Result>()
        for (doc in docs) {
            val hit = doc.folded.indexOf(needle)
            if (hit < 0) continue
            val snip = snippet(doc.haystack, hit, matchLen)
            val isExact = rawQuery.isNotBlank() && (doc.title.contains(rawQuery) || doc.haystack.contains(rawQuery))
            val quality = if (isExact) MatchQuality.EXACT else MatchQuality.FOLDED
            val score = scoreOf(doc.title, doc.haystack, needle, hit, isExact = isExact)
            val label = when (doc.source) {
                Source.SCRIPTURE -> labels.scripture
                Source.SYNAXARIUM -> labels.synaxarium
                else -> labels.wudase
            }
            out += Result(
                source = doc.source,
                targetId = doc.targetId,
                sourceLabel = label,
                targetIndex = doc.targetIndex,
                title = doc.title.ifBlank { label },
                snippet = snip.text,
                snippetMatchStart = snip.matchStart,
                snippetMatchLen = snip.matchLen,
                route = doc.routeTo(hit),
                score = score,
                matchQuality = quality,
            )
        }
        return out.sortedByDescending { it.score }
    }

    /** Text of a section, searchable as one blob (title + subtitle + verses). */
    private fun haystackOf(section: Section): String = buildString {
        append(section.title)
        section.subtitle?.let { append(' '); append(it) }
        section.verses.forEach { append(' '); append(it) }
    }

    /** Fold + find [needle] in a section; returns a highlighted snippet or null. */
    private fun match(section: Section, needle: String, matchLen: Int): Snip? {
        val haystack = haystackOf(section)
        // fold is 1:1 per character, so an index in the folded text is the same
        // index in the original — the highlight lands on the real characters.
        val hit = fold(haystack).indexOf(needle)
        return if (hit >= 0) snippet(haystack, hit, matchLen) else null
    }

    private data class Snip(val text: String, val matchStart: Int, val matchLen: Int)

    private fun snippet(text: String, matchStart: Int, matchLen: Int, radius: Int = 28): Snip {
        val start = (matchStart - radius).coerceAtLeast(0)
        val end = (matchStart + matchLen + radius).coerceAtMost(text.length)
        val prefix = if (start > 0) "…" else ""
        val core = text.substring(start, end).replace('\n', ' ')
        val suffix = if (end < text.length) "…" else ""
        // Offsets stay exact because we don't trim (which would shift them).
        return Snip(prefix + core + suffix, prefix.length + (matchStart - start), matchLen)
    }
}
