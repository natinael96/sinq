package com.agpeya.app.ui.reading

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.HighlightRepository
import com.agpeya.app.data.LayoutRepository
import com.agpeya.app.data.PrayerLayout
import com.agpeya.app.data.PrayerLevelRepository
import com.agpeya.app.data.PrayerLevel
import com.agpeya.app.data.ReadingMode
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.model.Bookmark
import com.agpeya.app.model.Hour
import com.agpeya.app.model.HourLayout
import com.agpeya.app.model.Section
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.ReadingMaxWidth
import com.agpeya.app.ui.theme.Spacing
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.inReadingFont
import kotlinx.coroutines.launch

private val FONT_STEPS_SP = com.agpeya.app.data.SettingsRepository.FONT_STEPS_SP

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    hourId: String,
    initialSectionIndex: Int = -1,
    /** Preferred over [initialSectionIndex]: section ids are permanent, while an
     *  index shifts as soon as the user reorders or hides sections. */
    initialSectionId: String? = null,
    onBack: () -> Unit,
    onSwitchHour: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val hour by produceState<Hour?>(initialValue = null, hourId) {
        value = com.agpeya.app.data.HoursRepository.hourById(context, hourId)
    }
    // All hours for the title dropdown (switch prayer without going back).
    val allHours by produceState<List<Hour>>(initialValue = emptyList()) {
        value = com.agpeya.app.data.HoursRepository.visibleHours(context)
    }
    var hourMenu by remember { mutableStateOf(false) }
    val fontStep by SettingsRepository.fontStep(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val readingMode by SettingsRepository.readingMode(context)
        .collectAsState(initial = ReadingMode.VERTICAL)
    val keepScreenOn by SettingsRepository.keepScreenOn(context).collectAsState(initial = true)
    val prayerLevel by SettingsRepository.prayerLevel(context).collectAsState(initial = PrayerLevel.FULL)
    // A reader can continue with the complete hour without changing the saved setting.
    var showFullHour by remember(hourId) { mutableStateOf(false) }
    val effectivePrayerLevel = if (showFullHour) PrayerLevel.FULL else prayerLevel
    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())
    // Scope to this hour: the same section id can be bookmarked from elsewhere
    // (a psalm added into an hour shares its id with the Psalter's copy).
    val bookmarkedIds = remember(bookmarks, hourId) {
        bookmarks.filter { it.hourId == hourId }.map { it.sectionId }.toSet()
    }
    val layout by LayoutRepository.layout(context, hourId).collectAsState(initial = HourLayout())
    val highlights by HighlightRepository.highlights(context).collectAsState(initial = emptyMap())
    var showContents by remember { mutableStateOf(false) }
    // The live verse selection: [selStart] anchors on the first tap, [selEnd]
    // follows later taps in the same section, so a run of verses can be
    // highlighted or shared together. Both null = no bar.
    var selStart by remember(hourId) { mutableStateOf<String?>(null) }
    var selEnd by remember(hourId) { mutableStateOf<String?>(null) }
    // Apply the user's per-hour customization (show/hide, reorder, added psalms).
    val sections by produceState(emptyList<Section>(), hour, layout, effectivePrayerLevel) {
        val h = hour
        value = if (h == null) emptyList()
        else {
            val extras = layout.added.mapNotNull { ContentRepository.psalm(context, it) }
            PrayerLevelRepository.apply(
                context,
                hourId,
                PrayerLayout.ordered(h.sections, extras, layout)
                    .filter { it.type == "gospel" || it.id !in layout.hidden },
                effectivePrayerLevel,
            )
        }
    }
    val listState = rememberLazyListState()
    val pagerState = rememberPagerState(pageCount = { sections.size })
    val contentsIndex by remember(readingMode, listState, pagerState) {
        derivedStateOf {
            if (readingMode == ReadingMode.VERTICAL) listState.firstVisibleItemIndex
            else pagerState.currentPage
        }
    }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    val motion = LocalMotion.current
    val highlightNamespaceFor: (Section) -> String? = { section ->
        if (section.id.startsWith("ps_")) HighlightRepository.AMHARIC_PSALTER_NAMESPACE else null
    }

    // The section we want kept in view; shared across both readers so the
    // position survives a mode switch.
    var anchor by remember(hourId) { mutableIntStateOf(-1) }
    // Neighbours in the user's own order, so hiding an hour doesn't leave a gap.
    val prevHour = remember(allHours, hourId) { com.agpeya.app.data.PrayerSchedule.previous(allHours, hourId) }
    val nextHour = remember(allHours, hourId) { com.agpeya.app.data.PrayerSchedule.next(allHours, hourId) }

    // Record recent + decide the initial anchor once content is ready.
    LaunchedEffect(hour, sections.size) {
        val h = hour ?: return@LaunchedEffect
        if (sections.isEmpty()) return@LaunchedEffect
        UserDataRepository.recordRecent(context, h.id)
        // Resolve the target to the displayed (customized) list. A section id is
        // exact; the legacy index is a fallback for bookmarks saved before ids
        // were passed, and only means anything against the full section list.
        anchor = when {
            initialSectionId != null ->
                sections.indexOfFirst { it.id == initialSectionId }.takeIf { it >= 0 } ?: 0
            initialSectionIndex >= 0 -> {
                val targetId = h.sections.getOrNull(initialSectionIndex)?.id
                sections.indexOfFirst { it.id == targetId }.takeIf { it >= 0 } ?: 0
            }
            else -> UserDataRepository.savedPosition(context, h.id).coerceIn(0, sections.size - 1)
        }
    }

    // Scroll whichever reader is currently active to the anchor. Keyed on
    // readingMode so a mode switch re-runs this against the now-composed reader
    // (calling scrollToPage on an un-composed pager would suspend forever).
    LaunchedEffect(readingMode, anchor, sections.size) {
        if (anchor < 0 || sections.isEmpty()) return@LaunchedEffect
        val target = anchor.coerceIn(0, sections.size - 1)
        if (readingMode == ReadingMode.VERTICAL) listState.scrollToItem(target)
        else pagerState.scrollToPage(target)
    }

    // Keep-screen-on while this screen is visible.
    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        val window = context.findActivity()?.window
        if (keepScreenOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Persist scroll position on leave.
    DisposableEffect(hourId, readingMode) {
        onDispose {
            val index = if (readingMode == ReadingMode.VERTICAL) listState.firstVisibleItemIndex
            else pagerState.currentPage
            scope.launch { UserDataRepository.savePosition(context, hourId, index) }
        }
    }

    fun toggleBookmark(section: Section, index: Int) {
        val h = hour ?: return
        scope.launch {
            UserDataRepository.toggleBookmark(
                context,
                Bookmark(
                    hourId = h.id,
                    hourName = h.name,
                    sectionId = section.id,
                    sectionIndex = index,
                    title = section.title,
                    subtitle = section.subtitle,
                ),
            )
        }
    }

    Scaffold(
        // Collapsing app bar only makes sense for vertical reading; in paged mode the
        // nested-scroll connection can swallow the pager's swipes, so leave it off.
        modifier = if (readingMode == ReadingMode.VERTICAL) {
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            Modifier
        },
        topBar = {
            SinqTopBar(
                title = hour?.name ?: "",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                titleContent = {
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable(role = Role.DropdownList) { hourMenu = true }
                                .padding(horizontal = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = hour?.name ?: "",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(expanded = hourMenu, onDismissRequest = { hourMenu = false }) {
                            allHours.forEach { h ->
                                val isCurrent = h.id == hourId
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            h.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isCurrent) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    onClick = {
                                        hourMenu = false
                                        if (!isCurrent) onSwitchHour(h.id)
                                    },
                                )
                            }
                        }
                    }
                },
                actions = {
                    com.agpeya.app.ui.common.ReaderToolsMenu(
                        fontStep = fontStep,
                        maxFontStep = FONT_STEPS_SP.lastIndex,
                        onFontChange = { step -> scope.launch { SettingsRepository.setFontStep(context, step) } },
                        secondaryActionLabel = s.showFullPsalms.takeIf { effectivePrayerLevel != PrayerLevel.FULL },
                        onSecondaryAction = if (effectivePrayerLevel != PrayerLevel.FULL) ({ showFullHour = true }) else null,
                        onToggleReadingMode = {
                            // Preserve the visible passage when changing layout.
                            anchor = if (readingMode == ReadingMode.VERTICAL) {
                                listState.firstVisibleItemIndex
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
                currentColor = selectionKeys(sections, selStart, null, highlightNamespaceFor)
                    .firstOrNull()?.let { highlights[it] },
                onPick = { colorKey ->
                    val keys = selectionKeys(sections, selStart, selEnd, highlightNamespaceFor)
                    selStart = null; selEnd = null
                    if (keys.isNotEmpty()) scope.launch {
                        HighlightRepository.setHighlights(context, keys, colorKey)
                    }
                },
                onDismiss = { selStart = null; selEnd = null },
                shareText = verseShareText(sections, selStart, selEnd),
                shareImage = versePayload(sections, selStart, hour?.name, selEnd),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = sections.isNotEmpty(),
                enter = fadeIn(tween(motion.millis(300))),
            ) {
                val onVerseTap: (String) -> Unit = { key ->
                    val (a, b) = advanceSelection(selStart, key)
                    selStart = a
                    selEnd = b
                }
                val selectionFor: (Section) -> IntRange = { sec -> selectionRangeFor(sec, selStart, selEnd) }
                when (readingMode) {
                    ReadingMode.VERTICAL -> VerticalReader(
                        sections, listState, bodyFontSp, innerPadding, bookmarkedIds,
                        ::toggleBookmark, highlights, onVerseTap, selectionFor,
                        highlightNamespaceFor,
                        prevHour?.let { h -> { onSwitchHour(h.id) } },
                        nextHour?.let { h -> { onSwitchHour(h.id) } },
                    )
                    ReadingMode.HORIZONTAL -> PagedReader(
                        sections, pagerState, bodyFontSp, innerPadding, bookmarkedIds,
                        ::toggleBookmark, highlights, onVerseTap, selectionFor,
                        highlightNamespaceFor,
                    )
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
            ContentsSheet(
                sections = sections,
                // Where the reader is right now, so opening the contents tells
                // you where you are before it asks where you want to go.
                currentIndex = contentsIndex,
                onSelect = { index ->
                    scope.launch {
                        sheetState.hide()
                        showContents = false
                        when (readingMode) {
                            ReadingMode.VERTICAL -> listState.animateScrollToItem(index)
                            ReadingMode.HORIZONTAL -> pagerState.animateScrollToPage(index)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun VerticalReader(
    sections: List<Section>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    bodyFontSp: Int,
    innerPadding: PaddingValues,
    bookmarkedIds: Set<String>,
    onToggleBookmark: (Section, Int) -> Unit,
    highlights: Map<String, String>,
    onVerseTap: (String) -> Unit,
    selectionFor: (Section) -> IntRange,
    highlightNamespaceFor: (Section) -> String?,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
) {
    ReadingColumn(
        state = listState,
        innerPadding = innerPadding,
    ) {
        items(sections.size, key = { sections[it].id }) { index ->
            val section = sections[index]
            if (section.part != null && sections.getOrNull(index - 1)?.part != section.part) {
                PartHeader(section.part)
            }
            SectionView(
                section = section,
                bodyFontSp = bodyFontSp,
                isBookmarked = section.id in bookmarkedIds,
                onToggleBookmark = { onToggleBookmark(section, index) },
                highlights = highlights,
                onVerseTap = onVerseTap,
                highlightNamespace = highlightNamespaceFor(section),
                selectedRange = selectionFor(section),
            )
        }
        item {
            HourStepper(onPrevious = onPrevious, onNext = onNext)
            Spacer(Modifier.height(56.dp))
        }
    }
}

@Composable
private fun PagedReader(
    sections: List<Section>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    bodyFontSp: Int,
    innerPadding: PaddingValues,
    bookmarkedIds: Set<String>,
    onToggleBookmark: (Section, Int) -> Unit,
    highlights: Map<String, String>,
    onVerseTap: (String) -> Unit,
    selectionFor: (Section) -> IntRange,
    highlightNamespaceFor: (Section) -> String?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            // The pager's pageCount lambda reads the live section list while
            // these lambdas hold the one from the current composition; when the
            // list changes size (the full-psalms toggle), the pager can measure
            // a page index the captured list doesn't have yet — index safely.
            key = { sections.getOrNull(it)?.id ?: it },
        ) { page ->
            val section = sections.getOrNull(page) ?: return@HorizontalPager
            // LazyColumn (not Column+verticalScroll) so verse taps register inside
            // the pager — the scroll Column was consuming them.
            ReadingColumn(innerPadding = PaddingValues(0.dp)) {
                item {
                    section.part?.let { part ->
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            text = part,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                    SectionView(
                        section = section,
                        bodyFontSp = bodyFontSp,
                        isBookmarked = section.id in bookmarkedIds,
                        onToggleBookmark = { onToggleBookmark(section, page) },
                        highlights = highlights,
                        onVerseTap = onVerseTap,
                        highlightNamespace = highlightNamespaceFor(section),
                        selectedRange = selectionFor(section),
                    )
                    Spacer(Modifier.height(Spacing.huge))
                }
            }
        }
        PageIndicator(current = pagerState.currentPage + 1, total = sections.size)
    }
}

/** "፫ / ፲፪" under a paged reader — where you are, in the numerals the page uses. */
@Composable
internal fun PageIndicator(current: Int, total: Int) {
    Text(
        text = "${geezNumeral(current)} / ${geezNumeral(total)}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        textAlign = TextAlign.Center,
    )
}

/**
 * The reading list every reader is built on: page margins, and a ceiling on how
 * wide a line of text may get. On a phone the ceiling never binds; on a tablet
 * it is the difference between a book and a spreadsheet.
 */
@Composable
internal fun ReadingColumn(
    innerPadding: PaddingValues,
    state: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize().widthIn(max = ReadingMaxWidth),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.sm),
            content = content,
        )
    }
}

/**
 * The contents sheet. The section being read is marked in gold — a table of
 * contents that can't tell you where you are is only half of one.
 */
@Composable
private fun ContentsSheet(sections: List<Section>, currentIndex: Int, onSelect: (Int) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            Text(
                com.agpeya.app.ui.strings.LocalStrings.current.contents,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
        }
        items(sections.size) { index ->
            val section = sections[index]
            if (section.part != null && sections.getOrNull(index - 1)?.part != section.part) {
                Spacer(Modifier.height(Spacing.md))
                Text(section.part, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(Spacing.xs))
            }
            val isCurrent = index == currentIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .selectable(selected = isCurrent, onClick = { onSelect(index) })
                    .padding(vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium.inReadingFont(),
                    color = if (isCurrent) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item { Spacer(Modifier.height(Spacing.huge)) }
    }
}

/**
 * The divider between two parts of an hour. Deliberately the loudest thing in
 * the reader after the text itself — it is the only structural signal that a new
 * movement of the office has begun.
 */
@Composable
private fun PartHeader(part: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(Spacing.huge + Spacing.sm))
        Text(
            text = part,
            style = MaterialTheme.typography.titleLarge.inReadingFont(),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        HorizontalDivider(
            modifier = Modifier.width(56.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

// SectionView, VerseText, and HighlightBar live in SectionUi.kt — shared with
// the Psalter screen.

/** Step to the neighbouring prayer hour from the foot of the reader. */
@Composable
private fun HourStepper(onPrevious: (() -> Unit)?, onNext: (() -> Unit)?) {
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
            TextButton(onClick = onPrevious, colors = colors) {
                Text("‹  ${s.previousHour}", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Spacer(Modifier.width(1.dp))
        }
        if (onNext != null) {
            TextButton(onClick = onNext, colors = colors) {
                Text("${s.nextHour}  ›", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
