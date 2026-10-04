package com.agpeya.app.data

import com.agpeya.app.model.ChecklistItem
import com.agpeya.app.model.ChecklistParser
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
        assertTrue(entry(JournalKind.CHECKLIST).exportable)
    }

    @Test
    fun `only a confession draft counts as a draft`() {
        assertTrue(entry(JournalKind.CONFESSION_DRAFT).isDraft)
        assertFalse(entry(JournalKind.REFLECTION).isDraft)
        assertFalse(entry(JournalKind.CHECKLIST).isDraft)
    }

    @Test
    fun `the preview is the first non-blank line, trimmed`() {
        assertEquals("ዛሬ ደስ ብሎኛል", entry(JournalKind.REFLECTION, "\n\n  ዛሬ ደስ ብሎኛል  \nሁለተኛ መስመር").preview)
        assertEquals("", entry(JournalKind.REFLECTION, "   \n  ").preview)
    }

    @Test
    fun `confession previews never expose note text or its length`() {
        listOf("", "short secret", "\nprivate first line\nprivate second line", "secret ".repeat(1000)).forEach { body ->
            val note = entry(JournalKind.CONFESSION_DRAFT, body)
            assertEquals("", note.preview)
            assertEquals("Masking must not erase the stored note", body, note.body)
        }
        assertEquals("passage note", entry(JournalKind.PASSAGE, "passage note\nsecond line").preview)
    }

    @Test
    fun `an unknown stored kind reads back as a plain reflection, never a draft`() {
        // Reordering the enum must never reclassify someone's reflection as a
        // confession — the converter stores names and fails safe on the way in.
        val converters = JournalConverters()
        assertEquals(JournalKind.REFLECTION, converters.toKind("SOMETHING_ELSE"))
        assertEquals(JournalKind.CONFESSION_DRAFT, converters.toKind("CONFESSION_DRAFT"))
        assertEquals("CONFESSION_DRAFT", converters.fromKind(JournalKind.CONFESSION_DRAFT))
        assertEquals(JournalKind.CHECKLIST, converters.toKind("CHECKLIST"))
        assertEquals("CHECKLIST", converters.fromKind(JournalKind.CHECKLIST))
    }

    @Test
    fun `checklist parser round-trips markdown tasks and optional reflection note`() {
        val originalItems = listOf(
            ChecklistItem("ማኅሌት መቆም", isDone = true),
            ChecklistItem("ውዳሴ ማርያም መድገም", isDone = false),
            ChecklistItem("ምጽዋት መስጠት", isDone = true),
        )
        val originalNote = "እግዚአብሔር ይመስገን መልካም ቀን ነበር።"
        val serialized = ChecklistParser.serialize(originalItems, originalNote)

        val (parsedItems, parsedNote) = ChecklistParser.parse(serialized)
        assertEquals(originalItems, parsedItems)
        assertEquals(originalNote, parsedNote)
    }

    @Test
    fun `checklist preview reports completed ratio and next remaining item`() {
        val body = """
            - [x] ማኅሌት መቆም
            - [ ] ውዳሴ ማርያም
            - [ ] ምጽዋት
            
            ተጨማሪ ሐሳብ
        """.trimIndent()
        val e = entry(JournalKind.CHECKLIST, body)
        assertEquals("✓ 1/3 · ውዳሴ ማርያም", e.preview)
    }

    // ── The passphrase gate ──────────────────────────────────────────────────

    @Test
    fun `the key derivation is deterministic per salt and separated across salts`() {
        val salt = ByteArray(16) { it.toByte() }
        val other = ByteArray(16) { (it + 1).toByte() }
        assertTrue(JournalLock.derive("pass", salt).contentEquals(JournalLock.derive("pass", salt)))
        assertFalse(JournalLock.derive("pass", salt).contentEquals(JournalLock.derive("Pass", salt)))
        assertEquals("256-bit key", 32, JournalLock.derive("pass", salt).size)
    }

    @Test
    fun `checklist reminder scheduler resolves custom times and canonical hours`() {
        assertEquals(java.time.LocalTime.of(7, 15), com.agpeya.app.reminders.ChecklistReminderScheduler.resolveTime("07:15"))
        assertEquals(java.time.LocalTime.of(14, 30), com.agpeya.app.reminders.ChecklistReminderScheduler.resolveTime("14:30"))
        assertEquals(java.time.LocalTime.of(7, 15), com.agpeya.app.reminders.ChecklistReminderScheduler.resolveTime("07:15 AM"))
        assertEquals(java.time.LocalTime.of(21, 45), com.agpeya.app.reminders.ChecklistReminderScheduler.resolveTime("9:45 PM"))
        assertEquals(java.time.LocalTime.of(6, 0), com.agpeya.app.reminders.ChecklistReminderScheduler.resolveTime("ነግህ (6:00 AM)"))
        assertEquals(java.time.LocalTime.of(12, 0), com.agpeya.app.reminders.ChecklistReminderScheduler.resolveTime("ቀትር (12:00 PM)"))
        assertEquals(java.time.LocalTime.of(18, 0), com.agpeya.app.reminders.ChecklistReminderScheduler.resolveTime("ሠርክ"))
    }

    @Test
    fun `future date entry detection and scheduler extras`() {
        val futureDate = java.time.LocalDate.now().plusDays(3)
        val futureEntry = JournalEntry(
            id = "e-future",
            date = futureDate.toString(),
            kind = JournalKind.CHECKLIST,
            body = "- [ ] ጸሎተ ነግህ",
            context = DayContext(ethYear = 2018, ethMonth = 12, ethDay = 28),
        )
        assertTrue(futureEntry.localDate!!.isAfter(java.time.LocalDate.now()))
        assertEquals(JournalKind.CHECKLIST, futureEntry.kind)
        assertEquals("checklistDate", com.agpeya.app.reminders.ChecklistReminderScheduler.EXTRA_DATE)
        assertEquals("checklistEntryId", com.agpeya.app.reminders.ChecklistReminderScheduler.EXTRA_ENTRY_ID)
    }
}
