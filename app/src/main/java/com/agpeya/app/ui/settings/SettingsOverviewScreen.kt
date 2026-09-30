package com.agpeya.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.AlarmAlert
import com.agpeya.app.data.Language
import com.agpeya.app.data.PrayerLevel
import com.agpeya.app.data.ReadingFont
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.ThemeChoice
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Settings landing page organized into three clean groups:
 * 1. Appearance & Reading (Theme, Language, Reading font, Text size, Copy format)
 * 2. Prayer & Reminders (Prayer rule, Alert style, Daily alarms)
 * 3. Data & About (Backup/Restore, What's new, Feedback, Rating, About)
 */
@Composable
fun SettingsScreen(
    onOpenReading: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val theme by SettingsRepository.theme(context).collectAsState(initial = ThemeChoice.SYSTEM)
    val language by SettingsRepository.language(context).collectAsState(initial = SettingsRepository.DEFAULT_LANGUAGE)
    val font by SettingsRepository.readingFont(context).collectAsState(initial = ReadingFont.ABYSSINICA)
    val fontStep by SettingsRepository.fontStep(context).collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val prayerLevel by SettingsRepository.prayerLevel(context).collectAsState(initial = PrayerLevel.FULL)
    val alert by SettingsRepository.alarmAlert(context).collectAsState(initial = AlarmAlert.SOUND_VIBRATE)
    val lastBackupAt by SettingsRepository.lastBackupAt(context).collectAsState(initial = 0L)
    val today by com.agpeya.app.ui.common.rememberCurrentDate()
    val schedule by remember(today) { com.agpeya.app.data.DaySchedule.observe(context, today) }
        .collectAsState(initial = emptyList())
    val enabledCount = schedule.count { it.today }
    val size = SettingsRepository.FONT_STEPS_SP[fontStep.coerceIn(0, SettingsRepository.FONT_STEPS_SP.lastIndex)]

    var showFontDialog by remember { mutableStateOf(false) }
    var showLevelDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                Text(s.settingsTitle, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(Spacing.md))

                // Group 1: ገጽታና ንባብ (Appearance & Reading)
                SectionHeader(s.settingsGroupReading)
                CompactSegmented(
                    label = s.appearance,
                    options = listOf(s.themeSystem, s.themeLight, s.themeDark),
                    selected = theme.ordinal,
                    onSelect = { scope.launch { SettingsRepository.setTheme(context, ThemeChoice.entries[it]) } },
                )
                Spacer(Modifier.height(Spacing.sm))
                CompactSegmented(
                    label = s.languageLabel,
                    options = listOf(s.langSystem, s.langAmharic, s.langEnglish),
                    selected = language.ordinal,
                    onSelect = { scope.launch { SettingsRepository.setLanguage(context, Language.entries[it]) } },
                )
                Spacer(Modifier.height(Spacing.xs))
                NavRow(
                    s.readingFontTitle,
                    onClick = { showFontDialog = true },
                    subtitle = fontLabel(font),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        s.fontSizeLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (fontStep > 0) scope.launch { SettingsRepository.setFontStep(context, fontStep - 1) }
                            },
                            enabled = fontStep > 0,
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = null)
                        }
                        Text(
                            "${size}sp",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        IconButton(
                            onClick = {
                                if (fontStep < SettingsRepository.FONT_STEPS_SP.lastIndex) {
                                    scope.launch { SettingsRepository.setFontStep(context, fontStep + 1) }
                                }
                            },
                            enabled = fontStep < SettingsRepository.FONT_STEPS_SP.lastIndex,
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                        }
                    }
                }
                NavRow(s.copyFormatTitle, onOpenReading)

                Spacer(Modifier.height(Spacing.lg))

                // Group 2: ጸሎትና ማስታወሻ (Prayer & Reminders)
                SectionHeader(s.settingsGroupPrayer)
                NavRow(
                    s.prayerLevelTitle,
                    onClick = { showLevelDialog = true },
                    subtitle = prayerLevelLabel(prayerLevel, s),
                )
                Spacer(Modifier.height(Spacing.xs))
                val alertIndex = when (alert) {
                    AlarmAlert.SOUND_VIBRATE -> 0
                    AlarmAlert.VIBRATE_ONLY -> 1
                    AlarmAlert.SILENT -> 2
                    AlarmAlert.SOUND_ONLY -> 0
                }
                CompactSegmented(
                    label = s.alertChoiceTitle,
                    options = if (s is com.agpeya.app.ui.strings.AmharicStrings) listOf("ደወል", "ንዝረት", "ማሳወቂያ")
                    else listOf("Alarm", "Vibrate", "Silent"),
                    selected = alertIndex,
                    onSelect = { idx ->
                        val choice = when (idx) {
                            0 -> AlarmAlert.SOUND_VIBRATE
                            1 -> AlarmAlert.VIBRATE_ONLY
                            else -> AlarmAlert.SILENT
                        }
                        scope.launch { SettingsRepository.setAlarmAlert(context, choice) }
                    },
                )
                Spacer(Modifier.height(Spacing.xs))
                NavRow(s.remindersSettingsTitle, onOpenReminders, subtitle = "${s.todayLabel}: $enabledCount")

                Spacer(Modifier.height(Spacing.lg))

                // Group 3: መረጃና ስለ መተግበሪያው (Data & About)
                SectionHeader(s.settingsGroupRecords)
                NavRow(
                    s.settingsGroupRecords,
                    onOpenRecords,
                    subtitle = "${s.settingsGroupRecordsDesc} · ${backupRelativeLabel(lastBackupAt, s)}",
                )
                NavRow(s.tutorial, onOpenTutorial)
                NavRow(s.whatsNew, onOpenChangelog, subtitle = "v${appVersion(context)}")
                NavRow(
                    s.rateAppTitle,
                    { com.agpeya.app.ui.common.openPlayStore(context) },
                    subtitle = s.rateAppSubtitle,
                )
                NavRow(
                    s.feedbackTitle,
                    { com.agpeya.app.ui.common.openUrl(context, com.agpeya.app.ui.common.FEEDBACK_URL) },
                    subtitle = s.feedbackSubtitle,
                )
                NavRow(s.about, onOpenAbout)
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    "ስንቅ · v${appVersion(context)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.sm))
            }
        }
    }

    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text(s.readingFontTitle) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ReadingFontPicker(
                        selected = font,
                        onSelect = {
                            scope.launch { SettingsRepository.setReadingFont(context, it) }
                            showFontDialog = false
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) { Text(s.ok) }
            },
        )
    }

    if (showLevelDialog) {
        val levels = listOf(
            PrayerLevel.BEGINNING,
            PrayerLevel.GROWTH,
            PrayerLevel.STEADFAST,
            PrayerLevel.FULL,
            PrayerLevel.PSALM_50,
        )
        AlertDialog(
            onDismissRequest = { showLevelDialog = false },
            title = { Text(s.prayerLevelTitle) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    levels.forEach { choice ->
                        val isSel = choice == prayerLevel
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSel,
                                    role = androidx.compose.ui.semantics.Role.RadioButton,
                                    onClick = {
                                        scope.launch { SettingsRepository.setPrayerLevel(context, choice) }
                                        showLevelDialog = false
                                    },
                                ),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSel) MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
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
                                        prayerLevelLabel(choice, s),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        prayerLevelDetail(choice, s),
                                        style = MaterialTheme.typography.bodySmall,
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
            },
            confirmButton = {
                TextButton(onClick = { showLevelDialog = false }) { Text(s.ok) }
            },
        )
    }
}

