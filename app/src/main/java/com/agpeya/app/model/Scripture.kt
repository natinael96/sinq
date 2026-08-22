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
)

/** One verse: its number [n] (versification can have gaps) and [text]. */
@Serializable
data class ScriptureVerse(
    val n: Int,
    val text: String,
)
