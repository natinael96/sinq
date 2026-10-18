package com.agpeya.app.ui.habits

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.agpeya.app.data.FastingCalendar
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import java.time.LocalDate

/**
 * Manuscript Folio contribution grid: 7 rows (Mon–Sun) × [weeksBack] weeks.
 * Carries the "● የብራና ሰሌዳ · Sacred Manuscript Folio" vellum framing,
 * center dot (●) and perforated/dashed edges for fasting days.
 */
@Composable
fun HabitHeatmap(
    records: Map<String, Set<String>>,
    today: LocalDate,
    modifier: Modifier = Modifier,
    weeksBack: Long = 52,
    showLegend: Boolean = true,
    cell: Dp = 12.dp,
    gap: Dp = 1.5.dp,
    onDayTap: ((LocalDate) -> Unit)? = null,
) {
    val weeks = remember(today, weeksBack) {
        val start = today.minusWeeks(weeksBack).let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
        val list = mutableListOf<List<LocalDate>>()
        var col = start
        while (!col.isAfter(today)) {
            list.add((0..6).map { col.plusDays(it.toLong()) })
            col = col.plusWeeks(1)
        }
        list
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.2f
    val manuscriptBg = if (isDark) Color(0xFF0B2822) else Color(0xFFFAF7EF)
    val manuscriptBorder = if (isDark) Color(0xFF235448) else Color(0xFFD6C7A1)
    val fastingBorder = if (isDark) Color(0xFF529467) else Color(0xFF4D785B)
    val fastingDot = if (isDark) Color(0xFF7ECA96) else Color(0xFF35533F)
    val fastingWash = if (isDark) Color(0x3362A478) else Color(0x243E5A47)

    val empty = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.secondary

    fun cellColor(date: LocalDate, isFast: Boolean, level: Int): Color {
        if (date.isAfter(today)) return Color.Transparent
        return when (level) {
            0 -> if (isFast) fastingWash else empty
            1 -> primary.copy(alpha = 0.30f)
            2 -> primary.copy(alpha = 0.50f)
            3 -> primary.copy(alpha = 0.75f)
            else -> primary
        }
    }

    val scroll = rememberScrollState()
    LaunchedEffect(weeks.size) { scroll.scrollTo(scroll.maxValue) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = manuscriptBg,
        border = BorderStroke(1.5.dp, manuscriptBorder),
    ) {
        Column(Modifier.padding(Spacing.sm)) {
            Row(Modifier.horizontalScroll(scroll)) {
                weeks.forEach { week ->
                    Column {
                        week.forEach { date ->
                            val tappable = onDayTap != null && !date.isAfter(today)
                            val isFast = FastingCalendar.isWeeklyFastDay(date) || FastingCalendar.fastOn(date) != null
                            val level = if (date.isAfter(today)) -1 else HabitsRepository.level(HabitsRepository.dayCount(records, date))
                            val shape = RoundedCornerShape(2.5.dp)

                            Box(
                                modifier = Modifier
                                    .padding(gap)
                                    .size(cell)
                                    .clip(shape)
                                    .background(cellColor(date, isFast, level.coerceAtLeast(0)))
                                    .then(if (tappable) Modifier.clickable { onDayTap!!(date) } else Modifier)
                                    .drawBehind {
                                        if (isFast && !date.isAfter(today)) {
                                            val dashPx = 2.4.dp.toPx()
                                            val gapPx = 1.6.dp.toPx()
                                            drawRoundRect(
                                                color = fastingBorder,
                                                size = size,
                                                cornerRadius = CornerRadius(2.dp.toPx()),
                                                style = Stroke(
                                                    width = 1.1.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx)),
                                                ),
                                            )
                                            val dotColor = if (level > 0) Color.White.copy(alpha = 0.92f) else fastingDot
                                            drawCircle(
                                                color = dotColor,
                                                radius = 1.3.dp.toPx(),
                                                center = Offset(size.width / 2f, size.height / 2f),
                                            )
                                        }
                                    },
                            )
                        }
                    }
                }
            }
            if (showLegend) {
                Spacer(Modifier.height(Spacing.xs))
                val s = LocalStrings.current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(s.less, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf(0, 1, 2, 3, 4).forEach { level ->
                        val c = heatColor(level, empty, primary)
                        Box(Modifier.size(cell).clip(RoundedCornerShape(2.dp)).background(c))
                    }
                    Text(s.more, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(Spacing.xs))
                    Box(
                        Modifier
                            .size(cell)
                            .clip(RoundedCornerShape(2.dp))
                            .background(fastingWash)
                            .drawBehind {
                                drawRoundRect(
                                    color = fastingBorder,
                                    size = size,
                                    cornerRadius = CornerRadius(2.dp.toPx()),
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 1.5.dp.toPx())),
                                    ),
                                )
                                drawCircle(
                                    color = fastingDot,
                                    radius = 1.2.dp.toPx(),
                                    center = Offset(size.width / 2f, size.height / 2f),
                                )
                            }
                    )
                    Text("● የጾም ቀን", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * The last [weeks] weeks as a single drawing on the "● የብራና ሰሌዳ · Sacred Manuscript Folio",
 * sized by the height it is given.
 *
 * Drawn in a single pass: the manuscript parchment plate backing with double-fillet
 * framing and debossed tray, fasting days rendered with distinct perforated/dashed
 * borders and sacred center dots (●).
 */
@Composable
fun CompactHeatmap(
    records: Map<String, Set<String>>,
    today: LocalDate,
    modifier: Modifier = Modifier,
    weeks: Int = 10,
    maxCell: Dp = 10.dp,
) {
    data class CompactDay(val date: LocalDate, val level: Int, val isFast: Boolean)

    // -1 is a day that has not happened yet: the current week runs to Sunday.
    val gridDays = remember(records, today, weeks) {
        val monday = today.minusWeeks((weeks - 1).toLong())
            .let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
        List(weeks) { w ->
            List(7) { d ->
                val date = monday.plusWeeks(w.toLong()).plusDays(d.toLong())
                val isFast = FastingCalendar.isWeeklyFastDay(date) || FastingCalendar.fastOn(date) != null
                val level = if (date.isAfter(today)) -1
                else HabitsRepository.level(HabitsRepository.dayCount(records, date))
                CompactDay(date, level, isFast)
            }
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.2f
    val manuscriptBg = if (isDark) Color(0xFF0B2822) else Color(0xFFFAF7EF)
    val manuscriptBorder = if (isDark) Color(0xFF235448) else Color(0xFFD6C7A1)
    val fastingBorder = if (isDark) Color(0xFF529467) else Color(0xFF4D785B)
    val fastingDot = if (isDark) Color(0xFF7ECA96) else Color(0xFF35533F)
    val fastingWash = if (isDark) Color(0x3362A478) else Color(0x243E5A47)

    val empty = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.secondary
    val density = LocalDensity.current
    val maxCellPx = with(density) { maxCell.toPx() }

    // Height calculation: seven rows and six gaps + plate padding
    fun cellFor(height: Float): Float {
        val available = (height - 8f).coerceAtLeast(10f)
        return (available / 8f).coerceAtMost(maxCellPx).coerceAtLeast(1f)
    }

    val s = LocalStrings.current
    val totalPrayedInWindow = remember(gridDays) {
        gridDays.flatten().count { it.level > 0 }
    }
    val a11ySummary = remember(totalPrayedInWindow, weeks, s) {
        if (s.isAmharic) {
            "ባለፉት ${com.agpeya.app.ui.reading.geezNumeral(weeks)} ሳምንታት ውስጥ ${com.agpeya.app.ui.reading.geezNumeral(totalPrayedInWindow)} ቀን ተጸልየዋል"
        } else {
            "$totalPrayedInWindow days prayed in the last $weeks weeks"
        }
    }

    Spacer(
        modifier
            .fillMaxHeight()
            .semantics { contentDescription = a11ySummary }
            .layout { measurable, constraints ->
                val height = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
                val cell = cellFor(height.toFloat())
                val gap = cell / 6f
                val platePad = (cell * 0.4f).coerceAtLeast(3f)
                val gridW = cell * weeks + gap * (weeks - 1)
                val width = (gridW + platePad * 2f).toInt()
                val placeable = measurable.measure(Constraints.fixed(width, height))
                layout(width, height) { placeable.place(0, 0) }
            }
            .drawBehind {
                val cell = cellFor(size.height)
                val gap = cell / 6f
                val gridW = cell * weeks + gap * (weeks - 1)
                val gridH = cell * 7 + gap * 6
                val platePad = (cell * 0.4f).coerceAtLeast(3f)

                val plateLeft = (size.width - gridW) / 2f - platePad
                val plateTop = (size.height - gridH) / 2f - platePad
                val plateW = gridW + platePad * 2f
                val plateH = gridH + platePad * 2f
                val plateCorner = CornerRadius(platePad * 1.5f)

                // Manuscript Folio Plate Base
                drawRoundRect(
                    color = manuscriptBg,
                    topLeft = Offset(plateLeft, plateTop),
                    size = Size(plateW, plateH),
                    cornerRadius = plateCorner,
                )
                // Manuscript Gold Fillet Border
                drawRoundRect(
                    color = manuscriptBorder,
                    topLeft = Offset(plateLeft + 0.5f, plateTop + 0.5f),
                    size = Size(plateW - 1f, plateH - 1f),
                    cornerRadius = plateCorner,
                    style = Stroke(width = 1.1f.coerceAtLeast(1f)),
                )

                // Grid Cells
                val startX = (size.width - gridW) / 2f
                val startY = (size.height - gridH) / 2f
                val cellCorner = CornerRadius(cell * 0.22f)

                gridDays.forEachIndexed { w, week ->
                    week.forEachIndexed { d, day ->
                        if (day.level < 0) return@forEachIndexed
                        val cellLeft = startX + w * (cell + gap)
                        val cellTop = startY + d * (cell + gap)
                        val cellOffset = Offset(cellLeft, cellTop)
                        val cellSize = Size(cell, cell)

                        val isToday = day.date == today

                        if (day.isFast) {
                            // Fasting background fill
                            val fill = if (day.level == 0) fastingWash else heatColor(day.level, empty, primary)
                            drawRoundRect(
                                color = fill,
                                topLeft = cellOffset,
                                size = cellSize,
                                cornerRadius = cellCorner,
                            )
                            // Fasting perforated / dashed border
                            val dash = (cell * 0.28f).coerceAtLeast(2f)
                            val dashGap = (cell * 0.18f).coerceAtLeast(1.5f)
                            drawRoundRect(
                                color = fastingBorder,
                                topLeft = cellOffset,
                                size = cellSize,
                                cornerRadius = cellCorner,
                                style = Stroke(
                                    width = (cell * 0.11f).coerceIn(1f, 1.4f),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dashGap)),
                                ),
                            )
                            // Fasting Sacred Center Dot (●) only when cell has optical clearance
                            if (cell >= with(density) { 12.dp.toPx() }) {
                                val dotRadius = (cell * 0.13f).coerceAtLeast(1.1f)
                                val dotColor = if (day.level > 0) Color.White.copy(alpha = 0.92f) else fastingDot
                                drawCircle(
                                    color = dotColor,
                                    radius = dotRadius,
                                    center = Offset(cellLeft + cell / 2f, cellTop + cell / 2f),
                                )
                            }
                        } else {
                            // Normal day rounded rect fill
                            drawRoundRect(
                                color = heatColor(day.level, empty, primary),
                                topLeft = cellOffset,
                                size = cellSize,
                                cornerRadius = cellCorner,
                            )
                        }

                        // Holy day of full devotion highlight (level 3+)
                        if (day.level >= 3) {
                            val cx = cellLeft + cell / 2f
                            val cy = cellTop + cell / 2f
                            val arm = (cell * 0.18f).coerceAtLeast(1.1f)
                            val stroke = (cell * 0.10f).coerceAtLeast(0.8f)
                            val markColor = Color.White.copy(alpha = 0.92f)
                            drawLine(markColor, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = stroke)
                            drawLine(markColor, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = stroke)
                        }

                        // Today distinct beacon ring
                        if (isToday) {
                            drawRoundRect(
                                color = primary,
                                topLeft = Offset(cellLeft - 0.5f, cellTop - 0.5f),
                                size = Size(cell + 1f, cell + 1f),
                                cornerRadius = CornerRadius(cellCorner.x + 0.5f),
                                style = Stroke(width = (cell * 0.15f).coerceIn(1.1f, 1.8f)),
                            )
                        }
                    }
                }
            },
    )
}

/** One ladder of colour for both grids, so a day looks the same on either. */
internal fun heatColor(level: Int, empty: Color, primary: Color): Color = when (level) {
    0 -> empty
    1 -> primary.copy(alpha = 0.30f)
    2 -> primary.copy(alpha = 0.50f)
    3 -> primary.copy(alpha = 0.75f)
    else -> primary
}
