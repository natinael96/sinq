package com.agpeya.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.agpeya.app.ui.common.HeroCard
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqDivider
import com.agpeya.app.ui.common.Tab
import com.agpeya.app.ui.common.liturgicalSeasonLabel
import com.agpeya.app.ui.habits.HabitHeatmap
import com.agpeya.app.ui.habits.journeyLine
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.sinqColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime

private const val PRAYER_AGGREGATE_ID = "prayer"

/** A glanceable dashboard; constrained accessibility layouts retain a scroll safety net. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenHour: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFasting: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenPrayerList: () -> Unit,
    onOpenPsalter: () -> Unit,
    onOpenZewotr: () -> Unit,
    onOpenGitsawe: () -> Unit,
    onOpenReading: () -> Unit,
    onSelectTab: (Tab) -> Unit,
) {
    val context = LocalContext.current
    val config by HoursRepository.config(context).collectAsState(initial = HoursConfig())
    val builtIn by produceState<List<Hour>>(initialValue = emptyList()) {
        value = ContentRepository.hours(context)
    }
    val hours = remember(builtIn, config) { HoursRepository.merge(builtIn, config, includeHidden = false) }

    // Keeps the date, daily content, and current prayer correct across time boundaries.
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(30_000)
        }
    }
    val today = now.toLocalDate()
    val currentHourId = remember(hours, now.hour, now.minute) {
        com.agpeya.app.data.PrayerSchedule.currentHourId(hours, now.toLocalTime())
    }
    val suggestedId = remember(now.hour) { ContentRepository.suggestedHourId(now.hour) }
    val suggested = hours.find { it.id == suggestedId } ?: hours.find { it.id == currentHourId }

    val habitState by HabitsRepository.state(context).collectAsState(initial = HabitsState())
    val doneToday = habitState.records[today.toString()] ?: emptySet()
    val prayedAnyHour = doneToday.any { it.startsWith("hour_") }
    val habitIds = remember(habitState) {
        listOf(PRAYER_AGGREGATE_ID) + HabitsRepository.orderedHabitIds(habitState, includeHidden = false)
    }
    val doneWithAggregate = if (prayedAnyHour) doneToday + PRAYER_AGGREGATE_ID else doneToday
    val strings = LocalStrings.current
    val seasonLabel = remember(today, strings) { liturgicalSeasonLabel(today, strings) }

    val readingsState by produceState<HomeReadingsState>(initialValue = HomeReadingsState.Loading, today) {
        value = runCatching { GitsaweRepository.readingsFor(context, today) }
            .fold(
                onSuccess = { HomeReadingsState.Ready(it) },
                onFailure = { HomeReadingsState.Unavailable },
            )
    }
    // Today's plan line, or null when no plan is being kept.
    val planContent by produceState(com.agpeya.app.model.ReadingPlanContent()) {
        value = com.agpeya.app.data.ReadingPlanRepository.content(context)
    }
    val planState by com.agpeya.app.data.ReadingPlanRepository.state(context)
        .collectAsState(initial = com.agpeya.app.model.ReadingPlanState())
    val planLine = remember(planContent, planState, today) {
        planContent.plans.firstOrNull { it.id == planState.activePlanId }?.let { plan ->
            val day = com.agpeya.app.data.ReadingPlanRepository
                .dayOn(planState.startedOn, today, plan.days)
            val passages = plan.day(day)?.r.orEmpty().joinToString(" · ") { r ->
                val name = r.b.split('-').joinToString(" ") { part ->
                    part.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
                }
                if (r.to > r.c) "$name ${r.c}–${r.to}" else "$name ${r.c}"
            }
            day to passages
        }
    }

    var showHours by remember { mutableStateOf(false) }

    // The only network-facing thing on this screen, and it draws nothing unless
    // the check is on AND a newer release was found AND it wasn't waved away.
    val scope = rememberCoroutineScope()
    val update by com.agpeya.app.data.UpdateRepository.available(context)
        .collectAsState(initial = null)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AgpeyaBottomBar(current = Tab.HOME, onSelect = onSelectTab) },
    ) { innerPadding ->
      Column(Modifier.fillMaxSize().padding(innerPadding)) {
        // Above the day, outside the screen margin: a notice about the app
        // itself has no business indenting the date beneath it.
        update?.let { found ->
            com.agpeya.app.ui.common.UpdateLine(
                version = found.version,
                onOpen = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(found.url),
                            ),
                        )
                    }
                },
                onDismiss = {
                    scope.launch {
                        com.agpeya.app.data.UpdateRepository.dismiss(context, found.version)
                    }
                },
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val fontScale = LocalDensity.current.fontScale
            val stackReadingCards = maxWidth < 360.dp || fontScale > 1.15f
            HomeDashboard(
                modifier = Modifier
                    // A tablet should gain calm margins, not comically wide cards.
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    // Always scrollable. The cards size to their own text now, so
                    // the page is as tall as the day's content makes it — on a
                    // short screen that is taller than the viewport, and the
                    // alternative to scrolling is squeezing.
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screen, vertical = Spacing.md),
                stackReadingCards = stackReadingCards,
                today = today,
                seasonLabel = seasonLabel,
                suggested = suggested,
                hours = hours,
                readingsState = readingsState,
                habitIds = habitIds,
                doneToday = doneWithAggregate,
                habitState = habitState,
                onOpenHour = onOpenHour,
                onOpenSearch = onOpenSearch,
                onOpenFasting = onOpenFasting,
                onOpenBookmarks = onOpenBookmarks,
                onOpenPrayerList = onOpenPrayerList,
                onOpenAllHours = { showHours = true },
                onOpenGitsawe = onOpenGitsawe,
                onOpenJourney = { onSelectTab(Tab.JOURNEY) },
                onOpenPsalter = onOpenPsalter,
                onOpenZewotr = onOpenZewotr,
                planLine = planLine,
                onOpenReading = onOpenReading,
            )
        }
      }
    }

    if (showHours) {
        ModalBottomSheet(onDismissRequest = { showHours = false }) {
            AllHoursSheet(hours, currentHourId) {
                showHours = false
                onOpenHour(it)
            }
        }
    }
}

private sealed interface HomeReadingsState {
    data object Loading : HomeReadingsState
    data object Unavailable : HomeReadingsState
    data class Ready(val readings: DayReadings) : HomeReadingsState
}


@Composable
private fun HomeDashboard(
    modifier: Modifier,
    stackReadingCards: Boolean,
    today: LocalDate,
    seasonLabel: String?,
    suggested: Hour?,
    hours: List<Hour>,
    readingsState: HomeReadingsState,
    habitIds: List<String>,
    doneToday: Set<String>,
    habitState: HabitsState,
    onOpenHour: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFasting: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenPrayerList: () -> Unit,
    onOpenAllHours: () -> Unit,
    onOpenGitsawe: () -> Unit,
    onOpenJourney: () -> Unit,
    onOpenPsalter: () -> Unit,
    onOpenZewotr: () -> Unit,
    planLine: Pair<Int, String>?,
    onOpenReading: () -> Unit,
) {
    // Cards size to their content. They used to be pinned to fixed heights
    // scaled by the font, which held the page to one screen — but a card whose
    // text outgrew its box got squeezed rather than given room, and the ንባብ
    // card at the foot was the one that showed it first on a small phone.
    // The page scrolls instead.
    val cardScale = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    Column(modifier) {
        DayHeader(today, seasonLabel, onOpenSearch, onOpenFasting, onOpenBookmarks, onOpenPrayerList)
        Spacer(Modifier.height(Spacing.md))
        if (suggested != null) NowCard(suggested) { onOpenHour(suggested.id) }
        else EmptyHoursCard(onOpenAllHours)
        if (suggested != null) {
            HoursStrip(
                current = suggested,
                next = com.agpeya.app.data.PrayerSchedule.next(hours, suggested.id),
                onOpenHour = onOpenHour,
                onOpenAll = onOpenAllHours,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        GitsaweCard(readingsState, onOpenGitsawe)
        Spacer(Modifier.height(Spacing.md))

        TodayCard(
            habitIds,
            doneToday,
            habitState.records,
            today,
            hours.size + (habitIds.size - 1),
            onOpenJourney,
            cardScale,
            Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))
        if (stackReadingCards) {
            DailyPsalmCard(today, onOpenPsalter, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.sm))
            ZewotrCard(onOpenZewotr, Modifier.fillMaxWidth())
        } else {
            // IntrinsicSize.Min so the pair matches the taller of the two
            // rather than a guessed height — they read as one row either way.
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                DailyPsalmCard(today, onOpenPsalter, Modifier.weight(1f).fillMaxHeight())
                ZewotrCard(onOpenZewotr, Modifier.weight(1f).fillMaxHeight())
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        ReadingCard(planLine, onOpenReading, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun DayHeader(
    today: LocalDate,
    seasonLabel: String?,
    onOpenSearch: () -> Unit,
    onOpenFasting: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenPrayerList: () -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val profileName by com.agpeya.app.data.SettingsRepository.profileName(context).collectAsState(initial = "")
    val christianName by com.agpeya.app.data.SettingsRepository.christianName(context).collectAsState(initial = "")
    val callName = christianName.ifBlank { profileName }
    var menuOpen by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text("ስንቅ", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(
                com.agpeya.app.ui.common.formatEthiopian(today, s),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                com.agpeya.app.ui.common.formatGregorianShort(today, s),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
            )
            val accent = listOfNotNull(
                seasonLabel,
                callName.takeIf { it.isNotBlank() }?.let { s.greeting(it) },
            )
            if (accent.isNotEmpty()) {
                Text(
                    accent.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onOpenSearch) {
            Icon(Icons.Outlined.Search, contentDescription = s.tabSearch, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = s.more, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                HomeMenuItem(Icons.Outlined.CalendarMonth, s.fastingTitle) { menuOpen = false; onOpenFasting() }
                HomeMenuItem(Icons.Outlined.Bookmarks, s.bookmarksTitle) { menuOpen = false; onOpenBookmarks() }
                HomeMenuItem(Icons.Outlined.VolunteerActivism, s.prayerListTitle) { menuOpen = false; onOpenPrayerList() }
            }
        }
    }
}

@Composable
private fun HomeMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
    )
}

@Composable
private fun NowCard(hour: Hour, onClick: () -> Unit) {
    val s = LocalStrings.current
    val sinq = sinqColors
    HeroCard(onClick = onClick, contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md)) {
        Column(Modifier.weight(1f)) {
            Text(s.nowPrayer, style = MaterialTheme.typography.labelMedium, color = sinq.onHeroMuted)
            Text(hour.name, style = MaterialTheme.typography.titleLarge, color = sinq.onHero, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (hour.timeHint.isNotBlank()) Text(hour.timeHint, style = MaterialTheme.typography.bodySmall, color = sinq.onHeroMuted, maxLines = 1)
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = sinq.onHeroMuted)
    }
}

/**
 * The hours, as one line under the prayer card.
 *
 * Built like the update notice above it — a dot, a sentence, a hairline rule —
 * so ቤት has one shape for "a quiet word about something", not two.
 *
 * The hero directly above already names the hour due now, so the line's real
 * work is the one after it: "ቀጥሎ …" is the thing the page could not say before.
 * The names are the app's own, ጸሎት and all, rather than shortened to fit.
 */
