package com.agpeya.app.ui.psalter

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
import com.agpeya.app.data.HighlightRepository
import com.agpeya.app.data.ReadingMode
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.ScriptureRepository
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.model.Bookmark
import com.agpeya.app.model.Section
import com.agpeya.app.search.AmharicSearch
import com.agpeya.app.ui.reading.HighlightBar
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
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var geez by rememberSaveable { mutableStateOf(initialGeez) }
    val psalms by produceState(emptyList<Section>(), geez) {
        value = ScriptureRepository.psalms(context, geez)
    }
    val fontStep by SettingsRepository.fontStep(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val readingMode by SettingsRepository.readingMode(context)
        .collectAsState(initial = ReadingMode.VERTICAL)
    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())
    // Scope to the Psalter's own group — an hour may bookmark the same "ps_N" id.
    val bookmarkedIds = remember(bookmarks) {
        bookmarks.filter { it.hourId == PSALTER_BOOKMARK_ID }.map { it.sectionId }.toSet()
    }
    val highlights by HighlightRepository.highlights(context).collectAsState(initial = emptyMap())
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]

    // A ግጻዌ psalm link carries the cited verse range; tint just that psalm's verses.
    val citedPsalmNumber = if (initialPsalmIndex >= 0 && initialStartVerse > 0) initialPsalmIndex + 1 else -1
    val citedRange = if (initialStartVerse > 0)
        initialStartVerse..(if (initialEndVerse > 0) initialEndVerse else initialStartVerse)
    else IntRange.EMPTY

    // Opened from a bookmark: show the whole psalter so the target psalm exists.
    var daily by remember { mutableStateOf(initialPsalmIndex < 0) }
    val today by rememberCurrentDate()
    val range = remember(today) { dailyRange(today.dayOfWeek) }
    // derivedStateOf (not a plain remember) so the pager/list lambdas below read
    // the current list at call time — a recomputed local would leave pageCount and
    // key lambdas holding different generations of the list while psalms load.
    val shown by remember(range) {
        derivedStateOf {
            if (!daily) psalms
            else range?.let { r -> psalms.filter { it.number in r } } ?: emptyList()
        }
    }
    // The vertical list has a range-header item before the psalms in daily mode.
    fun headerCount() = if (daily && range != null) 1 else 0

    var showContents by remember { mutableStateOf(false) }
    // Verse currently chosen for highlighting (shows the colour palette); null = none.
    // Live verse selection: first tap anchors, later taps in the same psalm
    // extend the run (see ReadingScreen — the same two-key model).
    var selStart by remember { mutableStateOf<String?>(null) }
    var selEnd by remember { mutableStateOf<String?>(null) }
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
                    TextButton(onClick = {
                        selStart = null
                        selEnd = null
                        geez = !geez
                    }) {
                        Text(
                            if (geez) s.wudaseLangGeez else s.wudaseLangAmharic,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    com.agpeya.app.ui.common.ReaderToolsMenu(
                        fontStep = fontStep,
                        maxFontStep = FONT_STEPS_SP.lastIndex,
                        onFontChange = { step -> scope.launch { SettingsRepository.setFontStep(context, step) } },
                        secondaryActionLabel = if (daily) s.wholePsalter else s.dailyPsalms,
                        onSecondaryAction = {
                            selStart = null
                            selEnd = null
                            daily = !daily
                            anchor = -1
                        },
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
        bottomBar = {
            HighlightBar(
                visible = selStart != null,
                currentColor = com.agpeya.app.ui.reading.selectionKeys(shown, selStart, null) {
                    if (geez) HighlightRepository.GEEZ_PSALTER_NAMESPACE
                    else HighlightRepository.AMHARIC_PSALTER_NAMESPACE
                }.firstOrNull()?.let { highlights[it] },
                onPick = { colorKey ->
                    val keys = com.agpeya.app.ui.reading.selectionKeys(shown, selStart, selEnd) {
                        if (geez) HighlightRepository.GEEZ_PSALTER_NAMESPACE
                        else HighlightRepository.AMHARIC_PSALTER_NAMESPACE
                    }
                    selStart = null; selEnd = null
                    if (keys.isNotEmpty()) scope.launch {
                        HighlightRepository.setHighlights(context, keys, colorKey)
                    }
                },
                onDismiss = { selStart = null; selEnd = null },
                shareText = com.agpeya.app.ui.reading.verseShareText(shown, selStart, selEnd),
                shareImage = com.agpeya.app.ui.reading.versePayload(shown, selStart, s.psalterTitle, selEnd),
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            val onVerseTap: (String) -> Unit = { key ->
                val (a, b) = com.agpeya.app.ui.reading.advanceSelection(selStart, key)
                selStart = a
                selEnd = b
            }
            if (daily && range == null) {
                // No daily division is appointed for Sunday — state the fact,
                // and offer the way out (the whole Psalter) instead of a dead end.
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    StatePanel(
                        icon = Icons.Outlined.LibraryMusic,
                        title = s.noSundayDivision,
                        actionLabel = s.wholePsalter,
                        onAction = { daily = false },
                    )
                }
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
                                highlights = highlights,
                                onVerseTap = onVerseTap,
                                highlightNamespace = if (geez) HighlightRepository.GEEZ_PSALTER_NAMESPACE
                                else HighlightRepository.AMHARIC_PSALTER_NAMESPACE,
                                citedRange = if (section.number == citedPsalmNumber) citedRange else IntRange.EMPTY,
                                selectedRange = com.agpeya.app.ui.reading.selectionRangeFor(section, selStart, selEnd),
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
                                        highlights = highlights,
                                        onVerseTap = onVerseTap,
                                        highlightNamespace = if (geez) HighlightRepository.GEEZ_PSALTER_NAMESPACE
                                        else HighlightRepository.AMHARIC_PSALTER_NAMESPACE,
                                        citedRange = if (section.number == citedPsalmNumber) citedRange else IntRange.EMPTY,
                                        selectedRange = com.agpeya.app.ui.reading.selectionRangeFor(section, selStart, selEnd),
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
                    selStart = null
                    selEnd = null
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
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(s.searchHint) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        Spacer(Modifier.height(8.dp))
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
            item { Spacer(Modifier.height(36.dp)) }
        }
    }
}
