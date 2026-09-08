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
import androidx.compose.ui.text.style.TextOverflow
import com.agpeya.app.ui.common.SinqDivider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Check
import com.agpeya.app.ui.settings.scheduleSummary

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
fun JourneyScreen(
    onSelectTab: (Tab) -> Unit,
    onOpenJournal: () -> Unit,
) {
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
    // Only what today asks for. A habit kept on Wednesday and Friday is not a
    // thing missed on a Tuesday, so it is not on Tuesday's list at all.
    val habitItems = remember(state, s, today) {
        HabitsRepository.dueHabitIds(state, today).map { it to habitName(it, state, s) }
    }
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
                // The mark, not a page title: the tab beneath is already
                // labelled ጉዞ, so the name of the app is what belongs here —
                // and without it the page read as content jammed to the top.
                com.agpeya.app.ui.common.SinqWordmark()
                Spacer(Modifier.height(Spacing.xs))
                //
                // The one hero on this screen: today's candle and the period's
                // count of days prayed. Restrained on purpose — the point is a
                // life of prayer, not a score, and the wording stays true
                // however many days were missed.
                HeroCard(
                    glow = summary.prayedToday,
                    contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.sm),
                ) {
                    Column(Modifier.weight(1f)) {
                        // Which hours, not just that some hour was prayed —
                        // the app knows their names and the reader knows what
                        // is still owed the day.
                        val prayedNames = hourItems
                            .filter { it.first in (state.records[todayKey] ?: emptySet()) }
                            .joinToString("፣ ") { it.second }
                        Text(
                            when {
                                summary.returning -> s.welcomeBack
                                summary.prayedToday && prayedNames.isNotEmpty() ->
                                    "${s.journeyTodayLit}  ·  $prayedNames"
                                summary.prayedToday -> s.journeyTodayLit
                                else -> s.journeyTodayUnlit
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = sinqColors.onHeroMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                Spacer(Modifier.height(Spacing.sm))
                val doneHours = hourItems.count { it.first in (state.records[todayKey] ?: emptySet()) }
                val (keptHabits, dueHabits) = HabitsRepository.keptOfDue(state, today)
                SectionHeader(s.todayLabel) {
                    Text(
                        "$doneHours/${hourItems.size}  ·  $keptHabits/$dueHabits",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Spacing.xxs))
            }

            // The seven hours as two hairline strips — four names to a line,
            // colour the only state. They were behind a ጸሎት row that opened a
            // list of seven 48 dp check rows, which is 336 dp to say what two
            // 28 dp lines say, and the page could not show a day's prayer and
            // a day's habits at the same time.
            item(key = "hours") {
                HourStrips(
                    items = hourItems,
                    done = state.records[todayKey] ?: emptySet(),
                    onToggle = { id -> scope.launch { HabitsRepository.toggle(context, todayKey, id) } },
                )
            }

            items(habitItems.size, key = { habitItems[it].first }) { i ->
                val (id, name) = habitItems[i]
                HabitRow(
                    name = name,
                    done = id in (state.records[todayKey] ?: emptySet()),
                    detail = habitDetail(state, id, monthStart, today, s),
                    onToggle = { scope.launch { HabitsRepository.toggle(context, todayKey, id) } },
                )
            }
            item(key = "habits_end") { SinqDivider() }

            // The year heatmap is the historical view — the story is density
            // and return across the Church's year, not any one unbroken run.
            item {
                Spacer(Modifier.height(Spacing.sm))
                SectionHeader(s.yearJourneyHeader) {
                    EthiopianYearSwitcher(displayedEcYear, today) { year ->
                        displayedEcYear = year
                        selectedEpochDay = null
                    }
                }
                Spacer(Modifier.height(Spacing.xxs))
                EthiopianYearHeatmap(
                    records = state.records,
                    today = today,
                    ecYear = displayedEcYear,
                    selectedDay = selectedDay,
                    modifier = Modifier.fillMaxWidth(),
                    onYearChange = { year ->
                        displayedEcYear = year
                        selectedEpochDay = null
                    },
                    onDaySelect = { selectedEpochDay = it.toEpochDay() },
                )
            }

            item {
                Spacer(Modifier.height(Spacing.xs))
                // The journal sits with ጉዞ because both are the day looked back
                // on — but it is never counted or scored alongside the habits.
                NavRow(s.journalTitle, onClick = onOpenJournal)
                Spacer(Modifier.height(Spacing.md))
            }
        }
    }
}

