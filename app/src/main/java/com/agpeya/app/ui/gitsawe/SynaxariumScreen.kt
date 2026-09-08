package com.agpeya.app.ui.gitsawe

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.SynaxariumRepository
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.model.Bookmark
import com.agpeya.app.model.SynaxariumEntry
import com.agpeya.app.ui.common.EthiopianDate
import com.agpeya.app.ui.common.formatEthiopianWithGregorian
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.ReadingMaxWidth
import com.agpeya.app.ui.theme.sinqColors
import com.agpeya.app.ui.theme.scaledReadingSp
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.readingBodyStyle
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.runtime.LaunchedEffect
import com.agpeya.app.data.HabitsRepository
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Menu

private val FONT_STEPS_SP = SettingsRepository.FONT_STEPS_SP

// The warm liturgical አርኬ red lives in SinqColors.arke, tuned per theme so it
// keeps its contrast on the ivory ground as well as the dark green one.

/** The hymn is verse, not prose: a little more air than the running text. */
private const val ArkeLineHeight = 1.78f

/** The fixed closing ጸሎት is a coda — noticeably smaller and tightly set. */
private const val CLOSING_FONT_SCALE = 0.78f
private const val CLOSING_LINE_HEIGHT = 1.28f

/** ስንክሳር — the day's synaxarium commemorations for [epochDay]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynaxariumScreen(epochDay: Long, initialEntry: Int = -1, onBack: () -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    // The day is state, not a fixed argument: ግጻዌ can step through the year and
    // so should this. Arriving on a day and wanting the one before it meant
    // going back, changing the day there, and opening ስንክሳር again.
    var epochDay by rememberSaveable(epochDay) { androidx.compose.runtime.mutableLongStateOf(epochDay) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showContents by rememberSaveable { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val date = remember(epochDay) { LocalDate.ofEpochDay(epochDay) }
    val eth = remember(date) { EthiopianDate.from(date) }
    var loadAttempt by rememberSaveable(epochDay) { mutableIntStateOf(0) }
    val entriesResult by produceState<Result<List<SynaxariumEntry>>?>(initialValue = null, epochDay, loadAttempt) {
        value = runCatching { SynaxariumRepository.forDate(context, date) }
    }
    val fontStep by SettingsRepository.fontStep(context).collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]

    // Live paragraph selection across the day's entries: a tap anchors, the
    // next tap moves the end. Keys are entryIndex*1000 + paragraphIndex, so a
    // run can span entries while staying in reading order. -1 = none.
    var selA by rememberSaveable(epochDay) { androidx.compose.runtime.mutableIntStateOf(-1) }
    var selB by rememberSaveable(epochDay) { androidx.compose.runtime.mutableIntStateOf(-1) }
    val selRange = com.agpeya.app.ui.reading.flatSelectionRange(selA, selB)

    // A search result knows which entry it matched — the index has stored the
    // ordinal all along and the route threw it away, so a hit on a twelve-entry
    // day landed at the top.
    var landed by rememberSaveable(epochDay) { mutableStateOf(false) }
    LaunchedEffect(entriesResult, initialEntry) {
        if (landed || initialEntry < 0) return@LaunchedEffect
        val count = entriesResult?.getOrNull()?.size ?: return@LaunchedEffect
        if (initialEntry < count) listState.scrollToItem(initialEntry)
        landed = true
    }

    fun tapAt(flatKey: Int) {
        val (a, b) = com.agpeya.app.ui.reading.advanceFlatSelection(selA, flatKey)
        selA = a
        selB = b
    }

    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())
    val bookmarkedIds = remember(bookmarks) {
        bookmarks.filter { it.hourId == "sinksar_verse" }.mapTo(HashSet()) { it.sectionId }
    }

    // Reading today's ስንክሳር marks it kept. Only today's: browsing back through
    // the year is reading about a day, not keeping it, and a day already past
    // cannot be kept now.
    LaunchedEffect(date, entriesResult) {
        if (date != LocalDate.now()) return@LaunchedEffect
        if (entriesResult?.getOrNull().isNullOrEmpty()) return@LaunchedEffect
        HabitsRepository.markDone(context, date.toString(), "sinksar")
    }

    fun mark(sectionId: String, title: String, rawText: String) {
        scope.launch {
            UserDataRepository.toggleBookmark(
                context,
                Bookmark(
                    hourId = "sinksar_verse",
                    hourName = s.bookmarkGroupSynaxarium,
                    sectionId = sectionId,
                    title = title,
                    subtitle = snippet(rawText),
                    route = "synaxarium/$epochDay",
                ),
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.synaxariumTitle,
                subtitle = formatEthiopianWithGregorian(date, s),
                onBack = onBack,
                actions = {
                    val list = entriesResult?.getOrNull().orEmpty()
                    IconButton(onClick = { epochDay -= 1 }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = s.previousDay,
                            modifier = Modifier.size(com.agpeya.app.ui.theme.IconSize.medium),
                        )
                    }
                    IconButton(onClick = { epochDay += 1 }) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = s.nextDay,
                            modifier = Modifier.size(com.agpeya.app.ui.theme.IconSize.medium),
                        )
                    }
                    IconButton(onClick = { showPicker = true }) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = s.gitsaweChangeDay,
                            modifier = Modifier.size(com.agpeya.app.ui.theme.IconSize.medium),
                        )
                    }
                    if (list.size > 2) {
                        IconButton(onClick = { showContents = true }) {
                            Icon(
                                Icons.Outlined.Menu,
                                contentDescription = s.contents,
                                modifier = Modifier.size(com.agpeya.app.ui.theme.IconSize.medium),
                            )
                        }
                    }
                    com.agpeya.app.ui.common.ReaderToolsMenu(
                        fontStep = fontStep,
                        maxFontStep = FONT_STEPS_SP.lastIndex,
                        onFontChange = { step -> scope.launch { SettingsRepository.setFontStep(context, step) } },
                        shareEnabled = list.isNotEmpty(),
                        sharePayload = {
                            if (list.isEmpty()) null
                            else com.agpeya.app.ui.common.SharePayload(
                                body = synaxariumShareBody(list),
                                kicker = s.synaxariumTitle,
                                title = list.firstNotNullOfOrNull {
                                    cleanSynaxariumText(it.title).takeIf { t -> t.isNotBlank() }
                                },
                                dateLabel = com.agpeya.app.ui.common.formatEthiopian(date, s),
                            )
                        },
                    )
                },
            )
        },
    ) { innerPadding ->
        val list = entriesResult?.getOrNull()
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when {
            entriesResult == null -> LoadingPanel(Modifier.padding(innerPadding))

            entriesResult?.isFailure == true -> StatePanel(
                title = s.contentUnavailable,
                body = s.contentMissingBody,
                actionLabel = s.retryAction,
                onAction = { loadAttempt++ },
                modifier = Modifier.padding(innerPadding),
            )

            // No empty state. Every one of the 366 days carries entries, so
            // "ለዛሬ የተመዘገበ ስንክሳር የለም" could only ever appear on a date the book
            // does not reach — and telling a reader the day is empty is worse
            // than the page simply being short.
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(innerPadding).widthIn(max = ReadingMaxWidth),
                contentPadding = PaddingValues(horizontal = Spacing.screen),
            ) {
                itemsIndexed(list.orEmpty()) { i, entry ->
                    val title = cleanSynaxariumText(entry.title)
                    val paras = remember(entry) { parseSynaxarium(entry.text) }
                    // An entry with no text is a heading over nothing. 59 of the
                    // 2,308 are; they are not drawn.
                    if (paras.isEmpty() && title.isBlank()) return@itemsIndexed
                    Column(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.height(Spacing.md))
                        when (synaxariumEntryKind(entry.title)) {
                            // The day's opening doxology. Its title names the
                            // month, which the header above already says, so
                            // only the words are kept — set quieter, because
                            // they open the day rather than being it.
                            SynaxariumEntryKind.OPENING -> paras.forEachIndexed { p, para ->
                                OpeningPara(
                                    text = para.text,
                                    fontSp = bodyFontSp,
                                    selected = (i * 1000 + p) in selRange,
                                    onTap = { tapAt(i * 1000 + p) },
                                )
                            }

                            // A list of names. It draws its own marks, so the
                            // source's printed numbers come off.
                            SynaxariumEntryKind.FEAST_LIST -> {
                                if (title.isNotBlank()) EntryTitle(title)
                                FeastList(
                                    items = remember(entry) { synaxariumListItems(entry.text) },
                                    fontSp = bodyFontSp,
                                )
                            }

                            SynaxariumEntryKind.SCRIPTURE -> {
                                if (title.isNotBlank()) EntryTitle(title)
                                // Keyed by the Ethiopian date, not the Gregorian
                                // one: the same commemoration recurs every year
                                // and should stay bookmarked across years.
                                val sectionId = "sinksar:${eth.month}-${eth.day}:$i"
                                ScriptureBody(
                                    rawText = entry.text,
                                    fontSp = bodyFontSp,
                                    bookmarked = sectionId in bookmarkedIds,
                                    onToggleBookmark = {
                                        mark(sectionId, title.ifBlank { s.synaxariumTitle }, entry.text)
                                    },
                                )
                            }

                            // A life. Its title is the sentence the account
                            // opens with, so it opens the account: the first
                            // paragraph carries it, and the bookmark sits beside
                            // it rather than only on the scripture entries.
                            SynaxariumEntryKind.COMMEMORATION -> {
                                val sectionId = "sinksar:${eth.month}-${eth.day}:$i"
                                var n = 0
                                paras.forEachIndexed { p, para ->
                                    val flatKey = i * 1000 + p
                                    when (para.kind) {
                                        SynaxariumParaKind.NARRATIVE -> {
                                            n = para.sourceNumber ?: (n + 1)
                                            NarrativePara(
                                                number = n,
                                                text = if (p == 0 && title.isNotBlank()) "$title  ${para.text}" else para.text,
                                                fontSp = bodyFontSp,
                                                selected = flatKey in selRange,
                                                onTap = { tapAt(flatKey) },
                                                bookmarked = if (p == 0) sectionId in bookmarkedIds else null,
                                                onToggleBookmark = {
                                                    mark(sectionId, title.ifBlank { s.synaxariumTitle }, entry.text)
                                                },
                                            )
                                        }
                                        SynaxariumParaKind.ARKE_LABEL -> ArkeLabel(para.text)
                                        SynaxariumParaKind.ARKE_VERSE ->
                                            ArkeVerse(para.text, bodyFontSp, selected = flatKey in selRange) { tapAt(flatKey) }
                                    }
                                }
                                // A commemoration ends where the next begins,
                                // whether or not it closed with an አርኬ — only
                                // 817 of the 2,308 entries carry one.
                                Spacer(Modifier.height(Spacing.xl))
                            }
                        }
                    }
                }
                item { ClosingPrayer(bodyFontSp) }
                item { Spacer(Modifier.height(Spacing.huge)) }
            }
        }
        // The selected run, ready to copy or leave as text or a PNG card.
        val selBody = if (selRange.isEmpty()) null else list.orEmpty()
            .flatMapIndexed { i, entry ->
                if (isScriptureEntry(entry.title)) emptyList()
                else parseSynaxarium(entry.text).mapIndexedNotNull { p, para ->
                    para.text.takeIf { (i * 1000 + p) in selRange }
                }
            }
            .joinToString("\n\n")
            .ifBlank { null }
        if (showPicker) {
            com.agpeya.app.ui.common.EthiopianDatePickerDialog(
                initial = date,
                onDismiss = { showPicker = false },
                onSelect = {
                    epochDay = it.toEpochDay()
                    showPicker = false
                },
            )
        }
        if (showContents) {
            val list = entriesResult?.getOrNull().orEmpty()
            androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showContents = false }) {
                SynaxariumContents(
                    entries = list,
                    onSelect = { index ->
                        showContents = false
                        scope.launch { listState.scrollToItem(index) }
                    },
                )
            }
        }
        com.agpeya.app.ui.reading.SelectionBar(
            visible = selA >= 0,
            onDismiss = { selA = -1; selB = -1 },
            // A paragraph reader: the day is the citation.
            passage = selBody?.let {
                com.agpeya.app.ui.common.Passage(
                    verses = listOf(null to it),
                    citation = "${s.synaxariumTitle}  ·  ${com.agpeya.app.ui.common.formatEthiopian(date, s)}",
                )
            },
            imageKicker = s.synaxariumTitle,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        }
    }
}

/**
 * The fixed closing ጸሎት, appended once after the day's commemorations and set
 * apart by a divider. Holy names are drawn in red.
 *
 * Tapping it switches between the Ge'ez verses and their Amharic rendering. The
 * choice is remembered while the screen lives but deliberately not persisted —
 * it is a reading aid for the moment, not a setting. Stanzas without an Amharic
 * counterpart stay in Ge'ez rather than vanishing.
 */
