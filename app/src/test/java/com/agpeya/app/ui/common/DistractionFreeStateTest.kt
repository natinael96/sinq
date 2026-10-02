package com.agpeya.app.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DistractionFreeStateTest {

    @Test
    fun `starts non-immersive by default`() {
        val state = DistractionFreeState()
        assertFalse(state.isImmersive)
        assertEquals(0, state.interactionCount)
    }

    @Test
    fun `toggle switches between states and increments interaction`() {
        val state = DistractionFreeState()

        state.toggle()
        assertTrue(state.isImmersive)
        assertEquals(1, state.interactionCount)

        state.toggle()
        assertFalse(state.isImmersive)
        assertEquals(2, state.interactionCount)
    }

    @Test
    fun `show reveals controls only when immersive`() {
        val state = DistractionFreeState(initialImmersive = true)

        state.show()
        assertFalse(state.isImmersive)
        assertEquals(1, state.interactionCount)

        // Calling show again when already visible does nothing
        state.show()
        assertFalse(state.isImmersive)
        assertEquals(1, state.interactionCount)
    }

    @Test
    fun `hide enters immersion only when visible`() {
        val state = DistractionFreeState(initialImmersive = false)

        state.hide()
        assertTrue(state.isImmersive)
        assertEquals(1, state.interactionCount)

        // Calling hide again when already hidden does nothing
        state.hide()
        assertTrue(state.isImmersive)
        assertEquals(1, state.interactionCount)
    }
}
