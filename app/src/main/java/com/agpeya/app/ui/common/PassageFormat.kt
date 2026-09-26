package com.agpeya.app.ui.common

import kotlinx.serialization.Serializable

/**
 * A selected passage, ready to leave the app.
 *
 * [verses] is the text as it was read, each with the number it carries in the
 * book — null for a reader whose unit is a paragraph, like ውዳሴ ማርያም or ስንክሳር.
 * [citation] is the Church's own name for the passage, built by
 * [com.agpeya.app.data.Citation]; [edition] names the translation where more
 * than one exists, which on this shelf means the Psalter's ግዕዝ and አማርኛ.
 */
data class Passage(
    val verses: List<Pair<Int?, String>>,
    val citation: String? = null,
    val edition: String? = null,
)

/**
 * What travels with a copied verse. Olive Tree and Logos both make this the
 * person's choice and remember it, and they are right to: someone quoting into
 * a sermon wants the reference, and someone quoting into a message does not
 * want three numbers in front of the words.
 *
 * The signature is not here. A share leaves for someone else and says where it
 * came from; a copy is the person quoting Scripture into their own notes, and
 * the app's name in it is only something to delete.
 */
@Serializable
data class CopyFormat(
    val verseNumbers: Boolean = true,
    val reference: Boolean = true,
    val edition: Boolean = true,
)

/**
 * The one place a passage becomes text. Copy, share, the image card's title and
 * the marks list all come through here, so a verse says the same thing however
 * it leaves.
 */
object PassageFormat {

    /** The verses alone, one per line, numbered when the reader asked for it. */
    fun body(passage: Passage, format: CopyFormat): String =
        passage.verses.joinToString("\n") { (n, text) ->
            if (format.verseNumbers && n != null) {
                "${com.agpeya.app.ui.reading.geezNumeral(n)}  $text"
            } else {
                text
            }
        }

    /** "— ሉቃስ ፲፥፴፰–፴፱ · አማርኛ ፲፱፻፹", or null when neither part is wanted. */
    fun citationLine(passage: Passage, format: CopyFormat): String? {
        val parts = listOfNotNull(
            passage.citation?.takeIf { format.reference && it.isNotBlank() },
            passage.edition?.takeIf { format.edition && it.isNotBlank() },
        )
        return if (parts.isEmpty()) null else "— " + parts.joinToString("  ·  ")
    }

    /** The passage as the person has asked to see it copied. */
    fun text(passage: Passage, format: CopyFormat): String =
        listOfNotNull(
            body(passage, format).takeIf { it.isNotBlank() },
            citationLine(passage, format),
        ).joinToString("\n")

    /** The heading the image card carries, always named however copy is set. */
    fun heading(passage: Passage): String? =
        listOfNotNull(
            passage.citation?.takeIf { it.isNotBlank() },
            passage.edition?.takeIf { it.isNotBlank() },
        ).takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}
