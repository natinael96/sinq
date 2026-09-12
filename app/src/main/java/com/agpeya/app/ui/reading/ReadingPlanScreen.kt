package com.agpeya.app.ui.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.DayReadings
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.data.ReadingPlanRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.ActivePlan
import com.agpeya.app.model.HabitsState
import com.agpeya.app.model.PlanDay
import com.agpeya.app.model.PlanReading
import com.agpeya.app.model.ReadingPlan
import com.agpeya.app.model.ReadingPlanContent
import com.agpeya.app.model.ReadingPlanState
import com.agpeya.app.ui.common.ListRow
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.sinqColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ንባብ — the day's reading, for every plan being kept.
 *
 * The ግጻዌ comes first on this screen, always. What the Church appoints is not
 * the app's to reorder, and the plan is explicitly the supplement: it reads the
 * Old Testament and the deuterocanon, which the lectionary never reaches.
 *
 * The lectionary block is never marked done. It is given, not achieved — making
 * it a checkbox would turn the appointed readings into a task.
 *
 * Two plans can be kept at once — a Bible plan and የዳዊት ንባብ — and each gets a
 * block of its own, named, dated and counted separately. Two plans that read
 * the same corpus at different speeds are refused in the chooser rather than
 * printed twice here.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReadingPlanScreen(
    onBack: () -> Unit,
    onOpenRoute: (String) -> Unit,
    onOpenGitsawe: () -> Unit,
    onOpenAllDays: (String) -> Unit,
    onOpenMap: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val today = remember { LocalDate.now() }

    val content by produceState(ReadingPlanContent()) { value = ReadingPlanRepository.content(context) }
    // The plan stores slugs; every screen that shows one uses the bundle's own name.
    val bookNames by produceState(emptyMap<String, String>()) {
        value = runCatching { com.agpeya.app.data.ScriptureRepository.bookNames(context) }.getOrDefault(emptyMap())
    }
    val state by ReadingPlanRepository.state(context).collectAsState(initial = ReadingPlanState())
    val habits by HabitsRepository.state(context).collectAsState(initial = HabitsState())
    val kept = state.plansKept.mapNotNull { a -> content.plans.firstOrNull { it.id == a.planId }?.let { a to it } }

    val readings by produceState<DayReadings?>(null, today) {
        value = runCatching { GitsaweRepository.readingsFor(context, today) }.getOrNull()
    }

    var stopping by remember { mutableStateOf<ReadingPlan?>(null) }
    var pending by remember { mutableStateOf<ReadingPlan?>(null) }
    var catching by remember { mutableStateOf<Pair<ActivePlan, ReadingPlan>?>(null) }

    pending?.let { choice ->
        StartDialog(
            plan = choice,
            onDismiss = { pending = null },
            onStart = {
                pending = null
                scope.launch {
                    ReadingPlanRepository.start(context, choice.id, today)
                    // The nudge has had nothing to say until now.
                    com.agpeya.app.reminders.ReadingReminderScheduler.sync(
                        context,
                        SettingsRepository.readingReminder(context).first(),
                    )
                }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.readingTitle, onBack = onBack) },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (kept.isEmpty()) {
                item { ChooserIntro(s.readingIntro) }
                items(content.plans.size) { i ->
                    val p = content.plans[i]
                    PlanChoice(plan = p, keeping = false, blockedBy = null) { pending = p }
                }
            } else {
                item {
                    WeekRow(ReadingPlanRepository.daysReadInWeek(habits.records, today))
                    Spacer(Modifier.height(Spacing.sm))
                }
                // ── the Church's reading, first ─────────────────────────────
                if (kept.any { it.second.withGitsawe }) {
                    item { SectionHeader(s.readingGitsaweHeader) }
                    item {
                        GitsaweSummary(readings, onOpenGitsawe)
                        Spacer(Modifier.height(Spacing.md))
                    }
                }

                // ── then each plan's own ────────────────────────────────────
                kept.forEach { (active, plan) ->
                    val days = ReadingPlanRepository.effectiveDays(plan, state)
                    val day = ReadingPlanRepository.dayOn(active.startedOn, today, plan.days)
                    val todayDay = days.firstOrNull { it.d == day }
                    val (read, total) = ReadingPlanRepository.chapterProgress(state, plan.id, days)
                    val complete = ReadingPlanRepository.isComplete(state, plan.id, days)
                    val oldest = ReadingPlanRepository.oldestUnread(state, plan.id, days, day)

                    item(key = "head_${plan.id}") {
                        PlanHeading(
                            plan = plan,
                            day = day,
                            read = read,
                            total = total,
                            only = kept.size == 1,
                        )
                    }
                    if (complete) {
                        item(key = "done_${plan.id}") {
                            CompletePanel(
                                plan = plan,
                                days = plan.days,
                                onRestart = {
                                    scope.launch { ReadingPlanRepository.start(context, plan.id, today) }
                                },
                                onOpenMap = onOpenMap,
                            )
                        }
                    } else if (todayDay == null) {
                        // A repacked plan can run out before its last day. That
                        // is not "you have no plan"; it is a day with nothing
                        // set, and what is owed is still offered below.
                        item(key = "empty_${plan.id}") {
                            Text(
                                s.readingNothingToday,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(todayDay.r.size, key = { "${plan.id}_r_$it" }) { i ->
                            val r = todayDay.r[i]
                            PassageRow(
                                reading = r,
                                names = bookNames,
                                read = ReadingPlanRepository.chaptersOf(PlanDay(d = 0, r = listOf(r)))
                                    .all { it in state.readFor(plan.id) },
                                onToggle = {
                                    scope.launch {
                                        ReadingPlanRepository.toggleReading(
                                            context,
                                            plan.id,
                                            ReadingPlanRepository.chaptersOf(PlanDay(d = 0, r = listOf(r))),
                                            today,
                                        )
                                    }
                                },
                                onOpen = { onOpenRoute(planReadingRoute(r.b, r.c)) },
                            )
                        }
                    }
                    // Offered, never insisted on: an unread day behind us is a
                    // fact, not a failure, and nothing here shows a shortfall.
                    if (!complete && oldest != null && oldest < day) {
                        item(key = "behind_${plan.id}") {
                            NavRow(
                                title = s.readingBehindTitle,
                                onClick = { catching = active to plan },
                                subtitle = s.readingDayLabel(geezNumeral(oldest)),
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(Spacing.md))
                    MapStrip(state, onOpenMap)
                }
                item {
                    Spacer(Modifier.height(Spacing.md))
                    ContinueStrip(
                        plans = kept,
                        canAdd = content.plans.any { p ->
                            kept.none { it.second.id == p.id } &&
                                ReadingPlanRepository.conflict(content, state, p.id) == null
                        },
                        onOpenMap = onOpenMap,
                        onOpenAllDays = onOpenAllDays,
                        onAdd = { onOpenRoute(READING_CHOOSE_ROUTE) },
                        onStop = { stopping = it },
                    )
                    Spacer(Modifier.height(Spacing.huge))
                }
            }
        }
    }

    stopping?.let { plan ->
        AlertDialog(
            onDismissRequest = { stopping = null },
            title = { Text("${s.readingStop} · ${plan.title}") },
            text = { Text(s.readingStopConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    stopping = null
                    scope.launch { ReadingPlanRepository.stop(context, plan.id) }
                }) { Text(s.readingStop) }
            },
            dismissButton = { TextButton(onClick = { stopping = null }) { Text(s.cancel) } },
        )
    }

    catching?.let { (active, plan) ->
        val days = ReadingPlanRepository.effectiveDays(plan, state)
        val day = ReadingPlanRepository.dayOn(active.startedOn, today, plan.days)
        val oldest = ReadingPlanRepository.oldestUnread(state, plan.id, days, day) ?: day
        AlertDialog(
            onDismissRequest = { catching = null },
            title = { Text(s.readingBehindTitle) },
            text = {
                // Two answers, and they are the only two: hold the daily reading
                // and let the finish date move, or hold the finish date and let
                // the days grow. Each says which it is costing.
                Column {
                    Text(
                        s.readingBehindBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    ListRow(
                        title = s.readingCatchOldest,
                        subtitle = s.readingCatchOldestDesc,
                        onClick = {
                            catching = null
                            scope.launch { ReadingPlanRepository.rebaseTo(context, plan.id, oldest, today) }
                        },
                    )
                    ListRow(
                        title = s.readingRedistribute,
                        subtitle = s.readingRedistributeDesc,
                        onClick = {
                            catching = null
                            scope.launch {
                                ReadingPlanRepository.redistributeFrom(context, plan.id, oldest, day)
                            }
                        },
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { catching = null }) { Text(s.cancel) } },
        )
    }
}

/** Where the chooser lives, so the ቀጥል strip can send someone to it. */
internal const val READING_CHOOSE_ROUTE = "reading/choose"

@Composable
private fun ChooserIntro(intro: String) {
    Text(
        intro,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.md))
}

/**
 * The plan's own heading: its name, the day it is on, and how much of it has
 * been read — which the screen never used to say at all.
 */
@Composable
private fun PlanHeading(plan: ReadingPlan, day: Int, read: Int, total: Int, only: Boolean) {
    val s = LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(top = if (only) 0.dp else Spacing.md)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                s.readingDayLabel(geezNumeral(day)),
                style = if (only) MaterialTheme.typography.headlineSmall
                else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                plan.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Rail(if (total > 0) read.toFloat() / total else 0f)
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            s.readingChaptersOf(geezNumeral(read), geezNumeral(total)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
    }
}

/** A hairline of progress. Gold on the page's own ground, never a percentage. */
@Composable
private fun Rail(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
    }
}

/**
 * One passage of the day, with a box of its own.
 *
 * The day was always recorded chapter by chapter; only the marking was
 * all-or-nothing, so a day of three passages could not be half kept. The box
 * keeps the passage, the rest of the row opens it.
 */
@Composable
private fun PassageRow(
    reading: PlanReading,
    names: Map<String, String>,
    read: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val s = LocalStrings.current
    val gold = MaterialTheme.colorScheme.secondary
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (read) gold else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (read) gold else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable(onClick = onToggle)
                .semantics {
                    role = Role.Checkbox
                    contentDescription = if (read) s.readingDone else s.readingMarkDone
                },
            contentAlignment = Alignment.Center,
        ) {
            if (read) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = sinqColors.hero,
                    modifier = Modifier.size(IconSize.small),
                )
            }
        }
        Spacer(Modifier.width(Spacing.md))
        Column(
            Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onOpen)
                .padding(vertical = Spacing.xs),
        ) {
            Text(
                bookName(reading.b, names),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${s.chapterUnit} ${chapterLabel(reading.c, reading.to)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The last seven days, as ጉዞ draws them: a record, never a streak to lose. */
@Composable
private fun WeekRow(days: Int) {
    val s = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(7) { i ->
            Box(
                Modifier
                    .size(15.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (i < days) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
            )
            if (i < 6) Spacer(Modifier.width(Spacing.xs))
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            s.readingWeekDays(geezNumeral(days)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The whole canon in one line, as a door to the map. */
@Composable
private fun MapStrip(state: ReadingPlanState, onOpenMap: () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val books by produceState(emptyList<ReadingMapBook>(), state.readChapters) {
        value = readingMapBooks(context, state.readChapters)
    }
    if (books.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onOpenMap)
            .semantics { role = Role.Button }
            .padding(vertical = Spacing.xs),
    ) {
        Text(
            s.readingMapTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(Spacing.xs))
        BookStrip(books)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            s.readingBooksDone(geezNumeral(books.count { it.read >= it.chapters })),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Where to go on from here. Chips, as elsewhere, rather than a stack of rows. */
@Composable
private fun ContinueStrip(
    plans: List<Pair<ActivePlan, ReadingPlan>>,
    canAdd: Boolean,
    onOpenMap: () -> Unit,
    onOpenAllDays: (String) -> Unit,
    onAdd: () -> Unit,
    onStop: (ReadingPlan) -> Unit,
) {
    val s = LocalStrings.current
    Column(Modifier.fillMaxWidth()) {
        Text(
            s.continueReading,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(Spacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            ReadingChip(s.readingMap, onClick = onOpenMap)
            plans.forEach { (_, plan) ->
                ReadingChip(
                    if (plans.size > 1) "${s.readingAllDays} · ${plan.title}" else s.readingAllDays,
                    onClick = { onOpenAllDays(plan.id) },
                )
            }
            if (canAdd) ReadingChip(s.readingAdd, onClick = onAdd)
            plans.forEach { (_, plan) ->
                ReadingChip(
                    if (plans.size > 1) "${s.readingStop} · ${plan.title}" else s.readingStop,
                    quiet = true,
                    onClick = { onStop(plan) },
                )
            }
        }
    }
}

/** The same chip the ግጻዌ passage page uses: gold for in, outline for quiet. */
@Composable
internal fun ReadingChip(label: String, quiet: Boolean = false, onClick: () -> Unit) {
    val gold = MaterialTheme.colorScheme.secondary
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (quiet) Color.Transparent else gold.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = if (quiet) MaterialTheme.colorScheme.outlineVariant else gold.copy(alpha = 0.42f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .heightIn(min = 48.dp)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (quiet) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
    }
}

/**
 * The end of a plan.
 *
 * Reading the whole Bible is the largest thing the app asks of anyone, and
 * until now it simply stopped answering on the last day. What was read is kept
 * whatever happens next, which is why beginning again costs nothing.
 */
@Composable
private fun CompletePanel(
    plan: ReadingPlan,
    days: Int,
    onRestart: () -> Unit,
    onOpenMap: () -> Unit,
) {
    val s = LocalStrings.current
    val sinq = sinqColors
    SinqCard(contentPadding = PaddingValues(Spacing.lg)) {
        Text(
            s.readingCompleteTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            s.readingCompleteBody(geezNumeral(days)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ReadingChip(s.readingMap, onClick = onOpenMap)
            ReadingChip(s.readingRestart, onClick = onRestart)
        }
    }
}

/** Starting a plan: what it asks of a day, before it is asked for. */
@Composable
private fun StartDialog(plan: ReadingPlan, onDismiss: () -> Unit, onStart: () -> Unit) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(plan.title) },
        text = {
            Column {
                if (plan.subtitle.isNotBlank()) {
                    Text(plan.subtitle, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(Spacing.sm))
                }
                Text(
                    s.readingPerDay(geezNumeral(planMinutes(plan))),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    s.readingCost(geezNumeral(planVersesADay(plan)), geezNumeral(plan.days)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    s.readingStartBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onStart) { Text(s.readingStart) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

/**
 * One plan, offered.
 *
 * The cost is given in the unit that is true. The plans are packed by verses,
 * not chapters, so "five chapters a day" was an average of a figure that runs
 * from one to sixteen — five chapters of Psalms is not five of Isaiah.
 */
@Composable
internal fun PlanChoice(
    plan: ReadingPlan,
    keeping: Boolean,
    blockedBy: String?,
    onStart: () -> Unit,
) {
    val s = LocalStrings.current
    SinqCard(onClick = onStart.takeIf { !keeping && blockedBy == null }, enabled = blockedBy == null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                plan.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (keeping) {
                Text(
                    s.readingKeeping,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        if (plan.subtitle.isNotBlank()) {
            Text(
                plan.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            s.readingPerDay(geezNumeral(planMinutes(plan))),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            s.readingCost(geezNumeral(planVersesADay(plan)), geezNumeral(plan.days)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        blockedBy?.let {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                s.readingConflict(it),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
    // Liturgical names, not translated — the same literals GitsaweScreen uses.
    val parts = buildList {
        entry?.kidassie?.let { k ->
            if (k.msbak.isNotEmpty()) add("ምስባክ")
            if (k.wengel.isNotEmpty()) add("ወንጌል")
            if (k.firstDeacon.isNotEmpty() || k.secondDeacon.isNotEmpty()) add("ሐዋርያት")
        }
    }
    // The header above already says የዕለቱ ግጻዌ; a row that falls back to the same
    // words says it twice, which is what it did while the lectionary loaded.
    NavRow(
        title = entry?.title?.takeIf { it.isNotBlank() } ?: s.loadingLabel,
        onClick = onOpen,
        subtitle = parts.takeIf { it.isNotEmpty() }?.joinToString(" · "),
    )
}

private fun chapterLabel(from: Int, to: Int): String =
    if (to > from) "${geezNumeral(from)}–${geezNumeral(to)}" else geezNumeral(from)

/** The Psalter's slug in the plans; it is not in the Bible reader's catalogue. */
internal const val PSALMS_SLUG = "psalms"

/**
 * The plan stores slugs. The bundle names every book in Amharic, so a slug is
 * only ever a fallback for a book that failed to load — never "2 Kings" on an
 * Amharic page.
 */
internal fun bookName(slug: String, names: Map<String, String>): String =
    names[slug] ?: slug.split('-').joinToString(" ") { part ->
        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

/**
 * Where a day's reading opens.
 *
 * The Psalter has its own reader and its own two editions, and
 * [com.agpeya.app.data.ScriptureRepository.books] leaves it out of the Bible
 * catalogue for exactly that reason — so a ዳዊት day routed at the Bible reader
 * would land on a book it cannot find.
 */
internal fun planReadingRoute(slug: String, chapter: Int): String =
    if (slug == PSALMS_SLUG) "psalter?section=${chapter - 1}"
    else "scripture/$slug/$chapter"

/**
 * What a day of the plan asks for, in the two units that are true.
 *
 * Verses come from the bundle, measured when the plans are built. The minutes
 * are arithmetic on them, not a promise: about eighteen words to a verse and
 * two hundred words a minute, which puts the year at eleven minutes and the
 * six months at twenty-two.
 */
internal fun planVersesADay(plan: ReadingPlan): Int = plan.versesADay

internal fun planMinutes(plan: ReadingPlan): Int =
    if (plan.versesADay <= 0) 0 else maxOf(1, Math.round(plan.versesADay * 18f / 200f))
