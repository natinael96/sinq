package com.agpeya.app.widget

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The widget prepares for the next liturgical day from the evening onward. */
internal val GITSAWE_WIDGET_EVENING: LocalTime = LocalTime.of(19, 0)

internal fun gitsaweWidgetDate(
    now: LocalDateTime,
    evening: LocalTime = GITSAWE_WIDGET_EVENING,
): LocalDate = if (now.toLocalTime() >= evening) {
    now.toLocalDate().plusDays(1)
} else {
    now.toLocalDate()
}
