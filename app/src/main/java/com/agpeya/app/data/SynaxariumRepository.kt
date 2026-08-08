package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.SynaxariumDay
import com.agpeya.app.model.SynaxariumEntry
import com.agpeya.app.model.SynaxariumMonth
import com.agpeya.app.ui.common.EthiopianDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads the bundled ስንክሳር (synaxarium) from assets/content/sinksar/, one month
 * at a time (only the month being viewed is held in memory). Keyed by the
 * Ethiopian date via the app's shared [EthiopianDate] converter.
 */
object SynaxariumRepository {

    private const val TAG = "SynaxariumRepository"
    private const val DIR = "content/sinksar"

    private val json = Json { ignoreUnknownKeys = true }
    private val monthCache = ConcurrentHashMap<Int, List<SynaxariumDay>>()

    /** All days of an Ethiopian month (1–13), cached after first successful load
     *  (a failed read is NOT cached, so a transient error doesn't stick). */
    suspend fun month(context: Context, month: Int): List<SynaxariumDay> =
        monthCache[month] ?: withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.applicationContext.assets
                    .open("$DIR/$month.json").readBytes().decodeToString()
                json.decodeFromString<SynaxariumMonth>(raw).days
            }.onFailure { Log.e(TAG, "Failed to load sinksar month $month", it) }
                .getOrNull()?.also { monthCache[month] = it } ?: emptyList()
        }

    /** The commemorations for a Gregorian [date] (empty if none). */
    suspend fun forDate(context: Context, date: LocalDate): List<SynaxariumEntry> {
        val e = EthiopianDate.from(date)
        return month(context, e.month).find { it.day == e.day }?.entries ?: emptyList()
    }
}
