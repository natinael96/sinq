package com.agpeya.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.DayReadings
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.data.HoursRepository
import com.agpeya.app.model.HabitsState
import com.agpeya.app.model.Hour
import com.agpeya.app.model.HoursConfig
import com.agpeya.app.ui.common.AgpeyaBottomBar
import com.agpeya.app.ui.common.CollapsibleHeader
import com.agpeya.app.ui.common.HeroCard
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqDivider
import com.agpeya.app.ui.common.Tab
import com.agpeya.app.ui.habits.HabitHeatmap
import com.agpeya.app.ui.habits.habitName
import com.agpeya.app.ui.common.liturgicalSeasonLabel
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.sinqColors
import java.time.LocalDate
import java.time.LocalTime

/** Synthetic id for the Home aggregate prayer dot; resolves to the ጸሎት label. */
private const val PRAYER_AGGREGATE_ID = "prayer"

/**
 * Home answers five questions, in this order, without making the reader work for
 * any of them: what day is it, what should I pray now, what is today's ግጻዌ, how
 * am I doing, and what else can I open.
 *
 * Only two things on this page are cards — the prayer for now and the day's
 * ግጻዌ. Everything else is separated by space and a header, because a screen
 * where every block is a rounded box has no hierarchy left to spend.
 */
@Composable
fun HomeScreen(
    onOpenHour: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFasting: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenPsalter: () -> Unit,
    onOpenWudase: () -> Unit,
    onOpenZewotr: () -> Unit,
    onOpenGitsawe: () -> Unit,
    onSelectTab: (Tab) -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val config by HoursRepository.config(context).collectAsState(initial = HoursConfig())
    val builtIn by produceState<List<Hour>>(initialValue = emptyList()) {
        value = ContentRepository.hours(context)
    }
    val hours = remember(builtIn, config) { HoursRepository.merge(builtIn, config, includeHidden = false) }
    val suggestedId = remember { ContentRepository.suggestedHourId(LocalTime.now().hour) }
    val suggested = hours.find { it.id == suggestedId }

    val habitState by HabitsRepository.state(context).collectAsState(initial = HabitsState())
    val today = remember { LocalDate.now() }
    // Prayer shows as one aggregate dot on Home (lit when any hour was prayed);
    // the per-hour breakdown lives on the Streak screen.
    val doneToday = habitState.records[today.toString()] ?: emptySet()
    val prayedAnyHour = doneToday.any { it.startsWith("hour_") }
    val habitIds = remember(habitState, prayedAnyHour) {
        listOf(PRAYER_AGGREGATE_ID) + HabitsRepository.orderedHabitIds(habitState, includeHidden = false)
    }
    val doneWithAggregate = if (prayedAnyHour) doneToday + PRAYER_AGGREGATE_ID else doneToday
    // The hours list is long enough to bury everything under it; collapsing keeps
    // Home scannable. Expanded by default — it's the app's primary content.
    var hoursExpanded by rememberSaveable { mutableStateOf(true) }
    val seasonLabel = remember(today, s) { liturgicalSeasonLabel(today, s) }
    val currentHourId = remember(hours) { com.agpeya.app.data.PrayerSchedule.currentHourId(hours) }
    val dayProgress by com.agpeya.app.data.PrayerProgressRepository.progress(context)
        .collectAsState(initial = com.agpeya.app.data.PrayerProgressRepository.DayProgress())
    // Section totals per hour, so a row can show "3 of 12" without loading text.
    val sectionCounts = remember(hours) { hours.associate { it.id to it.sections.size } }
    val resumeHour = remember(dayProgress, hours, sectionCounts) {
        com.agpeya.app.data.PrayerSchedule
            .resumable(dayProgress) { id -> hours.find { it.id == id }?.sections?.map { sec -> sec.id } ?: emptyList() }
            ?.let { id -> hours.find { it.id == id } }
    }
    // The day's lectionary, so the ግጻዌ card can name today's reading instead of
    // being a label with an arrow on it.
    val readings by produceState<DayReadings?>(initialValue = null, today) {
        value = runCatching { GitsaweRepository.readingsFor(context, today) }.getOrNull()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AgpeyaBottomBar(current = Tab.HOME, onSelect = onSelectTab) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen),
        ) {
            item(key = "header") {
                Spacer(Modifier.height(Spacing.xl))
                DayHeader(
                    today = today,
                    seasonLabel = seasonLabel,
                    onOpenFasting = onOpenFasting,
                    onOpenSearch = onOpenSearch,
                    onOpenBookmarks = onOpenBookmarks,
                )
                Spacer(Modifier.height(Spacing.xl))
            }

            if (suggested != null) {
                item(key = "now") {
                    NowCard(hour = suggested, onClick = { onOpenHour(suggested.id) })
                    Spacer(Modifier.height(Spacing.md))
                }
            }

            // Offered only when an hour was started today and left unfinished. A
            // slim row rather than a second card — it's a continuation, not a
            // rival to the prayer for now.
            resumeHour?.takeIf { it.id != suggested?.id }?.let { h ->
                item(key = "resume") {
                    ResumeRow(
                        hour = h,
                        done = dayProgress.done[h.id]?.size ?: 0,
                        total = sectionCounts[h.id] ?: 0,
                        onClick = { onOpenHour(h.id) },
                    )
                    Spacer(Modifier.height(Spacing.md))
                }
            }

            item(key = "gitsawe") {
                Spacer(Modifier.height(Spacing.sm))
                GitsaweCard(readings = readings, onClick = onOpenGitsawe)
                Spacer(Modifier.height(Spacing.xxl))
            }

            item(key = "today") {
                TodaySection(
                    habitIds = habitIds,
                    doneToday = doneWithAggregate,
                    habitState = habitState,
                    records = habitState.records,
                    today = today,
                    // Real trackables: visible hours + habits (aggregate dot excluded).
                    maxPossible = hours.size + (habitIds.size - 1),
                    onClick = { onSelectTab(Tab.STREAK) },
                )
                Spacer(Modifier.height(Spacing.xxl))
            }

            item(key = "shortcuts") {
                SectionHeader(s.libraryTitle)
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    QuickLink(s.psalterTitle, onOpenPsalter, Modifier.weight(1f))
                    QuickLink(s.zewotrTselot, onOpenZewotr, Modifier.weight(1f))
                    QuickLink(s.wudaseMariam, onOpenWudase, Modifier.weight(1f))
                }
                Spacer(Modifier.height(Spacing.xxl))
            }

            item(key = "hoursHeader") {
                CollapsibleHeader(
                    text = s.hoursHeader,
                    expanded = hoursExpanded,
                    badge = hours.size.toString(),
                    onToggle = { hoursExpanded = !hoursExpanded },
                )
            }
            if (hoursExpanded) {
                items(hours, key = { it.id }) { hour ->
                    HourRow(
                        hour = hour,
                        isCurrent = hour.id == currentHourId,
                        progress = sectionCounts[hour.id]?.let { total ->
                            (dayProgress.done[hour.id]?.size ?: 0) to total
                        },
                        onClick = { onOpenHour(hour.id) },
                    )
                    if (hour.id != hours.lastOrNull()?.id) SinqDivider()
                }
            }
            item { Spacer(Modifier.height(Spacing.xxl)) }
        }
    }
}

