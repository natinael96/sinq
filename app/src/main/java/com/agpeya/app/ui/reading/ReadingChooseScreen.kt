package com.agpeya.app.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.agpeya.app.data.ReadingPlanRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.ReadingPlanContent
import com.agpeya.app.model.ReadingPlanState
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Choosing a plan to keep, beside any already kept.
 *
 * A comparison rather than an alert: the three plans read the same books and
 * differ only in what a day costs, so the cost is what the cards lead with —
 * measured in verses, which is the unit they are packed in, and the minutes
 * those verses imply.
 *
 * A plan that would duplicate one already kept is shown with the reason rather
 * than hidden. The year and the six months are the same 1,610 chapters at two
 * speeds; የዳዊት ንባብ beside a Bible plan is not a duplicate but a second
 * rhythm through the psalms, and each keeps its own count.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReadingChooseScreen(onBack: () -> Unit, onStarted: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val today = remember { LocalDate.now() }
    val content by produceState(ReadingPlanContent()) { value = ReadingPlanRepository.content(context) }
    val state by ReadingPlanRepository.state(context).collectAsState(initial = ReadingPlanState())

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.readingChoose, onBack = onBack) },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                Text(
                    s.readingIntro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
            }
            items(content.plans.size) { i ->
                val plan = content.plans[i]
                val keeping = state.kept(plan.id) != null
                val blocker = ReadingPlanRepository.conflict(content, state, plan.id)
                    ?.let { id -> content.plans.firstOrNull { it.id == id }?.title }
                PlanChoice(plan = plan, keeping = keeping, blockedBy = blocker) {
                    scope.launch {
                        ReadingPlanRepository.start(context, plan.id, today)
                        com.agpeya.app.reminders.ReadingReminderScheduler.sync(
                            context,
                            SettingsRepository.readingReminder(context).first(),
                        )
                        onStarted()
                    }
                }
            }
            item {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    s.readingPsalterAlongside,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.huge))
            }
        }
    }
}
