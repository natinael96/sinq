@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.agpeya.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatAlignJustify
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.PrayerLevel
import com.agpeya.app.data.ThemeChoice
import com.agpeya.app.ui.common.AgpeyaBottomBar
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.Tab
import com.agpeya.app.ui.common.ToggleRow
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion
import com.agpeya.app.ui.theme.Spacing
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner


/**
 * Shown when a notification-only reminder is switched on but the system will
 * not deliver it. Once POST_NOTIFICATIONS has been denied twice the runtime
 * prompt no longer appears, so the only way back is the system settings page.
 */
@Composable
private fun NotificationsOffBanner(onOpenSettings: () -> Unit) {
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(
                s.notifDisabledTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                s.notifDisabledBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(Spacing.xs))
            TextButton(onClick = onOpenSettings) { Text(s.openSettings) }
        }
    }
}

@Composable
private fun <T> DropdownSetting(
    label: String,
    current: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val alpha = if (enabled) 1f else 0.4f
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = enabled) { expanded = true }
                .padding(vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
                Text(
                    current,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

/** Label + current value; tapping opens a dialog with a single text field. */
@Composable
private fun EditableRow(
    label: String,
    value: String,
    emptyLabel: String,
    onSave: (String) -> Unit,
) {
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    var editing by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable { editing = true }
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.weight(1f))
        Text(
            text = value.ifBlank { emptyLabel },
            style = MaterialTheme.typography.bodyMedium,
            color = if (value.isBlank()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
    if (editing) {
        var text by remember(value) { mutableStateOf(value) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(label) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onSave(text)
                    editing = false
                }) { Text(s.save) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { editing = false }) { Text(s.cancel) }
            },
        )
    }
}

/** The bundled reader faces, each shown rendered in itself. */
private val FONT_CHOICES = listOf(
    com.agpeya.app.data.ReadingFont.ABYSSINICA to "Abyssinica SIL",
    com.agpeya.app.data.ReadingFont.ABAY_LIGHT to "Ethiopic Abay Light",
    com.agpeya.app.data.ReadingFont.BELA_BEREKA to "Bela Bereka",
    com.agpeya.app.data.ReadingFont.ZEMENAY to "Zemenay",
)

/** A sample line of the script the choice actually affects. */
private const val FONT_SAMPLE = "አቡነ ዘበሰማያት ፩፪፫"

