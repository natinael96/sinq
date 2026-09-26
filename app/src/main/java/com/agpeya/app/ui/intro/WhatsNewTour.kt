package com.agpeya.app.ui.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.agpeya.app.model.Tour
import com.agpeya.app.model.TourPage
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing

/**
 * The walkthrough shown once after an install or an update.
 *
 * Longer than the changelog on purpose: a release note says what changed, this
 * says what the thing is for and where it lives.
 *
 * Optional page actions open the feature immediately. The tour remains replayable
 * from Settings when the reader wants to return to the remaining pages.
 *
 * Skipping and finishing are the same outcome: both mark the version seen, so
 * it is asked once and never nags.
 */
@Composable
fun WhatsNewTour(
    tour: Tour,
    onDone: () -> Unit,
    onOpenRoute: ((String) -> Unit)? = null,
) {
    val s = LocalStrings.current
    val pages = tour.pages
    if (pages.isEmpty()) {
        onDone()
        return
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isLast = pagerState.currentPage == pages.size - 1

    TourScaffold(
        pagerState = pagerState,
        pageCount = pages.size,
        isLast = isLast,
        finishLabel = s.gotIt,
        onSkip = onDone,
        onFinish = onDone,
    ) { i ->
        TourPageContent(page = pages[i], amharic = s.isAmharic, onOpenRoute = onOpenRoute)
    }
}

@Composable
private fun TourPageContent(page: TourPage, amharic: Boolean, onOpenRoute: ((String) -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val kicker = page.kicker.pick(amharic)
        if (kicker.isNotBlank()) {
            Text(
                kicker,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
        }
        Text(
            page.title.pick(amharic),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            page.body.pick(amharic),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (page.route != null && onOpenRoute != null && !page.actionLabel.isBlank) {
            Spacer(Modifier.height(Spacing.md))
            androidx.compose.material3.TextButton(onClick = { onOpenRoute(page.route) }) {
                Text(page.actionLabel.pick(amharic))
            }
        }
    }
}
