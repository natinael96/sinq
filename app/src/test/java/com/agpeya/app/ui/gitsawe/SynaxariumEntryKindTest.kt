package com.agpeya.app.ui.gitsawe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The four things a day of ስንክሳር is made of.
 *
 * The source calls all 2,308 of them entries and the screen drew all of them
 * alike, but only 852 are a commemoration. The rest are the day's opening
 * doxology under a heading naming the month, lists of feast names, and
 * scripture quotations, and each wants setting as what it is.
 */
class SynaxariumEntryKindTest {

    private val dir: File =
        listOf("src/main/assets/content/sinksar", "app/src/main/assets/content/sinksar")
            .map(::File).first { it.isDirectory }

    /** Every entry title in the bundle, without decoding the model. */
    private val titles: List<String> by lazy {
        dir.listFiles().orEmpty()
            .filter { it.name != "manifest.json" }
            .flatMap { file ->
                Regex(""""title"\s*:\s*"((?:[^"\\]|\\.)*)"""")
                    .findAll(file.readText())
                    .map { it.groupValues[1].replace("\\n", "\n").replace("\\\"", "\"") }
                    .toList()
            }
    }

    @Test
    fun `the whole year sorts into four kinds, in the proportions measured`() {
        assertEquals(2308, titles.size)
        val counts = titles.groupingBy { synaxariumEntryKind(it) }.eachCount()
        assertEquals(852, counts[SynaxariumEntryKind.COMMEMORATION])
        assertEquals(715, counts[SynaxariumEntryKind.FEAST_LIST])
        assertEquals(380, counts[SynaxariumEntryKind.SCRIPTURE])
        assertEquals(361, counts[SynaxariumEntryKind.OPENING])
    }

    @Test
    fun `a life is a commemoration, not a heading`() {
        assertEquals(
            SynaxariumEntryKind.COMMEMORATION,
            synaxariumEntryKind("በዚችም ዕለት ቅዱስ ቲቶ ረድእ አረፈ ።"),
        )
    }

    @Test
    fun `the month heading is recognised however the source spaces it`() {
        assertEquals(SynaxariumEntryKind.OPENING, synaxariumEntryKind("ስንክሳር ዘወርኀ መስከረም"))
        assertEquals(SynaxariumEntryKind.OPENING, synaxariumEntryKind("  ስንክሳር ዘወርኀ ጳጉሜን  "))
    }

    @Test
    fun `the two markers keep their own kinds`() {
        assertEquals(SynaxariumEntryKind.FEAST_LIST, synaxariumEntryKind("📌 ወርኀዊ በዓላት"))
        assertEquals(SynaxariumEntryKind.SCRIPTURE, synaxariumEntryKind("📖 ሉቃ ፬፥፲፯"))
    }

    @Test
    fun `a list drops the numbering it prints, since the list draws its own`() {
        val items = synaxariumListItems("፩.ርዕሰ ዓውደ ዓመት\n፪.ቅዱስ በርተሎሜዎስ ሐዋርያ\n\n፫.ቅዱስ ራጉኤል")
        assertEquals(listOf("ርዕሰ ዓውደ ዓመት", "ቅዱስ በርተሎሜዎስ ሐዋርያ", "ቅዱስ ራጉኤል"), items)
    }

    @Test
    fun `an unnumbered list keeps its lines whole`() {
        assertEquals(
            listOf("ልደታ ለማርያም ድንግል እግዝእትነ", "ቅዱሳን ኢያቄም ወሐና"),
            synaxariumListItems("ልደታ ለማርያም ድንግል እግዝእትነ\nቅዱሳን ኢያቄም ወሐና"),
        )
    }

    @Test
    fun `most entries close without an አርኬ, which is why the space cannot depend on one`() {
        val texts = dir.listFiles().orEmpty()
            .filter { it.name != "manifest.json" }
            .sumOf { file -> Regex("አርኬ").findAll(file.readText()).count() }
        assertTrue("no አርኬ found at all", texts > 0)
    }
}
