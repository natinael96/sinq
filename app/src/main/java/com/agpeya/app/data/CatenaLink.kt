package com.agpeya.app.data

/**
 * A verse's page on Catena Bible, where the Fathers comment on it.
 *
 * catenabible.com addresses a verse as `/{book}/{chapter}/{verse}` — Matthew
 * 5:3 is `/mt/5/3` — and carries a 76-book canon. Sinq bundles 93, so 17 books
 * have no page and get no link: the Ethiopian deuterocanon the site does not
 * reach (ሄኖክ, ኩፋሌ, ፫ መቃብያን, ዕዝራ ካልእ and the Daniel and Esther fragments) and
 * the nine broader-canon books. That is 19.5% of the app's verses; the other
 * 80.5% resolve.
 *
 * The abbreviations are read off the site's own navigation rather than guessed,
 * and CatenaLinkTest holds them to the bundled canon.
 */
object CatenaLink {

    private const val BASE = "https://catenabible.com"

    /**
     * The Psalter is the one book whose chapter numbers do not agree.
     *
     * Sinq numbers its psalms the way the Church does, following the LXX, and
     * Catena numbers them the way the Masoretic text does. መዝሙር ፳፪ — "the Lord
     * is my shepherd" — is Psalm 23 there, so a naive link would land a reader
     * on "my God, my God, why hast thou forsaken me" on the app's most-read
     * book.
     *
     * This table is not a rule of thumb: it is read out of the bundled Psalter,
     * whose headings carry the other number in parentheses — መዝሙር ፶ (፶፩) — for
     * 140 of the 150. The ten without one are psalms ፩–፰, where the two
     * numberings agree, and ፻፲፭ and ፻፵፯, which are the second half of a psalm
     * the Masoretic text joins to the one before it and so share its number.
     * The offset is not constant, which is why there is a table at all.
     */
    private val PSALM_TO_MASORETIC = intArrayOf(
        1, 2, 3, 4, 5, 6, 7, 8, 10, 11,
        12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
        22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
        32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
        42, 43, 44, 45, 46, 47, 48, 49, 50, 51,
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61,
        62, 63, 64, 65, 66, 67, 68, 69, 70, 71,
        72, 73, 74, 75, 76, 77, 78, 79, 80, 81,
        82, 83, 84, 85, 86, 87, 88, 89, 90, 91,
        92, 93, 94, 95, 96, 97, 98, 99, 100, 101,
        102, 103, 104, 105, 106, 107, 108, 109, 110, 111,
        112, 113, 114, 116, 116, 117, 118, 119, 120, 121,
        122, 123, 124, 125, 126, 127, 128, 129, 130, 131,
        132, 133, 134, 135, 136, 137, 138, 139, 140, 141,
        142, 143, 144, 145, 146, 147, 147, 148, 149, 150,
    )

    /**
     * The app's own book slug → the site's abbreviation, read off its navigation.
     *
     * Keyed by slug because that is what a loaded [com.agpeya.app.model.ScriptureBook]
     * carries; going through USFM would add a second table to keep in step.
     */
    private val ABBREVIATION = mapOf(
        "genesis" to "gn", "exodus" to "ex", "leviticus" to "lv",
        "numbers" to "nm", "deuteronomy" to "dt", "joshua" to "jo",
        "judges" to "jgs", "ruth" to "ru", "1-samuel" to "1sm",
        "2-samuel" to "2sm", "1-kings" to "1kgs", "2-kings" to "2kgs",
        "1-chronicles" to "1chr", "2-chronicles" to "2chr", "ezra" to "ezr",
        "nehemiah" to "neh", "esther" to "est", "job" to "jb",
        "psalms" to "ps", "proverbs" to "prv", "ecclesiastes" to "eccl",
        "song-of-solomon" to "sg", "isaiah" to "is", "jeremiah" to "jer",
        "lamentations" to "lam", "ezekiel" to "ez", "daniel" to "dn",
        "hosea" to "hos", "joel" to "jl", "amos" to "am",
        "obadiah" to "ob", "jonah" to "jon", "micah" to "mi",
        "nahum" to "na", "habakkuk" to "hb", "zephaniah" to "zep",
        "haggai" to "hg", "zechariah" to "zec", "malachi" to "mal",
        "tobit" to "tb", "yodit" to "jdt", "wisdom-of-solomon" to "ws",
        "sirach" to "sir", "baruch" to "bar", "teref-ermias" to "eoj",
        "1-maccabees" to "1mc", "2-maccabees" to "2mc", "ezra-sutuel" to "1esd",
        "prayer-of-manasseh" to "poman", "matthew" to "mt", "mark" to "mk",
        "luke" to "lk", "john" to "jn", "acts" to "acts",
        "romans" to "rom", "1-corinthians" to "1cor", "2-corinthians" to "2cor",
        "galatians" to "gal", "ephesians" to "eph", "philippians" to "phil",
        "colossians" to "col", "1-thessalonians" to "1thes", "2-thessalonians" to "2thes",
        "1-timothy" to "1tm", "2-timothy" to "2tm", "titus" to "ti",
        "philemon" to "phlm", "hebrews" to "heb", "james" to "jas",
        "1-peter" to "1pt", "2-peter" to "2pt", "1-john" to "1jn",
        "2-john" to "2jn", "3-john" to "3jn", "jude" to "jude",
        "revelation" to "rv",
    )

    /** True when this book has a page at all — used to hide the action, not to fail. */
    fun covers(slug: String): Boolean = slug in ABBREVIATION

    /**
     * The in-app route that shows [url], with [reference] for the bar.
     *
     * The page is opened by the app rather than handed to the browser so that
     * Catena can be asked for the early fathers alone; the ask is a cookie, and
     * a Custom Tab's cookies are Chrome's, not ours.
     */
    fun route(url: String, reference: String = ""): String =
        "catena?url=" + android.net.Uri.encode(url) +
            "&ref=" + android.net.Uri.encode(reference)

    /**
     * The page for one verse, or null when the book is not in Catena's canon.
     *
     * [slug] is the app's own book key. [chapter] and [verse] are the app's own
     * numbers; the Psalter's chapter is
     * translated on the way out and nothing else is.
     */
    fun url(slug: String, chapter: Int, verse: Int): String? {
        val abbr = ABBREVIATION[slug] ?: return null
        if (chapter < 1 || verse < 1) return null
        val ch = if (abbr == "ps") masoreticPsalm(chapter) ?: return null else chapter
        return "$BASE/$abbr/$ch/$verse"
    }

    /** Null rather than a wrong page if a psalm number is ever out of range. */
    internal fun masoreticPsalm(psalm: Int): Int? =
        PSALM_TO_MASORETIC.getOrNull(psalm - 1)

    private fun IntArray.getOrNull(index: Int): Int? =
        if (index in indices) this[index] else null
}