/**
 * The seven hours, four to a line, on hairlines.
 *
 * Colour is the whole of the state: a prayed hour is gold, an unprayed one
 * muted. There is no box to tick because there is no room for one at this
 * size, and none is needed — the names are the only thing on the line.
 *
 * A 28 dp line is under the 48 dp tap floor, so each name carries an invisible
 * target taller than the text it sits on.
 */
@Composable
private fun HourStrips(
    items: List<Pair<String, String>>,
    done: Set<String>,
    onToggle: (String) -> Unit,
) {
    val motion = LocalMotion.current
    val haptics = LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth()) {
        SinqDivider()
        items.chunked(4).forEach { line ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                line.forEach { (id, name) ->
                    val kept = id in done
                    val tint by animateColorAsState(
                        targetValue = if (kept) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = motion.spec(Motion.standard),
                        label = "hourTint",
                    )
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .toggleable(
                                value = kept,
                                role = Role.Checkbox,
                                onValueChange = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggle(id)
                                },
                            )
                            .padding(vertical = Spacing.sm, horizontal = Spacing.xxs),
                    )
                }
                // A short last line keeps its columns rather than spreading.
                repeat(4 - line.size) { Spacer(Modifier.weight(1f)) }
            }
            SinqDivider()
        }
    }
}

/**
 * One habit: a small hollow ring that fills with a check when it is kept, the
 * name, and how many days of the last thirty it has been kept.
 *
 * The ring is 20 dp and the row 44 dp, because the list has to take a habit
 * more without pushing the year off the page — a tile grid or a row of large
 * rings does not scale past the four that ship with the app. The whole row is
 * the target, so the small ring is a mark and not a hit area.
 */
@Composable
private fun HabitRow(
    name: String,
    done: Boolean,
    detail: String,
    onToggle: () -> Unit,
) {
    val motion = LocalMotion.current
    val haptics = LocalHapticFeedback.current
    val gold = MaterialTheme.colorScheme.secondary
    val tint by animateColorAsState(
        targetValue = if (done) gold else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motion.spec(Motion.standard),
        label = "habitTint",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clip(MaterialTheme.shapes.small)
            .toggleable(
                value = done,
                role = Role.Checkbox,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle()
                },
            )
            .padding(vertical = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .then(
                    if (done) Modifier.background(gold)
                    else Modifier.border(2.dp, gold, CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = tint,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * What a habit's row says on its right: days kept of the last thirty, in Ge'ez
 * numerals — and for a habit kept on a cadence, days kept of the days it was
 * actually asked for, with the cadence named. Four Sundays out of four is a
 * kept rule; four days out of thirty is not the same fact.
 */
@Composable
private fun habitDetail(
    state: HabitsState,
    habitId: String,
    monthStart: LocalDate,
    today: LocalDate,
    s: Strings,
): String {
    val kept = HabitsRepository.habitDaysBetween(state.records, habitId, monthStart, today)
    val schedule = state.schedules[habitId]
        ?: return s.keptOf(kept, daysBetweenInclusive(monthStart, today))
    val due = generateSequence(monthStart) { it.plusDays(1) }
        .takeWhile { !it.isAfter(today) }
        .count { schedule.isDueOn(it) }
    return "${s.keptOf(kept, due)}  ·  ${scheduleSummary(schedule, s, LocalContext.current)}"
}

private fun daysBetweenInclusive(start: LocalDate, end: LocalDate): Int =
    (end.toEpochDay() - start.toEpochDay()).toInt() + 1

