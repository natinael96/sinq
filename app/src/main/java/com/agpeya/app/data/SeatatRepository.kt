package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.SeatatContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Loads the bundled ሰዓታት (Seatat) from assets/content/seatat/. One small file,
 * loaded once and cached — the same shape as [WudaseRepository].
 */
object SeatatRepository {

    private const val TAG = "SeatatRepository"
    private const val PATH = "content/seatat/seatat.json"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: SeatatContent? = null

    suspend fun load(context: Context): SeatatContent =
        cache ?: withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.applicationContext.assets
                    .open(PATH).readBytes().decodeToString()
                json.decodeFromString<SeatatContent>(raw)
            }.onFailure { Log.e(TAG, "Failed to load seatat", it) }
                // Cache only success — a transient failure shouldn't stick as an
                // empty screen for the rest of the process lifetime.
                .getOrNull()?.also { cache = it } ?: SeatatContent()
        }
}
