package com.sinq.app.ui.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinq.app.data.FastingCalendar
import com.sinq.app.data.HabitsRepository
import com.sinq.app.ui.common.EthiopianDate
import com.sinq.app.ui.common.EthiopianDatePickerDialog
import com.sinq.app.ui.common.formatEthiopian
import com.sinq.app.ui.reading.geezNumeral
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.Spacing
import com.sinq.app.ui.theme.sinqColors
import java.time.LocalDate

// Generous dimensions: 15dp cell + 2.5dp gap = 17.5dp column.
private val CELL = 15.dp
private val GAP = 2.5.dp
private val COL = 17.5.dp

/** Nothing can be logged before Hamle 1, 2018 EC — the app's first day. */
internal val APP_EPOCH_EC = EthiopianDate(2018, 11, 1)

/** Days in [ecYear] for which this installation can possibly hold records. */
internal fun journeyYearSelectableRange(
    ecYear: Int,
    today: LocalDate,
    earliest: LocalDate = APP_EPOCH_EC.toGregorian(),
): ClosedRange<LocalDate>? {
    val yearStart = EthiopianDate(ecYear, 1, 1).toGregorian()
    val yearEnd = EthiopianDate(ecYear + 1, 1, 1).toGregorian().minusDays(1)
    val first = maxOf(yearStart, earliest)
    val last = minOf(yearEnd, today)
    return if (first <= last) first..last else null
}

/**
 * The year switcher, for the trailing edge of the grid's own section header.
 */
