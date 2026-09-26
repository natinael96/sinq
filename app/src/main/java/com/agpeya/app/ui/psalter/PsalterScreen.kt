package com.agpeya.app.ui.psalter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.ReadingMode
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.ScriptureRepository
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.model.Bookmark
import com.agpeya.app.model.Section
import com.agpeya.app.search.AmharicSearch
import com.agpeya.app.ui.reading.SelectionBar
import com.agpeya.app.ui.reading.SectionView
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.rememberCurrentDate
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.reading.ReadingColumn
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import androidx.compose.runtime.snapshotFlow
import com.agpeya.app.data.HabitsRepository
import kotlinx.coroutines.flow.first

/** Pseudo hour id psalter bookmarks are stored under — never a real hour id. */
const val PSALTER_BOOKMARK_ID = "psalter"

private val FONT_STEPS_SP = com.agpeya.app.data.SettingsRepository.FONT_STEPS_SP

/** Traditional weekday division of the Psalter; Sunday is not yet defined.
 *  Public because Home's የዕለቱ መዝሙረ ዳዊት card names the same portion. */
fun dailyRange(day: DayOfWeek): IntRange? = when (day) {
    DayOfWeek.MONDAY -> 1..30
    DayOfWeek.TUESDAY -> 31..60
    DayOfWeek.WEDNESDAY -> 61..80
    DayOfWeek.THURSDAY -> 81..110
    DayOfWeek.FRIDAY -> 111..130
    DayOfWeek.SATURDAY -> 131..150
    DayOfWeek.SUNDAY -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsalterScreen(
    initialPsalmIndex: Int = -1,
    initialStartVerse: Int = -1,
    initialEndVerse: Int = -1,
    initialGeez: Boolean = false,
    onBack: () -> Unit,
    onWriteNote: (route: String, label: String) -> Unit,
    /** Opens a chapter of a shelf book — Sunday's reading lives there, not here. */
    onOpenBook: (bookId: String, chapter: Int) -> Unit = { _, _ -> },
    /** Follows a route out of the selection bar — the Catena page. */
    onOpenRoute: (route: String) -> Unit = {},
) {
    com.agpeya.app.ui.common.ReaderAwake()
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var geez by rememberSaveable { mutableStateOf(initialGeez) }
    val psalmsLoad = com.agpeya.app.ui.common.rememberContentLoad(geez) { ScriptureRepository.psalms(context, geez) }

    // The edition on screen while another one loads.
    //
    // Switching edition is the one thing that makes this screen load again, and
    // emptying the reader for it took the ግእዝ/አማርኛ control off the bar along
    // with everything else: contentLoadScreen draws a top bar of its own, and it
    // carries no actions. Parsing either Psalter is half a megabyte of JSON —
    // 40ms on a desktop JVM, several hundred on a phone — so the control was
    // simply absent for as long as the switch took, and a second tap in that
    // window landed on nothing at all. That is what "it stops after a couple of
    // toggles" was: not a toggle that failed, a toggle that was not there.
    //
    // So the reader keeps the text it has, control included, and swaps when the
    // other edition arrives. Held in a LaunchedEffect rather than assigned
    // during composition, which would be a write to state from inside a
    // composition pass.
    val fresh = psalmsLoad.value
    var held by remember { mutableStateOf(emptyList<Section>()) }
    androidx.compose.runtime.LaunchedEffect(fresh) { if (!fresh.isNullOrEmpty()) held = fresh }
    val psalms = fresh ?: held
    // Showing the previous edition while the next one parses is only honest if
    // the page says so, because the control has already changed its label.
    val reloading = psalmsLoad.result == null && psalms.isNotEmpty()
    val fontStep by SettingsRepository.fontStep(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val readingMode by SettingsRepository.readingMode(context)
        .collectAsState(initial = ReadingMode.VERTICAL)
    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())
    // Scope to the Psalter's own group — an hour may bookmark the same "ps_N" id.
    val bookmarkedIds = remember(bookmarks) {
        bookmarks.filter { it.hourId == PSALTER_BOOKMARK_ID }.map { it.sectionId }.toSet()
    }
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]

    // A ግጻዌ psalm link carries the cited verse range; tint just that psalm's verses.
    // Opened from a bookmark: show the whole psalter so the target psalm exists.
    var daily by rememberSaveable { mutableStateOf(initialPsalmIndex < 0) }
    val today by rememberCurrentDate()
    val range = remember(today) { dailyRange(today.dayOfWeek) }
    // derivedStateOf (not a plain remember) so the pager/list lambdas below read
    // the current list at call time — a recomputed local would leave pageCount and
    // key lambdas holding different generations of the list while psalms load.
    //
    // The list itself arrives through [rememberUpdatedState] rather than being
    // captured, and this is remembered on [range] alone. Re-keying it on the
    // list would build a new derivedStateOf whenever the edition changed, and
    // pageCount and the key lambdas — remembered once — would go on reading the
    // old one.
    val currentPsalms by androidx.compose.runtime.rememberUpdatedState(psalms)
    val shown by remember(range) {
        derivedStateOf {
            if (!daily) currentPsalms
            else range?.let { r -> currentPsalms.filter { it.number in r } } ?: emptyList()
        }
    }
    // The vertical list has a range-header item before the psalms in daily mode.
    fun headerCount() = if (daily && range != null) 1 else 0

    var showContents by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val pagerState = rememberPagerState(pageCount = { shown.size })
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Psalm index (into `shown`) to land on; -1 = none. Set from a bookmark and
    // when switching reading mode, so the position carries over.
    var anchor by remember { mutableIntStateOf(initialPsalmIndex) }
    LaunchedEffect(readingMode, anchor, shown.size) {
        if (anchor < 0 || shown.isEmpty()) return@LaunchedEffect
        val target = anchor.coerceIn(0, shown.size - 1)
        when (readingMode) {
            ReadingMode.VERTICAL -> listState.scrollToItem(target + headerCount())
            ReadingMode.HORIZONTAL -> pagerState.scrollToPage(target)
        }
    }

    // Taking the screen over is for having nothing to show — the first open,
    // or content that is genuinely missing. Never for a switch of edition.
    //
    // This used to sit directly under the load, which meant that switching
    // edition returned from the composable for as long as it took, disposing
    // everything below: the scroll position, the pager, and `daily`. Tapping
    // ግእዝ while reading the whole Psalter threw the reader back to the top of
    // today's division.
    if (psalms.isEmpty()) {
        com.agpeya.app.ui.common.contentLoadScreen(psalmsLoad, s.psalterTitle, onBack, missing = true)
        return
    }

    fun toggleBookmark(section: Section) {
        scope.launch {
            UserDataRepository.toggleBookmark(
                context,
                Bookmark(
                    hourId = PSALTER_BOOKMARK_ID,
                    hourName = s.psalterTitle,
                    sectionId = section.id,
                    // Index into the whole psalter, so opening the bookmark can
                    // scroll there regardless of the daily filter.
                    sectionIndex = (section.number ?: 1) - 1,
                    title = section.title,
                    subtitle = section.subtitle,
                ),
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.psalterTitle,
                // The division being read, so the title bar answers "which
                // psalms am I looking at" without a second glance.
                subtitle = if (daily && range != null) s.psalmRange(range.first, range.last) else null,
                onBack = onBack,
                actions = {
                    com.agpeya.app.ui.common.EditionToggle(geez = geez) {

                        geez = !geez
                    }
                    com.agpeya.app.ui.common.ReaderToolsMenu(
                        fontStep = fontStep,
                        maxFontStep = FONT_STEPS_SP.lastIndex,
                        onFontChange = { step -> scope.launch { SettingsRepository.setFontStep(context, step) } },
                        onWriteNote = {
                            // Which psalm is actually on screen, derived the
                            // same way the layout toggle derives it.
                            val index = if (readingMode == ReadingMode.VERTICAL) {
                                (listState.firstVisibleItemIndex - headerCount()).coerceAtLeast(0)
                            } else {
                                pagerState.currentPage
                            }
                            onWriteNote(
                                "psalter?section=${((shown.getOrNull(index)?.number ?: 1) - 1)}&lang=${if (geez) "gez" else "am"}",
                                shown.getOrNull(index)?.title ?: s.psalterTitle,
                            )
                        },
                        secondaryActionLabel = if (daily) s.wholePsalter else s.dailyPsalms,
                        onSecondaryAction = {

                            daily = !daily
                            anchor = -1
                        },
                        readingMode = readingMode,
                        onToggleReadingMode = {
                            // Preserve the visible Psalm when changing layout.
                            anchor = if (readingMode == ReadingMode.VERTICAL) {
                                (listState.firstVisibleItemIndex - headerCount()).coerceAtLeast(0)
                            } else {
                                pagerState.currentPage
                            }
                            scope.launch {
                                SettingsRepository.setReadingMode(
                                    context,
                                    if (readingMode == ReadingMode.VERTICAL) ReadingMode.HORIZONTAL
                                    else ReadingMode.VERTICAL,
                                )
                            }
                        },
                    )
                    IconButton(onClick = { showContents = true }) {
                        Icon(
                            Icons.Outlined.Menu,
                            contentDescription = s.contents,
                            modifier = Modifier.size(IconSize.medium),
                        )
                    }
                },
            )
        },

    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            if (reloading) {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = innerPadding.calculateTopPadding())
                        .height(2.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = androidx.compose.ui.graphics.Color.Transparent,
                )
            }
            if (daily && range == null) {
                // No division of the Psalter is appointed for Sunday, and this
                // used to say so and stop. But the printed ዳዊት does not stop:
                // after the hundred and fifty psalms it carries the fifteen
                // canticles and መኃልየ መኃልይ, and those are Sunday's reading. The
                // book on the shelf follows the edition being read here.
                SundayCanticles(
                    geez = geez,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    onOpen = onOpenBook,
                    onWholePsalter = { daily = false },
                )
            } else when (readingMode) {
                ReadingMode.VERTICAL -> {
                    ReadingColumn(state = listState, innerPadding = innerPadding) {
                        if (daily && range != null) {
                            item {
                                Text(
                                    s.psalmRange(range.first, range.last),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                        items(shown.size, key = { shown[it].id }) { i ->
                            val section = shown[i]
                            SectionView(
                                section = section,
                                bodyFontSp = bodyFontSp,
                                isBookmarked = section.id in bookmarkedIds,
                                onToggleBookmark = { toggleBookmark(section) },

                            )
                        }
                        item { Spacer(Modifier.height(Spacing.huge)) }
                    }
                }
                ReadingMode.HORIZONTAL -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f),
                            key = { shown.getOrNull(it)?.id ?: it },
                        ) { page ->
                            // getOrNull: a composed page can outlive the list for a frame
                            // when the daily/all toggle shrinks it under the pager.
                            val section = shown.getOrNull(page) ?: return@HorizontalPager
                            ReadingColumn(innerPadding = PaddingValues(0.dp)) {
                                item {
                                    SectionView(
                                        section = section,
                                        bodyFontSp = bodyFontSp,
                                        isBookmarked = section.id in bookmarkedIds,
                                        onToggleBookmark = { toggleBookmark(section) },

                                    )
                                    Spacer(Modifier.height(Spacing.huge))
                                }
                            }
                        }
                        if (shown.isNotEmpty()) {
                            com.agpeya.app.ui.reading.PageIndicator(
                                current = pagerState.currentPage + 1,
                                total = shown.size,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showContents) {
        ModalBottomSheet(
            onDismissRequest = { showContents = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            PsalterContents(
                psalms = shown,
                onSelect = { index ->

                    scope.launch {
                        sheetState.hide()
                        showContents = false
                        when (readingMode) {
                            ReadingMode.VERTICAL -> listState.animateScrollToItem(index + headerCount())
                            ReadingMode.HORIZONTAL -> pagerState.animateScrollToPage(index)
                        }
                    }
                },
            )
        }
    }
}

/** Contents sheet with search: by psalm number or (homophone-tolerant) text. */
@Composable
private fun PsalterContents(psalms: List<Section>, onSelect: (Int) -> Unit) {
    val s = LocalStrings.current
    var query by remember { mutableStateOf("") }
    val indexed = remember(psalms) { psalms.withIndex().toList() }
    val filtered = remember(indexed, query) {
        val q = query.trim()
        if (q.isEmpty()) indexed
        else {
            val digits = q.filter { it.isDigit() }
            val folded = AmharicSearch.fold(q)
            indexed.filter { (_, p) ->
                (digits.isNotEmpty() && p.number.toString().startsWith(digits)) ||
                    (digits.isEmpty() && AmharicSearch.fold("${p.title} ${p.subtitle ?: ""}").contains(folded))
            }
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(s.contents, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(s.searchHint) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        Spacer(Modifier.height(Spacing.sm))
        LazyColumn(Modifier.fillMaxWidth()) {
            items(filtered.size, key = { filtered[it].value.id }) { i ->
                val (index, p) = filtered[i]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        .padding(vertical = 10.dp),
                ) {
                    Text(p.title, style = MaterialTheme.typography.titleMedium.inReadingFont(), color = MaterialTheme.colorScheme.onSurface)
                    p.subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item { Spacer(Modifier.height(Spacing.huge)) }
        }
    }
}

/**
 * Sunday's reading: the twenty chapters of ጸሎት ነቢያት, in the edition the Psalter
 * is being read in. The chapters open in the book reader rather than inline,
 * so a canticle uses the same quiet prayer layout and bookmark controls.
 */
@Composable
private fun SundayCanticles(
    geez: Boolean,
    modifier: Modifier,
    onOpen: (bookId: String, chapter: Int) -> Unit,
    onWholePsalter: () -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val title = if (geez) SUNDAY_BOOK_GEEZ else SUNDAY_BOOK_AMHARIC
    val book by produceState<com.agpeya.app.model.Book?>(null, title) {
        val meta = com.agpeya.app.data.BookRepository.all(context).find { it.title == title }
        value = meta?.let { com.agpeya.app.data.BookRepository.book(context, it.id) }
    }
    val found = book
    if (found == null) {
        // The shelf is missing the book — say the old thing rather than nothing.
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            StatePanel(
                icon = Icons.Outlined.LibraryMusic,
                title = s.noSundayDivision,
                actionLabel = s.wholePsalter,
                onAction = onWholePsalter,
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = com.agpeya.app.ui.theme.Spacing.screen,
            vertical = com.agpeya.app.ui.theme.Spacing.md,
        ),
    ) {
        item(key = "head") {
            com.agpeya.app.ui.common.SectionHeader(s.sundayCanticlesTitle)
            Text(
                s.sundayCanticlesBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = com.agpeya.app.ui.theme.Spacing.sm),
            )
        }
        items(found.chapters.size, key = { found.chapters[it].number }) { i ->
            val chapter = found.chapters[i]
            com.agpeya.app.ui.common.ListRow(
                title = chapter.title,
                onClick = { onOpen(found.id, chapter.number) },
            )
        }
        item(key = "whole") {
            Spacer(Modifier.height(com.agpeya.app.ui.theme.Spacing.lg))
            Text(
                s.wholePsalter,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .clickable(onClick = onWholePsalter)
                    .padding(vertical = com.agpeya.app.ui.theme.Spacing.sm),
            )
        }
    }
}

/** The two editions of the book on the shelf, matched by title. */
private const val SUNDAY_BOOK_AMHARIC = "ጸሎት ነቢያት በአማርኛ"
private const val SUNDAY_BOOK_GEEZ = "ጸሎት ነቢያት በግእዝ"
