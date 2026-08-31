package com.agpeya.app.data

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Frees the in-memory content caches when the system signals memory pressure.
 *
 * The scripture books, ስንክሳር months and the folded search index together can
 * hold tens of megabytes; all of it is rebuildable from bundled assets, so
 * under pressure it is simply dropped and reloaded lazily on next use.
 *
 * The app has no Application subclass, so each cache-owning repository calls
 * [ensureRegistered] with whatever Context passes through it; the first call
 * registers this object on the application context, the rest are a no-op.
 */
object CacheTrimmer : ComponentCallbacks2 {

    private val registered = AtomicBoolean(false)

    fun ensureRegistered(context: Context) {
        if (registered.compareAndSet(false, true)) {
            context.applicationContext.registerComponentCallbacks(this)
        }
    }

    override fun onTrimMemory(level: Int) {
        // BACKGROUND and above (API 34+ delivers only these), plus the
        // running-critical signal older platforms send while foregrounded.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        ) {
            ScriptureRepository.trimCaches()
            SynaxariumRepository.trimCaches()
            com.agpeya.app.search.AmharicSearch.trimCaches()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    @Deprecated("Platform callback; onTrimMemory carries the useful signal.")
    override fun onLowMemory() = onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
}
