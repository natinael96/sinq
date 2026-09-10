package com.agpeya.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.ScriptureRepository
import com.agpeya.app.data.HighlightRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.model.Bookmark
import com.agpeya.app.model.ScriptureBook
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.scaledReadingSp
import com.agpeya.app.ui.theme.LocalReadingFont
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.SelectPill
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.reading.ReadingColumn
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.readingBodyStyle
import com.agpeya.app.ui.theme.readingVerseGap
import com.agpeya.app.ui.theme.sinqColors
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import com.agpeya.app.ui.theme.inReadingFont
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import com.agpeya.app.ui.common.ListRow

private val FONT_STEPS_SP = com.agpeya.app.data.SettingsRepository.FONT_STEPS_SP

/**
 * A New-Testament book reader. Opens at [initialChapter] (and scrolls to
 * [initialStart], tinting [initialStart]..[initialEnd]) when arrived at from a
 * ግጻዌ reading link; otherwise chapter 1. A chapter strip switches chapters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptureReaderScreen(
    bookKey: String,
    initialChapter: Int = 1,
    initialStart: Int = -1,
    initialEnd: Int = -1,
    onBack: () -> Unit,
    /** Opens a journal entry anchored to the chapter, or to the selected run of verses. */
    onWriteNote: (route: String, label: String) -> Unit,
    /** Follows today's ንባብ into the next book when the day crosses one. */
    onOpenRoute: (route: String) -> Unit = {},
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var loadAttempt by rememberSaveable(bookKey) { mutableIntStateOf(0) }
    val bookResult by produceState<Result<ScriptureBook?>?>(initialValue = null, bookKey, loadAttempt) {
        value = runCatching { ScriptureRepository.book(context, bookKey) }
    }
    val fontStep by SettingsRepository.fontStep(context).collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]

    val highlights by HighlightRepository.highlights(context).collectAsState(initial = emptyMap())

    val b = bookResult?.getOrNull() ?: run {
        // Keep a back arrow visible: if the book never loads (stale bookmark,
        // bad key), the spinner would otherwise trap the user on this screen.
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { SinqTopBar(title = "", onBack = onBack) },
        ) { padding ->
            if (bookResult == null) LoadingPanel(Modifier.padding(padding))
            else StatePanel(
                title = s.contentUnavailable,
                body = s.contentMissingBody,
                actionLabel = s.retryAction,
                onAction = { loadAttempt++ },
                modifier = Modifier.padding(padding),
            )
        }
        return
    }

    // A book with no chapters at all (corrupt or truncated asset) would throw
    // in the coerceIn/first() below; show the same unavailable state instead.
    if (b.chapters.isEmpty()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { SinqTopBar(title = b.nameAm, onBack = onBack) },
        ) { padding ->
            StatePanel(
                title = s.contentUnavailable,
                body = s.contentMissingBody,
                modifier = Modifier.padding(padding),
            )
        }
        return
    }

    // Clamp into the book's real range: a few bundled ግጻዌ citations name chapters
    // that don't exist (e.g. Mark 17). Landing on the nearest real chapter — with
    // the title, chapter strip, and bookmark all agreeing — beats silently showing
    // chapter 1 under the cited number. The highlight only fires when the clamped
    // chapter still equals the cited one, so a bad citation never tints wrong verses.
    // Today's ንባብ, so a chapter that belongs to it can say so and be marked
    // from where it is read. Opening a passage from the plan and then having to
    // go back to the plan to tick it was the plan's own ledger asking twice.
    val planContent by androidx.compose.runtime.produceState(com.agpeya.app.model.ReadingPlanContent()) {
        value = com.agpeya.app.data.ReadingPlanRepository.content(context)
    }
    val planState by com.agpeya.app.data.ReadingPlanRepository.state(context)
        .collectAsState(initial = com.agpeya.app.model.ReadingPlanState())
    val today = com.agpeya.app.ui.common.rememberCurrentDate().value
    val planDay = remember(planContent, planState, today) {
        planContent.plans.firstOrNull { it.id == planState.activePlanId }?.let { plan ->
            val n = com.agpeya.app.data.ReadingPlanRepository.dayOn(planState.startedOn, today, plan.days)
            plan to com.agpeya.app.data.ReadingPlanRepository.effectiveDays(plan, planState)
                .firstOrNull { it.d == n }
        }
    }
    val lastChapters by SettingsRepository.lastChapters(context).collectAsState(initial = emptyMap())
    var chaptersOpen by remember { mutableStateOf(false) }
    var chapter by rememberSaveable(bookKey) {
        mutableIntStateOf(initialChapter.coerceIn(1, b.chapters.size))
    }
    // Opening a book with no chapter named carries on where it was left. A link
    // that names one — a ግጻዌ citation, a bookmark — always wins over the memory.
    var restoredChapter by rememberSaveable(bookKey) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(bookKey, lastChapters) {
        if (restoredChapter || initialChapter > 1) {
            restoredChapter = true
            return@LaunchedEffect
        }
        lastChapters[bookKey]?.takeIf { it in 1..b.chapters.size }?.let { chapter = it }
        restoredChapter = true
    }
    androidx.compose.runtime.LaunchedEffect(bookKey, chapter, restoredChapter) {
        if (restoredChapter) SettingsRepository.setLastChapter(context, bookKey, chapter)
    }
    // Live verse selection (tap anchors, next tap moves the end); -1 = none.
    var selA by rememberSaveable(bookKey, chapter) { mutableIntStateOf(-1) }
    var selB by rememberSaveable(bookKey, chapter) { mutableIntStateOf(-1) }
    val current = remember(b, chapter) { b.chapters.find { it.chapter == chapter } ?: b.chapters.first() }
    val highlightSectionId = "scripture:${ScriptureRepository.BIBLE_EDITION}:$bookKey:$chapter"
    // Only the chapter we arrived on shows the cited-verse tint. The cited range
    // is snapped onto verse numbers that actually exist: the Amharic source merges
    // some verses (so e.g. v17 may not appear) and a few citations run past the
    // chapter's end — both would otherwise scroll somewhere yet highlight nothing.
    val highlightRange = remember(current, chapter) {
        val verses = current.verses
        if (chapter != initialChapter || initialStart <= 0 || verses.isEmpty()) IntRange.EMPTY
        else {
            val lastN = verses.last().n
            val startN = verses.firstOrNull { it.n >= initialStart }?.n ?: lastN
            val endN = (if (initialEnd > 0) initialEnd else initialStart).coerceIn(startN, lastN)
            startN..endN
        }
    }
    // Verses grouped into rows: the cited run becomes one row, everything else
    // stays a row of its own, so the highlight can be drawn around the group.
    val rows = remember(current, highlightRange) {
        buildList {
            val vs = current.verses
            var i = 0
            while (i < vs.size) {
                if (vs[i].n in highlightRange) {
                    var j = i
                    while (j + 1 < vs.size && vs[j + 1].n in highlightRange) j++
                    add(vs.subList(i, j + 1))
                    i = j + 1
                } else {
                    add(listOf(vs[i]))
                    i++
                }
            }
        }
    }
    val hasFathers by androidx.compose.runtime.produceState(false, bookKey) {
        value = runCatching {
            com.agpeya.app.data.FathersRepository.covers(context, bookKey)
        }.getOrDefault(false)
    }
    val bookNames by androidx.compose.runtime.produceState(emptyMap<String, String>()) {
        value = runCatching { ScriptureRepository.bookNames(context) }.getOrDefault(emptyMap())
    }
    val listState = rememberLazyListState()
    val verseGap = readingVerseGap(bodyFontSp)
    val selRange = com.agpeya.app.ui.reading.flatSelectionRange(selA, selB)
    val sinq = sinqColors
    val chapterTitle = "${b.nameAm} ${s.chapterUnit} ${geezNumeral(chapter)}"
    // The selection as the Church names it: the book's Amharic name, the
    // chapter and the verses, in Ge'ez numerals — the same string the copy, the
    // share, the image card and the bookmark all carry.
    val selPassage = if (selRange.isEmpty()) null else {
        val verses = current.verses.filter { it.n in selRange }
        if (verses.isEmpty()) null else com.agpeya.app.ui.common.Passage(
            verses = verses.map { it.n as Int? to it.text },
            citation = com.agpeya.app.data.Citation.of(b.nameAm, chapter, selRange.first, selRange.last),
            edition = s.amharicEdition,
        )
    }
    val selRoute = "scripture/$bookKey/$chapter" +
        if (selRange.isEmpty()) "" else "?start=${selRange.first}&end=${selRange.last}"

    // Land on the cited verse when opened from a reading link — once. Without the
    // guard, paging away and back to this chapter re-ran the jump mid-reading.
    var landed by rememberSaveable(bookKey) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(current, highlightRange, rows) {
        if (!landed && !highlightRange.isEmpty()) {
            // Index into `rows`, not verses — the cited run collapses into one row.
            val idx = rows.indexOfFirst { row -> row.any { it.n >= highlightRange.first } }
            if (idx >= 0) listState.scrollToItem(idx + 1)   // +1 for the chapter-strip header item
            landed = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = b.nameAm,
                onBack = onBack,
                titleContent = {
                    com.agpeya.app.ui.common.ReaderTitleBar(
                        title = b.nameAm,
                        chapterLabel = "${s.chapterUnit} ${geezNumeral(chapter)}",
                        pickable = b.chapters.size > 1,
                        onPick = { chaptersOpen = true },
                    )
                },
                // The top bar has no bookmark of its own any more. It could
                // only ever mark a whole chapter, which is not the grain anyone
                // reads at; the selection bar marks what was actually chosen,
                // and it is the same control in every reader. Chapter bookmarks
                // already saved keep working and keep opening the chapter.
                actions = {
                    com.agpeya.app.ui.common.ReaderToolsMenu(
                        fontStep = fontStep,
                        maxFontStep = FONT_STEPS_SP.lastIndex,
                        onFontChange = { step -> scope.launch { SettingsRepository.setFontStep(context, step) } },
                        onWriteNote = {
                            // A selected run names the verses; otherwise the chapter.
                            if (selRange.isEmpty()) {
                                onWriteNote("scripture/$bookKey/$chapter", chapterTitle)
                            } else {
                                onWriteNote(
                                    "scripture/$bookKey/$chapter?start=${selRange.first}&end=${selRange.last}",
                                    "$chapterTitle፥${geezNumeral(selRange.first)}" +
                                        (if (selRange.last != selRange.first) "–${geezNumeral(selRange.last)}" else ""),
                                )
                            }
                        },
                        sharePayload = {
                            com.agpeya.app.ui.common.SharePayload(
                                body = current.verses.joinToString("\n") { "${geezNumeral(it.n)}  ${it.text}" },
                                kicker = s.scripturesTitle,
                                title = "${b.nameAm} ${s.chapterUnit} ${geezNumeral(chapter)}",
                            )
                        },
                    )
                },
            )
        },
        bottomBar = {
            com.agpeya.app.ui.reading.SelectionBar(
                visible = selA >= 0,
                passage = selPassage,
                currentColor = selA.takeIf { it >= 0 }
                    ?.let { highlights[HighlightRepository.verseKey(highlightSectionId, it)] },
                onPick = { colorKey ->
                    val keys = current.verses
                        .filter { it.n in selRange }
                        .map { HighlightRepository.verseKey(highlightSectionId, it.n) }
                    selA = -1
                    selB = -1
                    if (keys.isNotEmpty()) scope.launch {
                        HighlightRepository.setHighlights(context, keys, colorKey)
                    }
                },
                onDismiss = { selA = -1; selB = -1 },
                imageKicker = s.scripturesTitle,
                crossRefs = remember(current, selRange, bookNames) {
                    if (selRange.isEmpty()) emptyList()
                    else current.verses.filter { it.n in selRange }
                        .flatMap { com.agpeya.app.data.CrossReference.parse(it.refs.orEmpty(), bookNames) }
                        .distinctBy { it.route }
                },
                onOpenRef = { route -> selA = -1; selB = -1; onOpenRoute(route) },
                // The Fathers on the first verse of the selection. Null for the
                // seventeen books Catena's canon does not reach, which hides
                // the action rather than offering a page that is not there.
                commentaryUrl = selRange.firstOrNull()?.let {
                    com.agpeya.app.data.CatenaLink.url(bookKey, chapter, it)
                },
                // The bundled Fathers win over the link when the book is one of
                // the twenty they reach, so the reader never has two doors to
                // the same thing.
                onOpenFathers = selRange.firstOrNull()?.takeIf { hasFathers }?.let { verse ->
                    {
                        selA = -1; selB = -1
                        onOpenRoute(
                            "fathers/$bookKey/$chapter/$verse?name=" +
                                android.net.Uri.encode(b.nameAm),
                        )
                    }
                },
                // The bar is the only bookmark control now, so a bookmark is
                // made at the grain the reader chose rather than always at the
                // chapter — the route has carried a verse range all along.
                onBookmark = selPassage?.let { passage ->
                    {
                        scope.launch {
                            UserDataRepository.toggleBookmark(
                                context,
                                com.agpeya.app.model.Bookmark(
                                    hourId = "scripture_library",
                                    hourName = s.scripturesTitle,
                                    sectionId = "$bookKey:$chapter:${selRange.first}-${selRange.last}",
                                    sectionIndex = chapter,
                                    title = passage.citation ?: chapterTitle,
                                    subtitle = passage.verses.firstOrNull()?.second,
                                    route = selRoute,
                                ),
                            )
                        }
                        selA = -1; selB = -1
                    }
                },
                onWriteNote = selPassage?.let { passage ->
                    {
                        onWriteNote(selRoute, passage.citation ?: chapterTitle)
                        selA = -1; selB = -1
                    }
                },
            )
        },
    ) { innerPadding ->
        ReadingColumn(state = listState, innerPadding = innerPadding) {
            // The cited verses are emitted as ONE row so the citation reads as a
            // single tinted block instead of a stack of separate boxes.
            items(rows, key = { it.first().n }) { row ->
                val tinted = row.first().n in highlightRange
                val body = @Composable { verse: com.agpeya.app.model.ScriptureVerse ->
                    val annotated = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = scaledReadingSp(bodyFontSp) * 0.58f,
                                baselineShift = BaselineShift.Superscript,
                            )
                        ) { append(geezNumeral(verse.n)) }
                        append("  ")
                        append(verse.text)
                    }
                    Text(
                        text = annotated,
                        style = readingBodyStyle(bodyFontSp),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = verseGap / 2)
                            .clip(RoundedCornerShape(8.dp))
                            .background(when {
                                verse.n in selRange -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)
                                else -> sinq.highlight(
                                    highlights[HighlightRepository.verseKey(highlightSectionId, verse.n)]
                                ).takeUnless { it == Color.Transparent } ?: Color.Transparent
                            })
                            .semantics { selected = verse.n in selRange }
                            .clickable {
                                val (a, bSel) = com.agpeya.app.ui.reading.advanceFlatSelection(selA, verse.n)
                                selA = a
                                selB = bSel
                            }
                            .padding(horizontal = Spacing.sm),
                    )
                }
                // The edition's own heading, where it prints one — a psalm's
                // superscription, the note opening ሲኖዶስ. The parser used to
                // drop every one of them.
                val verseBlock = @Composable { verse: com.agpeya.app.model.ScriptureVerse ->
                    current.headings[verse.n]?.let { heading ->
                        Text(
                            heading,
                            style = MaterialTheme.typography.titleSmall.inReadingFont(),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.md, bottom = Spacing.xs, start = Spacing.sm, end = Spacing.sm),
                        )
                    }
                    body(verse)
                }
                if (tinted) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xxs)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f))
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                            .padding(vertical = Spacing.xs),
                    ) { row.forEach { verseBlock(it) } }
                } else {
                    Column(Modifier.fillMaxWidth()) { row.forEach { verseBlock(it) } }
                }
            }
            item {
                // The day's own chapters, when this is one of them: which of
                // them this is, the next of them, and the tick.
                val day = planDay?.second
                val dayChapters = day?.r.orEmpty().flatMap { r -> r.chapters.map { r.b to it } }
                val here = dayChapters.indexOf(bookKey to chapter)
                if (day != null && here >= 0) {
                    val read = com.agpeya.app.data.ReadingPlanRepository.isRead(planState, day)
                    ListRow(
                        title = s.readingChapterOfDay(here + 1, dayChapters.size),
                        subtitle = if (read) s.readingDone else s.readingMarkDone,
                        leadingIcon = if (read) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.MenuBook,
                        leadingTint = if (read) MaterialTheme.colorScheme.secondary else null,
                        onClick = {
                            scope.launch {
                                if (read) {
                                    planDay.first.let { plan ->
                                        com.agpeya.app.data.ReadingPlanRepository.unmarkDay(context, plan, day)
                                    }
                                } else {
                                    com.agpeya.app.data.ReadingPlanRepository.markDay(context, day, today)
                                }
                            }
                        },
                    )
                    dayChapters.getOrNull(here + 1)?.let { (nextBook, nextChapter) ->
                        if (nextBook == bookKey) {
                            ChapterStepper(onPrevious = null, onNext = { chapter = nextChapter })
                        } else {
                            ListRow(
                                title = s.nextChapter,
                                subtitle = nextBook,
                                onClick = {
                                    onOpenRoute(
                                        com.agpeya.app.ui.reading.planReadingRoute(nextBook, nextChapter),
                                    )
                                },
                            )
                        }
                    }
                }
                ChapterStepper(
                    onPrevious = { chapter -= 1 }.takeIf { chapter > 1 },
                    onNext = { chapter += 1 }.takeIf { chapter < b.chapters.size },
                )
                Spacer(Modifier.height(Spacing.huge))
            }
        }
        if (chaptersOpen) {
            com.agpeya.app.ui.common.ChapterSheet(
                count = b.chapters.size,
                current = chapter - 1,
                onPick = { chaptersOpen = false; chapter = it + 1 },
                onDismiss = { chaptersOpen = false },
            )
        }
    }
}

/**
 * The two ends of a chapter.
 *
 * Reading to the foot of ኦሪት ዘፍጥረት ፩ and wanting ፪ meant scrolling back to the
 * top and finding it in the strip. Every other reader in the app steps from
 * where the reading ends; this one did not.
 */
@Composable
private fun ChapterStepper(onPrevious: (() -> Unit)?, onNext: (() -> Unit)?) {
    if (onPrevious == null && onNext == null) return
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    val colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xxl),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onPrevious != null) {
            androidx.compose.material3.TextButton(onClick = onPrevious, colors = colors) {
                Text("‹  ${s.previousChapter}", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Spacer(Modifier.width(Spacing.xxs))
        }
        if (onNext != null) {
            androidx.compose.material3.TextButton(onClick = onNext, colors = colors) {
                Text("${s.nextChapter}  ›", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

