package com.agpeya.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * How a passage reads when it leaves the app.
 *
 * The shape is the one Olive Tree and Logos settled on: the verses, then the
 * reference under them behind an em dash. What the reader keeps of that is the
 * reader's choice, and the choice sticks.
 */
class PassageFormatTest {

    private val luke = Passage(
        verses = listOf(
            38 to "ወደ ቤትም ሲገቡ ማርታ የምትባል አንዲት ሴት በቤቷ ተቀበለችው።",
            39 to "ማርያም የምትባል እኅትም ነበረቻት።",
        ),
        citation = "የሉቃስ ወንጌል ፲፥፴፰–፴፱",
        edition = "አማርኛ ፲፱፻፹",
    )

    @Test
    fun `everything on reads verses, reference and edition`() {
        assertEquals(
            """
            ፴፰  ወደ ቤትም ሲገቡ ማርታ የምትባል አንዲት ሴት በቤቷ ተቀበለችው።
            ፴፱  ማርያም የምትባል እኅትም ነበረቻት።
            — የሉቃስ ወንጌል ፲፥፴፰–፴፱  ·  አማርኛ ፲፱፻፹
            """.trimIndent(),
            PassageFormat.text(luke, CopyFormat()),
        )
    }

    @Test
    fun `verse numbers can be left out of the words`() {
        assertEquals(
            """
            ወደ ቤትም ሲገቡ ማርታ የምትባል አንዲት ሴት በቤቷ ተቀበለችው።
            ማርያም የምትባል እኅትም ነበረቻት።
            — የሉቃስ ወንጌል ፲፥፴፰–፴፱  ·  አማርኛ ፲፱፻፹
            """.trimIndent(),
            PassageFormat.text(luke, CopyFormat(verseNumbers = false)),
        )
    }

    @Test
    fun `the edition can go while the reference stays`() {
        assertEquals(
            "— የሉቃስ ወንጌል ፲፥፴፰–፴፱",
            PassageFormat.citationLine(luke, CopyFormat(edition = false)),
        )
    }

    @Test
    fun `with neither reference nor edition there is no line at all`() {
        val bare = CopyFormat(reference = false, edition = false)
        assertNull(PassageFormat.citationLine(luke, bare))
        assertEquals(
            "፴፰  ወደ ቤትም ሲገቡ ማርታ የምትባል አንዲት ሴት በቤቷ ተቀበለችው።\n፴፱  ማርያም የምትባል እኅትም ነበረቻት።",
            PassageFormat.text(luke, bare),
        )
    }

    @Test
    fun `a paragraph unit has no numbers to print`() {
        val sinksar = Passage(
            verses = listOf(null to "በዚች ዕለት ቅዱስ ቲቶ ረድእ አረፈ።"),
            citation = "ስንክሳር · ጳጉሜን ፪",
        )
        assertEquals(
            "በዚች ዕለት ቅዱስ ቲቶ ረድእ አረፈ።\n— ስንክሳር · ጳጉሜን ፪",
            PassageFormat.text(sinksar, CopyFormat()),
        )
    }

    @Test
    fun `the image heading names the passage however copy is set`() {
        assertEquals("የሉቃስ ወንጌል ፲፥፴፰–፴፱  ·  አማርኛ ፲፱፻፹", PassageFormat.heading(luke))
        assertNull(PassageFormat.heading(Passage(verses = emptyList())))
    }
}
