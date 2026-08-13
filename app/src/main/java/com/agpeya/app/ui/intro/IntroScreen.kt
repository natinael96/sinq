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
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.inReadingFont
import kotlinx.coroutines.launch

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

/** The three headline features — offered after the name, replayable from Settings. */
private fun tutorialPages(s: Strings): List<IntroPage> = listOf(
    IntroPage(icon = Icons.Outlined.NotificationsActive, title = s.introRemindersTitle, body = s.introRemindersBody),
    IntroPage(icon = Icons.Outlined.Route, title = s.introJourneyTitle, body = s.introJourneyBody),
    IntroPage(icon = Icons.AutoMirrored.Outlined.MenuBook, title = s.introPsalterTitle, body = s.introPsalterBody),
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

    fun saveName() {
        scope.launch {
            if (name.isNotBlank()) SettingsRepository.setProfileName(context, name)
            if (christianName.isNotBlank()) SettingsRepository.setChristianName(context, christianName)
        }
    }

    when (stage) {
        IntroStage.PAGES -> {
            val pages = introPages(s)
            val pageCount = pages.size + 1 // + the name form
            val pagerState = rememberPagerState(pageCount = { pageCount })
            val isLast = pagerState.currentPage == pageCount - 1
            TourScaffold(
                pagerState = pagerState,
                pageCount = pageCount,
                isLast = isLast,
                finishLabel = s.next, // name page → the tour question, not straight to Home
                onSkip = { saveName(); onDone() },
                onFinish = { saveName(); stage = IntroStage.ASK },
            ) { page ->
                if (page < pages.size) {
                    IntroPageContent(pages[page])
                } else {
                    NameForm(
                        name = name,
                        christianName = christianName,
                        onName = { name = it },
                        onChristianName = { christianName = it },
                    )
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
            Spacer(Modifier.height(12.dp))
            Text(
                text = s.tutorialAskBody,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))
            Button(onClick = onShow, modifier = Modifier.fillMaxWidth()) { Text(s.showTutorial) }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) { Text(s.skip) }
        }
    }
}

@Composable
private fun TourScaffold(
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
                Spacer(Modifier.height(24.dp))
            }
            p.hero != null -> {
                Text(
                    text = p.hero,
                    style = MaterialTheme.typography.headlineMedium.inReadingFont(),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
        Text(
            text = p.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = s.introNameBody,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            singleLine = true,
            label = { Text(s.yourNameLabel) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = christianName,
            onValueChange = onChristianName,
            singleLine = true,
            label = { Text(s.christianNameLabel) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
