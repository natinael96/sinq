package com.agpeya.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.HabitSchedule
import com.agpeya.app.reminders.SpecialHabit
import com.agpeya.app.reminders.SpecialHabitReminderScheduler
import com.agpeya.app.ui.common.ListRow
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.ToggleRow
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The dedicated page for one scheduled intention — ምጽዋት or ንስሐ. Everything
 * about the intention lives here (the toggle, its cadence, its time, and when
 * it will next fire); Settings holds only the door in. The intentions stay
 * what they are: reminders, not habits — nothing is recorded or shown as done.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SpecialHabitScreen(habit: SpecialHabit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val motion = LocalMotion.current

    val enabled by when (habit) {
        SpecialHabit.ALMS -> SettingsRepository.almsReminder(context)
        SpecialHabit.REPENTANCE -> SettingsRepository.repentanceReminder(context)
    }.collectAsState(initial = habit == SpecialHabit.REPENTANCE)
    val schedule by when (habit) {
        SpecialHabit.ALMS -> SettingsRepository.almsSchedule(context)
        SpecialHabit.REPENTANCE -> SettingsRepository.repentanceSchedule(context)
    }.collectAsState(
        initial = when (habit) {
            SpecialHabit.ALMS -> HabitSchedule.DEFAULT_ALMS
            SpecialHabit.REPENTANCE -> HabitSchedule.DEFAULT_REPENTANCE
        },
    )

    // Same rule as Settings: these nudges are notification-only, so switching
    // one on asks for POST_NOTIFICATIONS on the way in.
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

    val title = when (habit) {
        SpecialHabit.ALMS -> s.settingsAlmsReminder
        SpecialHabit.REPENTANCE -> s.settingsRepentReminder
    }
    val description = when (habit) {
        SpecialHabit.ALMS -> s.settingsAlmsReminderDesc
        SpecialHabit.REPENTANCE -> s.settingsRepentReminderDesc
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
            ToggleRow(
                title = title,
                checked = enabled,
                onCheckedChange = { on ->
                    if (on) ensureNotificationPermission()
                    scope.launch {
                        when (habit) {
                            SpecialHabit.ALMS -> SettingsRepository.setAlmsReminder(context, on)
                            SpecialHabit.REPENTANCE -> SettingsRepository.setRepentanceReminder(context, on)
                        }
                        SpecialHabitReminderScheduler.sync(context, habit, on)
                    }
                },
            )
            AnimatedVisibility(
                visible = enabled,
                enter = fadeIn(motion.spec(Motion.standard)) + expandVertically(motion.spec(Motion.standard)),
                exit = fadeOut(motion.spec(Motion.fast)) + shrinkVertically(motion.spec(Motion.fast)),
            ) {
                Column {
                    SpecialHabitScheduleRow(s, habit)
                    SpecialHabitTimeRow(s, habit)
                    // When the reminder will actually next arrive — the one
                    // line a dedicated page can afford that a Settings row
                    // couldn't. Ethiopian date, like every date in the app.
                    schedule.nextDueOnOrAfter(LocalDate.now())?.let { due ->
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            s.nextDue(formatEthiopian(due, s)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.huge))
        }
    }
}

@Composable
private fun SpecialHabitScheduleRow(s: Strings, habit: SpecialHabit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val schedule by when (habit) {
        SpecialHabit.ALMS -> SettingsRepository.almsSchedule(context)
        SpecialHabit.REPENTANCE -> SettingsRepository.repentanceSchedule(context)
    }.collectAsState(
        initial = when (habit) {
            SpecialHabit.ALMS -> HabitSchedule.DEFAULT_ALMS
            SpecialHabit.REPENTANCE -> HabitSchedule.DEFAULT_REPENTANCE
        },
    )
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
            onSave = { edited ->
                editing = false
                scope.launch {
                    when (habit) {
                        SpecialHabit.ALMS -> SettingsRepository.setAlmsSchedule(context, edited)
                        SpecialHabit.REPENTANCE -> SettingsRepository.setRepentanceSchedule(context, edited)
                    }
                    SpecialHabitReminderScheduler.sync(context, habit, true)
                }
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
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    kinds.forEach { (k, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(label) },
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
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            s.dayLabels.forEachIndexed { index, label ->
                                val day = index + 1
                                androidx.compose.material3.FilterChip(
                                    selected = day in days,
                                    onClick = { days = if (day in days) days - day else days + day },
                                    label = { Text(label) },
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
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
 * When the reminder fires on its due day. Same clock dialog as the nightly
 * nudge's time row; saving re-arms the alarm immediately.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SpecialHabitTimeRow(s: Strings, habit: SpecialHabit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val minute by when (habit) {
        SpecialHabit.ALMS -> SettingsRepository.almsReminderTime(context)
        SpecialHabit.REPENTANCE -> SettingsRepository.repentanceReminderTime(context)
    }.collectAsState(
        initial = when (habit) {
            SpecialHabit.ALMS -> SettingsRepository.DEFAULT_ALMS_REMINDER_MIN
            SpecialHabit.REPENTANCE -> SettingsRepository.DEFAULT_REPENTANCE_REMINDER_MIN
        },
    )
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
                    scope.launch {
                        val newMinute = timeState.hour * 60 + timeState.minute
                        when (habit) {
                            SpecialHabit.ALMS ->
                                SettingsRepository.setAlmsReminderTime(context, newMinute)
                            SpecialHabit.REPENTANCE ->
                                SettingsRepository.setRepentanceReminderTime(context, newMinute)
                        }
                        SpecialHabitReminderScheduler.sync(context, habit, true)
                    }
                }) { Text(s.save) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text(s.cancel) }
            },
        )
    }
}
