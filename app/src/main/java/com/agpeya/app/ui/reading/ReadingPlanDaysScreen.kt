package com.agpeya.app.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.agpeya.app.data.ReadingPlanRepository
import com.agpeya.app.model.ReadingPlanContent
import com.agpeya.app.model.ReadingPlanState
import com.agpeya.app.ui.common.ListRow
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import java.time.LocalDate

/**
 * Every day of the plan, read or not.
 *
 * Deliberately a plain list rather than a progress chart: the honest picture of
 * a year's reading is which days were kept, not a percentage of a book. Days
 * ahead are openable too — nothing here is locked behind yesterday.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReadingPlanDaysScreen(onBack: () -> Unit, onOpenRoute: (String) -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val today = remember { LocalDate.now() }

    val content by produceState(ReadingPlanContent()) { value = ReadingPlanRepository.content(context) }
    val state by ReadingPlanRepository.state(context).collectAsState(initial = ReadingPlanState())
    val plan = content.plans.firstOrNull { it.id == state.activePlanId }
    val listState = rememberLazyListState()

    val currentDay = plan?.let { ReadingPlanRepository.dayOn(state.startedOn, today, it.days) } ?: 1
    // Open where the person actually is, not at day 1 of a year.
    LaunchedEffect(plan?.id, currentDay) {
        if (plan != null) listState.scrollToItem((currentDay - 1).coerceAtLeast(0))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.readingAllDays, onBack = onBack) },
    ) { inner ->
        if (plan == null) {
            Text(
                s.readingNoPlan,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(inner).padding(Spacing.screen),
            )
            return@Scaffold
        }
        val read = state.readDays(plan.id)
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            state = listState,
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            items(plan.readings.size) { i ->
                val day = plan.readings[i]
                val passages = day.r.joinToString(" · ") { r ->
                    val name = r.b.split('-').joinToString(" ") { p ->
                        p.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
                    }
                    if (r.to > r.c) "$name ${r.c}–${r.to}" else "$name ${r.c}"
                }
                ListRow(
                    title = s.readingDayLabel(day.d.toString()),
                    subtitle = passages,
                    onClick = { day.r.firstOrNull()?.let { onOpenRoute("scripture/${it.b}/${it.c}") } },
                    trailing = {
                        if (day.d in read) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = s.readingDone,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    },
                )
            }
            item { Spacer(Modifier.height(Spacing.huge)) }
        }
    }
}
