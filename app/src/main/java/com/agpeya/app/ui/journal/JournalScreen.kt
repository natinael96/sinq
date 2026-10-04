package com.agpeya.app.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.NotificationsActive
import com.agpeya.app.ui.common.SinqOutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.agpeya.app.data.JournalLock
import com.agpeya.app.data.JournalRepository
import com.agpeya.app.model.JournalEntry
import com.agpeya.app.model.JournalKind
import com.agpeya.app.ui.common.EthiopianDate
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.common.formatEthiopianShort
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.data.FastingCalendar
import com.agpeya.app.ui.theme.sinqColors
import androidx.compose.ui.unit.dp

/**
 * ማስታወሻ — the journal, browsed one Ethiopian month at a time.
 *
 * There is no search and no streak. The month is the unit because that is how
 * the Church's year is kept and how a person actually looks back: not "find
 * every time I wrote about anger", but "what was ጾመ ፍልሰታ like".
 *
 * The app never reads what is written here. It counts nothing, scores nothing,
 * and never asks why a week is empty — the moment writing becomes a habit to
 * keep, honesty is the first thing lost.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun JournalScreenContent(
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onNewEntry: (kind: JournalKind, targetDate: LocalDate?) -> Unit,
    onStartConfessionPrep: () -> Unit,
    onOpenPenance: () -> Unit,
    initialDate: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    // No screenshots, and nothing in the recents thumbnail.
    SecureScreen()

    val locked by JournalLock.isLocked(context).collectAsState(initial = true)
    val unlocked = true

    val today by com.agpeya.app.ui.common.rememberCurrentDate()
    val todayEth = remember(today) { EthiopianDate.from(today) }
    // Months back from the current one; 0 is now.
    var offset by remember { mutableIntStateOf(0) }
    val (year, month) = remember(offset, todayEth) {
        val total = todayEth.year * 13 + (todayEth.month - 1) - offset
        total / 13 to (total % 13) + 1
    }

    val parsedInitialDate = remember(initialDate) {
        initialDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }
    LaunchedEffect(parsedInitialDate) {
        parsedInitialDate?.let { initDate ->
            val initEth = EthiopianDate.from(initDate)
            val currentTotal = todayEth.year * 13 + (todayEth.month - 1)
            val targetTotal = initEth.year * 13 + (initEth.month - 1)
            offset = currentTotal - targetTotal
        }
    }

    val entries by JournalRepository.inEthiopianMonth(context, year, month)
        .collectAsState(initial = emptyList())
    val writtenDays by JournalRepository.writtenDaysIn(context, year, month)
        .collectAsState(initial = emptyList())

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(parsedInitialDate, entries) {
        if (parsedInitialDate != null) {
            val initEth = EthiopianDate.from(parsedInitialDate)
            val index = entries.indexOfFirst { it.context.ethDay == initEth.day }
            if (index >= 0) {
                listState.animateScrollToItem(index + 1)
            }
        }
    }

    var confessing by remember { mutableStateOf(false) }
    var penancePrompt by remember { mutableStateOf(false) }
    var settingPassphrase by remember { mutableStateOf(false) }
    var lockMenu by remember { mutableStateOf(false) }
    var removingPassphrase by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var createSheetDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedKindFilter by remember { mutableStateOf<JournalKind?>(null) }

    val displayedEntries = remember(entries, selectedKindFilter) {
        if (selectedKindFilter == null) entries else entries.filter { it.kind == selectedKindFilter }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.journalTitle,
                onBack = onBack,
                actions = {
                    // The icon says whether a lock is on, and a lock that is on
                    // can be changed or taken off — reachable only from inside,
                    // which is to say only by someone who has already opened it.
                    Box {
                        IconButton(onClick = { if (locked) lockMenu = true else settingPassphrase = true }) {
                            Icon(
                                imageVector = if (locked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                                contentDescription = if (locked) s.journalChangePassphrase else s.journalSetPassphrase,
                                tint = if (locked) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(expanded = lockMenu, onDismissRequest = { lockMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(s.journalChangePassphrase) },
                                onClick = { lockMenu = false; settingPassphrase = true },
                            )
                            DropdownMenuItem(
                                text = { Text(s.journalRemovePassphrase) },
                                onClick = { lockMenu = false; removingPassphrase = true },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!locked || unlocked) {
                FloatingActionButton(onClick = {
                    if (offset < 0) {
                        val futureDate = runCatching { EthiopianDate(year, month, 1).toGregorian() }.getOrNull()
                        createSheetDate = futureDate
                    } else {
                        createSheetDate = today
                    }
                    showCreateSheet = true
                }) {
                    Icon(Icons.Outlined.Add, contentDescription = s.newEntry)
                }
            }
        },
    ) { inner ->


        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { offset += 1 }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = s.previousMonth,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "${s.ethMonths.getOrElse(month - 1) { "" }} $year",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val daysInMonth = if (month == 13) EthiopianDate.pagumeLength(year) else 30
                        Text(
                            text = s.journalWrittenDays(geezNumeral(writtenDays.size), geezNumeral(daysInMonth)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                    IconButton(onClick = { offset -= 1 }, enabled = offset > -12) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = s.nextMonth,
                            tint = if (offset > -12) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        )
                    }
                }
                MonthStrip(
                    year = year,
                    month = month,
                    written = writtenDays.toSet(),
                    today = today,
                    onDay = { day ->
                        val index = displayedEntries.indexOfFirst { it.context.ethDay == day }
                        if (index >= 0) {
                            scope.launch { listState.animateScrollToItem(index + 1) }
                        } else {
                            val dayDate = runCatching {
                                EthiopianDate(year, month, day).toGregorian()
                            }.getOrNull()
                            if (dayDate != null && dayDate.isAfter(today)) {
                                onNewEntry(JournalKind.CHECKLIST, dayDate)
                            } else {
                                createSheetDate = dayDate
                                showCreateSheet = true
                            }
                        }
                    },
                )

                // Kind Filter Chips
                val filterOptions = listOf(
                    null to s.journalFilterAll,
                    JournalKind.CHECKLIST to s.journalKindChecklist,
                    JournalKind.REFLECTION to s.journalKindReflection,
                    JournalKind.PASSAGE to s.journalKindPassage,
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs, bottom = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filterOptions) { (kind, label) ->
                        val selected = selectedKindFilter == kind
                        FilterChip(
                            selected = selected,
                            onClick = { selectedKindFilter = kind },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                selectedLabelColor = MaterialTheme.colorScheme.secondary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.secondary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                selectedBorderColor = MaterialTheme.colorScheme.secondary,
                            ),
                        )
                    }
                }

                if (displayedEntries.isEmpty()) {
                    Spacer(Modifier.height(Spacing.lg))
                    Text(
                        s.journalEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(displayedEntries, key = { it.id }) { entry ->
                EntryRow(entry = entry, s = s, onClick = { onOpenEntry(entry.id) })
            }

            // The guided examination — the journal is where its draft lands, so
            // the journal is where it begins.
            item {
                Spacer(Modifier.height(Spacing.md))
                com.agpeya.app.ui.common.NavRow(
                    title = s.confessionPrepTitle,
                    onClick = onStartConfessionPrep,
                    subtitle = s.confessionPrepDesc,
                )
            }

            // Discharging drafts is an action on the whole journal, not on one
            // entry, so it lives at the foot of the list rather than in a card.
            if (entries.any { it.isDraft }) {
                item {
                    Spacer(Modifier.height(Spacing.md))
                    SinqOutlinedButton(
                        onClick = { confessing = true },
                        leadingIcon = Icons.Outlined.CheckCircle,
                    ) {
                        Text(s.confessedAction)
                    }
                }
            }
            item { Spacer(Modifier.height(Spacing.huge)) }
        }
    }

    if (showCreateSheet) {
        val isFuture = createSheetDate?.isAfter(today) == true || offset < 0
        NewEntryBottomSheet(
            s = s,
            isFutureDate = isFuture,
            onDismiss = {
                showCreateSheet = false
                createSheetDate = null
            },
            onSelectKind = { kind ->
                val targetDate = createSheetDate
                showCreateSheet = false
                createSheetDate = null
                onNewEntry(kind, targetDate)
            },
        )
    }

    if (confessing) {
        AlertDialog(
            onDismissRequest = { confessing = false },
            title = { Text(s.confessedAction) },
            text = { Text(s.confessedConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    confessing = false
                    scope.launch {
                        JournalRepository.dischargeDrafts(context)
                        penancePrompt = true
                    }
                    // The one moment a ቀኖና is fresh in hand is on the way home
                    // from confessing, so it is offered here and nowhere else.
                }) { Text(s.confessedAction) }
            },
            dismissButton = { TextButton(onClick = { confessing = false }) { Text(s.cancel) } },
        )
    }

    if (penancePrompt) {
        AlertDialog(
            onDismissRequest = { penancePrompt = false },
            title = { Text(s.penanceTitle) },
            text = { Text(s.confessionPrepPenancePrompt) },
            confirmButton = {
                TextButton(onClick = {
                    penancePrompt = false
                    onOpenPenance()
                }) { Text(s.penanceTitle) }
            },
            dismissButton = { TextButton(onClick = { penancePrompt = false }) { Text(s.cancel) } },
        )
    }

    if (removingPassphrase) {
        AlertDialog(
            onDismissRequest = { removingPassphrase = false },
            title = { Text(s.journalRemovePassphrase) },
            text = { Text(s.journalRemovePassphraseConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    removingPassphrase = false
                    scope.launch { JournalLock.clearPassphrase(context) }
                }) { Text(s.journalRemovePassphrase) }
            },
            dismissButton = {
                TextButton(onClick = { removingPassphrase = false }) { Text(s.cancel) }
            },
        )
    }

    if (settingPassphrase) {
        PassphraseDialog(
            s = s,
            onDismiss = { settingPassphrase = false },
            onSet = { phrase ->
                settingPassphrase = false
                scope.launch { JournalLock.setPassphrase(context, phrase) }
            },
        )
    }
}

/**
 * The month as a strip of days above its entries.
 *
 * The page's premise is the month — "what was ጾመ ፍልሰታ like" — but the month
 * was only ever a stack of cards, which says nothing about the days between
 * them. A written day is gold, a fast day carries the same green wash the ጉዞ
 * heatmap uses, today has a ring, and a tap scrolls to that day's first entry.
 *
 * Thirty cells, because an Ethiopian month is thirty days. ጳጉሜን gets five or
 * six, and the row simply ends there.
 */
