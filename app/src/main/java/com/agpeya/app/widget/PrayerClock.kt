package com.agpeya.app.widget

import java.time.LocalTime

/** Website's canonical times, deliberately independent of personal notification schedules. */
internal data class ClockHour(val id: String, val number: String, val amharic: String, val english: String, val hour: Int)

internal object PrayerClock {
    val hours = listOf(
        ClockHour("morning", "፩", "ጸሎተ ነግህ", "Morning", 6),
        ClockHour("terce", "፪", "ሠለስት", "Terce", 9),
        ClockHour("sext", "፫", "ቀትር", "Sext", 12),
        ClockHour("none", "፬", "ተሰዓት", "None", 15),
        ClockHour("vespers", "፭", "ሰርክ", "Vespers", 18),
        ClockHour("compline", "፮", "ንዋም", "Compline", 21),
        ClockHour("midnight", "፯", "መንፈቀ ሌሊት", "Midnight", 0),
    )
    fun current(time: LocalTime): ClockHour = hours.filter { it.hour <= time.hour }.maxBy { it.hour }
    fun fraction(time: LocalTime): Float = (time.hour * 60 + time.minute) / 1440f
}
