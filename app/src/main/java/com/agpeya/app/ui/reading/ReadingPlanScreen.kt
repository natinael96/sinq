package com.agpeya.app.ui.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.data.ReadingPlanRepository
import com.agpeya.app.data.DayReadings
import com.agpeya.app.model.PlanDay
import com.agpeya.app.model.ReadingPlan
import com.agpeya.app.model.ReadingPlanContent
import com.agpeya.app.model.ReadingPlanState
import com.agpeya.app.ui.common.ListRow
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ንባብ — the day's reading.
 *
 * The ግጻዌ comes first on this screen, always. What the Church appoints is not
 * the app's to reorder, and the plan is explicitly the supplement: it reads the
 * Old Testament and the deuterocanon, which the lectionary never reaches.
 *
 * The lectionary block is never marked done. It is given, not achieved — making
 * it a checkbox would turn the appointed readings into a task.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReadingPlanScreen(
    onBack: () -> Unit,
    onOpenRoute: (String) -> Unit,
    onOpenGitsawe: () -> Unit,
    onOpenAllDays: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val today = remember { LocalDate.now() }

    val content by produceState(ReadingPlanContent()) { value = ReadingPlanRepository.content(context) }
    val state by ReadingPlanRepository.state(context).collectAsState(initial = ReadingPlanState())
    val plan = content.plans.firstOrNull { it.id == state.activePlanId }

    val readings by produceState<DayReadings?>(null, today) {
        value = runCatching { GitsaweRepository.readingsFor(context, today) }.getOrNull()
    }

    var stopping by remember { mutableStateOf(false) }
    var catching by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.readingTitle, onBack = onBack) },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (plan == null) {
                item { ChooserIntro(s.readingIntro) }
                items(content.plans.size) { i ->
                    val p = content.plans[i]
                    PlanChoice(p) { scope.launch { ReadingPlanRepository.start(context, p.id, today) } }
                }
            } else {
                val day = ReadingPlanRepository.dayOn(state.startedOn, today, plan.days)
                val read = state.readDays(plan.id)
                val oldest = ReadingPlanRepository.oldestUnread(state, plan.id, day)

                item {
                    Text(
                        s.readingDayLabel(day.toString()),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        s.readingDaysRead(read.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(Spacing.md))
                }

                // ── the Church's reading, first ──────────────────────────────
                if (plan.withGitsawe) {
                    item { SectionHeader(s.readingGitsaweHeader) }
                    item {
                        GitsaweSummary(readings, onOpenGitsawe)
                        Spacer(Modifier.height(Spacing.md))
                    }
                }

                // ── then the plan's own ─────────────────────────────────────
                item { SectionHeader(s.readingTodayHeader) }
                val todayDay: PlanDay? = plan.day(day)
                if (todayDay == null) {
                    item { Text(s.readingNoPlan, style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(todayDay.r.size) { i ->
                        val r = todayDay.r[i]
                        ListRow(
                            title = bookName(r.b, s),
                            subtitle = chapterLabel(r.c, r.to),
                            onClick = { onOpenRoute("scripture/${r.b}/${r.c}") },
                        )
                    }
                    item {
                        Spacer(Modifier.height(Spacing.sm))
                        if (day in read) {
                            OutlinedButton(onClick = {
                                scope.launch { ReadingPlanRepository.unmarkDay(context, plan.id, day) }
                            }) { Text(s.readingDone) }
                        } else {
                            Button(onClick = {
                                scope.launch { ReadingPlanRepository.markDay(context, plan.id, day, today) }
                            }) { Text(s.readingMarkDone) }
                        }
                    }
                }

                // Offered, never insisted on: an unread day behind us is a
                // fact, not a failure, and nothing here shows a shortfall.
                if (oldest != null && oldest < day) {
                    item {
                        Spacer(Modifier.height(Spacing.md))
                        NavRow(
                            title = s.readingBehindTitle,
                            onClick = { catching = true },
                            subtitle = s.readingDayLabel(oldest.toString()),
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(Spacing.md))
                    NavRow(title = s.readingAllDays, onClick = onOpenAllDays)
                    NavRow(title = s.readingStop, onClick = { stopping = true })
                    Spacer(Modifier.height(Spacing.huge))
                }
            }
        }
    }

    if (stopping) {
        AlertDialog(
            onDismissRequest = { stopping = false },
            title = { Text(s.readingStop) },
            text = { Text(s.readingStopConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    stopping = false
                    scope.launch { ReadingPlanRepository.stop(context) }
                }) { Text(s.readingStop) }
            },
            dismissButton = { TextButton(onClick = { stopping = false }) { Text(s.cancel) } },
        )
    }

    if (catching && plan != null) {
        val day = ReadingPlanRepository.dayOn(state.startedOn, today, plan.days)
        val oldest = ReadingPlanRepository.oldestUnread(state, plan.id, day) ?: day
        AlertDialog(
            onDismissRequest = { catching = false },
            title = { Text(s.readingBehindTitle) },
            text = {
                Column {
                    // Redistribute is offered first: it is the only one of the
                    // three that never leaves a day behind or shows a deficit.
                    TextButton(onClick = {
                        catching = false
                        scope.launch { ReadingPlanRepository.rebaseTo(context, oldest, today) }
                    }) { Text(s.readingRedistribute) }
                    TextButton(onClick = {
                        catching = false
                        scope.launch { ReadingPlanRepository.rebaseTo(context, oldest, today) }
                    }) { Text(s.readingCatchOldest) }
                    TextButton(onClick = {
                        catching = false
                        scope.launch {
                            ReadingPlanRepository.markDay(context, plan.id, day, today)
                        }
                    }) { Text(s.readingCatchToday) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { catching = false }) { Text(s.cancel) } },
        )
    }
}

@Composable
private fun ChooserIntro(intro: String) {
    Text(
        intro,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.md))
}

