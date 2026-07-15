package com.agpeya.app.ui.modes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.ModesRepository
import com.agpeya.app.model.Hour
import com.agpeya.app.model.ModesState
import com.agpeya.app.model.ReminderEntry
import com.agpeya.app.reminders.ReminderScheduler
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeEditorScreen(modeId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    val state by ModesRepository.state(context)
        .collectAsState(initial = ModesState(ModesRepository.BUILT_IN_ID, emptyList()))
    val mode = state.modes.find { it.id == modeId }
    val hours by produceState<List<Hour>>(initialValue = emptyList()) {
        value = com.agpeya.app.data.HoursRepository.visibleHours(context)
    }
    val hourNames = remember(hours) { hours.associate { it.id to it.name } }
    var editing by remember { mutableStateOf<ReminderEntry?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled by the system; alarms work either way, notification needs grant */ }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    suspend fun reschedule() = ReminderScheduler.rescheduleAll(context, hourNames)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(mode?.name ?: "", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                actions = {
                    if (mode?.isBuiltIn == true) {
                        TextButton(onClick = {
                            scope.launch {
                                ModesRepository.resetBuiltIn(context)
                                reschedule()
                            }
                        }) { Text(s.resetTimes) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (mode == null) return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        ) {
            if (!mode.isBuiltIn) {
                item {
                    var name by remember(mode.id) { mutableStateOf(mode.name) }
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            scope.launch { ModesRepository.renameMode(context, mode.id, it) }
                        },
                        label = { Text(s.modeNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            items(
                // Hidden hours are synced out: their entries don't show here
                // (and the scheduler skips them too).
                mode.entries
                    .filter { it.hourId in hourNames }
                    .sortedWith(compareBy({ it.hour }, { it.minute })),
                key = { it.id },
            ) { entry ->
                EntryRow(
                    entry = entry,
                    hourName = hourNames[entry.hourId] ?: entry.hourId,
                    onToggle = { enabled ->
                        if (enabled) ensureNotificationPermission()
                        scope.launch {
                            ModesRepository.upsertEntry(context, mode.id, entry.copy(enabled = enabled))
                            reschedule()
                        }
                    },
                    onOpen = { editing = entry },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
            if (!mode.isBuiltIn) {
                item {
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = {
                        editing = ReminderEntry(
                            id = UUID.randomUUID().toString(),
                            hourId = hours.firstOrNull()?.id ?: "morning",
                            hour = 6,
                            minute = 0,
                        )
                    }) { Text(s.addReminder) }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    editing?.let { entry ->
        ModalBottomSheet(
            onDismissRequest = { editing = null },
            sheetState = sheetState,
        ) {
            EntryEditor(
                entry = entry,
                hours = hours,
                canPickHour = mode?.isBuiltIn != true,
                canDelete = mode?.isBuiltIn != true && mode?.entries?.any { it.id == entry.id } == true,
                onSave = { updated ->
                    if (updated.enabled) ensureNotificationPermission()
                    scope.launch {
                        ModesRepository.upsertEntry(context, modeId, updated)
                        reschedule()
                        editing = null
                    }
                },
                onDelete = {
                    scope.launch {
                        ModesRepository.deleteEntry(context, modeId, entry.id)
                        reschedule()
                        editing = null
                    }
                },
            )
        }
    }
}

@Composable
private fun EntryRow(
    entry: ReminderEntry,
    hourName: String,
    onToggle: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "%02d:%02d  ·  %s".format(entry.hour, entry.minute, hourName),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            val s = com.agpeya.app.ui.strings.LocalStrings.current
            Text(
                text = daysSummary(entry.days, s.dayLabels, s.daysSummaryDaily, s.noDaySelected),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = entry.enabled, onCheckedChange = onToggle)
    }
}

private fun daysSummary(days: Set<Int>, labels: List<String>, daily: String, none: String): String = when {
    days.size == 7 -> daily
    days.isEmpty() -> none
    else -> days.sorted().joinToString(" ") { labels[it - 1] }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryEditor(
    entry: ReminderEntry,
    hours: List<Hour>,
    canPickHour: Boolean,
    canDelete: Boolean,
    onSave: (ReminderEntry) -> Unit,
    onDelete: () -> Unit,
) {
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    var hourId by remember(entry.id) { mutableStateOf(entry.hourId) }
    var days by remember(entry.id) { mutableStateOf(entry.days) }
    val timeState = rememberTimePickerState(
        initialHour = entry.hour,
        initialMinute = entry.minute,
        is24Hour = true,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        if (canPickHour) {
            Text(
                text = s.prayerLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Column {
                hours.chunked(2).forEach { rowHours ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowHours.forEach { h ->
                            FilterChip(
                                selected = hourId == h.id,
                                onClick = { hourId = h.id },
                                label = { Text(h.name) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowHours.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Text(
            text = s.timeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        TimePicker(state = timeState)

        Text(
            text = s.daysLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            s.dayLabels.forEachIndexed { index, label ->
                val day = index + 1
                FilterChip(
                    selected = day in days,
                    onClick = {
                        days = if (day in days) days - day else days + day
                    },
                    label = { Text(label) },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { days = ReminderEntry.ALL_DAYS }) { Text(s.everyDay) }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (canDelete) {
                TextButton(onClick = onDelete) {
                    Text(s.delete, color = MaterialTheme.colorScheme.error)
                }
            } else {
                Spacer(Modifier.height(1.dp))
            }
            TextButton(
                enabled = days.isNotEmpty(),
                onClick = {
                    onSave(
                        entry.copy(
                            hourId = hourId,
                            hour = timeState.hour,
                            minute = timeState.minute,
                            days = days,
                            enabled = true,
                        )
                    )
                },
            ) { Text(s.save) }
        }
        Spacer(Modifier.height(36.dp))
    }
}
