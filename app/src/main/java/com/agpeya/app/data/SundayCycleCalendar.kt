package com.agpeya.app.data

import com.agpeya.app.data.BahreHasab.SeasonWindow
import com.agpeya.app.ui.common.EthiopianDate
import java.time.LocalDate

/**
 * The Sunday seasons that the ግጻዌ's Part 3 (ግጻዌ ዘሰናብት ወመዝሙር) counts from a
 * fixed Ethiopian date rather than from Fasika. [BahreHasab.movableSeasonOn]
 * covers the computus-driven ones (ነነዌ, ዐቢይ ጾም, ትንሣኤ); this covers the rest of
 * the year, so every Sunday resolves to the hymn rows the book prints for it.
 *
 * Season keys and week numbers match the `season` / `week` fields of
 * `sunday-cycle-gitsawe.json` and `seasonal-gitsawe.json`:
 *
 * - `tsige`      — ዘመነ ጽጌ, መስከረም ፳፮ – ኅዳር ፭; week = Sunday ordinal (1–6).
 * - `astemhro`   — ዘመነ አስተምህሮ, ኅዳር ፮ – ታኅሣሥ ፮; week = Sunday ordinal (1–5).
 * - `sibket`, `birhan`, `nolawi` — the three Sundays before ልደት, printed as the
 *   fixed weeks ታኅሣሥ ፯–፲፫, ፲፬–፳, ፳፩–፳፯.
 * - `lidet`      — from ልደት to the Sunday before ዘወረደ; week 0 is ልደት itself,
 *   otherwise the book's printed ordinal. The book prints nine hymns whose
 *   sixth (ስምዖን) is a fixed-date rule, so the regular Sundays take 1–5, 7–9.
 * - `seneAstemhro` — the book's ሰኔ ፲፯–፳፭ አስተምህሮ; week = Sunday ordinal.
 * - `kremt`      — ዘመነ ክረምት, ሰኔ ፳፬ – ጳጉሜን; week = the book's printed hymn
 *   ordinal (፩ኛ–፲፭ኛ). The book numbers the hymns as one list in which the
 *   feast-conditional rows (ሐዋርያት ፫, ቂርቆስ ፮, ነሐሴ ፮ ፲, ጳጉሜን ፲፭) sit between
 *   the regular Sundays of four sub-seasons, each of which the regular
 *   ordinals fill exactly:
 *     ዘርዕ ደመና         ሰኔ ፳፬ – ሐምሌ ፲፰  → ፩ ፪ ፬ ፭
 *     መብረቅ ነጐድጓድ …   ሐምሌ ፲፱ – ነሐሴ ፲፭ → ፯ ፰ ፱ ፲፩
 *     ዕጐለ ቋዓት …       ነሐሴ ፲፮ – ፳፯     → ፲፪ ፲፫
 *     ኑኀ ነግህ …        ነሐሴ ፳፰ – ጳጉሜን   → ፲፬
 *   The sub-season is emitted as its own window (`zere`, `mebreq`, `egule`,
 *   `nuha`) ahead of the `kremt` one, so a label can name it.
 * - `filseta`    — Sundays inside ጾመ ፍልሰታ (ነሐሴ ፩–፲፭); week = ordinal.
 * - `pagumen`    — any day of ጳጉሜን.
 *
 * Every window is computed for any date (a weekday takes the ordinal of the
 * seven-day block it falls in) so the same function can label the season; the
 * Sunday-only rules are applied by the caller.
 */
object SundayCycleCalendar {

    /** ልደት is ታኅሣሥ ፳፱, or ፳፰ in ዘመነ ዮሐንስ (the year after ጳጉሜን ፮). */
    fun ledet(ethYear: Int): EthiopianDate =
        EthiopianDate(ethYear, 4, if (BahreHasab.evangelist(ethYear) == 0) 28 else 29)

