package com.agpeya.app.data

import android.content.Context
import android.util.Log
import com.agpeya.app.model.AnnualHoliday
import com.agpeya.app.model.MonthlyHoliday
import com.agpeya.app.ui.common.EthiopianDate
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * The feasts a ስዕለት (vow) can be anchored to.
 *
 * Three kinds, because that is how the church year is actually kept:
 *
 * - **ወርኀዊ** — the same day of every Ethiopian month (ሚካኤል on ፲፪, ገብርኤል on
 *   ፲፱, ማርያም on ፳፩). Names come from the bundled ስንክሳር via
 *   tools/extract_holidays.py, not from anything authored here.
 * - **ዓመታዊ, fixed** — an Ethiopian month and day (መስቀል on መስከረም ፲፯).
 * - **ዓመታዊ, movable** — computed per year from the ባሕረ ሓሳብ ([BahreHasab]),
 *   so ፋሲካ and everything hanging off it land correctly every year.
 *
 * A person is never confined to this list: the vow editor also takes a plain
 * Ethiopian day or month+day, so a promise kept on a date the calendar doesn't
 * name still schedules.
 */
object HolidayCalendar {

    private const val TAG = "HolidayCalendar"
    private const val MONTHLY_ASSET = "content/holidays/monthly.json"

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var monthlyCache: List<MonthlyHoliday>? = null

    /** Drop the rebuildable monthly table under memory pressure. */
    fun trimCaches() {
        monthlyCache = null
    }

    /**
     * The ወርኀዊ በዓላት table, day ፩ to ፴. Empty only if the asset is unreadable,
     * in which case the vow editor still offers plain days.
     */
    fun monthly(context: Context): List<MonthlyHoliday> = monthlyCache ?: runCatching {
        val raw = context.applicationContext.assets
            .open(MONTHLY_ASSET).readBytes().decodeToString()
        json.decodeFromString<List<MonthlyHoliday>>(raw)
    }.onFailure { Log.e(TAG, "Failed to load monthly holidays", it) }
        .getOrNull()?.also { monthlyCache = it } ?: emptyList()

    /** The commemoration kept on [day] of every month, or null outside ፩–፴. */
    fun monthlyOn(context: Context, day: Int): MonthlyHoliday? =
        monthly(context).firstOrNull { it.day == day }

