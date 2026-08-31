package com.agpeya.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.agpeya.app.data.OfferingRepository
import com.agpeya.app.model.HabitSchedule
import com.agpeya.app.model.Vow
import com.agpeya.app.model.VowFulfilment
import com.agpeya.app.reminders.SpecialHabit
import com.agpeya.app.reminders.SpecialHabitReminderScheduler
import com.agpeya.app.ui.common.ListRow
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.ToggleRow
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.common.formatEthiopianShort
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/**
 * ስዕለት — vows and pledges.
 *
 * A vow is usually made to a saint and kept on that saint's day, so the cadence
 * editor here reaches the whole ወርኀዊ and ዓመታዊ calendar; but it is never forced
 * to, because promises are also made for a particular date, or for every
 * Sunday, or for no reason the calendar knows about.
 *
 * Unlike ምጽዋት, a vow IS recorded. That is not a change of heart about hiding
 * one's giving — it is that a vow is a debt a person chose to take on, and the
 * only way to know it has been discharged is to have written it down.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun VowScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    val vows by OfferingRepository.vows(context).collectAsState(initial = emptyList())
    val storedCurrency by OfferingRepository.currency(context).collectAsState(initial = "")
    val currency = storedCurrency.ifBlank { s.currencyDefault }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* a denial is reported by the Reminders banner, not a second dialog */ }
    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Renaming or annotating a vow changes nothing about when it is due, so it
    // saves without touching the AlarmManager; anything that moves the date, or
    // discharges a one-time vow, re-arms.
    fun persist(next: List<Vow>, reschedule: Boolean = true) {
        scope.launch {
            OfferingRepository.setVows(context, next)
            if (reschedule) SpecialHabitReminderScheduler.sync(context, SpecialHabit.VOW)
        }
    }

    fun update(vow: Vow, reschedule: Boolean = true) =
        persist(vows.map { if (it.id == vow.id) vow else it }, reschedule)

    fun add() {
        ensureNotificationPermission()
        persist(
            vows + Vow(
                id = UUID.randomUUID().toString(),
                // A new vow starts on the monthly commemoration of today's
                // Ethiopian day — the likeliest one to be promising something.
                schedule = HabitSchedule(
                    kind = HabitSchedule.Kind.MONTHLY,
                    monthDay = com.agpeya.app.ui.common.EthiopianDate.from(LocalDate.now()).day
                        .coerceIn(1, 30),
                ),
                enabled = true,
            ),
        )
    }

    var recording by remember { mutableStateOf<Vow?>(null) }
    var deleting by remember { mutableStateOf<Vow?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.vowsTitle, onBack = onBack) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Text(
                    s.vowsIntro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (vows.isEmpty()) {
                    Spacer(Modifier.height(Spacing.lg))
                    Text(
                        s.noVows,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(vows, key = { it.id }) { vow ->
                VowCard(
                    vow = vow,
                    currency = currency,
                    s = s,
                    onChange = { updated ->
                        if (updated.enabled) ensureNotificationPermission()
                        update(updated)
                    },
                    onLabelChange = { update(it, reschedule = false) },
                    onRecord = { recording = vow },
                    onDelete = { deleting = vow },
                )
            }

            item {
                OutlinedButton(onClick = { add() }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(s.addVow)
                }
                Spacer(Modifier.height(Spacing.huge))
            }
        }
    }

    recording?.let { vow ->
        AmountEntryDialog(
            title = s.recordPayment,
            currency = currency,
            s = s,
            // A vow of prayer or fasting is kept without a figure, so the
            // amount may be left blank and the record is the date alone.
            amountOptional = true,
            onDismiss = { recording = null },
            onSave = { amount, date, note ->
                recording = null
                update(
                    vow.copy(
                        fulfilments = vow.fulfilments + VowFulfilment(
                            id = UUID.randomUUID().toString(),
                            date = date.toString(),
                            amount = amount,
                            note = note,
                        ),
                    ),
                )
            },
        )
    }

    deleting?.let { vow ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(vow.label.ifBlank { s.untitledReminder }) },
            text = { Text(s.deleteVowConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    persist(vows.filterNot { it.id == vow.id })
                }) { Text(s.delete) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(s.cancel) } },
        )
    }
}

