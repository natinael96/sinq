package com.agpeya.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.data.HoursRepository
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.model.HabitsState
import com.agpeya.app.model.Hour
import com.agpeya.app.model.HoursConfig
import com.agpeya.app.ui.common.AgpeyaBottomBar
import com.agpeya.app.ui.common.Tab
import com.agpeya.app.ui.habits.HabitHeatmap
import com.agpeya.app.ui.habits.habitName
import com.agpeya.app.ui.strings.LocalStrings
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeScreen(
    onOpenHour: (String) -> Unit,
    onOpenModes: () -> Unit,
    onSelectTab: (Tab) -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val config by HoursRepository.config(context).collectAsState(initial = HoursConfig())
    val builtIn by produceState<List<Hour>>(initialValue = emptyList()) {
        value = ContentRepository.hours(context)
    }
    val hours = remember(builtIn, config) { HoursRepository.merge(builtIn, config, includeHidden = false) }
    val recentIds by UserDataRepository.recents(context).collectAsState(initial = emptyList())
    val suggestedId = remember { ContentRepository.suggestedHourId(LocalTime.now().hour) }
    val suggested = hours.find { it.id == suggestedId }
    val recents = recentIds.mapNotNull { id -> hours.find { it.id == id } }

    val habitState by HabitsRepository.state(context).collectAsState(initial = HabitsState())
    val today = remember { LocalDate.now() }
    val habitIds = HabitsRepository.orderedHabitIds(habitState, includeHidden = false)
    val doneToday = habitState.records[today.toString()] ?: emptySet()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AgpeyaBottomBar(current = Tab.HOME, onSelect = onSelectTab) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("ስንቅ", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            text = com.agpeya.app.ui.common.formatEthiopian(today, s),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenModes) {
                        Icon(Icons.Outlined.Notifications, contentDescription = s.reminderModes, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (suggested != null) {
                item {
                    NowCard(hour = suggested, onClick = { onOpenHour(suggested.id) })
                    Spacer(Modifier.height(24.dp))
                }
            }

            item {
                Text(s.todayLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(10.dp))
                TodayCard(
                    habitIds = habitIds,
                    doneToday = doneToday,
                    habitState = habitState,
                    records = habitState.records,
                    today = today,
                    onClick = { onSelectTab(Tab.STREAK) },
                )
                Spacer(Modifier.height(24.dp))
            }

            if (recents.isNotEmpty()) {
                item {
                    Text(s.continueReading, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recents, key = { it.id }) { hour ->
                            RecentChip(hour = hour, onClick = { onOpenHour(hour.id) })
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            item {
                Text(s.hoursHeader, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
            }
            items(hours, key = { it.id }) { hour ->
                HourRow(hour = hour, onClick = { onOpenHour(hour.id) })
                if (hour.id != hours.lastOrNull()?.id) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun NowCard(hour: Hour, onClick: () -> Unit) {
    val s = LocalStrings.current
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Box(Modifier.fillMaxWidth()) {
            // Soft gold glow, clipped by the card shape.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 28.dp, y = (-28).dp)
                    .size(130.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.34f), Color.Transparent),
                        ),
                        CircleShape,
                    ),
            )
            Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                Text(
                    s.nowPrayer,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(6.dp))
                Text(hour.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                if (hour.timeHint.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(hour.timeHint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.66f))
                }
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
            )
        }
    }
}

@Composable
private fun TodayCard(
    habitIds: List<String>,
    doneToday: Set<String>,
    habitState: HabitsState,
    records: Map<String, Set<String>>,
    today: LocalDate,
    onClick: () -> Unit,
) {
    val s = LocalStrings.current
    val doneCount = habitIds.count { it in doneToday }
    val overall = HabitsRepository.overallCurrentStreak(records, today)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${s.todayLabel}  $doneCount/${habitIds.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(s.daysUnit(overall), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }
            if (habitIds.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    habitIds.take(4).forEach { id ->
                        val done = id in doneToday
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (done) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                habitName(id, habitState, s),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (done) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(12.dp))
            HabitHeatmap(
                records = records,
                today = today,
                weeksBack = 14,
                showLegend = false,
                cell = 9.dp,
                gap = 2.dp,
            )
        }
    }
}

@Composable
private fun RecentChip(hour: Hour, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = hour.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun HourRow(hour: Hour, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = hour.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(text = hour.timeHint, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
