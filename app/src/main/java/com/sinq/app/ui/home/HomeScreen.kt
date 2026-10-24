package com.sinq.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import com.sinq.app.ui.reading.geezNumeral
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VolunteerActivism
import com.sinq.app.ui.common.ReminderSetupSheet
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sinq.app.data.ContentRepository
import com.sinq.app.data.DayReadings
import com.sinq.app.data.GitsaweRepository
import com.sinq.app.data.HabitsRepository
import com.sinq.app.data.HomeHorizonMode
import com.sinq.app.data.HoursRepository
import com.sinq.app.data.PrayerJourney
import com.sinq.app.model.HabitsState
import com.sinq.app.model.Hour
import com.sinq.app.model.HoursConfig
import com.sinq.app.model.toBookmark
import com.sinq.app.ui.common.Candle
import com.sinq.app.ui.common.FillColumn
import com.sinq.app.ui.common.HeroCard
import com.sinq.app.ui.common.SinqCard
import com.sinq.app.ui.common.SinqDivider
import com.sinq.app.ui.common.Tab
import com.sinq.app.ui.common.PrayerDisplay
import com.sinq.app.ui.common.liturgicalSeasonLabel
import com.sinq.app.ui.habits.CompactHeatmap
import com.sinq.app.ui.habits.journeyLine
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.IconSize
import com.sinq.app.ui.theme.Spacing
import com.sinq.app.ui.theme.sinqColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val PRAYER_AGGREGATE_ID = "prayer"

