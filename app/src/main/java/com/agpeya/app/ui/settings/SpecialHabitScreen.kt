package com.agpeya.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.HabitSchedule
import com.agpeya.app.model.SpecialReminder
import com.agpeya.app.reminders.SpecialHabit
import com.agpeya.app.reminders.SpecialHabitReminderScheduler
import com.agpeya.app.ui.common.ListRow
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/**
 * The dedicated page for one scheduled intention — ምጽዋት or ንስሐ. It now holds a
 * LIST of reminders: each has its own name, cadence, time, and on/off switch,
 * and shows when it will next fire. The intentions stay what they are —
 * reminders, not habits — so nothing here is recorded or shown as done.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SpecialHabitScreen(habit: SpecialHabit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    val reminders by when (habit) {
        SpecialHabit.ALMS -> SettingsRepository.almsReminders(context)
        SpecialHabit.REPENTANCE -> SettingsRepository.repentanceReminders(context)
    }.collectAsState(initial = emptyList())

    // Notification-only nudges, so switching one on asks for POST_NOTIFICATIONS.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* denial is reported by the Settings banner, not a second dialog */ }
    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Label edits don't change timing, so they save without re-arming alarms;
    // cadence/time/enabled edits do. Skipping the reschedule on every keystroke
    // also keeps the name field from thrashing the AlarmManager.
    fun persist(newList: List<SpecialReminder>, reschedule: Boolean = true) {
        scope.launch {
            when (habit) {
                SpecialHabit.ALMS -> SettingsRepository.setAlmsReminders(context, newList)
                SpecialHabit.REPENTANCE -> SettingsRepository.setRepentanceReminders(context, newList)
            }
            if (reschedule) SpecialHabitReminderScheduler.sync(context, habit)
        }
    }

    fun update(entry: SpecialReminder, reschedule: Boolean = true) =
        persist(reminders.map { if (it.id == entry.id) entry else it }, reschedule)
    fun delete(id: String) = persist(reminders.filterNot { it.id == id })
    fun add() {
        ensureNotificationPermission()
        val defaults = when (habit) {
            SpecialHabit.ALMS ->
                HabitSchedule.DEFAULT_ALMS to SettingsRepository.DEFAULT_ALMS_REMINDER_MIN
            SpecialHabit.REPENTANCE ->
                HabitSchedule.DEFAULT_REPENTANCE to SettingsRepository.DEFAULT_REPENTANCE_REMINDER_MIN
        }
        persist(
            reminders + SpecialReminder(
                id = UUID.randomUUID().toString(),
                schedule = defaults.first,
                minute = defaults.second,
                enabled = true,
            ),
        )
    }

    val title = when (habit) {
        SpecialHabit.ALMS -> s.settingsAlmsReminder
        SpecialHabit.REPENTANCE -> s.settingsRepentReminder
    }
    val description = when (habit) {
        SpecialHabit.ALMS -> s.settingsAlmsReminderDesc
        SpecialHabit.REPENTANCE -> s.settingsRepentReminderDesc
    }
    val nameHint = when (habit) {
        SpecialHabit.ALMS -> s.reminderNameHintAlms
        SpecialHabit.REPENTANCE -> s.reminderNameHintRepent
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = title, onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.lg))

            if (reminders.isEmpty()) {
                Text(
                    s.noSpecialReminders,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    reminders.forEach { entry ->
                        ReminderCard(
                            entry = entry,
                            s = s,
                            nameHint = nameHint,
                            onChange = { updated ->
                                if (updated.enabled) ensureNotificationPermission()
                                update(updated)
                            },
                            onLabelChange = { updated -> update(updated, reschedule = false) },
                            onDelete = { delete(entry.id) },
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }

            OutlinedButton(onClick = { add() }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text(s.addSpecialReminder)
            }
            Spacer(Modifier.height(Spacing.huge))
        }
    }
}

