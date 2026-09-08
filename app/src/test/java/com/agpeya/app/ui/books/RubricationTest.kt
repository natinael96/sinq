package com.agpeya.app.ui.books

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rule is applied to seventy books of forty stanzas, so the cases that
 * fixed its shape are kept here — every one of them a real line from
 * መልክአ መድኃኔ ዓለም or መልክአ ሀብተ ማርያም.
 */
class RubricationTest {

    private fun red(text: String): List<String> =
        Rubrication.redRanges(text).map { text.substring(it.first, it.last + 1) }

    @Test
    fun `salutation takes the le phrase and its construct`() {
        assertEquals(
            listOf("ሰላም ለዝክረ ስምከ"),
            red("ሰላም ለዝክረ ስምከ ዘኢረከቡ ተፍጻሚተ። መላእክተ ሰማይ ወምድር እለ ለመዱ ስብሐተ።"),
        )
    }

    @Test
    fun `salutation stops before a relative clause`() {
        assertEquals(
            listOf("ሰላም ለገጽከ"),
            red("ሰላም ለገጽከ ዘይቀትል በድንጋፄ። ዘምስለ ቀራንብት ክልዔ ዕልወ ወዐማፄ።"),
        )
    }

    @Test
    fun `salutation stops before a conjunction`() {
        assertEquals(
            listOf("ሰላም ለአዕይንቲከ"),
            red("ሰላም ለአዕይንቲከ ወለእዝንከ ሰማዒ። እንዘ ላዕለ ኪሩብ ትነብር።"),
        )
    }

    @Test
    fun `salutation stops before a second thing greeted`() {
        assertEquals(
            listOf("ሰላም ለመላትሒከ"),
            red("ሰላም ለመላትሒከ ለተወክፎ ሥቃይ ዘአጽነና።"),
        )
    }

    @Test
    fun `ebl is part of the salutation`() {
        assertEquals(
            listOf("ሰላም እብል ለስዕርተ ርእስከ"),
            red("ሰላም እብል ለስዕርተ ርእስከ ድሉል። ወለርእስከ ሠናይ።"),
        )
    }

    /**
     * The scans spell "I say" five ways; ዕብሎ is one of them.
     *
     * ጽሑቁ is kept: it is an adjective agreeing with ዲዮስቆሮስ, so it is part of
     * what is greeted rather than the start of a new clause — the same reason
     * ስምከ is kept in "ሰላም ለዝክረ ስምከ".
     */
    @Test
    fun `the say verb is matched however the scan spells it`() {
        assertEquals(
            listOf("ሰላም እብሎ ለዲዮስቆሮስ ጽሑቁ፤"),
            red("ሰላም እብሎ ለዲዮስቆሮስ ጽሑቁ፤ እንበይነ መድኅን ዘሐመ በሕቁ።"),
        )
        assertEquals(
            listOf("ሰላም ዕብል ለገጽከ"),
            red("ሰላም ዕብል ለገጽከ ዘይቀትል በድንጋፄ።"),
        )
    }

    @Test
    fun `the Name is red wherever it falls`() {
        assertEquals(
            listOf("ሰላም ለአዕይንቲከ", "ክርስቶስ", "እግዚአብሔር"),
            red("ሰላም ለአዕይንቲከ ወለእዝንከ ሰማዒ። መድኃኔ ዓለም ክርስቶስ ነፍሰ ዚአየ አጥዒ። አኮኑ እግዚእየ እግዚአብሔር መዋዒ።"),
        )
    }

    /** The hymn's subject is the addressee, not the Name: it stays in ink. */
    @Test
    fun `the recurring subject is left alone`() {
        assertEquals(
            listOf("ሰላም ለዝክረ ስምከ"),
            red("ሰላም ለዝክረ ስምከ ዘኢረከቡ። መድኃኔ ዓለም ተወከፍ እንተ አቅረብኩ ንስቲተ።"),
        )
    }

    /**
     * Two things at once: እም- stops the salutation (it opens "from the womb of",
     * not a continuation of what is greeted), and ሀብተ ማርያም is a saint's name —
     * reddening it would rubricate that hymn's refrain forty times over.
     */
    @Test
    fun `em stops the salutation and a saints Maryam stays in ink`() {
        assertEquals(
            listOf("ሰላም ለፅንሰትከ"),
            red("ሰላም ለፅንሰትከ እማኅፀነ ቅድስት ዮስቴና። ሀብተ ማርያም ጴጥሮስ ሐዋርያ ወልደ ዮና።"),
        )
    }

    @Test
    fun `a line that is not a salutation gets no opening red`() {
        assertEquals(
            listOf("እግዚአብሔር", "እግዚአብሔር"),
            red("በስመ እግዚአብሔር አብ እምቅድመ ዓለም ዘሀሎ፣ ወበስመ እግዚአብሔር ወልድ።"),
        )
    }
}
