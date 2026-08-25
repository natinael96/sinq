package com.agpeya.app.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.FastingCalendar
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.ui.common.EthiopianDate
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.sinqColors
import java.time.LocalDate

private val CELL = 13.dp
private val GAP = 1.5.dp
private val COL = 16.dp // CELL + 2*GAP

/** Nothing can be logged before Hamle 1, 2018 EC — the app's first day. */
internal val APP_EPOCH_EC = EthiopianDate(2018, 11, 1)

/** Days in [ecYear] for which this installation can possibly hold records. */
internal fun journeyYearSelectableRange(ecYear: Int, today: LocalDate): ClosedRange<LocalDate>? {
    val yearStart = EthiopianDate(ecYear, 1, 1).toGregorian()
    val yearEnd = EthiopianDate(ecYear + 1, 1, 1).toGregorian().minusDays(1)
    val first = maxOf(yearStart, APP_EPOCH_EC.toGregorian())
    val last = minOf(yearEnd, today)
    return if (first <= last) first..last else null
}

/**
 * Calendar heatmap of one Ethiopian year: month labels above, weekday labels
 * on the left, and an EC-year switcher below. The grid clips to
 * [APP_EPOCH_EC .. today].
 */
@Composable
fun EthiopianYearHeatmap(
    records: Map<String, Set<String>>,
    today: LocalDate,
    ecYear: Int,
    selectedDay: LocalDate?,
    maxPossible: Int,
    modifier: Modifier = Modifier,
    onYearChange: (Int) -> Unit,
    onDaySelect: (LocalDate) -> Unit,
) {
    val s = LocalStrings.current
    val currentEc = remember(today) { EthiopianDate.from(today).year }

    // The whole Ethiopian year, መስከረም 1 → ጳጉሜን end. Future days render as
    // faint placeholders so the year's shape is always visible.
    val start = remember(ecYear) { EthiopianDate(ecYear, 1, 1).toGregorian() }
    val end = remember(ecYear) { EthiopianDate(ecYear + 1, 1, 1).toGregorian().minusDays(1) }
    val selectableRange = remember(ecYear, today) { journeyYearSelectableRange(ecYear, today) }

    // Week columns, Monday-aligned, covering [start..end].
    val weeks = remember(start, end) {
        val firstCol = start.minusDays((start.dayOfWeek.value - 1).toLong())
        val list = mutableListOf<List<LocalDate>>()
        var col = firstCol
        while (!col.isAfter(end)) {
            list.add((0..6).map { col.plusDays(it.toLong()) })
            col = col.plusWeeks(1)
        }
        list
    }

    // The fasts touching the displayed year, so days can be read inside the
    // Church's calendar. A quiet wash on *unprayed* cells only — prayer still
    // shows as gold, and the liturgical context never outshines the history.
    val fasts = remember(ecYear) {
        (ecYear - 1..ecYear + 1).flatMap {
            runCatching { FastingCalendar.fastsOf(it) }.getOrDefault(emptyList())
        }
    }
    fun inFast(date: LocalDate): Boolean = fasts.any { it.contains(date) }

    fun dayDescription(date: LocalDate): String = listOfNotNull(
        com.agpeya.app.ui.common.formatEthiopian(date, s),
        s.habitsCount(HabitsRepository.dayCount(records, date)),
        fasts.firstOrNull { it.contains(date) }?.nameAm,
    ).joinToString(" · ")

    val empty = MaterialTheme.colorScheme.surfaceVariant
    val gold = MaterialTheme.colorScheme.secondary
    val fastWash = sinqColors.hero.copy(alpha = 0.14f)
    fun cellColor(date: LocalDate): Color {
        if (date.isBefore(start) || date.isAfter(end)) return Color.Transparent
        if (date.isAfter(today)) {
            // Future days stay faint placeholders; an upcoming fast shows even
            // fainter green, so the year ahead keeps its shape.
            return if (inFast(date)) fastWash.copy(alpha = 0.08f) else empty.copy(alpha = 0.35f)
        }
        return when (HabitsRepository.level(HabitsRepository.dayCount(records, date), maxPossible)) {
            0 -> if (inFast(date)) fastWash else empty
            1 -> gold.copy(alpha = 0.30f)
            2 -> gold.copy(alpha = 0.50f)
            3 -> gold.copy(alpha = 0.75f)
            else -> gold
        }
    }

    // Month label spans: consecutive columns grouped by the EC month of their
    // first in-range day.
    val monthSpans = remember(weeks, start, end) {
        val spans = mutableListOf<Pair<Int, Int>>() // (ecMonth, columns)
        var lastMonth = -1
        weeks.forEach { week ->
            val anchor = week.firstOrNull { !it.isBefore(start) && !it.isAfter(end) }
            val m = anchor?.let { EthiopianDate.from(it).month } ?: lastMonth
            if (m == lastMonth && spans.isNotEmpty()) {
                val (mm, c) = spans.removeAt(spans.size - 1)
                spans.add(mm to c + 1)
            } else {
                spans.add(m to 1)
                lastMonth = m
            }
        }
        spans
    }

    val scroll = rememberScrollState()
    LaunchedEffect(weeks.size, ecYear) {
        if (ecYear == currentEc && weeks.isNotEmpty()) {
            // Scroll so today's column is in view (the year now extends past today).
            val todayIndex = weeks.indexOfFirst { week -> week.any { it == today } }
                .takeIf { it >= 0 } ?: weeks.lastIndex
            val fraction = todayIndex.toFloat() / weeks.lastIndex.coerceAtLeast(1)
            scroll.scrollTo((scroll.maxValue * fraction).toInt())
        } else {
            scroll.scrollTo(0)
        }
    }

    Column(modifier) {
        Row {
            // Weekday labels (fixed), offset below the month-label row.
            Column(Modifier.padding(top = 16.dp)) {
                s.dayLabels.forEach { d ->
                    Box(Modifier.height(COL).width(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            d,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.horizontalScroll(scroll)) {
                // Month labels
                Row(Modifier.height(16.dp)) {
                    monthSpans.forEach { (m, cols) ->
                        Text(
                            text = if (m in 1..13) s.ethMonths[m - 1] else "",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 8.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            softWrap = false,
                            modifier = Modifier.width(COL * cols),
                        )
                    }
                }
                // Grid
                Row {
                    weeks.forEach { week ->
                        Column {
                            week.forEach { date ->
                                val inRange = selectableRange?.contains(date) == true
                                val description = if (inRange) dayDescription(date) else null
                                Spacer(
                                    Modifier
                                        .padding(GAP)
                                        .size(CELL)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(cellColor(date))
                                        .then(
                                            if (inRange) Modifier
                                                .semantics {
                                                    contentDescription = description.orEmpty()
                                                    selected = date == selectedDay
                                                }
                                                .clickable(
                                                    role = Role.Button,
                                                    onClickLabel = description,
                                                ) { onDaySelect(date) }
                                            else Modifier
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Legend
        Spacer(Modifier.height(10.dp))
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(s.less, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(empty, gold.copy(alpha = 0.30f), gold.copy(alpha = 0.50f), gold.copy(alpha = 0.75f), gold).forEach { c ->
                Spacer(Modifier.padding(GAP).size(CELL).clip(RoundedCornerShape(2.dp)).background(c))
            }
            Text(s.more, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(Spacing.sm))
            Spacer(Modifier.padding(GAP).size(CELL).clip(RoundedCornerShape(2.dp)).background(fastWash))
            Text(s.fastLegendLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Compact year switcher
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onYearChange(ecYear - 1) },
                enabled = ecYear > APP_EPOCH_EC.year,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = s.previousYear,
                    tint = if (ecYear > APP_EPOCH_EC.year) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            Text(
                text = "$ecYear ${s.eraSuffix}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(
                onClick = { onYearChange(ecYear + 1) },
                enabled = ecYear < currentEc,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = s.nextYear,
                    tint = if (ecYear < currentEc) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }

        val selectionStart = selectableRange?.start
        val selectionEnd = selectableRange?.endInclusive
        if (selectedDay != null && selectionStart != null && selectionEnd != null && selectableRange.contains(selectedDay)) {
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dayDescription(selectedDay),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onDaySelect(selectedDay.minusDays(1)) },
                    enabled = selectedDay.isAfter(selectionStart),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = s.previousDay,
                    )
                }
                IconButton(
                    onClick = { onDaySelect(selectedDay.plusDays(1)) },
                    enabled = selectedDay.isBefore(selectionEnd),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = s.nextDay,
                    )
                }
            }
        }
    }
}
