package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.journalLockStore by preferencesDataStore(name = "journal_lock")

/**
 * The passphrase that opens the journal.
 *
 * **What this does and does not do.** The journal body is stored in plaintext
 * in SQLite. On a device with a screen lock, Android's own file-based
 * encryption already protects that file at rest, and the passphrase here keeps
 * someone out of the journal while browsing an unlocked phone. It does NOT
 * protect against root, adb, or anyone holding a copy of the database file.
 * That was a deliberate decision, taken to avoid shipping SQLCipher's native
 * libraries and the Room 3 dead end that comes with them.
 *
 * The export is the one place that trade would have been genuinely dangerous —
 * a backup file gets mailed to oneself, dropped in Drive, handed to a relative
 * — so an export carrying journal entries IS encrypted, under the passphrase
 * the person types at the time. Once that file leaves the device it is theirs
 * to look after; the app's part is making sure it is not readable by whoever
 * finds it.
 *
 * The passphrase itself is never stored. Only a salted PBKDF2 hash is kept, so
 * reading this DataStore tells an attacker nothing they can use.
 */
object JournalLock {

    private val KEY_SALT = stringPreferencesKey("passphrase_salt")
    private val KEY_HASH = stringPreferencesKey("passphrase_hash")

    // PBKDF2 with a deliberately high iteration count: the passphrase is
    // typed by a person, so it is short and low-entropy, and the only defence
    // against guessing it offline is making each guess expensive.
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val PBKDF2 = "PBKDF2WithHmacSHA256"

    private val random = SecureRandom()

    // ── The lock ─────────────────────────────────────────────────────────────

    /** Whether a passphrase has been set. Drives whether the journal asks. */
    fun isLocked(context: Context): Flow<Boolean> =
        context.journalLockStore.data.map { it[KEY_HASH] != null }

    /**
     * Set or replace the passphrase.
     *
     * Changing it re-hashes with a fresh salt. Nothing else has to change,
     * because the passphrase encrypts nothing — it is a gate, not a key. A
     * backup already written stays exactly as readable as it was.
     */
    suspend fun setPassphrase(context: Context, passphrase: String) {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val hash = withContext(Dispatchers.Default) { derive(passphrase, salt) }
        context.journalLockStore.edit {
            it[KEY_SALT] = salt.encode()
            it[KEY_HASH] = hash.encode()
        }
    }

    /** Remove the lock entirely. The entries themselves are untouched. */
    suspend fun clearPassphrase(context: Context) {
        context.journalLockStore.edit {
            it.remove(KEY_SALT)
            it.remove(KEY_HASH)
        }
    }

    /**
     * True when [passphrase] matches. Compared in constant time so a wrong
     * guess cannot be narrowed down by how long the check took.
     */
    suspend fun verify(context: Context, passphrase: String): Boolean {
        val prefs = context.journalLockStore.data.first()
        val salt = prefs[KEY_SALT]?.decode() ?: return false
        val expected = prefs[KEY_HASH]?.decode() ?: return false
        val actual = withContext(Dispatchers.Default) { derive(passphrase, salt) }
        return constantTimeEquals(expected, actual)
    }

    // ── Internals ────────────────────────────────────────────────────────────

    internal fun derive(passphrase: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance(PBKDF2)
            .generateSecret(PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_BITS))
            .encoded

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    // java.util.Base64 (API 26+, which is this app's minSdk) rather than
    // android.util.Base64: it is unwrapped by default, so the values ride
    // safely inside a JSON document, and — unlike the Android one — it is real
    // code in a JVM unit test rather than a stub that throws.
    private fun ByteArray.encode(): String = Base64.getEncoder().encodeToString(this)
    private fun String.decode(): ByteArray = Base64.getDecoder().decode(this)
}
