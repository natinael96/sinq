package com.agpeya.app.ui.library

import com.agpeya.app.model.ScriptureBookMeta
import com.agpeya.app.ui.strings.AmharicStrings
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * How the Library files the books the bundle leaves unsectioned.
 *
 * The Church divides its eighty-one into nine groups — ብሉይ ኪዳን into ሕግ, ታሪክ,
 * ጥበብ and ትንቢት, ሐዲስ ኪዳን into ወንጌል, ታሪክ, መልእክታት, ትንቢት and ሥርዓት — and counts
 * the history books "from ኢያሱ ወልደ ነዌ to ዮሴፍ ወልደ ኮርዮን". The bundle's own
 * catalogue files Josippon with the books of order instead.
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
    fun `an unsectioned deuterocanonical book is grouped with the rest`() {
        assertEquals("Deuterocanonical", canonSectionKey(book("enoch", "deuterocanonical")))
    }

    @Test
    fun `the books of order are what is left over`() {
        assertEquals("ChurchOrder", canonSectionKey(book("didascalia", "new")))
        assertEquals("ChurchOrder", canonSectionKey(book("sirate-tsion", "new")))
        assertEquals("new", canonTestament(book("didascalia", "new")))
    }

    @Test
    fun `Josippon is read as the last of the history books, not as an order book`() {
        val josippon = book(JOSIPPON, "new")
        assertEquals("old", canonTestament(josippon))
        assertEquals("Historical", canonSectionKey(josippon))
    }

    @Test
    fun `the order group is named as the Church names it`() {
        assertEquals("የሥርዓት መጻሕፍት", AmharicStrings.canonSection("ChurchOrder"))
    }
}
