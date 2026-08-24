package com.agpeya.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.agpeya.app.ui.theme.Motion

class MainActivity : ComponentActivity() {

    // The hour to deep-link to from a fired alarm. Observable so a warm launch
    // (onNewIntent, singleTask reuses this instance) reaches the NavHost too.
    private val pendingDeepLinkHourId = mutableStateOf<String?>(null)

    // Set when opened from the nightly reminder notification.
    private val pendingOpenJourney = mutableStateOf(false)

    // Set when opened from the morning ግጻዌ-reminder notification.
    private val pendingOpenGitsawe = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeDeepLink(intent)
        setContent {
            val themeChoice by SettingsRepository.theme(this).collectAsState(initial = ThemeChoice.SYSTEM)
            val language by SettingsRepository.language(this).collectAsState(initial = Language.SYSTEM)
            val readingFont by SettingsRepository.readingFont(this)
                .collectAsState(initial = com.agpeya.app.data.ReadingFont.ABYSSINICA)
            val readingLineSpacing by SettingsRepository.readingLineSpacing(this)
                .collectAsState(initial = com.agpeya.app.data.ReadingLineSpacing.NORMAL)
            AgpeyaTheme(themeChoice = themeChoice) {
                CompositionLocalProvider(
                    LocalStrings provides stringsFor(language),
                    com.agpeya.app.ui.theme.LocalReadingFont provides
                        com.agpeya.app.ui.theme.readingFontFamily(readingFont),
                    com.agpeya.app.ui.theme.LocalReadingLineSpacing provides readingLineSpacing.multiplier,
                ) {
                    // The opening reminder sits over the graph rather than inside
                    // it: the start destination already flips once onboarding
                    // resolves, and a third destination in that race would be
                    // fragile. Once per activity launch. The Box is what makes it
                    // an overlay — without it the two are merely siblings.
                    var opened by rememberSaveable { mutableStateOf(false) }
                    Box(Modifier.fillMaxSize()) {
                        AgpeyaNavHost(
                            deepLinkHourId = pendingDeepLinkHourId.value,
                            onDeepLinkHandled = { pendingDeepLinkHourId.value = null },
                            openJourney = pendingOpenJourney.value,
                            onJourneyHandled = { pendingOpenJourney.value = false },
                            openGitsawe = pendingOpenGitsawe.value,
                            onGitsaweHandled = { pendingOpenGitsawe.value = false },
                        )
                        if (!opened) {
                            com.agpeya.app.ui.intro.MementoMoriScreen(onDone = { opened = true })
                        }
                    }
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
            pendingOpenJourney.value = true
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
    openJourney: Boolean,
    onJourneyHandled: () -> Unit,
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
    // here is idempotent — rescheduleAll cancels and re-adds; the nightly nudge
    // reuses its single alarm — so opening the app restores anything dropped.
    LaunchedEffect(ready) {
        if (ready) {
            runCatching {
                com.agpeya.app.data.HighlightRepository.migrateLegacyPsalterKeys(context)
            }
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
            runCatching {
                com.agpeya.app.reminders.BreathPrayerScheduler.sync(
                    context,
                    SettingsRepository.breathReminder(context).first(),
                )
            }
            runCatching {
                com.agpeya.app.reminders.SpecialHabitReminderScheduler.sync(
                    context,
                    com.agpeya.app.reminders.SpecialHabit.ALMS,
                )
            }
            runCatching {
                com.agpeya.app.reminders.SpecialHabitReminderScheduler.sync(
                    context,
                    com.agpeya.app.reminders.SpecialHabit.REPENTANCE,
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

    // Opened from the nightly reminder notification → jump to the Journey tab.
    LaunchedEffect(ready, openJourney) {
        if (ready && openJourney) {
            navController.switchTab(Tab.JOURNEY)
            onJourneyHandled()
        }
    }

    // Opened from the morning ግጻዌ-reminder notification → open the ግጻዌ screen.
    LaunchedEffect(ready, openGitsawe) {
        if (ready && openGitsawe) {
            navController.navigate("gitsawe") { launchSingleTop = true }
            onGitsaweHandled()
        }
    }

    // Freeze the start destination at the first resolved value. Finishing
    // onboarding writes the flag asynchronously; when that emits, `ready` flips
    // and an expression here would re-anchor the graph under the user — who may
    // by then have navigated somewhere else. The intro navigates on explicitly.
    val startDestination = rememberSaveable { if (ready) Tab.HOME.route else "intro" }

    // One motion for the whole graph: the page fades and drifts a fraction of a
    // screen in the direction of travel. Short (220ms) and small (a twelfth of
    // the width) — enough to say "this came from there" and not enough to make
    // anyone wait for it. At zero animation scale every duration below collapses
    // to nothing, so the system's reduce-motion setting is honoured rather than
    // approximated.
    val motion = com.agpeya.app.ui.theme.LocalMotion.current
    val fadeIn = androidx.compose.animation.fadeIn(motion.spec(Motion.standard))
    val fadeOut = androidx.compose.animation.fadeOut(motion.spec(Motion.fast))

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn + androidx.compose.animation.slideInHorizontally(motion.spec(Motion.standard)) { it / 12 }
        },
        exitTransition = { fadeOut },
        popEnterTransition = { fadeIn },
        popExitTransition = {
            fadeOut + androidx.compose.animation.slideOutHorizontally(motion.spec(Motion.fast)) { it / 12 }
        },
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
                onOpenFasting = { navController.navigate("fasting") { launchSingleTop = true } },
                onOpenBookmarks = { navController.navigate("bookmarks") { launchSingleTop = true } },
                onOpenPrayerList = { navController.navigate("prayerlist") { launchSingleTop = true } },
                onOpenPsalter = { navController.navigate("psalter") { launchSingleTop = true } },
                onOpenGitsawe = { navController.navigate("gitsawe") { launchSingleTop = true } },
                onSelectTab = navController::switchTab,
            )
        }
        composable("prayerlist") {
            com.agpeya.app.ui.prayerlist.PrayerListScreen(onBack = { navController.popBackStack() })
        }
        composable("fasting") {
            com.agpeya.app.ui.fasting.FastingScreen(onBack = { navController.popBackStack() })
        }
        composable("search") {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenResult = { result ->
                    // Newer corpora carry their own route; the original two are
                    // still addressed by (targetId, targetIndex).
                    val route = result.route ?: when (result.source) {
                        com.agpeya.app.search.AmharicSearch.Source.PSALTER ->
                            "psalter?section=${result.targetIndex}"
                        else ->
                            "reading/${result.targetId}?section=${result.targetIndex}"
                    }
                    runCatching { navController.navigate(route) { launchSingleTop = true } }
                },
            )
        }
        composable("bookmarks") {
            BookmarksScreen(
                onBack = { navController.popBackStack() },
                onOpen = { hourId, index, sectionId ->
                    // Psalter bookmarks live under a pseudo hour id and open the
                    // Psalter screen, not the hour reader.
                    if (hourId == com.agpeya.app.ui.psalter.PSALTER_BOOKMARK_ID) {
                        navController.navigate("psalter?section=$index") { launchSingleTop = true }
                    } else {
                        // Open by section id — a stored index goes stale the moment
                        // the user reorders or hides sections in that hour.
                        navController.navigate(
                            "reading/$hourId?section=$index&sectionId=${android.net.Uri.encode(sectionId)}",
                        ) { launchSingleTop = true }
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
            route = "psalter?section={section}&start={start}&end={end}&lang={lang}",
            arguments = listOf(
                navArgument("section") { type = NavType.IntType; defaultValue = -1 },
                navArgument("start") { type = NavType.IntType; defaultValue = -1 },
                navArgument("end") { type = NavType.IntType; defaultValue = -1 },
                navArgument("lang") { type = NavType.StringType; defaultValue = "am" },
            ),
        ) { backStackEntry ->
            com.agpeya.app.ui.psalter.PsalterScreen(
                initialPsalmIndex = backStackEntry.arguments?.getInt("section") ?: -1,
                initialStartVerse = backStackEntry.arguments?.getInt("start") ?: -1,
                initialEndVerse = backStackEntry.arguments?.getInt("end") ?: -1,
                initialGeez = backStackEntry.arguments?.getString("lang") == "gez",
                onBack = { navController.popBackStack() },
            )
        }
        composable(Tab.JOURNEY.route) {
            com.agpeya.app.ui.habits.JourneyScreen(
                onSelectTab = navController::switchTab,
                onManageHabits = { navController.navigate("habits") { launchSingleTop = true } },
            )
        }
        composable("habits") {
            com.agpeya.app.ui.habits.ManageHabitsScreen(onBack = { navController.popBackStack() })
        }
        composable(Tab.LIBRARY.route) {
            com.agpeya.app.ui.library.LibraryScreen(
                onOpenScriptures = { navController.navigate("scriptures") { launchSingleTop = true } },
                onOpenWudase = { navController.navigate("wudase") { launchSingleTop = true } },
                onOpenZewotr = { navController.navigate("wudase?sec=daily") { launchSingleTop = true } },
                onSelectTab = navController::switchTab,
            )
        }
        composable(
            route = "seatat?sec={sec}",
            arguments = listOf(navArgument("sec") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { backStackEntry ->
            com.agpeya.app.ui.library.SeatatScreen(
                onBack = { navController.popBackStack() },
                initialSectionId = backStackEntry.arguments?.getString("sec"),
            )
        }
        composable(
            route = "wudase?sec={sec}",
            arguments = listOf(navArgument("sec") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { backStackEntry ->
            com.agpeya.app.ui.library.WudaseMaryamScreen(
                onBack = { navController.popBackStack() },
                initialSectionId = backStackEntry.arguments?.getString("sec"),
            )
        }
        composable("scriptures") {
            com.agpeya.app.ui.library.ScriptureHubScreen(
                onBack = { navController.popBackStack() },
                onOpenOldTestament = { navController.navigate("scripture/books/old") { launchSingleTop = true } },
                onOpenNewTestament = { navController.navigate("scripture/books/new") { launchSingleTop = true } },
                onOpenPsalms = { navController.navigate("psalter") { launchSingleTop = true } },
            )
        }
        composable("scripture/books/{testament}") { backStackEntry ->
            com.agpeya.app.ui.library.ScriptureListScreen(
                testament = backStackEntry.arguments?.getString("testament") ?: "new",
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
                // A section opens its own focused passage page; the full reader
                // is reached from there ("open the book / chapter"), not here.
                onOpenReading = { target, role ->
                    navController.navigate(
                        com.agpeya.app.data.GitsaweLinks.passageRoute(target, role),
                    ) { launchSingleTop = true }
                },
                onOpenSynaxarium = { epochDay -> navController.navigate("synaxarium/$epochDay") { launchSingleTop = true } },
            )
        }
        composable(
            route = "gitsawePassage?psalm={psalm}&book={book}&chapter={chapter}&start={start}&end={end}&role={role}",
            arguments = listOf(
                navArgument("psalm") { type = NavType.IntType; defaultValue = -1 },
                navArgument("book") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("chapter") { type = NavType.IntType; defaultValue = 1 },
                navArgument("start") { type = NavType.IntType; defaultValue = -1 },
                navArgument("end") { type = NavType.IntType; defaultValue = -1 },
                navArgument("role") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val psalm = args?.getInt("psalm") ?: -1
            val bookKey = args?.getString("book")
            val chapter = args?.getInt("chapter") ?: 1
            val start = args?.getInt("start") ?: -1
            val end = args?.getInt("end") ?: -1
            // Rebuild the target once; both doors out derive their routes from
            // it, so section, book and chapter can never drift apart.
            val target = if (psalm >= 1) {
                com.agpeya.app.data.ReadingTarget.Psalm(
                    number = psalm,
                    sectionIndex = psalm - 1,
                    startVerse = start.takeIf { it >= 1 },
                    endVerse = end.takeIf { it >= 1 },
                )
            } else {
                com.agpeya.app.data.ReadingTarget.NtPassage(
                    bookKey = bookKey ?: "matthew",
                    chapter = chapter,
                    start = start.takeIf { it >= 1 },
                    end = end.takeIf { it >= 1 },
                )
            }
            com.agpeya.app.ui.gitsawe.GitsawePassageScreen(
                psalm = psalm,
                bookKey = bookKey,
                chapter = chapter,
                start = start,
                end = end,
                role = args?.getString("role"),
                onBack = { navController.popBackStack() },
                onOpenBook = {
                    navController.navigate(com.agpeya.app.data.GitsaweLinks.bookRoute(target)) { launchSingleTop = true }
                },
                onOpenChapter = { geez ->
                    val route = com.agpeya.app.data.GitsaweLinks.chapterRoute(target) +
                        if (target is com.agpeya.app.data.ReadingTarget.Psalm && geez) "&lang=gez" else ""
                    navController.navigate(route) { launchSingleTop = true }
                },
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
                onOpenReading = { navController.navigate("settings/reading") { launchSingleTop = true } },
                onOpenPrayer = { navController.navigate("settings/prayer") { launchSingleTop = true } },
                onOpenReminders = { navController.navigate("settings/reminders") { launchSingleTop = true } },
                onOpenData = { navController.navigate("settings/data") { launchSingleTop = true } },
                onOpenTutorial = { navController.navigate("tutorial") { launchSingleTop = true } },
                onOpenAbout = { navController.navigate("about") { launchSingleTop = true } },
            )
        }
        composable("settings/reading") {
            com.agpeya.app.ui.settings.ReadingSettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenFonts = { navController.navigate("settings/reading/fonts") { launchSingleTop = true } },
            )
        }
        composable("settings/reading/fonts") {
            com.agpeya.app.ui.settings.ReadingFontScreen(onBack = { navController.popBackStack() })
        }
        composable("settings/prayer") {
            com.agpeya.app.ui.settings.PrayerSettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenManageHours = { navController.navigate("customize") { launchSingleTop = true } },
            )
        }
        composable("settings/reminders") {
            com.agpeya.app.ui.settings.RemindersSettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenModes = { navController.navigate("modes") { launchSingleTop = true } },
                onOpenSpecialHabit = { habit ->
                    navController.navigate("intention/${habit.name.lowercase()}") { launchSingleTop = true }
                },
            )
        }
        composable("settings/data") {
            com.agpeya.app.ui.settings.DataSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "intention/{habit}",
            arguments = listOf(navArgument("habit") { type = NavType.StringType }),
        ) { backStackEntry ->
            val habit = when (backStackEntry.arguments?.getString("habit")) {
                "repentance" -> com.agpeya.app.reminders.SpecialHabit.REPENTANCE
                else -> com.agpeya.app.reminders.SpecialHabit.ALMS
            }
            com.agpeya.app.ui.settings.SpecialHabitScreen(
                habit = habit,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "reading/{hourId}?section={section}&sectionId={sectionId}",
            arguments = listOf(
                navArgument("hourId") { type = NavType.StringType },
                navArgument("section") { type = NavType.IntType; defaultValue = -1 },
                navArgument("sectionId") { type = NavType.StringType; nullable = true; defaultValue = null },
            ),
        ) { backStackEntry ->
            ReadingScreen(
                hourId = backStackEntry.arguments?.getString("hourId") ?: "morning",
                initialSectionIndex = backStackEntry.arguments?.getInt("section") ?: -1,
                initialSectionId = backStackEntry.arguments?.getString("sectionId"),
                onBack = { navController.popBackStack() },
                onSwitchHour = { id ->
                    navController.navigate("reading/$id") {
                        popUpTo("reading/{hourId}?section={section}&sectionId={sectionId}") { inclusive = true }
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
