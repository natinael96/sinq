package com.agpeya.app.ui.journal

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
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

    var confessing by remember { mutableStateOf(false) }
    var settingPassphrase by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.journalTitle,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { settingPassphrase = true }) {
                        Icon(Icons.Outlined.Lock, contentDescription = s.journalSetPassphrase)
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
                }) { Text(s.confessedAction) }
            },
            dismissButton = { TextButton(onClick = { confessing = false }) { Text(s.cancel) } },
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
