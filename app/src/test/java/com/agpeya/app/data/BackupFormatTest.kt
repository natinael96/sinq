package com.agpeya.app.data

import com.agpeya.app.model.HoursConfig
import com.agpeya.app.model.ModesState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFormatTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `version one backup remains readable`() {
        val backup = json.decodeFromString(
            BackupRepository.Backup.serializer(),
            """{"version":1,"app":"Sinq","created":"2026-08-25"}""",
        )

        assertEquals(1, backup.version)
        assertNull(backup.modes)
        assertNull(backup.hours)
        assertNull(backup.settings)
    }

    @Test
    fun `version two configuration round trips`() {
        val original = BackupRepository.Backup(
            version = 2,
            modes = ModesState(activeModeId = ModesRepository.BUILT_IN_ID, modes = listOf(ModesRepository.builtInMode())),
            hours = HoursConfig(hidden = setOf("veil")),
            settings = SettingsRepository.BackupSettings(
                theme = ThemeChoice.DARK.name,
                language = Language.ENGLISH.name,
            ),
        )

        val restored = json.decodeFromString(
            BackupRepository.Backup.serializer(),
            json.encodeToString(BackupRepository.Backup.serializer(), original),
        )

        assertEquals(original, restored)
    }

    @Test
    fun `reading progress round trips and still carries no penance data`() {
        val original = BackupRepository.Backup(
            version = 2,
            readingPlan = com.agpeya.app.model.ReadingPlanState(
                activePlanId = "annual",
                startedOn = "2026-09-04",
                completedDays = mapOf("annual" to setOf(1, 2, 5)),
            ),
        )
        val restored = json.decodeFromString(
            BackupRepository.Backup.serializer(),
            json.encodeToString(BackupRepository.Backup.serializer(), original),
        )
        assertEquals(original.readingPlan, restored.readingPlan)
        assertEquals(setOf(1, 2, 5), restored.readingPlan.readDays("annual"))
    }

    @Test
    fun `a fully populated backup still carries no penance data`() {
        // ቀኖና is confessional material and must never enter the export. The
        // Backup model simply has no field for it; this locks that in, so
        // adding one is a conscious decision that fails a test first.
        //
        // Encoded from a populated backup rather than an empty one: an empty
        // Backup serialises to almost nothing, which would pass this assertion
        // no matter what the model grew later.
        val encoded = json.encodeToString(
            BackupRepository.Backup.serializer(),
            BackupRepository.Backup(
                version = 2,
                modes = ModesState(
                    activeModeId = ModesRepository.BUILT_IN_ID,
                    modes = listOf(ModesRepository.builtInMode()),
                ),
                hours = HoursConfig(hidden = setOf("veil")),
                settings = SettingsRepository.BackupSettings(
                    theme = ThemeChoice.DARK.name,
                    language = Language.ENGLISH.name,
                ),
            ),
        )
        assertTrue(encoded.length > 100)
        assertFalse(encoded.contains("penance", ignoreCase = true))
        assertFalse(encoded.contains("ቀኖና"))
    }
}
