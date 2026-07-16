package com.agpeya.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deepLinkHourId = intent?.getStringExtra(ReminderScheduler.EXTRA_HOUR_ID)
        setContent {
            val themeChoice by SettingsRepository.theme(this).collectAsState(initial = ThemeChoice.SYSTEM)
            val language by SettingsRepository.language(this).collectAsState(initial = Language.SYSTEM)
            AgpeyaTheme(themeChoice = themeChoice) {
                CompositionLocalProvider(LocalStrings provides stringsFor(language)) {
                    AgpeyaNavHost(deepLinkHourId = deepLinkHourId)
                }
            }
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
private fun AgpeyaNavHost(deepLinkHourId: String?) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    // Wait until we know whether onboarding is done so the start destination is
    // correct from the first frame (no flash of Home before the intro).
    val onboarded by SettingsRepository.onboarded(context).collectAsState(initial = null as Boolean?)

    val ready = onboarded ?: return

    // Deep link from a fired alarm. Gated on `ready` so it never runs before the
    // NavHost graph below is composed (navigating earlier crashes the app).
    LaunchedEffect(ready, deepLinkHourId) {
        if (ready && deepLinkHourId != null) navController.navigate("reading/$deepLinkHourId")
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
                onOpenHour = { hourId -> navController.navigate("reading/$hourId") },
                onOpenSearch = { navController.navigate("search") },
                onOpenBookmarks = { navController.navigate("bookmarks") },
                onOpenPsalter = { navController.navigate("psalter") },
                onSelectTab = navController::switchTab,
            )
        }
        composable("search") {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenResult = { hourId, index -> navController.navigate("reading/$hourId?section=$index") },
            )
        }
        composable("bookmarks") {
            BookmarksScreen(
                onBack = { navController.popBackStack() },
                onOpen = { hourId, index ->
                    // Psalter bookmarks live under a pseudo hour id and open the
                    // Psalter screen, not the hour reader.
                    if (hourId == com.agpeya.app.ui.psalter.PSALTER_BOOKMARK_ID) {
                        navController.navigate("psalter?section=$index")
                    } else {
                        navController.navigate("reading/$hourId?section=$index")
                    }
                },
            )
        }
        composable(
            route = "psalter?section={section}",
            arguments = listOf(
                navArgument("section") { type = NavType.IntType; defaultValue = -1 },
            ),
        ) { backStackEntry ->
            com.agpeya.app.ui.psalter.PsalterScreen(
                initialPsalmIndex = backStackEntry.arguments?.getInt("section") ?: -1,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Tab.STREAK.route) {
            com.agpeya.app.ui.habits.StreakScreen(
                onSelectTab = navController::switchTab,
                onManageHabits = { navController.navigate("habits") },
            )
        }
        composable("habits") {
            com.agpeya.app.ui.habits.ManageHabitsScreen(onBack = { navController.popBackStack() })
        }
        composable(Tab.SETTINGS.route) {
            SettingsScreen(
                onSelectTab = navController::switchTab,
                onOpenModes = { navController.navigate("modes") },
                onOpenCustomize = { navController.navigate("customize") },
                onOpenAbout = { navController.navigate("about") },
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
                onEditMode = { modeId -> navController.navigate("mode/$modeId") },
                onOpenBatteryHelp = { navController.navigate("battery") },
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
                onEditHour = { hourId -> navController.navigate("customize/$hourId") },
            )
        }
        composable("customize/{hourId}") { backStackEntry ->
            val hourId = backStackEntry.arguments?.getString("hourId") ?: return@composable
            CustomizeHourScreen(hourId = hourId, onBack = { navController.popBackStack() })
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
