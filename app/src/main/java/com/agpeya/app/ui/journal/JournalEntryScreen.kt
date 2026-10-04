package com.agpeya.app.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.JournalRepository
import com.agpeya.app.model.ChecklistItem
import com.agpeya.app.model.ChecklistParser
import com.agpeya.app.model.JournalEntry
import com.agpeya.app.model.JournalKind
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/** How long typing must pause before the entry is written. */
private const val AUTOSAVE_DELAY_MS = 1000L

/**
 * Writing, or re-reading, one entry.
 *
 * There is no Save button: the entry saves itself a moment after you stop
 * typing, and again as the screen closes. A tick in the header says when it is
 * safely down — without that, "is this saved?" is a question the screen gives
 * no way to answer, which is worse than a button would have been.
 *
 * An entry left blank is deleted rather than stored, so the month view never
 * claims a day was written on when it was not.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun JournalEntryScreenContent(
    editor: JournalEditorState,
    entryId: String?,
    initialKind: JournalKind,
    confessionOnly: Boolean,
    anchorRoute: String?,
    anchorLabel: String?,
    onBack: () -> Unit,
    /** Opens the passage the entry was written about. */
    onOpenAnchor: (route: String) -> Unit,
    targetDate: LocalDate? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    // No screenshots, and nothing in the recents thumbnail.
    SecureScreen()

    var entry by editor::entry
    var body by editor::body
    var kind by editor::kind
    var deleting by remember { mutableStateOf(false) }

    var loadFailed by remember { mutableStateOf(false) }
    var loadAttempt by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    // Load an existing entry, or mint a draft stamped with today's Church day.
    LaunchedEffect(entryId, loadAttempt) {
        if (entry != null) return@LaunchedEffect
        loadFailed = false
        try {
            val isTargetFuture = targetDate != null && targetDate.isAfter(LocalDate.now())
            val effectiveInitialKind = if (isTargetFuture) JournalKind.CHECKLIST else initialKind
            val loaded = if (entryId != null) {
                JournalRepository.byId(context, entryId) ?: error("Entry no longer exists")
            } else (if (confessionOnly) JournalRepository.latestConfessionDraft(context) else null) ?: JournalRepository.draft(
                context = context,
                date = targetDate ?: LocalDate.now(),
                kind = effectiveInitialKind,
                anchorRoute = anchorRoute,
                anchorLabel = anchorLabel,
            )
            entry = loaded
            body = loaded.body
            kind = if (loaded.localDate?.isAfter(LocalDate.now()) == true) JournalKind.CHECKLIST else loaded.kind
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { loadFailed = true }
    }

    // Saved a second after typing stops, so nothing depends on leaving the
    // screen cleanly: switching apps, a crash, or the system killing the
    // process all leave the text on disk. `saved` drives the header, because a
    // journal that saves invisibly is indistinguishable from one that does not.
    var saved by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var saveAttempt by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(entry, body, kind, saveAttempt) {
        val current = entry ?: return@LaunchedEffect
        if (body.trim() == current.body && kind == current.kind) return@LaunchedEffect
        saved = false
        delay(AUTOSAVE_DELAY_MS)
        saveFailed = false
        try {
            JournalRepository.save(context, current.copy(body = body.trim(), kind = kind))
            entry = current.copy(body = body.trim(), kind = kind)
            saved = true
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { saveFailed = true }
    }

    // The last write, as the screen goes. It must NOT use the screen's own
    // scope: Compose cancels a rememberCoroutineScope at exactly the moment
    // onDispose runs, so a save launched there raced the cancellation and could
    // lose the final keystrokes. saveDetached hands it to a process-lived scope.
    val latestBody by rememberUpdatedState(body)
    val latestKind by rememberUpdatedState(kind)
    val latestEntry by rememberUpdatedState(entry)
    DisposableEffect(Unit) {
        onDispose {
            val current = editor.entry ?: return@onDispose
            JournalRepository.saveDetached(
                context,
                current.copy(body = editor.body.trim(), kind = editor.kind),
            )
        }
    }

    val loaded = entry
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = if (confessionOnly) s.confessionPrepTitle else s.journalTitle,
                subtitle = if (confessionOnly) null else loaded?.localDate?.let { formatEthiopian(it, s) },
                // The fast the day carried, in the app's gold accent
                accentLine = if (confessionOnly) null else loaded?.context?.fast,
                onBack = onBack,
                actions = {
                    if (saved) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = s.entrySaved,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    if (loaded != null && (entryId != null || (confessionOnly && body.isNotBlank()))) {
                        IconButton(onClick = { deleting = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = s.delete,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { inner ->
        if (loaded == null) {
            if (loadFailed) com.agpeya.app.ui.common.StatePanel(
                title = s.contentUnavailable, actionLabel = s.retryAction,
                onAction = { loadAttempt++ }, modifier = Modifier.padding(inner),
            ) else com.agpeya.app.ui.common.LoadingPanel(Modifier.padding(inner))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            val isFutureDate = (loaded.localDate ?: targetDate ?: LocalDate.now()).isAfter(LocalDate.now())
            if (!confessionOnly) {
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
                    Spacer(Modifier.height(Spacing.xs))
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        val options = listOf(
                            JournalKind.REFLECTION to s.journalKindReflection,
                            JournalKind.PASSAGE to s.journalKindPassage,
                            JournalKind.CHECKLIST to s.journalKindChecklist,
                            JournalKind.CONFESSION_DRAFT to s.journalKindConfession,
                        )
                        items(options) { (choice, label) ->
                            FilterChip(
                                selected = kind == choice,
                                onClick = { kind = choice },
                                label = { Text(label, maxLines = 1) },
                                leadingIcon = {
                                    val icon = when (choice) {
                                        JournalKind.REFLECTION -> Icons.Outlined.EditNote
                                        JournalKind.PASSAGE -> Icons.Outlined.AutoStories
                                        JournalKind.CHECKLIST -> Icons.Outlined.Checklist
                                        JournalKind.CONFESSION_DRAFT -> Icons.Outlined.Lock
                                    }
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                                    selectedLabelColor = MaterialTheme.colorScheme.secondary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.secondary,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = kind == choice,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    selectedBorderColor = MaterialTheme.colorScheme.secondary,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.2.dp,
                                ),
                                shape = RoundedCornerShape(10.dp),
                            )
                        }
                    }
                }
            }
            // Said plainly, at the moment the kind is chosen, because it is the
            // one kind whose handling the person needs to be able to rely on.
            if (!confessionOnly && kind == JournalKind.CONFESSION_DRAFT) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    s.journalKindConfessionNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            // The passage this was written about, as a way back to it. The
            // route has always been stored on the entry; until now nothing
            // opened it, so a note about a verse could not return to the verse.
            if (!confessionOnly) loaded?.anchorLabel?.let { label ->
                Spacer(Modifier.height(Spacing.sm))
                val route = loaded?.anchorRoute
                com.agpeya.app.ui.common.ListRow(
                    title = label,
                    onClick = route?.let { { onOpenAnchor(it) } },
                ) {
                    if (route != null) {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.md))
            if (saveFailed) {
                Text(s.entrySaveFailed, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { saveAttempt++ }) { Text(s.retryAction) }
            } else Text(if (saved || body == loaded.body) s.entrySaved else s.entrySaving,
                style = MaterialTheme.typography.labelMedium)
            if (!confessionOnly && kind == JournalKind.CHECKLIST) {
                ChecklistEditor(
                    body = body,
                    onBodyChange = { body = it },
                    s = s,
                    entryId = entryId ?: loaded.id,
                    entryDate = loaded.localDate ?: targetDate ?: LocalDate.now(),
                )
            } else {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = { Text(if (confessionOnly) s.noteAction else s.entryBodyHint) },
                    minLines = if (confessionOnly) 12 else 1,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                )
            }
            Spacer(Modifier.height(Spacing.huge))
        }
    }

    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(s.delete) },
            text = { Text(s.deleteEntryConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false
                    val id = loaded?.id
                    // Clear the body first so the save-on-dispose cannot write
                    // the entry back after the delete has run.
                    body = ""
                    entry = null
                    scope.launch {
                        if (id != null) JournalRepository.delete(context, id)
                        onBack()
                    }
                }) { Text(s.delete) }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text(s.cancel) } },
        )
    }
}