/** A glanceable dashboard; constrained accessibility layouts retain a scroll safety net. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenReading: () -> Unit = {},
    onManageHours: () -> Unit,
    onOpenHour: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFasting: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenPrayerList: () -> Unit,
    onOpenPsalter: () -> Unit,
    onOpenZewotr: () -> Unit,
    onOpenGitsawe: (Long?) -> Unit = {},
    onOpenBatteryHelp: () -> Unit,
    onSelectTab: (Tab) -> Unit,
    onOpenDesertFathersHub: () -> Unit = {},
    onReflectQuote: ((com.sinq.app.model.DailyQuote) -> Unit)? = null,
    openDailyQuote: Boolean = false,
    onDailyQuoteConsumed: (() -> Unit)? = null,
    onOpenTour: () -> Unit = {},
) {
    val context = LocalContext.current
    val config by HoursRepository.config(context).collectAsState(initial = HoursConfig())
    val hoursLoad = com.sinq.app.ui.common.rememberContentLoad { ContentRepository.hours(context) }
    val builtIn = hoursLoad.value.orEmpty()
    val hours = remember(builtIn, config) { HoursRepository.merge(builtIn, config, includeHidden = false) }

    // Keeps the date, daily content, and current prayer correct across time boundaries.
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(30_000)
        }
    }
    val today = now.toLocalDate()
    val wazemaRolloverEnabled by com.sinq.app.data.SettingsRepository.wazemaRollover(context)
        .collectAsState(initial = true)
    val wazemaRolloverTime by com.sinq.app.data.SettingsRepository.wazemaRolloverTime(context)
        .collectAsState(initial = com.sinq.app.data.SettingsRepository.DEFAULT_WAZEMA_ROLLOVER_MIN)
    val homeHorizonMode by com.sinq.app.data.SettingsRepository.homeHorizonMode(context)
        .collectAsState(initial = HomeHorizonMode.DEVOTIONAL_HEATMAP)

    val isWazema = remember(now, wazemaRolloverEnabled, wazemaRolloverTime) {
        com.sinq.app.data.BahreHasab.isWazema(now, wazemaRolloverEnabled, wazemaRolloverTime)
    }
    val liturgicalDate = remember(now, wazemaRolloverEnabled, wazemaRolloverTime) {
        com.sinq.app.data.BahreHasab.liturgicalDate(now, wazemaRolloverEnabled, wazemaRolloverTime)
    }

    val currentHourId = remember(hours, now.hour, now.minute) {
        com.sinq.app.data.PrayerSchedule.currentHourId(hours, now.toLocalTime())
    }
    val suggestedId = remember(now.hour, isWazema) {
        if (isWazema) "vespers" else ContentRepository.suggestedHourId(now.hour)
    }
    val suggested = hours.find { it.id == suggestedId } ?: hours.find { it.id == currentHourId }

    val habitState by HabitsRepository.state(context).collectAsState(initial = HabitsState())
    val doneToday = habitState.records[today.toString()] ?: emptySet()
    val prayedAnyHour = doneToday.any { it.startsWith("hour_") }
    // Prayer, then only the habits today asks for: the count under ዛሬ is kept
    // of *due*, so a weekly habit does not read as missed six days a week.
    val habitIds = remember(habitState, today) {
        listOf(PRAYER_AGGREGATE_ID) + HabitsRepository.dueHabitIds(habitState, today)
    }
    val doneWithAggregate = if (prayedAnyHour) doneToday + PRAYER_AGGREGATE_ID else doneToday
    val strings = LocalStrings.current
    val effectiveDate = if (isWazema) liturgicalDate else today
    val seasonLabel = remember(effectiveDate, strings) { liturgicalSeasonLabel(effectiveDate, strings) }

    val readingsState by produceState<HomeReadingsState>(initialValue = HomeReadingsState.Loading, effectiveDate) {
        value = runCatching { GitsaweRepository.readingsFor(context, effectiveDate) }
            .fold(
                onSuccess = { HomeReadingsState.Ready(it) },
                onFailure = { HomeReadingsState.Unavailable },
            )
    }
    var showHours by remember { mutableStateOf(false) }
    var showReminderSetup by remember { mutableStateOf(false) }

    val dailyQuote by produceState<com.sinq.app.model.DailyQuote?>(initialValue = null, today) {
        value = runCatching { com.sinq.app.data.DailyQuoteRepository.quoteFor(context, today) }.getOrNull()
    }
    var showDailyQuoteSheet by remember { mutableStateOf(false) }

    val bookmarks by com.sinq.app.data.UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())
    val isDailyQuoteBookmarked = remember(bookmarks, dailyQuote?.quoteId) {
        val id = dailyQuote?.quoteId
        id != null && bookmarks.any { it.hourId == "daily_quote" && it.sectionId == id }
    }

    LaunchedEffect(openDailyQuote) {
        if (openDailyQuote) {
            showDailyQuoteSheet = true
            onDailyQuoteConsumed?.invoke()
        }
    }

    // Android owns both settings, so refresh them whenever the app resumes
    // after a permission dialog or a visit to system settings.
    var permissionPulse by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionPulse++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    @Suppress("UNUSED_VARIABLE") val refreshSystemState = permissionPulse
    val notificationsReady = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED)
    val batteryReady = context.getSystemService(PowerManager::class.java)
        ?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    val offerReminderSetup by com.sinq.app.data.SettingsRepository.showHomeReminderSetup(context)
        .collectAsState(initial = false)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionPulse++ }

    val installedVersionCode = remember { com.sinq.app.data.TourRepository.installedVersionCode(context) }
    val lastTourVersion by com.sinq.app.data.SettingsRepository.lastTourVersion(context)
        .collectAsState(initial = installedVersionCode)
    val onboarded by com.sinq.app.data.SettingsRepository.onboarded(context)
        .collectAsState(initial = true)
    var showWhatsNewDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(lastTourVersion, installedVersionCode, onboarded) {
        if (installedVersionCode > 0 && onboarded) {
            if (lastTourVersion == null || lastTourVersion!! < installedVersionCode) {
                showWhatsNewDialog = true
            }
        }
    }

    LaunchedEffect(offerReminderSetup, notificationsReady, batteryReady) {
        if (offerReminderSetup && notificationsReady && batteryReady) {
            com.sinq.app.data.SettingsRepository.dismissHomeReminderSetup(context)
            showReminderSetup = false
        }
    }

    // Once the alarm can actually fire, say once how it will.
    //
    // An arriving hour vibrates rather than rings, which is the right default
    // and not the obvious one: someone who sets up reminders and is never told
    // will conclude the alarm is broken the first time it goes off in a pocket.
    // It reaches readers who were already installed when the default changed
    // too — see showAlarmSoundNotice — since their alarms used to ring.
    // Raised on the notification permission alone — battery optimisation is a
    // review rather than a grant, and waiting on it would mean never telling
    // the readers who skip it.
    val offerAlarmSoundNotice by com.sinq.app.data.SettingsRepository
        .showAlarmSoundNotice(context).collectAsState(initial = false)
    val alarmAlert by com.sinq.app.data.SettingsRepository.alarmAlert(context)
        .collectAsState(initial = com.sinq.app.data.AlarmAlert.VIBRATE_ONLY)
    var showAlarmSoundNotice by remember { mutableStateOf(false) }
    LaunchedEffect(offerAlarmSoundNotice, notificationsReady) {
        if (!offerAlarmSoundNotice || !notificationsReady) return@LaunchedEffect
        showAlarmSoundNotice = true
    }

    // The only network-facing thing on this screen, and it draws nothing unless
    // the check is on AND a newer release was found AND it wasn't waved away.
    val scope = rememberCoroutineScope()
    val update by com.sinq.app.data.UpdateRepository.available(context)
        .collectAsState(initial = null)
    // The Play half of the same sentence. Only one of the two is ever live in a
    // given build, so these can never both draw — see PlayUpdateRepository.
    val playUpdate by com.sinq.app.data.PlayUpdateRepository.stage
        .collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // The bar belongs to the host that holds all four tabs, and it carries
        // its own navigation-bar padding; this page only insets for the status bar.
        contentWindowInsets = WindowInsets.statusBars,
    ) { innerPadding ->
      Column(Modifier.fillMaxSize().padding(innerPadding)) {
        if (offerReminderSetup && (!notificationsReady || !batteryReady)) {
            ReminderSetupLine(
                onOpen = { showReminderSetup = true },
                onDismiss = {
                    scope.launch {
                        com.sinq.app.data.SettingsRepository.dismissHomeReminderSetup(context)
                    }
                },
            )
        }
        // Above the day, outside the screen margin: a notice about the app
        // itself has no business indenting the date beneath it.
        update?.let { found ->
            com.sinq.app.ui.common.UpdateLine(
                text = strings.updateAvailable(found.version),
                action = strings.updateDownload,
                onOpen = {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                found.url.toUri(),
                            ),
                        )
                    }
                },
                onDismiss = {
                    scope.launch {
                        com.sinq.app.data.UpdateRepository.dismiss(context, found.version)
                    }
                },
            )
        }
        // Play has the new build on the device already; all that is left is the
        // restart, which only the app can ask for.
        playUpdate?.let {
            val activity = context as? android.app.Activity
            com.sinq.app.ui.common.UpdateLine(
                text = strings.updateReady,
                action = strings.updateRestart,
                onOpen = { activity?.let(com.sinq.app.data.PlayUpdateRepository::install) },
                onDismiss = { com.sinq.app.data.PlayUpdateRepository.dismiss() },
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val fontScale = LocalDensity.current.fontScale
            val stackReadingCards = maxWidth < 360.dp || fontScale > 1.15f
            HomeDashboard(
                modifier = Modifier
                    // A tablet should gain calm margins, not comically wide cards.
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    // The page is built to be exactly the height of the viewport,
                    // so this scrolls only when it cannot be — a small screen at
                    // a large type size. At every other size there is nothing
                    // below the fold and nothing to scroll to.
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screen, vertical = Spacing.md),
                // What the page has to fill: the viewport, less its own margin.
                targetHeight = maxHeight - Spacing.md * 2,
                stackReadingCards = stackReadingCards,
                onManageHours = onManageHours,
                hoursLoading = hoursLoad.result == null,
                hoursFailed = hoursLoad.result?.isFailure == true,
                retryHours = hoursLoad.retry,
                today = today,
                seasonLabel = seasonLabel,
                suggested = suggested,
                hours = hours,
                readingsState = readingsState,
                isWazema = isWazema,
                horizonMode = homeHorizonMode,
                onToggleHorizonMode = {
                    scope.launch {
                        val nextMode = if (homeHorizonMode == HomeHorizonMode.DEVOTIONAL_HEATMAP) {
                            HomeHorizonMode.LITURGICAL_SEASON_HORIZON
                        } else {
                            HomeHorizonMode.DEVOTIONAL_HEATMAP
                        }
                        com.sinq.app.data.SettingsRepository.setHomeHorizonMode(context, nextMode)
                    }
                },
                habitIds = habitIds,
                doneToday = doneWithAggregate,
                habitState = habitState,
                onOpenHour = onOpenHour,
                onOpenSearch = onOpenSearch,
                onOpenFasting = onOpenFasting,
                onOpenBookmarks = onOpenBookmarks,
                onOpenPrayerList = onOpenPrayerList,
                onOpenAllHours = { showHours = true },
                onOpenGitsawe = { onOpenGitsawe(if (isWazema) effectiveDate.toEpochDay() else null) },
                onOpenJourney = { onSelectTab(Tab.JOURNEY) },
                onOpenPsalter = onOpenPsalter,
                onOpenZewotr = onOpenZewotr,
                dailyQuote = dailyQuote,
                isDailyQuoteBookmarked = isDailyQuoteBookmarked,
                onToggleDailyQuoteBookmark = {
                    dailyQuote?.let { q ->
                        scope.launch {
                            com.sinq.app.data.UserDataRepository.toggleBookmark(
                                context,
                                q.toBookmark(strings.dailyQuoteChannelName),
                            )
                        }
                    }
                },
                onOpenDailyQuote = { showDailyQuoteSheet = true },
                onOpenDesertFathersHub = onOpenDesertFathersHub,
            )
        }
      }
    }

    if (showDailyQuoteSheet && dailyQuote != null) {
        DailyQuoteSheet(
            quote = dailyQuote!!,
            isBookmarked = isDailyQuoteBookmarked,
            onToggleBookmark = {
                scope.launch {
                    dailyQuote?.let { q ->
                        com.sinq.app.data.UserDataRepository.toggleBookmark(
                            context,
                            q.toBookmark(strings.dailyQuoteChannelName),
                        )
                    }
                }
            },
            onDismiss = { showDailyQuoteSheet = false },
            onReflectInJournal = { q ->
                showDailyQuoteSheet = false
                onReflectQuote?.invoke(q)
            },
        )
    }

    if (showHours) {
        ModalBottomSheet(onDismissRequest = { showHours = false }) {
            AllHoursSheet(hours, currentHourId) {
                showHours = false
                onOpenHour(it)
            }
        }
    }
    if (showAlarmSoundNotice) {
        val dismissNotice: () -> Unit = {
            showAlarmSoundNotice = false
            scope.launch { com.sinq.app.data.SettingsRepository.dismissAlarmSoundNotice(context) }
        }
        var selectedAlert by remember { mutableStateOf(alarmAlert) }
        val options = listOf(
            Triple(
                Icons.Outlined.NotificationsActive,
                strings.alertChoiceAlarmTitle to strings.alertChoiceAlarmBody,
                com.sinq.app.data.AlarmAlert.SOUND_VIBRATE,
            ),
            Triple(
                Icons.Outlined.Vibration,
                strings.alertChoiceVibrateTitle to strings.alertChoiceVibrateBody,
                com.sinq.app.data.AlarmAlert.VIBRATE_ONLY,
            ),
            Triple(
                Icons.Outlined.Notifications,
                strings.alertChoiceNotificationTitle to strings.alertChoiceNotificationBody,
                com.sinq.app.data.AlarmAlert.SILENT,
            ),
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = dismissNotice,
            title = { Text(strings.alertChoiceTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        strings.alertChoiceBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    options.forEach { (icon, texts, alert) ->
                        val isSel = selectedAlert == alert
                        Surface(
                            onClick = { selectedAlert = alert },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSel) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(IconSize.small),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(texts.first, style = MaterialTheme.typography.titleSmall)
                                    Text(texts.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                RadioButton(selected = isSel, onClick = null)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAlarmSoundNotice = false
                        scope.launch {
                            com.sinq.app.data.SettingsRepository.setAlarmAlert(context, selectedAlert)
                            com.sinq.app.data.SettingsRepository.dismissAlarmSoundNotice(context)
                        }
                    },
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = dismissNotice) {
                    Text(strings.notNow)
                }
            },
        )
    }
    if (showWhatsNewDialog) {
        com.sinq.app.ui.intro.WhatsNewUpdateDialog(
            versionName = com.sinq.app.BuildConfig.VERSION_NAME,
            onDismiss = {
                showWhatsNewDialog = false
                scope.launch {
                    com.sinq.app.data.SettingsRepository.setLastTourVersion(context, installedVersionCode)
                }
            },
            onOpenTour = {
                showWhatsNewDialog = false
                scope.launch {
                    com.sinq.app.data.SettingsRepository.setLastTourVersion(context, installedVersionCode)
                }
                onOpenTour()
            },
        )
    }
    if (showReminderSetup) {
        ReminderSetupSheet(
            notificationsReady = notificationsReady,
            batteryReady = batteryReady,
            onEnableNotifications = {
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    com.sinq.app.ui.common.openNotificationSettings(context)
                }
            },
            onReviewBattery = {
                showReminderSetup = false
                onOpenBatteryHelp()
            },
            onNotNow = {
                showReminderSetup = false
                scope.launch {
                    com.sinq.app.data.SettingsRepository.dismissHomeReminderSetup(context)
                }
            },
            onDismiss = { showReminderSetup = false },
        )
    }
}

/** A single notice line above the dashboard; details stay in the sheet. */
@Composable
private fun ReminderSetupLine(onOpen: () -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .heightIn(min = 48.dp)
                .clickable(onClick = onOpen)
                .padding(start = Spacing.screen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(IconSize.small),
            )
            Text(
                s.reminderSetupTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                s.enableAction,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = s.dismiss,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(IconSize.small),
                )
            }
        }
        SinqDivider()
    }
}