@Composable
private fun ReminderCard(
    entry: SpecialReminder,
    s: Strings,
    nameHint: String,
    onChange: (SpecialReminder) -> Unit,
    onLabelChange: (SpecialReminder) -> Unit,
    onDelete: () -> Unit,
) {
    // The card owns a working copy keyed to the entry id, so the name field,
    // switch, cadence and time stay mutually consistent no matter how the
    // persistence round-trip is timed — an edit is always based on the card's
    // own latest state, never a stale value from the async flow.
    var draft by remember(entry.id) { mutableStateOf(entry) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft.label,
                    onValueChange = { draft = draft.copy(label = it); onLabelChange(draft) },
                    singleLine = true,
                    label = { Text(s.reminderNameLabel) },
                    placeholder = { Text(nameHint) },
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
            draft.schedule.nextDueOnOrAfter(LocalDate.now())?.let { due ->
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    s.nextDue(formatEthiopian(due, s)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun ScheduleRow(schedule: HabitSchedule, s: Strings, onChange: (HabitSchedule) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    ListRow(
        title = s.scheduleLabel,
        subtitle = scheduleSummary(schedule, s),
        onClick = { editing = true },
    )
    if (editing) {
        ScheduleEditorDialog(
            s = s,
            initial = schedule,
            onDismiss = { editing = false },
            onSave = {
                editing = false
                onChange(it)
            },
        )
    }
}

private fun scheduleSummary(schedule: HabitSchedule, s: Strings): String = when (schedule.kind) {
    HabitSchedule.Kind.WEEKLY -> when {
        schedule.days.size == 7 -> s.daysSummaryDaily
        schedule.days.isEmpty() -> s.noDaySelected
        else -> schedule.days.sorted().joinToString(" ") { s.dayLabels[it - 1] }
    }
    HabitSchedule.Kind.EVERY_OTHER_DAY -> s.scheduleEveryOtherDay
    HabitSchedule.Kind.MONTHLY -> s.monthlyOnDay(schedule.monthDay)
}

/**
 * Cadence editor: weekly (with day chips), every other day, or monthly on an
 * Ethiopian month day. Choosing every-other-day anchors on today, so it is due
 * immediately and then alternates; an existing anchor is kept so re-saving
 * can't shift the rhythm.
 */
@Composable
private fun ScheduleEditorDialog(
    s: Strings,
    initial: HabitSchedule,
    onDismiss: () -> Unit,
    onSave: (HabitSchedule) -> Unit,
) {
    var kind by remember { mutableStateOf(initial.kind) }
    var days by remember { mutableStateOf(initial.days) }
    var monthDay by remember { mutableStateOf(initial.monthDay) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.scheduleLabel) },
        text = {
            Column {
                val kinds = listOf(
                    HabitSchedule.Kind.WEEKLY to s.scheduleWeekly,
                    HabitSchedule.Kind.EVERY_OTHER_DAY to s.scheduleEveryOtherDay,
                    HabitSchedule.Kind.MONTHLY to s.scheduleMonthly,
                )
                // A dialog is narrower than a full screen: the three cadence
                // chips don't fit on one line, so they wrap instead of being
                // squeezed to one letter per line.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    kinds.forEach { (k, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
                when (kind) {
                    HabitSchedule.Kind.WEEKLY -> {
                        Text(
                            text = s.daysLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        // Seven chips don't fit a dialog's width, and the last
                        // two (ቅ/እ — Saturday, Sunday) were being clipped off
                        // the edge. Each day takes an equal share of the row
                        // instead, so all seven are always reachable.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            s.dayLabels.forEachIndexed { index, label ->
                                val day = index + 1
                                DayToggle(
                                    label = label,
                                    selected = day in days,
                                    onToggle = { days = if (day in days) days - day else days + day },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    HabitSchedule.Kind.EVERY_OTHER_DAY -> {
                        // Nothing to configure: the rhythm starts today.
                    }
                    HabitSchedule.Kind.MONTHLY -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { monthDay = if (monthDay > 1) monthDay - 1 else 30 }) {
                                Text("−")
                            }
                            Text(
                                text = s.monthlyOnDay(monthDay),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                            )
                            TextButton(onClick = { monthDay = if (monthDay < 30) monthDay + 1 else 1 }) {
                                Text("+")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = kind != HabitSchedule.Kind.WEEKLY || days.isNotEmpty(),
                onClick = {
                    onSave(
                        initial.copy(
                            kind = kind,
                            days = days,
                            monthDay = monthDay.coerceIn(1, 30),
                            anchor = if (initial.kind == HabitSchedule.Kind.EVERY_OTHER_DAY &&
                                initial.anchor.isNotBlank()
                            ) {
                                initial.anchor
                            } else {
                                LocalDate.now().toString()
                            },
                        ),
                    )
                },
            ) { Text(s.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

/**
 * One day of the week, as a small pill toggle. Its width comes from the row
 * that holds it (a seventh of the space) rather than from its own content, so
 * the full week always fits a dialog whatever the screen width or font scale;
 * the height is fixed so the target stays comfortably tappable even when that
 * seventh is narrow.
 */
@Composable
private fun DayToggle(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
            )
            .border(
                width = 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = CircleShape,
            )
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

/**
 * When the reminder fires on its due day. Same clock dialog as the nightly
 * nudge's time row.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TimeRow(minute: Int, s: Strings, onChange: (Int) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    ListRow(
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
                    onChange(timeState.hour * 60 + timeState.minute)
                }) { Text(s.save) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text(s.cancel) }
            },
        )
    }
}
