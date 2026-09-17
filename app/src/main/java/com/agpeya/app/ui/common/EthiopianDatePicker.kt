package com.agpeya.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import java.time.LocalDate

/** A weekday-aligned Ethiopian month, with a distinct current-day outline. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EthiopianDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val s = LocalStrings.current
    val start = remember(initial) { EthiopianDate.from(initial) }
    var year by rememberSaveable(initial) { mutableIntStateOf(start.year) }
    var month by rememberSaveable(initial) { mutableIntStateOf(start.month) }
    var day by rememberSaveable(initial) { mutableIntStateOf(start.day) }
    var monthsOpen by remember { mutableStateOf(false) }
    var yearsOpen by remember { mutableStateOf(false) }
    val today = EthiopianDate.from(LocalDate.now())
    val selectedDay = day.coerceAtMost(monthLength(year, month))
    val cells = remember(year, month) { ethiopianMonthCells(year, month) }
    val yearScroll = rememberScrollState()
    val density = LocalDensity.current
    val firstYear = (year - 100).coerceAtLeast(1)
    LaunchedEffect(yearsOpen) {
        if (yearsOpen) yearScroll.scrollTo(with(density) { ((year - firstYear) * 48).dp.roundToPx() })
    }
    fun showMonth(y: Int, m: Int) {
        year = y
        month = m
        day = day.coerceAtMost(monthLength(y, m))
    }
    fun moveMonth(delta: Int) {
        val target = shiftedEthiopianMonth(year, month, delta)
        showMonth(target.first, target.second)
    }
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val screenHeight = with(density) { windowHeight.toDp() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(8.dp).widthIn(max = 420.dp).fillMaxWidth()
                .heightIn(max = screenHeight - 32.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { moveMonth(-1) }, enabled = year > 1 || month > 1) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = s.previousMonth)
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            TextButton(onClick = { monthsOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(s.ethMonths[month - 1], style = MaterialTheme.typography.titleSmall)
                            }
                            DropdownMenu(expanded = monthsOpen, onDismissRequest = { monthsOpen = false }) {
                                s.ethMonths.forEachIndexed { index, name ->
                                    DropdownMenuItem(text = { Text(name) }, onClick = {
                                        showMonth(year, index + 1); monthsOpen = false
                                    })
                                }
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            TextButton(onClick = { yearsOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("${geezNumeral(year)} ${s.eraSuffix}", style = MaterialTheme.typography.titleSmall)
                            }
                            DropdownMenu(expanded = yearsOpen, onDismissRequest = { yearsOpen = false },
                                scrollState = yearScroll, modifier = Modifier.heightIn(max = 280.dp)) {
                                (firstYear..(year + 20).coerceAtMost(9999)).forEach { candidate ->
                                    DropdownMenuItem(text = { Text("${geezNumeral(candidate)} ${s.eraSuffix}") },
                                        modifier = Modifier.heightIn(min = 48.dp), onClick = {
                                            showMonth(candidate, month); yearsOpen = false
                                        })
                                }
                            }
                        }
                    }
                    IconButton(onClick = { moveMonth(1) }, enabled = year < 9999 || month < 13) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = s.nextMonth)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    // Keep seven true weekday columns, including at large system text sizes.
                    val gridWidth = maxOf(maxWidth, 40.dp * density.fontScale.coerceAtLeast(1f) * 7)
                    Column(Modifier.horizontalScroll(rememberScrollState())) {
                        Column(Modifier.width(gridWidth).selectableGroup()) {
                            Row(Modifier.fillMaxWidth()) {
                                s.weekdayNames.forEach { weekday ->
                                    Text(weekday, modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                                        textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            cells.chunked(7).forEach { week ->
                                Row(Modifier.fillMaxWidth()) {
                                    week.forEach { n ->
                                        if (n == null) {
                                            Spacer(Modifier.weight(1f).height(48.dp))
                                        } else {
                                            val selected = n == selectedDay
                                            val isToday = year == today.year && month == today.month && n == today.day
                                            val shape = RoundedCornerShape(8.dp)
                                            val date = EthiopianDate(year, month, n).toGregorian()
                                            Box(Modifier.weight(1f).heightIn(min = 48.dp).padding(2.dp)
                                                .clip(shape)
                                                .background(if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                                .then(if (isToday && !selected) Modifier.border(1.dp,
                                                    MaterialTheme.colorScheme.secondary, shape) else Modifier)
                                                .selectable(selected = selected, role = Role.RadioButton, onClick = { day = n })
                                                .semantics {
                                                    contentDescription = formatEthiopian(date, s) + if (isToday) " · ${s.todayLabel}" else ""
                                                }.padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center) {
                                                Text(geezNumeral(n), style = MaterialTheme.typography.bodyMedium,
                                                    color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { year = today.year; month = today.month; day = today.day }) { Text(s.todayLabel) }
                    Row {
                        TextButton(onClick = onDismiss) { Text(s.cancel) }
                        Button(onClick = { onSelect(EthiopianDate(year, month, selectedDay).toGregorian()) }) { Text(s.ok) }
                    }
                }
            }
        }
    }
}

/** Monday-first slots; padding is deliberately not selectable adjacent-month dates. */
internal fun ethiopianMonthCells(year: Int, month: Int): List<Int?> {
    val offset = EthiopianDate(year, month, 1).toGregorian().dayOfWeek.value - 1
    val length = monthLength(year, month)
    val total = ((offset + length + 6) / 7) * 7
    return List(total) { index -> (index - offset + 1).takeIf { it in 1..length } }
}

internal fun shiftedEthiopianMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    val index = year * 13 + month - 1 + delta
    return Math.floorDiv(index, 13) to Math.floorMod(index, 13) + 1
}

/** Pagume has a sixth day in Ethiopian years whose remainder modulo four is three. */
internal fun monthLength(year: Int, month: Int): Int = when {
    month != 13 -> 30
    year % 4 == 3 -> 6
    else -> 5
}
