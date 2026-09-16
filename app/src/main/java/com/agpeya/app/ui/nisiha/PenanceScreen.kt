package com.agpeya.app.ui.nisiha

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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

/** Private penance summaries; configuration is a local draft until Save. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PenanceScreenContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val action = com.agpeya.app.ui.common.rememberUserAction()
    val load = com.agpeya.app.ui.common.rememberFlowLoad { PenanceRepository.penances(context) }
    if (com.agpeya.app.ui.common.contentLoadScreen(load, s.penanceTitle, onBack)) return
    val penances = load.value ?: return
    val today by com.agpeya.app.ui.common.rememberCurrentDate()
    var editing by remember { mutableStateOf<Penance?>(null) }
    var recording by remember { mutableStateOf<Penance?>(null) }
    var deleting by remember { mutableStateOf<Penance?>(null) }
    var removeRecord by remember { mutableStateOf<Pair<Penance, String>?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionDenied = !it
    }
    fun persist(next: List<Penance>, done: () -> Unit = {}) = action.run {
        PenanceRepository.setPenances(context, next)
        SpecialHabitReminderScheduler.sync(context, SpecialHabit.PENANCE)
        done()
    }
    fun update(value: Penance, done: () -> Unit = {}) = persist(
        if (penances.any { it.id == value.id }) penances.map { if (it.id == value.id) value else it }
        else penances + value, done,
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(s.penanceTitle, onBack) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.screen), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            item {
                Text(s.penanceIntro, style = MaterialTheme.typography.bodyMedium)
                Text(s.penancePrivacyNote, style = MaterialTheme.typography.bodySmall)
                if (permissionDenied) {
                    Text(s.notifDisabledBody, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { com.agpeya.app.ui.common.openNotificationSettings(context) }) { Text(s.openSettings) }
                }
                if (penances.isEmpty()) Text(s.noPenances)
            }
            items(penances, key = { it.id }) { penance ->
                var history by remember(penance.id) { mutableStateOf(false) }
                Card {
                    Column(Modifier.padding(Spacing.md)) {
                        com.agpeya.app.ui.common.ToggleRow(
                            title = penance.label.ifBlank { s.penanceTitle },
                            checked = penance.enabled,
                            subtitle = kindName(penance.kind, s),
                            onCheckedChange = { enabled ->
                                if (!action.busy) {
                                    if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    update(penance.copy(enabled = enabled))
                                }
                            },
                        )
                        Text(if (penance.settled) s.penanceSettled else if (penance.quota == 0) "${s.penanceQuotaLabel} —" else
                            "${s.penanceDone} ${penance.done} · ${s.penanceRemaining} ${penance.remaining}",
                            style = MaterialTheme.typography.titleSmall)
                        Text(com.agpeya.app.ui.settings.scheduleSummary(penance.schedule, s, context),
                            style = MaterialTheme.typography.bodySmall)
                        Text("%02d:%02d".format(penance.minute / 60, penance.minute % 60))
                        if (penance.remindsStill) penance.schedule.nextDueOnOrAfter(today)?.let {
                            Text(s.nextDue(formatEthiopian(it, s)), style = MaterialTheme.typography.bodySmall)
                        }
                        androidx.compose.foundation.layout.FlowRow {
                            TextButton(enabled = !action.busy, onClick = { recording = penance }) { Text(s.penanceLogProgress) }
                            TextButton(enabled = !action.busy, onClick = { editing = penance }) { Text(s.editPerson) }
                            TextButton(enabled = !action.busy, onClick = { deleting = penance }) { Text(s.delete) }
                            if (penance.progress.isNotEmpty()) TextButton(onClick = { history = !history }) {
                                Text("${s.penanceProgressHeader} (${penance.progress.size})")
                            }
                        }
                        if (history) penance.progress.sortedByDescending { it.date }.forEach { progress ->
                            Text(listOfNotNull(progress.localDate?.let { formatEthiopianShort(it, s) } ?: progress.date,
                                progress.amount.takeIf { it > 0 }?.toString(), progress.note.takeIf { it.isNotBlank() })
                                .joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                            TextButton(enabled = !action.busy, onClick = { removeRecord = penance to progress.id }) { Text(s.delete) }
                        }
                    }
                }
            }
            item {
                OutlinedButton(enabled = !action.busy, onClick = {
                    editing = Penance(UUID.randomUUID().toString(), enabled = false,
                        schedule = HabitSchedule(kind = HabitSchedule.Kind.WEEKLY, days = (1..7).toSet()),
                        assignedDate = today.toString())
                }) { Text(s.penanceAdd) }
                Spacer(Modifier.height(Spacing.huge))
            }
        }
    }
    editing?.let { value ->
        PenanceEditor(value, action.busy, onDismiss = { editing = null }, onSave = {
            update(it) { editing = null }
        })
    }
    recording?.let { value ->
        ProgressDialog(s, action.busy, onDismiss = { if (!action.busy) recording = null }, onSave = { amount, note ->
            update(value.copy(progress = value.progress + PenanceProgress(UUID.randomUUID().toString(),
                today.toString(), amount, note))) { recording = null }
        })
    }
    deleting?.let { value ->
        AlertDialog(onDismissRequest = { if (!action.busy) deleting = null },
            title = { Text(value.label.ifBlank { s.penanceTitle }) }, text = { Text(s.deletePenanceConfirm) },
            confirmButton = { TextButton(enabled = !action.busy, onClick = {
                persist(penances.filterNot { it.id == value.id }) { deleting = null }
            }) { Text(s.delete) } },
            dismissButton = { TextButton(enabled = !action.busy, onClick = { deleting = null }) { Text(s.cancel) } })
    }
    removeRecord?.let { (value, id) ->
        AlertDialog(onDismissRequest = { if (!action.busy) removeRecord = null },
            title = { Text(s.delete) }, text = { Text(s.deleteEntryConfirm) },
            confirmButton = { TextButton(enabled = !action.busy, onClick = {
                update(value.copy(progress = value.progress.filterNot { it.id == id })) { removeRecord = null }
            }) { Text(s.delete) } },
            dismissButton = { TextButton(enabled = !action.busy, onClick = { removeRecord = null }) { Text(s.cancel) } })
    }
}

@Composable
private fun PenanceEditor(value: Penance, busy: Boolean, onDismiss: () -> Unit, onSave: (Penance) -> Unit) {
    val s = LocalStrings.current
    var draft by remember(value.id) { mutableStateOf(value) }
    var quotaEditing by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(s.penanceTitle) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                OutlinedTextField(draft.label, { draft = draft.copy(label = it) }, enabled = !busy,
                    label = { Text(s.penanceNameLabel) }, placeholder = { Text(s.penanceNameHint) }, singleLine = true)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    PenanceKind.entries.forEach { kind ->
                        FilterChip(selected = draft.kind == kind, enabled = !busy,
                            onClick = { draft = draft.copy(kind = kind) }, label = { Text(kindName(kind, s)) })
                    }
                }
                com.agpeya.app.ui.common.ListRow(s.penanceQuotaLabel,
                    subtitle = draft.quota.takeIf { it > 0 }?.toString() ?: "—", onClick = { if (!busy) quotaEditing = true })
                ScheduleRow(draft.schedule, s, { if (!busy) draft = draft.copy(schedule = it) })
                TimeRow(draft.minute, s, { if (!busy) draft = draft.copy(minute = it) })
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = { onSave(draft) }) { Text(s.save) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(s.cancel) } })
    if (quotaEditing) QuotaDialog(draft.quota, s, { quotaEditing = false }, {
        draft = draft.copy(quota = it); quotaEditing = false
    })
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
    busy: Boolean,
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
            Column(Modifier.verticalScroll(rememberScrollState())) {
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
            TextButton(enabled = valid && !busy, onClick = { onSave(parsed ?: 0, note.trim()) }) {
                Text(s.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

@Composable
fun PenanceScreen(onBack: () -> Unit) {
    com.agpeya.app.ui.journal.JournalAccess(onBack) { PenanceScreenContent(onBack) }
}
