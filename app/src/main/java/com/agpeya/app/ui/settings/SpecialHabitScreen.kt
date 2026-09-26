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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
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
fun SpecialHabitScreen(
    habit: SpecialHabit,
    onBack: () -> Unit,
    onOpenConfessionPrep: (() -> Unit)? = null,
    onOpenPenance: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val action = com.agpeya.app.ui.common.rememberUserAction()
    var draftJson by rememberSaveable { mutableStateOf<String?>(null) }
    val draft = remember(draftJson) { draftJson?.let { kotlinx.serialization.json.Json.decodeFromString(SpecialReminder.serializer(), it) } }
    fun setDraft(value: SpecialReminder?) {
        draftJson = value?.let { kotlinx.serialization.json.Json.encodeToString(SpecialReminder.serializer(), it) }
    }
    var deleting by remember { mutableStateOf<SpecialReminder?>(null) }
    val s = LocalStrings.current

    val remindersLoad = com.agpeya.app.ui.common.rememberFlowLoad(habit) { when (habit) {
        SpecialHabit.ALMS -> SettingsRepository.almsReminders(context)
        SpecialHabit.REPENTANCE -> SettingsRepository.repentanceReminders(context)
        // ስዕለት and ቀኖና never route here — each carries a record of its own, so
        // each has its own page — but the branch keeps the `when` exhaustive.
        SpecialHabit.TITHE, SpecialHabit.VOW, SpecialHabit.PENANCE ->
            SettingsRepository.titheReminders(context)
    } }
    if (com.agpeya.app.ui.common.contentLoadScreen(remindersLoad, s.remindersSettingsTitle, onBack)) return
    val reminders = remindersLoad.value ?: return

    val today by com.agpeya.app.ui.common.rememberCurrentDate()
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    // Notification-only nudges, so switching one on asks for POST_NOTIFICATIONS.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionDenied = !granted }
    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Draft edits stay local until Save; one write then rebuilds the schedule.
    fun persist(newList: List<SpecialReminder>, onSaved: () -> Unit = {}) {
        action.run {
            when (habit) {
                SpecialHabit.ALMS -> SettingsRepository.setAlmsReminders(context, newList)
                SpecialHabit.REPENTANCE -> SettingsRepository.setRepentanceReminders(context, newList)
                SpecialHabit.TITHE, SpecialHabit.VOW, SpecialHabit.PENANCE ->
                    SettingsRepository.setTitheReminders(context, newList)
            }
            SpecialHabitReminderScheduler.sync(context, habit)
            onSaved()
        }
    }

    fun update(entry: SpecialReminder, onSaved: () -> Unit = {}) =
        persist(if (reminders.any { it.id == entry.id }) reminders.map { if (it.id == entry.id) entry else it }
            else reminders + entry, onSaved)
    fun add() {

        val defaults = when (habit) {
            SpecialHabit.ALMS ->
                HabitSchedule.DEFAULT_ALMS to SettingsRepository.DEFAULT_ALMS_REMINDER_MIN
            SpecialHabit.REPENTANCE ->
                HabitSchedule.DEFAULT_REPENTANCE to SettingsRepository.DEFAULT_REPENTANCE_REMINDER_MIN
            // A tithe is reckoned by the month, so a new one starts monthly
            // rather than on a weekday.
            SpecialHabit.TITHE, SpecialHabit.VOW, SpecialHabit.PENANCE ->
                HabitSchedule(kind = HabitSchedule.Kind.MONTHLY, monthDay = 1) to
                    SettingsRepository.DEFAULT_TITHE_REMINDER_MIN
        }
        setDraft(
            SpecialReminder(
                id = UUID.randomUUID().toString(),
                schedule = defaults.first,
                minute = defaults.second,
                enabled = false,
            ),
        )
    }

    val title = when (habit) {
        SpecialHabit.ALMS -> s.settingsAlmsReminder
        SpecialHabit.REPENTANCE -> s.settingsRepentReminder
        SpecialHabit.TITHE, SpecialHabit.VOW, SpecialHabit.PENANCE -> s.titheRemindersRow
    }
    val description = when (habit) {
        SpecialHabit.ALMS -> s.settingsAlmsReminderDesc
        SpecialHabit.REPENTANCE -> s.settingsRepentReminderDesc
        SpecialHabit.TITHE, SpecialHabit.VOW, SpecialHabit.PENANCE -> s.settingsTitheDesc
    }
    val nameHint = when (habit) {
        SpecialHabit.ALMS -> s.reminderNameHintAlms
        SpecialHabit.REPENTANCE -> s.reminderNameHintRepent
        SpecialHabit.TITHE, SpecialHabit.VOW, SpecialHabit.PENANCE -> s.reminderNameHintTithe
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
            if (permissionDenied) {
                Text(s.notifDisabledBody, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { com.agpeya.app.ui.common.openNotificationSettings(context) }) { Text(s.openSettings) }
            }

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
                        Card {
                            Column(Modifier.padding(Spacing.md)) {
                                com.agpeya.app.ui.common.ToggleRow(
                                    title = entry.label.ifBlank { title },
                                    checked = entry.enabled,
                                    onCheckedChange = { enabled ->
                                        if (!action.busy) {
                                            if (enabled) ensureNotificationPermission()
                                            update(entry.copy(enabled = enabled))
                                        }
                                    },
                                    subtitle = scheduleSummary(entry.schedule, s, context),
                                )
                                Text("%02d:%02d".format(entry.minute / 60, entry.minute % 60), style = MaterialTheme.typography.bodyMedium)
                                if (entry.enabled) entry.schedule.nextDueOnOrAfter(today)?.let { due ->
                                    Text(s.nextDue(formatEthiopian(due, s)), style = MaterialTheme.typography.labelMedium)
                                }
                                FlowRow {
                                    TextButton(enabled = !action.busy, onClick = { setDraft(entry) }) { Text(s.editPerson) }
                                    TextButton(enabled = !action.busy, onClick = { deleting = entry }) { Text(s.delete) }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }

            OutlinedButton(enabled = !action.busy, onClick = { add() }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text(s.addSpecialReminder)
            }

            // The ንስሐ page also opens the preparation that reminding is FOR:
            // the examination, and the ቀኖና carried home from it.
            if (habit == SpecialHabit.REPENTANCE) {
                Spacer(Modifier.height(Spacing.lg))
                onOpenConfessionPrep?.let {
                    com.agpeya.app.ui.common.NavRow(
                        title = s.confessionPrepTitle,
                        onClick = it,
                        subtitle = s.confessionPrepDesc,
                    )
                }
                onOpenPenance?.let {
                    com.agpeya.app.ui.common.NavRow(
                        title = s.settingsPenanceTitle,
                        onClick = it,
                        subtitle = s.settingsPenanceDesc,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.huge))
        }
    }
    draft?.let { value ->
        AlertDialog(
            onDismissRequest = { if (!action.busy) setDraft(null) },
            title = { Text(if (reminders.any { it.id == value.id }) s.editPerson else s.addSpecialReminder) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                    OutlinedTextField(
                        value = value.label,
                        onValueChange = { setDraft(value.copy(label = it)) },
                        label = { Text(s.reminderNameLabel) },
                        placeholder = { Text(nameHint) },
                        singleLine = true,
                        enabled = !action.busy,
                    )
                    ScheduleRow(value.schedule, s, { setDraft(value.copy(schedule = it)) })
                    TimeRow(value.minute, s, { setDraft(value.copy(minute = it)) })
                }
            },
            confirmButton = {
                TextButton(enabled = !action.busy, onClick = {
                    update(value) { setDraft(null) }
                }) { Text(s.save) }
            },
            dismissButton = {
                TextButton(enabled = !action.busy, onClick = { setDraft(null) }) { Text(s.cancel) }
            },
        )
    }
    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { if (!action.busy) deleting = null },
            title = { Text(s.delete) },
            text = { Text(entry.label.ifBlank { title }) },
            confirmButton = {
                TextButton(enabled = !action.busy, onClick = {
                    persist(reminders.filterNot { it.id == entry.id }) { deleting = null }
                }) { Text(s.delete) }
            },
            dismissButton = { TextButton(enabled = !action.busy, onClick = { deleting = null }) { Text(s.cancel) } },
        )
    }
}
