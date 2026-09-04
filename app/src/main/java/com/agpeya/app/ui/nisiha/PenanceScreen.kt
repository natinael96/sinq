package com.agpeya.app.ui.nisiha

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.agpeya.app.data.JournalLock
import com.agpeya.app.data.PenanceRepository
import com.agpeya.app.model.HabitSchedule
import com.agpeya.app.model.Penance
import com.agpeya.app.model.PenanceKind
import com.agpeya.app.model.PenanceProgress
import com.agpeya.app.reminders.SpecialHabit
import com.agpeya.app.reminders.SpecialHabitReminderScheduler
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.common.formatEthiopianShort
import com.agpeya.app.ui.journal.JournalLockGate
import com.agpeya.app.ui.journal.SecureScreen
import com.agpeya.app.ui.settings.ScheduleRow
import com.agpeya.app.ui.settings.TimeRow
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/**
 * ቀኖና — the penance received from one's ንስሐ አባት.
 *
 * Kept like a ስዕለት (a debt with a measure), guarded like the journal: the
 * screen sits behind the passphrase when one is set, stays out of screenshots,
 * and its records never enter a backup. Nothing here is a habit or a streak —
 * when a penance is finished the card says so plainly and the reminders stop,
 * and that is all.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PenanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    SecureScreen()
    val locked by JournalLock.isLocked(context).collectAsState(initial = false)
    var unlocked by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.penanceTitle, onBack = onBack) },
    ) { innerPadding ->
        if (locked && !unlocked) {
            Column(Modifier.fillMaxSize().padding(innerPadding)) {
                JournalLockGate(s) { unlocked = true }
            }
            return@Scaffold
        }

        val penances by PenanceRepository.penances(context).collectAsState(initial = emptyList())

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

        // Renaming changes nothing about when it is due, so it saves without
        // touching the AlarmManager; anything that moves the date, or settles
        // the penance, re-arms (and a settled one un-arms itself).
        fun persist(next: List<Penance>, reschedule: Boolean = true) {
            scope.launch {
                PenanceRepository.setPenances(context, next)
                if (reschedule) SpecialHabitReminderScheduler.sync(context, SpecialHabit.PENANCE)
            }
        }

        fun update(penance: Penance, reschedule: Boolean = true) =
            persist(penances.map { if (it.id == penance.id) penance else it }, reschedule)

        fun add() {
            ensureNotificationPermission()
            persist(
                penances + Penance(
                    id = UUID.randomUUID().toString(),
                    // A ቀኖና is usually kept daily until it is finished; the
                    // cadence has no daily kind, so every weekday stands in.
                    schedule = HabitSchedule(
                        kind = HabitSchedule.Kind.WEEKLY,
                        days = (1..7).toSet(),
                    ),
                    assignedDate = LocalDate.now().toString(),
                    enabled = true,
                ),
            )
        }

        var recording by remember { mutableStateOf<Penance?>(null) }
        var deleting by remember { mutableStateOf<Penance?>(null) }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Text(
                    s.penanceIntro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    s.penancePrivacyNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (penances.isEmpty()) {
                    Spacer(Modifier.height(Spacing.lg))
                    Text(
                        s.noPenances,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(penances, key = { it.id }) { penance ->
                PenanceCard(
                    penance = penance,
                    s = s,
                    onChange = { updated ->
                        if (updated.enabled) ensureNotificationPermission()
                        update(updated)
                    },
                    onLabelChange = { update(it, reschedule = false) },
                    onRecord = { recording = penance },
                    onDelete = { deleting = penance },
                )
            }

            item {
                OutlinedButton(onClick = { add() }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(s.penanceAdd)
                }
                Spacer(Modifier.height(Spacing.huge))
            }
        }

        recording?.let { penance ->
            ProgressDialog(
                s = s,
                onDismiss = { recording = null },
                onSave = { amount, note ->
                    recording = null
                    update(
                        penance.copy(
                            progress = penance.progress + PenanceProgress(
                                id = UUID.randomUUID().toString(),
                                date = LocalDate.now().toString(),
                                amount = amount,
                                note = note,
                            ),
                        ),
                    )
                },
            )
        }

        deleting?.let { penance ->
            AlertDialog(
                onDismissRequest = { deleting = null },
                title = { Text(penance.label.ifBlank { s.penanceTitle }) },
                text = { Text(s.deletePenanceConfirm) },
                confirmButton = {
                    TextButton(onClick = {
                        deleting = null
                        persist(penances.filterNot { it.id == penance.id })
                    }) { Text(s.delete) }
                },
                dismissButton = { TextButton(onClick = { deleting = null }) { Text(s.cancel) } },
            )
        }
    }
}

@Composable
private fun PenanceCard(
    penance: Penance,
    s: Strings,
    onChange: (Penance) -> Unit,
    onLabelChange: (Penance) -> Unit,
    onRecord: () -> Unit,
    onDelete: () -> Unit,
) {
    // The card owns a working copy keyed to the id, so the name field, switch,
    // cadence and time stay mutually consistent no matter how the persistence
    // round-trip is timed — the same guard the ስዕለት cards use.
    var draft by remember(penance.id) { mutableStateOf(penance) }
    // Progress is written by the dialog, not by this card, so it arrives
    // through the flow and must be folded into the working copy.
    if (draft.progress != penance.progress) draft = draft.copy(progress = penance.progress)

    var quotaEditing by remember { mutableStateOf(false) }
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
                    label = { Text(s.penanceNameLabel) },
                    placeholder = { Text(s.penanceNameHint) },
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

            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                for (kind in PenanceKind.entries) {
                    FilterChip(
                        selected = draft.kind == kind,
                        onClick = { draft = draft.copy(kind = kind); onChange(draft) },
                        label = { Text(kindName(kind, s)) },
                    )
                }
            }

            // Plain figures, no bar to fill: what was given and what remains is
            // stated the way a ledger states it, and a finished penance gets a
            // quiet ተፈጽሟል rather than a celebration.
            if (draft.settled) {
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        s.penanceSettled,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            } else if (draft.quota > 0) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "${s.penanceDone} ${draft.done} · ${s.penanceRemaining} ${draft.remaining}",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            com.agpeya.app.ui.common.ListRow(
                title = s.penanceQuotaLabel,
                subtitle = if (draft.quota > 0) draft.quota.toString() else "—",
                onClick = { quotaEditing = true },
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

            // A finished penance has no next date, and saying nothing is better
            // than showing a date it will never fire on.
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
                OutlinedButton(onClick = onRecord) { Text(s.penanceLogProgress) }
                if (draft.progress.isNotEmpty()) {
                    Spacer(Modifier.width(Spacing.sm))
                    TextButton(onClick = { showRecord = !showRecord }) {
                        Text("${s.penanceProgressHeader} (${draft.progress.size})")
                    }
                }
            }

            if (showRecord) {
                Spacer(Modifier.height(Spacing.xs))
                draft.progress.sortedByDescending { it.date }.forEach { done ->
                    Text(
                        listOfNotNull(
                            done.localDate?.let { formatEthiopianShort(it, s) } ?: done.date,
                            done.amount.takeIf { it > 0 }?.toString(),
                            done.note.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }

    if (quotaEditing) {
        QuotaDialog(
            initial = draft.quota,
            s = s,
            onDismiss = { quotaEditing = false },
            onSave = { quota ->
                quotaEditing = false
                draft = draft.copy(quota = quota)
                // The quota decides whether the penance is settled, so changing
                // it can revive or retire the alarm.
                onChange(draft)
            },
        )
    }
}

private fun kindName(kind: PenanceKind, s: Strings): String = when (kind) {
    PenanceKind.PROSTRATIONS -> s.penanceKindProstrations
    PenanceKind.FASTING_DAYS -> s.penanceKindFastingDays
    PenanceKind.ALMS -> s.penanceKindAlms
    PenanceKind.PRAYERS -> s.penanceKindPrayers
    PenanceKind.OTHER -> s.penanceKindOther
}

/** The measure given — a plain count, edited on its own since it is set once. */
@Composable
private fun QuotaDialog(
    initial: Int,
    s: Strings,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(if (initial > 0) initial.toString() else "") }
    val parsed = text.trim().toIntOrNull()?.takeIf { it >= 0 }
    // Blank clears the quota back to a penance with no count attached.
    val valid = text.isBlank() || parsed != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.penanceQuotaLabel) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(s.penanceQuotaLabel) },
                isError = !valid,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                ),
            )
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(parsed ?: 0) }) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

/** One session performed today: a count and, if wanted, a word about it. */
@Composable
private fun ProgressDialog(
    s: Strings,
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val parsed = amountText.trim().toIntOrNull()?.takeIf { it > 0 }
    // An unmeasured penance is recorded by the date alone, so blank is allowed.
    val valid = amountText.isBlank() || parsed != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.penanceLogProgress) },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    singleLine = true,
                    label = { Text(s.penanceQuotaLabel) },
                    isError = !valid,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    singleLine = true,
                    label = { Text(s.noteLabel) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(parsed ?: 0, note.trim()) }) {
                Text(s.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
