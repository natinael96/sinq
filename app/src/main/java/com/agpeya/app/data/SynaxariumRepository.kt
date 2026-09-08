package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.SynaxariumDay
import com.agpeya.app.model.SynaxariumEdition
import com.agpeya.app.model.SynaxariumMonth
import com.agpeya.app.ui.common.EthiopianDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads the bundled ስንክሳር from assets/content/sinksar/, one month of one
 * edition at a time. Keyed by the Ethiopian date via the app's shared
 * [EthiopianDate] converter.
 */
object SynaxariumRepository {

    private const val TAG = "SynaxariumRepository"
    private const val DIR = "content/sinksar"

    private val json = Json { ignoreUnknownKeys = true }
    private val monthCache = ConcurrentHashMap<String, List<SynaxariumDay>>()

    /** Drop the rebuildable month cache under memory pressure (see [CacheTrimmer]). */
    fun trimCaches() {
        monthCache.clear()
    }

    /** All days of an Ethiopian month (1–13), cached after first successful load
     *  (a failed read is NOT cached, so a transient error doesn't stick). */
    suspend fun month(
        context: Context,
        month: Int,
        edition: String = SynaxariumEdition.AMHARIC,
    ): List<SynaxariumDay> {
        val key = "$edition-$month"
        monthCache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            CacheTrimmer.ensureRegistered(context)
            runCatching {
                val raw = context.applicationContext.assets
                    .open("$DIR/$key.json").readBytes().decodeToString()
                json.decodeFromString<SynaxariumMonth>(raw).days
            }.onFailure { Log.e(TAG, "Failed to load sinksar $key", it) }
                .getOrNull()?.also { monthCache[key] = it } ?: emptyList()
        }
    }

    /** The whole day for a Gregorian [date] — commemorations, feasts and reading. */
    suspend fun forDate(
        context: Context,
        date: LocalDate,
        edition: String = SynaxariumEdition.AMHARIC,
    ): SynaxariumDay? {
        val e = EthiopianDate.from(date)
        return month(context, e.month, edition).find { it.day == e.day }
    }
}
