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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

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
    // Prayer, then only the habits today asks for: the count under ዛሬ is kept
    // of *due*, so a weekly habit does not read as missed six days a week.
    val habitIds = remember(habitState, today) {
        listOf(PRAYER_AGGREGATE_ID) + HabitsRepository.dueHabitIds(habitState, today)
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
) {
    // Cards size to their content. They used to be pinned to fixed heights
    // scaled by the font, which held the page to one screen — but a card whose
    // text outgrew its box got squeezed rather than given room, and the ንባብ
    // card at the foot was the one that showed it first on a small phone.
    // The page scrolls instead.
    val cardScale = LocalDensity.current.fontScale.coerceIn(1f, 2f)
    // The order is the day's: what is due now, what the Church appoints, the
    // two standing prayers — and ዛሬ last, because a tally of what has been done
    // is a summary and not a task.
    //
    // ንባብ is not here. It is a plan you are partway through rather than
    // something the day asks of you, it was the fifth card competing for one
    // screen, and its own reminder already opens it by name. It lives in
    // ቤተ መጻሕፍት, which is where a plan belongs.
    Column(modifier) {
        DayHeader(today, seasonLabel, onOpenSearch, onOpenFasting, onOpenBookmarks, onOpenPrayerList)
        Spacer(Modifier.height(Spacing.sm))
        if (suggested != null) {
            NowCard(
                hour = suggested,
                prayed = HabitsRepository.hourHabitId(suggested.id) in doneToday,
                onClick = { onOpenHour(suggested.id) },
            )
            Spacer(Modifier.height(Spacing.sm))
            HoursLine(
                next = com.agpeya.app.data.PrayerSchedule.next(hours, suggested.id),
                onOpenAll = onOpenAllHours,
            )
        } else {
            EmptyHoursCard(onOpenAllHours)
        }
        Spacer(Modifier.height(Spacing.sm))
        GitsaweCard(readingsState, onOpenGitsawe)
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
        Spacer(Modifier.height(Spacing.md))
        TodayRow(
            habitIds = habitIds,
            doneToday = doneToday,
            records = habitState.records,
            today = today,
            onClick = onOpenJourney,
            scale = cardScale,
        )
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

    // Two lines where there were four. The wordmark went: an app that is open
    // does not need to say its own name, and it was pushing the day — the one
    // thing this header exists to say — into third place.
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            com.agpeya.app.ui.common.SinqWordmark()
            Text(
                com.agpeya.app.ui.common.formatEthiopian(today, s),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // The greeting leads in gold and survives the ellipsis; the season
            // and the Gregorian date follow it, quietly, on the same line.
            val greeting = callName.takeIf { it.isNotBlank() }?.let { s.greeting(it) }
            val tail = listOfNotNull(
                com.agpeya.app.ui.common.formatGregorianShort(today, s).takeIf { it.isNotBlank() },
                seasonLabel,
            ).joinToString("  ·  ")
            Text(
                buildAnnotatedString {
                    if (greeting != null) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                            append(greeting)
                        }
                        if (tail.isNotEmpty()) append("  ·  ")
                    }
                    append(tail)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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

/**
 * The hour due now — its name, its time, and whether it has been prayed.
 *
 * What follows it, and the way to all of them, is [HoursLine] beneath: the hero
 * says one thing, and the strip under the hairline carries the sequence.
 *
 * The chip on the right is the only prayer state on the page above the fold:
 * አሁን while the hour is due, a lit candle once it has been prayed — which the
 * reader no longer has to tell the app, since reaching the foot of the hour
 * records it.
 */
@Composable
private fun NowCard(
    hour: Hour,
    prayed: Boolean,
    onClick: () -> Unit,
) {
    val s = LocalStrings.current
    val sinq = sinqColors
    // The room the ንባብ card left goes here rather than to the gap below it.
    // This is the one card on ቤት that says what to do now, and it was the same
    // height as the two shortcuts under it.
    HeroCard(onClick = onClick, contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.xl)) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.nowPrayer, style = MaterialTheme.typography.labelMedium, color = sinq.onHeroMuted)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        buildAnnotatedString {
                            append(hour.name)
                            if (hour.timeHint.isNotBlank()) {
                                withStyle(SpanStyle(color = sinq.onHeroMuted)) { append("  ·  ${hour.timeHint}") }
                            }
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = sinq.onHero,
                        // ጸሎተ መንፈቀ ሌሊት with its time hint does not fit one line
                        // on a narrow phone, and clipping the name of the hour
                        // you are being sent to is the wrong thing to clip.
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                if (prayed) {
                    Candle(
                        lit = true,
                        contentDescription = s.journeyTodayLit,
                        bodyColor = sinq.onHeroMuted,
                        flameColor = sinq.onHeroGold,
                        modifier = Modifier.size(width = 13.dp, height = 22.dp),
                    )
                } else {
                    Text(
                        s.nowChip,
                        style = MaterialTheme.typography.labelSmall,
                        color = sinq.onHero,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(sinq.onHero.copy(alpha = 0.16f))
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                    )
                }
            }
        }
    }
}

