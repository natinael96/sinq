package com.agpeya.app.ui.fasting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.FastingCalendar
import com.agpeya.app.ui.common.EthiopianDate
import com.agpeya.app.ui.common.formatEthiopianWithGregorian
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import java.time.LocalDate

/**
 * አጽዋማት — the fasts of the church year: what's in effect today, then every fast
 * of the current Ethiopian year with its dates and length.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val today = remember { LocalDate.now() }
    val ethYear = remember(today) { EthiopianDate.from(today).year }
    val fasts = remember(ethYear) {
        runCatching { FastingCalendar.fastsOf(ethYear) }.getOrDefault(emptyList())
    }
    val activeFast = remember(today) { FastingCalendar.fastOn(today) }
    val weeklyFast = remember(today) { FastingCalendar.isWeeklyFastDay(today) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.fastingTitle, style = MaterialTheme.typography.titleLarge)
                        Text(
                            formatEthiopianWithGregorian(today, s),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 22.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                TodayCard(activeFast, weeklyFast, s)
                Spacer(Modifier.height(22.dp))
                Text(
                    s.fastingYearHeader(ethYear),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(8.dp))
            }
            items(fasts.size, key = { fasts[it].key }) { i ->
                FastRow(fasts[i], today, s)
            }
            item {
                Spacer(Modifier.height(18.dp))
                Text(
                    s.fastingWeeklyNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

/** What's in effect right now — the first thing anyone opens this screen for. */
@Composable
private fun TodayCard(
    fast: FastingCalendar.Fast?,
    weeklyFast: Boolean,
    s: Strings,
) {
    val fasting = fast != null || weeklyFast
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (fasting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (fasting) null else BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                s.fastingToday,
                style = MaterialTheme.typography.labelMedium,
                color = if (fasting) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = fast?.nameAm ?: if (weeklyFast) s.fastingWeekly else s.fastingNone,
                style = MaterialTheme.typography.titleLarge,
                color = if (fasting) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onBackground,
            )
            if (fast != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = s.fastingDayOf(fast.dayNumber(LocalDate.now()), fast.days),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/** Which day of the fast [date] is (1-based). */
private fun FastingCalendar.Fast.dayNumber(date: LocalDate): Int =
    (date.toEpochDay() - start.toEpochDay()).toInt() + 1

@Composable
private fun FastRow(fast: FastingCalendar.Fast, today: LocalDate, s: Strings) {
    val active = fast.contains(today)
    val past = fast.end.isBefore(today)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(8.dp)
                .height(8.dp)
                .clip(CircleShape)
                .background(
                    when {
                        active -> MaterialTheme.colorScheme.secondary
                        past -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                fast.nameAm,
                style = MaterialTheme.typography.titleMedium,
                color = if (past) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "${formatEthiopianShortSafe(fast.start, s)} – ${formatEthiopianShortSafe(fast.end, s)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = s.fastingDays(fast.days),
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

private fun formatEthiopianShortSafe(date: LocalDate, s: Strings): String =
    com.agpeya.app.ui.common.formatEthiopianShort(date, s)