@Composable
private fun VowCard(
    vow: Vow,
    currency: String,
    s: Strings,
    onChange: (Vow) -> Unit,
    onLabelChange: (Vow) -> Unit,
    onRecord: () -> Unit,
    onDelete: () -> Unit,
) {
    // The card owns a working copy keyed to the vow's id, so the name field,
    // switch, cadence and time stay mutually consistent no matter how the
    // persistence round-trip is timed — the same guard the ምጽዋት cards use.
    var draft by remember(vow.id) { mutableStateOf(vow) }
    // Fulfilments are written by the dialog, not by this card, so they arrive
    // through the flow and must be folded into the working copy.
    if (draft.fulfilments != vow.fulfilments) draft = draft.copy(fulfilments = vow.fulfilments)

    var pledgeEditing by remember { mutableStateOf(false) }
    var showRecord by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft.label,
                    onValueChange = { draft = draft.copy(label = it); onLabelChange(draft) },
                    singleLine = true,
                    label = { Text(s.vowNameLabel) },
                    placeholder = { Text(s.vowNameHint) },
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = draft.enabled,
                    onCheckedChange = { draft = draft.copy(enabled = it); onChange(draft) },
                    modifier = Modifier.padding(start = Spacing.sm),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = s.delete,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // What was promised against what has been kept. Only shown for a
            // vow that named an amount — a promise of prayer has no bar to fill.
            if (draft.pledged > 0) {
                Spacer(Modifier.height(Spacing.sm))
                FigureRow(
                    leftLabel = s.vowGiven,
                    leftValue = formatCents(draft.given, currency),
                    rightLabel = if (draft.settled) s.vowSettled else s.vowRemaining,
                    rightValue = if (draft.settled) {
                        formatCents(draft.pledged, currency)
                    } else {
                        formatCents(draft.remaining, currency)
                    },
                    rightEmphasis = true,
                )
                Spacer(Modifier.height(Spacing.sm))
                LinearProgressIndicator(
                    progress = {
                        (draft.given.toFloat() / draft.pledged.toFloat()).coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (draft.settled) {
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        s.vowSettled,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            ListRow(
                title = s.vowPledgeLabel,
                subtitle = if (draft.pledged > 0) formatCents(draft.pledged, currency) else "—",
                onClick = { pledgeEditing = true },
            )
            ScheduleRow(
                schedule = draft.schedule,
                s = s,
                onChange = { draft = draft.copy(schedule = it); onChange(draft) },
            )
            TimeRow(
                minute = draft.minute,
                s = s,
                onChange = { draft = draft.copy(minute = it); onChange(draft) },
            )
            ToggleRow(
                title = s.vowOneTime,
                subtitle = s.vowOneTimeDesc,
                checked = draft.oneTime,
                onCheckedChange = { draft = draft.copy(oneTime = it); onChange(draft) },
            )

            // A discharged one-time vow has no next date, and saying so is
            // better than showing a date it will never fire on.
            if (draft.remindsStill) {
                draft.schedule.nextDueOnOrAfter(LocalDate.now())?.let { due ->
                    Text(
                        s.nextDue(formatEthiopian(due, s)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onRecord) { Text(s.recordPayment) }
                if (draft.fulfilments.isNotEmpty()) {
                    Spacer(Modifier.width(Spacing.sm))
                    TextButton(onClick = { showRecord = !showRecord }) {
                        Text("${s.vowPaymentsHeader} (${draft.fulfilments.size})")
                    }
                }
            }

            if (showRecord) {
                Spacer(Modifier.height(Spacing.xs))
                draft.fulfilments.sortedByDescending { it.date }.forEach { paid ->
                    Text(
                        listOfNotNull(
                            paid.localDate?.let { formatEthiopianShort(it, s) } ?: paid.date,
                            paid.amount.takeIf { it > 0 }?.let { formatCents(it, currency) },
                            paid.note.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }

    if (pledgeEditing) {
        PledgeDialog(
            initial = draft.pledged,
            currency = currency,
            s = s,
            onDismiss = { pledgeEditing = false },
            onSave = { amount ->
                pledgeEditing = false
                draft = draft.copy(pledged = amount)
                // The pledge decides whether a one-time vow is discharged, so
                // changing it can revive or retire the alarm.
                onChange(draft)
            },
        )
    }
}

/** The amount promised — editable on its own, since it is set once and rarely. */
@Composable
private fun PledgeDialog(
    initial: Long,
    currency: String,
    s: Strings,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var text by remember {
        mutableStateOf(if (initial > 0) "%.2f".format(initial / 100.0) else "")
    }
    val parsed = parseAmount(text)
    // Blank clears the pledge back to a vow with no figure attached.
    val valid = text.isBlank() || parsed != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.vowPledgeLabel) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(s.amountLabel) },
                suffix = { Text(currency) },
                isError = !valid,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                ),
            )
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(parsed ?: 0L) }) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
