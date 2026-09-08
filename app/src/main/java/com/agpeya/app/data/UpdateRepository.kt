package com.agpeya.app.data

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.agpeya.app.BuildConfig
import kotlinx.coroutines.Dispatchers
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
 * browser, the same way [com.agpeya.app.ui.settings.AboutScreen] opens a link.
 *
 * Because the only outcome is a line the person reads when the app is open,
 * there is no background worker and no scheduler. The check runs at launch, at
 * most once a day, and fails silently — an update notice is not worth an error
 * message, and a person praying offline should never learn that from ስንቅ.
 *
 * All of that is for the hand-installed build alone. A build from Play compiles
 * with `BuildConfig.UPDATE_NOTICE` false and this object goes dark: no request,
 * no stored release, no line — because pointing a Play install at a page of
 * APKs is exactly what the store forbids, and Play already updates itself. The
 * gate sits here rather than at the two call sites so there is one place to be
 * right about, and every caller is right by construction.
 */
object UpdateRepository {

    private const val TAG = "UpdateRepository"
    private const val LATEST = "https://api.github.com/repos/natinael96/sinq/releases/latest"
    private const val RELEASES_PAGE = "https://github.com/natinael96/sinq/releases/latest"

    private val KEY_LATEST_VERSION = stringPreferencesKey("latest_version")
    private val KEY_LATEST_URL = stringPreferencesKey("latest_url")
    private val KEY_DISMISSED = stringPreferencesKey("dismissed_version")
    private val KEY_ETAG = stringPreferencesKey("etag")

    /** What the line needs to draw itself, or null when there is nothing to say. */
    data class Available(val version: String, val url: String)

    /**
     * The release to announce: newer than what is installed, and not one the
     * person has already waved away. Null the rest of the time — which is most
     * of the time, and draws nothing.
     */
    fun available(context: Context): Flow<Available?> =
        if (!BuildConfig.UPDATE_NOTICE) flowOf(null)
        else context.updateDataStore.data.map { prefs ->
            val latest = prefs[KEY_LATEST_VERSION].orEmpty()
            if (latest.isBlank()) return@map null
            if (prefs[KEY_DISMISSED] == latest) return@map null
            if (!isNewer(latest, installedVersion(context))) return@map null
            Available(latest, prefs[KEY_LATEST_URL]?.ifBlank { null } ?: RELEASES_PAGE)
        }

    /** Hide THIS version for good; the next one will raise the line again. */
    suspend fun dismiss(context: Context, version: String) {
        context.updateDataStore.edit { it[KEY_DISMISSED] = version }
    }

    /** The running app's own versionName, e.g. "1.6.1". */
    fun installedVersion(context: Context): String = runCatching {
        val pm = context.packageManager
        @Suppress("DEPRECATION")
        pm.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")

    /**
     * Ask GitHub, once per app launch, if [enabled].
     *
     * Not throttled to a day: the answer is only ever read while the app is
     * open, so checking when it opens is the one moment it can matter, and a
     * release published this morning should not wait until tomorrow to be
     * announced. The stored ETag makes the repeat cheap — an unchanged release
     * answers 304 with no body.
     *
     * Every failure — no network, no GitHub, a rate limit, a body that does not
     * parse — is swallowed: the line simply does not appear.
     */
    suspend fun check(context: Context, now: Long = System.currentTimeMillis()) {
        // The only switch that matters is the build's own: the .aab for Play is
        // built without UPDATE_NOTICE, because a Play build pointing at a page
        // of APKs is a policy breach. The hand-installed APK is the one that
        // checks, and someone who installed it that way wants to know.
        if (!BuildConfig.UPDATE_NOTICE) return
        val prefs = context.updateDataStore.data.first()
        val etag = prefs[KEY_ETAG].orEmpty()
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(LATEST).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    // Identifies the app to GitHub; they ask for a User-Agent.
                    setRequestProperty("User-Agent", "Sinq-Android")
                    // An unchanged release answers 304 with no body, which costs
                    // nothing against the unauthenticated rate limit.
                    if (etag.isNotBlank()) setRequestProperty("If-None-Match", etag)
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
                        // Anything else (rate limit, 5xx) — try again next launch.
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
