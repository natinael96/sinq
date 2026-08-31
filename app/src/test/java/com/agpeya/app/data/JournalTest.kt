package com.agpeya.app.data

import com.agpeya.app.model.DayContext
import com.agpeya.app.model.JournalEntry
import com.agpeya.app.model.JournalKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The journal's load-bearing rules: what may leave the device, and the
 * passphrase derivation behind the gate. Being wrong about the first means
 * exposing what someone wrote in confidence.
 */
class JournalTest {

    private fun entry(kind: JournalKind, body: String = "ጸሎት") = JournalEntry(
        id = "e-${kind.name}",
        date = "2026-08-31",
        kind = kind,
        body = body,
        context = DayContext(ethYear = 2018, ethMonth = 12, ethDay = 25),
    )

    // ── What may leave the device ────────────────────────────────────────────

    @Test
    fun `confession drafts are never exportable, everything else is`() {
        assertFalse(entry(JournalKind.CONFESSION_DRAFT).exportable)
        assertTrue(entry(JournalKind.REFLECTION).exportable)
        assertTrue(entry(JournalKind.PASSAGE).exportable)
    }

    @Test
    fun `only a confession draft counts as a draft`() {
        assertTrue(entry(JournalKind.CONFESSION_DRAFT).isDraft)
        assertFalse(entry(JournalKind.REFLECTION).isDraft)
    }

    @Test
    fun `the preview is the first non-blank line, trimmed`() {
        assertEquals("ዛሬ ደስ ብሎኛል", entry(JournalKind.REFLECTION, "\n\n  ዛሬ ደስ ብሎኛል  \nሁለተኛ መስመር").preview)
        assertEquals("", entry(JournalKind.REFLECTION, "   \n  ").preview)
    }

    @Test
    fun `an unknown stored kind reads back as a plain reflection, never a draft`() {
        // Reordering the enum must never reclassify someone's reflection as a
        // confession — the converter stores names and fails safe on the way in.
        val converters = JournalConverters()
        assertEquals(JournalKind.REFLECTION, converters.toKind("SOMETHING_ELSE"))
        assertEquals(JournalKind.CONFESSION_DRAFT, converters.toKind("CONFESSION_DRAFT"))
        assertEquals("CONFESSION_DRAFT", converters.fromKind(JournalKind.CONFESSION_DRAFT))
    }

    // ── The passphrase gate ──────────────────────────────────────────────────

    @Test
    fun `the key derivation is deterministic per salt and separated across salts`() {
        val salt = ByteArray(16) { it.toByte() }
        val other = ByteArray(16) { (it + 1).toByte() }
        assertTrue(JournalLock.derive("pass", salt).contentEquals(JournalLock.derive("pass", salt)))
        assertFalse(JournalLock.derive("pass", salt).contentEquals(JournalLock.derive("pass", other)))
        assertFalse(JournalLock.derive("pass", salt).contentEquals(JournalLock.derive("Pass", salt)))
        assertEquals("256-bit key", 32, JournalLock.derive("pass", salt).size)
    }

}
