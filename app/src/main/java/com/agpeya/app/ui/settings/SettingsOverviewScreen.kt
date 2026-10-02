package com.agpeya.app.ui.settings

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
 * Settings landing page organized into 4 clear category hubs with live status summaries:
 * 1. Appearance & Reading (Theme, Language, Reading font, Text size, Layout)
 * 2. Prayer Rule & Hours (Prayer rule level, Manage hours, Manage habits)
 * 3. Reminders & Daily Quote (Prayer alarms, Daily quote & lockscreen, Nightly nudge)
 * 4. Data, Profile & Backup (Christian name, Backup & Restore)
 * Followed by About & Help.
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
    val profileName by SettingsRepository.profileName(context).collectAsState(initial = "")
    val christianName by SettingsRepository.christianName(context).collectAsState(initial = "")
    val today by com.agpeya.app.ui.common.rememberCurrentDate()
    val schedule by remember(today) { com.agpeya.app.data.DaySchedule.observe(context, today) }
        .collectAsState(initial = emptyList())
    val enabledCount = schedule.count { it.today }
    val size = SettingsRepository.FONT_STEPS_SP[fontStep.coerceIn(0, SettingsRepository.FONT_STEPS_SP.lastIndex)]

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

                // Fast 1-tap toggles: Theme & Language
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

                Spacer(Modifier.height(Spacing.lg))

                // The 4 Category Hubs
                SectionHeader(s.settingsTitle)

                // 1. Appearance & Reading
                NavRow(
                    title = s.settingsGroupReading,
                    subtitle = "${fontLabel(font)} · ${size}sp",
                    onClick = onOpenReading,
                )

                // 2. Prayer Rule & Hours
                NavRow(
                    title = s.prayerSettingsTitle,
                    subtitle = "${prayerLevelLabel(prayerLevel, s)} · ${s.manageHours}",
                    onClick = onOpenPrayer,
                )

                // 3. Reminders & Daily Quote
                NavRow(
                    title = "${s.remindersSettingsTitle} · ${s.dailyQuoteChannelName}",
                    subtitle = "${alarmAlertLabel(alert, s)} · ${s.todayLabel}: $enabledCount",
                    onClick = onOpenReminders,
                )

                // 4. Data, Profile & Backup
                val callName = christianName.ifBlank { profileName }.takeIf { it.isNotBlank() }
                val backupText = backupRelativeLabel(lastBackupAt, s)
                NavRow(
                    title = s.settingsGroupRecords,
                    subtitle = if (callName != null) "$callName · $backupText" else backupText,
                    onClick = onOpenRecords,
                )

                Spacer(Modifier.height(Spacing.lg))

                // About & Help
                SectionHeader(s.about)
                NavRow(s.whatsNew, onOpenChangelog, subtitle = "v${appVersion(context)}")
                NavRow(s.tutorial, onOpenTutorial)
                NavRow(
                    s.feedbackTitle,
                    { com.agpeya.app.ui.common.openUrl(context, com.agpeya.app.ui.common.FEEDBACK_URL) },
                    subtitle = s.feedbackSubtitle,
                )
                NavRow(
                    s.rateAppTitle,
                    { com.agpeya.app.ui.common.openPlayStore(context) },
                    subtitle = s.rateAppSubtitle,
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
}

/** The installed versionName, straight from the package — never a hardcoded copy. */
private fun appVersion(context: android.content.Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: ""

private fun alarmAlertLabel(alert: AlarmAlert, s: com.agpeya.app.ui.strings.Strings): String = when (alert) {
    AlarmAlert.SOUND_VIBRATE -> s.alertSoundVibrate
    AlarmAlert.SOUND_ONLY -> s.alertSoundOnly
    AlarmAlert.VIBRATE_ONLY -> s.alertVibrateOnly
    AlarmAlert.SILENT -> s.alertSilent
}

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
    ReadingFont.ABBA_GARIMA -> "Abba Garima"
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
