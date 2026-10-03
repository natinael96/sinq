package com.agpeya.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Church
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.AlarmAlert
import com.agpeya.app.data.Language
import com.agpeya.app.data.PrayerLevel
import com.agpeya.app.data.ReadingFont
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.ThemeChoice
import com.agpeya.app.ui.common.HeroCard
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.theme.sinqColors
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqDivider
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
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
    val gold = MaterialTheme.colorScheme.secondary
    val primary = MaterialTheme.colorScheme.primary

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
                Text(
                    text = s.settingsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(Spacing.md))

                // Fast 1-tap toggles: Theme & Language in a unified card
                SinqCard(
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CompactSegmented(
                        label = s.appearance,
                        options = listOf(s.themeSystem, s.themeLight, s.themeDark),
                        selected = theme.ordinal,
                        onSelect = { scope.launch { SettingsRepository.setTheme(context, ThemeChoice.entries[it]) } },
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    SinqDivider()
                    Spacer(Modifier.height(Spacing.xs))
                    CompactSegmented(
                        label = s.languageLabel,
                        options = listOf(s.langSystem, s.langAmharic, s.langEnglish),
                        selected = language.ordinal,
                        onSelect = { scope.launch { SettingsRepository.setLanguage(context, Language.entries[it]) } },
                    )
                }

                Spacer(Modifier.height(Spacing.lg))

                // The 4 Category Hubs
                SectionHeader(s.settingsTitle)

                // 1. Appearance & Reading
                SettingsHubCard(
                    title = s.settingsGroupReading,
                    subtitle = "${fontLabel(font)} · ${size}sp",
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    iconTint = gold,
                    iconBackground = gold.copy(alpha = 0.12f),
                    onClick = onOpenReading,
                )
                Spacer(Modifier.height(Spacing.sm))

                // 2. Prayer Rule & Hours
                SettingsHubCard(
                    title = s.prayerSettingsTitle,
                    subtitle = "${prayerLevelLabel(prayerLevel, s)} · ${s.manageHours}",
                    icon = Icons.Outlined.Church,
                    iconTint = primary,
                    iconBackground = primary.copy(alpha = 0.12f),
                    onClick = onOpenPrayer,
                )
                Spacer(Modifier.height(Spacing.sm))

                // 3. Reminders & Daily Quote
                SettingsHubCard(
                    title = "${s.remindersSettingsTitle} · ${s.dailyQuoteChannelName}",
                    subtitle = "${alarmAlertLabel(alert, s)} · ${s.todayLabel}: $enabledCount",
                    icon = Icons.Outlined.NotificationsActive,
                    iconTint = gold,
                    iconBackground = gold.copy(alpha = 0.12f),
                    onClick = onOpenReminders,
                )
                Spacer(Modifier.height(Spacing.sm))

                // 4. Data, Profile & Backup
                val callName = christianName.ifBlank { profileName }.takeIf { it.isNotBlank() }
                val backupText = backupRelativeLabel(lastBackupAt, s)
                SettingsHubCard(
                    title = s.settingsGroupRecords,
                    subtitle = if (callName != null) "$callName · $backupText" else backupText,
                    icon = Icons.Outlined.CloudSync,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    onClick = onOpenRecords,
                )

                Spacer(Modifier.height(Spacing.xl))

                // Bold, Unique Feedback Card
                FeedbackCard(
                    title = s.feedbackTitle,
                    subtitle = s.feedbackSubtitle,
                    cta = s.feedbackCta,
                    onClick = { com.agpeya.app.ui.common.openUrl(context, com.agpeya.app.ui.common.FEEDBACK_URL) },
                )

                Spacer(Modifier.height(Spacing.xl))

                // About & Help Card
                SectionHeader(s.about)
                SinqCard(
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    NavRow(
                        title = s.whatsNew,
                        subtitle = "v${appVersion(context)}",
                        leadingIcon = Icons.Outlined.NewReleases,
                        onClick = onOpenChangelog,
                    )
                    SinqDivider()
                    NavRow(
                        title = s.userGuide,
                        subtitle = s.userGuideSubtitle,
                        leadingIcon = Icons.Outlined.AutoStories,
                        onClick = { com.agpeya.app.ui.common.openUrl(context, com.agpeya.app.ui.common.GUIDE_URL) },
                    )
                    SinqDivider()
                    NavRow(
                        title = s.tutorial,
                        leadingIcon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onClick = onOpenTutorial,
                    )
                    SinqDivider()
                    NavRow(
                        title = s.rateAppTitle,
                        subtitle = s.rateAppSubtitle,
                        leadingIcon = Icons.Outlined.StarOutline,
                        onClick = { com.agpeya.app.ui.common.openPlayStore(context) },
                    )
                    SinqDivider()
                    NavRow(
                        title = s.about,
                        leadingIcon = Icons.Outlined.Info,
                        onClick = onOpenAbout,
                    )
                }

                Spacer(Modifier.height(Spacing.xl))
                Text(
                    text = "☩  ስንቅ  ·  v${appVersion(context)}  ☩",
                    style = MaterialTheme.typography.labelSmall,
                    color = gold.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.md))
            }
        }
    }
}

@Composable
private fun SettingsHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    onClick: () -> Unit,
) {
    SinqCard(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(IconSize.medium),
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(Spacing.xs))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(IconSize.medium),
            )
        }
    }
}

/**
 * Prominent feedback card matching the exact HeroCard design of the Home screen buttons (Gitsawe).
 */
@Composable
private fun FeedbackCard(
    title: String,
    subtitle: String,
    cta: String,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val sinq = sinqColors

    HeroCard(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.RateReview,
            contentDescription = null,
            tint = sinq.onHeroMuted,
            modifier = Modifier.size(IconSize.large),
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = sinq.onHero,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = sinq.onHeroMuted,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = cta,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = sinq.onHeroGold,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = sinq.onHeroMuted,
        )
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
            Modifier.fillMaxWidth().heightIn(min = 44.dp),
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
