package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.Hour
import com.agpeya.app.model.Manifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Loads the bundled prayer content from assets once and caches it in memory
 * (~76k chars total — small enough to hold). Replaced by Room when search
 * and bookmarks arrive (PLAN.md Phase 5).
 */
object ContentRepository {

    private const val TAG = "ContentRepository"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: List<Hour>? = null

    @Volatile
    private var psalterCache: List<com.agpeya.app.model.Section>? = null

    /** All 150 psalms, ordered by number — used by the "add psalm" picker. */
    suspend fun psalter(context: Context): List<com.agpeya.app.model.Section> =
        psalterCache ?: withContext(Dispatchers.IO) {
            val assets = context.applicationContext.assets
            val loaded = runCatching {
                json.decodeFromString<com.agpeya.app.model.Psalter>(
                    assets.open("content/psalms.json").readBytes().decodeToString()
                ).psalms
            }.onFailure { Log.e(TAG, "Failed to load psalms.json", it) }
                .getOrDefault(emptyList())
            loaded.also { psalterCache = it }
        }

    suspend fun psalm(context: Context, number: Int): com.agpeya.app.model.Section? =
        psalter(context).find { it.number == number }

    suspend fun hours(context: Context): List<Hour> =
        cache ?: withContext(Dispatchers.IO) {
            load(context.applicationContext).also { cache = it }
        }

    suspend fun hour(context: Context, hourId: String): Hour? =
        hours(context).find { it.id == hourId }

    private fun load(context: Context): List<Hour> = runCatching {
        val assets = context.assets
        val manifest = json.decodeFromString<Manifest>(
            assets.open("content/manifest.json").readBytes().decodeToString()
        )
        // A single corrupt hour file is skipped rather than sinking the whole app.
        manifest.hours.mapNotNull { entry ->
            runCatching {
                json.decodeFromString<Hour>(
                    assets.open("content/${entry.file}").readBytes().decodeToString()
                )
            }.onFailure { Log.e(TAG, "Failed to load hour ${entry.file}", it) }.getOrNull()
        }.sortedBy { it.orderIndex }
    }.onFailure { Log.e(TAG, "Failed to load manifest.json", it) }.getOrDefault(emptyList())

    /** Which hour fits the current time of day — drives the Home suggestion. */
    fun suggestedHourId(hourOfDay: Int): String = when (hourOfDay) {
        in 4..7 -> "morning"
        in 8..10 -> "terce"
        in 11..13 -> "sext"
        in 14..16 -> "none"
        in 17..19 -> "vespers"
        in 20..22 -> "compline"
        else -> "midnight"
    }
}
