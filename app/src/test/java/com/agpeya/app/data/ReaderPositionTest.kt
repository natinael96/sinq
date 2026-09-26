package com.agpeya.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderPositionTest {
    @Test fun `saved section follows reordering instead of obsolete index`() {
        val saved = UserDataRepository.ReaderPosition("gospel", 2, 120)
        assertEquals(0, UserDataRepository.resolvePosition(saved, listOf("gospel", "psalm", "litany"), 2))
    }
    @Test fun `removed section falls back within the visible list`() {
        val saved = UserDataRepository.ReaderPosition("removed", 8)
        assertEquals(1, UserDataRepository.resolvePosition(saved, listOf("psalm", "gospel"), 8))
    }
}