@Composable
private fun PlanChoice(plan: ReadingPlan, onStart: () -> Unit) {
    val s = LocalStrings.current
    SinqCard(onClick = onStart) {
        Text(plan.title, style = MaterialTheme.typography.titleMedium)
        if (plan.subtitle.isNotBlank()) {
            Text(
                plan.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        val perDay = if (plan.days > 0) (plan.totalChapters + plan.days - 1) / plan.days else 0
        Text(
            s.readingPlanMeta(plan.days.toString(), perDay.toString()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/**
 * The day's appointed readings, in one line each rather than all nine rows —
 * a typical ግጻዌ day carries a median of nine passages, and a wall of them
 * would bury the plan reading underneath it.
 */
@Composable
private fun GitsaweSummary(readings: DayReadings?, onOpen: () -> Unit) {
    val s = LocalStrings.current
    val entry = readings?.daily
    val title = entry?.title?.takeIf { it.isNotBlank() } ?: s.readingGitsaweHeader
    // Liturgical names, not translated — the same literals GitsaweScreen uses.
    val parts = buildList {
        entry?.kidassie?.let { k ->
            if (k.msbak.isNotEmpty()) add("ምስባክ")
            if (k.wengel.isNotEmpty()) add("ወንጌል")
            if (k.firstDeacon.isNotEmpty() || k.secondDeacon.isNotEmpty()) add("ሐዋርያት")
        }
    }
    NavRow(
        title = title,
        onClick = onOpen,
        subtitle = parts.takeIf { it.isNotEmpty() }?.joinToString(" · "),
    )
}

private fun chapterLabel(from: Int, to: Int): String =
    if (to > from) "$from–$to" else "$from"

/** The plan stores slugs; the reader shows whatever the bundle calls the book. */
private fun bookName(slug: String, s: com.agpeya.app.ui.strings.Strings): String =
    slug.split('-').joinToString(" ") { part ->
        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
