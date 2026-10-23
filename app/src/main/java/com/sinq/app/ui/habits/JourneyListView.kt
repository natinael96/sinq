package com.sinq.app.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinq.app.data.FastingCalendar
import com.sinq.app.data.HabitsRepository
import com.sinq.app.ui.common.EthiopianDatePickerDialog
import com.sinq.app.ui.common.EthiopianDate
import com.sinq.app.ui.common.formatEthiopian
import com.sinq.app.ui.reading.geezNumeral
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.Spacing
import java.time.LocalDate

/**
 * Accessible non-visual alternative to the EthiopianYearHeatmap.
 *
 * Displays weeks as scrollable, screen-reader friendly rows with identical
 * underlying data (prayer levels, fasting indicators, today markers, and journal markers).
 */
@Composable
fun JourneyListView(
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

    val start = remember(ecYear) { EthiopianDate(ecYear, 1, 1).toGregorian() }
    val end = remember(ecYear) { EthiopianDate(ecYear + 1, 1, 1).toGregorian().minusDays(1) }
    val selectableRange = remember(ecYear, today, earliest) { journeyYearSelectableRange(ecYear, today, earliest) }

    // Week rows covering [start..end]
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

    val fasts = remember(ecYear) {
        (ecYear - 1..ecYear + 1).flatMap {
            runCatching { FastingCalendar.fastsOf(it) }.getOrDefault(emptyList())
        }
    }
    fun inFast(date: LocalDate): Boolean = fasts.any { it.contains(date) } || FastingCalendar.isWeeklyFastDay(date)

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.2f
    val sinq = com.sinq.app.ui.theme.sinqColors
    val manuscriptBg = sinq.manuscriptBg
    val manuscriptBorder = sinq.manuscriptBorder
    val goldFillet = sinq.goldFillet
    val fastingBorder = sinq.fastingBorder
    val fastingDot = sinq.fastingDot

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = manuscriptBg,
        border = BorderStroke(1.5.dp, manuscriptBorder),
        shadowElevation = if (isDark) 4.dp else 2.dp,
    ) {
        Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            // Header with year switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(text = "፨", style = MaterialTheme.typography.titleMedium, color = goldFillet)
                    Text(
                        text = "${s.yearJourneyHeader} (${s.journeyViewList})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

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

            // Scrollable list of weeks
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                weeks.forEachIndexed { weekIdx, week ->
                    val weekDaysInRange = week.filter { !it.isBefore(start) && !it.isAfter(end) }
                    if (weekDaysInRange.isNotEmpty()) {
                        val firstDate = weekDaysInRange.first()
                        val lastDate = weekDaysInRange.last()
                        val weekPrays = weekDaysInRange.count { HabitsRepository.dayCount(records, it) > 0 }
                        val weekFasts = weekDaysInRange.count { inFast(it) }
                        val hasToday = weekDaysInRange.any { it == today }

                        var expanded by remember { mutableStateOf(hasToday) }

                        val weekDesc = buildString {
                            append(formatEthiopian(firstDate, s))
                            append(" - ")
                            append(formatEthiopian(lastDate, s))
                            append("፣ ")
                            if (s.isAmharic) {
                                append("${geezNumeral(weekPrays)} የተጸለዩ ቀናት፣ ${geezNumeral(weekFasts)} የጾም ቀናት")
                            } else {
                                append("$weekPrays days prayed, $weekFasts fasting days")
                            }
                            if (hasToday) append(" (ዛሬ ያለበት)")
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (hasToday) goldFillet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, if (hasToday) goldFillet else manuscriptBorder.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics(mergeDescendants = true) {
                                    contentDescription = weekDesc
                                    role = Role.Button
                                }
                                .clickable { expanded = !expanded },
                        ) {
                            Column(Modifier.padding(Spacing.sm)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = "${formatEthiopian(firstDate, s)} – ${formatEthiopian(lastDate, s)}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = if (s.isAmharic)
                                                "${geezNumeral(weekPrays)} ቀናት ጸሎት · ${geezNumeral(weekFasts)} ቀናት ጾም"
                                            else
                                                "$weekPrays days prayed · $weekFasts fasting days",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Icon(
                                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                        contentDescription = null,
                                        tint = goldFillet,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }

                                AnimatedVisibility(visible = expanded) {
                                    Column(
                                        modifier = Modifier.padding(top = Spacing.xs),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        weekDaysInRange.forEach { date ->
                                            val inRange = selectableRange?.contains(date) == true
                                            val count = HabitsRepository.dayCount(records, date)
                                            val level = HabitsRepository.level(count)
                                            val isFast = inFast(date)
                                            val isToday = date == today
                                            val isSelected = date == selectedDay

                                            val dayDesc = buildString {
                                                append(formatEthiopian(date, s))
                                                append("፡ ")
                                                if (count > 0) {
                                                    append(if (s.isAmharic) "${geezNumeral(count)} ሰዓታት ተጸልይዋል" else "$count hours prayed")
                                                } else {
                                                    append(if (s.isAmharic) "ያልተጸለየ" else "No prayer recorded")
                                                }
                                                if (isFast) append(if (s.isAmharic) "፣ የጾም ቀን" else ", Fasting day")
                                                if (isToday) append(if (s.isAmharic) "፣ ዛሬ" else ", Today")
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isSelected) goldFillet.copy(alpha = 0.25f)
                                                        else if (isToday) goldFillet.copy(alpha = 0.1f)
                                                        else Color.Transparent
                                                    )
                                                    .semantics {
                                                        contentDescription = dayDesc
                                                        role = Role.Button
                                                    }
                                                    .clickable(enabled = inRange) { onDaySelect(date) }
                                                    .padding(horizontal = Spacing.xs, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                                ) {
                                                    // Status dot/indicator
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (count > 0) MaterialTheme.colorScheme.secondary
                                                                else MaterialTheme.colorScheme.outlineVariant
                                                            )
                                                            .then(
                                                                if (isFast) Modifier.border(1.dp, fastingBorder, CircleShape)
                                                                else Modifier
                                                            )
                                                    )
                                                    Text(
                                                        text = formatEthiopian(date, s),
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                                ) {
                                                    if (isFast) {
                                                        Text(
                                                            text = s.fastingDayLabel,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            color = fastingDot,
                                                            fontWeight = FontWeight.SemiBold,
                                                        )
                                                    }
                                                    if (count > 0) {
                                                        Text(
                                                            text = if (s.isAmharic) "${geezNumeral(count)} ሰዓት" else "$count hrs",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            color = goldFillet,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
