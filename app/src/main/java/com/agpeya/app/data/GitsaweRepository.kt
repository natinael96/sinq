package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.Feast
import com.agpeya.app.model.BahreHasabReference
import com.agpeya.app.model.GitsaweEntry
import com.agpeya.app.model.GitsaweMonth
import com.agpeya.app.model.GitsawePackage
import com.agpeya.app.model.MonthlyEntry
import com.agpeya.app.model.SeasonalEntry
import com.agpeya.app.model.SundayCycleEntry
import com.agpeya.app.ui.common.EthiopianDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Loads the bundled ግጻዌ (Gitsawe) lectionary from assets/content/gitsawe/ and
 * caches each collection in memory (loaded lazily, only what's asked for).
 * Mirrors [ContentRepository]: offline, no database, decode-once.
 *
 * Daily entries are keyed by the Ethiopian "DD-MM" date; [toGitsaweDateKey]
 * converts a Gregorian [LocalDate] to that key. The fixed calendar contains all
 * 366 possible Ethiopian month-days.
 */
object GitsaweRepository {

    private const val TAG = "GitsaweRepository"
    private const val DIR = "content/gitsawe"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile private var dailyCache: List<GitsaweEntry>? = null
    @Volatile private var seasonalCache: List<SeasonalEntry>? = null
    @Volatile private var movableWeekdayCache: List<SeasonalEntry>? = null
    @Volatile private var monthlyCache: List<MonthlyEntry>? = null
    @Volatile private var sundayCycleCache: List<SundayCycleEntry>? = null
    @Volatile private var bahreHasabCache: BahreHasabReference? = null
    @Volatile private var feastsCache: List<Feast>? = null
    @Volatile private var monthsCache: List<GitsaweMonth>? = null
    @Volatile private var packagesCache: List<GitsawePackage>? = null

    // ---- Collections ---------------------------------------------------------

    suspend fun daily(context: Context): List<GitsaweEntry> =
        dailyCache ?: withContext(Dispatchers.IO) {
            load(context, "daily-gitsawe.json", ListSerializer(GitsaweEntry.serializer()))
                ?.also { dailyCache = it } ?: emptyList()
        }

    suspend fun seasonal(context: Context): List<SeasonalEntry> =
        seasonalCache ?: withContext(Dispatchers.IO) {
            load(context, "seasonal-gitsawe.json", ListSerializer(SeasonalEntry.serializer()))
                ?.also { seasonalCache = it } ?: emptyList()
        }

    private suspend fun movableWeekdays(context: Context): List<SeasonalEntry> =
        movableWeekdayCache ?: withContext(Dispatchers.IO) {
            load(context, "movable-weekday-gitsawe.json", ListSerializer(SeasonalEntry.serializer()))
                ?.also { movableWeekdayCache = it } ?: emptyList()
        }

    suspend fun monthly(context: Context): List<MonthlyEntry> =
        monthlyCache ?: withContext(Dispatchers.IO) {
            load(context, "monthly-gitsawe.json", ListSerializer(MonthlyEntry.serializer()))
                ?.also { monthlyCache = it } ?: emptyList()
        }

    /** Ordered, losslessly normalized master Part 3 rows. */
    suspend fun sundayCycle(context: Context): List<SundayCycleEntry> =
        sundayCycleCache ?: withContext(Dispatchers.IO) {
            load(context, "sunday-cycle-gitsawe.json", ListSerializer(SundayCycleEntry.serializer()))
                ?.also { sundayCycleCache = it } ?: emptyList()
        }

    /** The finite printed Part 5 table; live dates still use [BahreHasab]. */
    suspend fun bahreHasabReference(context: Context): BahreHasabReference? =
        bahreHasabCache ?: withContext(Dispatchers.IO) {
            loadOne(context, "bahre-hasab-reference.json", BahreHasabReference.serializer())
                ?.also { bahreHasabCache = it }
        }

    suspend fun feasts(context: Context): List<Feast> =
        feastsCache ?: withContext(Dispatchers.IO) {
            load(context, "feasts.json", ListSerializer(Feast.serializer()))?.also { feastsCache = it } ?: emptyList()
        }



    suspend fun months(context: Context): List<GitsaweMonth> =
        monthsCache ?: withContext(Dispatchers.IO) {
            load(context, "months.json", ListSerializer(GitsaweMonth.serializer()))
                ?.also { monthsCache = it } ?: emptyList()
        }

    suspend fun packages(context: Context): List<GitsawePackage> =
        packagesCache ?: withContext(Dispatchers.IO) {
            load(context, "packages.json", ListSerializer(GitsawePackage.serializer()))
                ?.also { packagesCache = it } ?: emptyList()
        }

    // ---- Lookups -------------------------------------------------------------

    /** The daily entry for a Gregorian date, or null if that day has no reading. */
    suspend fun dailyFor(context: Context, date: LocalDate): GitsaweEntry? =
        dailyForKey(context, toGitsaweDateKey(date))

    /** The daily entry for an Ethiopian "DD-MM" key. */
    suspend fun dailyForKey(context: Context, key: String): GitsaweEntry? =
        daily(context).find { it.date == key }

    /** Fixed-date feasts falling on a Gregorian date. */
    suspend fun feastsOn(context: Context, date: LocalDate): List<Feast> {
        val key = toGitsaweDateKey(date)
        return feasts(context).filter { it.dateKey == key }
    }

    /** Monthly entries whose day-span (or nth-Sunday) covers a Gregorian date. */
    suspend fun monthlyFor(context: Context, date: LocalDate): List<MonthlyEntry> {
        val e = EthiopianDate.from(date)
        val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY
        return monthly(context).filter { monthlyMatches(it, e.month, e.day, isSunday) }
    }

    /**
     * Every season window a date sits in: the computus one from [BahreHasab]
     * first, then the fixed-anchored Sunday seasons from [SundayCycleCalendar].
     */
    fun seasonWindowsOn(date: LocalDate): List<BahreHasab.SeasonWindow> =
        listOfNotNull(BahreHasab.movableSeasonOn(date)) + SundayCycleCalendar.windowsOn(date)

    private fun BahreHasab.SeasonWindow.admits(season: String?, week: Int?): Boolean =
        season == this.season && (this.week == null || week == null || week == this.week)

    /** Seasonal (movable) entries for a date, located via the [BahreHasab] computus. */
    suspend fun seasonalFor(context: Context, date: LocalDate): List<SeasonalEntry> {
        val sunday = BahreHasab.movableSeasonOn(date)
        // The fixed-anchored seasons carry Sunday hymns only, so they join on Sundays.
        val fixed = if (date.dayOfWeek == DayOfWeek.SUNDAY) SundayCycleCalendar.windowsOn(date) else emptyList()
        val windows = listOfNotNull(sunday) + fixed
        val weekday = BahreHasab.movableWeekdayOn(date)
        val sundayEntries = if (windows.isEmpty()) emptyList() else seasonal(context).filter { entry ->
            windows.any { it.admits(entry.season, entry.week) }
        }
        val weekdayEntries = if (weekday == null) emptyList() else movableWeekdays(context).filter {
            it.season == weekday.season && it.week == weekday.week && it.part == weekday.part
        }
        return sundayEntries + weekdayEntries
    }

    /**
     * Part 3 rows the book prints for this Sunday (or for ቅዳሜ ሥዑር, the one
     * Saturday it covers). A feast that falls on the day overrides the season:
     * a single-date rubric beats a printed date range, which beats a season row.
     */
    suspend fun sundayCycleFor(context: Context, date: LocalDate): List<SundayCycleEntry> =
        selectSundayCycle(sundayCycle(context), date)

    /** The pure selection behind [sundayCycleFor], so it can be checked without assets. */
    fun selectSundayCycle(entries: List<SundayCycleEntry>, date: LocalDate): List<SundayCycleEntry> {
        val movable = BahreHasab.movableSeasonOn(date)
        if (date.dayOfWeek != DayOfWeek.SUNDAY && movable?.season != "holy_saturday") return emptyList()
        val eth = EthiopianDate.from(date)
        val windows = listOfNotNull(movable) + SundayCycleCalendar.windowsOn(date)
        val seasonal = entries.filter { e -> windows.any { w -> e.season == w.season && e.week == w.week } }
        // Great Lent suppresses the feasts that fall inside it (ስምዖን on ዘወረደ).
        if (movable?.season == "abiyTsom" && seasonal.isNotEmpty()) return seasonal
        val exact = entries.filter { e -> e.dateSpans.any { it.isSingleDay && it.contains(eth.month, eth.day) } }
        if (exact.isNotEmpty()) return exact
        val ranged = entries.filter { e -> e.dateSpans.any { it.contains(eth.month, eth.day) } }
        if (ranged.isNotEmpty()) return ranged
        return seasonal
    }

    /**
     * Everything to read on a given date, so a screen can offer the daily reading
     * plus any seasonal, feast, or monthly reading that also lands there.
     */
    suspend fun readingsFor(context: Context, date: LocalDate): DayReadings = DayReadings(
        date = date,
        dateKey = toGitsaweDateKey(date),
        daily = dailyFor(context, date),
        seasonal = seasonalFor(context, date),
        monthly = monthlyFor(context, date),
        sundayCycle = sundayCycleFor(context, date),
        feasts = feastsOn(context, date),
        mahlets = MahletRepository.ordersOn(context, date),
    )

    private fun monthlyMatches(m: MonthlyEntry, ethMonth: Int, ethDay: Int, isSunday: Boolean): Boolean {
        val month = m.monthNum ?: return false   // tsige/seasonal months are movable — skip
        m.nthSunday?.let { nth ->
            // A date that is a Sunday is the ((day-1)/7 + 1)-th Sunday of its month.
            return isSunday && ethMonth == month && (ethDay - 1) / 7 + 1 == nth
        }
        val from = m.fromDay ?: return false
        val to = m.toDay ?: from
        return if (m.crossMonth) {
            (ethMonth == month && ethDay >= from) ||
                (ethMonth == month % 13 + 1 && ethDay <= to)   // spills into the next month
        } else {
            ethMonth == month && ethDay in from..to
        }
    }

    // ---- Ethiopian date conversion ------------------------------------------

    /**
     * Gregorian [date] -> Ethiopian "DD-MM" key used by the daily data. Reuses
     * the app's own [EthiopianDate] converter (unit-tested, shared with the Home
     * screen) so the whole app agrees on the church date.
     */
    fun toGitsaweDateKey(date: LocalDate): String {
        val e = EthiopianDate.from(date)
        return "%02d-%02d".format(e.day, e.month)
    }

    // ---- Internal ------------------------------------------------------------

    // Null on failure so call sites cache only successful loads — a transient
    // read error must not stick as an empty collection for the process lifetime.
    private fun <T> load(context: Context, file: String, serializer: KSerializer<List<T>>): List<T>? =
        runCatching {
            val raw = context.applicationContext.assets
                .open("$DIR/$file").readBytes().decodeToString()
            json.decodeFromString(serializer, raw)
        }.onFailure { Log.e(TAG, "Failed to load $file", it) }.getOrNull()

    private fun <T> loadOne(context: Context, file: String, serializer: KSerializer<T>): T? =
        runCatching {
            val raw = context.applicationContext.assets.open("$DIR/$file").readBytes().decodeToString()
            json.decodeFromString(serializer, raw)
        }.onFailure { Log.e(TAG, "Failed to load $file", it) }.getOrNull()
}

/**
 * The reading options that fall on one date: the [daily] entry plus any [feasts]
 * or [monthly] readings for the same day. A screen can let the user switch among
 * whichever of these are present. ([hasChoice] is true when more than one exists.)
 */
data class DayReadings(
    val date: LocalDate,
    val dateKey: String,
    val daily: GitsaweEntry?,
    val seasonal: List<SeasonalEntry>,
    val monthly: List<MonthlyEntry>,
    val sundayCycle: List<SundayCycleEntry>,
    val feasts: List<Feast>,
    /**
     * The ዋዜማ and ነግሥ sung on this day, when the book appoints one — from the
     * merged ማኅሌት corpus, which covers the whole year where the ግጻዌ's own
     * stopped after ሚያዝያ.
     */
    val mahlets: List<com.agpeya.app.model.MahletOrder> = emptyList(),
) {
    val hasChoice: Boolean
        get() = listOfNotNull(daily).size + seasonal.size + monthly.size + sundayCycle.size + feasts.size > 1
}
