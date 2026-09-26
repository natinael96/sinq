package com.agpeya.app.ui.gitsawe

import com.agpeya.app.model.SynaxariumEntry
import com.agpeya.app.model.SynaxariumParagraph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The structure now comes from the scans, so what is worth asserting is no
 * longer "does the parser find the hymn" — it is that the model reads a day the
 * way the reader draws it. The old parser's three hundred lines of prose
 * heuristics, and the two test classes pinning them, went with it.
 */
class SynaxariumStructureTest {

    private val entry = SynaxariumEntry(
        id = "e1d08ab5",
        paragraphs = listOf(
            SynaxariumParagraph(1, "አንድ አምላክ በሚሆን በአብ በወልድ በመንፈስ ቅዱስ ስም ጥቅምት ሦስት ። ቅዱስ አባ ስምዖን አረፈ ።"),
            SynaxariumParagraph(2, "ይህም አባት ከእስክንድርያ ከታላላቆች ተወላጅ የሆነ ።"),
        ),
        arke = "ሰላም ለስምዖን ለልብሰ መላእክት ዘተዓጽፎ ።",
    )

    @Test
    fun `an entry is titled by the sentence its account opens with`() {
        assertEquals("አንድ አምላክ በሚሆን በአብ በወልድ በመንፈስ ቅዱስ ስም ጥቅምት ሦስት", entry.heading)
    }

    @Test
    fun `heading is blank rather than throwing when the entry has no paragraphs`() {
        assertEquals("", SynaxariumEntry(id = "x", arke = "ሰላም ለከ ።").heading)
    }

    @Test
    fun `text joins the paragraphs and leaves the hymn out of the prose`() {
        assertEquals(2, entry.text.lines().size)
        assertFalse(entry.text.contains("ሰላም ለስምዖን"))
    }

    @Test
    fun `an entry with no hymn reports none rather than an empty one`() {
        assertNull(SynaxariumEntry(id = "y", paragraphs = entry.paragraphs).arke)
    }
}
