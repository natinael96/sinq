package com.agpeya.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.HorizontalDivider
import com.agpeya.app.data.BookRepository
import com.agpeya.app.model.Book
import com.agpeya.app.model.BookBlock
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.books.Rubrication
import com.agpeya.app.ui.theme.sinqColors
import com.agpeya.app.ui.theme.scaledReadingSp
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.WudaseRepository
import com.agpeya.app.model.WudaseContent
import com.agpeya.app.model.WudaseSection
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.common.rememberCurrentDate
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.ReadingMaxWidth
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.readingBodyStyle
import kotlinx.coroutines.launch
import java.time.LocalDate

private val FONT_STEPS_SP = com.agpeya.app.data.SettingsRepository.FONT_STEPS_SP

/** ውዳሴ ማርያም — the Praise of Mary, one portion per weekday, in Amharic (default)
 *  or Ge'ez via a toggle. [initialSectionId] preselects a section (the ዘወትር ጸሎት
 *  card opens the same reader on the daily prayer); otherwise today's portion.
 *  Includes መልክአ ማርያም and መልክአ ኢየሱስ directly within the reading sequence. */
private sealed interface WudasePage {
    val id: String
    val label: String
    val titleAm: String
    val titleGe: String

    data class Portioned(val section: WudaseSection) : WudasePage {
        override val id: String get() = section.id
        override val label: String get() = section.label
        override val titleAm: String get() = section.titleAm
        override val titleGe: String get() = section.titleGe
    }

    data class Hymn(val book: Book, val labelName: String) : WudasePage {
        override val id: String get() = book.id
        override val label: String get() = labelName
        override val titleAm: String get() = book.title
        override val titleGe: String get() = book.title
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WudaseMaryamScreen(
    onBack: () -> Unit,
    initialSectionId: String? = null,
    initialGeez: Boolean = false,
    onWriteNote: ((String, String) -> Unit)? = null,
    onOpenBook: (String) -> Unit = {},
    onOpenPrayerList: () -> Unit = {},
) {
    com.agpeya.app.ui.common.ReaderAwake()
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()

    var loadAttempt by rememberSaveable { mutableIntStateOf(0) }
    val contentResult by produceState<Result<WudaseContent>?>(initialValue = null, loadAttempt) {
        value = runCatching { WudaseRepository.load(context) }
    }
    val fontStep by SettingsRepository.fontStep(context).collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]

    var geez by rememberSaveable { mutableStateOf(initialGeez) }

    val data = contentResult?.getOrNull()
    val sections = data?.sections ?: emptyList()

    // Load መልክአ ማርያም and መልክአ ኢየሱስ directly into this reader
    val melkeaMaryam by produceState<Book?>(initialValue = null) {
        val meta = BookRepository.byPartName(context, "መልክአ ማርያም")
        value = meta?.let { BookRepository.book(context, it.id) }
    }
    val melkeaEyesus by produceState<Book?>(initialValue = null) {
        val meta = BookRepository.byPartName(context, "መልክአ ኢየሱስ")
        value = meta?.let { BookRepository.book(context, it.id) }
    }

    val pages: List<WudasePage> = remember(sections, melkeaMaryam, melkeaEyesus) {
        buildList {
            sections.forEach { add(WudasePage.Portioned(it)) }
            melkeaMaryam?.let { add(WudasePage.Hymn(it, "መልክአ ማርያም")) }
            melkeaEyesus?.let { add(WudasePage.Hymn(it, "መልክአ ኢየሱስ")) }
        }
    }

    val today by rememberCurrentDate()
    val todayIndex = remember(sections, today) {
        val wd = today.dayOfWeek.value
        sections.indexOfFirst { it.weekday == wd }.takeIf { it >= 0 } ?: 0
    }
    val initialIndex = remember(pages, initialSectionId) {
        initialSectionId?.let { id -> pages.indexOfFirst { it.id == id || it.label == id }.takeIf { it >= 0 } }
    }
    val dailyIndex = remember(pages) {
        pages.indexOfFirst { it.id == "daily" }.takeIf { it >= 0 } ?: 0
    }

