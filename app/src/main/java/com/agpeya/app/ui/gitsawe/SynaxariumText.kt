package com.agpeya.app.ui.gitsawe

/**
 * Turns the raw synaxarium (ስንክሳር) entry text — a run of `\n`-separated lines
 * peppered with editorial emojis — into clean, typed paragraphs for rendering:
 * numbered narrative prose, and the አርኬ hymn set apart at the end.
 */

/**
 * Emoji / dingbat symbols used as line markers: arrows (U+2190–U+21FF), misc
 * symbols & dingbats (U+2600–U+27BF), symbols-and-arrows (U+2B00–U+2BFF),
 * variation selectors (U+FE00–U+FE0F), and the astral-plane emoji block
 * (U+1F000–U+1FAFF, matched by code point — Java regex is code-point-aware, so
 * a surrogate-pair class would never fire). Ethiopic (U+1200–U+137F,
 * U+2D80–U+2DDF, U+AB00–U+AB2F) sits outside every range, so Ge'ez is untouched.
 */
private val EMOJI = Regex("[←-⇿☀-➿⬀-⯿︀-️\\x{1F000}-\\x{1FAFF}]")

/** A leading "1." / "2)" list marker some entries already carry — dropped so the
 * rendered Ge'ez numeral doesn't double it up ("፩ 1.ቅድስት …"). */
private val LIST_PREFIX = Regex("^\\d+[.)]\\s*")

/** The line that marks the start of the አርኬ hymn within an entry. */
private const val ARKE_MARKER = "አርኬ" // አርኬ

/** The 📖 that prefixes an entry whose body is a quoted scripture passage. */
private const val SCRIPTURE_MARKER = "📖" // 📖

enum class SynaxariumParaKind { NARRATIVE, ARKE_LABEL, ARKE_VERSE }

data class SynaxariumPara(val kind: SynaxariumParaKind, val text: String)

/** Strip marker emojis, collapse the whitespace they leave behind, and trim. */
fun cleanSynaxariumText(raw: String): String =
    EMOJI.replace(raw, "").replace(Regex("[ \t]+"), " ").trim()

/** True when [rawTitle] marks a scripture-quote entry (📖 prefix in the source). */
fun isScriptureEntry(rawTitle: String): Boolean =
    rawTitle.trimStart().startsWith(SCRIPTURE_MARKER)

/**
 * Split [rawText] into display paragraphs. Everything before an `አርኬ` line is
 * narrative; the marker becomes an [SynaxariumParaKind.ARKE_LABEL] and every
 * line after it an [SynaxariumParaKind.ARKE_VERSE].
 */
fun parseSynaxarium(rawText: String): List<SynaxariumPara> {
    val out = mutableListOf<SynaxariumPara>()
    var inArke = false
    for (rawLine in rawText.split('\n')) {
        val line = cleanSynaxariumText(rawLine)
        if (line.isEmpty()) continue
        when {
            line == ARKE_MARKER -> {
                inArke = true
                out += SynaxariumPara(SynaxariumParaKind.ARKE_LABEL, line)
            }
            inArke -> out += SynaxariumPara(SynaxariumParaKind.ARKE_VERSE, line)
            else -> out += SynaxariumPara(
                SynaxariumParaKind.NARRATIVE,
                line.replaceFirst(LIST_PREFIX, ""),
            )
        }
    }
    return out
}
