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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Share
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
import com.agpeya.app.ui.common.HeroCard
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.sinqColors
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import kotlinx.coroutines.flow.first
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
    // Prayer hours group under a collapsible ጸሎት header; other habits are flat.
    // Hours hidden in Manage Hours are already filtered out by visibleHours.
    val hourItems = remember(hours) { hours.map { HabitsRepository.hourHabitId(it.id) to it.name } }
    val habitItems = remember(state, s) {
        HabitsRepository.orderedHabitIds(state, includeHidden = false).map { it to habitName(it, state, s) }
    }
    var prayersExpanded by remember { mutableStateOf(false) }
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
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(s.streaksTitle, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    androidx.compose.material3.IconButton(onClick = {
                        scope.launch {
                            val pName = com.agpeya.app.data.SettingsRepository.profileName(context).first()
                            val cName = com.agpeya.app.data.SettingsRepository.christianName(context).first()
                            StreakShare.share(
                                context = context,
                                records = state.records,
                                today = today,
                                name = cName.ifBlank { pName },
                                maxPossible = hourItems.size + habitItems.size,
                                s = s,
                            )
                        }
                    }) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.Share,
                            contentDescription = s.shareAction,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.lg))
                // The one hero on this screen. Restrained on purpose: a number
                // and what it counts, no confetti — the point is consistency in
                // prayer, not a score.
                HeroCard(
                    glow = overall > 0,
                    contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.xxl),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.currentStreakLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = sinqColors.onHeroMuted,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            s.daysUnit(overall),
                            style = MaterialTheme.typography.titleLarge,
                            color = sinqColors.onHero,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xxl))
                SectionHeader(s.todayLabel)
                Spacer(Modifier.height(Spacing.xs))
            }

            item {
                // ጸሎት group header: expand/collapse the per-hour rows.
                val doneHours = hourItems.count { it.first in (state.records[todayKey] ?: emptySet()) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { prayersExpanded = !prayersExpanded }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (prayersExpanded) Icons.Filled.KeyboardArrowDown
                        else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = s.habitPrayer,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "$doneHours/${hourItems.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (doneHours > 0) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (prayersExpanded) {
                items(hourItems.size, key = { hourItems[it].first }) { i ->
                    val (id, name) = hourItems[i]
                    CheckRow(
                        name = name,
                        done = id in (state.records[todayKey] ?: emptySet()),
                        streak = HabitsRepository.currentStreak(state.records, id, today),
                        indented = true,
                        onToggle = { scope.launch { HabitsRepository.toggle(context, todayKey, id) } },
                    )
                }
            }

            items(habitItems.size, key = { habitItems[it].first }) { i ->
                val (id, name) = habitItems[i]
                CheckRow(
                    name = name,
                    done = id in (state.records[todayKey] ?: emptySet()),
                    streak = HabitsRepository.currentStreak(state.records, id, today),
                    indented = false,
                    onToggle = { scope.launch { HabitsRepository.toggle(context, todayKey, id) } },
                )
            }

            item {
                Spacer(Modifier.height(28.dp))
                EthiopianYearHeatmap(
                    records = state.records,
                    today = today,
                    maxPossible = hourItems.size + habitItems.size,
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
                Spacer(Modifier.height(Spacing.xxl))
                SectionHeader(s.habitsHeader)
                Spacer(Modifier.height(Spacing.xs))
            }

            item {
                StatRow(
                    name = s.habitPrayer,
                    now = HabitsRepository.prayerCurrentStreak(state.records, today),
                    best = HabitsRepository.prayerLongestStreak(state.records),
                    indented = false,
                    s = s,
                )
            }
            if (prayersExpanded) {
                items(hourItems.size, key = { "stat_${hourItems[it].first}" }) { i ->
                    val (id, name) = hourItems[i]
                    StatRow(
                        name = name,
                        now = HabitsRepository.currentStreak(state.records, id, today),
                        best = HabitsRepository.longestStreak(state.records, id),
                        indented = true,
                        s = s,
                    )
                }
            }
            items(habitItems.size, key = { "stat_${habitItems[it].first}" }) { i ->
                val (id, name) = habitItems[i]
                StatRow(
                    name = name,
                    now = HabitsRepository.currentStreak(state.records, id, today),
                    best = HabitsRepository.longestStreak(state.records, id),
                    indented = false,
                    s = s,
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                NavRow(s.manageHabits, onClick = onManageHabits)
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

@Composable
private fun CheckRow(
    name: String,
    done: Boolean,
    streak: Int,
    indented: Boolean,
    onToggle: () -> Unit,
) {
    val motion = LocalMotion.current
    val haptics = LocalHapticFeedback.current
    val tint by animateColorAsState(
        targetValue = if (done) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motion.spec(Motion.standard),
        label = "checkTint",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .toggleable(
                value = done,
                role = Role.Checkbox,
                onValueChange = {
                    // A kept habit is worth a tick you can feel; it replaces the
                    // toast that would otherwise interrupt the page.
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle()
                },
            )
            .padding(vertical = Spacing.md)
            .padding(start = if (indented) 40.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(IconSize.medium),
        )
        Spacer(Modifier.width(Spacing.lg))
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

@Composable
private fun StatRow(name: String, now: Int, best: Int, indented: Boolean, s: Strings) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .padding(start = if (indented) 40.dp else 0.dp),
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
