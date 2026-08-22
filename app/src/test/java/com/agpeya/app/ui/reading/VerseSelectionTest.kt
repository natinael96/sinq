package com.agpeya.app.ui.reading

import com.agpeya.app.model.Section
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The verse-selection model behind the readers' share/highlight bar: the first
 * tap anchors, later taps in the same section move the end, and the helpers
 * resolve the run into share text, a card payload, and highlight keys.
 */
class VerseSelectionTest {

    private val psalm = Section(
        id = "ps_23",
        type = "psalm",
        number = 23,
        title = "መዝሙር ፳፫",
        firstVerse = 1,
        verses = listOf("የመጀመሪያ ጥቅስ", "ሁለተኛ ጥቅስ", "ሦስተኛ ጥቅስ", "አራተኛ ጥቅስ"),
    )

    @Test
    fun `a tap anchors and a same-section tap extends`() {
        val (a1, b1) = advanceSelection(null, "ps_23:2")
        assertEquals("ps_23:2", a1)
        assertNull(b1)
        val (a2, b2) = advanceSelection(a1, "ps_23:4")
        assertEquals("ps_23:2", a2)
        assertEquals("ps_23:4", b2)
    }

    @Test
    fun `a tap in another section starts over`() {
        val (a, b) = advanceSelection("ps_23:2", "ps_50:1")
        assertEquals("ps_50:1", a)
        assertNull(b)
    }

    @Test
    fun `the range is order-free and scoped to its own section`() {
        assertEquals(2..4, selectionRangeFor(psalm, "ps_23:4", "ps_23:2"))
        assertEquals(3..3, selectionRangeFor(psalm, "ps_23:3", null))
        assertTrue(selectionRangeFor(psalm, "ps_50:1", "ps_50:2").isEmpty())
        assertTrue(selectionRangeFor(psalm, null, null).isEmpty())
    }

    @Test
    fun `share text carries the title and every selected verse with its numeral`() {
        val text = verseShareText(listOf(psalm), "ps_23:1", "ps_23:2")!!
        assertEquals("መዝሙር ፳፫\n፩  የመጀመሪያ ጥቅስ\n፪  ሁለተኛ ጥቅስ", text)
        // Single verse — unchanged from the pre-range behaviour.
        assertEquals("መዝሙር ፳፫\n፫  ሦስተኛ ጥቅስ", verseShareText(listOf(psalm), "ps_23:3"))
    }

    @Test
    fun `the payload body is the run and the kicker survives`() {
        val payload = versePayload(listOf(psalm), "ps_23:2", "መዝሙረ ዳዊት", "ps_23:3")!!
        assertEquals("፪  ሁለተኛ ጥቅስ\n፫  ሦስተኛ ጥቅስ", payload.body)
        assertEquals("መዝሙረ ዳዊት", payload.kicker)
        assertEquals("መዝሙር ፳፫", payload.title)
    }

    @Test
    fun `highlight keys cover the run and skip verses past the end of the text`() {
        assertEquals(
            listOf("ps_23:3", "ps_23:4"),
            selectionKeys(listOf(psalm), "ps_23:3", "ps_23:4"),
        )
        // An end past the last verse yields only the verses that exist.
        assertEquals(
            listOf("ps_23:4"),
            selectionKeys(listOf(psalm), "ps_23:4", "ps_23:9"),
        )
    }

    @Test
    fun `highlight keys can be scoped to a translation`() {
        assertEquals(
            listOf("gez-1980:ps_23:2", "gez-1980:ps_23:3"),
            selectionKeys(listOf(psalm), "ps_23:2", "ps_23:3") { "gez-1980" },
        )
    }

    @Test
    fun `shared scripture selection includes its reference`() {
        val gospel = psalm.copy(title = "ወንጌል", reference = "Luke 2:25-32")
        val text = verseShareText(listOf(gospel), "ps_23:1")!!
        assertTrue(text.startsWith("ወንጌል — Luke 2:25-32\n"))
        assertEquals("ወንጌል — Luke 2:25-32", versePayload(listOf(gospel), "ps_23:1", null)!!.title)
    }

    @Test
    fun `an unknown section or malformed key resolves to nothing`() {
        assertNull(verseShareText(listOf(psalm), "missing:1", "missing:2"))
        assertNull(versePayload(listOf(psalm), "ps_23:notanumber", null))
    }
}