/**
 * The month as a smooth, horizontally scrollable strip of days above its entries.
 *
 * Each card displays the weekday, day number, and status indicator.
 * A written day carries a warm gold accent, a fast day carries the fasting wash,
 * today carries a highlighted accent ring, and tapping a written day smoothly scrolls to its first entry.
 */
@Composable
private fun MonthStrip(
    year: Int,
    month: Int,
    written: Set<Int>,
    today: LocalDate,
    onDay: (Int) -> Unit,
) {
    val s = LocalStrings.current
    val haptics = LocalHapticFeedback.current
    val days = remember(year, month) {
        if (month == 13) EthiopianDate.pagumeLength(year) else 30
    }
    val sinq = sinqColors
    val fastWash = sinq.hero.copy(alpha = 0.14f)
    val gold = MaterialTheme.colorScheme.secondary
    val empty = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val todayEth = remember(today) { EthiopianDate.from(today) }

    // Pre-calculate fasting and weekdays for all days in the month once
    val dayInfos = remember(year, month, days) {
        (1..days).map { day ->
            runCatching {
                val date = EthiopianDate(year, month, day).toGregorian()
                val isFast = FastingCalendar.fastOn(date) != null || FastingCalendar.isWeeklyFastDay(date)
                val weekday = s.weekdayNames.getOrElse(date.dayOfWeek.value - 1) { "" }
                Triple(date, isFast, weekday)
            }.getOrElse { Triple(null, false, "") }
        }
    }

    val rowState = rememberLazyListState()
    LaunchedEffect(year, month) {
        if (todayEth.year == year && todayEth.month == month) {
            rowState.animateScrollToItem((todayEth.day - 3).coerceAtLeast(0))
        }
    }

    LazyRow(
        state = rowState,
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(days) { index ->
            val day = index + 1
            val isWritten = day in written
            val isToday = todayEth.year == year && todayEth.month == month && todayEth.day == day
            val (gregDate, isFast, weekday) = dayInfos[index]

            val containerColor = when {
                isWritten -> gold.copy(alpha = 0.22f)
                isFast -> fastWash
                else -> empty
            }
            val borderColor = when {
                isToday -> gold
                isWritten -> gold.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            }

            Column(
                modifier = Modifier
                    .width(48.dp)
                    .height(68.dp)
                    .semantics(mergeDescendants = true) {
                        gregDate?.let { contentDescription = formatEthiopian(it, s) }
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerColor)
                    .border(
                        width = if (isToday) 1.6.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable(onClickLabel = s.journalTitle) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDay(day)
                    }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) gold else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = geezNumeral(day),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isToday || isWritten) FontWeight.Bold else FontWeight.Medium,
                    color = if (isWritten) gold else MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isWritten -> gold
                                isToday -> gold.copy(alpha = 0.5f)
                                isFast -> sinq.hero.copy(alpha = 0.7f)
                                else -> Color.Transparent
                            }
                        ),
                )
            }
        }
    }
}

