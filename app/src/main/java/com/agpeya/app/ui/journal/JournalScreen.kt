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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.agpeya.app.ui.common.formatEthiopianShort
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
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
fun JournalScreen(
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onNewEntry: (kind: JournalKind) -> Unit,
    onStartConfessionPrep: () -> Unit,
    onOpenPenance: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    // No screenshots, and nothing in the recents thumbnail.
    SecureScreen()

    val locked by JournalLock.isLocked(context).collectAsState(initial = false)
    var unlocked by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now() }
    val todayEth = remember(today) { EthiopianDate.from(today) }
    // Months back from the current one; 0 is now.
    var offset by remember { mutableIntStateOf(0) }
    val (year, month) = remember(offset, todayEth) {
        val total = todayEth.year * 13 + (todayEth.month - 1) - offset
        total / 13 to (total % 13) + 1
    }

    val entries by JournalRepository.inEthiopianMonth(context, year, month)
        .collectAsState(initial = emptyList())
    val writtenDays by JournalRepository.writtenDaysIn(context, year, month)
        .collectAsState(initial = emptyList())

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var confessing by remember { mutableStateOf(false) }
    var penancePrompt by remember { mutableStateOf(false) }
    var settingPassphrase by remember { mutableStateOf(false) }
    var lockMenu by remember { mutableStateOf(false) }
    var removingPassphrase by remember { mutableStateOf(false) }

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
                FloatingActionButton(onClick = { onNewEntry(JournalKind.REFLECTION) }) {
                    Icon(Icons.Outlined.Add, contentDescription = s.newEntry)
                }
            }
        },
    ) { inner ->
        if (locked && !unlocked) {
            Column(Modifier.fillMaxSize().padding(inner)) {
                JournalLockGate(s) { unlocked = true }
            }
            return@Scaffold
        }

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
                    TextButton(onClick = { offset += 1 }) { Text("←") }
                    Text(
                        "${s.ethMonths.getOrElse(month - 1) { "" }} $year",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = { offset -= 1 }, enabled = offset > 0) { Text("→") }
                }
                MonthStrip(
                    year = year,
                    month = month,
                    written = writtenDays.toSet(),
                    today = today,
                    onDay = { day ->
                        // The header is item 0, so the first entry of a day
                        // sits one past its index in the list.
                        val index = entries.indexOfFirst { it.context.ethDay == day }
                        if (index >= 0) scope.launch { listState.animateScrollToItem(index + 1) }
                    },
                )
                if (entries.isEmpty()) {
                    Spacer(Modifier.height(Spacing.lg))
                    Text(
                        s.journalEmpty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(entries, key = { it.id }) { entry ->
                EntryRow(entry = entry, s = s, onClick = { onOpenEntry(entry.id) })
            }

            // The guided examination — the journal is where its draft lands, so
            // the journal is where it begins.
            item {
                Spacer(Modifier.height(Spacing.md))
                com.agpeya.app.ui.common.NavRow(
                    title = s.confessionPrepTitle,
                    onClick = onStartConfessionPrep,
                    subtitle = "${s.confessionPrepDesc} · ${s.comingSoon}",
                )
            }

            // Discharging drafts is an action on the whole journal, not on one
            // entry, so it lives at the foot of the list rather than in a card.
            if (entries.any { it.isDraft }) {
                item {
                    Spacer(Modifier.height(Spacing.md))
                    TextButton(onClick = { confessing = true }) { Text(s.confessedAction) }
                }
            }
            item { Spacer(Modifier.height(Spacing.huge)) }
        }
    }

    if (confessing) {
        AlertDialog(
            onDismissRequest = { confessing = false },
            title = { Text(s.confessedAction) },
            text = { Text(s.confessedConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    confessing = false
                    scope.launch { JournalRepository.dischargeDrafts(context) }
                    // The one moment a ቀኖና is fresh in hand is on the way home
                    // from confessing, so it is offered here and nowhere else.
                    penancePrompt = true
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthStrip(
    year: Int,
    month: Int,
    written: Set<Int>,
    today: LocalDate,
    onDay: (Int) -> Unit,
) {
    val s = LocalStrings.current
    val days = remember(year, month) {
        if (month == 13) EthiopianDate.pagumeLength(year) else 30
    }
    val fastWash = sinqColors.hero.copy(alpha = 0.14f)
    val gold = MaterialTheme.colorScheme.secondary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val todayEth = remember(today) { EthiopianDate.from(today) }
    // One pass over the month, so the calendar work is not redone per cell.
    val fasting = remember(year, month, days) {
        (1..days).map { day ->
            runCatching {
                val date = EthiopianDate(year, month, day).toGregorian()
                FastingCalendar.fastOn(date) != null || FastingCalendar.isWeeklyFastDay(date)
            }.getOrDefault(false)
        }
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        (1..days).forEach { day ->
            val isWritten = day in written
            val isToday = todayEth.year == year && todayEth.month == month && todayEth.day == day
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            isWritten -> gold
                            fasting[day - 1] -> fastWash
                            else -> empty
                        },
                    )
                    .then(if (isToday) Modifier.border(1.dp, gold, RoundedCornerShape(2.dp)) else Modifier)
                    .clickable(enabled = isWritten, onClickLabel = s.journalTitle) { onDay(day) },
            )
        }
    }
}

/**
 * One entry in the month list: the day it belongs to, its first line, and — for
 * a ንስሐ draft — an unmistakable mark, since that is the one kind whose handling
 * differs.
 */
@Composable
private fun EntryRow(entry: JournalEntry, s: Strings, onClick: () -> Unit) {
    SinqCard(onClick = onClick) {
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
        Spacer(Modifier.height(Spacing.xs))
        Text(
            entry.preview,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        // The Church's day, which is most of why an old entry is worth reading.
        val context = listOfNotNull(
            entry.context.monthlyFeast,
            entry.context.fast,
        ).joinToString(" · ")
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
