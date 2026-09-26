package com.agpeya.app.ui.library

import com.agpeya.app.model.ScriptureBookMeta
import com.agpeya.app.ui.strings.AmharicStrings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * How the Library files the bundled books.
 *
 * ፍትሐ ነገሥት አንቀጽ ፪ enumerates the canon: forty-six ብሉይ ኪዳን and thirty-five
 * ሐዲስ ኪዳን, the latter opening with the eight books of church order. The
 * upstream catalogue the app bundles does not follow it — it files everything
 * past the Hebrew thirty-nine as one "deuterocanonical" block, leaves the
 * books of order sectionless, counts ዮሴፍ ወልደ ኮርዮን among them, and puts ወደ
 * ዕብራውያን with the catholic epistles. These tests hold the corrections.
 */
class CanonGroupingTest {

    private fun book(key: String, testament: String, section: String = "") =
        ScriptureBookMeta(
            number = 0,
            key = key,
            nameAm = key,
            nameEn = key,
            chapters = 1,
            testament = testament,
            section = section,
        )

    @Test
    fun `a sectioned book keeps the section the bundle gives it`() {
        val esther = book("esther", "old", "Historical")
        assertEquals("old", canonTestament(esther))
        assertEquals("Historical", canonSectionKey(esther))
    }

    @Test
    fun `the books of order are what is left unplaced`() {
        assertEquals("ChurchOrder", canonSectionKey(book("didascalia", "new")))
        assertEquals("ChurchOrder", canonSectionKey(book("sirate-tsion", "new")))
        assertEquals("new", canonTestament(book("didascalia", "new")))
    }

    @Test
    fun `Josippon is the last of the histories, not a book of order`() {
        val josippon = book(JOSIPPON, "new")
        assertEquals("old", canonTestament(josippon))
        assertEquals("Historical", canonSectionKey(josippon))
    }

    @Test
    fun `a book counted inside another is filed with it`() {
        // ኩፋሌ is counted as one with ኦሪት ዘፍጥረት; ባሮክ and ተረፈ ኤርምያስ inside
        // ትንቢተ ኤርምያስ; ሶስና inside ትንቢተ ዳንኤል.
        val d = "deuterocanonical"
        assertEquals("Pentateuch", canonSectionKey(book("kufale", d, "Deuterocanonical")))
        assertEquals("MajorProphet", canonSectionKey(book("baruch", d, "Deuterocanonical")))
        assertEquals("MajorProphet", canonSectionKey(book("susanna", d, "Deuterocanonical")))
        assertEquals("PoetryWisdom", canonSectionKey(book("sirach", d, "Deuterocanonical")))
        assertEquals("Historical", canonSectionKey(book("tobit", d, "Deuterocanonical")))
    }

    @Test
    fun `Hebrews leads the Pauline epistles rather than the catholic ones`() {
        assertEquals("PaulineEpistle", canonSectionKey(book("hebrews", "new", "GeneralEpistle")))
    }

    @Test
    fun `the order group is named as the Church names it`() {
        assertEquals("የሥርዓት መጻሕፍት", AmharicStrings.canonSection("ChurchOrder"))
        assertEquals("የሕግ መጻሕፍት", AmharicStrings.canonSection("Pentateuch"))
        assertEquals("የታሪክ መጻሕፍት", AmharicStrings.canonSection("Historical"))
    }

    // ── against the bundle itself ────────────────────────────────────────────

    private val bibleDir: File =
        listOf("src/main/assets/content/bible", "app/src/main/assets/content/bible")
            .map(::File).first { it.isDirectory }

    /** Every bundled book, as the Library sees it. */
    private val bundled: List<ScriptureBookMeta> by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val canon = json.parseToJsonElement(File(bibleDir, "canon.json").readText())
            .jsonArray.associateBy { it.jsonObject["id"]!!.jsonPrimitive.content }
        val meta = json.parseToJsonElement(File(bibleDir, "am-1980/meta.json").readText()).jsonObject
        meta["books"]!!.jsonArray.map { node ->
            val b = node.jsonObject
            val id = b["id"]!!.jsonPrimitive.content
            val c = canon[id]?.jsonObject
            book(
                key = b["file"]!!.jsonPrimitive.content.substringAfter('-').removeSuffix(".json"),
                testament = c?.text("testament") ?: "old",
                section = c?.text("section").orEmpty(),
            )
        }
    }

    /** Reads a field the way the repository does — JSON null is absent. */
    private fun JsonObject.text(key: String): String? =
        this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content

    /** The nine sections the Church divides the canon into. */
    private val sections = setOf(
        "Pentateuch", "Historical", "PoetryWisdom", "MajorProphet", "MinorProphet",
        "Gospel", "Acts", "PaulineEpistle", "GeneralEpistle", "Revelation", "ChurchOrder",
    )

    @Test
    fun `every bundled book lands in a section the Church has a name for`() {
        val stray = bundled.filterNot { canonSectionKey(it) in sections }
        assertTrue("unplaced: ${stray.map { it.key }}", stray.isEmpty())
        assertTrue(bundled.none { canonSectionKey(it) == "Deuterocanonical" })
    }

    @Test
    fun `the books of church order are the eight of ፍትሐ ነገሥት`() {
        assertEquals(
            setOf(
                "didascalia", "1-clement", "abtilis", "sirate-tsion",
                "tizaz", "1-covenant", "2-covenant", "gitsew",
            ),
            bundled.filter { canonSectionKey(it) == "ChurchOrder" }.mapTo(mutableSetOf()) { it.key },
        )
    }

    @Test
    fun `no book of the Old Testament is stranded off both lists`() {
        // Psalms has its own reader and is not in this catalogue; everything
        // else must answer to "old" or "new" once Josippon has been moved.
        val homeless = bundled.filterNot { canonTestament(it) in setOf("old", "new", "deuterocanonical") }
        assertTrue("stranded: ${homeless.map { it.key }}", homeless.isEmpty())
        assertFalse(bundled.filter { canonTestament(it) == "new" }.any { it.key == JOSIPPON })
    }

    @Test
    fun `the epistles divide fourteen Pauline to seven catholic`() {
        val paul = bundled.count { canonSectionKey(it) == "PaulineEpistle" }
        val catholic = bundled.count { canonSectionKey(it) == "GeneralEpistle" }
        assertEquals(14, paul)
        assertEquals(7, catholic)
        assertEquals(21, paul + catholic)   // የትምህርት ክፍል — twenty-one
    }
}