@Composable
private fun ReadingFontPicker(
    selected: com.agpeya.app.data.ReadingFont,
    onSelect: (com.agpeya.app.data.ReadingFont) -> Unit,
) {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        FONT_CHOICES.forEach { (choice, name) ->
            val isSel = choice == selected
            androidx.compose.material3.Surface(
                onClick = { onSelect(choice) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = if (isSel) MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = FONT_SAMPLE,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = com.agpeya.app.ui.theme.readingFontFamily(choice),
                                fontSize = 21.sp,
                                lineHeight = 34.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isSel) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}

/** The face's own name, for the collapsed header. */
private fun fontDisplayName(font: com.agpeya.app.data.ReadingFont): String =
    FONT_CHOICES.firstOrNull { it.first == font }?.second ?: "Abyssinica SIL"

/**
 * Backup and restore of the things the user can't recover: Journey history,
 * bookmarks, highlights, personal lists, custom hours, modes and settings. Written through the system file picker — the app has
 * no network access, so a backup is simply a file the user keeps.
 *
 * Import shows what the file holds and how much of it is new here BEFORE
 * anything is written, because a restore is not something to discover after
 * the fact.
 */
@Composable
private fun BackupRows(s: com.agpeya.app.ui.strings.Strings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Headline plus an explanation. A failed backup or a rejected file has to
    // say what happened AND that nothing of the user's was lost — that second
    // half is the one people actually need.
    var message by remember { mutableStateOf<Pair<String, String?>?>(null) }
    // The file the user chose, held while they confirm the preview.
    var pending by remember { mutableStateOf<android.net.Uri?>(null) }
    var preview by remember { mutableStateOf<com.agpeya.app.data.BackupRepository.Summary?>(null) }
    // What to write, chosen before the file dialog opens. A backup is no longer
    // all-or-nothing: it can now carry money and the journal, and neither
    // should ride along unasked.
    var choosing by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(com.agpeya.app.data.BackupRepository.Selection()) }
    var askingPassphrase by remember { mutableStateOf(false) }
    val journalLocked by com.agpeya.app.data.JournalLock.isLocked(context)
        .collectAsState(initial = false)

    val createDoc = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val today = java.time.LocalDate.now().toString()
            val ok = com.agpeya.app.data.BackupRepository.writeTo(context, uri, today, selection)
            if (ok) SettingsRepository.setLastBackupAt(context, System.currentTimeMillis())
            message = if (ok) s.backupSaved to null else s.backupFailed to s.backupFailedBody
        }
    }

    val openDoc = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val summary = com.agpeya.app.data.BackupRepository.peek(context, uri)
            if (summary == null) {
                message = s.restoreFailed to s.restoreFailedBody
            } else {
                pending = uri
                preview = summary
            }
        }
    }

    NavRow(
        s.backupExport,
        onClick = { choosing = true },
        leadingIcon = Icons.Outlined.Upload,
    )
    NavRow(
        s.backupImport,
        onClick = { openDoc.launch(arrayOf("application/json", "text/plain", "*/*")) },
        leadingIcon = Icons.Outlined.Download,
    )

    // Preview: what's in the file, and what a restore would actually add.
    val summary = preview
    if (summary != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { preview = null; pending = null },
            title = { Text(s.restorePreviewTitle) },
            text = {
                Column {
                    if (summary.created.isNotBlank()) {
                        Text(
                            s.backupCreated(summary.created),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(s.backupContains(summary.days, summary.bookmarks, summary.highlights))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (summary.newDays == 0 && summary.newBookmarks == 0) s.restoreNothingNew
                        else s.restoreWillAdd(summary.newDays, summary.newBookmarks),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s.restoreMergeNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pending
                    preview = null
                    pending = null
                    if (uri != null) scope.launch {
                        val ok = com.agpeya.app.data.BackupRepository.restore(context, uri)
                        message = if (ok) s.restoreDone to null else s.restoreFailed to s.restoreFailedBody
                    }
                }) { Text(s.backupImport) }
            },
            dismissButton = {
                TextButton(onClick = { preview = null; pending = null }) { Text(s.cancel) }
            },
        )
    }

    fun launchWriter() = createDoc.launch("sinq-backup-${java.time.LocalDate.now()}.json")

    if (choosing) {
        ExportPickerDialog(
            s = s,
            selection = selection,
            onSelection = { selection = it },
            onDismiss = { choosing = false },
            onConfirm = {
                choosing = false
                // Including the journal means proving it is yours first. The
                // gate protects the act, not the file — the file is plaintext,
                // which the dialog says in as many words.
                if (selection.journal && journalLocked) askingPassphrase = true else launchWriter()
            },
        )
    }

    if (askingPassphrase) {
        com.agpeya.app.ui.journal.PassphrasePrompt(
            title = s.exportSectionJournal,
            body = s.exportJournalWarning,
            s = s,
            onDismiss = { askingPassphrase = false },
            onVerified = {
                askingPassphrase = false
                launchWriter()
            },
        )
    }

    message?.let { (headline, detail) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text(s.ok) } },
            title = { Text(headline) },
            text = { if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium) },
        )
    }
}

/**
 * What goes into the backup file.
 *
 * Everything is on by default except the journal, which has to be asked for:
 * a backup gets mailed to oneself and handed to relatives, and the journal is
 * the one thing in the app whose accidental inclusion would be a real harm.
 */
