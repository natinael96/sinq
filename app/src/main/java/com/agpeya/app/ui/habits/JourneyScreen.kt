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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.agpeya.app.data.FastingCalendar
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.data.PrayerJourney
import com.agpeya.app.model.HabitsState
import com.agpeya.app.ui.common.AgpeyaBottomBar
import com.agpeya.app.ui.common.Candle
import com.agpeya.app.ui.common.EthiopianDate
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
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

/**
 * The one sentence Home and Journey both lead with: distinct days prayed in
 * the current period. During a fast the period is the fast (named in Amharic —
 * fast names are content, like psalm text); otherwise the Ethiopian month.
 */
fun journeyLine(summary: PrayerJourney.Summary, s: Strings): String {
    val fast = summary.fast
    return if (fast != null && summary.fastDay != null) {
        s.journeyFastLine(fast.nameAm, summary.fastDay, summary.daysPrayed)
    } else {
        s.journeyMonthLine(summary.daysPrayed)
    }
}

/**
 * ጉዞ — where have I walked?
 *
 * The hero holds today's candle and the period's prayer-day count; the year
 * heatmap below is the historical view. Nothing on this screen is consecutive,
 * so nothing here can "break": a returning user sees the same screen as a
 * faithful one, with today's candle waiting.
 */
@Composable
fun JourneyScreen(onSelectTab: (Tab) -> Unit, onManageHabits: () -> Unit) {
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
    val summary = remember(state, today) { PrayerJourney.summarize(state.records, today) }
    // Per-habit summaries always count the Ethiopian month, even during a fast:
    // the hero speaks the period's language, the private records stay steady.
    val monthStart = remember(today) {
        EthiopianDate.from(today).let { EthiopianDate(it.year, it.month, 1).toGregorian() }
    }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AgpeyaBottomBar(current = Tab.JOURNEY, onSelect = onSelectTab) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            item {
                Text(s.journeyTitle, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(Spacing.lg))
                // The one hero on this screen: today's candle and the period's
                // count of days prayed. Restrained on purpose — the point is a
                // life of prayer, not a score, and the wording stays true
                // however many days were missed.
                HeroCard(
                    glow = summary.prayedToday,
                    contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.xxl),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                summary.returning -> s.welcomeBack
                                summary.prayedToday -> s.journeyTodayLit
                                else -> s.journeyTodayUnlit
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = sinqColors.onHeroMuted,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            journeyLine(summary, s),
                            style = MaterialTheme.typography.titleLarge,
                            color = sinqColors.onHero,
                        )
                    }
                    Spacer(Modifier.width(Spacing.lg))
                    Candle(
                        lit = summary.prayedToday,
                        contentDescription = if (summary.prayedToday) s.journeyTodayLit else s.journeyTodayUnlit,
                        bodyColor = sinqColors.onHeroMuted,
                        flameColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(width = 30.dp, height = 52.dp),
                    )
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
                        indented = true,
                        onToggle = {
                            scope.launch {
                                HabitsRepository.toggle(context, todayKey, id)
                                // Marking an hour prayed moves the የመሃል ጸሎት
                                // anchor — only when it was turned ON.
                                val nowDone = HabitsRepository.current(context)
                                    .records[todayKey]?.contains(id) == true
                                if (nowDone) {
                                    com.agpeya.app.reminders.BreathPrayerScheduler.onPrayerRecorded(context)
                                }
                            }
                        },
                    )
                }
            }

            items(habitItems.size, key = { habitItems[it].first }) { i ->
                val (id, name) = habitItems[i]
                CheckRow(
                    name = name,
                    done = id in (state.records[todayKey] ?: emptySet()),
                    indented = false,
                    onToggle = { scope.launch { HabitsRepository.toggle(context, todayKey, id) } },
                )
            }

            // The year heatmap is the historical view — the story is density
            // and return across the Church's year, not any one unbroken run.
            item {
                Spacer(Modifier.height(Spacing.xxl))
                SectionHeader(s.yearJourneyHeader)
                Spacer(Modifier.height(Spacing.md))
                EthiopianYearHeatmap(
                    records = state.records,
                    today = today,
                    maxPossible = hourItems.size + habitItems.size,
                    modifier = Modifier.fillMaxWidth(),
                    onDayTap = { selectedDay = it },
                )
                selectedDay?.let { day ->
                    val n = HabitsRepository.dayCount(state.records, day)
                    // Name the fast the tapped day fell in, if any — the day is
                    // read inside the Church's year, not a productivity grid.
                    val fastName = remember(day) {
                        runCatching { FastingCalendar.fastOn(day)?.nameAm }.getOrNull()
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = listOfNotNull(
                            com.agpeya.app.ui.common.formatEthiopian(day, s),
                            s.habitsCount(n),
                            fastName,
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(Modifier.height(Spacing.xxl))
                SectionHeader(s.habitsHeader)
                Spacer(Modifier.height(Spacing.xs))
            }

            // Private per-habit records: days kept this month. Distinct days,
            // no runs, no bests — self-knowledge rather than a scoreboard.
            item {
                StatRow(
                    name = s.habitPrayer,
                    detail = s.daysThisMonth(
                        HabitsRepository.prayerDaysBetween(state.records, monthStart, today),
                    ),
                    indented = false,
                )
            }
            if (prayersExpanded) {
                items(hourItems.size, key = { "stat_${hourItems[it].first}" }) { i ->
                    val (id, name) = hourItems[i]
                    StatRow(
                        name = name,
                        detail = s.daysThisMonth(
                            HabitsRepository.habitDaysBetween(state.records, id, monthStart, today),
                        ),
                        indented = true,
                    )
                }
            }
            items(habitItems.size, key = { "stat_${habitItems[it].first}" }) { i ->
                val (id, name) = habitItems[i]
                StatRow(
                    name = name,
                    detail = s.daysThisMonth(
                        HabitsRepository.habitDaysBetween(state.records, id, monthStart, today),
                    ),
                    indented = false,
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
    }
}

@Composable
private fun StatRow(name: String, detail: String, indented: Boolean) {
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
            text = detail,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
