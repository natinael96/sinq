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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
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
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.model.Bookmark
import com.agpeya.app.model.Section
import com.agpeya.app.search.AmharicSearch
import com.agpeya.app.ui.reading.HighlightBar
import com.agpeya.app.ui.reading.SectionView
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Abyssinica
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/** Pseudo hour id psalter bookmarks are stored under — never a real hour id. */
const val PSALTER_BOOKMARK_ID = "psalter"

private val FONT_STEPS_SP = listOf(17, 19, 22, 25, 29)

/** Traditional weekday division of the Psalter; Sunday is not yet defined. */
private fun dailyRange(day: DayOfWeek): IntRange? = when (day) {
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
fun PsalterScreen(initialPsalmIndex: Int = -1, onBack: () -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val psalms by produceState(emptyList<Section>()) { value = ContentRepository.psalter(context) }
    val fontStep by SettingsRepository.fontStep(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val readingMode by SettingsRepository.readingMode(context)
        .collectAsState(initial = ReadingMode.VERTICAL)
    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())
    val bookmarkedIds = remember(bookmarks) { bookmarks.map { it.sectionId }.toSet() }
    val highlights by HighlightRepository.highlights(context).collectAsState(initial = emptyMap())
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]

    // Opened from a bookmark: show the whole psalter so the target psalm exists.
    var daily by remember { mutableStateOf(initialPsalmIndex < 0) }
    val today = remember { LocalDate.now() }
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
    var selectedVerseKey by remember { mutableStateOf<String?>(null) }
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
            TopAppBar(
                title = { Text(s.psalterTitle, style = MaterialTheme.typography.titleLarge, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                actions = {
                    // Today's / All — compact toggle, same row as everything else.
                    TextButton(onClick = {
                        daily = !daily
                        anchor = -1
                    }) {
                        Text(
                            if (daily) s.dailyPsalms else s.wholePsalter,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    TextButton(
                        onClick = { scope.launch { SettingsRepository.setFontStep(context, fontStep - 1) } },
                        enabled = fontStep > 0,
                    ) { Text("A−") }
                    TextButton(
                        onClick = { scope.launch { SettingsRepository.setFontStep(context, fontStep + 1) } },
                        enabled = fontStep < FONT_STEPS_SP.lastIndex,
                    ) { Text("A+") }
                    IconButton(onClick = {
                        // Capture the current position, then switch the mode. The
                        // anchor effect scrolls the newly-composed reader to it.
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            val onVerseTap: (String) -> Unit = { selectedVerseKey = it }
            if (daily && range == null) {
                // Sunday's division isn't defined yet.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        s.comingSoon,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = Abyssinica),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else when (readingMode) {
                ReadingMode.VERTICAL -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    ) {
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
                            )
                        }
                        item { Spacer(Modifier.height(56.dp)) }
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
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                            ) {
                                item {
                                    SectionView(
                                        section = section,
                                        bodyFontSp = bodyFontSp,
                                        isBookmarked = section.id in bookmarkedIds,
                                        onToggleBookmark = { toggleBookmark(section) },
                                        highlights = highlights,
                                        onVerseTap = onVerseTap,
                                    )
                                    Spacer(Modifier.height(48.dp))
                                }
                            }
                        }
                        if (shown.isNotEmpty()) {
                            Text(
                                text = "${geezNumeral(pagerState.currentPage + 1)} / ${geezNumeral(shown.size)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
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
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showContents) {
        ModalBottomSheet(onDismissRequest = { showContents = false }, sheetState = sheetState) {
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
                    Text(p.title, style = MaterialTheme.typography.titleMedium.copy(fontFamily = Abyssinica), color = MaterialTheme.colorScheme.onSurface)
                    p.subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
            item { Spacer(Modifier.height(36.dp)) }
        }
    }
}
