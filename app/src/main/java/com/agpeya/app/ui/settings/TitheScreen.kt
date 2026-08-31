package com.agpeya.app.ui.settings

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.OfferingRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.TitheEntry
import com.agpeya.app.model.TitheEntryKind
import com.agpeya.app.ui.common.EthiopianDate
import com.agpeya.app.ui.common.ListRow
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.formatEthiopianShort
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** Whether the reckoning is read a month at a time or a year at a time. */
private enum class Period { MONTH, YEAR }

/**
 * አስራት — the tithe.
 *
 * The page answers one question: what is still owed. Income recorded here is
 * only ever a means to that answer — the app works out the tenth (or whatever
 * share the person keeps) and sets it against what has actually been given.
 * Everything is read over an Ethiopian month or year, because that is the
 * calendar the obligation is kept on.
 *
 * There is no streak, no score, and no history chart. A tithe fully paid says
 * so once and then stops talking about it.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TitheScreen(onBack: () -> Unit, onOpenReminders: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    val entries by OfferingRepository.titheEntries(context).collectAsState(initial = emptyList())
    val percent by OfferingRepository.tithePercent(context)
        .collectAsState(initial = OfferingRepository.DEFAULT_TITHE_PERCENT)
    val storedCurrency by OfferingRepository.currency(context).collectAsState(initial = "")
    val reminders by SettingsRepository.titheReminders(context).collectAsState(initial = emptyList())
    val currency = storedCurrency.ifBlank { s.currencyDefault }

    val today = remember { LocalDate.now() }
    val todayEth = remember(today) { EthiopianDate.from(today) }
    var period by remember { mutableStateOf(Period.MONTH) }
    // How many periods back from today we are looking; 0 is the current one.
    var offset by remember { mutableIntStateOf(0) }

    // The window being read. Months walk backwards through the 13-month year,
    // so ጳጉሜ is included rather than skipped — a gift given in it still counts.
    val (year, month) = remember(period, offset, todayEth) {
        when (period) {
            Period.YEAR -> todayEth.year - offset to null
            Period.MONTH -> {
                val total = todayEth.year * 13 + (todayEth.month - 1) - offset
                total / 13 to (total % 13) + 1
            }
        }
    }
    val periodEntries = remember(entries, year, month) {
        if (month == null) {
            OfferingRepository.inEthiopianYear(entries, year)
        } else {
            OfferingRepository.inEthiopianMonth(entries, year, month)
        }
    }
    val reckoning = remember(periodEntries, percent) {
        OfferingRepository.reckon(periodEntries, percent)
    }
    val periodLabel = if (month == null) {
        "$year ${s.eraSuffix}"
    } else {
        "${s.ethMonths.getOrElse(month - 1) { "" }} $year"
    }

    var adding by remember { mutableStateOf<TitheEntryKind?>(null) }
    var editingPercent by remember { mutableStateOf(false) }
    var editingCurrency by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<TitheEntry?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.titheTitle, onBack = onBack) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            item {
                Text(
                    s.titheIntro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.lg))

                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(s.periodMonth, s.periodYear).forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = period.ordinal == index,
                            onClick = {
                                period = Period.entries[index]
                                // The offset counts periods, so keeping it
                                // across a switch would jump years back.
                                offset = 0
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                        ) { Text(label, maxLines = 1) }
                    }
                }
                Spacer(Modifier.height(Spacing.sm))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { offset += 1 }) { Text("← ${s.previousPeriod}") }
                    Text(
                        periodLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    // Nothing beyond the present to look at, so the forward
                    // step stops at the current period instead of scrolling
                    // through empty future months.
                    TextButton(onClick = { offset -= 1 }, enabled = offset > 0) {
                        Text("${s.nextPeriod} →")
                    }
                }
                Spacer(Modifier.height(Spacing.sm))

                // SinqCard already pads its content, so the column only sets
                // the rhythm between the two rows of figures.
                SinqCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        FigureRow(
                            leftLabel = s.titheIncome,
                            leftValue = formatCents(reckoning.income, currency),
                            rightLabel = "${s.titheDue} · $percent%",
                            rightValue = formatCents(reckoning.due, currency),
                        )
                        FigureRow(
                            leftLabel = s.titheGiven,
                            leftValue = formatCents(reckoning.given, currency),
                            // Giving beyond the tenth is shown as a surplus,
                            // not as a negative debt.
                            rightLabel = if (reckoning.owed < 0) s.titheSurplus else s.titheOwed,
                            rightValue = formatCents(
                                if (reckoning.owed < 0) -reckoning.owed else reckoning.owed,
                                currency,
                            ),
                            rightEmphasis = true,
                        )
                        if (reckoning.settled && reckoning.due > 0) {
                            Text(
                                s.titheSettledNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.md))

                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedButton(
                        onClick = { adding = TitheEntryKind.INCOME },
                        modifier = Modifier.weight(1f),
                    ) { Text(s.addIncome, maxLines = 1) }
                    OutlinedButton(
                        onClick = { adding = TitheEntryKind.GIVEN },
                        modifier = Modifier.weight(1f),
                    ) { Text(s.addGiven, maxLines = 1) }
                }
                Spacer(Modifier.height(Spacing.sm))

                ListRow(
                    title = s.tithePercentLabel,
                    subtitle = "$percent%",
                    onClick = { editingPercent = true },
                )
                ListRow(
                    title = s.currencyLabel,
                    subtitle = currency,
                    onClick = { editingCurrency = true },
                )
                NavRow(
                    title = s.titheRemindersRow,
                    subtitle = s.remindersOnCount(reminders.count { it.enabled }),
                    onClick = onOpenReminders,
                )

                Spacer(Modifier.height(Spacing.lg))
                Text(
                    s.titheLedgerHeader,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(Spacing.sm))
                if (periodEntries.isEmpty()) {
                    Text(
                        s.noTitheEntries,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(periodEntries, key = { it.id }) { entry ->
                LedgerRow(
                    entry = entry,
                    currency = currency,
                    label = if (entry.kind == TitheEntryKind.INCOME) s.incomeLabel else s.givenLabel,
                    dateLabel = entry.localDate?.let { formatEthiopianShort(it, s) } ?: entry.date,
                    deleteDescription = s.delete,
                    onDelete = { deleting = entry },
                )
            }

            item { Spacer(Modifier.height(Spacing.huge)) }
        }
    }

    adding?.let { kind ->
        AmountEntryDialog(
            title = if (kind == TitheEntryKind.INCOME) s.addIncome else s.addGiven,
            currency = currency,
            s = s,
            onDismiss = { adding = null },
            onSave = { amount, date, note ->
                adding = null
                scope.launch {
                    OfferingRepository.addTitheEntry(
                        context,
                        TitheEntry(
                            id = UUID.randomUUID().toString(),
                            kind = kind,
                            amount = amount,
                            date = date.toString(),
                            note = note,
                        ),
                    )
                }
            },
        )
    }

    if (editingPercent) {
        NumberEntryDialog(
            title = s.tithePercentLabel,
            initial = percent.toString(),
            s = s,
            onDismiss = { editingPercent = false },
            onSave = { text ->
                editingPercent = false
                text.toIntOrNull()?.let { scope.launch { OfferingRepository.setTithePercent(context, it) } }
            },
        )
    }

    if (editingCurrency) {
        TextEntryDialog(
            title = s.currencyLabel,
            initial = storedCurrency,
            s = s,
            onDismiss = { editingCurrency = false },
            onSave = { text ->
                editingCurrency = false
                scope.launch { OfferingRepository.setCurrency(context, text) }
            },
        )
    }

    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(s.delete) },
            text = { Text(formatCents(entry.amount, currency)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    scope.launch { OfferingRepository.deleteTitheEntry(context, entry.id) }
                }) { Text(s.delete) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(s.cancel) } },
        )
    }
}

/** One ledger line: what it was, when, how much, and a way to take it back. */
@Composable
private fun LedgerRow(
    entry: TitheEntry,
    currency: String,
    label: String,
    dateLabel: String,
    deleteDescription: String,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatCents(entry.amount, currency),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    listOf(label, dateLabel).joinToString(" · ") +
                        entry.note.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = deleteDescription,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** A whole number in a dialog — the tithe's percentage. */
@Composable
private fun NumberEntryDialog(
    title: String,
    initial: String,
    s: com.agpeya.app.ui.strings.Strings,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val valid = text.toIntOrNull()?.let { it in 1..100 } == true
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                isError = text.isNotBlank() && !valid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(text) }) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

/** A short free-text value in a dialog — the currency's name. */
@Composable
private fun TextEntryDialog(
    title: String,
    initial: String,
    s: com.agpeya.app.ui.strings.Strings,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 8) text = it },
                singleLine = true,
                placeholder = { Text(s.currencyDefault) },
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(s.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
