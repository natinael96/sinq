package com.agpeya.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The journal passphrase must keep verifying across an Android upgrade.
 *
 * A hash made with SHA-1 on Android 6 has to stay verifiable when the same
 * person later runs Android 8 — where the app would otherwise pick SHA-256 and
 * silently reject a correct passphrase, locking them out of their own journal.
 */
class JournalLockAlgorithmTest {

    private val salt = ByteArray(16) { it.toByte() }

    @Test
    fun `each algorithm is stable for the same passphrase and salt`() {
        for (algo in listOf("PBKDF2WithHmacSHA256", "PBKDF2WithHmacSHA1")) {
            assertTrue(
                algo,
                JournalLock.derive("pass", salt, algo)
                    .contentEquals(JournalLock.derive("pass", salt, algo)),
            )
        }
    }

    @Test
    fun `both algorithms produce a 256-bit key`() {
        assertEquals(32, JournalLock.derive("pass", salt, "PBKDF2WithHmacSHA256").size)
        assertEquals(32, JournalLock.derive("pass", salt, "PBKDF2WithHmacSHA1").size)
    }

    @Test
    fun `the two algorithms do not agree, which is why the choice is recorded`() {
        assertFalse(
            JournalLock.derive("pass", salt, "PBKDF2WithHmacSHA256")
                .contentEquals(JournalLock.derive("pass", salt, "PBKDF2WithHmacSHA1")),
        )
    }

    @Test
    fun `a wrong passphrase fails under either algorithm`() {
        for (algo in listOf("PBKDF2WithHmacSHA256", "PBKDF2WithHmacSHA1")) {
            assertFalse(
                algo,
                JournalLock.derive("pass", salt, algo)
                    .contentEquals(JournalLock.derive("Pass", salt, algo)),
            )
        }
    }

    @Test
    fun `a JVM offers SHA-256, so desktop tests exercise the same path as a modern phone`() {
        assertEquals("PBKDF2WithHmacSHA256", JournalLock.preferredAlgorithm())
    }
}
