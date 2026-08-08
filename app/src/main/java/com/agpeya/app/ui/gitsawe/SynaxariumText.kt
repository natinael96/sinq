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

/** Stray `<b>`-style markup left in the source (incl. malformed `< b >`, `</b >`,
 * `<b<`), then any remaining lone angle brackets — never legitimate in this data. */
private val B_TAG = Regex("<\\s*/?\\s*b\\s*[<>]")
private val ANGLE = Regex("[<>]")

/** A leading list marker some entries already carry: "1." / "2)" / keycap "1⃣" /
 * circled "①"–"⓾". Captured so the source's own numbering can be preserved —
 * blind stripping destroyed meaningful numbers (the four-seasons list). */
private val LIST_PREFIX = Regex("^(?:(\\d+)[.)⃣]|([①-⑳⓵-⓾]))\\s*")

/** The number a [LIST_PREFIX] match denotes, or null. */
private fun listPrefixNumber(m: MatchResult): Int? {
    m.groupValues[1].toIntOrNull()?.let { return it }
    val c = m.groupValues[2].firstOrNull() ?: return null
    return when (c.code) {
        in 0x2460..0x2473 -> c.code - 0x245F   // ① .. ⑳
        in 0x24F5..0x24FD -> c.code - 0x24F4   // ⓵ .. ⓽
        0x24FE -> 10                            // ⓾
        else -> null
    }
}

/** Split a cleaned line into (text without marker, source number or null). */
private fun stripListPrefix(line: String): Pair<String, Int?> {
    val m = LIST_PREFIX.find(line) ?: return line to null
    return line.removeRange(m.range) to listPrefixNumber(m)
}

/** The line that marks the start of the አርኬ hymn within an entry. */
private const val ARKE_MARKER = "አርኬ" // አርኬ

/** The 📖 that prefixes an entry whose body is a quoted scripture passage. */
private const val SCRIPTURE_MARKER = "📖" // 📖

enum class SynaxariumParaKind { NARRATIVE, ARKE_LABEL, ARKE_VERSE }

/** [sourceNumber] is the entry's own list numbering when the source line carried
 * one ("1.", "①", "1⃣") — the renderer shows it instead of a sequential count. */
data class SynaxariumPara(
    val kind: SynaxariumParaKind,
    val text: String,
    val sourceNumber: Int? = null,
)

/** Strip marker emojis, `<b>` markup debris, and C0 control characters (the data
 * contains stray backspaces), collapse the whitespace left behind, and trim. */
fun cleanSynaxariumText(raw: String): String =
    ANGLE.replace(B_TAG.replace(EMOJI.replace(raw, ""), " "), " ")
        .replace(Regex("[\\u0000-\\u0020]+"), " ")
        .trim()

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
                out += SynaxariumPara(SynaxariumParaKind.ARKE_LABEL, ARKE_MARKER)
            }
            inArke -> out += SynaxariumPara(SynaxariumParaKind.ARKE_VERSE, line)

            // Marker glued to the first verse on one line ("አርኬሰላም…" / "አርኬ ሰላም…").
            // The ሰላም/punctuation requirement keeps names like አርኬላዖስ from matching.
            line.startsWith(ARKE_MARKER) && isArkeRemainder(line.removePrefix(ARKE_MARKER)) -> {
                inArke = true
                out += SynaxariumPara(SynaxariumParaKind.ARKE_LABEL, ARKE_MARKER)
                val verse = line.removePrefix(ARKE_MARKER).trimStart(' ', '፡', '።', '፤', '፥')
                if (verse.isNotEmpty()) out += SynaxariumPara(SynaxariumParaKind.ARKE_VERSE, verse)
            }

            // Marker dangling at the end of a prose line ("… አሜን። አርኬ").
            line.endsWith(" $ARKE_MARKER") -> {
                val head = line.removeSuffix(ARKE_MARKER).trim()
                if (head.isNotEmpty()) {
                    val (text, num) = stripListPrefix(head)
                    out += SynaxariumPara(SynaxariumParaKind.NARRATIVE, text, num)
                }
                inArke = true
                out += SynaxariumPara(SynaxariumParaKind.ARKE_LABEL, ARKE_MARKER)
            }

            else -> {
                val (text, num) = stripListPrefix(line)
                out += SynaxariumPara(SynaxariumParaKind.NARRATIVE, text, num)
            }
        }
    }
    return out
}

private val ARKE_SEPARATORS = charArrayOf(' ', '፡', '።', '፤', '፥')

/** True when the text after a leading አርኬ is a hymn verse rather than the
 * continuation of a longer word (e.g. the saint's name አርኬላዖስ). */
private fun isArkeRemainder(rest: String): Boolean {
    val first = rest.firstOrNull() ?: return false
    return first in ARKE_SEPARATORS || rest.startsWith("ሰላም")
}
