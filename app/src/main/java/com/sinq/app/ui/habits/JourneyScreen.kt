package com.sinq.app.ui.habits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.sinq.app.ui.common.PrayerDisplay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Surface
import com.sinq.app.ui.common.formatEthiopian
import com.sinq.app.model.JournalEntry
import androidx.compose.runtime.produceState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.ui.text.font.FontWeight
import com.sinq.app.data.FastingCalendar
import com.sinq.app.data.JournalRepository
import com.sinq.app.data.SettingsRepository
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import com.sinq.app.data.HabitsRepository
import com.sinq.app.data.PrayerJourney
import com.sinq.app.model.HabitsState
import com.sinq.app.ui.common.Candle
import com.sinq.app.ui.common.EthiopianDate
import com.sinq.app.ui.common.Tab
import com.sinq.app.ui.common.HeroCard
import com.sinq.app.ui.common.SectionHeader
import com.sinq.app.ui.theme.IconSize
import com.sinq.app.ui.theme.LocalMotion
import com.sinq.app.ui.theme.Motion
import com.sinq.app.ui.theme.Spacing
import com.sinq.app.ui.theme.sinqColors
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import com.sinq.app.ui.common.rememberCurrentDate
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.strings.Strings
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.ui.text.style.TextOverflow
import com.sinq.app.ui.common.SinqDivider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.sinq.app.ui.settings.scheduleSummary

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
        val name = when (s.languageMode) {
            com.sinq.app.data.Language.ENGLISH -> fast.nameEn
            com.sinq.app.data.Language.AMHARIC -> fast.nameAm
            com.sinq.app.data.Language.AMH_ENG -> "${fast.nameAm} (${fast.nameEn})"
        }
        s.journeyFastLine(name, summary.fastDay, summary.daysPrayed)
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
    onOpenJournal: () -> Unit,
    onOpenEntry: (String) -> Unit = {},
    onOpenManageHabits: () -> Unit = {},
    onOpenHour: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val state by HabitsRepository.state(context).collectAsState(initial = HabitsState())
    val today by rememberCurrentDate()
    val todayKey = today.toString()
    val hours by androidx.compose.runtime.produceState(emptyList<com.sinq.app.model.Hour>()) {
        value = com.sinq.app.data.HoursRepository.visibleHours(context)
    }
    // Prayer hours group under a collapsible ጸሎት header; other habits are flat.
    // Hours hidden in Manage Hours are already filtered out by visibleHours.
    val hourItems = remember(hours, s) {
        hours.map { hour ->
            val displayName = PrayerDisplay.hourName(
                hourId = hour.id,
                rawName = hour.name,
                transliteration = hour.transliteration,
                mode = s.languageMode,
                compact = true,
            )
            Triple(HabitsRepository.hourHabitId(hour.id), hour.id, displayName)
        }
    }
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
    val journeyViewMode by SettingsRepository.journeyViewMode(context).collectAsState(initial = com.sinq.app.data.JourneyViewMode.GRID)

    val listState = rememberLazyListState()
    val motion = LocalMotion.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // The bar belongs to the host that holds all four tabs, and it carries
        // its own navigation-bar padding; this page only insets for the status bar.
        contentWindowInsets = WindowInsets.statusBars,
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.End,
        floatingActionButton = {
            androidx.compose.animation.AnimatedVisibility(
                visible = selectedEpochDay == null,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = onOpenJournal,
                    icon = { Icon(Icons.Outlined.EditNote, contentDescription = null) },
                    text = { Text(s.journalTitle) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 10.dp,
                    ),
                    modifier = Modifier.semantics { contentDescription = s.journalTitle },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, top = Spacing.xs, bottom = 120.dp),
        ) {
            item {
                // The mark, not a page title: the tab beneath is already
                // labelled ጉዞ, so the name of the app is what belongs here —
                // and without it the page read as content jammed to the top.
                com.sinq.app.ui.common.SinqWordmark()
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
                        val separator = if (s.languageMode == com.sinq.app.data.Language.AMHARIC) "፣ " else ", "
                        val prayedNames = hourItems
                            .filter { it.first in (state.records[todayKey] ?: emptySet()) }
                            .joinToString(separator) { it.third }
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
                Spacer(Modifier.height(Spacing.xs))
                val doneHours = hourItems.count { it.first in (state.records[todayKey] ?: emptySet()) }
                val (keptHabits, dueHabits) = HabitsRepository.keptOfDue(state, today)
                SectionHeader(s.todayLabel) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(
                            "$doneHours/${hourItems.size}  ·  $keptHabits/$dueHabits",
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = "${s.hoursHeader}: $doneHours/${hourItems.size} · ${s.habitsHeader}: $keptHabits/$dueHabits"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Surface(
                            onClick = onOpenManageHabits,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f)),
                        ) {
                            Text(
                                text = s.manage,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 3.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xxs))
            }

            // The seven hours as two hairline strips — four names to a line,
            // colour the only state. They were behind a ጸሎት row that opened a
            // list of seven 48 dp check rows, which is 336 dp to say what two
            // 28 dp lines say, and the page could not show a day's prayer and
            // a day's habits at the same time.
            // The seven hours as a single horizontal scroll row.
            item(key = "hours") {
                HourHorizontalList(
                    items = hourItems,
                    done = state.records[todayKey] ?: emptySet(),
                    onToggle = { id -> scope.launch { HabitsRepository.toggle(context, todayKey, id) } },
                    onOpenHour = onOpenHour,
                )
            }

            items(habitItems.size, key = { habitItems[it].first }) { i ->
                val (id, name) = habitItems[i]
                HabitRow(
                    name = name,
                    done = id in (state.records[todayKey] ?: emptySet()),
                    detail = habitDetail(state, id, monthStart, today, s),
                    s = s,
                    onToggle = { scope.launch { HabitsRepository.toggle(context, todayKey, id) } },
                )
            }
            item(key = "habits_end") { SinqDivider() }

            // The year heatmap is the historical view — the story is density
            // and return across the Church's year, not any one unbroken run.
            item {
                Spacer(Modifier.height(Spacing.md))
                val earliestYear = remember(state.records) {
                    state.records.keys.mapNotNull { runCatching { EthiopianDate.from(LocalDate.parse(it)).year }.getOrNull() }.minOrNull()?.coerceAtMost(APP_EPOCH_EC.year) ?: APP_EPOCH_EC.year
                }
                if (journeyViewMode == com.sinq.app.data.JourneyViewMode.LIST) {
                    JourneyListView(
                        records = state.records,
                        today = today,
                        ecYear = displayedEcYear,
                        selectedDay = selectedDay,
                        earliestYear = earliestYear,
                        modifier = Modifier.fillMaxWidth(),
                        onYearChange = { year ->
                            displayedEcYear = year
                            selectedEpochDay = null
                        },
                        onDaySelect = { day ->
                            selectedEpochDay = if (selectedEpochDay == day.toEpochDay()) null else day.toEpochDay()
                        },
                    )
                } else {
                    EthiopianYearHeatmap(
                        records = state.records,
                        today = today,
                        ecYear = displayedEcYear,
                        selectedDay = selectedDay,
                        earliestYear = earliestYear,
                        modifier = Modifier.fillMaxWidth(),
                        onYearChange = { year ->
                            displayedEcYear = year
                            selectedEpochDay = null
                        },
                        onDaySelect = { day ->
                            selectedEpochDay = if (selectedEpochDay == day.toEpochDay()) null else day.toEpochDay()
                        },
                    )
                }
                val selectableRange = remember(displayedEcYear, today) {
                    val earliest = state.records.keys.mapNotNull { runCatching { EthiopianDate.from(LocalDate.parse(it)).toGregorian() }.getOrNull() }.minOrNull() ?: APP_EPOCH_EC.toGregorian()
                    journeyYearSelectableRange(displayedEcYear, today, earliest)
                }
                if (selectedDay != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    DayInspectionCard(
                        date = selectedDay,
                        records = state.records,
                        hourItems = hourItems,
                        state = state,
                        s = s,
                        onClose = { selectedEpochDay = null },
                        onPreviousDay = selectableRange?.let { range ->
                            if (selectedDay.isAfter(range.start)) {
                                { selectedEpochDay = selectedDay.minusDays(1).toEpochDay() }
                            } else null
                        },
                        onNextDay = selectableRange?.let { range ->
                            if (selectedDay.isBefore(range.endInclusive)) {
                                { selectedEpochDay = selectedDay.plusDays(1).toEpochDay() }
                            } else null
                        },
                        onOpenEntry = onOpenEntry,
                        onOpenHour = onOpenHour,
                    )
                }
            }
        }
    }
}

