package com.agpeya.app.ui.reading

import org.junit.Assert.*
import org.junit.Test

class ReadingMapProgressTest {
    @Test fun `reading chapter ten does not complete chapter one`() {
        val book = ReadingMapBook("genesis", "Genesis", 50, "Torah", setOf(10))
        assertEquals(1, book.read)
        assertFalse(1 in book.readChapterNumbers)
        assertTrue(10 in book.readChapterNumbers)
        assertEquals(1, book.nextUnread)
    }
    @Test fun `next unread skips actual completed chapters`() {
        val book = ReadingMapBook("genesis", "Genesis", 50, "Torah", setOf(1, 2, 10))
        assertEquals(3, book.nextUnread)
    }
    @Test fun `completed book has no next unread chapter`() {
        val book = ReadingMapBook("genesis", "Genesis", 50, "Torah", (1..50).toSet())
        assertNull(book.nextUnread)
    }
}
