package com.agpeya.app.ui.journal

import org.junit.Assert.*
import org.junit.Test

class JournalAccessTest {
    @Test fun `unknown lock state stays closed even with prior authorization`() {
        assertFalse(JournalSession.mayRead(null, false))
        assertFalse(JournalSession.mayRead(null, true))
    }
    @Test fun `locked records require authentication`() {
        assertFalse(JournalSession.mayRead(true, false))
        assertTrue(JournalSession.mayRead(true, true))
        assertTrue(JournalSession.mayRead(false, false))
    }
    @Test fun `ending foreground session revokes private access`() {
        JournalSession.authenticated.value = true
        JournalSession.lock()
        assertFalse(JournalSession.mayRead(true, JournalSession.authenticated.value))
    }
}
