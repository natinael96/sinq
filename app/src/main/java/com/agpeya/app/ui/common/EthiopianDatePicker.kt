package com.agpeya.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import java.time.LocalDate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

/**
 * A date picker in the calendar the rest of the app is written in.
 *
 * ግጻዌ is dated ጳጉሜን ፪, its readings are cited in Ge'ez numerals, and its
 * "change day" button opened a Gregorian grid of Arabic digits. Someone looking
 * for መስከረም ፩ had to know it falls on 11 September, which is precisely the
 * arithmetic the app exists to do for them.
 *
 * Thirteen months: twelve of thirty days and ጳጉሜን of five, or six before a
 * leap year. A day that does not exist in the month chosen is clamped rather
 * than refused — moving from ጥር ፴ to ጳጉሜን lands on its last day.
 */
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
    val length = monthLength(year, month)
    if (day > length) day = length

    val monthState = rememberLazyListState()
    LaunchedEffect(month) { monthState.animateScrollToItem((month - 2).coerceAtLeast(0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { year -= 1 }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = s.previousDay,
                    )
                }
                Text(
                    "${geezNumeral(year)} ${s.eraSuffix}",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { year += 1 }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = s.nextDay,
                    )
                }
            }
        },
        text = {
            Column {
                LazyRow(
                    state = monthState,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    items(13) { i ->
                        val n = i + 1
                        SelectPill(
                            label = s.ethMonths.getOrElse(i) { geezNumeral(n) },
                            selected = n == month,
                            onClick = { month = n },
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    contentPadding = PaddingValues(vertical = Spacing.xxs),
                ) {
                    items((1..length).toList()) { n ->
                        val selected = n == day
                        Box(
                            modifier = Modifier
                                .padding(Spacing.xxs)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.secondary
                                    else androidx.compose.ui.graphics.Color.Transparent,
                                )
                                .clickable { day = n },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                geezNumeral(n),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) MaterialTheme.colorScheme.onSecondary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSelect(EthiopianDate(year, month, day.coerceAtMost(monthLength(year, month))).toGregorian())
            }) { Text(s.ok) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

/**
 * Days in an Ethiopian month: thirty, except ጳጉሜን, which has five — six in the
 * year before a Gregorian leap year, which is the year whose EC number leaves
 * three when divided by four.
 */
internal fun monthLength(year: Int, month: Int): Int = when {
    month != 13 -> 30
    year % 4 == 3 -> 6
    else -> 5
}