private sealed interface HomeReadingsState {
    data object Loading : HomeReadingsState
    data object Unavailable : HomeReadingsState
    data class Ready(val readings: DayReadings) : HomeReadingsState
}


@Composable
private fun HomeDashboard(
    onManageHours: () -> Unit,
    hoursLoading: Boolean,
    hoursFailed: Boolean,
    retryHours: () -> Unit,
    modifier: Modifier,
    targetHeight: Dp,
    stackReadingCards: Boolean,
    today: LocalDate,
    seasonLabel: String?,
    suggested: Hour?,
    hours: List<Hour>,
    readingsState: HomeReadingsState,
    isWazema: Boolean = false,
    horizonMode: HomeHorizonMode = HomeHorizonMode.DEVOTIONAL_HEATMAP,
    onToggleHorizonMode: () -> Unit = {},
    habitIds: List<String>,
    doneToday: Set<String>,
    habitState: HabitsState,
    onOpenHour: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFasting: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenPrayerList: () -> Unit,
    onOpenAllHours: () -> Unit,
    onOpenGitsawe: () -> Unit,
    onOpenJourney: () -> Unit,
    onOpenPsalter: () -> Unit,
    onOpenZewotr: () -> Unit,
    dailyQuote: com.sinq.app.model.DailyQuote? = null,
    isDailyQuoteBookmarked: Boolean = false,
    onToggleDailyQuoteBookmark: () -> Unit = {},
    onOpenDailyQuote: () -> Unit = {},
    onOpenDesertFathersHub: () -> Unit = {},
) {
    val cardScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    // The ceilings rise with the type size, so a page set large can still use
    // the room it has instead of stopping at a height meant for small text.
    val room = LocalDensity.current.fontScale.coerceAtLeast(1f)

    // ቤት is one screen, and it ends where the screen ends. Each block keeps the
    // height its own content asks for; the marked grow blocks share what is left
    // over, and what their ceilings turn down becomes air between them.
    val nowWeight = 3f
    val nowCeiling = NOW_CEILING * room
    val gitsaweWeight = 2f
    val gitsaweCeiling = GITSAWE_CEILING * room

    FillColumn(targetHeight = targetHeight, gap = Spacing.sm, modifier = modifier) {
        DayHeader(today, seasonLabel, onOpenSearch, onOpenFasting, onOpenBookmarks, onOpenPrayerList)
        if (suggested != null) {
            NowCard(
                hour = suggested,
                prayed = HabitsRepository.hourHabitId(suggested.id) in doneToday,
                onClick = { onOpenHour(suggested.id) },
                modifier = Modifier.grow(nowWeight, max = nowCeiling),
            )
            HoursLine(
                next = com.sinq.app.data.PrayerSchedule.next(hours, suggested.id),
                onOpenAll = onOpenAllHours,
            )
        } else {
            when {
                hoursLoading -> com.sinq.app.ui.common.LoadingPanel()
                hoursFailed -> com.sinq.app.ui.common.StatePanel(
                    title = LocalStrings.current.contentUnavailable,
                    actionLabel = LocalStrings.current.retryAction, onAction = retryHours,
                )
                else -> EmptyHoursCard(onManageHours)
            }
        }
        GitsaweCard(
            state = readingsState,
            isWazema = isWazema,
            onClick = onOpenGitsawe,
            modifier = Modifier.grow(gitsaweWeight, max = gitsaweCeiling),
        )
        TodayAndQuoteSection(
            records = habitState.records,
            today = today,
            onClick = onOpenJourney,
            scale = cardScale,
            horizonMode = horizonMode,
            onToggleHorizonMode = onToggleHorizonMode,
            dailyQuote = dailyQuote,
            isDailyQuoteBookmarked = isDailyQuoteBookmarked,
            onToggleDailyQuoteBookmark = onToggleDailyQuoteBookmark,
            onOpenDailyQuote = onOpenDailyQuote,
            onOpenDesertFathersHub = onOpenDesertFathersHub,
            modifier = Modifier.grow(1f, max = (if (dailyQuote != null) 126.dp else TODAY_CEILING) * room),
        )
        ShortcutsRow(stackReadingCards, today, onOpenPsalter, onOpenZewotr)
    }
}

