package com.agpeya.app.ui.books

/**
 * Which words of a መልክእ stanza are written in red.
 *
 * The printed መልክእ and the manuscripts before them rubricate two things, and
 * this reproduces both:
 *
 *  1. The salutation that opens every stanza — ሰላም, and the ለ… phrase naming
 *     what is being greeted. "ሰላም ለዝክረ ስምከ ዘኢረከቡ ተፍጻሚተ።" reddens the first
 *     three words and leaves the rest of the line in ink.
 *  2. The Name of God, wherever it falls in the stanza.
 *
 * Deliberately NOT reddened: the hymn's own subject, which recurs in every
 * stanza as the one being addressed — መድኃኔ ዓለም in መልክአ መድኃኔ ዓለም, ሀብተ ማርያም in
 * መልክአ ሀብተ ማርያም. Red marks the Name, not the addressee, which is why [NAMES]
 * holds no bare ማርያም: in a መልክእ of a saint called ሀብተ ማርያም it would rubricate
 * the refrain forty times over.
 *
 * Kept free of Compose so the rule itself can be tested; the reader turns these
 * ranges into spans.
 */
object Rubrication {

    /**
     * The divine names, longest first so ማርያም ድንግል is matched before any
     * shorter form inside it could be.
     */
    private val NAMES = listOf(
        "እግዚአብሔር",
        "ማርያም ድንግል",
        "ድንግል ማርያም",
        "መንፈስ ቅዱስ",
        "ክርስቶስ",
        "ኢየሱስ",
    )

    /**
     * "I say", which some stanzas put between ሰላም and what is greeted.
     *
     * Matched on the first two letters because the scans spell it five ways —
     * ዕብል 79 times, እብል 25, then ዕብሎን, እብሎ, እብሎን. Keying on the whole word
     * would have caught a quarter of them.
     */
    private val SAY_HEADS = listOf("እብ", "ዕብ")

    /**
     * Words that begin a new clause rather than continuing the salutation.
     *
     * Drawn from the corpus, not guessed: across the 2,148 salutation stanzas on
     * the shelf, what follows the ለ… word is either a function word opening a
     * new clause — ዘ- and ወ- the relative and the conjunction, እም- and በ- and
     * ውስተ prepositions, እለ and ከመ and እንዘ conjunctions, a second ለ- the next
     * thing greeted — or a noun continuing the construct: ስም, ሥጋ, ርእስ, ነፍስ.
     * The first list stops the salutation; anything else is taken as part of it.
     *
     * Matched as prefixes. እም- is listed twice because it assimilates into the
     * word it governs — "from the womb" is written እማኅፀነ, with ማ and not ም — so
     * the bare እም spelling would miss it. Bare እ is deliberately absent: it
     * would swallow እግዚአብሔር.
     */
    private val CLAUSE_HEADS = listOf(
        "ዘ", "ወ", "ለ", "በ", "እም", "እማ", "እን", "እለ", "ከመ", "ውስተ", "ምስለ", "ኀበ", "ዲበ",
    )

    /** Red spans over [text], in order and non-overlapping. */
    fun redRanges(text: String): List<IntRange> {
        val ranges = salutation(text) + names(text)
        return merge(ranges)
    }

    /**
     * The opening salutation: ሰላም, an optional እብል, the ለ… word, and one more
     * word after it when that word continues the phrase rather than starting a
     * clause — "ለዝክረ ስምከ" is one thing greeted, "ለገጽከ ዘይቀትል" is two.
     */
    private fun salutation(text: String): List<IntRange> {
        val words = words(text)
        if (words.isEmpty()) return emptyList()
        if (!text.substring(words[0]).startsWith("ሰላም")) return emptyList()
        var last = 0
        fun word(i: Int) = words.getOrNull(i)?.let { text.substring(it) }
        val say = word(last + 1)
        if (say != null && SAY_HEADS.any { say.startsWith(it) }) last++
        val le = word(last + 1) ?: return listOf(words[0].first..words[last].last)
        if (!le.startsWith("ለ")) return listOf(words[0].first..words[last].last)
        last++
        val after = word(last + 1)
        if (after != null && CLAUSE_HEADS.none { after.startsWith(it) }) last++
        return listOf(words[0].first..words[last].last)
    }

    private fun names(text: String): List<IntRange> {
        val found = mutableListOf<IntRange>()
        for (name in NAMES) {
            var from = 0
            while (true) {
                val at = text.indexOf(name, from)
                if (at < 0) break
                found += at until (at + name.length)
                from = at + name.length
            }
        }
        return found
    }

    /** Word boundaries, as ranges into [text]. */
    private fun words(text: String): List<IntRange> =
        Regex("\\S+").findAll(text).map { it.range }.toList()

    private fun merge(ranges: List<IntRange>): List<IntRange> {
        if (ranges.isEmpty()) return ranges
        val sorted = ranges.sortedBy { it.first }
        val out = mutableListOf(sorted.first())
        for (r in sorted.drop(1)) {
            val last = out.last()
            if (r.first <= last.last + 1) out[out.size - 1] = last.first..maxOf(last.last, r.last)
            else out += r
        }
        return out
    }
}
