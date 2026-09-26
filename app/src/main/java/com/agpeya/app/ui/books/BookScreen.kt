package com.agpeya.app.ui.books

import androidx.compose.foundation.selection.selectable
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.BookRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.Book
import com.agpeya.app.model.BookBlock
import com.agpeya.app.model.BookMeta
import com.agpeya.app.ui.common.Passage
import com.agpeya.app.ui.common.SelectPill
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.reading.ReadingColumn
import com.agpeya.app.ui.reading.SelectionBar
import com.agpeya.app.ui.reading.advanceFlatSelection
import com.agpeya.app.ui.reading.flatSelectionRange
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.readingBodyStyle
import com.agpeya.app.ui.theme.sinqColors

/**
 * One book off the ሌሎች መጻሕፍት shelf.
 *
 * The books have nothing in common but their script: a መልክእ is one chapter of
 * twenty stanzas, ሥርዓተ ቅዳሴ is twenty-three chapters and two thousand
 * paragraphs. So the chapter strip appears only when there is more than one
 * chapter to pick, and a one-chapter hymn opens straight into its text.
 *
 * Selection is the same two-tap model every other reader uses, so a stanza
 * copies, shares and becomes a card exactly as a psalm or a ዚቅ does.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun BookScreen(
    bookId: String,
    /** 1-based explicit chapter; 0 opens the first chapter. */
    openAtChapter: Int = 0,
    /**
     * Stanza within that chapter to land on; -1 lands at the top. Set when a
     * ማኅሌት movement opens the hymn it is an excerpt of — a መልክእ is one chapter
     * of forty stanzas, so the chapter alone would land nowhere useful.
     */
    openAtBlock: Int = -1,
    onBack: () -> Unit,
    onOpenBook: (String) -> Unit,
    onWriteNote: (String, String) -> Unit,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    com.agpeya.app.ui.common.ReaderAwake()
    val context = LocalContext.current
    val s = LocalStrings.current
    val fontStep by SettingsRepository.fontStep(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = SettingsRepository.FONT_STEPS_SP[
        fontStep.coerceIn(0, SettingsRepository.FONT_STEPS_SP.lastIndex),
    ]

    val bookLoad = com.agpeya.app.ui.common.rememberContentLoad(bookId) { BookRepository.book(context, bookId) to BookRepository.meta(context, bookId) }
    val book = bookLoad.value?.first
    val meta = bookLoad.value?.second
    val isComingSoon = meta?.comingSoon == true || bookId == "f9217f008c"
    if (com.agpeya.app.ui.common.contentLoadScreen(bookLoad, meta?.title ?: s.booksTitle, onBack, book?.chapters.isNullOrEmpty(), comingSoon = isComingSoon)) return
    val chapters = book?.chapters.orEmpty()

    var chapter by rememberSaveable(bookId) {
        mutableIntStateOf((openAtChapter - 1).coerceAtLeast(0))
    }
    val shown = chapters.getOrNull(chapter.coerceIn(0, (chapters.size - 1).coerceAtLeast(0)))
    val blocks = shown?.blocks.orEmpty()

    val listState = rememberLazyListState()
    // Once per book: landing again after the reader has scrolled away, or picked
    // another chapter, would be the screen taking the wheel back off them.
    var landed by rememberSaveable(bookId) { mutableStateOf(false) }
    val blocksNow = shown?.blocks.orEmpty()
    // The stanza's row is preceded only by the recension line now, and that may
    // not be there either: the chapter strip has moved into the title.
    val leadingItems =
        if (meta?.let { it.variants.isNotEmpty() || it.variantOf != null } == true) 1 else 0
    var chaptersOpen by remember { mutableStateOf(false) }
    // Which stanza is at the top of the screen. derivedStateOf so the top bar
    // recomposes when the number changes and not on every frame of the scroll.
    val atBlock by remember(blocks.size, leadingItems) {
        derivedStateOf {
            (listState.firstVisibleItemIndex - leadingItems + 1).coerceIn(1, blocks.size.coerceAtLeast(1))
        }
    }
    LaunchedEffect(bookId, blocksNow.size, openAtBlock) {
        if (!landed && openAtBlock >= 0 && openAtBlock < blocksNow.size) {
            landed = true
            listState.scrollToItem(leadingItems + openAtBlock)
        }
    }
    var selA by rememberSaveable(bookId, chapter) { mutableIntStateOf(-1) }
    var selB by rememberSaveable(bookId, chapter) { mutableIntStateOf(-1) }
    val selRange = flatSelectionRange(selA, selB)
    val selBody = if (selRange.isEmpty()) null
    else blocks.filterIndexed { i, _ -> i in selRange }
        .joinToString("\n\n") { it.text }
        .ifBlank { null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = book?.title ?: meta?.title.orEmpty(),
                // Which scanned copy this chapter came from, when the book was
                // merged from several — the reader should never have to guess.
                accentLine = shown?.source,
                onBack = onBack,
                actions = {
                    com.agpeya.app.ui.common.ReaderToolsMenu(
                        fontStep = fontStep, maxFontStep = SettingsRepository.FONT_STEPS_SP.lastIndex,
                        onFontChange = { step -> scope.launch { SettingsRepository.setFontStep(context, step) } },
                        sharePayload = { com.agpeya.app.ui.common.SharePayload(
                            body = blocks.joinToString("\n\n") { it.text }, title = shown?.title, kicker = book?.title,
                        ) },
                        onWriteNote = { onWriteNote("book/$bookId?ch=${chapter + 1}&blk=${atBlock - 1}", book?.title.orEmpty()) },
                    )
                },
                titleContent = {
                    com.agpeya.app.ui.common.ReaderTitleBar(
                        title = book?.title ?: meta?.title.orEmpty(),
                        chapterLabel = shown?.title?.takeIf { it.isNotBlank() }
                            ?: geezNumeral(chapter + 1).takeIf { chapters.size > 1 },
                        pickable = chapters.size > 1,
                        onPick = { chaptersOpen = true },
                        position = if (blocks.size > 1) {
                            "${geezNumeral(atBlock)} / ${geezNumeral(blocks.size)}"
                        } else null,
                    )
                },
            )
        },
        bottomBar = {
            SelectionBar(
                visible = selA >= 0,
                onDismiss = { selA = -1; selB = -1 },
                passage = selBody?.let {
                    Passage(
                        verses = listOf(null to it),
                        citation = listOfNotNull(
                            book?.title,
                            shown?.title?.takeIf { t -> t.isNotBlank() },
                        ).joinToString("  ·  "),
                    )
                },
                imageKicker = book?.title ?: s.booksTitle,
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize()) {
            ReadingColumn(innerPadding = inner, state = listState) {
                // A second recension is the one thing a reader of these hymns
                // most wants to know exists, and the shelf does not say it.
                meta?.let { m ->
                    val others = m.variants + listOfNotNull(m.variantOf)
                    if (others.isNotEmpty()) {
                        item(key = "variants") {
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = Spacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                others.forEach { other ->
                                    Text(
                                        s.booksOtherRecension(other.title),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .clickable { onOpenBook(other.id) }
                                            .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                                    )
                                }
                            }
                        }
                    }
                }
                items(blocks.size, key = { "$chapter:$it" }) { index ->
                    BookBlockRow(
                        block = blocks[index],
                        bilingual = meta?.lang == "mixed",
                        scope = rubricScopeFor(meta?.shelf),
                        bodyFontSp = bodyFontSp,
                        selected = index in selRange,
                        onTap = {
                            val (a, b) = advanceFlatSelection(selA, index)
                            selA = a
                            selB = b
                        },
                    )
                }
                // The end of a chapter is a junction, not a dead end. The Bible
                // reader has said so since 1.9.0; these books had to be scrolled
                // back to the strip at the top, which in ሥርዓተ ቅዳሴ is two
                // thousand paragraphs away.
                if (chapters.size > 1) {
                    item(key = "stepper") {
                        com.agpeya.app.ui.common.ChapterStepper(
                            previousLabel = chapters.getOrNull(chapter - 1)?.title
                                ?.takeIf { it.isNotBlank() } ?: s.previousChapter,
                            nextLabel = chapters.getOrNull(chapter + 1)?.title
                                ?.takeIf { it.isNotBlank() } ?: s.nextChapter,
                            onPrevious = if (chapter > 0) {
                                { chapter -= 1; selA = -1; selB = -1; scope.launch { listState.scrollToItem(0) } }
                            } else null,
                            onNext = if (chapter < chapters.size - 1) {
                                { chapter += 1; selA = -1; selB = -1; scope.launch { listState.scrollToItem(0) } }
                            } else null,
                        )
                    }
                }
                item { Spacer(Modifier.height(Spacing.huge)) }
            }
        }
        if (chaptersOpen) {
            com.agpeya.app.ui.common.ChapterSheet(
                count = chapters.size,
                current = chapter,
                onPick = { chaptersOpen = false; chapter = it; selA = -1; selB = -1; scope.launch { listState.scrollToItem(0) } },
                onDismiss = { chaptersOpen = false },
                labelFor = { chapters[it].title.takeIf { t -> t.isNotBlank() } },
            )
        }
    }
}

