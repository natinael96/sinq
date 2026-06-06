package com.agpeya.app.data

import android.content.Context
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

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: List<Hour>? = null

    suspend fun hours(context: Context): List<Hour> =
        cache ?: withContext(Dispatchers.IO) {
            load(context.applicationContext).also { cache = it }
        }

    suspend fun hour(context: Context, hourId: String): Hour? =
        hours(context).find { it.id == hourId }

    private fun load(context: Context): List<Hour> {
        val assets = context.assets
        val manifest = json.decodeFromString<Manifest>(
            assets.open("content/manifest.json").readBytes().decodeToString()
        )
        return manifest.hours.map { entry ->
            json.decodeFromString<Hour>(
                assets.open("content/${entry.file}").readBytes().decodeToString()
            )
        }.sortedBy { it.orderIndex }
    }

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