@Composable
private fun ClosingPrayer(fontSp: Int) {
    val s = LocalStrings.current
    var showAmharic by rememberSaveable { mutableStateOf(false) }
    // The same fixed prayer closes every day, so it reads as a coda rather than
    // content: smaller than the body and tightly leaded, to keep it from
    // claiming a screenful at the end of each ስንክሳር.
    val style = readingBodyStyle(fontSp, CLOSING_LINE_HEIGHT).let {
        it.copy(fontSize = it.fontSize * CLOSING_FONT_SCALE, lineHeight = it.lineHeight * CLOSING_FONT_SCALE)
    }
    Spacer(Modifier.height(Spacing.xl))
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
    )
    val language = if (showAmharic) s.closingPrayerAmharic else s.closingPrayerGeez
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                onClickLabel = s.closingPrayerSwitchHint,
            ) { showAmharic = !showAmharic }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = language,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
        SYNAXARIUM_CLOSING_STANZAS.forEach { stanza ->
            // Fall back to the Ge'ez when a stanza has no Amharic rendering.
            val verse = if (showAmharic) stanza.amharic ?: stanza.geez else stanza.geez
            Text(
                text = highlightHolyNames(verse, sinqColors.arke),
                style = style,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = SYNAXARIUM_CLOSING_CODA,
            style = style,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Colour every occurrence of a holy name in [text] with [color]. */
private fun highlightHolyNames(text: String, color: Color): AnnotatedString =
    buildAnnotatedString {
        append(text)
        for (name in CLOSING_HOLY_NAMES) {
            var idx = text.indexOf(name)
            while (idx >= 0) {
                addStyle(SpanStyle(color = color), idx, idx + name.length)
                idx = text.indexOf(name, idx + name.length)
            }
        }
    }

/**
 * The label over a list or a quotation.
 *
 * Not gold and not centered. A centred gold line reads as a title, and none of
 * these are titles: "ወርኀዊ በዓላት" labels the list under it and "ሉቃ ፬፥፲፯" is a
 * citation. Setting them like headings made the day look like a stack of
 * separate documents instead of a page of one book.
 */
@Composable
private fun EntryTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.inReadingFont(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
    )
}

/** A numbered narrative paragraph: inline gold Ge'ez numeral, justified prose
 *  spanning the full width so the text block stays centered on the page.
 *  Selectable — nothing here competes with a long-press. */
@Composable
private fun NarrativePara(
    number: Int,
    text: String,
    fontSp: Int,
    selected: Boolean = false,
    onTap: (() -> Unit)? = null,
    /** Null on every paragraph but a commemoration's first, which carries it. */
    bookmarked: Boolean? = null,
    onToggleBookmark: () -> Unit = {},
) {
    val s = LocalStrings.current
    Box(Modifier.fillMaxWidth()) {
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = scaledReadingSp(fontSp) * 0.85f,
                        ),
                    ) { append(geezNumeral(number)) }
                    append("  ")
                    append(text)
                },
                style = readingBodyStyle(fontSp),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .paragraphSelection(selected, onTap)
                    .padding(bottom = 18.dp, end = if (bookmarked != null) 40.dp else 0.dp),
            )
        }
        if (bookmarked != null) {
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (bookmarked) s.removeAction else s.bookmarkAction,
                    tint = if (bookmarked) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(com.agpeya.app.ui.theme.IconSize.small),
                )
            }
        }
    }
}

