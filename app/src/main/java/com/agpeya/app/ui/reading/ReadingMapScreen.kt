package com.agpeya.app.ui.reading

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ReadingPlanRepository
import com.agpeya.app.data.ScriptureRepository
import com.agpeya.app.model.ReadingPlanState
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.library.canonSectionKey
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.sinqColors

/** One book on the map: what it is, how long, and how much of it has been read. */
internal data class ReadingMapBook(
    val slug: String,
    val name: String,
    val chapters: Int,
    val sectionKey: String,
    val read: Int,
)

/**
 * Every book the app carries, with what has been read of each.
 *
 * Drawn from [ReadingPlanState.readChapters] — the record of what has been
 * read by any plan — rather than from one plan's count, because a map of the
 * books is about the books and not about a schedule.
 */
internal suspend fun readingMapBooks(context: Context, read: Set<String>): List<ReadingMapBook> {
    val books = runCatching { ScriptureRepository.books(context) }.getOrDefault(emptyList())
    if (books.isEmpty()) return emptyList()
    val all = books.map { b ->
        ReadingMapBook(
            slug = b.key,
            name = b.nameAm,
            chapters = b.chapters,
            sectionKey = canonSectionKey(b),
            read = (1..b.chapters).count {
                ReadingPlanRepository.chapterKey(b.key, it) in read
            },
        )
    }
    // The Psalter is kept out of the Bible catalogue — it has its own reader and
    // its own two editions — but it is a book of the canon and belongs here.
    val psalms = ReadingMapBook(
        slug = PSALMS_SLUG,
        name = "መዝሙረ ዳዊት",
        chapters = PSALTER_CHAPTERS,
        sectionKey = "PoetryWisdom",
        read = (1..PSALTER_CHAPTERS).count {
            ReadingPlanRepository.chapterKey(PSALMS_SLUG, it) in read
        },
    )
    return (all + psalms).sortedBy { SECTION_ORDER.indexOf(it.sectionKey) }
}

private const val PSALTER_CHAPTERS = 150

/** The Church's own order of the groups, which is the order they are drawn in. */
private val SECTION_ORDER = listOf(
    "Pentateuch", "Historical", "PoetryWisdom", "MajorProphet", "MinorProphet",
    "Gospel", "Acts", "PaulineEpistle", "GeneralEpistle", "Revelation", "ChurchOrder",
)

/**
 * የመጻሕፍቱ ካርታ — the canon, filling as it is read.
 *
 * A percentage says how much; this says where. The books are grouped as the
 * Church groups them, each one a bar that fills with its chapters, so a year of
 * reading is a shape rather than a number — and the gaps are as legible as the
 * gold. Tapping a book opens its chapters.
 *
 * Nothing here is new data. The plan has always recorded chapters; this is the
 * first screen to draw them.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReadingMapScreen(onBack: () -> Unit, onOpenRoute: (String) -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val state by ReadingPlanRepository.state(context).collectAsState(initial = ReadingPlanState())
    val books by produceState(emptyList<ReadingMapBook>(), state.readChapters) {
        value = readingMapBooks(context, state.readChapters)
    }
    var open by remember { mutableStateOf<ReadingMapBook?>(null) }

    val read = books.sumOf { it.read }
    val total = books.sumOf { it.chapters }
    val done = books.count { it.read >= it.chapters && it.chapters > 0 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.readingMapTitle,
                accentLine = if (total > 0) {
                    "${s.readingChaptersOf(geezNumeral(read), geezNumeral(total))} · " +
                        s.readingBooksDone(geezNumeral(done))
                } else null,
                onBack = onBack,
            )
        },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            milestoneOf(books)?.let { milestone ->
                item(key = "milestone") {
                    Milestone(milestone, s)
                    Spacer(Modifier.height(Spacing.md))
                }
            }
            val sections = books.groupBy { it.sectionKey }
            SECTION_ORDER.forEach { key ->
                val group = sections[key].orEmpty()
                if (group.isEmpty()) return@forEach
                item(key = "sec_$key") {
                    SectionRule(
                        label = s.canonSection(key),
                        done = group.count { it.read >= it.chapters },
                        total = group.size,
                    )
                }
                item(key = "books_$key") {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        group.forEach { book -> BookCell(book) { open = book } }
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }
            }
            item { Spacer(Modifier.height(Spacing.huge)) }
        }
    }

    open?.let { book ->
        ModalBottomSheet(onDismissRequest = { open = null }) {
            BookSheet(book) { chapter ->
                open = null
                onOpenRoute(planReadingRoute(book.slug, chapter))
            }
        }
    }
}

/**
 * The one line worth putting at the top: the book in hand, or the group
 * closest to finishing. Both are read off the chapters already recorded.
 */