/**
 * The date block. Three lines at most: the app, the day, and — only when the day
 * actually falls in one — the liturgical season. The greeting joins the season
 * line rather than claiming a fourth.
 */
@Composable
private fun DayHeader(
    today: LocalDate,
    seasonLabel: String?,
    onOpenFasting: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val profileName by com.agpeya.app.data.SettingsRepository.profileName(context)
        .collectAsState(initial = "")
    val christianName by com.agpeya.app.data.SettingsRepository.christianName(context)
        .collectAsState(initial = "")
    val callName = christianName.ifBlank { profileName }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "ስንቅ",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = com.agpeya.app.ui.common.formatEthiopian(today, s),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = com.agpeya.app.ui.common.formatGregorianShort(today, s),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
            val accent = listOfNotNull(
                seasonLabel,
                callName.takeIf { it.isNotBlank() }?.let { s.greeting(it) },
            )
            if (accent.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = accent.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Row {
            HeaderAction(Icons.Outlined.CalendarMonth, s.fastingTitle, onOpenFasting)
            HeaderAction(Icons.Outlined.Search, s.tabSearch, onOpenSearch)
            HeaderAction(Icons.Outlined.Bookmarks, s.bookmarksTitle, onOpenBookmarks)
        }
    }
}

@Composable
private fun HeaderAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize.medium),
        )
    }
}

@Composable
private fun NowCard(hour: Hour, onClick: () -> Unit) {
    val s = LocalStrings.current
    val sinq = sinqColors
    HeroCard(onClick = onClick) {
        Column(Modifier.weight(1f)) {
            Text(s.nowPrayer, style = MaterialTheme.typography.labelMedium, color = sinq.onHeroMuted)
            Spacer(Modifier.height(Spacing.xxs))
            Text(hour.name, style = MaterialTheme.typography.titleLarge, color = sinq.onHero)
            if (hour.timeHint.isNotBlank()) {
                Text(hour.timeHint, style = MaterialTheme.typography.bodySmall, color = sinq.onHeroMuted)
            }
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = sinq.onHeroMuted,
            modifier = Modifier.size(IconSize.medium),
        )
    }
}

