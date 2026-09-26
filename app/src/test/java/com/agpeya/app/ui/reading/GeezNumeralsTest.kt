package com.agpeya.app.ui.reading

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ge'ez numerals cover two ranges in the app: verse numbers (1..199) in the
 * readers, and full years / amete alem (four to five digits) in Bahre Hasab.
 */
class GeezNumeralsTest {

    @Test
    fun `verse-range numbers`() {
        assertEquals("፩", geezNumeral(1))
        assertEquals("፲", geezNumeral(10))
        assertEquals("፳፫", geezNumeral(23))
        assertEquals("፻", geezNumeral(100))
        assertEquals("፻፶", geezNumeral(150))
        assertEquals("፻፺፱", geezNumeral(199))
    }

    @Test
    fun `hundreds write the count before the hundred mark`() {
        assertEquals("፪፻", geezNumeral(200))
        assertEquals("፱፻፺፱", geezNumeral(999))
    }

    @Test
    fun `ethiopian years`() {
        assertEquals("፳፻፲፰", geezNumeral(2018))
        assertEquals("፳፻፵፫", geezNumeral(2043))
    }

    @Test
    fun `amete alem years`() {
        assertEquals("፸፭፻፲፰", geezNumeral(7518))
    }

    @Test
    fun `ten thousands use the myriad mark`() {
        assertEquals("፼", geezNumeral(10_000))
        assertEquals("፼፳፫፻፵፭", geezNumeral(12_345))
    }
}
