package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.MahletIndex
import com.agpeya.app.model.MahletMonth
import com.agpeya.app.model.MahletOrder
import com.agpeya.app.model.MahletOrderMeta
import com.agpeya.app.model.MahletSeason
import com.agpeya.app.ui.common.EthiopianDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * The merged ሥርዓተ ማኅሌት: the book's orders and the Telegram editions of them,
 * over the whole year.
 *
 * The index is small and cached; a month's orders are read only when that month
 * is opened. What this repository adds over a plain loader is [ordersOn] — the
 * ዘመነ ጽጌ orders are appointed by a date falling on a Sunday, so which one
 * applies has to be computed against the actual year rather than looked up.
 */
object MahletRepository {

    private const val TAG = "MahletRepository"
    private const val DIR = "content/mahlet"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var indexCache: MahletIndex? = null
    private val monthCache = ConcurrentHashMap<Int, List<MahletOrder>>()

    /** Drop the rebuildable caches under memory pressure (see [CacheTrimmer]). */
    fun trimCaches() {
        monthCache.clear()
    }

    suspend fun index(context: Context): MahletIndex =
        indexCache ?: withContext(Dispatchers.IO) {
            read(context, "index.json")
                ?.let { runCatching { json.decodeFromString<MahletIndex>(it) }
                    .onFailure { e -> Log.e(TAG, "Failed to parse the ማኅሌት index", e) }
                    .getOrNull() }
                ?.also { indexCache = it } ?: MahletIndex()
        }

    /** Every order of one Ethiopian month; month 0 holds the undated ones. */
    suspend fun month(context: Context, month: Int): List<MahletOrder> {
        monthCache[month]?.let { return it }
        return withContext(Dispatchers.IO) {
            CacheTrimmer.ensureRegistered(context)
            read(context, "m$month.json")
                ?.let { runCatching { json.decodeFromString<MonthFile>(it).orders }
                    .onFailure { e -> Log.e(TAG, "Failed to parse ማኅሌት month $month", e) }
                    .getOrNull() }
                ?.also { monthCache[month] = it } ?: emptyList()
        }
    }

    suspend fun order(context: Context, id: String): MahletOrder? {
        if (id.isBlank()) return null
        for (m in index(context).months) {
            if (m.orders.none { it.id == id }) continue
            return month(context, m.month).find { it.id == id }
        }
        return null
    }

    /**
     * The orders appointed for a date.
     *
     * Two rules, because the book has two. A dated feast is kept on its day. A
     * ዘመነ ጽጌ order is kept when *its* date falls on a Sunday — the season's
     * services move with the week, which is why the ግጻዌ could only ever call
     * them "the fourth week" and why this needs the calendar to answer.
     */
    suspend fun ordersOn(context: Context, date: LocalDate): List<MahletOrder> {
        val eth = EthiopianDate.from(date)
        val orders = month(context, eth.month)
        val onDay = orders.filter { it.season == null && it.day == eth.day }
        val tsige = if (date.dayOfWeek != DayOfWeek.SUNDAY) emptyList()
        else orders.filter { it.season == MahletSeason.TSIGE && it.whenSunday && it.day == eth.day }
        return tsige + onDay
    }

    /** Which months actually hold orders, in the year's order, undated last. */
    suspend fun months(context: Context): List<MahletMonth> =
        index(context).months.sortedBy { if (it.month == 0) 99 else it.month }

    suspend fun meta(context: Context, id: String): MahletOrderMeta? =
        index(context).months.firstNotNullOfOrNull { m -> m.orders.find { it.id == id } }

    @kotlinx.serialization.Serializable
    private data class MonthFile(val month: Int = 0, val orders: List<MahletOrder> = emptyList())

    private fun read(context: Context, name: String): String? = runCatching {
        context.applicationContext.assets.open("$DIR/$name").readBytes().decodeToString()
    }.onFailure { Log.e(TAG, "Failed to read $DIR/$name", it) }.getOrNull()
}
