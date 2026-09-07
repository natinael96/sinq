package com.agpeya.app.data

import com.agpeya.app.ui.reading.geezNumeral

/**
 * How a passage is named when it leaves a screen — shared, copied, written
 * about, or drawn on a card: the Amharic book name and Ge'ez numerals, the way
 * every other page of the app writes a reference.
 *
 * The content pipeline writes the hours' own references in Latin ("Ps 1",
 * "John 1:1-17"), which is what a verse shared out of ጸሎተ ነግህ used to carry
 * into a chat under Amharic text. [fromLatin] reads those back; everything
 * else in the app already knows its book in Amharic and calls [of] directly.
 */
object Citation {

    /** `የሉቃስ ወንጌል ፲፥፴፰–፵፪` — verses omitted when the whole chapter is meant. */
    fun of(bookAm: String, chapter: Int, start: Int? = null, end: Int? = null): String =
        buildString {
            append(bookAm)
            append(' ')
            append(geezNumeral(chapter))
            if (start != null && start >= 1) {
                append('፥')
                append(geezNumeral(start))
                if (end != null && end > start) {
                    append('–')
                    append(geezNumeral(end))
                }
            }
        }

    private val LATIN = Regex(
        """^\s*([1-3]?\s?[A-Za-z]+)\s+(\d{1,3})(?::(\d{1,3})(?:\s*[-–]\s*(\d{1,3}))?)?\s*$""",
    )

    private val PSALM_WORDS = setOf("ps", "psalm", "psalms")

    /** How an English book name is keyed, applied on both sides of the map. */
    fun englishKey(name: String): String = name.replace(" ", "").lowercase()

    /**
     * A Latin reference as the Church names it, or null when it cannot be read —
     * a caller keeps what it had rather than showing a guess.
     *
     * [booksByEnglishName] maps a canon `name_en`, lowercased, to the bundle's
     * Amharic name; [psalmName] names a single psalm ("መዝሙር"), since the Psalter
     * is not in that catalogue.
     */
    fun fromLatin(
        reference: String,
        psalmName: String,
        booksByEnglishName: Map<String, String>,
    ): String? {
        val match = LATIN.find(reference) ?: return null
        val (rawBook, rawChapter, rawStart, rawEnd) = match.destructured
        val chapter = rawChapter.toIntOrNull() ?: return null
        val book = englishKey(rawBook)
        val nameAm = if (book in PSALM_WORDS) psalmName else booksByEnglishName[book] ?: return null
        return of(nameAm, chapter, rawStart.toIntOrNull(), rawEnd.toIntOrNull())
    }
}
