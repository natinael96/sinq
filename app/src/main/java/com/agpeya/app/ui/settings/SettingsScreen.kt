package com.agpeya.app.ui.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.ThemeChoice
import com.agpeya.app.ui.common.AgpeyaBottomBar
import com.agpeya.app.ui.common.Tab
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onSelectTab: (Tab) -> Unit,
    onOpenModes: () -> Unit,
    onOpenCustomize: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    val theme by SettingsRepository.theme(context).collectAsState(initial = ThemeChoice.SYSTEM)
    val keepOn by SettingsRepository.keepScreenOn(context).collectAsState(initial = true)
    val language by SettingsRepository.language(context).collectAsState(initial = com.agpeya.app.data.Language.SYSTEM)
    val alarmAlert by SettingsRepository.alarmAlert(context)
        .collectAsState(initial = com.agpeya.app.data.AlarmAlert.SOUND_VIBRATE)
    val alarmSound by SettingsRepository.alarmSound(context)
        .collectAsState(initial = com.agpeya.app.data.AlarmSound.ALARM)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AgpeyaBottomBar(current = Tab.SETTINGS, onSelect = onSelectTab) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            item {
                Text(
                    text = s.settingsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(20.dp))
            }

            item {
                Text(
                    text = s.appearance,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(8.dp))
                val choices = listOf(
                    ThemeChoice.SYSTEM to s.themeSystem,
                    ThemeChoice.LIGHT to s.themeLight,
                    ThemeChoice.DARK to s.themeDark,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    choices.forEachIndexed { index, (choice, label) ->
                        SegmentedButton(
                            selected = theme == choice,
                            onClick = { scope.launch { SettingsRepository.setTheme(context, choice) } },
                            shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                        ) { Text(label) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                Text(
                    text = s.languageLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(8.dp))
                val langChoices = listOf(
                    com.agpeya.app.data.Language.SYSTEM to s.langSystem,
                    com.agpeya.app.data.Language.AMHARIC to s.langAmharic,
                    com.agpeya.app.data.Language.ENGLISH to s.langEnglish,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    langChoices.forEachIndexed { index, (choice, label) ->
                        SegmentedButton(
                            selected = language == choice,
                            onClick = { scope.launch { SettingsRepository.setLanguage(context, choice) } },
                            shape = SegmentedButtonDefaults.itemShape(index, langChoices.size),
                        ) { Text(label) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.keepScreenOn, style = MaterialTheme.typography.titleMedium)
                        Text(
                            s.keepScreenOnDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = keepOn,
                        onCheckedChange = { scope.launch { SettingsRepository.setKeepScreenOn(context, it) } },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text(
                    text = s.alarmSection,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(8.dp))
                DropdownSetting(
                    label = s.alarmSection,
                    current = when (alarmAlert) {
                        com.agpeya.app.data.AlarmAlert.SOUND_VIBRATE -> s.alertSoundVibrate
                        com.agpeya.app.data.AlarmAlert.SOUND_ONLY -> s.alertSoundOnly
                        com.agpeya.app.data.AlarmAlert.VIBRATE_ONLY -> s.alertVibrateOnly
                        com.agpeya.app.data.AlarmAlert.SILENT -> s.alertSilent
                    },
                    options = listOf(
                        com.agpeya.app.data.AlarmAlert.SOUND_VIBRATE to s.alertSoundVibrate,
                        com.agpeya.app.data.AlarmAlert.SOUND_ONLY to s.alertSoundOnly,
                        com.agpeya.app.data.AlarmAlert.VIBRATE_ONLY to s.alertVibrateOnly,
                        com.agpeya.app.data.AlarmAlert.SILENT to s.alertSilent,
                    ),
                    onSelect = { scope.launch { SettingsRepository.setAlarmAlert(context, it) } },
                )
                DropdownSetting(
                    label = s.soundLabel,
                    current = when (alarmSound) {
                        com.agpeya.app.data.AlarmSound.ALARM -> s.soundAlarm
                        com.agpeya.app.data.AlarmSound.RINGTONE -> s.soundRingtone
                        com.agpeya.app.data.AlarmSound.NOTIFICATION -> s.soundNotification
                    },
                    enabled = alarmAlert == com.agpeya.app.data.AlarmAlert.SOUND_VIBRATE ||
                        alarmAlert == com.agpeya.app.data.AlarmAlert.SOUND_ONLY,
                    options = listOf(
                        com.agpeya.app.data.AlarmSound.ALARM to s.soundAlarm,
                        com.agpeya.app.data.AlarmSound.RINGTONE to s.soundRingtone,
                        com.agpeya.app.data.AlarmSound.NOTIFICATION to s.soundNotification,
                    ),
                    onSelect = { scope.launch { SettingsRepository.setAlarmSound(context, it) } },
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                SettingsLink(s.customizePrayers, onOpenCustomize)
                SettingsLink(s.reminderModes, onOpenModes)
                SettingsLink(s.about, onOpenAbout)
            }
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
                .clickable(enabled = enabled) { expanded = true }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
            )
            Text(
                current,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
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

@Composable
private fun SettingsLink(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
