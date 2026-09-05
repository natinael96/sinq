package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.Tour
import com.agpeya.app.model.TourContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * The walkthrough shown once after an update, and once after a first install.
 *
 * It is not the changelog. The changelog says what changed in a line; this says
 * what a thing is for and where it lives, at length, and offers to open it.
 *
 * Keyed on versionCode rather than versionName: it is the number the app's own
 * versioning policy guarantees increases on every release, so it cannot be
 * fooled by a re-tagged or hand-edited name.
 */
object TourRepository {

    private const val TAG = "TourRepository"
    private const val PATH = "content/tour/tour.json"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: TourContent? = null

    suspend fun content(context: Context): TourContent =
        cache ?: withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.applicationContext.assets
                    .open(PATH).readBytes().decodeToString()
                json.decodeFromString<TourContent>(raw)
            }.onFailure { Log.e(TAG, "Failed to load tour", it) }
                .getOrNull()?.also { cache = it } ?: TourContent()
        }

    /** This build's versionCode, which is what the tour is keyed to. */
    fun installedVersionCode(context: Context): Int = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }
    }.getOrDefault(0)

    /**
     * The tour to show now, or null.
     *
     * Only ever the newest one that applies: someone returning after several
     * releases wants to know where the app stands today, not to read four
     * tours back to back.
     *
     * [lastSeen] null means the tour has never run on this device — a first
     * install, or an upgrade from a build made before the tour existed. Both
     * get the newest tour, so nobody arrives at the app with no idea what is
     * in it.
     */
    fun pending(tours: List<Tour>, lastSeen: Int?, installed: Int): Tour? {
        if (installed <= 0) return null
        if (lastSeen != null && lastSeen >= installed) return null
        return tours
            .filter { it.versionCode <= installed && it.pages.isNotEmpty() }
            .maxByOrNull { it.versionCode }
    }
}
