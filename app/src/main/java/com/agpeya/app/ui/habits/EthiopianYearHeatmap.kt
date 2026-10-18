package com.agpeya.app.ui.habits

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
import com.agpeya.app.data.FastingCalendar
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.ui.common.EthiopianDate
import com.agpeya.app.ui.common.EthiopianDatePickerDialog
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.sinqColors
import java.time.LocalDate

// Compact dimensions: 12dp cell + 2dp gap = 14dp column.
// Height stays compact (~98dp for the 7 rows), allowing 5-6 months visible on screen.
private val CELL = 12.dp
private val GAP = 2.dp
private val COL = 14.dp

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
    buttonSize: Dp = 32.dp,
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
                modifier = Modifier.size(18.dp),
            )
        }
        val eraText = when (s.languageMode) {
            com.agpeya.app.data.Language.ENGLISH -> "$ecYear E.C."
            com.agpeya.app.data.Language.AMHARIC -> "$ecYear ዓ.ም"
            com.agpeya.app.data.Language.AMH_ENG -> "$ecYear ዓ.ም"
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
                modifier = Modifier.size(18.dp),
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
    val manuscriptBg = if (isDark) Color(0xFF0B2822) else Color(0xFFFAF7EF)
    val manuscriptBorder = if (isDark) Color(0xFF235448) else Color(0xFFD6C7A1)
    val goldFillet = if (isDark) Color(0xFFE0BC65) else Color(0xFF7E5F1E)
    val fastingBorder = if (isDark) Color(0xFF529467) else Color(0xFF4D785B)
    val fastingDot = if (isDark) Color(0xFF7ECA96) else Color(0xFF35533F)
    val fastingWash = if (isDark) Color(0x3362A478) else Color(0x243E5A47)

    val empty = MaterialTheme.colorScheme.surfaceVariant
    val gold = MaterialTheme.colorScheme.secondary

    fun cellColor(date: LocalDate, isFast: Boolean, level: Int): Color {
        if (date.isBefore(start) || date.isAfter(end)) return Color.Transparent
        if (date.isAfter(today)) {
            return if (isFast) fastingWash.copy(alpha = 0.08f) else empty.copy(alpha = 0.35f)
        }
        return when (level) {
            0 -> if (isFast) fastingWash else empty
            1 -> gold.copy(alpha = 0.35f)
            2 -> gold.copy(alpha = 0.65f)
            3 -> gold.copy(alpha = 0.85f)
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
            val fraction = todayIndex.toFloat() / weeks.lastIndex.coerceAtLeast(1)
            scroll.scrollTo((scroll.maxValue * fraction).toInt())
        } else {
            scroll.scrollTo(0)
        }
    }

    // Manuscript Folio Plate Container
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = manuscriptBg,
        border = BorderStroke(1.5.dp, manuscriptBorder),
        shadowElevation = if (isDark) 4.dp else 2.dp,
    ) {
        Column(Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)) {
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
                                com.agpeya.app.data.Language.AMHARIC -> "${geezNumeral(yearStats.first)} ቀናት"
                                com.agpeya.app.data.Language.ENGLISH -> "${yearStats.first} days"
                                com.agpeya.app.data.Language.AMH_ENG -> "${geezNumeral(yearStats.first)} ቀናት (${yearStats.first}d)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = goldFillet,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                // Right: Year Switcher Chevrons & Quick Calendar Picker
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EthiopianYearSwitcher(
                        ecYear = ecYear,
                        today = today,
                        earliestYear = earliestYear,
                        buttonSize = 32.dp,
                        onYearChange = onYearChange,
                    )
                    IconButton(
                        onClick = { pickingDate = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = s.dateLabel,
                            tint = goldFillet,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xxs))

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

            Spacer(Modifier.height(Spacing.xxs))

            // Debossed Inner Parchment Bed for Grid
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0x28000000) else Color(0x08000000),
                border = BorderStroke(1.dp, manuscriptBorder.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs),
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
                                        val a11yDesc = remember(date, isFast, level, s) {
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
                                                        onClick = { onDaySelect(date) },
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
                    Text(s.less, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(empty, gold.copy(alpha = 0.35f), gold.copy(alpha = 0.65f), gold).forEach { c ->
                        Box(
                            Modifier
                                .size(CELL)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(c)
                        )
                    }
                    Text(s.more, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            com.agpeya.app.data.Language.AMHARIC -> "● ጾም"
                            com.agpeya.app.data.Language.ENGLISH -> "● Fasting"
                            com.agpeya.app.data.Language.AMH_ENG -> "● ጾም (Fast)"
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
                            com.agpeya.app.data.Language.AMHARIC -> "ዛሬ"
                            com.agpeya.app.data.Language.ENGLISH -> "Today"
                            com.agpeya.app.data.Language.AMH_ENG -> "ዛሬ (Today)"
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
                            com.agpeya.app.data.Language.AMHARIC -> "✦ ሙሉ"
                            com.agpeya.app.data.Language.ENGLISH -> "✦ Full"
                            com.agpeya.app.data.Language.AMH_ENG -> "✦ ሙሉ (Full)"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
