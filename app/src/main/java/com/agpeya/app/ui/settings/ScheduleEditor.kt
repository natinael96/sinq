package com.agpeya.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.HolidayCalendar
import com.agpeya.app.model.HabitSchedule
import com.agpeya.app.ui.common.ListRow
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.Spacing
import java.time.LocalDate

/**
 * The cadence editor shared by every scheduled intention — ምጽዋት, ንስሐ, አስራት
 * and ስዕለት.
 *
 * Five rhythms, because that is the range people actually keep things on: a
 * weekday or several, every other day, a day of every ግእዝ month, a date once a
 * year, or a named feast. The last two exist for ስዕለት in particular: a vow is
 * usually promised on a saint's day, and the monthly day already carries that
 * saint's name from the bundled ስንክሳር, so choosing "ቀን ፲፱" and choosing
 * "ቅዱስ ገብርኤል" are the same act.
 *
 * Nothing here forces a feast, though. A promise kept on a date of one's own
 * choosing schedules exactly as well as one kept on ፋሲካ.
 */
@Composable
fun ScheduleRow(
    schedule: HabitSchedule,
    s: Strings,
    onChange: (HabitSchedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    ListRow(
        title = s.scheduleLabel,
        subtitle = scheduleSummary(schedule, s, context),
        onClick = { editing = true },
        modifier = modifier,
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

/**
 * The cadence in one line. A monthly day names the saint kept on it — "በየወሩ በ12
 * ቀን · ቅዱስ ሚካኤል ሊቀ መላእክት" says far more than the number alone.
 */
fun scheduleSummary(
    schedule: HabitSchedule,
    s: Strings,
    context: android.content.Context? = null,
): String = when (schedule.kind) {
    HabitSchedule.Kind.WEEKLY -> when {
        schedule.days.size == 7 -> s.daysSummaryDaily
        schedule.days.isEmpty() -> s.noDaySelected
        else -> schedule.days.sorted().joinToString(" ") { s.dayLabels[it - 1] }
    }
    HabitSchedule.Kind.EVERY_OTHER_DAY -> s.scheduleEveryOtherDay
    HabitSchedule.Kind.MONTHLY -> {
        val saint = context?.let { HolidayCalendar.monthlyOn(it, schedule.monthDay)?.primary }
        listOfNotNull(s.monthlyOnDay(schedule.monthDay), saint).joinToString(" · ")
    }
    HabitSchedule.Kind.YEARLY -> s.yearlyOn(
        s.ethMonths.getOrElse(schedule.monthNum - 1) { "" },
        schedule.monthDay,
    )
    HabitSchedule.Kind.FEAST -> HolidayCalendar.annualByKey(schedule.feastKey)
        ?.let { if (s.isAmharic) it.nameAm else it.nameEn }
        ?: s.chooseFeast
}

@Composable
private fun ScheduleEditorDialog(
    s: Strings,
    initial: HabitSchedule,
    onDismiss: () -> Unit,
    onSave: (HabitSchedule) -> Unit,
) {
    val context = LocalContext.current
    var kind by remember { mutableStateOf(initial.kind) }
    var days by remember { mutableStateOf(initial.days) }
    var monthDay by remember { mutableIntStateOf(initial.monthDay) }
    var monthNum by remember { mutableIntStateOf(initial.monthNum) }
    var feastKey by remember { mutableStateOf(initial.feastKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.scheduleLabel) },
        text = {
            Column {
                val kinds = listOf(
                    HabitSchedule.Kind.WEEKLY to s.scheduleWeekly,
                    HabitSchedule.Kind.EVERY_OTHER_DAY to s.scheduleEveryOtherDay,
                    HabitSchedule.Kind.MONTHLY to s.scheduleMonthly,
                    HabitSchedule.Kind.YEARLY to s.scheduleYearly,
                    HabitSchedule.Kind.FEAST to s.scheduleFeast,
                )
                // A dialog is narrower than a full screen: the cadence chips
                // don't fit on one line, so they wrap instead of being squeezed
                // to one letter per line.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    kinds.forEach { (k, label) ->
                        FilterChip(
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
                        Stepper(
                            label = s.monthlyOnDay(monthDay),
                            onDown = { monthDay = if (monthDay > 1) monthDay - 1 else 30 },
                            onUp = { monthDay = if (monthDay < 30) monthDay + 1 else 1 },
                        )
                        // The saint whose day this is, so the number stops
                        // being an arbitrary one.
                        HolidayCalendar.monthlyOn(context, monthDay)?.let { holiday ->
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                s.monthlyFeastOf(holiday.summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    HabitSchedule.Kind.YEARLY -> {
                        Stepper(
                            label = s.ethMonths.getOrElse(monthNum - 1) { "" },
                            onDown = { monthNum = if (monthNum > 1) monthNum - 1 else 13 },
                            onUp = { monthNum = if (monthNum < 13) monthNum + 1 else 1 },
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        // ጳጉሜ is five or six days long, so a date chosen there
                        // cannot run to 30 the way the other months do.
                        val maxDay = if (monthNum == 13) 5 else 30
                        Stepper(
                            label = "$monthDay",
                            onDown = { monthDay = if (monthDay > 1) monthDay - 1 else maxDay },
                            onUp = { monthDay = if (monthDay < maxDay) monthDay + 1 else 1 },
                        )
                    }
                    HabitSchedule.Kind.FEAST -> {
                        LazyColumn(Modifier.heightIn(max = 280.dp)) {
                            items(HolidayCalendar.annual, key = { it.key }) { holiday ->
                                val name = if (s.isAmharic) holiday.nameAm else holiday.nameEn
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .toggleable(
                                            value = feastKey == holiday.key,
                                            role = Role.RadioButton,
                                            onValueChange = { feastKey = holiday.key },
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = feastKey == holiday.key,
                                        onClick = { feastKey = holiday.key },
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(name, style = MaterialTheme.typography.bodyLarge)
                                        if (holiday.movable) {
                                            Text(
                                                s.feastMovableNote,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Two cadences can be saved in a state that would never fire: a
            // weekly with no day, and a feast with none chosen. Both are
            // blocked here rather than saved and silently never arming.
            val valid = when (kind) {
                HabitSchedule.Kind.WEEKLY -> days.isNotEmpty()
                HabitSchedule.Kind.FEAST -> feastKey.isNotBlank()
                else -> true
            }
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        initial.copy(
                            kind = kind,
                            days = days,
                            monthDay = monthDay.coerceIn(1, 30),
                            monthNum = monthNum.coerceIn(1, 13),
                            feastKey = feastKey,
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

/** A value with a −/+ on either side: month, or day of month. */
@Composable
private fun Stepper(label: String, onDown: () -> Unit, onUp: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onDown) { Text("−") }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onUp) { Text("+") }
    }
}

/**
 * One day of the week, as a small pill toggle. Its width comes from the row
 * that holds it (a seventh of the space) rather than from its own content, so
 * the full week always fits a dialog whatever the screen width or font scale;
 * a minimum height keeps the target comfortably tappable even when that
 * seventh is narrow, while still letting it grow with large font scales.
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
            .heightIn(min = 48.dp)
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
fun TimeRow(minute: Int, s: Strings, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var picking by remember { mutableStateOf(false) }
    ListRow(
        title = s.timeLabel,
        subtitle = "%02d:%02d".format(minute / 60, minute % 60),
        onClick = { picking = true },
        modifier = modifier,
    )
    if (picking) {
        val timeState = rememberTimePickerState(
            initialHour = minute / 60,
            initialMinute = minute % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(s.timeLabel) },
            text = { TimePicker(state = timeState) },
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