    /**
     * The yearly feasts offered by name. Deliberately the major ones people
     * actually vow on rather than the whole ስንክሳር — a picker with 1822 entries
     * is a worse tool than one with thirty and a free date field beside it.
     *
     * Fixed dates agree with the bundled ግጻዌ feasts.json where the two overlap.
     */
    val annual: List<AnnualHoliday> = listOf(
        AnnualHoliday("newYear", "ርእሰ ዐውደ ዓመት (እንቁጣጣሽ)", "Ethiopian New Year", monthNum = 1, day = 1),
        AnnualHoliday("meskel", "መስቀል", "Finding of the True Cross", monthNum = 1, day = 17),
        AnnualHoliday("medhaneAlem", "መድኃኔዓለም", "Saviour of the World", monthNum = 2, day = 27),
        AnnualHoliday("michaelHidar", "ቅዱስ ሚካኤል (ኅዳር)", "St Michael (Hidar)", monthNum = 3, day = 12),
        AnnualHoliday("tsion", "ጽዮን ማርያም", "Our Lady of Zion", monthNum = 3, day = 21),
        AnnualHoliday("gabriel", "ቅዱስ ገብርኤል (ታኅሣሥ)", "St Gabriel (Tahsas)", monthNum = 4, day = 19),
        AnnualHoliday("tekleHaymanot", "አቡነ ተክለ ሃይማኖት", "Abune Tekle Haymanot", monthNum = 4, day = 24),
        AnnualHoliday("lidet", "ልደተ ክርስቶስ (ገና)", "Nativity", monthNum = 4, day = 29),
        AnnualHoliday("timket", "ጥምቀት", "Theophany", monthNum = 5, day = 11),
        AnnualHoliday("asteriyo", "አስተርእዮ ማርያም", "Manifestation of St Mary", monthNum = 5, day = 21),
        AnnualHoliday("kidaneMihret", "ኪዳነ ምሕረት", "Covenant of Mercy", monthNum = 6, day = 16),
        AnnualHoliday("giorgis", "ቅዱስ ጊዮርጊስ (ሚያዝያ)", "St George (Miyazya)", monthNum = 8, day = 23),
        AnnualHoliday("lideta", "ልደታ ለማርያም", "Nativity of St Mary", monthNum = 9, day = 1),
        AnnualHoliday("debreTabor", "ደብረ ታቦር (ቡሄ)", "Transfiguration", monthNum = 12, day = 13),
        AnnualHoliday("filseta", "ፍልሰታ ለማርያም", "Assumption of St Mary", monthNum = 12, day = 16),
        // Movable — dates come from the ባሕረ ሓሳብ, never from a stored day.
        AnnualHoliday("nineveh", "ጾመ ነነዌ", "Fast of Nineveh", movableKey = "nineveh"),
        AnnualHoliday("greatLent", "ዐቢይ ጾም (መጀመሪያ)", "Start of Great Lent", movableKey = "greatLent"),
        AnnualHoliday("debreZeit", "ደብረ ዘይት", "Debre Zeit", movableKey = "debreZeit"),
        AnnualHoliday("hosanna", "ሆሣዕና", "Palm Sunday", movableKey = "hosanna"),
        AnnualHoliday("siklet", "ስቅለት", "Good Friday", movableKey = "siklet"),
        AnnualHoliday("fasika", "ትንሣኤ (ፋሲካ)", "Pascha", movableKey = "fasika"),
        AnnualHoliday("rikbeKahnat", "ርክበ ካህናት", "Rikbe Kahnat", movableKey = "rikbeKahnat"),
        AnnualHoliday("erget", "ዕርገት", "Ascension", movableKey = "erget"),
        AnnualHoliday("peraklitos", "ጰራቅሊጦስ", "Pentecost", movableKey = "peraklitos"),
    )

    fun annualByKey(key: String): AnnualHoliday? = annual.firstOrNull { it.key == key }

    /**
     * When [key]'s feast falls in Ethiopian year [ethYear], or null if the key
     * is unknown. A fixed feast is a plain conversion; a movable one asks the
     * computus. Never throws — a bad year only yields null.
     */
    fun dateOf(key: String, ethYear: Int): LocalDate? {
        val holiday = annualByKey(key) ?: return null
        holiday.movableKey?.let { return movableDate(it, ethYear) }
        val month = holiday.monthNum ?: return null
        val day = holiday.day ?: return null
        return runCatching { EthiopianDate(ethYear, month, day).toGregorian() }.getOrNull()
    }

    private fun movableDate(movableKey: String, ethYear: Int): LocalDate? = runCatching {
        when (movableKey) {
            "nineveh" -> BahreHasab.nineveh(ethYear)
            "greatLent" -> BahreHasab.greatLentStart(ethYear)
            "debreZeit" -> BahreHasab.debreZeit(ethYear)
            "hosanna" -> BahreHasab.hosanna(ethYear)
            "siklet" -> BahreHasab.siklet(ethYear)
            "fasika" -> BahreHasab.fasika(ethYear)
            "rikbeKahnat" -> BahreHasab.rikbeKahnat(ethYear)
            "erget" -> BahreHasab.ascension(ethYear)
            "peraklitos" -> BahreHasab.pentecost(ethYear)
            else -> null
        }
    }.getOrNull()

    /**
     * True when [date] is [key]'s feast. Movable feasts are checked against
     * both the date's own Ethiopian year and the next one: ጾመ ነነዌ and ፋሲካ sit
     * well inside the year, but the check costs nothing and keeps a feast that
     * drifts near a year boundary from being missed.
     */
    fun isFeastOn(key: String, date: LocalDate): Boolean {
        val ethYear = runCatching { EthiopianDate.from(date).year }.getOrNull() ?: return false
        return dateOf(key, ethYear) == date || dateOf(key, ethYear - 1) == date
    }
}
