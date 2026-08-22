package com.agpeya.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.DayReadings
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.data.HoursRepository
import com.agpeya.app.data.PrayerJourney
import com.agpeya.app.model.HabitsState
import com.agpeya.app.model.Hour
import com.agpeya.app.model.HoursConfig
import com.agpeya.app.ui.common.AgpeyaBottomBar
import com.agpeya.app.ui.common.Candle
import com.agpeya.app.ui.common.CollapsibleHeader
import com.agpeya.app.ui.common.HeroCard
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqDivider
import com.agpeya.app.ui.common.Tab
import com.agpeya.app.ui.habits.HabitHeatmap
import com.agpeya.app.ui.habits.journeyLine
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
 * am I doing, and what is today's psalm.
 *
 * The cards on this page are the day's doors: the prayer for now, the prayer
 * hours, the ግጻዌ, and the Today/psalter pair. Hours stays compact until the
 * reader asks for the complete list, keeping that primary route visible without
 * letting it push the rest of the day below the fold.
 */
@Composable
fun HomeScreen(
    onOpenHour: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFasting: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenPrayerList: () -> Unit,
    onOpenPsalter: () -> Unit,
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
    // the per-hour breakdown lives on the Journey screen.
    val doneToday = habitState.records[today.toString()] ?: emptySet()
    val prayedAnyHour = doneToday.any { it.startsWith("hour_") }
    val habitIds = remember(habitState, prayedAnyHour) {
        listOf(PRAYER_AGGREGATE_ID) + HabitsRepository.orderedHabitIds(habitState, includeHidden = false)
    }
    val doneWithAggregate = if (prayedAnyHour) doneToday + PRAYER_AGGREGATE_ID else doneToday
    // Keep the full list opt-in, but leave its compact preview near the top so
    // Hours remains discoverable instead of becoming a footer.
    var hoursExpanded by rememberSaveable { mutableStateOf(false) }
    val seasonLabel = remember(today, s) { liturgicalSeasonLabel(today, s) }
    val currentHourId = remember(hours) { com.agpeya.app.data.PrayerSchedule.currentHourId(hours) }
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
                    onOpenPrayerList = onOpenPrayerList,
                )
                Spacer(Modifier.height(Spacing.xl))
            }

            if (suggested != null) {
                item(key = "now") {
                    NowCard(hour = suggested, onClick = { onOpenHour(suggested.id) })
                    Spacer(Modifier.height(Spacing.md))
                }
            }

            item(key = "hours") {
                HoursCard(
                    hours = hours,
                    currentHourId = currentHourId,
                    expanded = hoursExpanded,
                    onToggle = { hoursExpanded = !hoursExpanded },
                    onOpenHour = onOpenHour,
                )
                Spacer(Modifier.height(Spacing.lg))
            }

            item(key = "gitsawe") {
                GitsaweCard(readings = readings, onClick = onOpenGitsawe)
                Spacer(Modifier.height(Spacing.xxl))
            }

            // Today's progress and the day's psalter portion, side by side: what
            // I have done, and what there is to read. The library shortcuts that
            // used to sit here live on the Library tab, where they belong.
            item(key = "today") {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    TodaySection(
                        habitIds = habitIds,
                        doneToday = doneWithAggregate,
                        habitState = habitState,
                        records = habitState.records,
                        today = today,
                        // Real trackables: visible hours + habits (aggregate dot excluded).
                        maxPossible = hours.size + (habitIds.size - 1),
                        onClick = { onSelectTab(Tab.JOURNEY) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                    DailyPsalmCard(
                        today = today,
                        onClick = onOpenPsalter,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
                Spacer(Modifier.height(Spacing.xxl))
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
    onOpenPrayerList: () -> Unit,
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
            HeaderAction(Icons.Outlined.VolunteerActivism, s.prayerListTitle, onOpenPrayerList)
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
 * The complete prayer cycle in a compact, early-page card. Collapsed state still
 * names the available hours, while expansion keeps the existing direct links and
 * current-hour marker in place.
 */
@Composable
private fun HoursCard(
    hours: List<Hour>,
    currentHourId: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenHour: (String) -> Unit,
) {
    val s = LocalStrings.current
    val motion = LocalMotion.current
    SinqCard(
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        accented = true,
    ) {
        CollapsibleHeader(
            text = s.hoursHeader,
            expanded = expanded,
            badge = hours.size.toString(),
            onToggle = onToggle,
        )
        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(motion.spec(Motion.fast)),
            exit = fadeOut(motion.spec(Motion.fast)),
        ) {
            Text(
                text = hours.joinToString("  ·  ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(bottom = Spacing.md),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(motion.spec(Motion.standard)) + expandVertically(motion.spec(Motion.standard)),
            exit = fadeOut(motion.spec(Motion.fast)) + shrinkVertically(motion.spec(Motion.fast)),
        ) {
            Column {
                hours.forEachIndexed { index, hour ->
                    HourRow(
                        hour = hour,
                        isCurrent = hour.id == currentHourId,
                        onClick = { onOpenHour(hour.id) },
                    )
                    if (index < hours.lastIndex) SinqDivider()
                }
            }
        }
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
    val sinq = sinqColors
    val feast = readings?.feasts?.firstOrNull()?.amharicName
    val reading = readings?.daily?.title
        ?: readings?.seasonal?.firstOrNull()?.title
        ?: readings?.monthly?.firstOrNull()?.let { it.title ?: it.raw }
    HeroCard(onClick = onClick) {
        Icon(
            Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = sinq.onHeroMuted,
            modifier = Modifier.size(IconSize.large),
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                s.gitsaweKicker,
                style = MaterialTheme.typography.labelMedium,
                color = sinq.onHeroMuted,
            )
            Text(
                s.gitsaweTitle,
                style = MaterialTheme.typography.titleMedium,
                color = sinq.onHero,
            )
            feast?.let {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = sinq.onHeroMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            reading?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = sinq.onHeroMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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
 * Today's progress at half width: the count, today's candle, and the recent
 * history. A card now, because it shares a row with [DailyPsalmCard] and two
 * shapes of different rank would read as a mistake. The metric line is the same
 * [journeyLine] the Journey screen leads with — one [PrayerJourney] source of
 * truth, so Home and Journey can never disagree.
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
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val doneCount = habitIds.count { it in doneToday }
    val summary = remember(records, today) { PrayerJourney.summarize(records, today) }
    SinqCard(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.lg),
    ) {
        Text(
            text = s.todayLabel,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$doneCount/${habitIds.size}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(Spacing.md))
            Candle(
                lit = summary.prayedToday,
                contentDescription = if (summary.prayedToday) s.journeyTodayLit else s.journeyTodayUnlit,
                bodyColor = MaterialTheme.colorScheme.onSurfaceVariant,
                flameColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(width = 13.dp, height = 22.dp),
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = journeyLine(summary, s),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(Spacing.md))
        HabitHeatmap(
            records = records,
            today = today,
            maxPossible = maxPossible,
            weeksBack = 10,
            showLegend = false,
            cell = 7.dp,
            gap = 2.dp,
        )
    }
}

/**
 * የዕለቱ መዝሙረ ዳዊት — the day's portion of the Psalter by the traditional weekday
 * division, named on the card so Home tells you what today's reading *is*, not
 * just that one exists. Sunday has no fixed portion; the card offers the whole
 * Psalter instead.
 */
@Composable
private fun DailyPsalmCard(today: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val range = remember(today) { com.agpeya.app.ui.psalter.dailyRange(today.dayOfWeek) }
    SinqCard(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.lg),
    ) {
        Text(
            text = s.dailyPsalms,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = s.psalterTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = range?.let { s.psalmRange(it.first, it.last) } ?: s.wholePsalter,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(IconSize.small),
            )
        }
    }
}

@Composable
private fun HourRow(
    hour: Hour,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val s = LocalStrings.current
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
                            // A lighter wash than it looks like it wants: gold text
                            // on a gold tint only clears 4.5:1 once the tint drops
                            // to about 8% on the ivory ground.
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                    )
                }
            }
        }
        Text(
            text = hour.timeHint,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
