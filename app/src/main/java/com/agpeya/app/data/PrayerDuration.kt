package com.agpeya.app.data

import android.content.Context
import com.agpeya.app.model.Hour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * How long an hour takes to read, at the level it will actually be read at.
 *
 * The app has five prayer levels and has never said what any of them costs, so
 * the difference between them — ጸሎተ ነግህ is ፳ ደቂቃ at ሙሉ and ፭ at መጀመሪያ — was
 * invisible at the moment it matters, which is deciding whether to open it.
 *
 * The estimate is deliberately built on [PrayerLevelRepository.apply] rather
 * than on its own copy of the level rules: whatever the reader will be shown is
 * what gets counted, so the two can never drift apart.
 */
object PrayerDuration {

    /**
     * Ethiopic characters read silently, per minute.
     *
     * Each character is a syllable, so this is not comparable to a Latin-script
     * figure. It is the maintainer's own pace rather than a measurement, and it
     * is the one number here that is not derived from the text — worth timing a
     * real reading of ጸሎተ ነግህ before leaning on it any harder than a label.
     */
    const val CHARS_PER_MINUTE = 510

    /** Below this, the label would claim more precision than the estimate has. */
    private const val FLOOR_MINUTES = 1

    private val cache = ConcurrentHashMap<String, Int>()

    /** Drop the estimates when the content caches go (see [CacheTrimmer]). */
    fun trimCaches() {
        cache.clear()
    }

    /**
     * Minutes to read [hour] at [level], rounded to the nearest minute and never
     * less than one. Null when the hour has no readable text at all, so a caller
     * shows nothing rather than "0 min".
     */
    suspend fun minutes(context: Context, hour: Hour, level: PrayerLevel): Int? {
        val key = "${hour.id}:${level.name}"
        cache[key]?.let { return it }
        return withContext(Dispatchers.Default) {
            val shown = runCatching {
                PrayerLevelRepository.apply(context, hour.id, hour.sections, level)
            }.getOrDefault(hour.sections)
            val chars = shown.sumOf { section -> section.verses.sumOf { it.length } }
            if (chars == 0) return@withContext null
            val minutes = Math.round(chars.toDouble() / CHARS_PER_MINUTE).toInt()
                .coerceAtLeast(FLOOR_MINUTES)
            cache[key] = minutes
            minutes
        }
    }

    /** The whole visible cycle at [level] — what a day of prayer costs in full. */
    suspend fun dayMinutes(context: Context, hours: List<Hour>, level: PrayerLevel): Int =
        hours.sumOf { minutes(context, it, level) ?: 0 }
}
