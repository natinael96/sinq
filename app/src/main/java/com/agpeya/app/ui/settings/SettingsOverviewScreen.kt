package com.agpeya.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.Language
import com.agpeya.app.data.PrayerLevel
import com.agpeya.app.data.ReadingFont
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.ThemeChoice
import com.agpeya.app.ui.common.AgpeyaBottomBar
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.common.Tab
import com.agpeya.app.ui.strings.LocalStrings
import kotlinx.coroutines.launch

/** The deliberately shallow Settings landing page: two direct choices and six doors. */
@Composable
fun SettingsScreen(
    onSelectTab: (Tab) -> Unit,
    onOpenReading: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenData: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val theme by SettingsRepository.theme(context).collectAsState(initial = ThemeChoice.SYSTEM)
    val language by SettingsRepository.language(context).collectAsState(initial = Language.SYSTEM)
    val font by SettingsRepository.readingFont(context).collectAsState(initial = ReadingFont.ABYSSINICA)
    val fontStep by SettingsRepository.fontStep(context).collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val prayerLevel by SettingsRepository.prayerLevel(context).collectAsState(initial = PrayerLevel.FULL)
    val night by SettingsRepository.streakReminder(context).collectAsState(initial = true)
    val gitsawe by SettingsRepository.gitsaweReminder(context).collectAsState(initial = true)
    val breath by SettingsRepository.breathReminder(context).collectAsState(initial = true)
    val alms by SettingsRepository.almsReminders(context).collectAsState(initial = emptyList())
    val repentance by SettingsRepository.repentanceReminders(context).collectAsState(initial = emptyList())
    val lastBackupAt by SettingsRepository.lastBackupAt(context).collectAsState(initial = 0L)
    val enabledCount = listOf(night, gitsawe, breath, alms.any { it.enabled }, repentance.any { it.enabled }).count { it }
    val size = SettingsRepository.FONT_STEPS_SP[fontStep.coerceIn(0, SettingsRepository.FONT_STEPS_SP.lastIndex)]

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AgpeyaBottomBar(Tab.SETTINGS, onSelectTab) },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                Text(s.settingsTitle, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(12.dp))
                CompactSegmented(
                    label = s.appearance,
                    options = listOf(s.themeSystem, s.themeLight, s.themeDark),
                    selected = theme.ordinal,
                    onSelect = { scope.launch { SettingsRepository.setTheme(context, ThemeChoice.entries[it]) } },
                )
                Spacer(Modifier.height(8.dp))
                CompactSegmented(
                    label = s.languageLabel,
                    options = listOf(s.langSystem, s.langAmharic, s.langEnglish),
                    selected = language.ordinal,
                    onSelect = { scope.launch { SettingsRepository.setLanguage(context, Language.entries[it]) } },
                )
                Spacer(Modifier.height(8.dp))
                NavRow(s.settingsGroupReading, onOpenReading, subtitle = "${fontLabel(font)} · ${size}sp")
                NavRow(s.prayerSettingsTitle, onOpenPrayer, subtitle = prayerLevelLabel(prayerLevel))
                NavRow(s.remindersSettingsTitle, onOpenReminders, subtitle = if (enabledCount == 0) s.remindersOff else s.remindersOn(enabledCount))
                NavRow(s.settingsGroupData, onOpenData, subtitle = backupRelativeLabel(lastBackupAt, s))
                NavRow(s.tutorial, onOpenTutorial)
                NavRow(s.about, onOpenAbout)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CompactSegmented(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        if (LocalDensity.current.fontScale > 1.5f) {
            options.forEachIndexed { index, text ->
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onSelect(index) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.RadioButton(selected == index, onClick = { onSelect(index) })
                    Text(text, style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            SingleChoiceSegmentedButtonRow {
                options.forEachIndexed { index, text ->
                    SegmentedButton(
                        selected = selected == index,
                        onClick = { onSelect(index) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        icon = { if (selected == index) Icon(Icons.Outlined.Check, contentDescription = null) },
                    ) { Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                }
            }
        }
    }
}

internal fun fontLabel(font: ReadingFont): String = when (font) {
    ReadingFont.ABYSSINICA -> "Abyssinica SIL"
    ReadingFont.ABAY_LIGHT -> "Ethiopic Abay Light"
    ReadingFont.BELA_BEREKA -> "Bela Bereka"
    ReadingFont.ZEMENAY -> "Zemenay"
}

internal fun prayerLevelLabel(level: PrayerLevel): String = when (level) {
    PrayerLevel.PSALM_50 -> "መዝሙር ፶"
    PrayerLevel.BEGINNING -> "መጀመሪያ"
    PrayerLevel.GROWTH -> "እድገት"
    PrayerLevel.STEADFAST -> "ጽናት"
    PrayerLevel.FULL -> "ሙሉ"
}

private fun backupRelativeLabel(epochMillis: Long, s: com.agpeya.app.ui.strings.Strings): String {
    if (epochMillis <= 0L) return s.noBackupYet
    val saved = java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val days = java.time.temporal.ChronoUnit.DAYS.between(saved, java.time.LocalDate.now())
    return when (days) {
        0L -> s.backedUpToday
        1L -> s.backedUpYesterday
        else -> s.backedUpDays(days)
    }
}
