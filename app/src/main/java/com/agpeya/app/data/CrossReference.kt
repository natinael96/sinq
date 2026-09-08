package com.agpeya.app.data

/**
 * The edition's own cross references, read into something tappable.
 *
 * The Amharic 1980 prints them the way a printed Bible does — an abbreviated
 * book, a chapter, a verse, and a book carried over from the reference before
 * it: `2ጢሞ 4፥5፤ 4፥17፤ የሐዋ 3፥18።` is three references to two books. 22,905 of
 * the bundle's verses carry them and none of it ever reached the page.
 *
 * Numbers here are Arabic even though every number the app prints is Ge'ez;
 * that is how the source writes them, and [Citation] does the converting.
 */
object CrossReference {

    /** One reference, resolved far enough to open. */
    data class Ref(
        val bookKey: String,
        val bookName: String,
        val chapter: Int,
        val verse: Int,
    ) {
        val route: String get() = "scripture/$bookKey/$chapter?start=$verse&end=$verse"
    }

    /**
     * Every citation in a string, in order. Not anchored: a verse can carry
     * more than one reference list and they arrive joined, so a rule that only
     * matched from the start read the first citation of each and dropped the
     * rest — and the bare "chapter፥verse" that followed then inherited whatever
     * book came before it, which put መዝሙር ፴፰፥፲፪ under ሚክያስ.
     *
     * The book class is Ethiopic syllables only (U+1200–U+135A). Written as
     * ሀ-፿ it also swallowed the ፤ that separates the citations, and every
     * reference after the first was then dropped as an unknown book.
     */
    private val ATOM = Regex("""(?:(\d?[\u1200-\u135A]+)\s*)?(\d{1,3})[፥:](\d{1,3})""")

    /**
     * Parse a printed reference string. [names] maps a book slug to the Amharic
     * name, so a chip can say "ማቴዎስ ፱፥፴፯" rather than the abbreviation.
     *
     * Anything unrecognised is dropped rather than guessed at: a reference that
     * opens the wrong chapter is worse than one that is not offered.
     */
    fun parse(raw: String, names: Map<String, String> = emptyMap()): List<Ref> {
        val out = mutableListOf<Ref>()
        var lastBook: String? = null
        var lastAbbreviation: String? = null
        for (m in ATOM.findAll(raw)) {
            val abbreviation = m.groupValues[1].takeIf { it.isNotBlank() }
            val key = when {
                // A citation with no book of its own continues the last one.
                abbreviation == null -> lastBook ?: continue
                // One the table does not know is dropped, and does not become
                // the book the citations after it inherit.
                else -> BOOKS[abbreviation] ?: continue
            }
            lastBook = key
            if (abbreviation != null) lastAbbreviation = abbreviation
            val chapter = m.groupValues[2].toIntOrNull() ?: continue
            val verse = m.groupValues[3].toIntOrNull() ?: continue
            out += Ref(
                bookKey = key,
                bookName = names[key] ?: abbreviation ?: lastAbbreviation.orEmpty(),
                chapter = chapter,
                verse = verse,
            )
        }
        return out
    }

    /**
     * Every abbreviation the bundle uses, mapped to its book. Derived by
     * matching each one against the edition's own book names; all 73 resolve,
     * covering 55,128 of the printed references. Numbered books are keyed with
     * the source's leading digit — "1ነገ" is ቀዳማዊ ነገሥት.
     */
    private val BOOKS: Map<String, String> = mapOf(
        "ኢሳይ" to "isaiah",
        "መዝሙ" to "psalms",
        "ኤርም" to "jeremiah",
        "ማቴዎ" to "matthew",
        "ዘዳግ" to "deuteronomy",
        "ሕዝቅ" to "ezekiel",
        "ዘጸአ" to "exodus",
        "የሐዋ" to "acts",
        "ዮሐን" to "john",
        "ሉቃስ" to "luke",
        "ዘፍጥ" to "genesis",
        "ዘኊል" to "numbers",
        "ዘሌዋ" to "leviticus",
        "ኢዮብ" to "job",
        "ሮሜ" to "romans",
        "ምሳሌ" to "proverbs",
        "1ነገ" to "1-kings",
        "1ሳሙ" to "1-samuel",
        "ራዕይ" to "revelation",
        "ኢያሱ" to "joshua",
        "1ቆሮ" to "1-corinthians",
        "2ነገ" to "2-kings",
        "2ዜና" to "2-chronicles",
        "ማርቆ" to "mark",
        "ዕብራ" to "hebrews",
        "2ሳሙ" to "2-samuel",
        "1ዜና" to "1-chronicles",
        "መሳፍ" to "judges",
        "ዳንኤ" to "daniel",
        "2ቆሮ" to "2-corinthians",
        "ኤፌሶ" to "ephesians",
        "ዘካር" to "zechariah",
        "ሆሴዕ" to "hosea",
        "ነህም" to "nehemiah",
        "1ጴጥ" to "1-peter",
        "ገላት" to "galatians",
        "1ጢሞ" to "1-timothy",
        "አሞጽ" to "amos",
        "ቆላስ" to "colossians",
        "1ዮሐ" to "1-john",
        "ዕዝራ" to "ezra",
        "ያዕቆ" to "james",
        "1ተሰ" to "1-thessalonians",
        "ሚክያ" to "micah",
        "ፊልጵ" to "philippians",
        "መክብ" to "ecclesiastes",
        "2ጢሞ" to "2-timothy",
        "ኢዩኤ" to "joel",
        "ሚልክ" to "malachi",
        "2ጴጥ" to "2-peter",
        "አስቴ" to "esther",
        "ቲቶ" to "titus",
        "2ተሰ" to "2-thessalonians",
        "መኃል" to "song-of-solomon",
        "ሶፎን" to "zephaniah",
        "ዕንባ" to "habakkuk",
        "ሩት" to "ruth",
        "ናሆም" to "nahum",
        "ይሁዳ" to "jude",
        "ሐጌ" to "haggai",
        "ዮናስ" to "jonah",
        "አብድ" to "obadiah",
        "ፊልሞ" to "philemon",
        "2ዮሐ" to "2-john",
        "3ዮሐ" to "3-john",
        "ሲራክ" to "sirach",
        "ጥበብ" to "wisdom-of-solomon",
        "1መቃ" to "1-maccabees",
        "2መቃ" to "2-maccabees",
        "አስቴር" to "esther",
        "ጦቢት" to "tobit",
        "ዮዲት" to "yodit",
        "ባሮክ" to "baruch",
    )
}
