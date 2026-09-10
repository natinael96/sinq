package com.agpeya.app.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.ui.strings.LocalStrings
import java.time.LocalDate
import com.agpeya.app.ui.theme.Spacing

/** GitHub-style contribution grid: 7 rows (Mon–Sun) × [weeksBack] weeks. */
@Composable
fun HabitHeatmap(
    records: Map<String, Set<String>>,
    today: LocalDate,
    modifier: Modifier = Modifier,
    weeksBack: Long = 52,
    showLegend: Boolean = true,
    cell: Dp = 13.dp,
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

    val empty = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.secondary
    fun cellColor(date: LocalDate): Color {
        if (date.isAfter(today)) return Color.Transparent
        return heatColor(HabitsRepository.level(HabitsRepository.dayCount(records, date)), empty, primary)
    }

    val scroll = rememberScrollState()
    LaunchedEffect(weeks.size) { scroll.scrollTo(scroll.maxValue) }

    Column(modifier) {
        Row(Modifier.horizontalScroll(scroll)) {
            weeks.forEach { week ->
                Column {
                    week.forEach { date ->
                        val tappable = onDayTap != null && !date.isAfter(today)
                        Cell(
                            color = cellColor(date),
                            size = cell,
                            gap = gap,
                            onTap = if (tappable) ({ onDayTap!!(date) }) else null,
                        )
                    }
                }
            }
        }
        if (showLegend) {
            Spacer(Modifier.height(Spacing.sm))
            val s = LocalStrings.current
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(s.less, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(0, 1, 2, 3, 4).forEach { level ->
                    val c = heatColor(level, empty, primary)
                    Cell(c, cell, gap)
                }
                Text(s.more, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Cell(color: Color, size: Dp, gap: Dp, onTap: (() -> Unit)? = null) {
    Spacer(
        Modifier
            .padding(gap)
            .size(size)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
            .then(if (onTap != null) Modifier.clickable(onClick = onTap) else Modifier),
    )
}

/**
 * The last [weeks] weeks as a single drawing, sized by the height it is given.
 *
 * ቤት's ዛሬ row grows with the screen (see
 * [com.agpeya.app.ui.common.FillColumn]), and the grid is the one thing on that
 * row with any use for the height: on a tall phone the squares come up from
 * 6dp to [maxCell] and the ten weeks become legible instead of a smudge. The
 * full [HabitHeatmap] cannot do this — its square size is fixed when it is
 * composed, and asking the layout for the height first would mean
 * subcomposition, which a growing block may not contain.
 *
 * So it is drawn: no cell is a layout node, the width follows from the height,
 * and the whole grid costs one draw pass and nothing to measure.
 */
@Composable
fun CompactHeatmap(
    records: Map<String, Set<String>>,
    today: LocalDate,
    modifier: Modifier = Modifier,
    weeks: Int = 10,
    maxCell: Dp = 10.dp,
) {
    // -1 is a day that has not happened yet: the current week runs to Sunday.
    val levels = remember(records, today, weeks) {
        val monday = today.minusWeeks((weeks - 1).toLong())
            .let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
        List(weeks) { w ->
            List(7) { d ->
                val date = monday.plusWeeks(w.toLong()).plusDays(d.toLong())
                if (date.isAfter(today)) -1
                else HabitsRepository.level(HabitsRepository.dayCount(records, date))
            }
        }
    }
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val primary = MaterialTheme.colorScheme.secondary
    val density = LocalDensity.current
    val maxCellPx = with(density) { maxCell.toPx() }

    // A square and the gap after it come to 8/7 of the square, so seven rows
    // and six gaps make eight squares' worth of height: cell = height / 8.
    fun cellFor(height: Float) = (height / 8f).coerceAtMost(maxCellPx).coerceAtLeast(1f)

    Spacer(
        modifier
            .fillMaxHeight()
            .layout { measurable, constraints ->
                val height = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
                val cell = cellFor(height.toFloat())
                val width = (cell * weeks + cell / 6f * (weeks - 1)).toInt()
                val placeable = measurable.measure(Constraints.fixed(width, height))
                layout(width, height) { placeable.place(0, 0) }
            }
            .drawBehind {
                val cell = cellFor(size.height)
                val gap = cell / 6f
                val top = (size.height - (cell * 7 + gap * 6)) / 2f
                val corner = CornerRadius(cell * 0.22f)
                levels.forEachIndexed { w, week ->
                    week.forEachIndexed { d, level ->
                        if (level < 0) return@forEachIndexed
                        drawRoundRect(
                            color = heatColor(level, empty, primary),
                            topLeft = Offset(w * (cell + gap), top + d * (cell + gap)),
                            size = Size(cell, cell),
                            cornerRadius = corner,
                        )
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
