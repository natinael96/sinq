package com.agpeya.app.ui.gitsawe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the ስንክሳር text pipeline: emoji stripping, arke splitting, scripture detection. */
class SynaxariumTextTest {

    @Test
    fun `strips marker emojis but keeps Ethiopic`() {
        val raw = "❖ ✍️ 📌 📖 ሰላም እብል"
        val clean = cleanSynaxariumText(raw)
        assertFalse("no dingbats remain", clean.any { it.code in 0x2600..0x27BF })
        assertFalse("no variation selectors", clean.contains('️'))
        assertTrue("Ethiopic survives", clean.contains("ሰላም"))
        assertEquals("ሰላም እብል", clean)
    }

    @Test
    fun `keeps Ethiopic Extended characters`() {
        // U+2DC8 (Ethiopic Extended) must not be mistaken for a symbol.
        val clean = cleanSynaxariumText("❖ አⷈሉ")
        assertEquals("አⷈሉ", clean)
    }

    @Test
    fun `splits narrative from the arke hymn`() {
        val raw = "❖ ፈነው prose one\nmore prose\nአርኬ\n" +
            "✍️ ሰላም ለዮሐንስ"
        val paras = parseSynaxarium(raw)

        val narrative = paras.filter { it.kind == SynaxariumParaKind.NARRATIVE }
        assertEquals(2, narrative.size)

        assertEquals(1, paras.count { it.kind == SynaxariumParaKind.ARKE_LABEL })
        val verses = paras.filter { it.kind == SynaxariumParaKind.ARKE_VERSE }
        assertEquals(1, verses.size)
        assertTrue("verse keeps its salam", verses.first().text.startsWith("ሰላም"))
        assertFalse("verse emoji stripped", verses.first().text.contains('✍'))
    }

    @Test
    fun `blank lines are dropped`() {
        val paras = parseSynaxarium("one\n\n  \ntwo")
        assertEquals(2, paras.size)
        assertTrue(paras.all { it.kind == SynaxariumParaKind.NARRATIVE })
    }

    @Test
    fun `strips an existing list marker so numbering does not double up`() {
        val paras = parseSynaxarium("1.ቅድስት አንስጣስያ\n2.ቅድስት ሶስና ድንግል")
        assertEquals("ቅድስት አንስጣስያ", paras[0].text)
        assertEquals("ቅድስት ሶስና ድንግል", paras[1].text)
    }

    @Test
    fun `detects scripture entries by the book marker`() {
        assertTrue(isScriptureEntry("📖ሉቃ 10፥38"))
        assertFalse(isScriptureEntry("ቅድስት አንስጣስያ"))
    }
}