    val pager = androidx.compose.foundation.pager.rememberPagerState(
        pageCount = { pages.size },
    )
    val selected = pager.currentPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0))

    var landed by rememberSaveable { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(pages.size) {
        if (!landed && pages.isNotEmpty()) {
            pager.scrollToPage(initialIndex ?: dailyIndex)
            landed = true
        }
    }
    val goToSection: (Int) -> Unit = { i -> scope.launch { pager.animateScrollToPage(i) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                val shown = pages.getOrNull(selected)
                SinqTopBar(
                    title = s.wudaseMariam,
                    subtitle = shown?.let { if (geez) it.titleGe else it.titleAm },
                    onBack = onBack,
                    actions = {
                        com.agpeya.app.ui.common.ReaderToolsMenu(
                            fontStep = fontStep,
                            maxFontStep = FONT_STEPS_SP.lastIndex,
                            onFontChange = { step -> scope.launch { SettingsRepository.setFontStep(context, step) } },
                            onWriteNote = shown?.let { pageItem -> onWriteNote?.let { write -> {
                                when (pageItem) {
                                    is WudasePage.Portioned ->
                                        write("wudase?sec=${android.net.Uri.encode(pageItem.section.id)}&lang=${if (geez) "gez" else "am"}",
                                            if (geez) pageItem.titleGe else pageItem.titleAm)
                                    is WudasePage.Hymn ->
                                        write("book/${pageItem.book.id}", pageItem.book.title)
                                }
                            } } },
                            shareEnabled = shown != null,
                            sharePayload = {
                                shown?.let { pageItem ->
                                    when (pageItem) {
                                        is WudasePage.Portioned ->
                                            com.agpeya.app.ui.common.SharePayload(
                                                body = (if (geez) pageItem.section.ge else pageItem.section.am).joinToString("\n\n"),
                                                kicker = s.wudaseMariam,
                                                title = if (geez) pageItem.titleGe else pageItem.titleAm,
                                            )
                                        is WudasePage.Hymn ->
                                            com.agpeya.app.ui.common.SharePayload(
                                                body = pageItem.book.chapters.firstOrNull()?.blocks
                                                    ?.filterNot { it.isHeading }
                                                    ?.joinToString("\n\n") { it.text } ?: "",
                                                kicker = s.wudaseMariam,
                                                title = pageItem.book.title,
                                            )
                                    }
                                } ?: com.agpeya.app.ui.common.SharePayload(body = "", kicker = s.wudaseMariam)
                            },
                        )
                    },
                )
                if (pages.isNotEmpty()) {
                    WudaseControlBar(
                        pages = pages,
                        selected = selected,
                        geez = geez,
                        onSelectPage = goToSection,
                        onToggleGeez = { geez = !geez },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                    )
                }
            }
        },
    ) { innerPadding ->
        if (contentResult == null) {
            LoadingPanel(Modifier.padding(innerPadding))
            return@Scaffold
        }
        if (data == null) {
            StatePanel(
                title = s.contentUnavailable,
                body = s.contentMissingBody,
                actionLabel = s.retryAction,
                onAction = { loadAttempt++ },
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        if (pages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                StatePanel(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = s.contentUnavailable,
                    body = s.contentMissingBody,
                )
            }
            return@Scaffold
        }

        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { pages[it].id },
            ) { page ->
                val pageItem = pages[page]
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().widthIn(max = ReadingMaxWidth),
                    contentPadding = PaddingValues(horizontal = Spacing.screen),
                ) {
                    item(key = "title") {
                        Text(
                            text = if (geez) pageItem.titleGe else pageItem.titleAm,
                            style = MaterialTheme.typography.titleMedium.inReadingFont(),
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 14.dp),
                        )
                    }

                    when (pageItem) {
                        is WudasePage.Portioned -> {
                            val stanzas = if (geez) pageItem.section.ge else pageItem.section.am
                            items(stanzas.size, key = { "st_$it" }) { i ->
                                val body = com.agpeya.app.ui.common.rubricated(
                                    stanzas[i],
                                    Rubrication.Scope.GENERAL,
                                )
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(
                                        text = body,
                                        style = readingBodyStyle(bodyFontSp),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                    )
                                }
                            }

                            val todaySection = sections.getOrNull(todayIndex)
                            val showToday = todaySection != null && page != todayIndex
                            val showPrayerList = pageItem.section.id == "yiwedsewa_melaekt"
                            if (showToday || showPrayerList) {
                                item(key = "doors") {
                                    Spacer(Modifier.height(Spacing.lg))
                                    com.agpeya.app.ui.common.SinqDivider()
                                    Spacer(Modifier.height(Spacing.md))
                                    Text(
                                        s.continueReading,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                    Spacer(Modifier.height(Spacing.sm))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                                    ) {
                                        if (showToday) {
                                            com.agpeya.app.ui.common.DoorChip(
                                                icon = Icons.Outlined.Today,
                                                label = "${s.todayLabel}  ·  ${todaySection.label}",
                                                onClick = { goToSection(todayIndex) },
                                            )
                                        }
                                        if (showPrayerList) {
                                            com.agpeya.app.ui.common.DoorChip(
                                                icon = Icons.Outlined.VolunteerActivism,
                                                label = s.prayerListTitle,
                                                onClick = onOpenPrayerList,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is WudasePage.Hymn -> {
                            val blocks = pageItem.book.chapters.firstOrNull()?.blocks.orEmpty()
                            itemsIndexed(blocks, key = { i, _ -> "bk_${pageItem.id}_$i" }) { _, block ->
                                if (block.isHeading) {
                                    Text(
                                        text = block.text,
                                        style = (if (block.level == 1) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall).inReadingFont(),
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
                                    )
                                } else {
                                    val red = sinqColors.arke
                                    val secondary = MaterialTheme.colorScheme.secondary
                                    val markerSize = scaledReadingSp(bodyFontSp) * 0.85f
                                    val body = remember(block.text, block.red, block.index, red, secondary, markerSize) {
                                        buildAnnotatedString {
                                            val n = block.index
                                            if (n != null && n > 0) {
                                                withStyle(SpanStyle(color = secondary, fontSize = markerSize)) {
                                                    append(geezNumeral(n))
                                                    append("፡")
                                                }
                                                append(" ")
                                            }
                                            val startOffset = length
                                            append(block.text)
                                            val spans = block.redRanges.ifEmpty {
                                                Rubrication.redRanges(block.text, Rubrication.Scope.MELKIE)
                                            }
                                            spans.forEach {
                                                addStyle(SpanStyle(color = red), startOffset + it.first, startOffset + it.last + 1)
                                            }
                                        }
                                    }
                                    androidx.compose.foundation.text.selection.SelectionContainer {
                                        Text(
                                            text = body,
                                            style = readingBodyStyle(bodyFontSp),
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(Spacing.huge)) }
                }
            }
        }
    }
}

/**
 * Single-line docked bar for Wudase Maryam.
 * Displays all portions (including Melkea Maryam and Melkea Yesus) in a horizontally scrollable track,
 * separated by a vertical hairline from the compact single-button edition toggle.
 */
@Composable
private fun WudaseControlBar(
    pages: List<WudasePage>,
    selected: Int,
    geez: Boolean,
    onSelectPage: (Int) -> Unit,
    onToggleGeez: () -> Unit,
) {
    val trackState = androidx.compose.foundation.lazy.rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(selected) {
        if (selected in pages.indices) {
            trackState.animateScrollToItem(selected)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LazyRow(
                state = trackState,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(pages, key = { _, it -> it.id }) { i, item ->
                    val isSel = i == selected
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(
                                if (isSel) Modifier.background(MaterialTheme.colorScheme.primary)
                                else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                            )
                            .clickable { onSelectPage(i) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }

            VerticalDivider(
                modifier = Modifier.height(20.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            com.agpeya.app.ui.common.EditionToggle(
                geez = geez,
                onToggle = onToggleGeez,
            )
        }
    }
}
