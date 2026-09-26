package com.agpeya.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * How a passage is named when it leaves the app.
 *
 * The bundled hours carry their references in Latin — a fact of the extraction,
 * not of the prayer book — so a verse shared out of ጸሎተ ነግህ used to arrive in a
 * chat as "John 1:1-17" under Amharic text.
 */
class CitationTest {

    private val books = mapOf(
        Citation.englishKey("John") to "የዮሐንስ ወንጌል",
        Citation.englishKey("Luke") to "የሉቃስ ወንጌል",
        Citation.englishKey("1 Corinthians") to "1ኛ ወደ ቆሮንቶስ ሰዎች",
    )

    private fun latin(reference: String) = Citation.fromLatin(reference, "መዝሙር", books)

    @Test
    fun `a chapter and verse range reads in Amharic and Geez`() {
        assertEquals("የሉቃስ ወንጌል ፲፥፴፰–፵፪", Citation.of("የሉቃስ ወንጌል", 10, 38, 42))
    }

    @Test
    fun `a whole chapter names no verses`() {
        assertEquals("የዮሐንስ ወንጌል ፫", Citation.of("የዮሐንስ ወንጌል", 3))
    }

    @Test
    fun `a single verse is not written as a range`() {
        assertEquals("የዮሐንስ ወንጌል ፫፥፲፮", Citation.of("የዮሐንስ ወንጌል", 3, 16, 16))
    }

    @Test
    fun `the hours' Latin references become the Church's own`() {
        assertEquals("የዮሐንስ ወንጌል ፩፥፩–፲፯", latin("John 1:1-17"))
        assertEquals("የሉቃስ ወንጌል ፬፥፴፰–፵፩", latin("Luke 4:38-41"))
    }

    @Test
    fun `a psalm is named as a psalm, since the Psalter is not in the catalogue`() {
        assertEquals("መዝሙር ፩", latin("Ps 1"))
        assertEquals("መዝሙር ፶", latin("Psalm 50"))
    }

    @Test
    fun `a numbered book is found however its name is spaced`() {
        assertEquals("1ኛ ወደ ቆሮንቶስ ሰዎች ፲፫፥፬", latin("1 Corinthians 13:4"))
    }

    @Test
    fun `an en dash separates a range as readily as a hyphen`() {
        assertEquals(latin("John 1:1-17"), latin("John 1:1–17"))
    }

    @Test
    fun `what cannot be read is left alone rather than guessed at`() {
        assertNull(latin("Habakkuk 2:4"))   // not in the map
        assertNull(latin("ወንጌል"))            // not a reference at all
        assertNull(latin(""))
    }
}