/**
 * One block: a heading, a Ge'ez stanza, or the Amharic beside it.
 *
 * The Amharic is set apart rather than beside — a phone has no second column —
 * by an indent and the gold the app accents with, so the eye can run down the
 * Ge'ez alone and drop into the translation only where it wants one. Gold and
 * not the muted grey it used to take: a passage in the language the reader
 * actually has should not be the faintest thing on the page. The italic is gone
 * with it — Ethiopic has no italic, so that was a synthesised slant, and it
 * read as a rendering fault rather than a voice.
 *
 * [bilingual] is the whole of what makes an Amharic block a translation. A book
 * that is simply written in Amharic — ድርሳነ ሚካኤል has five hundred such blocks —
 * gets no rule and no indent, because there is nothing there to be a gloss of,
 * and marking its body as commentary was both wrong and the reason it read
 * greyed-out from end to end.
 *
 * Headings take the rubric red the printed books and the manuscripts before
 * them use. In the body, red comes from the block's own spans when the source
 * carries them — a ዜማ book's rubrication is a decision per line and cannot be
 * derived — and otherwise from [Rubrication] at this shelf's [scope].
 */
/**
 * How much of a shelf reddens.
 *
 * Only a መልክእ opens every stanza with the salutation formula, so only it gets
 * the opening rule; the saints are named on this shelf whatever the kind, so
 * they redden throughout it. Everywhere outside the books — a Psalm, a Gospel —
 * the caller passes GENERAL and the saints stay in ink.
 */
