package com.agpeya.app.model

import kotlinx.serialization.Serializable

/**
 * ሰዓታት (Seatat) — the Horologion, the prayers of the hours, bundled under
 * assets/content/seatat/. The text is Ge'ez-first with a line-by-line Amharic
 * translation: each [SeatatLine] pairs one Ge'ez line with its Amharic, so the
 * one-to-one relationship lives in the data itself and can never drift.
 */
@Serializable
data class SeatatLine(
    /** The Ge'ez line — the prayer itself. Never blank. */
    val ge: String,
    /** Its Amharic translation; blank when a line has none. */
    val am: String = "",
)

@Serializable
data class SeatatSection(
    val id: String,
    /** Short chip label for the section strip (ጠዋት, ቀትር, ማታ, ሌሊት). */
    val label: String = "",
    val titleGe: String = "",
    val titleAm: String = "",
    val lines: List<SeatatLine> = emptyList(),
)

@Serializable
data class SeatatContent(
    val contentVersion: Int = 1,
    val sections: List<SeatatSection> = emptyList(),
)
