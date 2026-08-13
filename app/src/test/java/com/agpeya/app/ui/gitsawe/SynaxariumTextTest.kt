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
    fun `strips an existing list marker and preserves its number`() {
        val paras = parseSynaxarium("1.ቅድስት አንስጣስያ\n2.ቅድስት ሶስና ድንግል")
        assertEquals("ቅድስት አንስጣስያ", paras[0].text)
        assertEquals(1, paras[0].sourceNumber)
        assertEquals("ቅድስት ሶስና ድንግል", paras[1].text)
        assertEquals(2, paras[1].sourceNumber)
    }

    @Test
    fun `keeps non-sequential source numbering`() {
        // The four-seasons list: intro paragraphs first, then a 1..3 sub-list whose
        // numbers are meaningful and must not be rewritten.
        val paras = parseSynaxarium("መግቢያ ጽሑፍ\n1. ክረምት\n2. መፀው\n3. በጋ")
        assertEquals(null, paras[0].sourceNumber)
        assertEquals(listOf(1, 2, 3), paras.drop(1).map { it.sourceNumber })
    }

    @Test
    fun `recognizes keycap and circled digits as list markers`() {
        val paras = parseSynaxarium("1⃣ በታሪክ ዓለምን\n⓵ ጾሙም ሆነ ጸሎቱ\n② ሁለተኛ ነጥብ")
        assertEquals(listOf(1, 1, 2), paras.map { it.sourceNumber })
        assertEquals("በታሪክ ዓለምን", paras[0].text)
        assertEquals("ጾሙም ሆነ ጸሎቱ", paras[1].text)
    }

    @Test
    fun `a bare number that is part of the text is not a list marker`() {
        val paras = parseSynaxarium("900 ዓመት ያስቆጠሩ የብራና መጻሕፍት")
        assertEquals(null, paras.single().sourceNumber)
        assertTrue(paras.single().text.startsWith("900 ዓመት"))
    }

    @Test
    fun `strips bold-tag debris including malformed variants`() {
        assertEquals("አራቱ ክፍላተ ዘመን", cleanSynaxariumText("<b> አራቱ ክፍላተ ዘመን</b>"))
        assertEquals("ጽሑፍ", cleanSynaxariumText("< b >ጽሑፍ</b >"))
        assertEquals("በእጆቹ", cleanSynaxariumText("<b<በእጆቹ"))
        assertEquals("የምንመለሰ", cleanSynaxariumText("<የምንመለሰ"))
    }

    @Test
    fun `detects scripture entries by the book marker`() {
        assertTrue(isScriptureEntry("📖ሉቃ 10፥38"))
        assertFalse(isScriptureEntry("ቅድስት አንስጣስያ"))
    }

    @Test
    fun `detects the arke marker glued to its verse`() {
        // Both real data shapes: no space ("አርኬሰላም…") and with a space.
        for (raw in listOf("prose\nአርኬሰላም ለሶስና ዘተዓገሠት", "prose\nአርኬ ሰላም ለቢሶራ አቃቤ")) {
            val paras = parseSynaxarium(raw)
            assertEquals(raw, 1, paras.count { it.kind == SynaxariumParaKind.ARKE_LABEL })
            val verse = paras.single { it.kind == SynaxariumParaKind.ARKE_VERSE }
            assertTrue("verse starts at ሰላም", verse.text.startsWith("ሰላም"))
        }
    }

    @Test
    fun `detects the arke marker dangling after prose with control characters`() {
        val raw = "በረከቱም ከእኛ ጋር ትኑር ለዘላለሙ አሜን።\u0008\u0008\u0008 አርኬ\nሰላም ለከ በአሚን ስኩብ"
        val paras = parseSynaxarium(raw)
        assertEquals(1, paras.count { it.kind == SynaxariumParaKind.ARKE_LABEL })
        assertEquals("ሰላም ለከ በአሚን ስኩብ", paras.single { it.kind == SynaxariumParaKind.ARKE_VERSE }.text)
        val narrative = paras.single { it.kind == SynaxariumParaKind.NARRATIVE }
        assertFalse("backspaces stripped", narrative.text.any { it.code < 0x20 })
        assertTrue(narrative.text.endsWith("አሜን።"))
    }

    @Test
    fun `does not mistake the name Archelaus for the arke marker`() {
        val paras = parseSynaxarium("አርኬላዖስ ሰማዕት መታሰቢያው ነው።")
        assertEquals(1, paras.size)
        assertEquals(SynaxariumParaKind.NARRATIVE, paras.single().kind)
    }

    /**
     * ነሐሴ ፯, verbatim from the bundled data: the hymn follows the closing
     * benediction with no አርኬ line between them. Before this was handled it
     * rendered as one more paragraph of ጴጥሮስ's life.
     */
    @Test
    fun `an unlabelled salutation still reads as a hymn`() {
        val raw = "ለእግዚአብሔር ምስጋና ይሁን እኛንም በጌቶቻችን ሐዋርያት ጸሎት ይማረን ለዘላለሙ አሜን ።\n" +
            "ሰላም ዕብል ለጴጥሮስ ሥዩም፡፡ ላዕለ ሐዋርያት ኄራን ካህናተ ኵሉ ዓለም፡፡"
        val paras = parseSynaxarium(raw)
        assertEquals("the benediction stays prose", 1, paras.count { it.kind == SynaxariumParaKind.NARRATIVE })
        assertEquals("a heading is supplied", 1, paras.count { it.kind == SynaxariumParaKind.ARKE_LABEL })
        assertTrue(paras.single { it.kind == SynaxariumParaKind.ARKE_VERSE }.text.startsWith("ሰላም ዕብል ለጴጥሮስ"))
    }

    @Test
    fun `an explicit marker is not doubled by the salutation that follows it`() {
        val raw = "አርኬ\nሰላም ለጢሞቲዎስ እንተ ረሰየ ምርዋጾ፡፡ ለምሂር ወለገሠጾ፡፡"
        val paras = parseSynaxarium(raw)
        assertEquals(1, paras.count { it.kind == SynaxariumParaKind.ARKE_LABEL })
        assertEquals(1, paras.count { it.kind == SynaxariumParaKind.ARKE_VERSE })
    }

    /** ሰላም is an ordinary Amharic word; only the Ge'ez clause stops make it a hymn. */
    @Test
    fun `prose that merely uses the word selam stays prose`() {
        val paras = parseSynaxarium("ሰላም ለሁላችሁ ይሁን")
        assertEquals(1, paras.size)
        assertEquals(SynaxariumParaKind.NARRATIVE, paras.single().kind)
    }
}
