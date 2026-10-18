package com.sinq.app.ui.intro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sinq.app.data.Language
import com.sinq.app.data.ReadingFont
import com.sinq.app.data.SettingsRepository
import com.sinq.app.reminders.ReminderScheduler
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.strings.Strings
import com.sinq.app.ui.theme.IconSize
import com.sinq.app.ui.theme.Spacing
import com.sinq.app.ui.theme.inReadingFont
import com.sinq.app.ui.theme.readingFontFamily
import kotlinx.coroutines.launch

/**
 * Three-screen first-run onboarding per Section 9 specification:
 * (1) Language mode (Amh+Eng, English, አማርኛ)
 * (2) Reader font and size with a live preview
 * (3) Reminder preference (off by default, with explanation of exact-alarm permission only if opted in)
 *
 * Guaranteed properties:
 * - Skippable at every step
 * - Resumable via DataStore
 * - Saved to DataStore
 * - Shown once
 * - Consistent manuscript design system styling
 */
@Composable
fun IntroScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    // Resumable step tracking (0 = Language, 1 = Typography, 2 = Reminders)
    val storedStep by SettingsRepository.onboardingStep(context).collectAsState(initial = 0)
    var currentStep by remember(storedStep) { mutableIntStateOf(storedStep) }

    val isDark = isSystemInDarkTheme()
    val manuscriptBg = if (isDark) Color(0xFF0B2822) else Color(0xFFFAF7EF)
    val manuscriptBorder = if (isDark) Color(0xFF235448) else Color(0xFFD6C7A1)
    val goldAccent = Color(0xFFE8C46B)
    val rubricRed = Color(0xFF931A1D)

    fun advanceTo(step: Int) {
        val next = step.coerceIn(0, 2)
        currentStep = next
        scope.launch { SettingsRepository.setOnboardingStep(context, next) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top App Bar row with Step Rosette and Skip Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Sacred step indicator rosette: ፨ ፩ ፨, ፨ ፪ ፨, ፨ ፫ ፨
                val stepRosette = when (currentStep) {
                    0 -> "፨ ፩ ፨"
                    1 -> "፨ ፪ ፨"
                    else -> "፨ ፫ ፨"
                }
                Text(
                    text = stepRosette,
                    style = MaterialTheme.typography.titleMedium,
                    color = rubricRed,
                    fontWeight = FontWeight.Bold,
                )

                TextButton(onClick = onDone) {
                    Text(
                        text = s.skip,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // Step Progress Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { index ->
                    val active = index == currentStep
                    Box(
                        modifier = Modifier
                            .size(if (active) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (active) goldAccent else manuscriptBorder.copy(alpha = 0.5f)),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            // Screen 0: Language Mode
            if (currentStep == 0) {
                LanguageStep(
                    manuscriptBg = manuscriptBg,
                    manuscriptBorder = manuscriptBorder,
                    goldAccent = goldAccent,
                    onNext = { advanceTo(1) },
                )
            }
            // Screen 1: Reader Font and Size with Live Preview
            else if (currentStep == 1) {
                TypographyStep(
                    manuscriptBg = manuscriptBg,
                    manuscriptBorder = manuscriptBorder,
                    goldAccent = goldAccent,
                    onNext = { advanceTo(2) },
                )
            }
            // Screen 2: Reminder Preference
            else {
                RemindersStep(
                    manuscriptBg = manuscriptBg,
                    manuscriptBorder = manuscriptBorder,
                    onFinish = onDone,
                )
            }

            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun LanguageStep(
    manuscriptBg: Color,
    manuscriptBorder: Color,
    goldAccent: Color,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val currentLang by SettingsRepository.language(context).collectAsState(initial = Language.AMH_ENG)

    Text(
        text = s.onboardingStepLanguageTitle,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = s.onboardingStepLanguageBody,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.xl))

    val options = listOf(
        Triple(Language.AMH_ENG, "አማርኛ + English", "Bilingual interface · Amharic liturgical terms with English explanations"),
        Triple(Language.ENGLISH, "English", "Complete English interface for all prayers, readings, and settings"),
        Triple(Language.AMHARIC, "አማርኛ", "ሙሉ የአማርኛ በይነገጽ ለጸሎታት፣ ንባባትና ማብራሪያዎች"),
    )

    options.forEach { (lang, label, desc) ->
        val selected = lang == currentLang
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable {
                    scope.launch { SettingsRepository.setLanguage(context, lang) }
                },
            shape = RoundedCornerShape(14.dp),
            color = if (selected) manuscriptBg else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) goldAccent else manuscriptBorder.copy(alpha = 0.5f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selected) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = goldAccent,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(Spacing.xxl))

    Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Text(s.next)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypographyStep(
    manuscriptBg: Color,
    manuscriptBorder: Color,
    goldAccent: Color,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    val currentFont by SettingsRepository.readingFont(context)
        .collectAsState(initial = ReadingFont.ABYSSINICA)
    val fontStep by SettingsRepository.fontStep(context)
        .collectAsState(initial = 2)

    val maxStep = 5
    val currentFontSize = 18.sp * (1f + fontStep * 0.12f)
    val lineSpacing = currentFontSize * 0.4f

    Text(
        text = s.onboardingStepTypographyTitle,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = s.onboardingStepTypographyBody,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.lg))

    // Live Preview Manuscript Card
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = manuscriptBg,
        border = BorderStroke(1.5.dp, manuscriptBorder),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "✦  መዝሙረ ዳዊት ፳፫  ✦",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF931A1D),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "በስመ አብ ወወልድ ወመንፈስ ቅዱስ አሐዱ አምላክ አሜን።\n\nእግዚአብሔር እረኛዬ ነው፥ የሚያሳጣኝም የለም። በለመለመ መስክ ያሳድረኛል፤ በዕረፍት ውኃ ዘንድ ይመራኛል።",
                fontSize = currentFontSize,
                lineHeight = (18f * (1f + fontStep * 0.12f) * 1.4f).sp,
                fontFamily = readingFontFamily(currentFont),
                color = if (isSystemInDarkTheme()) Color(0xFFEDE0C8) else Color(0xFF1C1B1F),
            )
        }
    }

    Spacer(Modifier.height(Spacing.lg))

    // Font family selection chips
    Text(
        text = s.readingFontTitle,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(Spacing.xs))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReadingFont.entries.forEach { font ->
            val fontLabel = when (font) {
                ReadingFont.ABYSSINICA -> "Abyssinica SIL"
                ReadingFont.ABAY_LIGHT -> "Abay Light"
                ReadingFont.BELA_BEREKA -> "Bela Bereka"
                ReadingFont.ZEMENAY -> "Zemenay"
                ReadingFont.ABBA_GARIMA -> "Abba Garima"
            }
            FilterChip(
                selected = font == currentFont,
                onClick = {
                    scope.launch { SettingsRepository.setReadingFont(context, font) }
                },
                label = { Text(fontLabel, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }

    Spacer(Modifier.height(Spacing.md))

    // Size Controls (A- / A+)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = s.fontSizeLabel,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    if (fontStep > 0) scope.launch { SettingsRepository.setFontStep(context, fontStep - 1) }
                },
                enabled = fontStep > 0,
            ) { Text("A−") }

            Text(
                text = "${fontStep + 1} / ${maxStep + 1}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            OutlinedButton(
                onClick = {
                    if (fontStep < maxStep) scope.launch { SettingsRepository.setFontStep(context, fontStep + 1) }
                },
                enabled = fontStep < maxStep,
            ) { Text("A+") }
        }
    }

    Spacer(Modifier.height(Spacing.xl))

    Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Text(s.next)
    }
}