@Composable
private fun ExportPickerDialog(
    s: com.agpeya.app.ui.strings.Strings,
    selection: com.agpeya.app.data.BackupRepository.Selection,
    onSelection: (com.agpeya.app.data.BackupRepository.Selection) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val rows = listOf<Triple<String, Boolean, (Boolean) -> com.agpeya.app.data.BackupRepository.Selection>>(
        Triple(s.exportSectionHabits, selection.habits) { selection.copy(habits = it) },
        Triple(s.exportSectionBookmarks, selection.bookmarks) { selection.copy(bookmarks = it) },
        Triple(s.exportSectionHighlights, selection.highlights) { selection.copy(highlights = it) },
        Triple(s.exportSectionPrayerList, selection.prayerList) { selection.copy(prayerList = it) },
        Triple(s.exportSectionSetup, selection.setup) { selection.copy(setup = it) },
        Triple(s.exportSectionOfferings, selection.offerings) { selection.copy(offerings = it) },
        Triple(s.exportSectionJournal, selection.journal) { selection.copy(journal = it) },
    )
    val anyChosen = rows.any { it.second }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.exportChooseTitle) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    s.exportChooseBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                rows.forEach { (label, checked, toggle) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(MaterialTheme.shapes.small)
                            .toggleable(
                                value = checked,
                                role = androidx.compose.ui.semantics.Role.Checkbox,
                                onValueChange = { onSelection(toggle(it)) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(Modifier.width(Spacing.sm))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (selection.journal) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        s.exportJournalWarning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (!anyChosen) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        s.exportNothingChosen,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = anyChosen, onClick = onConfirm) { Text(s.continueAction) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

/**
 * When the nightly streak nudge fires. The same Material clock the prayer-mode
 * editor uses, in a dialog; saving re-arms the alarm immediately, so the change
 * takes effect tonight rather than after the old time fires once more.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun StreakReminderTimeRow(s: com.agpeya.app.ui.strings.Strings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val minute by SettingsRepository.streakReminderTime(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_STREAK_REMINDER_MIN)
    var picking by remember { mutableStateOf(false) }

    com.agpeya.app.ui.common.ListRow(
        title = s.timeLabel,
        subtitle = "%02d:%02d".format(minute / 60, minute % 60),
        onClick = { picking = true },
    )

    if (picking) {
        val timeState = androidx.compose.material3.rememberTimePickerState(
            initialHour = minute / 60,
            initialMinute = minute % 60,
            is24Hour = true,
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(s.timeLabel) },
            text = { androidx.compose.material3.TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    picking = false
                    scope.launch {
                        SettingsRepository.setStreakReminderTime(
                            context,
                            timeState.hour * 60 + timeState.minute,
                        )
                        com.agpeya.app.reminders.StreakReminderScheduler.sync(context, true)
                    }
                }) { Text(s.save) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text(s.cancel) }
            },
        )
    }
}

/**
 * A nightly window in which every reminder stays silent.
 *
 * Because the silence is now total — the ringing prayer alarms and all the
 * notification nudges alike — a reminder timed inside the window simply never
 * arrives. That is what was asked for, but it must not be invisible, so the row
 * counts the reminders it is swallowing and says so.
 */
@Composable
private fun QuietHoursRow(s: com.agpeya.app.ui.strings.Strings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val quiet by SettingsRepository.quietHours(context)
        .collectAsState(initial = com.agpeya.app.data.QuietHours())
    var editingStart by remember { mutableStateOf<Boolean?>(null) }

    fun fmt(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)

    // Every reminder whose firing time is known in advance. The ሕሊና prayer is
    // left out on purpose: its moment is drawn at random inside a daytime
    // window and it already defers itself to tomorrow when silenced, so there
    // is no single time to warn about.
    val streakOn by SettingsRepository.streakReminder(context).collectAsState(initial = true)
    val streakMinute by SettingsRepository.streakReminderTime(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_STREAK_REMINDER_MIN)
    val gitsaweOn by SettingsRepository.gitsaweReminder(context).collectAsState(initial = true)
    val alms by SettingsRepository.almsReminders(context).collectAsState(initial = emptyList())
    val repentance by SettingsRepository.repentanceReminders(context).collectAsState(initial = emptyList())
    val tithe by SettingsRepository.titheReminders(context).collectAsState(initial = emptyList())
    val vows by com.agpeya.app.data.OfferingRepository.vows(context).collectAsState(initial = emptyList())
    val modes by com.agpeya.app.data.ModesRepository.state(context)
        .collectAsState(initial = null as com.agpeya.app.model.ModesState?)

    val swallowed = if (!quiet.enabled) 0 else buildList {
        if (streakOn) add(streakMinute)
        val gitsaweTime = com.agpeya.app.reminders.GitsaweReminderScheduler.REMINDER_TIME
        if (gitsaweOn) add(gitsaweTime.hour * 60 + gitsaweTime.minute)
        (alms + repentance + tithe).filter { it.enabled }.forEach { add(it.minute) }
        vows.filter { it.remindsStill }.forEach { add(it.minute) }
        // Only the active mode arms alarms, so only its entries can be lost.
        modes?.activeMode?.entries?.filter { it.enabled }?.forEach { add(it.hour * 60 + it.minute) }
    }.count { quiet.covers(it) }

    // The shared ToggleRow, not a hand-rolled Row+Switch: the whole width
    // toggles, and TalkBack announces one switch with its state instead of an
    // unlabelled row followed by a stray control.
    ToggleRow(
        title = s.quietHours,
        subtitle = if (quiet.enabled) s.quietHoursRange(fmt(quiet.startMinute), fmt(quiet.endMinute))
        else s.quietHoursDesc,
        checked = quiet.enabled,
        onCheckedChange = { on ->
            scope.launch { SettingsRepository.setQuietHours(context, quiet.copy(enabled = on)) }
        },
    )
    if (quiet.enabled) {
        com.agpeya.app.ui.common.ListRow(
            title = s.startTimeLabel,
            subtitle = fmt(quiet.startMinute),
            onClick = { editingStart = true },
        )
        com.agpeya.app.ui.common.ListRow(
            title = s.endTimeLabel,
            subtitle = fmt(quiet.endMinute),
            onClick = { editingStart = false },
        )
        if (swallowed > 0) {
            Text(
                s.quietHoursConflict(swallowed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = Spacing.xs),
            )
        }
    }
    editingStart?.let { isStart ->
        val minute = if (isStart) quiet.startMinute else quiet.endMinute
        val state = androidx.compose.material3.rememberTimePickerState(
            initialHour = minute / 60,
            initialMinute = minute % 60,
            is24Hour = true,
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editingStart = null },
            title = { Text(if (isStart) s.startTimeLabel else s.endTimeLabel) },
            text = { androidx.compose.material3.TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val selected = state.hour * 60 + state.minute
                    editingStart = null
                    scope.launch {
                        SettingsRepository.setQuietHours(
                            context,
                            if (isStart) quiet.copy(startMinute = selected) else quiet.copy(endMinute = selected),
                        )
                    }
                }) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { editingStart = null }) { Text(s.cancel) } },
        )
    }
}

