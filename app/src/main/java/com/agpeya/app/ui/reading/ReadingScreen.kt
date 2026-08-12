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
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
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
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.HighlightRepository
import com.agpeya.app.data.LayoutRepository
import com.agpeya.app.data.PrayerLayout
import com.agpeya.app.data.ReadingMode
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.model.Bookmark
import com.agpeya.app.model.Hour
import com.agpeya.app.model.HourLayout
import com.agpeya.app.model.Section
import com.agpeya.app.ui.theme.inReadingFont
import kotlinx.coroutines.launch

private val FONT_STEPS_SP = listOf(17, 19, 22, 25, 29)

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
    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())
    // Scope to this hour: the same section id can be bookmarked from elsewhere
    // (a psalm added into an hour shares its id with the Psalter's copy).
    val bookmarkedIds = remember(bookmarks, hourId) {
        bookmarks.filter { it.hourId == hourId }.map { it.sectionId }.toSet()
    }
    val layout by LayoutRepository.layout(context, hourId).collectAsState(initial = HourLayout())
    val highlights by HighlightRepository.highlights(context).collectAsState(initial = emptyMap())
    // Today's read-state for this hour, keyed by permanent section id.
    val readIds by com.agpeya.app.data.PrayerProgressRepository.doneIn(context, hourId)
        .collectAsState(initial = emptySet())

    var showContents by remember { mutableStateOf(false) }
    // Verse currently chosen for highlighting (shows the colour palette); null = none.
    var selectedVerseKey by remember { mutableStateOf<String?>(null) }
    // Apply the user's per-hour customization (show/hide, reorder, added psalms).
    val sections by produceState(emptyList<Section>(), hour, layout) {
        val h = hour
        value = if (h == null) emptyList()
        else {
            val extras = layout.added.mapNotNull { ContentRepository.psalm(context, it) }
            PrayerLayout.visible(h.sections, extras, layout)
        }
    }
    val listState = rememberLazyListState()
    val pagerState = rememberPagerState(pageCount = { sections.size })
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]
    val s = com.agpeya.app.ui.strings.LocalStrings.current

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
        com.agpeya.app.data.PrayerProgressRepository.recordOpened(context, h.id)
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

    fun toggleRead(section: Section) {
        scope.launch {
            com.agpeya.app.data.PrayerProgressRepository.toggleSection(context, hourId, section.id)
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
            TopAppBar(
                title = {
                    Box {
                        Row(
                            modifier = Modifier.clickable { hourMenu = true },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(hour?.name ?: "", style = MaterialTheme.typography.titleLarge)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = hourMenu, onDismissRequest = { hourMenu = false }) {
                            allHours.forEach { h ->
                                DropdownMenuItem(
                                    text = { Text(h.name, style = MaterialTheme.typography.titleMedium) },
                                    onClick = {
                                        hourMenu = false
                                        if (h.id != hourId) onSwitchHour(h.id)
                                    },
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                actions = {
                    FontSizeActions(
                        fontStep = fontStep,
                        maxStep = FONT_STEPS_SP.lastIndex,
                    ) { step -> scope.launch { SettingsRepository.setFontStep(context, step) } }
                    IconButton(onClick = {
                        // Capture the current position, then switch the mode. The
                        // anchor effect scrolls the newly-composed reader to it.
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
                    }) {
                        Icon(
                            imageVector = if (readingMode == ReadingMode.VERTICAL) Icons.Outlined.SwapHoriz
                            else Icons.Outlined.SwapVert,
                            contentDescription = s.readingModeToggle,
                        )
                    }
                    IconButton(onClick = { showContents = true }) {
                        Icon(Icons.Outlined.Menu, contentDescription = s.contents)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            // How far through the hour today's reading is. Sits under the app
            // bar so it stays visible while scrolling.
            if (sections.isNotEmpty()) {
                val doneCount = sections.count { it.id in readIds }
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { doneCount.toFloat() / sections.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = innerPadding.calculateTopPadding())
                        .height(2.dp)
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {},
                )
            }
            AnimatedVisibility(visible = sections.isNotEmpty(), enter = fadeIn(tween(350))) {
                val onVerseTap: (String) -> Unit = { selectedVerseKey = it }
                when (readingMode) {
                    ReadingMode.VERTICAL -> VerticalReader(
                        sections, listState, bodyFontSp, innerPadding, bookmarkedIds,
                        ::toggleBookmark, highlights, onVerseTap, readIds, ::toggleRead,
                        prevHour?.let { h -> { onSwitchHour(h.id) } },
                        nextHour?.let { h -> { onSwitchHour(h.id) } },
                    )
                    ReadingMode.HORIZONTAL -> PagedReader(
                        sections, pagerState, bodyFontSp, innerPadding, bookmarkedIds,
                        ::toggleBookmark, highlights, onVerseTap, readIds, ::toggleRead,
                    )
                }
            }
            HighlightBar(
                visible = selectedVerseKey != null,
                currentColor = selectedVerseKey?.let { highlights[it] },
                onPick = { colorKey ->
                    val key = selectedVerseKey
                    selectedVerseKey = null
                    if (key != null) scope.launch { HighlightRepository.setHighlight(context, key, colorKey) }
                },
                onDismiss = { selectedVerseKey = null },
                shareText = com.agpeya.app.ui.reading.verseShareText(sections, selectedVerseKey),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showContents) {
        ModalBottomSheet(onDismissRequest = { showContents = false }, sheetState = sheetState) {
            ContentsSheet(
                sections = sections,
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
    readIds: Set<String>,
    onToggleRead: (Section) -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
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
                isRead = section.id in readIds,
                onToggleRead = { onToggleRead(section) },
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
    readIds: Set<String>,
    onToggleRead: (Section) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            key = { sections[it].id },
        ) { page ->
            val section = sections[page]
            // LazyColumn (not Column+verticalScroll) so verse taps register inside
            // the pager — the scroll Column was consuming them.
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
            ) {
                item {
                    section.part?.let { part ->
                        Spacer(Modifier.height(12.dp))
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
                        isRead = section.id in readIds,
                        onToggleRead = { onToggleRead(section) },
                    )
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
        Text(
            text = "${geezNumeral(pagerState.currentPage + 1)} / ${geezNumeral(sections.size)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ContentsSheet(sections: List<Section>, onSelect: (Int) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            Text(
                com.agpeya.app.ui.strings.LocalStrings.current.contents,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
        items(sections.size) { index ->
            val section = sections[index]
            if (section.part != null && sections.getOrNull(index - 1)?.part != section.part) {
                Spacer(Modifier.height(12.dp))
                Text(section.part, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium.inReadingFont(),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
            )
        }
        item { Spacer(Modifier.height(36.dp)) }
    }
}

@Composable
private fun PartHeader(part: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(44.dp))
        Text(
            text = part,
            style = MaterialTheme.typography.titleLarge.inReadingFont(),
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(modifier = Modifier.width(56.dp), thickness = 2.dp, color = MaterialTheme.colorScheme.secondary)
    }
}

// SectionView, VerseText, and HighlightBar live in SectionUi.kt — shared with
// the Psalter screen.

/** Step to the neighbouring prayer hour from the foot of the reader. */
@Composable
private fun HourStepper(onPrevious: (() -> Unit)?, onNext: (() -> Unit)?) {
    if (onPrevious == null && onNext == null) return
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onPrevious != null) {
            TextButton(onClick = onPrevious) { Text("‹  ${s.previousHour}") }
        } else {
            Spacer(Modifier.width(1.dp))
        }
        if (onNext != null) {
            TextButton(onClick = onNext) { Text("${s.nextHour}  ›") }
        }
    }
}