@Composable
fun EthiopianYearSwitcher(
    ecYear: Int,
    today: LocalDate,
    earliestYear: Int = APP_EPOCH_EC.year,
    buttonSize: Dp = 48.dp,
    modifier: Modifier = Modifier,
    onYearChange: (Int) -> Unit,
) {
    val s = LocalStrings.current
    val currentEc = remember(today) { EthiopianDate.from(today).year }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onYearChange(ecYear - 1) },
            enabled = ecYear > earliestYear,
            modifier = Modifier.size(buttonSize),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = s.previousYear,
                tint = if (ecYear > earliestYear) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp),
            )
        }
        val eraText = when (s.languageMode) {
            com.sinq.app.data.Language.ENGLISH -> "$ecYear E.C."
            com.sinq.app.data.Language.AMHARIC -> "$ecYear ዓ.ም"
        }
        Text(
            text = eraText,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.secondary,
        )
        IconButton(
            onClick = { onYearChange(ecYear + 1) },
            enabled = ecYear < currentEc,
            modifier = Modifier.size(buttonSize),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = s.nextYear,
                tint = if (ecYear < currentEc) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Calendar heatmap of one Ethiopian year rendered in "● የብራና ሰሌዳ · Sacred Manuscript Folio" style.
 *
 * Compact height-wise so 5–6 months fit gracefully on screen, with:
 * - Sacred Manuscript parchment folio framing & corner rosettes (፨)
 * - Perforated/dashed border & centered dot (●) for fasting days (ዐቢይ ጾም, ጾመ ፍልሰታ, ረቡዕ/ዓርብ)
 * - Direct cell click interaction that displays that day's canonical hours, habits, and reflections.
 */
@Composable
fun EthiopianYearHeatmap(
    records: Map<String, Set<String>>,
    today: LocalDate,
    ecYear: Int,
    selectedDay: LocalDate?,
    modifier: Modifier = Modifier,
    earliestYear: Int = APP_EPOCH_EC.year,
    onYearChange: (Int) -> Unit,
    onDaySelect: (LocalDate) -> Unit,
) {
    val s = LocalStrings.current
    val earliest = remember(records) {
        minOf(APP_EPOCH_EC.toGregorian(), records.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.minOrNull() ?: today)
    }
    var pickingDate by remember { mutableStateOf(false) }
    if (pickingDate) {
        EthiopianDatePickerDialog(
            initial = selectedDay ?: today,
            onDismiss = { pickingDate = false },
            onSelect = { date ->
                val bounded = date.coerceIn(earliest, today)
                onYearChange(EthiopianDate.from(bounded).year)
                onDaySelect(bounded)
                pickingDate = false
            },
        )
    }

    val currentEc = remember(today) { EthiopianDate.from(today).year }

    // The whole Ethiopian year, መስከረም 1 → ጳጉሜን end.
    val start = remember(ecYear) { EthiopianDate(ecYear, 1, 1).toGregorian() }
    val end = remember(ecYear) { EthiopianDate(ecYear + 1, 1, 1).toGregorian().minusDays(1) }
    val selectableRange = remember(ecYear, today, earliest) { journeyYearSelectableRange(ecYear, today, earliest) }

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

    // Fasts touching the displayed year (seasonal fasts + weekly Wednesday/Friday)
    val fasts = remember(ecYear) {
        (ecYear - 1..ecYear + 1).flatMap {
            runCatching { FastingCalendar.fastsOf(it) }.getOrDefault(emptyList())
        }
    }
    fun inFast(date: LocalDate): Boolean = fasts.any { it.contains(date) } || FastingCalendar.isWeeklyFastDay(date)

    // Annual prayer & fast statistics
    val yearStats = remember(records, start, end, today) {
        var prayedDays = 0
        var fastDays = 0
        var totalEligible = 0
        var cur = start
        val limit = minOf(end, today)
        while (!cur.isAfter(limit)) {
            totalEligible++
            val count = HabitsRepository.dayCount(records, cur)
            if (count > 0) prayedDays++
            if (inFast(cur)) fastDays++
            cur = cur.plusDays(1)
        }
        Triple(prayedDays, fastDays, totalEligible)
    }

    // Palette & Manuscript Folio Tokens
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.2f
    val sinq = com.sinq.app.ui.theme.sinqColors
    val manuscriptBg = sinq.manuscriptBg
    val manuscriptBorder = sinq.manuscriptBorder
    val goldFillet = sinq.goldFillet
    val fastingBorder = sinq.fastingBorder
    val fastingDot = sinq.fastingDot
    val fastingWash = sinq.fastingWash

    val empty = MaterialTheme.colorScheme.surfaceVariant
    val gold = MaterialTheme.colorScheme.secondary

    fun cellColor(date: LocalDate, isFast: Boolean, level: Int): Color {
        if (date.isBefore(start) || date.isAfter(end)) return Color.Transparent
        if (date.isAfter(today)) {
            return if (isFast) fastingWash.copy(alpha = 0.08f) else empty.copy(alpha = 0.35f)
        }
        return when (level) {
            0 -> if (isFast) fastingWash else empty
            1 -> if (isDark) gold.copy(alpha = 0.28f) else gold.copy(alpha = 0.30f)
            2 -> if (isDark) gold.copy(alpha = 0.55f) else gold.copy(alpha = 0.58f)
            3 -> if (isDark) gold.copy(alpha = 0.80f) else gold.copy(alpha = 0.82f)
            else -> gold
        }
    }

    // Month label spans: consecutive columns grouped by the EC month
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
            }
            lastMonth = m
        }
        spans
    }

    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val fontScale = density.fontScale.coerceAtLeast(1f)
    val cell = CELL * fontScale
    val gap = GAP * fontScale
    val columnWidth = COL * fontScale
    val rowHeight = cell + gap * 2
    val monthHeaderHeight = 16.dp

    LaunchedEffect(weeks.size, ecYear) {
        if (ecYear == currentEc && weeks.isNotEmpty()) {
            val todayIndex = weeks.indexOfFirst { week -> week.any { it == today } }
                .takeIf { it >= 0 } ?: weeks.lastIndex
            val colPx = density.run { columnWidth.toPx() }
            val centerOffset = density.run { 120.dp.toPx() }
            val targetOffset = (todayIndex * colPx - centerOffset).toInt().coerceIn(0, scroll.maxValue)
            scroll.scrollTo(targetOffset)
        } else {
            scroll.scrollTo(0)
        }
    }

    // Open Folio Heatmap Display (unboxed)
    val pageBg = MaterialTheme.colorScheme.background
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxs, vertical = Spacing.xs),
    ) {
        // Consolidated Folio Header: Section Title, Stats Pill, Year Switcher & Date Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Left: Liturgical Rosette (፨), Title and Devotion Count Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = "፨",
                        style = MaterialTheme.typography.titleMedium,
                        color = goldFillet,
                    )
                    Text(
                        text = s.yearJourneyHeader,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0x33E0BC65) else Color(0x227E5F1E),
                    ) {
                        Text(
                            text = when (s.languageMode) {
                                com.sinq.app.data.Language.AMHARIC -> "${geezNumeral(yearStats.first)} ቀናት"
                                com.sinq.app.data.Language.ENGLISH -> "${yearStats.first} days"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = goldFillet,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                // Right: Year Switcher Chevrons & Calendar Picker
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EthiopianYearSwitcher(
                        ecYear = ecYear,
                        today = today,
                        earliestYear = earliestYear,
                        buttonSize = 48.dp,
                        onYearChange = onYearChange,
                    )
                    IconButton(
                        onClick = { pickingDate = true },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = s.dateLabel,
                            tint = goldFillet,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xs))

            // Gold Progress Hairline
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = Spacing.xxs),
                shape = RoundedCornerShape(1.dp),
                color = if (isDark) Color(0x33FFFFFF) else Color(0x1A000000),
            ) {
                val pct = if (yearStats.third > 0) (yearStats.first.toFloat() / yearStats.third.toFloat()).coerceIn(0f, 1f) else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pct)
                        .background(goldFillet)
                )
            }

            Spacer(Modifier.height(Spacing.xs))

            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0x28000000) else Color(0x08000000),
                border = BorderStroke(1.dp, manuscriptBorder.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // Weekday labels (fixed), aligned under month header row
                        Column(Modifier.padding(top = monthHeaderHeight)) {
                            s.dayLabels.forEach { d ->
                                Box(
                                    modifier = Modifier
                                        .height(rowHeight)
                                        .width(columnWidth),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = d,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(Spacing.xxs))

                        // Horizontally Scrollable Month Spans & Week Grid
                        Column(Modifier.horizontalScroll(scroll)) {
                            // Month labels row
                            Row(Modifier.height(monthHeaderHeight), verticalAlignment = Alignment.CenterVertically) {
                                monthSpans.forEach { (m, cols) ->
                                    Text(
                                        text = if (m in 1..13) s.ethMonths[m - 1] else "",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                        color = goldFillet,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                        softWrap = false,
                                        modifier = Modifier.width(columnWidth * cols),
                                    )
                                }
                            }

                            // Day Grid
                            Row {
                                var prevMonth = -1
                                weeks.forEachIndexed { wIdx, week ->
                                    val curMonth = week.firstOrNull { !it.isBefore(start) && !it.isAfter(end) }
                                        ?.let { EthiopianDate.from(it).month } ?: prevMonth
                                    val isNewMonth = wIdx > 0 && curMonth != prevMonth && curMonth > 0
                                    prevMonth = curMonth

                                    if (isNewMonth) {
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(rowHeight * 7)
                                                .background(goldFillet.copy(alpha = 0.25f))
                                        )
                                    }

                                    Column {
                                        week.forEach { date ->
                                            val inRange = selectableRange?.contains(date) == true
                                            val isFast = inFast(date)
                                            val level = if (date.isAfter(today)) -1
                                            else HabitsRepository.level(HabitsRepository.dayCount(records, date))
                                            val bg = cellColor(date, isFast, level.coerceAtLeast(0))
                                            val isSelected = inRange && date == selectedDay
                                            val isToday = date == today

                                            val shape = RoundedCornerShape(2.5.dp)
                                            val a11yDesc = run {
                                                val ethDate = formatEthiopian(date, s)
                                                val prayStatus = when {
                                                    level <= 0 -> if (s.isAmharic) "ያልተጸለየ" else "No prayers logged"
                                                    level == 1 -> if (s.isAmharic) "፩ ጸሎት" else "1 prayer"
                                                    else -> if (s.isAmharic) "$level ጸሎቶች" else "$level prayers"
                                                }
                                                val fastStatus = if (isFast) (if (s.isAmharic) "የጾም ቀን" else "Fasting day") else ""
                                                listOf(ethDate, prayStatus, fastStatus).filter { it.isNotBlank() }.joinToString("፣ ")
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .padding(gap)
                                                    .size(cell)
                                                    .clip(shape)
                                                    .background(bg)
                                                    .semantics { contentDescription = a11yDesc }
                                                    .then(
                                                        if (inRange) Modifier.clickable(
                                                            role = Role.Button,
                                                            onClick = {
                                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                                                onDaySelect(date)
                                                            },
                                                        ) else Modifier
                                                    )
                                                    .then(
                                                        if (isSelected) Modifier.border(1.8.dp, MaterialTheme.colorScheme.secondary, shape)
                                                        else if (isToday) Modifier.border(1.2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f), shape)
                                                        else Modifier
                                                    )
                                                    .drawBehind {
                                                        val cellWidth = size.width
                                                        val cellHeight = size.height

                                                        // Fasting Day Styling: Perforated / Dashed Border + Sacred Center Dot (●)
                                                        if (isFast && !date.isBefore(start) && !date.isAfter(end)) {
                                                            val dashPx = 2.4.dp.toPx()
                                                            val gapPx = 1.6.dp.toPx()
                                                            drawRoundRect(
                                                                color = fastingBorder,
                                                                topLeft = Offset(0.5f, 0.5f),
                                                                size = Size(cellWidth - 1f, cellHeight - 1f),
                                                                cornerRadius = CornerRadius(2.dp.toPx()),
                                                                style = Stroke(
                                                                    width = 1.1.dp.toPx(),
                                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx)),
                                                                ),
                                                            )

                                                            val dotRadius = 1.4.dp.toPx()
                                                            val dotColor = if (level > 0) Color.White.copy(alpha = 0.92f) else fastingDot
                                                            drawCircle(
                                                                color = dotColor,
                                                                radius = dotRadius,
                                                                center = Offset(cellWidth / 2f, cellHeight / 2f),
                                                            )
                                                        }

                                                        // Level 3+ Sacred Illumination Mark (micro-cross)
                                                        if (level >= 3 && !date.isBefore(start) && !date.isAfter(end)) {
                                                            val cx = cellWidth / 2f
                                                            val cy = cellHeight / 2f
                                                            val arm = 1.6.dp.toPx()
                                                            val stroke = 0.9.dp.toPx()
                                                            val markColor = Color.White.copy(alpha = 0.92f)
                                                            drawLine(markColor, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = stroke)
                                                            drawLine(markColor, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = stroke)
                                                        }

                                                        // Today Beacon Center Dot when unprayed
                                                        if (isToday && level <= 0) {
                                                            drawCircle(
                                                                color = gold,
                                                                radius = 1.4.dp.toPx(),
                                                                center = Offset(cellWidth / 2f, cellHeight / 2f),
                                                            )
                                                        }
                                                    },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Left edge fade shadow
                    if (scroll.value > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = columnWidth + Spacing.xs)
                                .fillMaxHeight()
                                .width(36.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            pageBg,
                                            pageBg.copy(alpha = 0.85f),
                                            pageBg.copy(alpha = 0.35f),
                                            Color.Transparent,
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = goldFillet.copy(alpha = 0.75f),
                                modifier = Modifier.padding(start = 2.dp).size(14.dp),
                            )
                        }
                    }

                    // Right edge fade shadow
                    if (scroll.value < scroll.maxValue) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(36.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            pageBg.copy(alpha = 0.35f),
                                            pageBg.copy(alpha = 0.85f),
                                            pageBg,
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = goldFillet.copy(alpha = 0.75f),
                                modifier = Modifier.padding(end = 2.dp).size(14.dp),
                            )
                        }
                    }
                }
            }

            // Legend Strip: Intensity ladder + Fasting + Today + Full Prayer
            Spacer(Modifier.height(Spacing.xs))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xxs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Ladder
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Level 0", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(
                        0 to empty,
                        1 to (if (isDark) gold.copy(alpha = 0.28f) else gold.copy(alpha = 0.30f)),
                        2 to (if (isDark) gold.copy(alpha = 0.55f) else gold.copy(alpha = 0.58f)),
                        3 to gold,
                    ).forEach { (lvl, c) ->
                        Box(
                            Modifier
                                .size(CELL)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(c)
                                .semantics { contentDescription = "Level $lvl" }
                        )
                    }
                    Text("3+", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Fasting indicator
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .size(CELL)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(fastingWash)
                            .drawBehind {
                                val dashPx = 2.dp.toPx()
                                val gapPx = 1.5.dp.toPx()
                                drawRoundRect(
                                    color = fastingBorder,
                                    size = size,
                                    cornerRadius = CornerRadius(2.dp.toPx()),
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx)),
                                    ),
                                )
                                drawCircle(
                                    color = fastingDot,
                                    radius = 1.3.dp.toPx(),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                )
                            }
                    )
                    Text(
                        text = when (s.languageMode) {
                            com.sinq.app.data.Language.AMHARIC -> "● ጾም"
                            com.sinq.app.data.Language.ENGLISH -> "● Fasting"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Today indicator
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .size(CELL)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(empty)
                            .border(1.2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.5.dp))
                            .drawBehind {
                                drawCircle(
                                    color = gold,
                                    radius = 1.3.dp.toPx(),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                )
                            }
                    )
                    Text(
                        text = when (s.languageMode) {
                            com.sinq.app.data.Language.AMHARIC -> "ዛሬ"
                            com.sinq.app.data.Language.ENGLISH -> "Today"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Full prayer indicator
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .size(CELL)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(gold)
                            .drawBehind {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val arm = 1.6.dp.toPx()
                                val stroke = 0.9.dp.toPx()
                                drawLine(Color.White, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = stroke)
                                drawLine(Color.White, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = stroke)
                            }
                    )
                    Text(
                        text = when (s.languageMode) {
                            com.sinq.app.data.Language.AMHARIC -> "✦ ሙሉ"
                            com.sinq.app.data.Language.ENGLISH -> "✦ Full"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