/**
 * The day's opening doxology.
 *
 * The same body text as everything else. It was set smaller and muted to keep
 * it from competing with the first commemoration, which was the wrong tool: it
 * is the same book in the same voice. Carrying no number is enough to mark it
 * as the day's preface, and it leaves the አርኬ as the only thing on the page
 * that is set apart.
 */
@Composable
private fun OpeningPara(text: String, fontSp: Int, selected: Boolean, onTap: () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        Text(
            text = text,
            style = readingBodyStyle(fontSp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .paragraphSelection(selected, onTap)
                .padding(bottom = 16.dp),
        )
    }
}

/** A 📌 list of feast names, set as a list. */
@Composable
private fun FeastList(items: List<String>, fontSp: Int) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
            items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(
                        "·",
                        style = readingBodyStyle(fontSp),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                    Text(
                        item,
                        style = readingBodyStyle(fontSp),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

/** Tap-to-select support shared by the paragraph kinds: the run tint and the
 *  anchor/extend tap, layered under the padding so the tint hugs the text. */
private fun Modifier.paragraphSelection(selected: Boolean, onTap: (() -> Unit)?): Modifier =
    composed {
        val tinted = if (selected)
            this
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f))
        else this
        (if (onTap != null) tinted.clickable(onClick = onTap) else tinted)
            .semantics { this.selected = selected }
    }