@Composable
private fun RemindersStep(
    manuscriptBg: Color,
    manuscriptBorder: Color,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    // Reminders preference (default OFF per Section 9 specification)
    var remindersEnabled by remember { mutableStateOf(false) }

    val canExactAlarm = remember { ReminderScheduler.canScheduleExact(context) }
    var hasNotificationPermission by remember {
        val granted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        mutableStateOf(granted)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted
    }

    Text(
        text = s.onboardingStepRemindersTitle,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(Spacing.xs))
    Text(
        text = s.onboardingStepRemindersBody,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.xl))

    // Main Toggle Card
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = manuscriptBg,
        border = BorderStroke(1.5.dp, manuscriptBorder),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.onboardingReminderOptIn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = s.onboardingReminderOptInDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = remindersEnabled,
                onCheckedChange = { enabled ->
                    remindersEnabled = enabled
                    if (enabled && Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
        }
    }

    // Only if the user opts in, show permission requests and exact alarm guidance
    if (remindersEnabled) {
        Spacer(Modifier.height(Spacing.md))

        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.reminderSetupNotifications,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = s.reminderSetupNotificationsBody,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text(s.enableAction)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        // Exact Alarm Permission explanation (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canExactAlarm) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = s.exactAlarmWarningTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = s.exactAlarmWarningBody,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(s.grantExactAlarmPermission)
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(Spacing.xxl))

    Button(
        onClick = {
            scope.launch {
                // Save reminder state
                if (remindersEnabled) {
                    SettingsRepository.setStreakReminder(context, true)
                } else {
                    SettingsRepository.setStreakReminder(context, false)
                }
                onFinish()
            }
        },
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        Text(s.getStarted)
    }
}

/**
 * Replayable tour reached from Settings.
 */
@Composable
fun TutorialScreen(onDone: () -> Unit, onOpenRoute: ((String) -> Unit)? = null) {
    val s = LocalStrings.current
    val pages = listOf(
        IntroPage(icon = Icons.AutoMirrored.Outlined.MenuBook, title = s.introGitsaweTitle, body = s.introGitsaweBody),
        IntroPage(icon = Icons.Outlined.Schedule, title = s.introHoursTitle, body = s.introHoursBody),
        IntroPage(icon = Icons.Outlined.NotificationsActive, title = s.introRemindersTitle, body = s.introRemindersBody),
        IntroPage(icon = Icons.AutoMirrored.Outlined.MenuBook, title = s.introPsalterTitle, body = s.introPsalterBody),
        IntroPage(icon = Icons.AutoMirrored.Outlined.MenuBook, title = s.introReadingTitle, body = s.introReadingBody),
        IntroPage(icon = Icons.Outlined.Route, title = s.introJourneyTitle, body = s.introJourneyBody),
        IntroPage(icon = Icons.Outlined.EditNote, title = s.introJournalTitle, body = s.introJournalBody),
        IntroPage(icon = Icons.Outlined.VolunteerActivism, title = s.introOfferingsTitle, body = s.introOfferingsBody),
        IntroPage(icon = Icons.Outlined.Search, title = s.introSearchTitle, body = s.introSearchBody),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isLast = pagerState.currentPage == pages.size - 1
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDone) { Text(s.skip) }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { p ->
                val page = pages[p]
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (page.icon != null) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp),
                        )
                        Spacer(Modifier.height(Spacing.screen))
                    }
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = page.body,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(pages.size) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (active) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                    )
                }
            }

            Button(
                onClick = {
                    if (isLast) onDone()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            ) {
                Text(if (isLast) s.gotIt else s.next)
            }
        }
    }
}

private data class IntroPage(
    val icon: ImageVector? = null,
    val title: String,
    val body: String,
)
