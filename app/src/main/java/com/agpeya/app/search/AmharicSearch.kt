package com.agpeya.app.search

import android.content.Context
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.ScriptureRepository
import com.agpeya.app.data.SeatatRepository
import com.agpeya.app.data.SynaxariumRepository
import com.agpeya.app.data.WudaseRepository
import com.agpeya.app.model.Section
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
    enum class Source { HOUR, PSALTER, SCRIPTURE, SYNAXARIUM, WUDASE, SEATAT }

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
    )

    /** Per-source result cap, so one huge corpus can't crowd out the others. */
    private const val PER_SOURCE_LIMIT = 40

    /**
     * One searchable unit of a bundled corpus, with its haystack pre-folded.
     *
     * The scripture, ስንክሳር and ውዳሴ ማርያም corpora together are megabytes of text;
     * folding them on every keystroke would allocate all of it each time. They're
     * folded once into [Doc]s and kept for the process lifetime instead.
     */
    private class Doc(
        val source: Source,
        val targetId: String,
        val targetIndex: Int,
        val title: String,
        val route: String,
        val haystack: String,
        val folded: String,
    )

    @Volatile private var docIndex: List<Doc>? = null

    /** Build (once) the folded index over the bundled corpora. */
    private suspend fun index(context: Context): List<Doc> =
        docIndex ?: buildIndex(context).also { docIndex = it }

    private suspend fun buildIndex(context: Context): List<Doc> {
        val docs = mutableListOf<Doc>()

        for (meta in ScriptureRepository.books(context)) {
            val book = ScriptureRepository.book(context, meta.key) ?: continue
            for (chapter in book.chapters) {
                val hay = chapter.verses.joinToString(" ") { it.text }
                docs += Doc(
                    source = Source.SCRIPTURE,
                    targetId = meta.key,
                    targetIndex = chapter.chapter,
                    title = "${book.nameAm} ${geezNumeral(chapter.chapter)}",
                    route = "scripture/${meta.key}/${chapter.chapter}",
                    haystack = hay,
                    folded = fold(hay),
                )
            }
        }

        // The ስንክሳር is keyed by Ethiopian month/day; the route resolves to the
        // Gregorian date that day falls on in the current Ethiopian year.
        val ethYear = EthiopianDate.from(LocalDate.now()).year
        for (month in 1..13) {
            for (day in SynaxariumRepository.month(context, month)) {
                val epochDay = runCatching {
                    EthiopianDate(ethYear, month, day.day).toGregorian().toEpochDay()
                }.getOrNull() ?: continue
                for (entry in day.entries) {
                    val hay = entry.title + " " + entry.text
                    docs += Doc(
                        source = Source.SYNAXARIUM,
                        targetId = "$month-${day.day}",
                        targetIndex = day.day,
                        title = com.agpeya.app.ui.gitsawe.cleanSynaxariumText(entry.title),
                        route = "synaxarium/$epochDay",
                        haystack = hay,
                        folded = fold(hay),
                    )
                }
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

        // ሰዓታት: both halves of every paired line are searchable, so a query
        // in either Ge'ez or Amharic finds the section it belongs to. The
        // printed text separates every word with the traditional ፡; queries
        // are typed with plain spaces, so the haystack (and thus the folded
        // index AND the snippet shown) is normalized to spaces. The `*`
        // typo-review flag is display-only and stripped the same way.
        for (section in SeatatRepository.load(context).sections) {
            val hay = section.lines.joinToString(" ") { line ->
                if (line.am.isBlank()) line.ge else "${line.ge} ${line.am}"
            }.replace("*", "").replace("፡", " ").replace(Regex("\\s+"), " ")
            docs += Doc(
                source = Source.SEATAT,
                targetId = section.id,
                targetIndex = 0,
                title = section.titleGe.ifBlank { section.titleAm },
                route = "seatat?sec=${section.id}",
                haystack = hay,
                folded = fold(hay),
            )
        }
        return docs
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
                        )
                    }
                }
            }
            ContentRepository.psalter(context).forEachIndexed { index, psalm ->
                match(psalm, needle, query.length)?.let { snip ->
                    results += Result(
                        source = Source.PSALTER,
                        targetId = "",
                        sourceLabel = labels.psalter,
                        targetIndex = index,
                        title = psalm.title,
                        snippet = snip.text,
                        snippetMatchStart = snip.matchStart,
                        snippetMatchLen = snip.matchLen,
                    )
                }
            }

            results += searchBundled(context, needle, query.length, labels)
            results
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

    /** Split "ሉቃስ 10" into its book part and trailing number; null if it isn't
     *  shaped like a reference at all. Pure, so it is unit-testable. */
    internal fun parseReference(raw: String): Pair<String, Int>? {
        val m = Regex("^(.*?)[\\s:፥]*(\\d{1,3})\\s*$").find(raw.trim()) ?: return null
        val number = m.groupValues[2].toIntOrNull() ?: return null
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
        val scripture: String,
        val synaxarium: String,
        val wudase: String,
        val seatat: String,
    )

    /** One pass over the pre-folded index, capped per source. */
    private suspend fun searchBundled(
        context: Context,
        needle: String,
        matchLen: Int,
        labels: Labels,
    ): List<Result> {
        val counts = mutableMapOf<Source, Int>()
        val out = mutableListOf<Result>()
        for (doc in index(context)) {
            if ((counts[doc.source] ?: 0) >= PER_SOURCE_LIMIT) continue
            val hit = doc.folded.indexOf(needle)
            if (hit < 0) continue
            val snip = snippet(doc.haystack, hit, matchLen)
            val label = when (doc.source) {
                Source.SCRIPTURE -> labels.scripture
                Source.SYNAXARIUM -> labels.synaxarium
                Source.SEATAT -> labels.seatat
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
                route = doc.route,
            )
            counts[doc.source] = (counts[doc.source] ?: 0) + 1
        }
        return out
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
