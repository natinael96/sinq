package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.KurbanContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Loads the bundled ቁርባን preparation content — the rules of approach and the
 * prayers before and after receiving — from assets/content/kurban/. One small
 * file, loaded once and cached.
 */
object KurbanRepository {

    private const val TAG = "KurbanRepository"
    private const val PATH = "content/kurban/kurban.json"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: KurbanContent? = null

    suspend fun load(context: Context): KurbanContent =
        cache ?: withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.applicationContext.assets
                    .open(PATH).readBytes().decodeToString()
                json.decodeFromString<KurbanContent>(raw)
            }.onFailure { Log.e(TAG, "Failed to load kurban", it) }
                // Cache only success — a transient failure shouldn't stick as an
                // empty screen for the rest of the process lifetime.
                .getOrNull()?.also { cache = it } ?: KurbanContent()
        }
}