@Composable
fun JournalEntryScreen(
    entryId: String?,
    initialKind: JournalKind,
    anchorRoute: String?,
    anchorLabel: String?,
    onBack: () -> Unit,
    onOpenAnchor: (String) -> Unit,
    confessionOnly: Boolean = false,
    targetDate: LocalDate? = null,
) {
    val isTargetFuture = targetDate != null && targetDate.isAfter(LocalDate.now())
    val effectiveKind = if (isTargetFuture) JournalKind.CHECKLIST else initialKind
    // Keep the draft in memory while the private UI is removed on relock.
    val editor = androidx.compose.runtime.saveable.rememberSaveable(entryId,
        saver = androidx.compose.runtime.saveable.Saver<JournalEditorState, String>(
            save = { state -> state.entry?.copy(body = state.body, kind = state.kind)?.let {
                kotlinx.serialization.json.Json.encodeToString(JournalEntry.serializer(), it)
            }.orEmpty() },
            restore = { encoded -> JournalEditorState(effectiveKind).apply {
                if (encoded.isNotEmpty()) {
                    entry = kotlinx.serialization.json.Json.decodeFromString(JournalEntry.serializer(), encoded)
                    body = entry!!.body; kind = entry!!.kind
                }
            } },
        ),
    ) { JournalEditorState(effectiveKind) }
    com.agpeya.app.ui.journal.JournalAccess(onBack) {
        JournalEntryScreenContent(editor, entryId, effectiveKind, confessionOnly, anchorRoute, anchorLabel, onBack, onOpenAnchor, targetDate)
    }
}

