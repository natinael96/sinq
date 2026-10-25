package com.sinq.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinq.app.data.ScriptureRepository
import com.sinq.app.data.HighlightRepository
import com.sinq.app.data.SettingsRepository
import com.sinq.app.data.UserDataRepository
import com.sinq.app.model.Bookmark
import com.sinq.app.model.ScriptureBook
import com.sinq.app.ui.reading.geezNumeral
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.scaledReadingSp
import com.sinq.app.ui.theme.LocalReadingFont
import com.sinq.app.ui.common.LoadingPanel
import com.sinq.app.ui.common.SelectPill
import com.sinq.app.ui.common.SinqTopBar
import com.sinq.app.ui.common.StatePanel
import com.sinq.app.ui.reading.ReadingColumn
import com.sinq.app.ui.theme.IconSize
import com.sinq.app.ui.theme.Spacing
import com.sinq.app.ui.theme.readingBodyStyle
import com.sinq.app.ui.theme.readingVerseGap
import com.sinq.app.ui.theme.sinqColors
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import com.sinq.app.ui.theme.inReadingFont
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import com.sinq.app.ui.common.ListRow

private val FONT_STEPS_SP = com.sinq.app.data.SettingsRepository.FONT_STEPS_SP

enum class ScriptureTranslationMode {
    AMHARIC,
    GEEZ,
    ENGLISH,
    PARALLEL_AMH_GEEZ,
    PARALLEL_AMH_ENG;

    fun next(hasEnglish: Boolean): ScriptureTranslationMode = when (this) {
        AMHARIC -> GEEZ
        GEEZ -> if (hasEnglish) ENGLISH else PARALLEL_AMH_GEEZ
        ENGLISH -> PARALLEL_AMH_GEEZ
        PARALLEL_AMH_GEEZ -> if (hasEnglish) PARALLEL_AMH_ENG else AMHARIC
        PARALLEL_AMH_ENG -> AMHARIC
    }
}