/**
 * How tall a growing block may become.
 *
 * A hero card given the whole surplus turns into a slab of green with two lines
 * of text adrift in it; past these heights the page is better served by the
 * space going between the blocks instead. ዛሬ stops a little above its own
 * height because the grid it carries can only use so much.
 */
private val NOW_CEILING = 200.dp
private val GITSAWE_CEILING = 168.dp
private val TODAY_CEILING = 110.dp

/** መዝሙረ ዳዊት and ውዳሴ ማርያም, side by side unless the page is too narrow for it. */
@Composable
private fun ShortcutsRow(
    stacked: Boolean,
    today: LocalDate,
    onOpenPsalter: () -> Unit,
    onOpenZewotr: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (stacked) {
        Column(modifier.fillMaxWidth()) {
            DailyPsalmCard(today, onOpenPsalter, Modifier.fillMaxWidth())
            Spacer(Modifier.height(Spacing.sm))
            ZewotrCard(onOpenZewotr, Modifier.fillMaxWidth())
        }
    } else {
        // IntrinsicSize.Min so the pair matches the taller of the two rather
        // than a guessed height — they read as one row either way.
        Row(
            modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            DailyPsalmCard(today, onOpenPsalter, Modifier.weight(1f).fillMaxHeight())
            ZewotrCard(onOpenZewotr, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun DayHeader(
    today: LocalDate,
    seasonLabel: String?,
    onOpenSearch: () -> Unit,
    onOpenFasting: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenPrayerList: () -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val profileName by com.sinq.app.data.SettingsRepository.profileName(context).collectAsState(initial = "")
    val christianName by com.sinq.app.data.SettingsRepository.christianName(context).collectAsState(initial = "")
    val callName = christianName.ifBlank { profileName }
    var menuOpen by remember { mutableStateOf(false) }

    // Two lines where there were four. The wordmark went: an app that is open
    // does not need to say its own name, and it was pushing the day — the one
    // thing this header exists to say — into third place.
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(
                com.sinq.app.ui.common.formatEthiopian(today, s),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // The greeting leads in gold and survives the ellipsis; the season
            // and the Gregorian date follow it, quietly, on the same line.
            val greeting = callName.takeIf { it.isNotBlank() }?.let { s.greeting(it) }
            val tail = listOfNotNull(
                com.sinq.app.ui.common.formatGregorianShort(today, s).takeIf { it.isNotBlank() },
                seasonLabel,
            ).joinToString("  ·  ")
            Text(
                buildAnnotatedString {
                    if (greeting != null) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                            append(greeting)
                        }
                        if (tail.isNotEmpty()) append("  ·  ")
                    }
                    append(tail)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onOpenSearch) {
            Icon(Icons.Outlined.Search, contentDescription = s.tabSearch, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = s.more, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                HomeMenuItem(Icons.Outlined.CalendarMonth, s.fastingTitle) { menuOpen = false; onOpenFasting() }
                HomeMenuItem(Icons.Outlined.Bookmarks, s.bookmarksTitle) { menuOpen = false; onOpenBookmarks() }
                HomeMenuItem(Icons.Outlined.VolunteerActivism, s.prayerListTitle) { menuOpen = false; onOpenPrayerList() }
            }
        }
    }
}

@Composable
private fun HomeMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
    )
}

/**
 * The hour due now — its name, its time, and whether it has been prayed.
 *
 * What follows it, and the way to all of them, is [HoursLine] beneath: the hero
 * says one thing, and the strip under the hairline carries the sequence.
 *
 * The chip on the right is the only prayer state on the page above the fold:
 * አሁን while the hour is due, a lit candle once it has been prayed — which the
 * reader no longer has to tell the app, since reaching the foot of the hour
 * records it.
 */
@Composable
private fun NowCard(
    hour: Hour,
    prayed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val sinq = sinqColors
    // The room the ንባብ card left goes here rather than to the gap below it.
    // This is the one card on ቤት that says what to do now, and it was the same
    // height as the two shortcuts under it.
    HeroCard(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.xl),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (s.isAmharic) "በዕለቱ ሰዓት የሚመከር ጸሎት" else "Suggested by time of day",
                        style = MaterialTheme.typography.labelMedium, color = sinq.onHeroMuted)
                    Spacer(Modifier.height(Spacing.xs))
                    val hourDisplayName = PrayerDisplay.hourName(
                        hour.id,
                        rawName = hour.name,
                        transliteration = hour.transliteration,
                        mode = s.languageMode,
                        compact = false,
                    )
                    Text(
                        buildAnnotatedString {
                            append(hourDisplayName)
                            if (hour.timeHint.isNotBlank()) {
                                withStyle(SpanStyle(color = sinq.onHeroMuted)) { append("  ·  ${hour.timeHint}") }
                            }
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = sinq.onHero,
                        // ጸሎተ መንፈቀ ሌሊት with its time hint does not fit one line
                        // on a narrow phone, and clipping the name of the hour
                        // you are being sent to is the wrong thing to clip.
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                if (prayed) {
                    Candle(
                        lit = true,
                        contentDescription = s.journeyTodayLit,
                        bodyColor = sinq.onHeroMuted,
                        flameColor = sinq.onHeroGold,
                        modifier = Modifier.size(width = 13.dp, height = 22.dp),
                    )
                } else {
                    Text(
                        s.nowChip,
                        style = MaterialTheme.typography.labelSmall,
                        color = sinq.onHero,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(sinq.onHero.copy(alpha = 0.16f))
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                    )
                }
            }
        }
    }
}

