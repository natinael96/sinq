package com.sinq.app.data

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sinq.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Context.updateDataStore by preferencesDataStore(name = "updates")

/**
 * The one place in ስንቅ that touches the network, and only when asked to.
 *
 * The app announces a new release with a line on ቤት — nothing more. It never
 * downloads or installs anything: the line opens the release page in the
 * browser, the same way [com.sinq.app.ui.settings.AboutScreen] opens a link.
 *
 * Checks whenever the app is opened, without permanent dismiss suppression,
 * so available updates are always announced unless dismissed for the current session.
 */
object UpdateRepository {

    private const val TAG = "UpdateRepository"
    private const val LATEST = "https://api.github.com/repos/natinael96/sinq/releases/latest"
    private const val RELEASES_PAGE = "https://github.com/natinael96/sinq/releases/latest"

    private val KEY_LATEST_VERSION = stringPreferencesKey("latest_version")
    private val KEY_LATEST_URL = stringPreferencesKey("latest_url")
    private val KEY_DISMISSED = stringPreferencesKey("dismissed_version")
    private val KEY_ETAG = stringPreferencesKey("etag")

    // In-memory dismissal scoped only to the current app session.
    // Resetting when the app opens ensures the user is always informed of updates.
    private val sessionDismissed = MutableStateFlow<String?>(null)

    /** What the line needs to draw itself, or null when there is nothing to say. */
    data class Available(val version: String, val url: String)

    /**
     * The release to announce: newer than what is installed, and not dismissed
     * for the current app session. Null the rest of the time.
     */
    fun available(context: Context): Flow<Available?> =
        if (!BuildConfig.UPDATE_NOTICE) flowOf(null)
        else combine(context.updateDataStore.data, sessionDismissed) { prefs, dismissedVersion ->
            val latest = prefs[KEY_LATEST_VERSION].orEmpty()
            if (latest.isBlank()) return@combine null
            if (dismissedVersion == latest) return@combine null
            if (!isNewer(latest, installedVersion(context))) return@combine null
            Available(latest, prefs[KEY_LATEST_URL]?.ifBlank { null } ?: RELEASES_PAGE)
        }

    /** Dismiss for the current session only; reopening the app will check and show it again. */
    suspend fun dismiss(context: Context, version: String) {
        sessionDismissed.value = version
        runCatching {
            context.updateDataStore.edit { it.remove(KEY_DISMISSED) }
        }
    }

    /** The running app's own versionName, e.g. "1.6.1". */
    fun installedVersion(context: Context): String = runCatching {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        pm.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")

    /**
     * Ask GitHub for the latest release whenever the app is opened.
     *
     * Stored ETag makes the check cheap (304 Not Modified).
     * All failures are swallowed silently.
     */
    suspend fun check(context: Context) {
        if (!BuildConfig.UPDATE_NOTICE) return
        val prefs = context.updateDataStore.data.first()
        val latest = prefs[KEY_LATEST_VERSION].orEmpty()
        val etag = prefs[KEY_ETAG].orEmpty()

        // Clean up legacy persistent dismissal so users aren't locked out
        if (prefs[KEY_DISMISSED] != null) {
            runCatching { context.updateDataStore.edit { it.remove(KEY_DISMISSED) } }
        }

        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(LATEST).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "Sinq-Android")
                    // Only send If-None-Match if we already hold a valid latest version
                    if (etag.isNotBlank() && latest.isNotBlank()) {
                        setRequestProperty("If-None-Match", etag)
                    }
                }
                try {
                    when (conn.responseCode) {
                        HttpURLConnection.HTTP_NOT_MODIFIED -> Unit
                        HttpURLConnection.HTTP_OK -> {
                            val body = conn.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(body)
                            val tag = json.optString("tag_name").orEmpty()
                            val page = json.optString("html_url").ifBlank { RELEASES_PAGE }
                            val newEtag = conn.getHeaderField("ETag").orEmpty()
                            context.updateDataStore.edit {
                                if (tag.isNotBlank()) {
                                    it[KEY_LATEST_VERSION] = normalise(tag)
                                    it[KEY_LATEST_URL] = page
                                }
                                if (newEtag.isNotBlank()) it[KEY_ETAG] = newEtag
                            }
                        }
                        else -> Unit
                    }
                } finally {
                    conn.disconnect()
                }
            }.onFailure { Log.d(TAG, "update check skipped: ${it.message}") }
        }
    }

    /** "v1.7.0" and "1.7.0" are the same release. */
    fun normalise(tag: String): String = tag.trim().removePrefix("v").removePrefix("V")

    /**
     * Whether [latest] is a later release than [installed].
     *
     * Compared number by number, never as text: "1.10.0" is newer than "1.9.0"
     * but sorts before it as a string. A missing or unparseable part counts as
     * 0, and anything that cannot be read at all is treated as "not newer" so a
     * malformed tag can never nag someone about an update that isn't there.
     */
    fun isNewer(latest: String, installed: String): Boolean {
        val a = parts(latest)
        val b = parts(installed)
        if (a.isEmpty() || b.isEmpty()) return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun parts(v: String): List<Int> =
        normalise(v).takeWhile { it.isDigit() || it == '.' }
            .split('.')
            .mapNotNull { it.toIntOrNull() }
}
