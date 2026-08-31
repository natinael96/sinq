package com.agpeya.app.ui.habits

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import com.agpeya.app.ui.common.rememberCurrentDate
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun builtInName(id: String, s: Strings): String = when (id) {
    "prayer" -> s.habitPrayer
    "sinksar" -> s.habitSynaxarium
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
    val today by rememberCurrentDate()
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
    var prayersExpanded by rememberSaveable { mutableStateOf(false) }
    val summary = remember(state, today) { PrayerJourney.summarize(state.records, today) }
    // Per-habit summaries always count the Ethiopian month, even during a fast:
    // the hero speaks the period's language, the private records stay steady.
    val monthStart = remember(today) {
        EthiopianDate.from(today).let { EthiopianDate(it.year, it.month, 1).toGregorian() }
    }
    val currentEcYear = remember(today) { EthiopianDate.from(today).year }
    var displayedEcYear by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(currentEcYear) }
    var selectedEpochDay by rememberSaveable { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    val selectedDay = selectedEpochDay?.let(LocalDate::ofEpochDay)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AgpeyaBottomBar(current = Tab.JOURNEY, onSelect = onSelectTab) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.xs),
        ) {
            item {
                Text(s.journeyTitle, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(Spacing.sm))
                // The one hero on this screen: today's candle and the period's
                // count of days prayed. Restrained on purpose — the point is a
                // life of prayer, not a score, and the wording stays true
                // however many days were missed.
                HeroCard(
                    glow = summary.prayedToday,
                    contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.sm),
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
                        Spacer(Modifier.height(Spacing.xxs))
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
                        modifier = Modifier.size(width = 26.dp, height = 44.dp),
                    )
                }
                Spacer(Modifier.height(Spacing.md))
                SectionHeader(s.todayLabel)
                Spacer(Modifier.height(Spacing.xs))
            }

            item {
                // ጸሎት group header: expand/collapse the per-hour rows.
                val doneHours = hourItems.count { it.first in (state.records[todayKey] ?: emptySet()) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            stateDescription = if (prayersExpanded) s.expandedState else s.collapsedState
                        }
                        .clickable(
                            onClickLabel = if (prayersExpanded) s.collapse else s.expand,
                            role = Role.Button,
                        ) { prayersExpanded = !prayersExpanded }
                        .heightIn(min = 48.dp)
                        .padding(vertical = Spacing.xs),
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
                        text = "$doneHours/${hourItems.size} · " + s.daysThisMonth(
                            HabitsRepository.prayerDaysBetween(state.records, monthStart, today),
                        ),
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
                        detail = s.daysThisMonth(
                            HabitsRepository.habitDaysBetween(state.records, id, monthStart, today),
                        ),
                        indented = true,
                        onToggle = {
                            scope.launch {
                                HabitsRepository.toggle(context, todayKey, id)
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
                    detail = s.daysThisMonth(
                        HabitsRepository.habitDaysBetween(state.records, id, monthStart, today),
                    ),
                    indented = false,
                    onToggle = { scope.launch { HabitsRepository.toggle(context, todayKey, id) } },
                )
            }

            // The year heatmap is the historical view — the story is density
            // and return across the Church's year, not any one unbroken run.
            item {
                Spacer(Modifier.height(Spacing.md))
                SectionHeader(s.yearJourneyHeader)
                Spacer(Modifier.height(Spacing.xs))
                EthiopianYearHeatmap(
                    records = state.records,
                    today = today,
                    ecYear = displayedEcYear,
                    selectedDay = selectedDay,
                    maxPossible = hourItems.size + habitItems.size,
                    modifier = Modifier.fillMaxWidth(),
                    onYearChange = { year ->
                        displayedEcYear = year
                        selectedEpochDay = null
                    },
                    onDaySelect = { selectedEpochDay = it.toEpochDay() },
                )
            }

            item {
                Spacer(Modifier.height(Spacing.md))
                NavRow(s.manageHabits, onClick = onManageHabits)
                Spacer(Modifier.height(Spacing.md))
            }
        }
    }
}

@Composable
private fun CheckRow(
    name: String,
    done: Boolean,
    detail: String,
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
            .padding(vertical = Spacing.xs)
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
            maxLines = 1,
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = detail,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
