package com.agpeya.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.PrayerLevel
import com.agpeya.app.data.ThemeChoice
import com.agpeya.app.ui.common.AgpeyaBottomBar
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.Tab
import com.agpeya.app.ui.common.ToggleRow
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion
import com.agpeya.app.ui.theme.Spacing
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onSelectTab: (Tab) -> Unit,
    onOpenModes: () -> Unit,
    onOpenCustomize: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSpecialHabit: (com.agpeya.app.reminders.SpecialHabit) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    val motion = LocalMotion.current
    val theme by SettingsRepository.theme(context).collectAsState(initial = ThemeChoice.SYSTEM)
    val readingFont by SettingsRepository.readingFont(context)
        .collectAsState(initial = com.agpeya.app.data.ReadingFont.ABYSSINICA)
    val keepOn by SettingsRepository.keepScreenOn(context).collectAsState(initial = true)
    val prayerLevel by SettingsRepository.prayerLevel(context).collectAsState(initial = PrayerLevel.FULL)
    val streakReminder by SettingsRepository.streakReminder(context).collectAsState(initial = true)
    val gitsaweReminder by SettingsRepository.gitsaweReminder(context).collectAsState(initial = true)
    val breathReminder by SettingsRepository.breathReminder(context).collectAsState(initial = true)
    val almsReminder by SettingsRepository.almsReminder(context).collectAsState(initial = false)
    val repentReminder by SettingsRepository.repentanceReminder(context).collectAsState(initial = true)
    val language by SettingsRepository.language(context).collectAsState(initial = com.agpeya.app.data.Language.SYSTEM)
    val alarmAlert by SettingsRepository.alarmAlert(context)
        .collectAsState(initial = com.agpeya.app.data.AlarmAlert.SOUND_VIBRATE)
    val alarmSound by SettingsRepository.alarmSound(context)
        .collectAsState(initial = com.agpeya.app.data.AlarmSound.ALARM)

    // The streak and ግጻዌ nudges are notification-only — unlike a prayer alarm,
    // which rings and shows a full-screen intent regardless, they are silently
    // dropped when POST_NOTIFICATIONS was never granted. Ask on the way in, so
    // switching one on here is enough to actually make it arrive.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* denial is reported by the banner below, not a second dialog */ }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AgpeyaBottomBar(current = Tab.SETTINGS, onSelect = onSelectTab) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            item {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = s.settingsTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(Spacing.xl))
            }

            // ── Reading: theme, face, and what the screen does while you pray ─
            item {
                SectionHeader(s.settingsGroupReading)
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = s.appearance,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
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
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
            }

            item {
                // Collapsed by default — six preview rows would otherwise dominate
                // the settings screen. The header still names the active face, so
                // the current choice is visible without expanding.
                var fontsExpanded by rememberSaveable { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { fontsExpanded = !fontsExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        // Muted like the Appearance/Language labels above it —
                        // gold here read as a second section header.
                        Text(
                            text = s.readingFontTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = fontDisplayName(readingFont),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = s.readingFontSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = if (fontsExpanded) Icons.Outlined.ExpandLess
                        else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AnimatedVisibility(
                    visible = fontsExpanded,
                    enter = expandVertically(motion.spec(Motion.standard)) + fadeIn(motion.spec(Motion.standard)),
                    exit = shrinkVertically(motion.spec(Motion.fast)) + fadeOut(motion.spec(Motion.fast)),
                ) {
                    Column {
                        Spacer(Modifier.height(Spacing.md))
                        // Each row previews itself in its own face — the only honest
                        // way to choose a typeface is to see the script rendered in it.
                        ReadingFontPicker(
                            selected = readingFont,
                            onSelect = { scope.launch { SettingsRepository.setReadingFont(context, it) } },
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xl))
            }

            item {
                Text(
                    text = s.languageLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
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
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                ToggleRow(
                    title = s.keepScreenOn,
                    subtitle = s.keepScreenOnDesc,
                    checked = keepOn,
                    onCheckedChange = { scope.launch { SettingsRepository.setKeepScreenOn(context, it) } },
                )
                Spacer(Modifier.height(Spacing.xxl))
            }

            // ── Prayer and reminders: everything that decides when the app
            //    speaks to you, in one place instead of four.
            item {
                SectionHeader(s.settingsGroupPrayer)
                Spacer(Modifier.height(Spacing.xs))
                DropdownSetting(
                    label = s.prayerLevelTitle,
                    current = prayerLevelName(prayerLevel),
                    options = PrayerLevel.entries.map { it to prayerLevelName(it) },
                    onSelect = { scope.launch { SettingsRepository.setPrayerLevel(context, it) } },
                )
                Text(
                    text = s.prayerLevelDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                // Re-read on each (re)composition, so returning from the system
                // settings page reflects the change without a restart.
                if (!NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                    (streakReminder || gitsaweReminder || breathReminder || almsReminder || repentReminder)
                ) {
                    NotificationsOffBanner(
                        onOpenSettings = {
                            context.startActivity(
                                android.content.Intent(
                                    android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS,
                                ).putExtra(
                                    android.provider.Settings.EXTRA_APP_PACKAGE,
                                    context.packageName,
                                ),
                            )
                        },
                    )
                    Spacer(Modifier.height(Spacing.md))
                }
                ToggleRow(
                    title = s.settingsNightReminder,
                    subtitle = s.settingsNightReminderDesc,
                    checked = streakReminder,
                    onCheckedChange = { on ->
                        if (on) ensureNotificationPermission()
                        scope.launch {
                            SettingsRepository.setStreakReminder(context, on)
                            com.agpeya.app.reminders.StreakReminderScheduler.sync(context, on)
                        }
                    },
                )
                // The nudge's time, shown only while it is on — a time for a
                // reminder that will not fire is clutter.
                AnimatedVisibility(
                    visible = streakReminder,
                    enter = fadeIn(motion.spec(Motion.standard)) + expandVertically(motion.spec(Motion.standard)),
                    exit = fadeOut(motion.spec(Motion.fast)) + shrinkVertically(motion.spec(Motion.fast)),
                ) {
                    StreakReminderTimeRow(s)
                }
                ToggleRow(
                    title = s.settingsGitsaweReminder,
                    subtitle = s.settingsGitsaweReminderDesc,
                    checked = gitsaweReminder,
                    onCheckedChange = { on ->
                        if (on) ensureNotificationPermission()
                        scope.launch {
                            SettingsRepository.setGitsaweReminder(context, on)
                            com.agpeya.app.reminders.GitsaweReminderScheduler.sync(context, on)
                        }
                    },
                )
                // የመሃል ጸሎት: once a day, at a random moment between the
                // hours, one short prayer — praying, not reading. No time row:
                // the whole point is that nobody, including the user, picks it.
                ToggleRow(
                    title = s.settingsBreathReminder,
                    subtitle = s.settingsBreathReminderDesc,
                    checked = breathReminder,
                    onCheckedChange = { on ->
                        if (on) ensureNotificationPermission()
                        scope.launch {
                            SettingsRepository.setBreathReminder(context, on)
                            com.agpeya.app.reminders.BreathPrayerScheduler.sync(context, on)
                        }
                    },
                )
                // ምጽዋት and ንስሐ: scheduled intentions, not habits. Each has
                // its own page (toggle, cadence, time, next due day) — a row
                // here is only the door; the state subtitle says at a glance
                // whether the reminder is on.
                NavRow(
                    title = s.settingsAlmsReminder,
                    subtitle = s.settingsAlmsReminderDesc,
                    onClick = { onOpenSpecialHabit(com.agpeya.app.reminders.SpecialHabit.ALMS) },
                )
                NavRow(
                    title = s.settingsRepentReminder,
                    subtitle = s.settingsRepentReminderDesc,
                    onClick = { onOpenSpecialHabit(com.agpeya.app.reminders.SpecialHabit.REPENTANCE) },
                )
                NavRow(s.manageHours, onOpenCustomize, leadingIcon = Icons.Outlined.Tune)
                NavRow(s.reminderModes, onOpenModes, leadingIcon = Icons.Outlined.Alarm)
                Spacer(Modifier.height(Spacing.md))
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
                QuietHoursRow(s)
                Spacer(Modifier.height(Spacing.xxl))
            }

            // ── Your data: the things only this device holds. ────────────────
            item {
                SectionHeader(s.settingsGroupData)
                Spacer(Modifier.height(Spacing.xs))
                val profileName by SettingsRepository.profileName(context).collectAsState(initial = "")
                val christianName by SettingsRepository.christianName(context).collectAsState(initial = "")
                EditableRow(s.yourNameLabel, profileName) { v ->
                    scope.launch { SettingsRepository.setProfileName(context, v) }
                }
                EditableRow(s.christianNameLabel, christianName) { v ->
                    scope.launch { SettingsRepository.setChristianName(context, v) }
                }
                BackupRows(s)
                Spacer(Modifier.height(Spacing.xxl))
            }

            // ── More ─────────────────────────────────────────────────────────
            item {
                SectionHeader(s.settingsGroupMore)
                Spacer(Modifier.height(Spacing.xs))
                NavRow(s.tutorial, onOpenTutorial, leadingIcon = Icons.Outlined.School)
                NavRow(s.about, onOpenAbout, leadingIcon = Icons.Outlined.Info)
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

private fun prayerLevelName(level: PrayerLevel): String = when (level) {
    PrayerLevel.PSALM_50 -> "መዝሙር ፶"
    PrayerLevel.BEGINNING -> "መጀመሪያ"
    PrayerLevel.GROWTH -> "እድገት"
    PrayerLevel.STEADFAST -> "ጽናት"
    PrayerLevel.FULL -> "ሙሉ"
}

/**
 * Shown when a notification-only reminder is switched on but the system will
 * not deliver it. Once POST_NOTIFICATIONS has been denied twice the runtime
 * prompt no longer appears, so the only way back is the system settings page.
 */
@Composable
private fun NotificationsOffBanner(onOpenSettings: () -> Unit) {
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(
                s.notifDisabledTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                s.notifDisabledBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(Spacing.xs))
            TextButton(onClick = onOpenSettings) { Text(s.openSettings) }
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
                .heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = enabled) { expanded = true }
                .padding(vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                )
                Text(
                    current,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
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

/** Label + current value; tapping opens a dialog with a single text field. */
@Composable
private fun EditableRow(label: String, value: String, onSave: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable { editing = true }
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value.ifBlank { "—" },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (editing) {
        val s = com.agpeya.app.ui.strings.LocalStrings.current
        var text by remember(value) { mutableStateOf(value) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(label) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onSave(text)
                    editing = false
                }) { Text(s.save) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { editing = false }) { Text(s.cancel) }
            },
        )
    }
}

/** The bundled reader faces, each shown rendered in itself. */
private val FONT_CHOICES = listOf(
    com.agpeya.app.data.ReadingFont.ABYSSINICA to "Abyssinica SIL",
    com.agpeya.app.data.ReadingFont.ABAY_LIGHT to "Ethiopic Abay Light",
    com.agpeya.app.data.ReadingFont.BELA_BEREKA to "Bela Bereka",
    com.agpeya.app.data.ReadingFont.ZEMENAY to "Zemenay",
)

/** A sample line of the script the choice actually affects. */
private const val FONT_SAMPLE = "አቡነ ዘበሰማያት ፩፪፫"

@Composable
private fun ReadingFontPicker(
    selected: com.agpeya.app.data.ReadingFont,
    onSelect: (com.agpeya.app.data.ReadingFont) -> Unit,
) {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        FONT_CHOICES.forEach { (choice, name) ->
            val isSel = choice == selected
            androidx.compose.material3.Surface(
                onClick = { onSelect(choice) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = if (isSel) MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
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
                            text = FONT_SAMPLE,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = com.agpeya.app.ui.theme.readingFontFamily(choice),
                                fontSize = 21.sp,
                                lineHeight = 34.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
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
}

/** The face's own name, for the collapsed header. */
private fun fontDisplayName(font: com.agpeya.app.data.ReadingFont): String =
    FONT_CHOICES.firstOrNull { it.first == font }?.second ?: "Abyssinica SIL"

/**
 * Backup and restore of the things the user can't recover: streak history,
 * bookmarks, highlights. Written through the system file picker — the app has
 * no network access, so a backup is simply a file the user keeps.
 *
 * Import shows what the file holds and how much of it is new here BEFORE
 * anything is written, because a restore is not something to discover after
 * the fact.
 */
@Composable
private fun BackupRows(s: com.agpeya.app.ui.strings.Strings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Headline plus an explanation. A failed backup or a rejected file has to
    // say what happened AND that nothing of the user's was lost — that second
    // half is the one people actually need.
    var message by remember { mutableStateOf<Pair<String, String?>?>(null) }
    // The file the user chose, held while they confirm the preview.
    var pending by remember { mutableStateOf<android.net.Uri?>(null) }
    var preview by remember { mutableStateOf<com.agpeya.app.data.BackupRepository.Summary?>(null) }

    val createDoc = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val today = java.time.LocalDate.now().toString()
            val ok = com.agpeya.app.data.BackupRepository.writeTo(context, uri, today)
            message = if (ok) s.backupSaved to null else s.backupFailed to s.backupFailedBody
        }
    }

    val openDoc = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val summary = com.agpeya.app.data.BackupRepository.peek(context, uri)
            if (summary == null) {
                message = s.restoreFailed to s.restoreFailedBody
            } else {
                pending = uri
                preview = summary
            }
        }
    }

    NavRow(
        s.backupExport,
        onClick = { createDoc.launch("sinq-backup-${java.time.LocalDate.now()}.json") },
        leadingIcon = Icons.Outlined.Upload,
    )
    NavRow(
        s.backupImport,
        onClick = { openDoc.launch(arrayOf("application/json", "text/plain", "*/*")) },
        leadingIcon = Icons.Outlined.Download,
    )

    // Preview: what's in the file, and what a restore would actually add.
    val summary = preview
    if (summary != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { preview = null; pending = null },
            title = { Text(s.restorePreviewTitle) },
            text = {
                Column {
                    if (summary.created.isNotBlank()) {
                        Text(
                            s.backupCreated(summary.created),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(s.backupContains(summary.days, summary.bookmarks, summary.highlights))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (summary.newDays == 0 && summary.newBookmarks == 0) s.restoreNothingNew
                        else s.restoreWillAdd(summary.newDays, summary.newBookmarks),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s.restoreMergeNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pending
                    preview = null
                    pending = null
                    if (uri != null) scope.launch {
                        val ok = com.agpeya.app.data.BackupRepository.restore(context, uri)
                        message = if (ok) s.restoreDone to null else s.restoreFailed to s.restoreFailedBody
                    }
                }) { Text(s.backupImport) }
            },
            dismissButton = {
                TextButton(onClick = { preview = null; pending = null }) { Text(s.cancel) }
            },
        )
    }

    message?.let { (headline, detail) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text(s.ok) } },
            title = { Text(headline) },
            text = { if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium) },
        )
    }
}

