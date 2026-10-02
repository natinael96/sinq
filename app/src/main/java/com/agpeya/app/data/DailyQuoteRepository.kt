package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.DailyQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * Loads and caches the bundled 366-day Desert Fathers sayings (Apophthegmata Patrum / የአበው ምክር).
 */
object DailyQuoteRepository {

    private const val TAG = "DailyQuoteRepository"
    private const val PATH = "content/daily_quotes.json"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cache: List<DailyQuote>? = null

    private val FALLBACK_QUOTE = DailyQuote(
        day = 1,
        dayGeez = "፩",
        authorEn = "Abba Antony the Great",
        authorAm = "አባ እንጦንስ ዓቢይ",
        quote = "Lord, I want to be saved but these thoughts do not leave me alone; what shall I do in my affliction? How can I be saved? ... Do this and you will be saved.",
        quoteId = "1",
    )

    suspend fun quotes(context: Context): List<DailyQuote> =
        cache ?: withContext(Dispatchers.IO) {
            loadFromAssets(context)
        }

    fun quotesBlocking(context: Context): List<DailyQuote> {
        cache?.let { return it }
        return synchronized(this) {
            cache ?: loadFromAssets(context)
        }
    }

    private fun loadFromAssets(context: Context): List<DailyQuote> {
        return runCatching {
            val raw = context.applicationContext.assets.open(PATH).bufferedReader().use { it.readText() }
            json.decodeFromString<List<DailyQuote>>(raw)
        }.onFailure { Log.e(TAG, "Failed to load daily quotes from $PATH", it) }
            .getOrNull()
            ?.also { cache = it }
            ?: listOf(FALLBACK_QUOTE)
    }

    /**
     * Continuous multi-year rotation index:
     * Advances by exactly 1 quote per day across years, rotating through the entire
     * corpus of Desert Fathers sayings without repeating for ~2.3 years.
     * Consecutive years never show the same quotes on the same day.
     */
    fun quoteIndexFor(date: LocalDate, total: Int): Int {
        if (total <= 0) return 0
        val baseEpoch = LocalDate.of(2026, 1, 1).toEpochDay()
        val dayOffset = date.toEpochDay() - baseEpoch
        return Math.floorMod(dayOffset, total.toLong()).toInt()
    }

    /**
     * Retrieves today's Desert Father saying for the given date using the multi-year rotation cycle.
     */
    suspend fun quoteFor(context: Context, date: LocalDate): DailyQuote {
        val list = quotes(context)
        if (list.isEmpty()) return FALLBACK_QUOTE
        val index = quoteIndexFor(date, list.size)
        return list[index]
    }

    /**
     * Synchronous retrieval for BroadcastReceivers, Alarms, and RemoteViews Widgets.
     */
    fun quoteForBlocking(context: Context, date: LocalDate): DailyQuote {
        val list = quotesBlocking(context)
        if (list.isEmpty()) return FALLBACK_QUOTE
        val index = quoteIndexFor(date, list.size)
        return list[index]
    }
}
