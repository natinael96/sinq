package com.agpeya.app.ui.common

import com.agpeya.app.data.Language

/**
 * Resolves localized liturgical names for canonical hours and fasts
 * based on the active [Language] mode:
 * - [Language.AMH_ENG]: Bilingual / mixed representation (e.g. "ነግህ · Morning")
 * - [Language.ENGLISH]: Strict English representation (e.g. "Morning")
 * - [Language.AMHARIC]: Strict Amharic representation (e.g. "ነግህ" / "ጸሎተ ነግህ")
 */
object PrayerDisplay {

    fun hourName(
        hourId: String,
        rawName: String = "",
        transliteration: String = "",
        mode: Language = Language.AMH_ENG,
        compact: Boolean = false,
    ): String {
        val cleanId = hourId.removePrefix("hour_")
        val english = when (cleanId) {
            "morning" -> if (compact) "Morning" else "Morning Prayer"
            "terce" -> "Terce"
            "sext" -> "Sext"
            "none" -> "None"
            "vespers" -> "Vespers"
            "compline" -> "Compline"
            "midnight" -> if (compact) "Midnight" else "Midnight Prayer"
            "veil" -> if (compact) "Veil" else "Night 9th Hour"
            else -> transliteration.ifBlank { rawName.ifBlank { hourId } }
        }
        val amharic = when (cleanId) {
            "morning" -> if (compact) "ነግህ" else "ጸሎተ ነግህ"
            "terce" -> if (compact) "ሠለስት" else "ጸሎተ ሠለስት"
            "sext" -> if (compact) "ቀትር" else "ጸሎተ ቀትር"
            "none" -> if (compact) "ተሰዓት" else "ጸሎተ ተሰዓት"
            "vespers" -> if (compact) "ሰርክ" else "ጸሎተ ሰርክ"
            "compline" -> if (compact) "ንዋም" else "ጸሎተ ንዋም"
            "midnight" -> if (compact) "መንፈቀ ሌሊት" else "ጸሎተ መንፈቀ ሌሊት"
            "veil" -> "ሌሊት ፱ ሰዓት"
            else -> rawName.ifBlank { hourId }
        }
        return when (mode) {
            Language.ENGLISH -> english
            Language.AMHARIC -> amharic
            Language.AMH_ENG -> if (compact) "$amharic · $english" else "$amharic ($english)"
        }
    }

    fun fastName(
        seasonalNameAm: String?,
        seasonalNameEn: String?,
        isWeeklyWednesday: Boolean,
        isWeeklyFriday: Boolean,
        mode: Language = Language.AMH_ENG,
    ): String? {
        if (seasonalNameAm != null) {
            val en = seasonalNameEn ?: seasonalNameAm
            return when (mode) {
                Language.ENGLISH -> en
                Language.AMHARIC -> seasonalNameAm
                Language.AMH_ENG -> if (en != seasonalNameAm) "$seasonalNameAm ($en)" else seasonalNameAm
            }
        }
        if (isWeeklyWednesday) {
            return when (mode) {
                Language.ENGLISH -> "Wednesday Fast"
                Language.AMHARIC -> "ጾመ ድህነት (ረቡዕ)"
                Language.AMH_ENG -> "ጾመ ድህነት (Wed Fast)"
            }
        }
        if (isWeeklyFriday) {
            return when (mode) {
                Language.ENGLISH -> "Friday Fast"
                Language.AMHARIC -> "ጾመ ድህነት (ዓርብ)"
                Language.AMH_ENG -> "ጾመ ድህነት (Fri Fast)"
            }
        }
        return null
    }

    fun fastName(
        date: java.time.LocalDate,
        mode: Language = Language.AMH_ENG,
    ): String? {
        val seasonal = com.agpeya.app.data.FastingCalendar.fastOn(date)
        val isWed = date.dayOfWeek == java.time.DayOfWeek.WEDNESDAY && com.agpeya.app.data.FastingCalendar.isWeeklyFastDay(date)
        val isFri = date.dayOfWeek == java.time.DayOfWeek.FRIDAY && com.agpeya.app.data.FastingCalendar.isWeeklyFastDay(date)
        return fastName(
            seasonalNameAm = seasonal?.nameAm,
            seasonalNameEn = seasonal?.nameEn,
            isWeeklyWednesday = isWed,
            isWeeklyFriday = isFri,
            mode = mode,
        )
    }
}
