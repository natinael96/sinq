package com.agpeya.app.model

import kotlinx.serialization.Serializable

/** Reader-facing projection of the unified 80-weahadu Bible assets. */

/** Lightweight manifest entry — enough to list/route without loading a book. */
@Serializable
data class ScriptureBookMeta(
    val number: Int,
    val key: String,
    val nameAm: String,
    val nameEn: String,
    val chapters: Int,
    val testament: String = "new",
    val section: String = "",
)

@Serializable
data class ScriptureBook(
    val number: Int,
    val key: String,
    val nameAm: String,
    val nameEn: String,
    val chapters: List<ScriptureChapter>,
)

@Serializable
data class ScriptureChapter(
    val chapter: Int,
    val verses: List<ScriptureVerse>,
    /**
     * Headings the edition prints above a verse, keyed by the verse they come
     * before — a psalm's superscription, the note that opens ሲኖዶስ. The
     * bundle's "ምዕራፍ ፲" headings are not here: the page already says which
     * chapter it is, and printing it twice is not a heading.
     */
    val headings: Map<Int, String> = emptyMap(),
)

/** One verse: its number [n] (versification can have gaps) and [text]. */
@Serializable
data class ScriptureVerse(
    val n: Int,
    val text: String,
    /**
     * The edition's own cross references for this verse — "ማቴዎ ፱፥፴፯፤ ዮሐን
     * ፬፥፴፭" — as printed, not as routes. 22,905 verses carry them and the app
     * had been dropping every one at parse.
     */
    val refs: String? = null,
)