@Composable
private fun HoursStrip(
    current: Hour,
    next: Hour?,
    onOpenHour: (String) -> Unit,
    onOpenAll: () -> Unit,
) {
    val s = LocalStrings.current
    Spacer(Modifier.height(Spacing.sm))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable { onOpenHour(current.id) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
        Text(
            current.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
        if (next != null) {
            Text(
                "·",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                "${s.hoursNext} ${next.name}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        // Its own target, so tapping "all" never opens the current hour by
        // accident. Width earns it a real target on a line this short.
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onOpenAll)
                .semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                s.hoursAll,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
            )
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
            .clearAndSetSemantics { },
    )
}

@Composable
private fun EmptyHoursCard(onClick: () -> Unit) {
    val s = LocalStrings.current
    SinqCard(onClick = onClick, accented = true, contentPadding = PaddingValues(Spacing.lg)) {
        Text(s.hoursHeader, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(s.manageHours, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}



/** Preserves the full feast and reading content from the existing Home card. */
@Composable
private fun GitsaweCard(state: HomeReadingsState, onClick: () -> Unit) {
    val s = LocalStrings.current
    val sinq = sinqColors
    val readings = (state as? HomeReadingsState.Ready)?.readings
    val feast = readings?.feasts?.firstOrNull()?.amharicName
    val reading = readings?.daily?.title
        ?: readings?.seasonal?.firstOrNull()?.title
        ?: readings?.monthly?.firstOrNull()?.let { it.title ?: it.raw }
        ?: readings?.sundayCycle?.firstOrNull()?.title

    HeroCard(onClick = onClick, contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md)) {
        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = sinq.onHeroMuted, modifier = Modifier.size(IconSize.large))
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(s.gitsaweKicker, style = MaterialTheme.typography.labelMedium, color = sinq.onHeroMuted)
            Text(s.gitsaweTitle, style = MaterialTheme.typography.titleMedium, color = sinq.onHero)
            when (state) {
                HomeReadingsState.Loading -> Text(s.loadingLabel, style = MaterialTheme.typography.bodySmall, color = sinq.onHeroMuted)
                HomeReadingsState.Unavailable -> Text(s.contentMissingTitle, style = MaterialTheme.typography.bodySmall, color = sinq.onHeroMuted)
                is HomeReadingsState.Ready -> {
                    feast?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = sinq.onHeroMuted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    reading?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = sinq.onHeroMuted, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = sinq.onHeroMuted)
    }
}

@Composable
private fun TodayCard(
    habitIds: List<String>,
    doneToday: Set<String>,
    records: Map<String, Set<String>>,
    today: LocalDate,
    maxPossible: Int,
    onClick: () -> Unit,
    /** Matches the card's own font-driven growth — see [HomeDashboard]. */
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val doneCount = habitIds.count { it in doneToday }
    val summary = remember(records, today) { PrayerJourney.summarize(records, today) }
    // The heatmap is drawn in dp, so it ignores the font scale that stretches
    // this card — left alone it would sit adrift in a half-empty box. It grows
    // with the card, but capped: past 1.5x it would take the width the reading
    // beside it needs.
    val glyphScale = scale.coerceAtMost(1.5f)
    SinqCard(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(Spacing.md)) {
        Text(s.todayLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
        Spacer(Modifier.height(Spacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$doneCount/${habitIds.size}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.width(Spacing.sm))
                    Candle(
                        lit = summary.prayedToday,
                        contentDescription = if (summary.prayedToday) s.journeyTodayLit else s.journeyTodayUnlit,
                        bodyColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        flameColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(width = 13.dp * glyphScale, height = 22.dp * glyphScale),
                    )
                }
                Text(
                    journeyLine(summary, s),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(Spacing.xs))
            HabitHeatmap(
                records = records,
                today = today,
                maxPossible = maxPossible.coerceAtLeast(1),
                weeksBack = 10,
                showLegend = false,
                cell = 6.dp * glyphScale,
                gap = 1.dp * glyphScale,
            )
        }
    }
}

@Composable
private fun DailyPsalmCard(today: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val range = remember(today) { com.agpeya.app.ui.psalter.dailyRange(today.dayOfWeek) }
    SinqCard(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(Spacing.md)) {
        Text(s.dailyPsalms, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            range?.let { s.psalmRange(it.first, it.last) } ?: s.wholePsalter,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.psalterTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(IconSize.small))
        }
    }
}

/**
 * ንባብ — the day of the reading plan, below the Psalter and ዘወትር.
 *
 * Placed last on ቤት and below [GitsaweCard] on purpose: the appointed readings
 * lead, and the plan follows. It reads what the ግጻዌ does not.
 */
@Composable
private fun ReadingCard(planLine: Pair<Int, String>?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    SinqCard(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(Spacing.md)) {
        Text(
            s.readingTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(Spacing.xs))
        if (planLine == null) {
            Text(s.readingNoPlan, style = MaterialTheme.typography.titleSmall, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        } else {
            val (day, passages) = planLine
            Text(
                listOf(
                    s.readingDayLabel(com.agpeya.app.ui.reading.geezNumeral(day)),
                    passages,
                ).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                s.readingWithGitsawe,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ZewotrCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    SinqCard(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(Spacing.md)) {
        Text(
            s.zewotrTselot,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            s.wudaseMariam,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "ጸሎት ዘዘወትር",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
private fun AllHoursSheet(hours: List<Hour>, currentHourId: String?, onOpenHour: (String) -> Unit) {
    val s = LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.screen).padding(bottom = Spacing.xxl)) {
        Text(s.hoursHeader, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Spacing.md))
        if (hours.isEmpty()) {
            Text(s.manageHours, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            hours.forEachIndexed { index, hour ->
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(MaterialTheme.shapes.small).clickable { onOpenHour(hour.id) }.padding(vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(hour.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        if (hour.id == currentHourId) {
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                s.currentHourBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)).padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                            )
                        }
                    }
                    Text(hour.timeHint, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (index < hours.lastIndex) SinqDivider()
            }
        }
    }
}
