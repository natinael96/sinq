package com.agpeya.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class HighlightRepositoryTest {

    @Test
    fun `legacy psalter highlights migrate to Amharic 1980`() {
        val migrated = HighlightRepository.migrateLegacyPsalterMap(
            linkedMapOf(
                "ps_50:3" to "yellow",
                "morning_gospel:2" to "green",
                "gez-1980:ps_50:3" to "blue",
            ),
        )

        assertEquals("yellow", migrated["am-1980:ps_50:3"])
        assertEquals("green", migrated["morning_gospel:2"])
        assertEquals("blue", migrated["gez-1980:ps_50:3"])
        assertFalse("ps_50:3" in migrated)
    }

    @Test
    fun `existing edition-specific highlight wins during migration`() {
        val migrated = HighlightRepository.migrateLegacyPsalterMap(
            linkedMapOf("ps_23:1" to "yellow", "am-1980:ps_23:1" to "pink"),
        )

        assertEquals("pink", migrated["am-1980:ps_23:1"])
    }

    @Test
    fun `already migrated map is returned unchanged`() {
        val source = mapOf("am-1980:ps_1:1" to "yellow")
        assertSame(source, HighlightRepository.migrateLegacyPsalterMap(source))
    }
}