/**
 * A dated list entry. Confession rows show a fixed label instead of note text.
 */
@Composable
private fun EntryRow(entry: JournalEntry, s: Strings, onClick: () -> Unit) {
    SinqCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.localDate?.let { formatEthiopianShort(it, s) } ?: entry.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                if (entry.isDraft) {
                    Spacer(Modifier.width(Spacing.sm))
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = s.journalKindConfession,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            when (entry.kind) {
                JournalKind.CHECKLIST -> {
                    val (items, _) = com.agpeya.app.model.ChecklistParser.parse(entry.body)
                    if (items.isNotEmpty()) {
                        val done = items.count { it.isDone }
                        val total = items.size
                        val isAllDone = done == total
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isAllDone) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Icon(
                                if (isAllDone) Icons.Outlined.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                contentDescription = null,
                                tint = if (isAllDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${geezNumeral(done)}/${geezNumeral(total)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAllDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (items.any { it.hasReminder }) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Outlined.NotificationsActive,
                                    contentDescription = s.checklistAlarmOn,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }
                JournalKind.PASSAGE -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            Icons.Outlined.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            entry.anchorLabel ?: s.journalKindPassage,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                        )
                    }
                }
                JournalKind.CONFESSION_DRAFT -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            s.journalKindConfession,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                JournalKind.REFLECTION -> {
                    // Minimal liturgical reflection presentation
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            if (entry.isDraft) s.journalKindConfession else entry.preview,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        // Fast information if the day carried a fast
        val context = entry.context.fast.orEmpty()
        if (context.isNotBlank()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                context,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NewEntryBottomSheet(
    s: Strings,
    isFutureDate: Boolean = false,
    onDismiss: () -> Unit,
    onSelectKind: (JournalKind) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.huge),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = s.journalNewEntryPrompt,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            if (isFutureDate) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = s.journalFutureDateNotice,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            val allOptions = listOf(
                EntryKindOption(
                    kind = JournalKind.CHECKLIST,
                    icon = Icons.Outlined.Checklist,
                    title = s.journalKindChecklist,
                    desc = s.journalKindChecklistDesc,
                ),
                EntryKindOption(
                    kind = JournalKind.REFLECTION,
                    icon = Icons.Outlined.EditNote,
                    title = s.journalKindReflection,
                    desc = s.journalKindReflectionDesc,
                ),
                EntryKindOption(
                    kind = JournalKind.PASSAGE,
                    icon = Icons.Outlined.AutoStories,
                    title = s.journalKindPassage,
                    desc = s.journalKindPassageDesc,
                ),
                EntryKindOption(
                    kind = JournalKind.CONFESSION_DRAFT,
                    icon = Icons.Outlined.Lock,
                    title = s.journalKindConfession,
                    desc = s.journalKindConfessionDesc,
                ),
            )
            val options = if (isFutureDate) {
                allOptions.filter { it.kind == JournalKind.CHECKLIST }
            } else {
                allOptions
            }
            options.forEach { opt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable {
                            onDismiss()
                            onSelectKind(opt.kind)
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = opt.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = opt.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = opt.desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

private data class EntryKindOption(
    val kind: JournalKind,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val desc: String,
)

@Composable
fun JournalScreen(
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onNewEntry: (JournalKind, LocalDate?) -> Unit,
    onStartConfessionPrep: () -> Unit,
    onOpenPenance: () -> Unit,
    initialDate: String? = null,
) {
    com.agpeya.app.ui.journal.JournalAccess(onBack) {
        JournalScreenContent(onBack, onOpenEntry, onNewEntry, onStartConfessionPrep, onOpenPenance, initialDate)
    }
}

@Composable
fun JournalScreen(
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onNewEntry: (JournalKind) -> Unit,
    onStartConfessionPrep: () -> Unit,
    onOpenPenance: () -> Unit,
) {
    JournalScreen(
        onBack = onBack,
        onOpenEntry = onOpenEntry,
        onNewEntry = { kind, _ -> onNewEntry(kind) },
        onStartConfessionPrep = onStartConfessionPrep,
        onOpenPenance = onOpenPenance,
        initialDate = null,
    )
}
