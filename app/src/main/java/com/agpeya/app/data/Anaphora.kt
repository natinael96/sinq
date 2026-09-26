package com.agpeya.app.data

/**
 * The ቅዳሴ named on a day, normalised.
 *
 * The lectionary writes the anaphora as free text, and across the bundled year
 * it does so 120 different ways for what are only thirteen anaphoras: with and
 * without the ዘ- prefix, as the council's number or as its byname, with the
 * incipit in brackets, with ጐ or ጎ, and with a handful of plain typos. The
 * ግጻዌ header printed all of it verbatim, so the same liturgy appeared under
 * four names in one week.
 *
 * What the reader wants is which anaphora is sung. The bracketed word is the
 * incipit — the line it opens on — and it is kept beside the name rather than
 * folded into it.
 */
object Anaphora {

    /** A day's ቅዳሴ: which anaphora, and the incipit the book prints with it. */
    data class Named(val name: String, val incipit: String? = null) {
        /** "ዘሠለስቱ ምዕት · ግሩም" — what the header shows. */
        val label: String get() = listOfNotNull(name, incipit).joinToString("  ·  ")
    }

    /**
     * The thirteen, each with the words that identify it. The council of ፫፻፲፰
     * and the council of ፫፻ are both written for ሠለስቱ ምዕት, whose byname is
     * ግሩም; ግሩም alone therefore names it too.
     */
    private val CANON: List<Pair<String, List<String>>> = listOf(
        "ዘእግዝእትነ ማርያም" to listOf("እግዝእትነ", "እግዝእትን", "ማርያም", "ጐሥዓ", "ጎሥዓ", "ጐሥሣ"),
        "ዘእግዚእነ" to listOf("እግዚእነ"),
        "ዘሐዋርያት" to listOf("ሐዋርያት"),
        "ዘዮሐንስ ወልደ ነጎድጓድ" to listOf("ወልደ ነጎድጓድ", "ወልደ ነጐድጓድ", "ነጉድንድ"),
        "ዘሠለስቱ ምዕት" to listOf("፫፻፲፰", "፫፻", "፻፶", "ሠለስቱ ምዕት", "ሠለስቱ ምእት", "ግሩም", "ጋሩም"),
        "ዘባስልዮስ" to listOf("ባስልዮስ"),
        "ዘጎርጎርዮስ" to listOf("ጎርጎርዮስ", "ጐርጐርዮስ"),
        "ዘኤጲፋንዮስ" to listOf("ኤጲፋንዮስ", "ኢዲፋንዮስ", "ኤጵፋንዮስ"),
        "ዘዮሐንስ አፈወርቅ" to listOf("አፈ ወርቅ", "አፈወርቅ"),
        "ዘቄርሎስ" to listOf("ቄርሎስ"),
        "ዘያዕቆብ ዘሥሩግ" to listOf("ያዕቆብ", "ተንሥኡ"),
        "ዘዲዮስቆሮስ" to listOf("ዲዮስቆሮስ"),
        "ዘአትናቴዎስ" to listOf("አትናቴዎስ"),
    )

    /** Which anaphora [raw] names, with its incipit; null when nothing matches. */
    fun of(raw: String): Named? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val incipit = INCIPIT.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
        val body = text.replace(INCIPIT, " ")
        // The name first, then the bracket: the source sometimes writes the
        // incipit as the whole entry and the anaphora inside the brackets —
        // "ተንሥኡ (ያዕቆብ ዘሥሩግ)" is the same liturgy the other way round. An
        // incipit that belongs to exactly one anaphora names it either way.
        val name = match(body) ?: incipit?.let { match(it) }
        // What matches nothing is passed through rather than guessed at.
            ?: return Named(name = tidy(text))
        return Named(name = name, incipit = incipit?.takeIf { match(it) != name || body != " " })
    }

    /**
     * Every distinct ቅዳሴ of a day, in the order the book names them, with
     * duplicates from the three services collapsed. "ዘእግዝእትነ አው ግሩም" is one
     * day offering a choice of two, and reads as two.
     */
    fun allOf(raws: List<String>): List<Named> =
        raws.flatMap { it.split(" አው ", " ወይም ") }
            .mapNotNull { of(it) }
            .distinctBy { it.name }

    private fun match(text: String): String? =
        CANON.firstOrNull { (_, keys) -> keys.any { text.contains(it) } }?.first

    private val INCIPIT = Regex("""\(([^)]*)\)""")

    /** Trailing ። and stray spaces, which the source uses inconsistently. */
    private fun tidy(text: String): String =
        text.replace("።", " ").replace(Regex("\\s+"), " ").trim()
}
