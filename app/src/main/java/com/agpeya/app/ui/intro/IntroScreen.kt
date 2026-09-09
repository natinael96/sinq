package com.agpeya.app.ui.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.PrayerLevel
import androidx.compose.foundation.clickable
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.inReadingFont
import kotlinx.coroutines.launch
import com.agpeya.app.ui.theme.Spacing

/** One tour page: an icon (or wordmark hero) above a title and a line of body. */
private data class IntroPage(
    val icon: ImageVector? = null,
    val hero: String? = null,
    val title: String,
    val body: String,
)

/** Shown before the name form: what the app is, and that it's fully offline. */
private fun introPages(s: Strings): List<IntroPage> = listOf(
    IntroPage(hero = "ስንቅ", title = s.introTitle, body = s.introBody),
    IntroPage(icon = Icons.Outlined.CloudOff, title = s.introOfflineTitle, body = s.introOfflineBody),
)

/**
 * The tour of what the app holds — offered after the name, replayable from
 * Settings. Ordered as a day is: what the Church appoints, then the hours, then
 * the reading, then what you keep yourself.
 */
private fun tutorialPages(s: Strings): List<IntroPage> = listOf(
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

private enum class IntroStage { PAGES, ASK, TUTORIAL }

/**
 * First-run flow: intro pages → name → "want a quick tour?" → optional feature
 * tutorial → Home. [onDone] is called exactly once, at the end or on any skip.
 */
@Composable
fun IntroScreen(onDone: () -> Unit) {
    val s = LocalStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var stage by remember { mutableStateOf(IntroStage.PAGES) }
    var name by remember { mutableStateOf("") }
    var christianName by remember { mutableStateOf("") }
    // መጀመሪያ, not ሙሉ. The five levels have always existed and the app has always
    // opened on the longest without asking — two hours of reading a day, where
    // the same seven hours at መጀመሪያ are thirty-six minutes. Writing it here
    // rather than changing the stored default means only people who see this
    // question are answered by it; anyone already praying keeps what they had.
    var level by remember { mutableStateOf(com.agpeya.app.data.PrayerLevel.BEGINNING) }

    fun saveAnswers() {
        scope.launch {
            if (name.isNotBlank()) SettingsRepository.setProfileName(context, name)
            if (christianName.isNotBlank()) SettingsRepository.setChristianName(context, christianName)
            SettingsRepository.setPrayerLevel(context, level)
        }
    }

    when (stage) {
        IntroStage.PAGES -> {
            val pages = introPages(s)
            val pageCount = pages.size + 2 // + the name form + how much to pray
            val pagerState = rememberPagerState(pageCount = { pageCount })
            val isLast = pagerState.currentPage == pageCount - 1
            TourScaffold(
                pagerState = pagerState,
                pageCount = pageCount,
                isLast = isLast,
                finishLabel = s.next, // name page → the tour question, not straight to Home
                onSkip = { saveAnswers(); onDone() },
                onFinish = { saveAnswers(); stage = IntroStage.ASK },
            ) { page ->
                when (page) {
                    in pages.indices -> IntroPageContent(pages[page])
                    pages.size -> NameForm(
                        name = name,
                        christianName = christianName,
                        onName = { name = it },
                        onChristianName = { christianName = it },
                    )
                    else -> LevelForm(level = level, onLevel = { level = it })
                }
            }
        }
        IntroStage.ASK -> TutorialAsk(
            onShow = { stage = IntroStage.TUTORIAL },
            onSkip = onDone,
        )
        IntroStage.TUTORIAL -> {
            val pages = tutorialPages(s)
            val pagerState = rememberPagerState(pageCount = { pages.size })
            val isLast = pagerState.currentPage == pages.size - 1
            TourScaffold(
                pagerState = pagerState,
                pageCount = pages.size,
                isLast = isLast,
                finishLabel = s.gotIt,
                onSkip = onDone,
                onFinish = onDone,
            ) { page -> IntroPageContent(pages[page]) }
        }
    }
}

/** Replayable tour reached from Settings — feature pages only, no name form. */
@Composable
fun TutorialScreen(onDone: () -> Unit) {
    val s = LocalStrings.current
    val pages = tutorialPages(s)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isLast = pagerState.currentPage == pages.size - 1

    TourScaffold(
        pagerState = pagerState,
        pageCount = pages.size,
        isLast = isLast,
        finishLabel = s.gotIt,
        onSkip = onDone,
        onFinish = onDone,
    ) { page -> IntroPageContent(pages[page]) }
}

/** "Want a quick tour?" gate shown after the name — Show me / Skip. */
@Composable
private fun TutorialAsk(onShow: () -> Unit, onSkip: () -> Unit) {
    val s = LocalStrings.current
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = s.tutorialAskTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = s.tutorialAskBody,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.huge))
            Button(onClick = onShow, modifier = Modifier.fillMaxWidth()) { Text(s.showTutorial) }
            Spacer(Modifier.height(Spacing.sm))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text(s.skip) }
        }
    }
}

/** Shared by the first-run tour and the what's-new tour; one pager, one shape. */
@Composable
internal fun TourScaffold(
    pagerState: androidx.compose.foundation.pager.PagerState,
    pageCount: Int,
    isLast: Boolean,
    finishLabel: String,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    page: @Composable (Int) -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onSkip) { Text(s.skip) }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { p -> page(p) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(pageCount) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (active) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Button(
                onClick = {
                    if (isLast) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            ) {
                Text(if (isLast) finishLabel else s.next)
            }
        }
    }
}

@Composable
private fun IntroPageContent(p: IntroPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            p.icon != null -> {
                Icon(
                    imageVector = p.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp),
                )
                Spacer(Modifier.height(Spacing.screen))
            }
            p.hero != null -> {
                Text(
                    text = p.hero,
                    style = MaterialTheme.typography.headlineMedium.inReadingFont(),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(Spacing.screen))
            }
        }
        Text(
            text = p.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = p.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NameForm(
    name: String,
    christianName: String,
    onName: (String) -> Unit,
    onChristianName: (String) -> Unit,
) {
    val s = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = s.introNameTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = s.introNameBody,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.screen))
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            singleLine = true,
            label = { Text(s.yourNameLabel) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = christianName,
            onValueChange = onChristianName,
            singleLine = true,
            label = { Text(s.christianNameLabel) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * How much to pray — the question the app has never asked.
 *
 * Three of the five levels, not all five: the two extremes are reachable in
 * Settings and offering five on a first run turns a welcome into a form.
 *
 * Each is described by how much of the hour it keeps rather than by how long it
 * takes. A minute count would be the more useful answer, but the only reading
 * rate available is an estimate, and a number on this screen is one people plan
 * a morning around.
 */
@Composable
private fun LevelForm(level: PrayerLevel, onLevel: (PrayerLevel) -> Unit) {
    val s = LocalStrings.current
    val offered = listOf(PrayerLevel.BEGINNING, PrayerLevel.GROWTH, PrayerLevel.FULL)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = s.introLevelTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = s.introLevelBody,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.screen))
        offered.forEach { choice ->
            val selected = choice == level
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.sm)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                        else androidx.compose.ui.graphics.Color.Transparent,
                    )
                    .clickable { onLevel(choice) }
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        com.agpeya.app.ui.settings.prayerLevelLabel(choice),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = s.introLevelDesc(choice),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = s.introLevelFooter,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
