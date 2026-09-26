package com.agpeya.app.ui.reading

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Reading back what the app prints.
 *
 * Every number on every page of this app is a Ge'ez numeral, so someone who
 * types "መዝሙር ፶" into search is typing what they just read. The reference
 * parser knew Arabic digits only, and answered nothing.
 */
class GeezNumeralTest {

    @Test
    fun `every numeral the app prints reads back as itself`() {
        (1..999).forEach { n ->
            assertEquals("፡$n፡", n, parseGeezNumeral(geezNumeral(n)))
        }
    }

    @Test
    fun `the marks stand alone as one of themselves`() {
        assertEquals(100, parseGeezNumeral("፻"))
        assertEquals(10_000, parseGeezNumeral("፼"))
        assertEquals(150, parseGeezNumeral("፻፶"))
    }

    @Test
    fun `what is not a numeral is not read as one`() {
        assertNull(parseGeezNumeral("መዝሙር"))
        assertNull(parseGeezNumeral("50"))
        assertNull(parseGeezNumeral(""))
        assertNull(parseGeezNumeral("፩ሀ"))
    }
}