/**
 * The canonical prayer hours arranged as a single-row horizontal scroll.
 * Each hour is presented in a compact, non-wrapping chip/card with smooth scrolling.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HourHorizontalList(
    items: List<Triple<String, String, String>>,
    done: Set<String>,
    onToggle: (String) -> Unit,
    onOpenHour: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val haptics = LocalHapticFeedback.current
    val s = LocalStrings.current
    val listState = rememberLazyListState()
    val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }
    val canScrollBackward by remember { derivedStateOf { listState.canScrollBackward } }
    val bgColor = MaterialTheme.colorScheme.background

    Column(modifier.fillMaxWidth()) {
        SinqDivider()
        Spacer(Modifier.height(Spacing.xs))
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = PaddingValues(start = Spacing.xxs, end = Spacing.md, top = Spacing.xxs, bottom = Spacing.xxs),
            ) {
                items(items, key = { it.first }) { (id, rawHourId, name) ->
                    val kept = id in done
                    val tint by animateColorAsState(
                        targetValue = if (kept) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = motion.spec(Motion.standard),
                        label = "hourTint",
                    )
                    Row(
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .clip(MaterialTheme.shapes.small)
                            .semantics {
                                contentDescription = "$name: ${if (kept) s.doneLabel else (if (s.isAmharic) "ያልተጸለየ" else "Not prayed")}"
                            }
                            .combinedClickable(
                                role = Role.Checkbox,
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggle(id)
                                },
                                onLongClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpenHour(rawHourId)
                                },
                                onLongClickLabel = s.prayHour,
                            )
                            .padding(horizontal = Spacing.xs, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
                    ) {
                        if (kept) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (kept) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            color = tint,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }

            // Left fading edge shadow with indicator when scrolled
            val leftAlpha by animateFloatAsState(
                targetValue = if (canScrollBackward) 1f else 0f,
                animationSpec = motion.spec(Motion.standard),
                label = "scrollLeftIndicator",
            )
            if (leftAlpha > 0.001f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .matchParentSize()
                        .graphicsLayer { alpha = leftAlpha },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(42.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        bgColor,
                                        bgColor.copy(alpha = 0.85f),
                                        bgColor.copy(alpha = 0.4f),
                                        Color.Transparent,
                                    )
                                )
                            ),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .size(14.dp),
                        )
                    }
                }
            }

            // Right fading edge shadow with indicator when more items exist
            val rightAlpha by animateFloatAsState(
                targetValue = if (canScrollForward) 1f else 0f,
                animationSpec = motion.spec(Motion.standard),
                label = "scrollRightIndicator",
            )
            if (rightAlpha > 0.001f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .matchParentSize()
                        .graphicsLayer { alpha = rightAlpha },
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(42.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        bgColor.copy(alpha = 0.4f),
                                        bgColor.copy(alpha = 0.85f),
                                        bgColor,
                                    )
                                )
                            ),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(14.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        SinqDivider()
    }
}

/**
 * Inspection card shown below the heatmap when a day is selected.
 * Displays the exact canonical hours and habits kept on that day,
 * along with a button to view any journal entry written on that date.
 */