/**
 * Today's ግጻዌ. The card names the actual reading — and the feast, when there is
 * one — so the day's lectionary is legible from Home rather than being a door
 * you have to open to find out what's behind it.
 */
@Composable
private fun GitsaweCard(readings: DayReadings?, onClick: () -> Unit) {
    val s = LocalStrings.current
    val motion = LocalMotion.current
    val feast = readings?.feasts?.firstOrNull()?.amharicName
    val reading = readings?.daily?.title
        ?: readings?.seasonal?.firstOrNull()?.title
        ?: readings?.monthly?.firstOrNull()?.let { it.title ?: it.raw }
    SinqCard(onClick = onClick, contentPadding = PaddingValues(Spacing.lg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(IconSize.large),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    s.gitsaweKicker,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    s.gitsaweTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(IconSize.medium),
            )
        }
        // Fades in once the lectionary resolves, so the card doesn't jump.
        AnimatedVisibility(
            visible = feast != null || reading != null,
            enter = fadeIn(motion.spec(Motion.standard)) + expandVertically(motion.spec(Motion.standard)),
            exit = fadeOut(motion.spec(Motion.fast)) + shrinkVertically(motion.spec(Motion.fast)),
        ) {
            Column {
                Spacer(Modifier.height(Spacing.md))
                SinqDivider()
                Spacer(Modifier.height(Spacing.md))
                feast?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                reading?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

/**
 * Today's progress: a count, the streak, one dot per trackable, and the recent
 * history. No card — the header and the space around it do that job, and one
 * less rounded box on this screen is one more place for the eye to rest.
 */
@Composable
private fun TodaySection(
    habitIds: List<String>,
    doneToday: Set<String>,
    habitState: HabitsState,
    records: Map<String, Set<String>>,
    today: LocalDate,
    maxPossible: Int,
    onClick: () -> Unit,
) {
    val s = LocalStrings.current
    val doneCount = habitIds.count { it in doneToday }
    val overall = HabitsRepository.overallCurrentStreak(records, today)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick, onClickLabel = s.streaksTitle)
            .padding(vertical = Spacing.xs),
    ) {
        SectionHeader(s.todayLabel) {
            Text(
                text = "$doneCount/${habitIds.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(Spacing.md))
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(IconSize.small),
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                s.daysUnit(overall),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (habitIds.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                habitIds.take(4).forEach { id ->
                    HabitDot(
                        label = habitName(id, habitState, s),
                        done = id in doneToday,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        HabitHeatmap(
            records = records,
            today = today,
            maxPossible = maxPossible,
            weeksBack = 14,
            showLegend = false,
            cell = 9.dp,
            gap = 2.dp,
        )
    }
}

/**
 * One trackable's state for today. The check cross-fades its tint rather than
 * popping — no scaling, no bounce; this is a status, not an award. The dot is
 * announced as a single labelled state so a screen reader doesn't read an
 * unlabelled icon followed by a stray word.
 */
@Composable
private fun HabitDot(label: String, done: Boolean, modifier: Modifier = Modifier) {
    val motion = LocalMotion.current
    val tint by animateColorAsState(
        targetValue = if (done) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        animationSpec = motion.spec(Motion.standard),
        label = "habitDotTint",
    )
    val s = LocalStrings.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = if (done) "$label — ${s.markRead}" else label
        },
    ) {
        Icon(
            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (done) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** A quiet shortcut into the library. Filled, not outlined — three bordered
 *  boxes in a row read as a control panel; three soft tiles read as a shelf. */
@Composable
private fun QuickLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun HourRow(
    hour: Hour,
    isCurrent: Boolean,
    progress: Pair<Int, Int>?,
    onClick: () -> Unit,
) {
    val s = LocalStrings.current
    val complete = progress != null && progress.second > 0 && progress.first == progress.second
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = hour.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (isCurrent) {
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = s.currentHourBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                    )
                }
            }
            // Only shown once the hour has been started today.
            progress?.takeIf { it.first > 0 }?.let { (done, total) ->
                Text(
                    text = s.progressOf(done, total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = hour.timeHint,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (complete) {
            Spacer(Modifier.width(Spacing.sm))
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = s.markRead,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(IconSize.small),
            )
        }
    }
}

/**
 * "Pick up where you left off" — only shown for an hour started but unfinished
 * today. A gold rule down the left edge marks it without a second card outline.
 */
@Composable
private fun ResumeRow(hour: Hour, done: Int, total: Int, onClick: () -> Unit) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                s.resumePrayer,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    hour.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (total > 0) {
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        s.progressOf(done, total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(IconSize.medium),
        )
    }
}