/** Reading-specific preferences, separated from the Settings landing page. */
@Composable
fun ReadingSettingsScreen(onBack: () -> Unit, onOpenFonts: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    val font by SettingsRepository.readingFont(context).collectAsState(initial = com.agpeya.app.data.ReadingFont.ABYSSINICA)
    val step by SettingsRepository.fontStep(context).collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val lineSpacing by SettingsRepository.readingLineSpacing(context)
        .collectAsState(initial = com.agpeya.app.data.ReadingLineSpacing.NORMAL)
    val readingAlignment by SettingsRepository.readingAlignment(context)
        .collectAsState(initial = com.agpeya.app.data.ReadingAlignment.JUSTIFIED)
    val keepOn by SettingsRepository.keepScreenOn(context).collectAsState(initial = true)
    val size = SettingsRepository.FONT_STEPS_SP[step.coerceIn(0, SettingsRepository.FONT_STEPS_SP.lastIndex)]
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { com.agpeya.app.ui.common.SinqTopBar(s.settingsGroupReading, onBack) },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "አቡነ ዘበሰማያት ስምከ ይትቀደስ።\nመንግሥትከ ትምጻእ።",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = com.agpeya.app.ui.theme.readingFontFamily(font),
                            fontSize = size.sp,
                            lineHeight = (size * lineSpacing.multiplier).sp,
                            textAlign = when (readingAlignment) {
                                com.agpeya.app.data.ReadingAlignment.JUSTIFIED -> androidx.compose.ui.text.style.TextAlign.Justify
                                com.agpeya.app.data.ReadingAlignment.LEFT -> androidx.compose.ui.text.style.TextAlign.Left
                                com.agpeya.app.data.ReadingAlignment.RIGHT -> androidx.compose.ui.text.style.TextAlign.Right
                                com.agpeya.app.data.ReadingAlignment.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
                            },
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(Spacing.lg),
                    )
                }
                Spacer(Modifier.height(Spacing.lg))
                NavRow(s.readingFontTitle, onOpenFonts, subtitle = com.agpeya.app.ui.settings.fontLabel(font))
                Text(s.fontSizeLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { scope.launch { SettingsRepository.setFontStep(context, step - 1) } },
                        enabled = step > 0,
                    ) { Text("A−") }
                    Text("${size}sp", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    TextButton(
                        onClick = { scope.launch { SettingsRepository.setFontStep(context, step + 1) } },
                        enabled = step < SettingsRepository.FONT_STEPS_SP.lastIndex,
                    ) { Text("A+") }
                }
                Spacer(Modifier.height(Spacing.md))
                Text(s.lineSpacingLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(Spacing.xs))
                androidx.compose.material3.SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val choices = listOf(
                        com.agpeya.app.data.ReadingLineSpacing.COMPACT to s.lineCompact,
                        com.agpeya.app.data.ReadingLineSpacing.NORMAL to s.lineNormal,
                        com.agpeya.app.data.ReadingLineSpacing.RELAXED to s.lineRelaxed,
                    )
                    choices.forEachIndexed { index, (choice, label) ->
                        SegmentedButton(
                            selected = lineSpacing == choice,
                            onClick = { scope.launch { SettingsRepository.setReadingLineSpacing(context, choice) } },
                            shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(index, choices.size),
                            icon = {
                                if (lineSpacing == choice) Icon(Icons.Outlined.Check, contentDescription = null)
                            },
                        ) { Text(label, maxLines = 1) }
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                Text(s.textAlignmentLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(Spacing.xs))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val choices = listOf(
                        Triple(com.agpeya.app.data.ReadingAlignment.JUSTIFIED, Icons.Outlined.FormatAlignJustify, s.alignJustified),
                        Triple(com.agpeya.app.data.ReadingAlignment.LEFT, Icons.AutoMirrored.Outlined.FormatAlignLeft, s.alignLeft),
                        Triple(com.agpeya.app.data.ReadingAlignment.RIGHT, Icons.AutoMirrored.Outlined.FormatAlignRight, s.alignRight),
                        Triple(com.agpeya.app.data.ReadingAlignment.CENTER, Icons.Outlined.FormatAlignCenter, s.alignCenter),
                    )
                    choices.forEachIndexed { index, (choice, icon, description) ->
                        SegmentedButton(
                            selected = readingAlignment == choice,
                            onClick = { scope.launch { SettingsRepository.setReadingAlignment(context, choice) } },
                            shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                            icon = {},
                        ) {
                            Icon(icon, contentDescription = description)
                        }
                    }
                }
                ToggleRow(
                    s.keepScreenOn,
                    keepOn,
                    { scope.launch { SettingsRepository.setKeepScreenOn(context, it) } },
                    subtitle = s.keepScreenOnDesc,
                )
            }
        }
    }
}

