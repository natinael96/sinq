package com.agpeya.app.widget

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Which day the ግጻዌ widget shows, and when it next has to change.
 *
 * The Church's day turns in the evening, not at midnight: ዋዜማ is sung the night
 * before the feast it belongs to, and someone looking at the widget after
 * supper is usually looking ahead to what they will pray at dawn, not back at a
 * morning that is over. So from a chosen hour the card moves to the next day.
 *
 * Pure and here rather than in the provider, so the rule can be tested without
 * an emulator — every other part of a widget needs one.
 */

/** Evening, as the app ships it. 20:00. */
const val DEFAULT_WIDGET_ROLLOVER_MIN: Int = 20 * 60

/**
 * The date the card should carry.
 *
 * [rolloverMinute] is minutes past midnight, or null when the reader has turned
 * the rollover off and the card should simply follow the calendar.
 *
 * A rollover of 0 is honoured literally — every minute is at or past midnight,
 * so the card sits permanently one day ahead. That is a strange thing to ask
 * for and a legitimate one: it is what someone preparing a day early wants.
 */
internal fun widgetDayFor(now: LocalDateTime, rolloverMinute: Int?): LocalDate {
    val today = now.toLocalDate()
    if (rolloverMinute == null) return today
    val minuteOfDay = now.hour * 60 + now.minute
    return if (minuteOfDay >= rolloverMinute) today.plusDays(1) else today
}

/**
 * When the card next shows something different.
 *
 * With a rollover there is exactly one such moment a day, and it is the
 * rollover itself — **not** midnight. Between 20:00 on Monday and 20:00 on
 * Tuesday the card reads Tuesday throughout: at midnight the date it shows
 * stops being "tomorrow" and starts being "today" without the reader seeing
 * anything change. Refreshing then would redraw the same card.
 *
 * Without a rollover the card changes just past midnight, as it did before.
 */
internal fun nextWidgetRefresh(now: LocalDateTime, rolloverMinute: Int?): LocalDateTime {
    if (rolloverMinute == null) {
        return now.toLocalDate().plusDays(1).atStartOfDay().plusMinutes(1)
    }
    val at = now.toLocalDate().atTime(rolloverMinute / 60, rolloverMinute % 60)
    return if (at.isAfter(now)) at else at.plusDays(1)
}
