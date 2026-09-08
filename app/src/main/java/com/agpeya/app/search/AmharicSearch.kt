package com.agpeya.app.search

import android.content.Context
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.ScriptureRepository
import com.agpeya.app.data.SynaxariumRepository
import com.agpeya.app.data.WudaseRepository
import com.agpeya.app.model.Section
import com.agpeya.app.model.SynaxariumDay
import com.agpeya.app.ui.common.EthiopianDate
import com.agpeya.app.ui.reading.geezNumeral
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

    /** Fold one character to its canonical homophone form, keeping the vowel order. */
    fun foldChar(c: Char): Char = when (val cp = c.code) {
        in 0x1220..0x1227 -> (0x1230 + (cp - 0x1220)).toChar() // ሠ-series -> ሰ-series
        in 0x1210..0x1217 -> (0x1200 + (cp - 0x1210)).toChar() // ሐ-series -> ሀ-series
        in 0x1280..0x1287 -> (0x1200 + (cp - 0x1280)).toChar() // ኀ-series -> ሀ-series
        in 0x12B8..0x12BF -> (0x1200 + (cp - 0x12B8)).toChar() // ኸ-series -> ሀ-series
        in 0x12D0..0x12D7 -> (0x12A0 + (cp - 0x12D0)).toChar() // ዐ-series -> አ-series
        in 0x1340..0x1347 -> (0x1338 + (cp - 0x1340)).toChar() // ፀ-series -> ጸ-series
        else -> c
    }

    fun fold(text: String): String = buildString(text.length) {
        for (c in text) append(foldChar(c))
    }

    /** Where a result came from — lets the UI label it and route the tap. */
    enum class Source { HOUR, PSALTER, SCRIPTURE, SYNAXARIUM, WUDASE, BOOK }

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
    )

    /**
     * How good a hit is, high to low.
     *
     * A match in the title is what someone searching a book name wants; a match
     * at the start of a word is a word, and a match inside one is a coincidence
     * of spelling that is still worth showing, last. An earlier hit in a long
     * document beats a later one, which only breaks ties.
     */
    internal fun scoreOf(title: String, haystack: String, needle: String, hit: Int): Int {
        val inTitle = fold(title).contains(needle)
        val boundary = hit == 0 || !haystack[hit - 1].isLetter()
        val base = when {
            inTitle && boundary -> 400
            inTitle -> 300
            boundary -> 200
            else -> 100
        }
        return base + (50 - hit / 40).coerceIn(0, 50)
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
            com.agpeya.app.data.CacheTrimmer.ensureRegistered(context)
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
            docs += synaxariumDocs(month, ethYear, SynaxariumRepository.month(context, month))
        }

        // The ሌሎች መጻሕፍት shelf, a document per chapter rather than per book:
        // ሥርዓተ ቅዳሴ is 230k characters, and a hit that lands on "the book" makes
        // the reader hunt through twenty-three chapters for the word.
        for (meta in com.agpeya.app.data.BookRepository.all(context)) {
            val book = com.agpeya.app.data.BookRepository.book(context, meta.id) ?: continue
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
                    val hay = entry.title + " " + entry.text
                    add(
                        Doc(
                            source = Source.SYNAXARIUM,
                            targetId = "$month-${day.day}-$entryIndex",
                            targetIndex = day.day,
                            title = com.agpeya.app.ui.gitsawe.cleanSynaxariumText(entry.title),
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
     * Read the bundled corpora ahead of the first keystroke. Without it the
     * first search builds the index inside itself while the screen, holding no
     * results yet, says "nothing found" — an answer, not a wait.
     */
    suspend fun warm(context: Context) {
        withContext(Dispatchers.Default) { index(context) }
    }

    suspend fun search(context: Context, rawQuery: String, labels: Labels): List<Result> {
        val query = rawQuery.trim()
        if (query.length < 2) return emptyList()
        val needle = fold(query)

        return withContext(Dispatchers.Default) {
            val results = mutableListOf<Result>()
            for (hour in ContentRepository.hours(context)) {
                hour.sections.forEachIndexed { index, section ->
                    match(section, needle, query.length)?.let { snip ->
                        results += Result(
                            source = Source.HOUR,
                            targetId = hour.id,
                            sourceLabel = hour.name,
                            targetIndex = index,
                            title = section.title,
                            snippet = snip.text,
                            snippetMatchStart = snip.matchStart,
                            snippetMatchLen = snip.matchLen,
                            score = scoreOf(section.title, haystackOf(section), needle, snip.matchStart),
                        )
                    }
                }
            }
            // Both editions of the Psalter. Only the Amharic was searched, so a
            // ግዕዝ verse — the text the ምስባክ is chanted from — could not be found
            // by its own words.
            listOf(false to labels.psalter, true to labels.psalterGeez).forEach { (geez, label) ->
                val psalms = runCatching {
                    if (geez) ScriptureRepository.psalms(context, geez = true)
                    else ContentRepository.psalter(context)
                }.getOrDefault(emptyList())
                psalms.forEachIndexed { index, psalm ->
                    match(psalm, needle, query.length)?.let { snip ->
                        results += Result(
                            source = Source.PSALTER,
                            targetId = if (geez) "geez" else "",
                            sourceLabel = label,
                            targetIndex = index,
                            title = psalm.title,
                            snippet = snip.text,
                            snippetMatchStart = snip.matchStart,
                            snippetMatchLen = snip.matchLen,
                            score = scoreOf(psalm.title, haystackOf(psalm), needle, snip.matchStart),
                        )
                    }
                }
            }

            results += searchBundled(context, needle, query.length, labels)
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
            ?: com.agpeya.app.ui.reading.parseGeezNumeral(digits)
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
        needle: String,
        matchLen: Int,
        labels: Labels,
    ): List<Result> {
        return searchDocs(index(context), needle, matchLen, labels)
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
    ): List<Result> {
        if (needle.isEmpty()) return emptyList()
        val out = mutableListOf<Result>()
        for (doc in docs) {
            val hit = doc.folded.indexOf(needle)
            if (hit < 0) continue
            val snip = snippet(doc.haystack, hit, matchLen)
            val score = scoreOf(doc.title, doc.haystack, needle, hit)
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