/** Prayer content and structure, without notification behavior. */
@Composable
fun PrayerSettingsScreen(onBack: () -> Unit, onOpenManageHours: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    val level by SettingsRepository.prayerLevel(context).collectAsState(initial = PrayerLevel.FULL)
    var levelSheetOpen by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { com.agpeya.app.ui.common.SinqTopBar(s.prayerSettingsTitle, onBack) },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                NavRow(s.prayerLevelTitle, { levelSheetOpen = true }, subtitle = com.agpeya.app.ui.settings.prayerLevelLabel(level))
                NavRow(s.manageHours, onOpenManageHours)
            }
        }
    }
    if (levelSheetOpen) {
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { levelSheetOpen = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
                Text(s.prayerLevelTitle, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(Spacing.sm))
                Text(s.prayerLevelDescription, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.md))
                PrayerLevel.entries.forEach { choice ->
                    SettingsRadioRow(
                        label = com.agpeya.app.ui.settings.prayerLevelLabel(choice),
                        selected = choice == level,
                        subtitle = com.agpeya.app.ui.settings.prayerLevelDetail(choice, s),
                    ) {
                        scope.launch { SettingsRepository.setPrayerLevel(context, choice) }
                    }
                }
            }
        }
    }
}

/** All notification and alarm behavior in one place. */
@Composable
fun RemindersSettingsScreen(
    onBack: () -> Unit,
    onOpenModes: () -> Unit,
    onOpenSpecialHabit: (com.agpeya.app.reminders.SpecialHabit) -> Unit,
    onOpenTithe: () -> Unit,
    onOpenVows: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    val streak by SettingsRepository.streakReminder(context).collectAsState(initial = true)
    val gitsawe by SettingsRepository.gitsaweReminder(context).collectAsState(initial = true)
    val breath by SettingsRepository.breathReminder(context).collectAsState(initial = true)
    val almsEntries by SettingsRepository.almsReminders(context).collectAsState(initial = emptyList())
    val repentanceEntries by SettingsRepository.repentanceReminders(context).collectAsState(initial = emptyList())
    val titheEntries by SettingsRepository.titheReminders(context).collectAsState(initial = emptyList())
    val vowEntries by com.agpeya.app.data.OfferingRepository.vows(context).collectAsState(initial = emptyList())
    val alert by SettingsRepository.alarmAlert(context).collectAsState(initial = com.agpeya.app.data.AlarmAlert.SOUND_VIBRATE)
    val sound by SettingsRepository.alarmSound(context).collectAsState(initial = com.agpeya.app.data.AlarmSound.ALARM)
    var soundSheetOpen by remember { mutableStateOf(false) }
    var permissionPulse by remember { mutableStateOf(0) }
    val lifecycleOwner = context as? LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionPulse++
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }
    val remindersOn = streak || gitsawe || breath ||
        almsEntries.any { it.enabled } || repentanceEntries.any { it.enabled } ||
        titheEntries.any { it.enabled } || vowEntries.any { it.remindsStill }
    @Suppress("UNUSED_VARIABLE") val refreshPermissions = permissionPulse
    val batteryRestricted = remindersOn &&
        !(context.getSystemService(android.os.PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) ?: true)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { com.agpeya.app.ui.common.SinqTopBar(s.remindersSettingsTitle, onBack) },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                if (!NotificationManagerCompat.from(context).areNotificationsEnabled() && remindersOn) {
                    NotificationsOffBanner {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName))
                    }
                    Spacer(Modifier.height(Spacing.md))
                }
                if (batteryRestricted) {
                    SettingsWarningPanel(
                        title = s.backgroundRestrictedTitle,
                        body = s.backgroundRestrictedBody,
                        action = s.allowBackground,
                    ) {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                    Spacer(Modifier.height(Spacing.md))
                }
                SectionHeader(s.remindersGroupDaily)
                ToggleRow(s.settingsNightReminder, streak, { on ->
                    if (on) requestNotifications()
                    scope.launch {
                        SettingsRepository.setStreakReminder(context, on)
                        com.agpeya.app.reminders.StreakReminderScheduler.sync(context, on)
                    }
                }, subtitle = s.settingsNightReminderDesc)
                if (streak) StreakReminderTimeRow(s)
                ToggleRow(s.settingsGitsaweReminder, gitsawe, { on ->
                    if (on) requestNotifications()
                    scope.launch {
                        SettingsRepository.setGitsaweReminder(context, on)
                        com.agpeya.app.reminders.GitsaweReminderScheduler.sync(context, on)
                    }
                }, subtitle = s.settingsGitsaweReminderDesc)
                ToggleRow(s.settingsBreathReminder, breath, { on ->
                    if (on) requestNotifications()
                    scope.launch {
                        SettingsRepository.setBreathReminder(context, on)
                        com.agpeya.app.reminders.BreathPrayerScheduler.sync(context, on)
                    }
                }, subtitle = s.settingsBreathReminderDesc)
                Spacer(Modifier.height(Spacing.lg))
                SectionHeader(s.remindersGroupGiving)
                NavRow(s.settingsAlmsReminder, { onOpenSpecialHabit(com.agpeya.app.reminders.SpecialHabit.ALMS) }, subtitle = s.settingsAlmsReminderDesc)
                NavRow(s.settingsRepentReminder, { onOpenSpecialHabit(com.agpeya.app.reminders.SpecialHabit.REPENTANCE) }, subtitle = s.settingsRepentReminderDesc)
                NavRow(s.settingsTitheTitle, onOpenTithe, subtitle = s.settingsTitheDesc)
                NavRow(s.settingsVowTitle, onOpenVows, subtitle = s.settingsVowDesc)
                Spacer(Modifier.height(Spacing.lg))
                SectionHeader(s.remindersGroupSound)
                NavRow(s.reminderModes, onOpenModes)
                val alertLabel = when (alert) {
                    com.agpeya.app.data.AlarmAlert.SOUND_VIBRATE -> s.alertSoundVibrate
                    com.agpeya.app.data.AlarmAlert.SOUND_ONLY -> s.alertSoundOnly
                    com.agpeya.app.data.AlarmAlert.VIBRATE_ONLY -> s.alertVibrateOnly
                    com.agpeya.app.data.AlarmAlert.SILENT -> s.alertSilent
                }
                val soundLabel = when (sound) {
                    com.agpeya.app.data.AlarmSound.ALARM -> s.soundAlarm
                    com.agpeya.app.data.AlarmSound.RINGTONE -> s.soundRingtone
                    com.agpeya.app.data.AlarmSound.NOTIFICATION -> s.soundNotification
                }
                NavRow(
                    title = s.alarmSection,
                    subtitle = if (alert == com.agpeya.app.data.AlarmAlert.SOUND_VIBRATE || alert == com.agpeya.app.data.AlarmAlert.SOUND_ONLY) "$alertLabel · $soundLabel" else alertLabel,
                    onClick = { soundSheetOpen = true },
                )
                QuietHoursRow(s)
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
    if (soundSheetOpen) {
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { soundSheetOpen = false }) {
            ReminderSoundSheetContent(
                alert = alert,
                sound = sound,
                onAlert = { scope.launch { SettingsRepository.setAlarmAlert(context, it) } },
                onSound = { scope.launch { SettingsRepository.setAlarmSound(context, it) } },
            )
        }
    }
}

