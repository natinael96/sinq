package com.agpeya.app.data

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class BackupScopeTest {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test fun `unselected offering settings remain absent after round trip`() {
        val original = BackupRepository.Backup()
        val restored = json.decodeFromString(BackupRepository.Backup.serializer(),
            json.encodeToString(BackupRepository.Backup.serializer(), original))
        assertNull(restored.tithePercent)
        assertNull(restored.currency)
    }

    @Test fun `version two offering preferences still decode`() {
        val restored = json.decodeFromString(BackupRepository.Backup.serializer(),
            """{"version":2,"tithePercent":10,"currency":"ETB"}""")
        assertEquals(10, restored.tithePercent)
        assertEquals("ETB", restored.currency)
    }

    @Test fun `selected empty currency is distinct from omitted currency`() {
        val restored = json.decodeFromString(BackupRepository.Backup.serializer(),
            """{"version":3,"tithePercent":15,"currency":""}""")
        assertEquals("", restored.currency)
        assertEquals(15, restored.tithePercent)
    }
}