/**
 * When the nightly streak nudge fires. The same Material clock the prayer-mode
 * editor uses, in a dialog; saving re-arms the alarm immediately, so the change
 * takes effect tonight rather than after the old time fires once more.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun StreakReminderTimeRow(s: com.agpeya.app.ui.strings.Strings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val minute by SettingsRepository.streakReminderTime(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_STREAK_REMINDER_MIN)
    var picking by remember { mutableStateOf(false) }

    com.agpeya.app.ui.common.ListRow(
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
                        SettingsRepository.setStreakReminderTime(
                            context,
                            timeState.hour * 60 + timeState.minute,
                        )
                        com.agpeya.app.reminders.StreakReminderScheduler.sync(context, true)
                    }
                }) { Text(s.save) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text(s.cancel) }
            },
        )
    }
}

/** A nightly window in which prayer reminders stay silent. */
@Composable
private fun QuietHoursRow(s: com.agpeya.app.ui.strings.Strings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val quiet by SettingsRepository.quietHours(context)
        .collectAsState(initial = com.agpeya.app.data.QuietHours())

    fun fmt(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)

    // The shared ToggleRow, not a hand-rolled Row+Switch: the whole width
    // toggles, and TalkBack announces one switch with its state instead of an
    // unlabelled row followed by a stray control.
    ToggleRow(
        title = s.quietHours,
        subtitle = if (quiet.enabled) s.quietHoursRange(fmt(quiet.startMinute), fmt(quiet.endMinute))
        else s.quietHoursDesc,
        checked = quiet.enabled,
        onCheckedChange = { on ->
            scope.launch { SettingsRepository.setQuietHours(context, quiet.copy(enabled = on)) }
        },
    )
}