@Composable
private fun SettingsWarningPanel(title: String, body: String, action: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(Spacing.xs))
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun ReminderSoundSheetContent(
    alert: com.agpeya.app.data.AlarmAlert,
    sound: com.agpeya.app.data.AlarmSound,
    onAlert: (com.agpeya.app.data.AlarmAlert) -> Unit,
    onSound: (com.agpeya.app.data.AlarmSound) -> Unit,
) {
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
        Text(s.alarmSection, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Spacing.md))
        listOf(
            com.agpeya.app.data.AlarmAlert.SOUND_VIBRATE to s.alertSoundVibrate,
            com.agpeya.app.data.AlarmAlert.SOUND_ONLY to s.alertSoundOnly,
            com.agpeya.app.data.AlarmAlert.VIBRATE_ONLY to s.alertVibrateOnly,
            com.agpeya.app.data.AlarmAlert.SILENT to s.alertSilent,
        ).forEach { (choice, label) -> SettingsRadioRow(label, choice == alert) { onAlert(choice) } }
        if (alert == com.agpeya.app.data.AlarmAlert.SOUND_VIBRATE || alert == com.agpeya.app.data.AlarmAlert.SOUND_ONLY) {
            Spacer(Modifier.height(Spacing.lg))
            Text(s.soundLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            listOf(
                com.agpeya.app.data.AlarmSound.ALARM to s.soundAlarm,
                com.agpeya.app.data.AlarmSound.RINGTONE to s.soundRingtone,
                com.agpeya.app.data.AlarmSound.NOTIFICATION to s.soundNotification,
            ).forEach { (choice, label) -> SettingsRadioRow(label, choice == sound) { onSound(choice) } }
        }
    }
}

