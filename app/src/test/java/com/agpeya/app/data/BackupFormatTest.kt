package com.agpeya.app.data

import com.agpeya.app.model.HoursConfig
import com.agpeya.app.model.ModesState
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
