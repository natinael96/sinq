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
     * Where the text is being read, which decides how much of it reddens.
     *
     * The Name of God is red wherever it falls. A saint's name is red only in
     * the three places whose whole subject is the saint — a መልክእ, the ስንክሳር's
     * commemoration, a ማኅሌት order — and stays in ink everywhere else, because a
     * Psalm that reddened every ዳዊት and a Gospel that reddened every ጴጥሮስ would
     * be marking the narrative, not the holy.
     */
    enum class Scope { GENERAL, MELKIE, SINKSAR, MAHLET }

    /**
     * The divine names, longest first so a form is never matched inside a
     * longer one that contains it — ማርያም ድንግል before ማርያም, እግዚእነ before እግዚእ.
     *
     * Four words are deliberately absent, all for one reason: each is an
     * ordinary noun as often as it is the Name, and a list cannot tell the two
     * apart.
     *
     *  - **ድንግል** on its own is "virgin" and reddens ordinary description. It
     *    is the Name only standing with ማርያም, so the two paired forms are
     *    listed and the bare word is not.
     *  - **ጌታ** is "master" as often as it is the Lord — much of its 2,414
     *    occurrences in the Amharic Bible belong to a household, not to God.
     *  - **አምላክ** is "god", the false ones included: ባዕድ አምላክ and አማልክት would
     *    redden with the true One.
     *  - **እግዚእ** is "lord" in the same generic way, and it matches inside
     *    every inflection — እግዚእየ, እግዚእነ — so a wrong match is never isolated.
     *
     * ወላዲተ አምላክ and እግዚእነ stay, because the compound is unambiguous where the
     * bare word is not.
     */
    private val NAMES = listOf(
        "እግዚአብሔር",
        "ማርያም ድንግል",
        "ድንግል ማርያም",
        "ወላዲተ አምላክ",
        "መንፈስ ቅዱስ",
        "መድኃኔ ዓለም",
        "ኪዳነ ምሕረት",
        "እግዝእትነ",
        "ክርስቶስ",
        "አማኑኤል",
        "እግዚእነ",
        "ኢየሱስ",
        "እግዚኦ",
        "አዶናይ",
        "ሥላሴ",
        "ማርያም",
    )

    /**
     * The saints the shelf, the ስንክሳር and the ማኅሌት name.
     *
     * Longest first for the same reason, and ገብረ መንፈስ ቅዱስ ahead of መንፈስ ቅዱስ in
     * [NAMES] is handled by [merge]: overlapping spans become one, so whichever
     * matched first the whole name reddens.
     */
    private val SAINTS = listOf(
        "ገብረ መንፈስ ቅዱስ",
        "ተክለ ሃይማኖት",
        "ክርስቶስ ሠምራ",
        "ሀብተ ማርያም",
        "እስጢፋኖስ",
        "መርቆሬዎስ",
        "ኤልሳቤጥ",
        "ገብርኤል",
        "ሚካኤል",
        "ሩፋኤል",
        "ዑራኤል",
        "ሱራፌል",
        "ኪሩቤል",
        "ጊዮርጊስ",
        "አርሴማ",
        "ጴጥሮስ",
        "ጳውሎስ",
        "ማርቆስ",
        "ዮሐንስ",
        "አረጋዊ",
        "ኢያቄም",
        "አብርሃም",
        "ይስሐቅ",
        "ያዕቆብ",
        "ኤልያስ",
        "ቂርቆስ",
        "ያሬድ",
        "ዮሴፍ",
        "ዳዊት",
        "ኢዮብ",
        "ሙሴ",
        "ሐና",
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

    private val LITURGICAL_ROLES = listOf("ይካ፡", "ይዲ፡", "ይሕ፡", "ይካ", "ይዲ", "ይሕ")

    /** Red spans over [text], in order and non-overlapping. */
    fun redRanges(text: String, scope: Scope = Scope.MELKIE): List<IntRange> {
        // The salutation formula is the መልክእ's and the ስንክሳር አርኬ's — the same
        // verse form, so the same rule reads both.
        val opening = when (scope) {
            Scope.MELKIE, Scope.SINKSAR -> salutation(text)
            else -> emptyList()
        }
        val people = when (scope) {
            Scope.MELKIE, Scope.SINKSAR, Scope.MAHLET -> match(text, SAINTS)
            Scope.GENERAL -> emptyList()
        }
        val liturgical = liturgicalRoles(text)
        return merge(opening + match(text, NAMES) + people + liturgical)
    }

    private fun liturgicalRoles(text: String): List<IntRange> {
        for (prefix in LITURGICAL_ROLES) {
            if (text.startsWith(prefix)) {
                return listOf(0 until prefix.length)
            }
        }
        return emptyList()
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

    private fun match(text: String, names: List<String>): List<IntRange> {
        val found = mutableListOf<IntRange>()
        for (name in names) {
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
