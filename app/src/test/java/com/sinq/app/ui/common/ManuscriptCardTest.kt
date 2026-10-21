package com.sinq.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ManuscriptCardTest {

    @Test
    fun `aspect ratios have exact English labels and dimensions`() {
        assertEquals("Square (1:1)", CardAspectRatio.SQUARE.label)
        assertEquals(1080, CardAspectRatio.SQUARE.width)
        assertEquals(1080, CardAspectRatio.SQUARE.height)

        assertEquals("Story (9:16)", CardAspectRatio.STORY.label)
        assertEquals(1080, CardAspectRatio.STORY.width)
        assertEquals(1920, CardAspectRatio.STORY.height)
    }
}
