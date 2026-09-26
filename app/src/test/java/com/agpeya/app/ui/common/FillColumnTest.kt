package com.agpeya.app.ui.common

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The arithmetic behind ቤት's layout: how the height a screen has left over is
 * shared among the blocks that grow.
 *
 * These are the cases the page actually meets — a tall phone where every block
 * reaches its ceiling, a short one where none does, and the awkward middle
 * where one does and the others have to be offered its share again.
 */
class FillColumnTest {

    private fun grow(
        floors: IntArray,
        weights: FloatArray,
        ceilings: IntArray = IntArray(floors.size) { Int.MAX_VALUE },
        free: Int,
    ) = growHeights(floors, weights, ceilings, free)

    @Test
    fun `nothing to share leaves every block on its floor`() {
        val floors = intArrayOf(64, 92, 37, 80, 65, 60)
        val weights = floatArrayOf(0f, 3f, 0f, 2f, 1f, 0f)
        assertArrayEquals(floors, grow(floors, weights, free = 0))
        assertArrayEquals(floors, grow(floors, weights, free = -120))
    }

    /** A block that does not grow is never touched, however much room there is. */
    @Test
    fun `fixed blocks keep their own height`() {
        val heights = grow(
            floors = intArrayOf(64, 92, 37),
            weights = floatArrayOf(0f, 1f, 0f),
            free = 300,
        )
        assertEquals(64, heights[0])
        assertEquals(37, heights[2])
        assertEquals(392, heights[1])
    }

    @Test
    fun `the surplus is shared in proportion to weight`() {
        val heights = grow(
            floors = intArrayOf(100, 100, 100),
            weights = floatArrayOf(3f, 2f, 1f),
            free = 600,
        )
        assertArrayEquals(intArrayOf(400, 300, 200), heights)
    }

    /**
     * The case ቤት meets on a large phone: the hero fills to its ceiling long
     * before the room runs out, and what it cannot take has to be offered to
     * the blocks still under theirs rather than left on the floor.
     */
    @Test
    fun `a block that reaches its ceiling hands its share back`() {
        val heights = grow(
            floors = intArrayOf(100, 100),
            weights = floatArrayOf(3f, 1f),
            ceilings = intArrayOf(150, Int.MAX_VALUE),
            free = 400,
        )
        assertEquals(150, heights[0])
        // 300 was its share and it could take 50; the other 250 went next door.
        assertEquals(450, heights[1])
        assertEquals(600, heights.sum())
    }

    /** When every ceiling is reached the rest is left over, for the gaps. */
    @Test
    fun `what no ceiling will take is left unspent`() {
        val floors = intArrayOf(92, 80, 65)
        val heights = grow(
            floors = floors,
            weights = floatArrayOf(3f, 2f, 1f),
            ceilings = intArrayOf(200, 168, 110),
            free = 600,
        )
        assertArrayEquals(intArrayOf(200, 168, 110), heights)
        val unspent = 600 - (heights.sum() - floors.sum())
        assertEquals(359, unspent)
    }

    /** The real numbers: ቤት on a 430×932 phone, 816dp of usable page. */
    @Test
    fun `the home page fills a large phone without passing a ceiling`() {
        val floors = intArrayOf(64, 92, 37, 80, 65, 60)
        val weights = floatArrayOf(0f, 3f, 0f, 2f, 1f, 0f)
        val ceilings = intArrayOf(
            Int.MAX_VALUE, 200, Int.MAX_VALUE, 168, 110, Int.MAX_VALUE,
        )
        val gaps = 8 * 5
        val free = 816 - floors.sum() - gaps
        val heights = grow(floors, weights, ceilings, free)

        assertArrayEquals(intArrayOf(64, 200, 37, 168, 110, 60), heights)
        assertTrue("the page must not outgrow the screen", heights.sum() + gaps <= 816)
    }

    /** And on a small one, where nothing reaches a ceiling and it fills exactly. */
    @Test
    fun `the home page fills a small phone to the pixel`() {
        val floors = intArrayOf(64, 92, 37, 80, 65, 60)
        val weights = floatArrayOf(0f, 3f, 0f, 2f, 1f, 0f)
        val ceilings = intArrayOf(
            Int.MAX_VALUE, 200, Int.MAX_VALUE, 168, 110, Int.MAX_VALUE,
        )
        val usable = 524
        val gaps = 8 * 5
        val free = usable - floors.sum() - gaps
        val heights = grow(floors, weights, ceilings, free)

        // 86 to share, three ways: 43, 28, 14 — nothing near a ceiling.
        assertArrayEquals(intArrayOf(64, 135, 37, 108, 79, 60), heights)
        // One pixel of rounding is all that is left, and the gaps take it.
        assertEquals(1, usable - (heights.sum() + gaps))
    }

    /** No block may ever be shortened, whatever the weights say. */
    @Test
    fun `a floor is never taken away`() {
        val floors = intArrayOf(200, 10, 10)
        val heights = grow(
            floors = floors,
            weights = floatArrayOf(1f, 1f, 0f),
            ceilings = intArrayOf(50, 50, Int.MAX_VALUE),
            free = 100,
        )
        for (i in floors.indices) {
            assertTrue("block $i shrank", heights[i] >= floors[i])
        }
        assertEquals(200, heights[0])
        assertEquals(50, heights[1])
    }

    /** Rounding dust is left for the gaps rather than spun on forever. */
    @Test
    fun `an odd pixel does not hang the distribution`() {
        val heights = grow(
            floors = intArrayOf(0, 0, 0),
            weights = floatArrayOf(1f, 1f, 1f),
            free = 2,
        )
        assertTrue(heights.sum() <= 2)
    }
}
