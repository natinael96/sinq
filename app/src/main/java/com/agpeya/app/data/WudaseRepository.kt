package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.WudaseContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Loads the bundled ውዳሴ ማርያም (Wudase Maryam) from assets/content/wudase/. The
 * whole book is one small file, loaded once and cached.
 */
object WudaseRepository {

    private const val TAG = "WudaseRepository"
    private const val PATH = "content/wudase/wudase.json"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: WudaseContent? = null

    suspend fun load(context: Context): WudaseContent =
        cache ?: withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.applicationContext.assets
                    .open(PATH).readBytes().decodeToString()
                json.decodeFromString<WudaseContent>(raw)
            }.onFailure { Log.e(TAG, "Failed to load wudase", it) }
                .getOrDefault(WudaseContent())
                .also { cache = it }
        }
}
