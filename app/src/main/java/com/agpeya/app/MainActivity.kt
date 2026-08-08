package com.agpeya.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.agpeya.app.data.Language
import com.agpeya.app.ui.strings.AmharicStrings
import com.agpeya.app.ui.strings.EnglishStrings
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import java.util.Locale
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.ThemeChoice
import com.agpeya.app.reminders.ReminderScheduler
import com.agpeya.app.reminders.StreakReminderScheduler
import kotlinx.coroutines.flow.first
import com.agpeya.app.ui.bookmarks.BookmarksScreen
import com.agpeya.app.ui.common.Tab
import com.agpeya.app.ui.customize.CustomizeHourScreen
import com.agpeya.app.ui.home.HomeScreen
import com.agpeya.app.ui.modes.ModeEditorScreen
import com.agpeya.app.ui.modes.ModesScreen
import com.agpeya.app.ui.reading.ReadingScreen
import com.agpeya.app.ui.search.SearchScreen
import com.agpeya.app.ui.settings.AboutScreen
import com.agpeya.app.ui.settings.SettingsScreen
import com.agpeya.app.ui.theme.AgpeyaTheme

class MainActivity : ComponentActivity() {

    // The hour to deep-link to from a fired alarm. Observable so a warm launch
    // (onNewIntent, singleTask reuses this instance) reaches the NavHost too.
    private val pendingDeepLinkHourId = mutableStateOf<String?>(null)

    // Set when opened from the nightly streak-reminder notification.
    private val pendingOpenStreak = mutableStateOf(false)