/** The centered "አርኬ" heading above the hymn. */
@Composable
private fun ArkeLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.inReadingFont(),
        // The rubric colour, not the accent. Ethiopic manuscripts mark a
        // section title and an opening line in cinnabar, and every printed
        // liturgy sets the sung text in black and the rubric in red. Taking it
        // off was over-reach when the entry colouring came off.
        color = sinqColors.arke,
        textAlign = TextAlign.Center,
        letterSpacing = 6.sp,
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl, bottom = 10.dp),
    )
}

/** An arke verse — italic, centered, red, and selectable like the prose. */
@Composable
private fun ArkeVerse(
    text: String,
    fontSp: Int,
    selected: Boolean = false,
    onTap: (() -> Unit)? = null,
) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        Text(
            text = text,
            style = readingBodyStyle(fontSp, ArkeLineHeight).copy(fontStyle = FontStyle.Italic),
            color = sinqColors.arke,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .paragraphSelection(selected, onTap)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** A scripture-quote entry: the passage in a card, with a bookmark toggle. */
@Composable
private fun ScriptureBody(
    rawText: String,
    fontSp: Int,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
) {
    val s = LocalStrings.current
    // A few scripture entries carry an embedded አርኬ hymn after the quotation —
    // the quote goes in the card, the hymn sits centered below it.
    val paras = parseSynaxarium(rawText)
    val quote = paras.filter { it.kind == SynaxariumParaKind.NARRATIVE }
        .joinToString("\n\n") { it.text }
    // The bookmark sits in the card's own corner. It used to float in a
    // full-width row of its own above it, right-aligned against nothing, so it
    // read as belonging to the page rather than to the passage under it.
    Box(Modifier.fillMaxWidth()) {
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text(
                text = quote,
                style = readingBodyStyle(fontSp),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 46.dp),
            )
        }
        IconButton(
            onClick = onToggleBookmark,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp),
        ) {
            Icon(
                imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (bookmarked) s.removeAction else s.bookmarkAction,
                tint = if (bookmarked) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(com.agpeya.app.ui.theme.IconSize.small),
            )
        }
    }
    paras.forEach { para ->
        when (para.kind) {
            SynaxariumParaKind.ARKE_LABEL -> ArkeLabel(para.text)
            SynaxariumParaKind.ARKE_VERSE -> ArkeVerse(para.text, fontSp)
            SynaxariumParaKind.NARRATIVE -> Unit
        }
    }
}

