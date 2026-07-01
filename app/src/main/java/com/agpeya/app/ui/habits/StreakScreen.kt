package com.agpeya.app.ui.habits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.model.HabitsState
import com.agpeya.app.ui.common.AgpeyaBottomBar
import com.agpeya.app.ui.common.Tab
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun builtInName(id: String, s: Strings): String = when (id) {
    "prayer" -> s.habitPrayer
    "church" -> s.habitChurch
    "prostrate" -> s.habitProstrate
    "bible" -> s.habitBible
    else -> id
}

fun habitName(id: String, state: HabitsState, s: Strings): String =
    state.names[id] ?: state.custom.find { it.id == id }?.name ?: builtInName(id, s)

@Composable
fun StreakScreen(onSelectTab: (Tab) -> Unit, onManageHabits: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val state by HabitsRepository.state(context).collectAsState(initial = HabitsState())
    val today = remember { LocalDate.now() }
    val todayKey = today.toString()
    val hours by androidx.compose.runtime.produceState(emptyList<com.agpeya.app.model.Hour>()) {
        value = com.agpeya.app.data.HoursRepository.visibleHours(context)
    }
    // Each prayer hour tracks separately, followed by the other habits.
    val trackables = remember(hours, state, s) {
        hours.map { HabitsRepository.hourHabitId(it.id) to it.name } +
            HabitsRepository.orderedHabitIds(state, includeHidden = false).map { it to habitName(it, state, s) }
    }
    val overall = HabitsRepository.overallCurrentStreak(state.records, today)
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AgpeyaBottomBar(current = Tab.STREAK, onSelect = onSelectTab) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            item {
                Text(s.streaksTitle, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Column(Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
                        Text(
                            s.currentStreakLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            s.daysUnit(overall),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(s.todayLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
            }

            items(trackables.size, key = { trackables[it].first }) { i ->
                val (id, name) = trackables[i]
                val done = id in (state.records[todayKey] ?: emptySet())
                val streak = HabitsRepository.currentStreak(state.records, id, today)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.launch { HabitsRepository.toggle(context, todayKey, id) } }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (done) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    if (streak > 0) {
                        Text(
                            text = streak.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(28.dp))
                EthiopianYearHeatmap(
                    records = state.records,
                    today = today,
                    modifier = Modifier.fillMaxWidth(),
                    onDayTap = { selectedDay = it },
                )
                selectedDay?.let { day ->
                    val n = HabitsRepository.dayCount(state.records, day)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "${com.agpeya.app.ui.common.formatEthiopian(day, s)} · ${s.habitsCount(n)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(Modifier.height(28.dp))
                Text(s.habitsHeader, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
            }

            items(trackables.size, key = { "stat_${trackables[it].first}" }) { i ->
                val (id, name) = trackables[i]
                val now = HabitsRepository.currentStreak(state.records, id, today)
                val best = HabitsRepository.longestStreak(state.records, id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        text = "${s.streakCurrent} $now · ${s.streakBest} $best",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onManageHabits)
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s.manageHabits, style = MaterialTheme.typography.titleMedium)
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