/**
 * The hours line: what is being prayed now, what follows it, and the way to all
 * of them — on the page itself, between the hero and the ግጻዌ, fenced by a
 * hairline top and bottom.
 *
 * It lived here before and was folded into the hero's foot; it is back out on
 * the page, but without repeating the hour the hero has just named in full.
 * The hero says what is due now, the strip says what comes after it, and
 * ሁሉም opens the rest — each thing said once.
 */
@Composable
private fun HoursLine(next: Hour?, onOpenAll: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    // One block on the page, so its two hairlines and the strip between them
    // travel together — see [HomeDashboard].
    Column(modifier.fillMaxWidth()) {
        SinqDivider()
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                Modifier.size(5.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
            )
            Text(
                text = next?.let {
                    val nextName = PrayerDisplay.hourName(
                        it.id,
                        rawName = it.name,
                        transliteration = it.transliteration,
                        mode = s.languageMode,
                        compact = true,
                    )
                    listOf("${s.hoursNext} $nextName", it.timeHint)
                        .filter { part -> part.isNotBlank() }.joinToString("  ·  ")
                }.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // Its own target, so tapping "all" never opens the current hour.
            Text(
                s.hoursAll,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onOpenAll)
                    .semantics { role = Role.Button }
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
            )
        }
        SinqDivider()
    }
}

