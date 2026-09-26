package com.agpeya.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The half of the fit that can be tested without a device.
 *
 * Everything downstream of [sizeForLength] needs a real
 * `StaticLayout` and a real `Typeface`, so the measuring and the pagination are
 * exercised by rendering rather than here. The size ladder is where the
 * judgement lives, and it is pure.
 */
class PassageShareFitTest {

    @Test
    fun `a single verse is set as display type`() {
        // ~60 characters: the verse is the image, not a paragraph in a card.
        val verse = "ሕግከ ብርሃን ለእገርየ ወብርሃን ለመጽያሕትየ።"
        assertEquals(78f, sizeForLength(verse.length))
    }

    @Test
    fun `a reading settles at reading size, however long it is`() {
        // Not the floor: the ladder says what the length wants and the frame
        // decides whether it survives. A story with room for 44px should not be
        // handed 40px merely because the passage is long.
        assertEquals(44f, sizeForLength(701))
        assertEquals(44f, sizeForLength(5_000))
        assertEquals(44f, sizeForLength(Int.MAX_VALUE))
    }

    @Test
    fun `the ladder never rises with length`() {
        var previous = Float.MAX_VALUE
        for (chars in 0..1_200 step 10) {
            val size = sizeForLength(chars)
            assertTrue(
                "size went up at $chars chars: $previous -> $size",
                size <= previous,
            )
            previous = size
        }
    }

    @Test
    fun `every size stays inside the readable band`() {
        for (chars in 0..2_000 step 7) {
            val size = sizeForLength(chars)
            assertTrue("$size too small at $chars chars", size >= 40f)
            assertTrue("$size too large at $chars chars", size <= 78f)
        }
    }

    @Test
    fun `an empty passage is still given a size`() {
        // A blank body is rendered as a blank page rather than a crash; it must
        // still come back with something drawable.
        assertTrue(sizeForLength(0) >= 40f)
    }
}