/** The whole day's commemorations as plain text, for the share actions. */
private fun synaxariumShareBody(entries: List<SynaxariumEntry>): String =
    entries.joinToString("\n\n") { e ->
        buildString {
            val t = cleanSynaxariumText(e.title)
            if (t.isNotBlank()) {
                append(t)
                append("\n")
            }
            append(parseSynaxarium(e.text).joinToString("\n") { it.text })
        }
    }

/** A short one-line preview of an entry's body for the bookmarks list. */
private fun snippet(rawText: String): String {
    val clean = cleanSynaxariumText(rawText.replace('\n', ' '))
    return if (clean.length > 90) clean.take(89).trimEnd() + "…" else clean
}

/**
 * The day's commemorations, to jump by.
 *
 * The longest day of the year is twelve entries and sixteen thousand
 * characters — about twenty screenfuls — and there was nothing to move through
 * it with. The opening doxology and the feast lists are left out: what a reader
 * is looking for is a life.
 */
@Composable
private fun SynaxariumContents(entries: List<SynaxariumEntry>, onSelect: (Int) -> Unit) {
    val s = LocalStrings.current
    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.sm),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            Text(
                s.contents,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
        }
        itemsIndexed(entries) { index, entry ->
            val kind = synaxariumEntryKind(entry.title)
            if (kind == SynaxariumEntryKind.OPENING) return@itemsIndexed
            val title = cleanSynaxariumText(entry.title)
            if (title.isBlank()) return@itemsIndexed
            com.agpeya.app.ui.common.ListRow(
                title = title,
                subtitle = when (kind) {
                    SynaxariumEntryKind.SCRIPTURE -> s.bookmarkGroupScripture
                    SynaxariumEntryKind.FEAST_LIST -> s.fastingTitle
                    else -> null
                },
                onClick = { onSelect(index) },
            )
        }
        item { Spacer(Modifier.height(Spacing.xxl)) }
    }
}