    // Set when opened from the morning ግጻዌ-reminder notification.
    private val pendingOpenGitsawe = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeDeepLink(intent)
        setContent {
            val themeChoice by SettingsRepository.theme(this).collectAsState(initial = ThemeChoice.SYSTEM)
            val language by SettingsRepository.language(this).collectAsState(initial = Language.SYSTEM)
            AgpeyaTheme(themeChoice = themeChoice) {
                CompositionLocalProvider(LocalStrings provides stringsFor(language)) {
                    AgpeyaNavHost(
                        deepLinkHourId = pendingDeepLinkHourId.value,
                        onDeepLinkHandled = { pendingDeepLinkHourId.value = null },
                        openStreak = pendingOpenStreak.value,
                        onStreakHandled = { pendingOpenStreak.value = false },
                        openGitsawe = pendingOpenGitsawe.value,
                        onGitsaweHandled = { pendingOpenGitsawe.value = false },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeDeepLink(intent)
    }

    /** Read the notification extras and strip them, so a rotation can't replay them. */
    private fun consumeDeepLink(intent: Intent?) {
        intent ?: return
        intent.getStringExtra(ReminderScheduler.EXTRA_HOUR_ID)?.let {
            pendingDeepLinkHourId.value = it
            intent.removeExtra(ReminderScheduler.EXTRA_HOUR_ID)
        }
        if (intent.getBooleanExtra(StreakReminderScheduler.EXTRA_OPEN_STREAK, false)) {
            pendingOpenStreak.value = true
            intent.removeExtra(StreakReminderScheduler.EXTRA_OPEN_STREAK)
        }
        if (intent.getBooleanExtra(com.agpeya.app.reminders.GitsaweReminderScheduler.EXTRA_OPEN_GITSAWE, false)) {
            pendingOpenGitsawe.value = true
            intent.removeExtra(com.agpeya.app.reminders.GitsaweReminderScheduler.EXTRA_OPEN_GITSAWE)
        }
    }
}

fun stringsFor(language: Language): Strings = when (language) {
    Language.AMHARIC -> AmharicStrings
    Language.ENGLISH -> EnglishStrings
    Language.SYSTEM -> if (Locale.getDefault().language == "am") AmharicStrings else EnglishStrings
}

/** Switch bottom-nav tabs preserving each tab's state. */
private fun NavController.switchTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(Tab.HOME.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AgpeyaNavHost(
    deepLinkHourId: String?,
    onDeepLinkHandled: () -> Unit,
    openStreak: Boolean,
    onStreakHandled: () -> Unit,
    openGitsawe: Boolean,
    onGitsaweHandled: () -> Unit,
) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    // Wait until we know whether onboarding is done so the start destination is
    // correct from the first frame (no flash of Home before the intro).
    val onboarded by SettingsRepository.onboarded(context).collectAsState(initial = null as Boolean?)

    val ready = onboarded ?: return

    // Self-heal the alarm schedule on every launch. Alarm chains are otherwise
    // only re-armed on boot/update/time-change or a mode edit, so an OEM
    // battery manager force-stopping the app (which cancels all pending alarms)
    // would silence prayer reminders for days until the next reboot. Rearming
    // here is idempotent — rescheduleAll cancels and re-adds; the streak reuses
    // its single alarm — so opening the app restores anything that was dropped.
    LaunchedEffect(ready) {
        if (ready) {
            runCatching {
                val names = com.agpeya.app.data.HoursRepository
                    .visibleHours(context).associate { it.id to it.name }
                ReminderScheduler.rescheduleAll(context, names)
            }
            runCatching {
                StreakReminderScheduler.sync(
                    context,
                    SettingsRepository.streakReminder(context).first(),
                )
            }
            runCatching {
                com.agpeya.app.reminders.GitsaweReminderScheduler.sync(
                    context,
                    SettingsRepository.gitsaweReminder(context).first(),
                )
            }
        }
    }

    // Deep link from a fired alarm. Gated on `ready` so it never runs before the
    // NavHost graph below is composed (navigating earlier crashes the app).
    // Consume it once so it isn't re-navigated on the next recomposition.
    LaunchedEffect(ready, deepLinkHourId) {
        if (ready && deepLinkHourId != null) {
            navController.navigate("reading/$deepLinkHourId") { launchSingleTop = true }
            onDeepLinkHandled()
        }
    }

    // Opened from the streak-reminder notification → jump to the Streak tab.
    LaunchedEffect(ready, openStreak) {
        if (ready && openStreak) {
            navController.switchTab(Tab.STREAK)
            onStreakHandled()
        }
    }

    // Opened from the morning ግጻዌ-reminder notification → open the ግጻዌ screen.
    LaunchedEffect(ready, openGitsawe) {
        if (ready && openGitsawe) {
            navController.navigate("gitsawe") { launchSingleTop = true }
            onGitsaweHandled()
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (ready) Tab.HOME.route else "intro",
    ) {
        composable("intro") {
            com.agpeya.app.ui.intro.IntroScreen(
                onDone = {
                    scope.launch { SettingsRepository.setOnboarded(context) }
                    navController.navigate(Tab.HOME.route) {
                        popUpTo("intro") { inclusive = true }
                    }
                },
            )
        }
        composable(Tab.HOME.route) {
            HomeScreen(
                onOpenHour = { hourId -> navController.navigate("reading/$hourId") { launchSingleTop = true } },
                onOpenSearch = { navController.navigate("search") { launchSingleTop = true } },
                onOpenBookmarks = { navController.navigate("bookmarks") { launchSingleTop = true } },
                onOpenPsalter = { navController.navigate("psalter") { launchSingleTop = true } },
                onOpenWudase = { navController.navigate("wudase") { launchSingleTop = true } },
                onOpenGitsawe = { navController.navigate("gitsawe") { launchSingleTop = true } },
                onSelectTab = navController::switchTab,
            )
        }
        composable("search") {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenResult = { result ->
                    when (result.source) {
                        com.agpeya.app.search.AmharicSearch.Source.HOUR ->
                            navController.navigate("reading/${result.targetId}?section=${result.targetIndex}") { launchSingleTop = true }
                        com.agpeya.app.search.AmharicSearch.Source.PSALTER ->
                            navController.navigate("psalter?section=${result.targetIndex}") { launchSingleTop = true }
                    }
                },
            )
        }
        composable("bookmarks") {
            BookmarksScreen(
                onBack = { navController.popBackStack() },
                onOpen = { hourId, index ->
                    // Psalter bookmarks live under a pseudo hour id and open the
                    // Psalter screen, not the hour reader.
                    if (hourId == com.agpeya.app.ui.psalter.PSALTER_BOOKMARK_ID) {
                        navController.navigate("psalter?section=$index") { launchSingleTop = true }
                    } else {
                        navController.navigate("reading/$hourId?section=$index") { launchSingleTop = true }
                    }
                },
                // Persisted bookmark routes are raw strings replayed verbatim; if a
                // future version renames a route, an old bookmark must not crash.
                onOpenRoute = { route ->
                    runCatching { navController.navigate(route) { launchSingleTop = true } }
                },
            )
        }
        composable(
            route = "psalter?section={section}&start={start}&end={end}",
            arguments = listOf(
                navArgument("section") { type = NavType.IntType; defaultValue = -1 },
                navArgument("start") { type = NavType.IntType; defaultValue = -1 },
                navArgument("end") { type = NavType.IntType; defaultValue = -1 },
            ),
        ) { backStackEntry ->
            com.agpeya.app.ui.psalter.PsalterScreen(
                initialPsalmIndex = backStackEntry.arguments?.getInt("section") ?: -1,
                initialStartVerse = backStackEntry.arguments?.getInt("start") ?: -1,
                initialEndVerse = backStackEntry.arguments?.getInt("end") ?: -1,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Tab.STREAK.route) {
            com.agpeya.app.ui.habits.StreakScreen(
                onSelectTab = navController::switchTab,
                onManageHabits = { navController.navigate("habits") { launchSingleTop = true } },
            )
        }
        composable("habits") {
            com.agpeya.app.ui.habits.ManageHabitsScreen(onBack = { navController.popBackStack() })
        }
        composable(Tab.LIBRARY.route) {
            com.agpeya.app.ui.library.LibraryScreen(
                onOpenPsalter = { navController.navigate("psalter") { launchSingleTop = true } },
                onOpenScriptures = { navController.navigate("scriptures") { launchSingleTop = true } },
                onOpenWudase = { navController.navigate("wudase") { launchSingleTop = true } },
                onSelectTab = navController::switchTab,
            )
        }
        composable("wudase") {
            com.agpeya.app.ui.library.WudaseMaryamScreen(onBack = { navController.popBackStack() })
        }
        composable("scriptures") {
            com.agpeya.app.ui.library.ScriptureListScreen(
                onBack = { navController.popBackStack() },
                onOpenBook = { key -> navController.navigate("scripture/$key/1") { launchSingleTop = true } },
            )
        }
        composable(
            route = "scripture/{book}/{chapter}?start={start}&end={end}",
            arguments = listOf(
                navArgument("book") { type = NavType.StringType },
                navArgument("chapter") { type = NavType.IntType; defaultValue = 1 },
                navArgument("start") { type = NavType.IntType; defaultValue = -1 },
                navArgument("end") { type = NavType.IntType; defaultValue = -1 },
            ),
        ) { backStackEntry ->
            com.agpeya.app.ui.library.ScriptureReaderScreen(
                bookKey = backStackEntry.arguments?.getString("book") ?: "matthew",
                initialChapter = backStackEntry.arguments?.getInt("chapter") ?: 1,
                initialStart = backStackEntry.arguments?.getInt("start") ?: -1,
                initialEnd = backStackEntry.arguments?.getInt("end") ?: -1,
                onBack = { navController.popBackStack() },
            )
        }
        composable("gitsawe") {
            com.agpeya.app.ui.gitsawe.GitsaweScreen(
                onBack = { navController.popBackStack() },
                onOpenReading = { target ->
                    navController.navigate(com.agpeya.app.data.GitsaweLinks.route(target)) { launchSingleTop = true }
                },
                onOpenSynaxarium = { epochDay -> navController.navigate("synaxarium/$epochDay") { launchSingleTop = true } },
            )
        }
        composable(
            route = "synaxarium/{epochDay}",
            arguments = listOf(navArgument("epochDay") { type = NavType.LongType }),
        ) { backStackEntry ->
            com.agpeya.app.ui.gitsawe.SynaxariumScreen(
                epochDay = backStackEntry.arguments?.getLong("epochDay") ?: 0L,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Tab.SETTINGS.route) {
            SettingsScreen(
                onSelectTab = navController::switchTab,
                onOpenModes = { navController.navigate("modes") { launchSingleTop = true } },
                onOpenCustomize = { navController.navigate("customize") { launchSingleTop = true } },
                onOpenTutorial = { navController.navigate("tutorial") { launchSingleTop = true } },
                onOpenAbout = { navController.navigate("about") { launchSingleTop = true } },
            )
        }
        composable(
            route = "reading/{hourId}?section={section}",
            arguments = listOf(
                navArgument("hourId") { type = NavType.StringType },
                navArgument("section") { type = NavType.IntType; defaultValue = -1 },
            ),
        ) { backStackEntry ->
            ReadingScreen(
                hourId = backStackEntry.arguments?.getString("hourId") ?: "morning",
                initialSectionIndex = backStackEntry.arguments?.getInt("section") ?: -1,
                onBack = { navController.popBackStack() },
                onSwitchHour = { id ->
                    navController.navigate("reading/$id") {
                        popUpTo("reading/{hourId}?section={section}") { inclusive = true }
                    }
                },
            )
        }
        composable("modes") {
            ModesScreen(
                onBack = { navController.popBackStack() },
                onEditMode = { modeId -> navController.navigate("mode/$modeId") { launchSingleTop = true } },
                onOpenBatteryHelp = { navController.navigate("battery") { launchSingleTop = true } },
            )
        }
        composable("battery") {
            com.agpeya.app.ui.settings.BatteryHelpScreen(onBack = { navController.popBackStack() })
        }
        composable("mode/{modeId}") { backStackEntry ->
            val modeId = backStackEntry.arguments?.getString("modeId") ?: return@composable
            ModeEditorScreen(modeId = modeId, onBack = { navController.popBackStack() })
        }
        composable("customize") {
            com.agpeya.app.ui.hours.ManageHoursScreen(
                onBack = { navController.popBackStack() },
                onEditHour = { hourId -> navController.navigate("customize/$hourId") { launchSingleTop = true } },
            )
        }
        composable("customize/{hourId}") { backStackEntry ->
            val hourId = backStackEntry.arguments?.getString("hourId") ?: return@composable
            CustomizeHourScreen(hourId = hourId, onBack = { navController.popBackStack() })
        }
        composable("tutorial") {
            com.agpeya.app.ui.intro.TutorialScreen(onDone = { navController.popBackStack() })
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