@Composable
private fun EmptyHoursCard(onClick: () -> Unit) {
    val s = LocalStrings.current
    SinqCard(onClick = onClick, accented = true, contentPadding = PaddingValues(Spacing.lg)) {
        Text(s.hoursHeader, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(s.manageHours, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}



/** Preserves the full feast and reading content from the existing Home card. */
@Composable
private fun GitsaweCard(
    state: HomeReadingsState,
    isWazema: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val readings = (state as? HomeReadingsState.Ready)?.readings
    val feast = readings?.feasts?.firstOrNull()?.amharicName
    val reading = readings?.daily?.title
        ?: readings?.seasonal?.firstOrNull()?.title
        ?: readings?.monthly?.firstOrNull()?.let { it.title ?: it.raw }
        ?: readings?.sundayCycle?.firstOrNull()?.title
    val mezmur = readings?.sundayCycle?.firstNotNullOfOrNull { it.mezmur }
    val lectionaryTitle = reading ?: feast
    val kickerText = when {
        isWazema -> if (feast != null) s.wazemaEveOf(feast) else s.wazemaBadge
        feast != null && reading != null -> feast
        else -> null
    }

    SinqCard(
        onClick = onClick,
        accented = true,
        fillMaxHeight = true,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(IconSize.large),
                )
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    if (kickerText != null) {
                        Text(
                            text = kickerText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(Spacing.xxs))
                    }
                    Text(s.gitsaweTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    when (state) {
                        HomeReadingsState.Loading -> Text(s.loadingLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HomeReadingsState.Unavailable -> Text(s.contentMissingTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        is HomeReadingsState.Ready -> {
                            lectionaryTitle?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            mezmur?.let { Text("${s.sundayMezmurTitle} · $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                        }
                    }
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
/**
 * Merged Today (ዛሬ) and Desert Father saying (የአበው ምክር) section.
 *
 * Today sits upper and Desert Father below, sharing a single middle dividing
 * hairline to eliminate duplicate parallel divider lines.
 */
@Composable
private fun TodayAndQuoteSection(
    records: Map<String, Set<String>>,
    today: LocalDate,
    onClick: () -> Unit,
    scale: Float,
    horizonMode: HomeHorizonMode,
    onToggleHorizonMode: () -> Unit,
    dailyQuote: com.sinq.app.model.DailyQuote?,
    isDailyQuoteBookmarked: Boolean,
    onToggleDailyQuoteBookmark: () -> Unit,
    onOpenDailyQuote: () -> Unit,
    onOpenDesertFathersHub: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SinqDivider()
        TodayRowContent(
            records = records,
            today = today,
            onClick = onClick,
            scale = scale,
            horizonMode = horizonMode,
            onToggleHorizonMode = onToggleHorizonMode,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 60.dp),
        )
        if (dailyQuote != null) {
            SinqDivider()
            DailyQuoteRowContent(
                quote = dailyQuote,
                isBookmarked = isDailyQuoteBookmarked,
                onToggleBookmark = onToggleDailyQuoteBookmark,
                onClick = onOpenDailyQuote,
                onOpenHub = onOpenDesertFathersHub,
            )
        }
        SinqDivider()
    }
}

@Composable
private fun TodayRowContent(
    records: Map<String, Set<String>>,
    today: LocalDate,
    onClick: () -> Unit,
    scale: Float,
    horizonMode: HomeHorizonMode,
    onToggleHorizonMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val summary = remember(records, today) { PrayerJourney.summarize(records, today) }
    // The heatmap is drawn in dp, so it ignores the font scale that stretches
    // the text beside it. It grows with the row, but capped: past 1.5x it
    // would take the width that text needs.
    val glyphScale = scale.coerceAtMost(1.5f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.todayLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(Spacing.sm))
                Candle(
                    lit = summary.prayedToday,
                    contentDescription = if (summary.prayedToday) s.journeyTodayLit else s.journeyTodayUnlit,
                    bodyColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    flameColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(width = 13.dp * glyphScale, height = 22.dp * glyphScale),
                )
            }
            Text(
                journeyLine(summary, s),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(Spacing.xs))
        when (horizonMode) {
            HomeHorizonMode.DEVOTIONAL_HEATMAP -> {
                Box(
                    modifier = Modifier
                        .clickable(onClick = onToggleHorizonMode)
                        .semantics { role = Role.Button },
                ) {
                    CompactHeatmap(
                        records = records,
                        today = today,
                        weeks = 12,
                        maxCell = 11.dp * glyphScale,
                    )
                }
            }
            HomeHorizonMode.LITURGICAL_SEASON_HORIZON -> {
                LiturgicalSeasonArc(
                    today = today,
                    onClick = onToggleHorizonMode,
                    scale = glyphScale,
                )
            }
        }
    }
}

@Composable
private fun ShortcutCard(
    title: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // SinqCard lays its content out in a Column, so the row has to be one:
    // Modifier.weight in a column whose height is its content measures to
    // zero, which is why these cards shipped in 1.8.1 drawing nothing at all.
    SinqCard(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize.small),
        )
      }
    }
}

@Composable
private fun DailyPsalmCard(today: LocalDate, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val range = remember(today) { com.sinq.app.ui.psalter.dailyRange(today.dayOfWeek) }
    ShortcutCard(
        title = s.psalterTitle,
        caption = range?.let { s.psalmRange(it.first, it.last) } ?: s.dailyPsalms,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun ZewotrCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    ShortcutCard(
        title = s.wudaseMariam,
        caption = s.zewotrTselot,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun AllHoursSheet(hours: List<Hour>, currentHourId: String?, onOpenHour: (String) -> Unit) {
    val s = LocalStrings.current
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen).padding(bottom = Spacing.xxl)) {
        Text(s.hoursHeader, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Spacing.md))
        if (hours.isEmpty()) {
            Text(s.manageHours, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            hours.forEachIndexed { index, hour ->
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(MaterialTheme.shapes.small).clickable { onOpenHour(hour.id) }.padding(vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        val hourTitle = PrayerDisplay.hourName(
                            hour.id,
                            rawName = hour.name,
                            transliteration = hour.transliteration,
                            mode = s.languageMode,
                            compact = false,
                        )
                        Text(hourTitle, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        if (hour.id == currentHourId) {
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                s.currentHourBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)).padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                            )
                        }
                    }
                    Text(hour.timeHint, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (index < hours.lastIndex) SinqDivider()
            }
        }
    }
}

/**
 * Ultra-compact single-line hairline row content for today's Desert Father saying (የአበው ምክር).
 */
@Composable
private fun DailyQuoteRowContent(
    quote: com.sinq.app.model.DailyQuote,
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    onClick: () -> Unit,
    onOpenHub: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val gold = MaterialTheme.colorScheme.secondary
    val author = quote.authorAm.ifBlank { quote.authorEn }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "☩",
                    style = MaterialTheme.typography.labelSmall,
                    color = gold,
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = s.dailyQuoteChannelName,
                    style = MaterialTheme.typography.titleSmall,
                    color = gold,
                    maxLines = 1,
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = s.bookmarkAction,
                        tint = if (isBookmarked) gold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(IconSize.small),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(IconSize.small),
                )
            }
        }
        val quoteText = quote.quote.trim()
        if (quoteText.isNotBlank()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "“$quoteText”",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
