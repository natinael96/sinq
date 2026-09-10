package com.agpeya.app.data

import com.agpeya.app.ui.common.EthiopianDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Which of the ማኅሌት's undatable feasts a day is.
 *
 * The book prints ሆሣዕና, ትንሣኤ, ዕርገት and ጰራቅሊጦስ under the month it happens to
 * bind them in, with no day, because they move with Fasika; and ስብከት, ብርሃን,
 * ኖላዊ and the ዘመነ ጽጌ weeks under a range, because each is a Sunday the
 * calendar has to find. Both kinds are questions the app already answers for
 * the ግጻዌ — [BahreHasab] for the Paschal cycle, [SundayCycleCalendar] for the
 * fixed-anchored Sundays — so the ማኅሌት asks the same two and gets the same
 * days, rather than carrying a third calendar of its own.
 *
 * The keys are the ones tools/build_mahlet.py writes into
 * [com.agpeya.app.model.MahletOrder.movable]; validate_content.py holds the
 * same list, so a key that appears in one and not the other fails the build.
 */
object MahletComputus {

    /** Everything [on] can return. */
    val KEYS: Set<String> = setOf(
        "sibket", "birhan", "nolawi",
        "hosanna", "himamat", "siklet", "kedamSiur", "fasika", "dagmTinsae", "erget", "peraklitos",
    ) + (1..6).map { "tsige$it" }

    /**
     * The keys appointed on [date] — usually none, at most a couple.
     *
     * The Paschal ones are offsets from ጾመ ነነዌ, the same way [BahreHasab]
     * counts them: ሆሣዕና is the sixty-second day, ስቅለት the sixty-seventh, ትንሣኤ
     * the sixty-ninth. ሰሙነ ሕማማት is the four days between ሆሣዕና and ስቅለት that
     * have no order of their own. The Sunday ones come from the windows the
     * ግጻዌ's Sunday cycle already computes, kept only when the day is in fact
     * a Sunday — the windows are defined for every day so the season can be
     * named, but the ማኅሌት is sung on the Sunday.
     */
    fun on(date: LocalDate): Set<String> {
        val keys = mutableSetOf<String>()
        val year = EthiopianDate.from(date).year
        val off = ChronoUnit.DAYS.between(BahreHasab.nineveh(year), date).toInt()
        when (off) {
            62 -> keys += "hosanna"
            in 63..66 -> keys += "himamat"
            67 -> keys += "siklet"
            68 -> keys += "kedamSiur"
            69 -> keys += "fasika"
            76 -> keys += "dagmTinsae"
            108 -> keys += "erget"
            118 -> keys += "peraklitos"
        }
        if (date.dayOfWeek == DayOfWeek.SUNDAY) {
            for (w in SundayCycleCalendar.windowsOn(date)) {
                when (w.season) {
                    "sibket", "birhan", "nolawi" -> keys += w.season
                    "tsige" -> w.week?.let { keys += "tsige$it" }
                }
            }
        }
        return keys
    }

    /**
     * The day [key] falls on in [ethYear], or null when it does not — ጽጌ has
     * five Sundays some years and six others, and week six is simply absent
     * in a five-Sunday year.
     *
     * For a Sunday key the candidate is computed and then checked against
     * [on], so the two can never disagree about which day a feast is.
     */
    fun dateOf(key: String, ethYear: Int): LocalDate? {
        val candidate = when (key) {
            "hosanna" -> BahreHasab.hosanna(ethYear)
            "himamat" -> BahreHasab.hosanna(ethYear).plusDays(1)
            "siklet" -> BahreHasab.siklet(ethYear)
            "kedamSiur" -> BahreHasab.fasika(ethYear).minusDays(1)
            "fasika" -> BahreHasab.fasika(ethYear)
            "dagmTinsae" -> BahreHasab.fasika(ethYear).plusDays(7)
            "erget" -> BahreHasab.ascension(ethYear)
            "peraklitos" -> BahreHasab.pentecost(ethYear)
            // ታኅሣሥ ፯–፲፫, ፲፬–፳, ፳፩–፳፯: the one Sunday in each.
            "sibket" -> sundayIn(ethYear, 4, 7..13)
            "birhan" -> sundayIn(ethYear, 4, 14..20)
            "nolawi" -> sundayIn(ethYear, 4, 21..27)
            else -> {
                val week = key.removePrefix("tsige").toIntOrNull() ?: return null
                // The Nth Sunday on or after መስከረም ፳፮.
                sundayIn(ethYear, 1, 26..30)?.plusDays(7L * (week - 1))
            }
        } ?: return null
        return candidate.takeIf { key in on(it) }
    }

    /** The Sunday among the days [days] of Ethiopian month [month], if any. */
    private fun sundayIn(ethYear: Int, month: Int, days: IntRange): LocalDate? =
        days.asSequence()
            .map { EthiopianDate(ethYear, month, it).toGregorian() }
            .firstOrNull { it.dayOfWeek == DayOfWeek.SUNDAY }
}