@Composable
private fun DayInspectionCard(
    date: LocalDate,
    records: Map<String, Set<String>>,
    hourItems: List<Triple<String, String, String>>,
    state: HabitsState,
    s: Strings,
    onClose: () -> Unit,
    onPreviousDay: (() -> Unit)?,
    onNextDay: (() -> Unit)?,
    onOpenEntry: (String) -> Unit,
    onOpenHour: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dayKey = date.toString()
    val dayRecords = records[dayKey] ?: emptySet()
    val keptHours = remember(dayRecords, hourItems) {
        hourItems.filter { it.first in dayRecords }
    }
    val keptOtherHabits = remember(dayRecords, state, s) {
        dayRecords.filter { !it.startsWith("hour_") }.map { habitName(it, state, s) }
    }
    val fastName = remember(date, s.languageMode) {
        PrayerDisplay.fastName(date, s.languageMode)
    }
    val journalEntries by produceState<List<JournalEntry>>(emptyList(), date) {
        JournalRepository.onDate(context, date).collect { value = it }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.2f
    val manuscriptCardBg = if (isDark) Color(0xFF0B2822) else Color(0xFFFAF7EF)
    val manuscriptBorder = if (isDark) Color(0xFF235448) else Color(0xFFD6C7A1)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = manuscriptCardBg,
        border = BorderStroke(1.5.dp, manuscriptBorder),
        shadowElevation = if (isDark) 4.dp else 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            // Header: Date info + Nav chevrons + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatEthiopian(date, s),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    if (fastName != null) {
                        Text(
                            text = "● $fastName · ${s.fastingDayLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF7ECA96) else Color(0xFF35533F),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        text = s.hoursPrayedCount(keptHours.size, hourItems.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = { onPreviousDay?.invoke() },
                    enabled = onPreviousDay != null,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = s.previousDay,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = { onNextDay?.invoke() },
                    enabled = onNextDay != null,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = s.nextDay,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = s.cancel,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Kept Hours & Habits List
            if (keptHours.isEmpty() && keptOtherHabits.isEmpty()) {
                Text(
                    text = s.noRecordOnDay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.xxs),
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    keptHours.forEach { (_, rawHourId, name) ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                            modifier = Modifier.clickable { onOpenHour(rawHourId) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                    keptOtherHabits.forEach { name ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            // Journal Entry Button if exists
            val entry = journalEntries.firstOrNull()
            if (entry != null) {
                OutlinedButton(
                    onClick = { onOpenEntry(entry.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary,
                    ),
                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.xxs),
                ) {
                    Icon(
                        Icons.Outlined.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.small),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = s.viewDayJournalEntry,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

/**
 * One habit: a small hollow ring that fills with a check when it is kept, the
 * name, and how many days of the last thirty it has been kept.
 *
 * The ring is a mark rather than a hit area; the whole row meets the 48 dp
 * platform target floor.
 */
@Composable
private fun HabitRow(
    name: String,
    done: Boolean,
    detail: String,
    s: Strings,
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
    val statusText = if (done) (if (s.isAmharic) "የተፈጸመ" else "Completed") else (if (s.isAmharic) "ያልተፈጸመ" else "Not completed")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .semantics {
                contentDescription = "$name: $statusText. $detail"
            }
            .toggleable(
                value = done,
                role = Role.Checkbox,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle()
                },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .then(
                    if (done) Modifier.background(gold)
                    else Modifier.border(1.5.dp, gold, CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
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