/**
 * The hours line: what is being prayed now, what follows it, and the way to all
 * of them — on the page itself, between the hero and the ግጻዌ, fenced by a
 * hairline top and bottom.
 *
 * It lived here before and was folded into the hero's foot; it is back out on
 * the page, but without repeating the hour the hero has just named in full.
 * The hero says what is due now, the strip says what comes after it, and
 * ሁሉም opens the rest — each thing said once.
 */
@Composable
private fun HoursLine(next: Hour?, onOpenAll: () -> Unit) {
    val s = LocalStrings.current
    SinqDivider()
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            Modifier.size(5.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
        Text(
            text = next?.let {
                listOf("${s.hoursNext} ${it.name}", it.timeHint)
                    .filter { part -> part.isNotBlank() }.joinToString("  ·  ")
            }.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        // Its own target, so tapping "all" never opens the current hour.
        Text(
            s.hoursAll,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onOpenAll)
                .semantics { role = Role.Button }
                .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
        )
    }
    SinqDivider()
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
    val mezmur = readings?.sundayCycle?.firstNotNullOfOrNull { it.mezmur }

    HeroCard(onClick = onClick, contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md)) {
        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = sinq.onHeroMuted, modifier = Modifier.size(IconSize.large))
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            // The kicker said "today's ግጻዌ" directly above a title saying the
            // same, and the citations below it — መዝ ፻፲፡፲–፲፩ · ሉቃስ ፲፡፩–፲፪ —
            // are for the page this card opens, not for a glance at ቤት. What
            // is left is the day's own name.
            Text(s.gitsaweTitle, style = MaterialTheme.typography.titleMedium, color = sinq.onHero)
            when (state) {
                HomeReadingsState.Loading -> Text(s.loadingLabel, style = MaterialTheme.typography.bodySmall, color = sinq.onHeroMuted)
                HomeReadingsState.Unavailable -> Text(s.contentMissingTitle, style = MaterialTheme.typography.bodySmall, color = sinq.onHeroMuted)
                is HomeReadingsState.Ready -> {
                    // The card grows down rather than cutting a feast off at
                    // the edge. A day named "በዓለ ቅዱስ ገብርኤል ሊቀ መላእክት" was being
                    // shown as much of itself as fitted and no more, on the one
                    // card whose whole job is to name the day.
                    (feast ?: reading)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = sinq.onHeroMuted)
                    }
                    mezmur?.let { Text("${s.sundayMezmurTitle} · $it", style = MaterialTheme.typography.bodySmall, color = sinq.onHero) }
                }
            }
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = sinq.onHeroMuted)
    }
}

/**
 * ዛሬ, at the foot of the page: a row between two hairlines rather than a card.
 *
 * It is a summary, not a task, so it comes last and does not compete with the
 * things above it for the eye. The card cost 99 dp to say what a 64 dp row
 * says — the count, the candle, the month's line, and the full ten weeks of
 * the heatmap, which is the part worth keeping at full size.
 */
@Composable
private fun TodayRow(
    habitIds: List<String>,
    doneToday: Set<String>,
    records: Map<String, Set<String>>,
    today: LocalDate,
    onClick: () -> Unit,
    /** Matches the page's own font-driven growth — see [HomeDashboard]. */
    scale: Float,
) {
    val s = LocalStrings.current
    val doneCount = habitIds.count { it in doneToday }
    val summary = remember(records, today) { PrayerJourney.summarize(records, today) }
    // The heatmap is drawn in dp, so it ignores the font scale that stretches
    // the text beside it. It grows with the row, but capped: past 1.5x it
    // would take the width that text needs.
    val glyphScale = scale.coerceAtMost(1.5f)
    Column(Modifier.fillMaxWidth()) {
        SinqDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(onClick = onClick)
                .semantics { role = Role.Button }
                .padding(vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        s.todayLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        "$doneCount/${habitIds.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
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
                weeksBack = 10,
                showLegend = false,
                cell = 6.dp * glyphScale,
                gap = 1.dp * glyphScale,
            )
        }
        SinqDivider()
    }
}

@Composable
private fun ShortcutCard(
    title: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // SinqCard lays its content out in a Column, so the row has to be one:
    // Modifier.weight in a column whose height is its content measures to
    // zero, which is why these cards shipped in 1.8.1 drawing nothing at all.
    SinqCard(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(Spacing.sm))
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
private fun DailyPsalmCard(today: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val range = remember(today) { com.agpeya.app.ui.psalter.dailyRange(today.dayOfWeek) }
    ShortcutCard(
        title = range?.let { s.psalmRange(it.first, it.last) } ?: s.wholePsalter,
        caption = "${s.psalterTitle}  ·  ${s.dailyPsalms}",
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun ZewotrCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    ShortcutCard(
        title = s.wudaseMariam,
        caption = s.zewotrTselot,
        onClick = onClick,
        modifier = modifier,
    )
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