private class JournalEditorState(initialKind: JournalKind) {
    var entry by mutableStateOf<JournalEntry?>(null)
    var body by mutableStateOf("")
    var kind by mutableStateOf(initialKind)
}

/**
 * Customizable day-to-day checklist editor for spiritual tasks, prayers, and deeds.
 */
@Composable
private fun ChecklistEditor(
    body: String,
    onBodyChange: (String) -> Unit,
    s: Strings,
    entryId: String,
    entryDate: LocalDate = LocalDate.now(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val (items, note) = remember(body) { ChecklistParser.parse(body) }
    var newTaskText by remember { mutableStateOf("") }
    var selectedHour by remember { mutableStateOf<String?>("06:00") }
    var hasReminder by remember { mutableStateOf(false) }

    val doneCount = items.count { it.isDone }
    val totalCount = items.size
    val progress = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f
    val allDone = totalCount > 0 && doneCount == totalCount

    Column(modifier = modifier.fillMaxWidth()) {
        // ── 1. Progress and stats card ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .border(
                    width = 1.dp,
                    color = if (allDone) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = s.checklistProgress,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (totalCount == 0) "—" else "${geezNumeral(doneCount)} ከ ${geezNumeral(totalCount)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            if (allDone) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = s.checklistCompletedAll,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // ── 2. Checklist items ──────────────────────────────────────────────
        if (items.isEmpty()) {
            Text(
                text = s.checklistEmpty,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Spacing.sm),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEachIndexed { index, item ->
                    val taskId = "$entryId-$index"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (item.isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (item.isDone) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val updated = items.mapIndexed { i, it ->
                                    if (i == index) it.copy(isDone = !it.isDone) else it
                                }
                                onBodyChange(ChecklistParser.serialize(updated, note))
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val updated = items.mapIndexed { i, it ->
                                    if (i == index) it.copy(isDone = !it.isDone) else it
                                }
                                onBodyChange(ChecklistParser.serialize(updated, note))
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = if (item.isDone) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (item.isDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                                color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.onSurface,
                            )
                            if (item.scheduledHour != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = item.scheduledHour,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            }
                        }
                        // Reminder toggle button
                        IconButton(
                            onClick = {
                                val nextArmed = !item.hasReminder
                                val updated = items.mapIndexed { i, it ->
                                    if (i == index) it.copy(hasReminder = nextArmed) else it
                                }
                                onBodyChange(ChecklistParser.serialize(updated, note))
                                if (nextArmed) {
                                    com.agpeya.app.reminders.ChecklistReminderScheduler.schedule(
                                        context = context,
                                        taskId = taskId,
                                        taskText = item.text,
                                        scheduledHour = item.scheduledHour,
                                        date = entryDate,
                                        entryId = entryId,
                                    )
                                } else {
                                    com.agpeya.app.reminders.ChecklistReminderScheduler.cancel(
                                        context = context,
                                        taskId = taskId,
                                    )
                                }
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = if (item.hasReminder) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                                contentDescription = if (item.hasReminder) s.checklistAlarmOn else s.checklistAlarmOff,
                                tint = if (item.hasReminder) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                com.agpeya.app.reminders.ChecklistReminderScheduler.cancel(context, taskId)
                                val updated = items.filterIndexed { i, _ -> i != index }
                                onBodyChange(ChecklistParser.serialize(updated, note))
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = s.delete,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // ── 3. Add custom task field with flexible time & reminder ──────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val onAddTask = {
                if (newTaskText.isNotBlank()) {
                    val task = ChecklistItem(
                        text = newTaskText.trim(),
                        isDone = false,
                        scheduledHour = selectedHour,
                        hasReminder = hasReminder,
                    )
                    val updated = items + task
                    onBodyChange(ChecklistParser.serialize(updated, note))
                    if (hasReminder) {
                        val taskId = "$entryId-${items.size}"
                        com.agpeya.app.reminders.ChecklistReminderScheduler.schedule(
                            context = context,
                            taskId = taskId,
                            taskText = task.text,
                            scheduledHour = task.scheduledHour,
                            date = entryDate,
                            entryId = entryId,
                        )
                    }
                    newTaskText = ""
                }
            }

            OutlinedTextField(
                value = newTaskText,
                onValueChange = { newTaskText = it },
                placeholder = { Text(s.checklistAddItemHint) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddTask() }),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            )

            // When is it for? Custom time / presets
            Text(
                text = s.checklistWhenFor,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = selectedHour ?: "",
                    onValueChange = { selectedHour = it.ifBlank { null } },
                    placeholder = { Text("06:00 / ነግህ") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
                val presets = listOf(
                    "06:00" to s.checklistHourMorning,
                    "12:00" to s.checklistHourNoon,
                    "18:00" to s.checklistHourVespers,
                    "21:00" to s.checklistHourCompline,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1.6f),
                ) {
                    items(presets) { (timeVal, label) ->
                        FilterChip(
                            selected = selectedHour == timeVal,
                            onClick = { selectedHour = if (selectedHour == timeVal) null else timeVal },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }
            }

            // Wire reminder switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = if (hasReminder) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = if (hasReminder) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = s.checklistReminderSwitch,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Switch(
                    checked = hasReminder,
                    onCheckedChange = { hasReminder = it },
                )
            }

            // Add button
            Button(
                onClick = onAddTask,
                enabled = newTaskText.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(s.checklistAddItemHint)
            }
        }

        Spacer(Modifier.height(Spacing.md))

        // ── 4. Optional Reflection / Notes Section ──────────────────────────
        Text(
            text = s.checklistNotesHint,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))
        OutlinedTextField(
            value = note,
            onValueChange = { newNote ->
                onBodyChange(ChecklistParser.serialize(items, newNote))
            },
            placeholder = { Text(s.entryBodyHint) },
            minLines = 3,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            shape = RoundedCornerShape(10.dp),
        )
    }
}

