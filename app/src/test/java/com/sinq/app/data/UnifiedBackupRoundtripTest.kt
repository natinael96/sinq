package com.sinq.app.data

import com.sinq.app.model.Bookmark
import com.sinq.app.model.HabitsState
import com.sinq.app.model.JournalEntry
import com.sinq.app.model.JournalKind
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class UnifiedBackupRoundtripTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    private val canonicalJson = Json { ignoreUnknownKeys = true; prettyPrint = false; encodeDefaults = true }

    @Test
    fun `default backup filename matches format pattern`() {
        val now = LocalDateTime.of(2026, 10, 18, 23, 45)
        val name = BackupRepository.defaultBackupFilename(now)
        assertEquals("sinq-backup-20261018-2345.json", name)
        assertTrue(name.matches(Regex("sinq-backup-\\d{8}-\\d{4}\\.json")))
    }

    @Test
    fun `unified envelope roundtrips and passes checksum verification`() {
        val data = BackupRepository.BackupData(
            habits = HabitsState(records = mapOf("2026-10-18" to setOf("hour_terce", "hour_sext"))),
            bookmarks = listOf(
                Bookmark(
                    hourId = "morning",
                    hourName = "ጸሎተ ነግህ",
                    sectionId = "intro",
                    title = "መቅድመ ጸሎት"
                )
            ),
            highlights = mapOf("scripture:am-1980:gen:1:1" to "yellow"),
            journal = listOf(
                JournalEntry(
                    id = "entry-1",
                    date = "2026-10-18",
                    body = "Glory to God in the highest.",
                    kind = JournalKind.REFLECTION,
                    createdAt = 1000L,
                    updatedAt = 1000L,
                    context = com.sinq.app.model.DayContext(ethYear = 2019, ethMonth = 2, ethDay = 8),
                )
            ),
        )
        val dataJson = canonicalJson.encodeToString(BackupRepository.BackupData.serializer(), data)
        val checksum = BackupRepository.computeSha256(dataJson)

        val envelope = BackupRepository.UnifiedEnvelope(
            schemaVersion = BackupRepository.UNIFIED_SCHEMA_VERSION,
            appVersion = "Sinq-1.0",
            createdAt = "2026-10-18T23:45:00Z",
            contentEdition = "am-1980",
            checksum = checksum,
            data = data,
        )

        val raw = json.encodeToString(BackupRepository.UnifiedEnvelope.serializer(), envelope)
        val parsed = BackupRepository.parseJson(raw)

        assertNotNull("Valid envelope with matching checksum must parse successfully", parsed)
        val (restoredData, summary) = parsed!!
        assertEquals(4, summary.version)
        assertEquals(1, summary.days)
        assertEquals(1, summary.bookmarks)
        assertEquals(1, summary.highlights)
        assertEquals(1, summary.journalEntries)
        assertEquals(data.highlights, restoredData.highlights)
        assertEquals(data.journal, restoredData.journal)
    }

    @Test
    fun `tampered data block or invalid checksum fails parsing`() {
        val data = BackupRepository.BackupData(
            highlights = mapOf("scripture:am-1980:gen:1:1" to "yellow"),
        )
        val dataJson = canonicalJson.encodeToString(BackupRepository.BackupData.serializer(), data)
        val validChecksum = BackupRepository.computeSha256(dataJson)

        val corruptedEnvelope = BackupRepository.UnifiedEnvelope(
            schemaVersion = 4,
            checksum = "0000000000000000000000000000000000000000000000000000000000000000", // Wrong checksum!
            data = data,
        )
        val raw = json.encodeToString(BackupRepository.UnifiedEnvelope.serializer(), corruptedEnvelope)
        val parsed = BackupRepository.parseJson(raw)

        assertNull("Corrupted checksum must be rejected with null", parsed)
    }

    @Test
    fun `unsupported future schema version fails parsing`() {
        val data = BackupRepository.BackupData()
        val dataJson = canonicalJson.encodeToString(BackupRepository.BackupData.serializer(), data)
        val checksum = BackupRepository.computeSha256(dataJson)

        val futureEnvelope = BackupRepository.UnifiedEnvelope(
            schemaVersion = 99, // Future unsupported version
            checksum = checksum,
            data = data,
        )
        val raw = json.encodeToString(BackupRepository.UnifiedEnvelope.serializer(), futureEnvelope)
        val parsed = BackupRepository.parseJson(raw)

        assertNull("Future schema version must be rejected", parsed)
    }

    @Test
    fun `legacy version 2 and 3 backups parse successfully`() {
        val legacyJson = """
            {
              "version": 2,
              "app": "Sinq",
              "created": "2026-09-01",
              "habits": {
                "records": { "2026-09-01": ["hour_morning"] }
              },
              "bookmarks": [],
              "highlights": { "scripture:am-1980:gen:1:1": "yellow" }
            }
        """.trimIndent()

        val parsed = BackupRepository.parseJson(legacyJson)
        assertNotNull("Legacy v2 backup must parse cleanly", parsed)
        val (data, summary) = parsed!!
        assertEquals(2, summary.version)
        assertEquals(1, summary.days)
        assertEquals(1, summary.highlights)
        assertEquals("yellow", data.highlights["scripture:am-1980:gen:1:1"])
    }

    @Test
    fun `corrupted content-addressed key fails validation`() {
        val data = BackupRepository.BackupData(
            highlights = mapOf("../malicious/path" to "yellow"), // Invalid content key
        )
        val dataJson = canonicalJson.encodeToString(BackupRepository.BackupData.serializer(), data)
        val checksum = BackupRepository.computeSha256(dataJson)

        val envelope = BackupRepository.UnifiedEnvelope(
            schemaVersion = 4,
            checksum = checksum,
            data = data,
        )
        val raw = json.encodeToString(BackupRepository.UnifiedEnvelope.serializer(), envelope)
        val parsed = BackupRepository.parseJson(raw)

        assertNull("Malformed content-addressed keys must fail validation", parsed)
    }

    @Test
    fun `content key validator validates canonical contract`() {
        assertTrue(BackupRepository.isValidContentKey("scripture:am-1980:gen:1:1"))
        assertTrue(BackupRepository.isValidContentKey("am-1980:ps_23:1"))
        assertTrue(BackupRepository.isValidContentKey("gez-1980:ps_150:6"))
        assertTrue(BackupRepository.isValidContentKey("sinksar:7-27:deadbeef"))
        assertTrue(BackupRepository.isValidContentKey("terce:intro"))
        assertTrue(BackupRepository.isValidContentKey("sinksar_verse"))

        assertFalse(BackupRepository.isValidContentKey(""))
        assertFalse(BackupRepository.isValidContentKey("../secret"))
        assertFalse(BackupRepository.isValidContentKey("invalid\nkey"))
    }
}