private fun milestoneOf(books: List<ReadingMapBook>): Pair<ReadingMapBook?, Pair<String, Int>?>? {
    if (books.isEmpty()) return null
    books.firstOrNull { it.read in 1 until it.chapters }?.let { return it to null }
    val section = books.groupBy { it.sectionKey }
        .filterValues { g -> g.any { it.read > 0 } && g.any { it.read < it.chapters } }
        .minByOrNull { (_, g) -> g.sumOf { it.chapters - it.read } }
        ?: return null
    return null to (section.key to section.value.sumOf { it.chapters - it.read })
}

@Composable
private fun Milestone(
    milestone: Pair<ReadingMapBook?, Pair<String, Int>?>,
    s: com.agpeya.app.ui.strings.Strings,
) {
    val (book, section) = milestone
    val text = when {
        book != null -> s.readingInBook(book.name, geezNumeral(book.chapters - book.read))
        section != null -> s.readingSectionLeft(s.canonSection(section.first), geezNumeral(section.second))
        else -> return
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SectionRule(label: String, done: Int, total: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = Spacing.md, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.width(Spacing.sm))
        HorizontalDivider(
            Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            "${geezNumeral(done)}/${geezNumeral(total)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One book: a bar that fills with its chapters, named across the fill. */
@Composable
private fun BookCell(book: ReadingMapBook, onClick: () -> Unit) {
    val gold = MaterialTheme.colorScheme.secondary
    val fraction = if (book.chapters > 0) book.read.toFloat() / book.chapters else 0f
    val whole = fraction >= 1f
    Box(
        Modifier
            .fillMaxWidth(0.5f)
            .padding(end = Spacing.xxs)
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(34.dp)
                .background(if (whole) gold else gold.copy(alpha = 0.42f)),
        )
        Text(
            book.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (whole) sinqColors.hero else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = Spacing.sm),
        )
    }
}

/** The whole canon as one line, for the pages that only have room for a line. */
@Composable
internal fun BookStrip(books: List<ReadingMapBook>) {
    val gold = MaterialTheme.colorScheme.secondary
    Row(
        Modifier.fillMaxWidth().height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        books.forEach { book ->
            val fraction = if (book.chapters > 0) book.read.toFloat() / book.chapters else 0f
            Box(
                Modifier
                    .weight(1f)
                    .height((6 + 16 * fraction.coerceIn(0f, 1f)).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            fraction >= 1f -> gold
                            fraction > 0f -> gold.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            )
        }
    }
}

/** A book's chapters, read and unread, with a way into the next one. */
@Composable
private fun BookSheet(book: ReadingMapBook, onOpen: (Int) -> Unit) {
    val s = LocalStrings.current
    val gold = MaterialTheme.colorScheme.secondary
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen)
            .padding(bottom = Spacing.xxl),
    ) {
        Text(
            book.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            s.readingBookChapters(geezNumeral(book.read), geezNumeral(book.chapters)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(Spacing.md))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            // Read from the front: the plans move through a book in order, so
            // the count is enough to say which chapters they were.
            (1..book.chapters).forEach { n ->
                val read = n <= book.read
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (read) gold else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onOpen(n) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        geezNumeral(n),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (read) sinqColors.hero else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
        com.agpeya.app.ui.common.DoorChip(
            icon = Icons.AutoMirrored.Outlined.MenuBook,
            label = s.readingOpenChapter(geezNumeral(minOf(book.read + 1, book.chapters))),
            onClick = { onOpen(minOf(book.read + 1, book.chapters)) },
        )
    }
}

/**
 * ንባብ's door in ቤተ መጻሕፍት, which is the only door it has.
 *
 * Every other card on that page is a shelf: it says what it holds and holds it
 * whether or not you come. This one is a commitment being kept, so it reports
 * itself — today's passage by name, how far through the books you are, and the
 * week behind you. Before anything is started it is an invitation, and it says
 * what a day of it actually costs.
 *
 * It is the one card cut from the hero's green, which is the whole of being
 * noticed on a page of seven.
 */
@Composable
fun ReadingHeroCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val sinq = sinqColors
    val today = remember { java.time.LocalDate.now() }
    val content by produceState(com.agpeya.app.model.ReadingPlanContent()) {
        value = ReadingPlanRepository.content(context)
    }
    val state by ReadingPlanRepository.state(context).collectAsState(initial = ReadingPlanState())
    val habits by com.agpeya.app.data.HabitsRepository.state(context)
        .collectAsState(initial = com.agpeya.app.model.HabitsState())
    val names by produceState(emptyMap<String, String>()) {
        value = runCatching { ScriptureRepository.bookNames(context) }.getOrDefault(emptyMap())
    }
    val books by produceState(emptyList<ReadingMapBook>(), state.readChapters) {
        value = readingMapBooks(context, state.readChapters)
    }
    val kept = state.plansKept.mapNotNull { a ->
        content.plans.firstOrNull { it.id == a.planId }?.let { a to it }
    }

    com.agpeya.app.ui.common.HeroCard(
        onClick = onOpen,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.lg),
    ) {
        Column(Modifier.weight(1f)) {
            if (kept.isEmpty()) {
                val offer = content.plans.firstOrNull()
                Text(s.readingTitle, style = MaterialTheme.typography.labelMedium, color = sinq.onHeroMuted)
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    s.readingIntro,
                    style = MaterialTheme.typography.titleMedium,
                    color = sinq.onHero,
                )
                offer?.let {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        s.readingPerDay(geezNumeral(planMinutes(it))),
                        style = MaterialTheme.typography.labelMedium,
                        color = sinq.onHeroGold,
                    )
                }
                if (books.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.sm))
                    BookStrip(books)
                }
                return@Column
            }
            kept.forEachIndexed { index, (active, plan) ->
                val days = ReadingPlanRepository.effectiveDays(plan, state)
                val day = ReadingPlanRepository.dayOn(active.startedOn, today, plan.days)
                val (read, total) = ReadingPlanRepository.chapterProgress(state, plan.id, days)
                val complete = ReadingPlanRepository.isComplete(state, plan.id, days)
                val line = days.firstOrNull { it.d == day }?.r.orEmpty().joinToString(" · ") { r ->
                    val name = bookName(r.b, names)
                    if (r.to > r.c) "$name ${geezNumeral(r.c)}–${geezNumeral(r.to)}"
                    else "$name ${geezNumeral(r.c)}"
                }
                if (index > 0) {
                    Spacer(Modifier.height(Spacing.sm))
                    HorizontalDivider(thickness = 1.dp, color = sinq.onHeroMuted.copy(alpha = 0.25f))
                    Spacer(Modifier.height(Spacing.sm))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (kept.size > 1) plan.title else "${s.readingTitle} · ${plan.title}",
                        style = MaterialTheme.typography.labelMedium,
                        color = sinq.onHeroMuted,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (index == 0) {
                        Text(
                            s.readingWeekDays(
                                geezNumeral(ReadingPlanRepository.daysReadInWeek(habits.records, today)),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = sinq.onHeroGold,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    if (complete) s.readingCompleteTitle
                    else line.ifBlank { s.readingNothingToday },
                    style = if (index == 0) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.bodyMedium,
                    color = sinq.onHero,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.sm))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(sinq.onHeroMuted.copy(alpha = 0.25f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(if (total > 0) read.toFloat() / total else 0f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(sinq.onHeroGold),
                    )
                }
                Spacer(Modifier.height(Spacing.xxs))
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "${s.readingDayLabel(geezNumeral(day))} / ${geezNumeral(plan.days)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = sinq.onHeroMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        s.readingChaptersOf(geezNumeral(read), geezNumeral(total)),
                        style = MaterialTheme.typography.labelSmall,
                        color = sinq.onHeroMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
