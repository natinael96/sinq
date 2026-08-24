package com.agpeya.app.ui.modes

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agpeya.app.ui.common.SinqTopBar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.ModesRepository
import com.agpeya.app.model.ModesState
import com.agpeya.app.model.PrayerMode
import com.agpeya.app.reminders.ReminderScheduler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModesScreen(onBack: () -> Unit, onEditMode: (String) -> Unit, onOpenBatteryHelp: () -> Unit) {
    val context = LocalContext.current
    val state by ModesRepository.state(context)
        .collectAsState(initial = ModesState(ModesRepository.BUILT_IN_ID, emptyList()))
    val scope = rememberCoroutineScope()
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    var deleteCandidate by remember { mutableStateOf<PrayerMode?>(null) }

    suspend fun reschedule() {
        val names = com.agpeya.app.data.HoursRepository.visibleHours(context).associate { it.id to it.name }
        ReminderScheduler.rescheduleAll(context, names)
        com.agpeya.app.reminders.BreathPrayerScheduler.schedule(context)
    }

    Scaffold(
        topBar = {
            SinqTopBar(title = s.modesTitle, onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        ) {
            // Re-read on each (re)composition, so returning from settings reflects a change.
            val notificationsEnabled =
                androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            if (!notificationsEnabled) {
                item {
                    NotificationsOffBanner(
                        onOpenSettings = {
                            context.startActivity(
                                android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName),
                            )
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
            items(state.modes, key = { it.id }) { mode ->
                ModeRow(
                    mode = mode,
                    isActive = mode.id == state.activeModeId,
                    onActivate = {
                        scope.launch {
                            ModesRepository.setActiveMode(context, mode.id)
                            reschedule()
                        }
                    },
                    onOpen = { onEditMode(mode.id) },
                    onDelete = if (mode.isBuiltIn) null else ({ deleteCandidate = mode }),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item {
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = {
                        scope.launch {
                            val source = state.modes.find { it.isBuiltIn }
                            val mode = ModesRepository.addMode(
                                context,
                                name = s.newModeName,
                                copyFrom = source,
                            )
                            onEditMode(mode.id)
                        }
                    }) { Text(s.startFromAgpeya) }
                    TextButton(onClick = {
                        scope.launch {
                            val mode = ModesRepository.addMode(context, name = s.newModeName)
                            onEditMode(mode.id)
                        }
                    }) { Text(s.startEmpty) }
                }
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = s.remindersNotFiringTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenBatteryHelp)
                        .padding(vertical = 18.dp),
                )
            }
        }
    }

    deleteCandidate?.let { mode ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(s.deleteModeTitle) },
            text = { Text(s.deleteModeBody(mode.name, mode.entries.size)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        ModesRepository.deleteMode(context, mode.id)
                        reschedule()
                        deleteCandidate = null
                    }
                }) { Text(s.delete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text(s.cancel) }
            },
        )
    }
}

/** Shown on the Modes screen when the OS has notifications switched off for the app. */
@Composable
private fun NotificationsOffBanner(onOpenSettings: () -> Unit) {
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                s.notifDisabledTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                s.notifDisabledBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onOpenSettings) { Text(s.openSettings) }
        }
    }
}

@Composable
private fun ModeRow(
    mode: PrayerMode,
    isActive: Boolean,
    onActivate: () -> Unit,
    onOpen: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val s = com.agpeya.app.ui.strings.LocalStrings.current
        RadioButton(selected = isActive, onClick = onActivate)
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = mode.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            val enabledCount = mode.entries.count { it.enabled }
            Text(
                text = if (mode.isBuiltIn) "${s.builtInBadge} · ${s.remindersOn(enabledCount)}"
                else s.remindersOn(enabledCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = s.delete,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