private fun rubricScopeFor(shelf: String?): Rubrication.Scope =
    if (shelf == "melkie") Rubrication.Scope.MELKIE else Rubrication.Scope.MAHLET

@Composable
private fun BookBlockRow(
    block: BookBlock,
    bilingual: Boolean,
    scope: Rubrication.Scope,
    bodyFontSp: Int,
    selected: Boolean,
    onTap: () -> Unit,
) {
    if (block.isHeading) {
        Text(
            block.text,
            style = MaterialTheme.typography.titleSmall.inReadingFont(),
            color = sinqColors.arke,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.lg, bottom = Spacing.xs),
        )
        return
    }
    val isGloss = block.isAmharic && bilingual
    Column(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onTap)
            .background(
                if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f)
                else Color.Transparent,
            )
            .padding(
                start = if (isGloss) Spacing.lg else Spacing.sm,
                end = Spacing.sm,
                top = if (isGloss) Spacing.xxs else Spacing.sm,
                bottom = Spacing.xs,
            ),
    ) {
        if (!block.targetAnatomy.isNullOrBlank()) {
            Text(
                text = if (block.targetAnatomy.startsWith("ለ")) block.targetAnatomy else "ለ${block.targetAnatomy}",
                style = MaterialTheme.typography.labelSmall.inReadingFont(),
                color = sinqColors.arke,
                modifier = Modifier.padding(bottom = Spacing.xxs),
            )
        }
        if (!block.speaker.isNullOrBlank() && !isGloss) {
            val (label, bg) = when (block.speaker.lowercase()) {
                "priest", "ካህን" -> "ካህን" to sinqColors.arke.copy(alpha = 0.12f)
                "deacon", "ዲያቆን" -> "ዲያቆን" to MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                "congregation", "ሕዝብ" -> "ሕዝብ" to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                "reader", "አንባቢ" -> "አንባቢ" to MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                else -> block.speaker to MaterialTheme.colorScheme.surfaceVariant
            }
            Surface(
                color = bg,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(bottom = Spacing.xxs),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.inReadingFont(),
                    color = if (block.speaker.lowercase() in listOf("priest", "ካህን")) sinqColors.arke else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                )
            }
        }
        if (!block.rubric.isNullOrBlank()) {
            Text(
                text = block.rubric,
                style = MaterialTheme.typography.bodySmall.inReadingFont().copy(fontStyle = FontStyle.Italic),
                color = sinqColors.arke,
                modifier = Modifier.padding(bottom = Spacing.xxs),
            )
        }
        val red = sinqColors.arke
        // Two sources of red, unioned. The data's own spans come from a printed
        // page whose rubrication is editorial and cannot be derived — a ዜማ book
        // reddens a name on one line and leaves it black on the next. The rule's
        // spans are the salutation and the Names, which are a rule everywhere.
        val body = if (isGloss) {
            remember(block.text) { AnnotatedString(block.text) }
        } else {
            remember(block.text, block.red, scope, red) {
                buildAnnotatedString {
                    append(block.text)
                    val spans = block.redRanges.ifEmpty {
                        Rubrication.redRanges(block.text, scope)
                    }
                    spans.forEach {
                        addStyle(SpanStyle(color = red), it.first, it.last + 1)
                    }
                }
            }
        }
        Text(
            body,
            style = readingBodyStyle(bodyFontSp),
            color = if (isGloss) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.onBackground,
        )
    }
}
