package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.Feast
import com.agpeya.app.model.BahreHasabReference
import com.agpeya.app.model.GitsaweEntry
import com.agpeya.app.model.GitsaweMonth
import com.agpeya.app.model.GitsawePackage
import com.agpeya.app.model.Mahlet
import com.agpeya.app.model.MonthlyEntry
import com.agpeya.app.model.SeasonalEntry
import com.agpeya.app.model.SubFeast
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
    @Volatile private var subFeastsCache: List<SubFeast>? = null
    @Volatile private var mahletsCache: List<Mahlet>? = null
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

    suspend fun subFeasts(context: Context): List<SubFeast> =
        subFeastsCache ?: withContext(Dispatchers.IO) {
            load(context, "sub-feasts.json", ListSerializer(SubFeast.serializer()))
                ?.also { subFeastsCache = it } ?: emptyList()
        }

    suspend fun mahlets(context: Context): List<Mahlet> =
        mahletsCache ?: withContext(Dispatchers.IO) {
            load(context, "mahlets.json", ListSerializer(Mahlet.serializer()))?.also { mahletsCache = it } ?: emptyList()
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

    /**
     * The orders of service sung on a date: ዋዜማ the evening before, ነግሥ at
     * dawn, or the week's ማኅሌት in ዘመነ ጽጌ.
     *
     * The book files a ማኅሌት under a sub-feast and a sub-feast under a feast, so
     * dating one is a join and not a new calendar. Nineteen of the twenty-one
     * feasts are fixed and carry a `dateKey` of the same shape the daily
     * lectionary uses; the ጽጌ weeks are the Sunday ordinal
     * [SundayCycleCalendar] already computes, and ትንሣኤ is [BahreHasab.fasika].
     *
     * Two feasts share ኅዳር ፮ — ቁስቋም ማርያም and ቅዱስ ጊዮርጊስ — and both are kept:
     * the day carries both, and so should the page.
     */
    suspend fun mahletsOn(context: Context, date: LocalDate): List<DayMahlet> {
        val all = mahlets(context)
        if (all.isEmpty()) return emptyList()
        val subs = subFeasts(context).associateBy { it.key }
        val feasts = feasts(context).associateBy { it.key }
        val todaysKeys = mahletFeastKeysOn(context, date)
        if (todaysKeys.isEmpty()) return emptyList()
        return all.mapNotNull { mahlet ->
            val sub = subs[mahlet.subFeast ?: return@mapNotNull null] ?: return@mapNotNull null
            if (sub.feast !in todaysKeys) return@mapNotNull null
            // A ጽጌ week only matches its own ordinal, not every week of the season.
            val week = todaysKeys[sub.feast]
            if (week != null && !sub.key.startsWith("1_") ) return@mapNotNull null
            if (week != null && seasonWeekOf(sub.key) != week) return@mapNotNull null
            DayMahlet(mahlet = mahlet, subFeast = sub, feast = feasts[sub.feast])
        }
    }

    /**
     * Which of the twenty-one ማኅሌት feasts fall on [date], and for a movable one
     * the week of its season. A null week means "the whole feast".
     */
    private suspend fun mahletFeastKeysOn(context: Context, date: LocalDate): Map<String, Int?> {
        val out = mutableMapOf<String, Int?>()
        val key = toGitsaweDateKey(date)
        feasts(context).forEach { feast ->
            when {
                !feast.movable && feast.dateKey == key -> out[feast.key] = null
                feast.key == TSIGE_FEAST ->
                    SundayCycleCalendar.windowsOn(date)
                        .firstOrNull { it.season == "tsige" }?.week
                        ?.let { out[feast.key] = it }
                feast.key == EASTER_FEAST -> {
                    val ethYear = EthiopianDate.from(date).year
                    if (runCatching { BahreHasab.fasika(ethYear) }.getOrNull() == date) {
                        out[feast.key] = null
                    }
                }
            }
        }
        return out
    }

    /** A feast and the orders of service the book gives it, ዋዜማ before ነግሥ. */
    data class FeastMahlets(
        val feastName: String?,
        val orders: List<DayMahlet>,
        val feast: Feast? = null,
        /** False when the transcription has not reached this feast yet. */
        val complete: Boolean = true,
    )

    /**
     * Every order of service belonging to the same feast as [subFeastKey].
     *
     * The reader opens on one and offers the other: ዋዜማ and ነግሥ are one night
     * and the morning after it, not two separate pages to find separately.
     */
    suspend fun mahletsOfFeastFor(context: Context, subFeastKey: String): List<FeastMahlets> {
        val subs = subFeasts(context).associateBy { it.key }
        val target = subs[subFeastKey] ?: return emptyList()
        val feast = feasts(context).firstOrNull { it.key == target.feast }
        val orders = mahlets(context).mapNotNull { mahlet ->
            val sub = subs[mahlet.subFeast ?: return@mapNotNull null] ?: return@mapNotNull null
            if (sub.feast != target.feast) return@mapNotNull null
            // A season's weeks share one feast, so only the week asked for.
            if (seasonWeekOf(sub.key) != null && sub.key != subFeastKey) return@mapNotNull null
            DayMahlet(mahlet = mahlet, subFeast = sub, feast = feast)
        }.sortedBy { if (it.isEve) 0 else 1 }
        return listOf(FeastMahlets(feastName = feast?.amharicName, orders = orders, feast = feast))
    }

    /**
     * The whole book, in the order the year sings it: every sub-feast that has
     * an order of service, and the three that do not, which the list marks
     * rather than hides — the book has them, the transcription has not reached
     * them yet.
     */
    suspend fun allMahlets(context: Context): List<DayMahlet> {
        val byKey = mahlets(context).associateBy { it.subFeast }
        val feasts = feasts(context).associateBy { it.key }
        return subFeasts(context)
            .sortedWith(
                compareBy(
                    { feasts[it.feast]?.monthNum ?: 99 },
                    { feasts[it.feast]?.day ?: 99 },
                    { if (it.key.endsWith("_eve")) 0 else 1 },
                ),
            )
            .map { sub ->
                DayMahlet(
                    mahlet = byKey[sub.key] ?: Mahlet(title = sub.amharicName, subFeast = sub.key),
                    subFeast = sub,
                    feast = feasts[sub.feast],
                )
            }
    }

/**
     * The book grouped as the book is: one feast, with the services it appoints.
     *
     * A feast gets a ዋዜማ the evening before and the ማኅሌት at dawn, and listing
     * those as two sibling rows breaks a feast in half and makes the index
     * twice as long as the book. 20 feasts carry a ማኅሌት; 14 have both services.
     */
    suspend fun mahletsByFeast(context: Context): List<FeastMahlets> {
        val all = allMahlets(context)
        // A fixed feast has one row carrying its ዋዜማ and its ማኅሌት. ዘመነ ጽጌ is
        // not that shape: its "feast" holds six numbered weeks, each with its
        // own order of service, so grouping them together would put five of
        // them behind one row and leave four unreachable.
        return all.groupBy { it.subFeast.feast + (seasonWeekOf(it.subFeast.key)?.let { w -> "#$w" } ?: "") }
            .map { (_, orders) ->
                val kept = orders.filter { it.mahlet.detail.isNotEmpty() }
                val week = seasonWeekOf(orders.first().subFeast.key)
                FeastMahlets(
                    // The source writes the date into the name as well, so it
                    // would appear twice beside a date column. A week of a
                    // season is named by the week, not by the season.
                    feastName = (
                        if (week != null) orders.first().subFeast.amharicName
                        else orders.first().feast?.amharicName ?: orders.first().subFeast.amharicName
                        ).substringBefore(" (").trim(),
                    orders = kept.ifEmpty { orders },
                    feast = orders.first().feast,
                    complete = kept.isNotEmpty(),
                )
            }
            .sortedWith(compareBy({ it.feast?.monthNum ?: 99 }, { it.feast?.day ?: 99 }))
    }

    /** "1_3rd_week" → 3. The book numbers the ጽጌ weeks in the sub-feast key. */
    internal fun seasonWeekOf(subFeastKey: String): Int? =
        Regex("""^1_(\d+)""").find(subFeastKey)?.groupValues?.get(1)?.toIntOrNull()

    private const val TSIGE_FEAST = "1st_tsige"
    private const val EASTER_FEAST = "easter"

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
        mahlets = mahletsOn(context, date),
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
/**
 * One order of service on a day, with the feast it belongs to.
 *
 * Kept together because a screen needs all three: the ማኅሌት for its parts, the
 * sub-feast to say whether it is the ዋዜማ or the ነግሥ, and the feast for its name.
 */
data class DayMahlet(
    val mahlet: Mahlet,
    val subFeast: SubFeast,
    val feast: Feast?,
) {
    /** ዋዜማ is sung the evening before; everything else belongs to the day. */
    val isEve: Boolean get() = subFeast.key.endsWith("_eve")
}

data class DayReadings(
    val date: LocalDate,
    val dateKey: String,
    val daily: GitsaweEntry?,
    val seasonal: List<SeasonalEntry>,
    val monthly: List<MonthlyEntry>,
    val sundayCycle: List<SundayCycleEntry>,
    val feasts: List<Feast>,
    /** The ዋዜማ and ነግሥ sung on this day, when the book appoints one. */
    val mahlets: List<DayMahlet> = emptyList(),
) {
    val hasChoice: Boolean
        get() = listOfNotNull(daily).size + seasonal.size + monthly.size + sundayCycle.size + feasts.size > 1
}