    /** Fixed-anchored Sunday-season windows covering [date], most specific first. */
    fun windowsOn(date: LocalDate): List<SeasonWindow> {
        val e = EthiopianDate.from(date)
        val doy = dayOfYear(e)
        val out = mutableListOf<SeasonWindow>()
        when {
            doy in TSIGE_START..TSIGE_END -> out += SeasonWindow("tsige", ordinal(doy, TSIGE_START))
            doy in ASTEMHRO_START..ASTEMHRO_END -> out += SeasonWindow("astemhro", ordinal(doy, ASTEMHRO_START))
            doy in 97..103 -> out += SeasonWindow("sibket", null)
            doy in 104..110 -> out += SeasonWindow("birhan", null)
            doy in 111..117 -> out += SeasonWindow("nolawi", null)
            doy >= KREMT_START -> out += kremtWindows(e, doy)
        }
        lidetWindow(date, e, doy)?.let { out += it }
        if (doy in SENE_ASTEMHRO_START..SENE_ASTEMHRO_END) {
            out += SeasonWindow("seneAstemhro", ordinal(doy, SENE_ASTEMHRO_START))
        }
        return out
    }

    private fun lidetWindow(date: LocalDate, e: EthiopianDate, doy: Int): SeasonWindow? {
        val ledetDoy = dayOfYear(ledet(e.year))
        if (doy == ledetDoy) return SeasonWindow("lidet", 0)
        if (doy < ledetDoy) return null
        // The series runs up to (not including) ዘወረደ, the first Sunday of Lent.
        val zewerede = BahreHasab.nineveh(e.year).plusDays(13)
        if (!date.isBefore(zewerede)) return null
        val k = ordinal(doy, ledetDoy + 1)
        val printed = when {
            k <= 5 -> k
            k <= 8 -> k + 1          // ፮ተኛ is the ስምዖን rule, a fixed date
            else -> 9
        }
        return SeasonWindow("lidet", printed)
    }

    private fun kremtWindows(e: EthiopianDate, doy: Int): List<SeasonWindow> {
        val out = mutableListOf<SeasonWindow>()
        fun pool(sub: String, start: Int, printed: List<Int>) {
            out += SeasonWindow(sub, null)
            out += SeasonWindow("kremt", printed[(ordinal(doy, start) - 1).coerceIn(printed.indices)])
        }
        when {
            doy <= ZERE_END -> pool("zere", KREMT_START, listOf(1, 2, 4, 5))
            doy <= MEBREQ_END -> pool("mebreq", ZERE_END + 1, listOf(7, 8, 9, 11))
            doy <= EGULE_END -> pool("egule", MEBREQ_END + 1, listOf(12, 13))
            else -> pool("nuha", EGULE_END + 1, listOf(14, 14))
        }
        if (doy in FILSETA_START..FILSETA_END) out += SeasonWindow("filseta", ordinal(doy, FILSETA_START))
        if (e.month == 13) out += SeasonWindow("pagumen", null)
        // The feast-conditional ordinals, for rows that key on them rather than on the date.
        when {
            e.month == 11 && e.day == 5 -> 3      // ሐዋርያት
            e.month == 11 && e.day == 19 -> 6     // ቂርቆስ
            e.month == 12 && e.day == 6 -> 10     // ነሐሴ ፮
            e.month == 13 -> 15                   // ጳጉሜን
            else -> null
        }?.let { out += SeasonWindow("kremt", it) }
        return out
    }

    /** 1-based day of the Ethiopian year; ጳጉሜን is 361–366. */
    private fun dayOfYear(e: EthiopianDate): Int = (e.month - 1) * 30 + e.day

    private fun ordinal(doy: Int, start: Int): Int = (doy - start) / 7 + 1

    private const val TSIGE_START = 26            // መስከረም ፳፮
    private const val TSIGE_END = 65              // ኅዳር ፭
    private const val ASTEMHRO_START = 66         // ኅዳር ፮
    private const val ASTEMHRO_END = 96           // ታኅሣሥ ፮
    private const val SENE_ASTEMHRO_START = 287   // ሰኔ ፲፯
    private const val SENE_ASTEMHRO_END = 295     // ሰኔ ፳፭
    private const val KREMT_START = 294           // ሰኔ ፳፬ (በአተ ክረምት)
    private const val ZERE_END = 318              // ሐምሌ ፲፰ (ቂርቆስ is ፲፱)
    private const val MEBREQ_END = 345            // ነሐሴ ፲፭ (ፍልሰታ is ፲፮)
    private const val EGULE_END = 357             // ነሐሴ ፳፯ (አብርሃም is ፳፰)
    private const val FILSETA_START = 331         // ነሐሴ ፩
    private const val FILSETA_END = 345           // ነሐሴ ፲፭
}