/**
 * A New-Testament book reader. Opens at [initialChapter] (and scrolls to
 * [initialStart], tinting [initialStart]..[initialEnd]) when arrived at from a
 * ግጻዌ reading link; otherwise chapter 1. A chapter strip switches chapters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptureReaderScreen(
    bookKey: String,
    selectedPlanId: String = "",
    selectedPlanDay: Int = 0,
    initialChapter: Int = 0,
    initialStart: Int = -1,
    initialEnd: Int = -1,
    onBack: () -> Unit,
    /** Opens a journal entry anchored to the chapter, or to the selected run of verses. */
    onWriteNote: (route: String, label: String) -> Unit,
    /** Follows today's ንባብ into the next book when the day crosses one. */
    onOpenRoute: (route: String) -> Unit = {},
) {
    com.sinq.app.ui.common.ReaderAwake()
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val hasEnglish = remember(bookKey) { ScriptureRepository.hasEnglish(bookKey) }
    var translationMode by rememberSaveable(bookKey) { mutableStateOf(ScriptureTranslationMode.AMHARIC) }
    val effectiveMode = translationMode
    val activeEdition = when (effectiveMode) {
        ScriptureTranslationMode.GEEZ -> ScriptureRepository.PSALMS_GEEZ_EDITION
        ScriptureTranslationMode.ENGLISH -> ScriptureRepository.BIBLE_ENGLISH_EDITION
        else -> ScriptureRepository.BIBLE_EDITION
    }
    var loadAttempt by rememberSaveable(bookKey, activeEdition) { mutableIntStateOf(0) }
    val bookResult by produceState<Result<ScriptureBook?>?>(initialValue = null, bookKey, activeEdition, loadAttempt) {
        value = runCatching { ScriptureRepository.book(context, bookKey, activeEdition) }
    }
    val parallelBookResult by produceState<ScriptureBook?>(initialValue = null, bookKey, effectiveMode) {
        value = when (effectiveMode) {
            ScriptureTranslationMode.PARALLEL_AMH_ENG -> {
                if (hasEnglish) ScriptureRepository.book(context, bookKey, ScriptureRepository.BIBLE_ENGLISH_EDITION) else null
            }
            ScriptureTranslationMode.PARALLEL_AMH_GEEZ -> {
                ScriptureRepository.book(context, bookKey, ScriptureRepository.PSALMS_GEEZ_EDITION)
            }
            else -> null
        }
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
    val loadedPlanContent by androidx.compose.runtime.produceState<com.sinq.app.model.ReadingPlanContent?>(null) {
        value = com.sinq.app.data.ReadingPlanRepository.content(context)
    }
    val loadedPlanState by com.sinq.app.data.ReadingPlanRepository.state(context)
        .collectAsState(initial = null)
    val planContent = loadedPlanContent ?: com.sinq.app.model.ReadingPlanContent()
    val planState = loadedPlanState ?: com.sinq.app.model.ReadingPlanState()
    val today = com.sinq.app.ui.common.rememberCurrentDate().value
    // Every plan being kept, with the day it is on. A psalm can belong to
    // today in two plans at once — the year reaches it, and የዳዊት ንባብ is on it —
    // and each wants its own tick.
    val planDays = remember(planContent, planState, today, selectedPlanId, selectedPlanDay) {
        com.sinq.app.ui.reading.readerPlanDays(planContent, planState, today, selectedPlanId, selectedPlanDay)
    }
    var chaptersOpen by remember { mutableStateOf(false) }
    var chapter by rememberSaveable(bookKey) {
        mutableIntStateOf(initialChapter.coerceIn(1, b.chapters.size))
    }
    // Live verse selection (tap anchors, next tap moves the end); -1 = none.
    var selA by rememberSaveable(bookKey, chapter) { mutableIntStateOf(-1) }
    var selB by rememberSaveable(bookKey, chapter) { mutableIntStateOf(-1) }
    val current = remember(b, chapter) { b.chapters.find { it.chapter == chapter } ?: b.chapters.first() }
    val parallelChapter = remember(parallelBookResult, chapter) {
        parallelBookResult?.chapters?.find { it.chapter == chapter }
    }
    val parallelVersesMap = remember(parallelChapter) {
        parallelChapter?.verses?.associateBy { it.n } ?: emptyMap()
    }
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
    val bookNames by androidx.compose.runtime.produceState(emptyMap<String, String>()) {
        value = runCatching { ScriptureRepository.bookNames(context) }.getOrDefault(emptyMap())
    }
    val listState = rememberLazyListState()
    val progressKey = "bible:$bookKey:$chapter:$today"
    val readingWeights = remember(rows) { rows.map { row -> row.sumOf { it.text.length } } }
    val recordRead = com.sinq.app.ui.common.rememberHalfwayRead(progressKey) {
        val matchingPlans = planDays.filter { (_, day) ->
            day.r.any { it.b == bookKey && chapter in it.chapters }
        }.map { it.first.id }
        com.sinq.app.data.ReadingPlanRepository.markChapter(context, bookKey, chapter, matchingPlans, today)
    }
    com.sinq.app.ui.common.ObserveReadingProgress(listState, progressKey, readingWeights,
        enabled = loadedPlanContent != null && loadedPlanState != null, onProgress = recordRead)
    val verseGap = readingVerseGap(bodyFontSp)
    val selRange = com.sinq.app.ui.reading.flatSelectionRange(selA, selB)
    val sinq = sinqColors
    val isPsalms = bookKey == "psalms" || b.key == "psalms"
    val displayBookName = if (effectiveMode == ScriptureTranslationMode.ENGLISH) b.nameEn else b.nameAm
    val chapterUnitLabel = if (effectiveMode == ScriptureTranslationMode.ENGLISH) {
        if (isPsalms) "Psalm" else "Chapter"
    } else {
        if (isPsalms) (if (s.isAmharic) "መዝሙር" else "Psalm") else s.chapterUnit
    }
    val chapterNumStr = if (effectiveMode == ScriptureTranslationMode.ENGLISH) "$chapter" else geezNumeral(chapter)
    val chapterTitle = "$displayBookName $chapterUnitLabel $chapterNumStr"
    // The selection as the Church names it: the book's Amharic name, the
    // chapter and the verses, in Ge'ez numerals — the same string the copy, the
    // share, the image card and the bookmark all carry.
    val selPassage = if (selRange.isEmpty()) null else {
        val verses = current.verses.filter { it.n in selRange }
        if (verses.isEmpty()) null else com.sinq.app.ui.common.Passage(
            verses = verses.map { it.n as Int? to it.text },
            citation = com.sinq.app.data.Citation.of(displayBookName, chapter, selRange.first, selRange.last),
            edition = if (effectiveMode == ScriptureTranslationMode.ENGLISH) "NKJV" else s.amharicEdition,
        )
    }
    val selRoute = "scripture/$bookKey/$chapter" +
        if (selRange.isEmpty()) "" else "?start=${selRange.first}&end=${selRange.last}"

    // Land on the cited verse when opened from a reading link — once. Without the
    // guard, paging away and back to this chapter re-ran the jump mid-reading.
    var landed by rememberSaveable(bookKey) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(current, highlightRange, rows) {
        if (!landed && !highlightRange.isEmpty()) {
            val idx = rows.indexOfFirst { row -> row.any { it.n >= highlightRange.first } }
            if (idx >= 0) listState.scrollToItem(idx + 1)
            landed = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = displayBookName,
                onBack = onBack,
                titleContent = {
                    com.sinq.app.ui.common.ReaderTitleBar(
                        title = displayBookName,
                        chapterLabel = "$chapterUnitLabel $chapterNumStr",
                        pickable = b.chapters.size > 1,
                        onPick = { chaptersOpen = true },
                    )
                },
                actions = {
                    val gold = MaterialTheme.colorScheme.secondary
                    var langMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(gold.copy(alpha = 0.12f))
                                .border(1.dp, gold.copy(alpha = 0.40f), CircleShape)
                                .clickable(
                                    onClickLabel = "የትርጉም ምርጫ",
                                    onClick = { langMenuOpen = true },
                                )
                                .heightIn(min = 36.dp)
                                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            val editionText = when (effectiveMode) {
                                ScriptureTranslationMode.AMHARIC -> s.wudaseLangAmharic
                                ScriptureTranslationMode.GEEZ -> s.wudaseLangGeez
                                ScriptureTranslationMode.ENGLISH -> "English"
                                ScriptureTranslationMode.PARALLEL_AMH_GEEZ -> "አማ + ግእዝ"
                                ScriptureTranslationMode.PARALLEL_AMH_ENG -> "አማ + Eng"
                            }
                            Text(
                                text = editionText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = gold,
                                maxLines = 1,
                            )
                            Icon(
                                Icons.Outlined.ArrowDropDown,
                                contentDescription = null,
                                tint = gold,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        DropdownMenu(
                            expanded = langMenuOpen,
                            onDismissRequest = { langMenuOpen = false },
                        ) {
                            val modes = if (hasEnglish) {
                                listOf(
                                    ScriptureTranslationMode.AMHARIC to s.wudaseLangAmharic,
                                    ScriptureTranslationMode.GEEZ to s.wudaseLangGeez,
                                    ScriptureTranslationMode.ENGLISH to "English",
                                    ScriptureTranslationMode.PARALLEL_AMH_GEEZ to "${s.wudaseLangAmharic} + ${s.wudaseLangGeez}",
                                    ScriptureTranslationMode.PARALLEL_AMH_ENG to "${s.wudaseLangAmharic} + English",
                                )
                            } else {
                                listOf(
                                    ScriptureTranslationMode.AMHARIC to s.wudaseLangAmharic,
                                    ScriptureTranslationMode.GEEZ to s.wudaseLangGeez,
                                    ScriptureTranslationMode.PARALLEL_AMH_GEEZ to "${s.wudaseLangAmharic} + ${s.wudaseLangGeez}",
                                )
                            }

                            modes.forEach { (mode, label) ->
                                val isSelected = effectiveMode == mode
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            ),
                                            color = if (isSelected) gold else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = gold,
                                                modifier = Modifier.size(IconSize.small),
                                            )
                                        }
                                    } else null,
                                    onClick = {
                                        translationMode = mode
                                        langMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(Spacing.xs))
                    com.sinq.app.ui.common.ReaderToolsMenu(
                        fontStep = fontStep,
                        maxFontStep = FONT_STEPS_SP.lastIndex,
                        onFontChange = { step -> scope.launch { SettingsRepository.setFontStep(context, step) } },
                        onWriteNote = {
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
                            com.sinq.app.ui.common.SharePayload(
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
            com.sinq.app.ui.reading.SelectionBar(
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
                        .flatMap { com.sinq.app.data.CrossReference.parse(it.refs.orEmpty(), bookNames) }
                        .distinctBy { it.route }
                },
                onOpenRef = { route -> selA = -1; selB = -1; onOpenRoute(route) },
                commentaryUrl = selRange.firstOrNull()?.let {
                    com.sinq.app.data.CatenaLink.url(bookKey, chapter, it)
                },
                onBookmark = selPassage?.let { passage ->
                    {
                        scope.launch {
                            UserDataRepository.toggleBookmark(
                                context,
                                com.sinq.app.model.Bookmark(
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
        Box(Modifier.fillMaxSize()) {
            ReadingColumn(state = listState, innerPadding = innerPadding) {
                items(rows, key = { it.first().n }) { row ->
                    val tinted = row.first().n in highlightRange
                    val body = @Composable { verse: com.sinq.app.model.ScriptureVerse ->
                        val isEnglishMode = effectiveMode == ScriptureTranslationMode.ENGLISH
                        val marker = if (isEnglishMode) "${verse.n}" else geezNumeral(verse.n)
                        val annotated = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = scaledReadingSp(bodyFontSp) * 0.58f,
                                    baselineShift = BaselineShift.Superscript,
                                )
                            ) { append(marker) }
                            append("  ")
                            append(verse.text)
                        }
                        val secondaryVerseText = if (effectiveMode == ScriptureTranslationMode.PARALLEL_AMH_ENG || effectiveMode == ScriptureTranslationMode.PARALLEL_AMH_GEEZ) {
                            parallelVersesMap[verse.n]?.text
                        } else null

                        Column(
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
                                .heightIn(min = 48.dp)
                                .clickable {
                                    val (a, bSel) = com.sinq.app.ui.reading.advanceFlatSelection(selA, verse.n)
                                    selA = a
                                    selB = bSel
                                }
                                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        ) {
                            Text(
                                text = annotated,
                                style = readingBodyStyle(bodyFontSp),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            if (secondaryVerseText != null) {
                                Spacer(Modifier.height(Spacing.xs))
                                Text(
                                    text = secondaryVerseText,
                                    style = readingBodyStyle(bodyFontSp).copy(
                                        fontSize = readingBodyStyle(bodyFontSp).fontSize * 0.94f,
                                        color = MaterialTheme.colorScheme.secondary,
                                    ),
                                    modifier = Modifier.padding(start = Spacing.md),
                                )
                            }
                        }
                    }

                    val verseBlock = @Composable { verse: com.sinq.app.model.ScriptureVerse ->
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
                    planDays.forEach { (plan, day) ->
                        val dayChapters = day.r.flatMap { r -> r.chapters.map { r.b to it } }
                        val here = dayChapters.indexOf(bookKey to chapter)
                        if (here < 0) return@forEach
                        val chapterKey = com.sinq.app.data.ReadingPlanRepository.chapterKey(bookKey, chapter)
                        val read = chapterKey in planState.readFor(plan.id)
                        ListRow(
                            title = plan.title,
                            subtitle = "${if (read) s.readingDone + " · " else ""}${s.readingDayLabel(com.sinq.app.ui.reading.geezNumeral(day.d))} · ${s.readingChapterOfDay(here + 1, dayChapters.size)}",
                            leadingIcon = if (read) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.MenuBook,
                            leadingTint = if (read) MaterialTheme.colorScheme.secondary else null,
                        )
                        dayChapters.getOrNull(here + 1)?.takeUnless { it.first == bookKey && it.second == chapter + 1 }?.let { (nextBook, nextChapter) ->
                            if (nextBook == bookKey) {
                                com.sinq.app.ui.common.ChapterStepper(
                                    previousLabel = null,
                                    nextLabel = "${s.chapterUnit} ${com.sinq.app.ui.reading.geezNumeral(nextChapter)}",
                                    onPrevious = null,
                                    onNext = { chapter = nextChapter; scope.launch { listState.scrollToItem(0) } },
                                )
                            } else {
                                ListRow(
                                    title = s.nextChapter,
                                    subtitle = bookNames[nextBook] ?: nextBook,
                                    onClick = {
                                        onOpenRoute(
                                            com.sinq.app.ui.reading.planReadingRoute(nextBook, nextChapter, selectedPlanId.takeIf { it.isNotBlank() }, day.d),
                                        )
                                    },
                                )
                            }
                        }
                    }
                    com.sinq.app.ui.common.ChapterStepper(
                        previousLabel = "$chapterUnitLabel ${com.sinq.app.ui.reading.geezNumeral(chapter - 1)}",
                        nextLabel = "$chapterUnitLabel ${com.sinq.app.ui.reading.geezNumeral(chapter + 1)}",
                        onPrevious = { chapter -= 1; scope.launch { listState.scrollToItem(0) }; Unit }.takeIf { chapter > 1 },
                        onNext = { chapter += 1; scope.launch { listState.scrollToItem(0) }; Unit }.takeIf { chapter < b.chapters.size },
                    )
                    Spacer(Modifier.height(Spacing.huge))
                }
            }
            if (rows.size > 1) {
                com.sinq.app.ui.common.ReadingProgressBar(
                    progress = com.sinq.app.ui.common.rememberLazyListProgress(listState),
                    topPadding = innerPadding.calculateTopPadding(),
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
        if (chaptersOpen) {
            com.sinq.app.ui.common.ChapterSheet(
                count = b.chapters.size,
                current = chapter - 1,
                onPick = { chaptersOpen = false; chapter = it + 1; scope.launch { listState.scrollToItem(0) }; Unit },
                onDismiss = { chaptersOpen = false },
                labelFor = { i -> if (isPsalms) "$chapterUnitLabel ${com.sinq.app.ui.reading.geezNumeral(i + 1)}" else null },
            )
        }
    }
}
