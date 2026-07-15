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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.ui.strings.LocalStrings
import java.time.LocalDate

/** GitHub-style contribution grid: 7 rows (Mon–Sun) × [weeksBack] weeks. */
@Composable
fun HabitHeatmap(
    records: Map<String, Set<String>>,
    today: LocalDate,
    maxPossible: Int,
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
        return when (HabitsRepository.level(HabitsRepository.dayCount(records, date), maxPossible)) {
            0 -> empty
            1 -> primary.copy(alpha = 0.30f)
            2 -> primary.copy(alpha = 0.50f)
            3 -> primary.copy(alpha = 0.75f)
            else -> primary
        }
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
            Spacer(Modifier.height(10.dp))
            val s = LocalStrings.current
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(s.less, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(0, 1, 2, 3, 4).forEach { level ->
                    val c = when (level) {
                        0 -> empty
                        1 -> primary.copy(alpha = 0.30f)
                        2 -> primary.copy(alpha = 0.50f)
                        3 -> primary.copy(alpha = 0.75f)
                        else -> primary
                    }
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