/** The installed versionName, straight from the package — never a hardcoded copy. */
private fun appVersion(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: ""

/**
 * A label and its choice on one line, which is 48 dp rather than the 62 dp the
 * stacked version cost. Above a font scale of 1.5 it still stacks and becomes a
 * radio list, because at that size neither the label nor the segments fit
 * beside each other.
 */
@Composable
private fun CompactSegmented(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    if (LocalDensity.current.fontScale <= 1.5f) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                options.forEachIndexed { index, text ->
                    SegmentedButton(
                        selected = selected == index,
                        onClick = { onSelect(index) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        icon = {},
                    ) { Text(text, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                }
            }
        }
        return
    }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))
        options.forEachIndexed { index, text ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onSelect(index) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.RadioButton(selected == index, onClick = { onSelect(index) })
                Text(text, style = MaterialTheme.typography.titleMedium)
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

internal fun prayerLevelLabel(level: PrayerLevel, s: com.agpeya.app.ui.strings.Strings): String = if (s === com.agpeya.app.ui.strings.EnglishStrings) when (level) {
    PrayerLevel.PSALM_50 -> "Psalm 50"
    PrayerLevel.BEGINNING -> "Beginning"
    PrayerLevel.GROWTH -> "Growth"
    PrayerLevel.STEADFAST -> "Steadfast"
    PrayerLevel.FULL -> "Full"
} else when (level) {
    PrayerLevel.PSALM_50 -> "መዝሙር ፶"
    PrayerLevel.BEGINNING -> "መጀመሪያ"
    PrayerLevel.GROWTH -> "እድገት"
    PrayerLevel.STEADFAST -> "ጽናት"
    PrayerLevel.FULL -> "ሙሉ"
}

internal fun prayerLevelDetail(level: PrayerLevel, s: com.agpeya.app.ui.strings.Strings): String = when (level) {
    PrayerLevel.PSALM_50 -> s.prayerLevelPsalm50Description
    PrayerLevel.BEGINNING -> s.prayerLevelBeginningDescription
    PrayerLevel.GROWTH -> s.prayerLevelGrowthDescription
    PrayerLevel.STEADFAST -> s.prayerLevelSteadfastDescription
    PrayerLevel.FULL -> s.prayerLevelFullDescription
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