@Composable
private fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(MaterialTheme.shapes.small).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(Spacing.sm))
        Column(Modifier.weight(1f).padding(vertical = Spacing.sm)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Local identity and recoverable user-created data. */
@Composable
fun DataSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    val name by SettingsRepository.profileName(context).collectAsState(initial = "")
    val christianName by SettingsRepository.christianName(context).collectAsState(initial = "")
    val lastBackupAt by SettingsRepository.lastBackupAt(context).collectAsState(initial = 0L)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { com.agpeya.app.ui.common.SinqTopBar(s.settingsGroupData, onBack) },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                EditableRow(s.yourNameLabel, name, s.addName) {
                    scope.launch { SettingsRepository.setProfileName(context, it) }
                }
                EditableRow(s.christianNameLabel, christianName, s.addChristianName) {
                    scope.launch { SettingsRepository.setChristianName(context, it) }
                }
                if (lastBackupAt > 0L) {
                    val saved = java.time.Instant.ofEpochMilli(lastBackupAt)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    Text(
                        "${s.lastBackupLabel}: ${com.agpeya.app.ui.common.formatEthiopian(saved, s)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BackupRows(s)
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

/** Four honest previews; each sample is rendered in the font it selects. */
@Composable
fun ReadingFontScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    val selected by SettingsRepository.readingFont(context).collectAsState(initial = com.agpeya.app.data.ReadingFont.ABYSSINICA)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { com.agpeya.app.ui.common.SinqTopBar(s.readingFontTitle, onBack) },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp, vertical = 12.dp)) {
            ReadingFontPicker(selected) { scope.launch { SettingsRepository.setReadingFont(context, it) } }
        }
    }
}
