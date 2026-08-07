package com.agpeya.app.data

import com.agpeya.app.model.VerseRef

/**
 * Where a ግጻዌ (Gitsawe) reading reference should open. Psalms land in the
 * bundled Psalter; New Testament references land in the bundled scripture reader.
 */
sealed interface ReadingTarget {

    /**
     * A psalm in the Psalter. [sectionIndex] is the index the `psalter?section=`
     * route expects (psalm number − 1); [startVerse]/[endVerse] let a future
     * verse-level scroll land on the cited verses.
     */
    data class Psalm(
        val number: Int,
        val sectionIndex: Int,
        val startVerse: Int?,
        val endVerse: Int?,
    ) : ReadingTarget

    /** A New Testament passage, addressed by bundled book key + chapter + range. */
    data class NtPassage(
        val bookKey: String,
        val chapter: Int,
        val start: Int?,
        val end: Int?,
    ) : ReadingTarget
}

/**
 * Turns a Gitsawe [VerseRef] into a navigable [ReadingTarget]. Psalms (መዝሙረ
 * ዳዊት) route to the Psalter — Gitsawe and the Psalter share the same Ethiopian
 * (LXX) psalm numbering, so the chapter number is the psalm number. Everything
 * else goes through [ScriptureRepository.resolveBookKey]. Returns null when the
 * reference has no chapter or the book can't be identified.
 */
object GitsaweLinks {

    fun target(verse: VerseRef): ReadingTarget? {
        val title = verse.bookTitle ?: return null
        val chapter = verse.chapter ?: return null
        if (isPsalms(title)) {
            return ReadingTarget.Psalm(
                number = chapter,
                sectionIndex = chapter - 1,
                startVerse = verse.start,
                endVerse = verse.end,
            )
        }
        val key = ScriptureRepository.resolveBookKey(title) ?: return null
        return ReadingTarget.NtPassage(key, chapter, verse.start, verse.end)
    }

    /** Navigation route for a resolved target. */
    fun route(target: ReadingTarget): String = when (target) {
        is ReadingTarget.Psalm ->
            "psalter?section=${target.sectionIndex}" +
                "&start=${target.startVerse ?: -1}&end=${target.endVerse ?: -1}"
        is ReadingTarget.NtPassage ->
            "scripture/${target.bookKey}/${target.chapter}" +
                "?start=${target.start ?: -1}&end=${target.end ?: -1}"
    }

    fun isPsalms(title: String): Boolean {
        val t = title.replace(" ", "")
        return t.contains("ዳዊት") || t.contains("መዝሙረ")
    }
}
