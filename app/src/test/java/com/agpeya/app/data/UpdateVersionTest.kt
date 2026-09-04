package com.agpeya.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The version compare behind the update line. Getting this wrong either nags
 * someone about a release they already have, or stays silent about a real one.
 */
class UpdateVersionTest {

    @Test
    fun `a later release is newer`() {
        assertTrue(UpdateRepository.isNewer("1.7.0", "1.6.1"))
        assertTrue(UpdateRepository.isNewer("1.6.2", "1.6.1"))
        assertTrue(UpdateRepository.isNewer("2.0.0", "1.9.9"))
    }

    @Test
    fun `the same release is not newer`() {
        assertFalse(UpdateRepository.isNewer("1.6.1", "1.6.1"))
        assertFalse(UpdateRepository.isNewer("v1.6.1", "1.6.1"))
    }

    @Test
    fun `an older release is not newer`() {
        assertFalse(UpdateRepository.isNewer("1.6.0", "1.6.1"))
        assertFalse(UpdateRepository.isNewer("1.5.9", "1.6.1"))
    }

    @Test
    fun `numbers compare as numbers, not as text`() {
        // The whole reason this function exists: "1.10.0" sorts BEFORE "1.9.0"
        // as a string, and would silently hide every release after 1.9.
        assertTrue(UpdateRepository.isNewer("1.10.0", "1.9.0"))
        assertFalse(UpdateRepository.isNewer("1.9.0", "1.10.0"))
        assertTrue(UpdateRepository.isNewer("1.6.10", "1.6.9"))
    }

    @Test
    fun `a leading v is not part of the number`() {
        assertEquals("1.7.0", UpdateRepository.normalise("v1.7.0"))
        assertEquals("1.7.0", UpdateRepository.normalise(" V1.7.0 "))
        assertTrue(UpdateRepository.isNewer("v1.7.0", "1.6.1"))
    }

    @Test
    fun `a missing part counts as zero`() {
        assertTrue(UpdateRepository.isNewer("1.7", "1.6.9"))
        assertFalse(UpdateRepository.isNewer("1.6", "1.6.0"))
        assertTrue(UpdateRepository.isNewer("1.6.0.1", "1.6.0"))
    }

    @Test
    fun `an unreadable tag never nags`() {
        // A malformed or unexpected tag must fail closed: no line at all,
        // rather than a notice about an update that may not exist.
        assertFalse(UpdateRepository.isNewer("", "1.6.1"))
        assertFalse(UpdateRepository.isNewer("nightly", "1.6.1"))
        assertFalse(UpdateRepository.isNewer("1.7.0", ""))
    }

    @Test
    fun `a suffixed tag still compares on its numbers`() {
        assertTrue(UpdateRepository.isNewer("1.7.0-beta", "1.6.1"))
    }
}
