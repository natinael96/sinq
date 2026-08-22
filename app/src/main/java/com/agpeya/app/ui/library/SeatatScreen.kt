package com.agpeya.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.SeatatLang
import com.agpeya.app.data.SeatatRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.SeatatContent
import com.agpeya.app.model.SeatatLine
import com.agpeya.app.model.SeatatSection
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.readingBodyStyle
import com.agpeya.app.ui.theme.scaledReadingSp
import com.agpeya.app.ui.theme.sinqColors
import kotlinx.coroutines.launch

private val FONT_STEPS_SP = SettingsRepository.FONT_STEPS_SP

/** One row of the continuous office: a section heading, or one paired line
 *  (with its flat line index, so tap-selection can run across sections). */
private sealed interface SeatatRow {
    val key: String

    data class Header(val section: SeatatSection, val sectionIndex: Int) : SeatatRow {
        override val key get() = "h_${section.id}"
    }

    data class Line(val line: SeatatLine, val sectionIndex: Int, val flatIndex: Int) : SeatatRow {
        override val key get() = "l_$flatIndex"
    }
}

/**
 * ሰዓታት (Seatat) — መጽሐፈ ሰዓታት ዘሌሊት ወዘነግህ, one office read as one continuous
 * scroll: the መቅድም, then the fifteen ስብሐት parts alternating with the biblical
 * canticles and the ምስለ intercessions, exactly in the printed order. Ge'ez is
 * the prayer; each Amharic line sits directly under its own Ge'ez line, smaller
 * and italic. Navigation is a contents sheet (ይዘት) — 42 sections are a list to
 * jump within, not chips to flip between — and the language mode (paired /
 * Ge'ez / Amharic) is persisted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeatatScreen(onBack: () -> Unit, initialSectionId: String? = null) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()

    val content by produceState<SeatatContent?>(initialValue = null) {
        value = SeatatRepository.load(context)
    }
    val fontStep by SettingsRepository.fontStep(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]
    val lang by SettingsRepository.seatatLang(context).collectAsState(initial = SeatatLang.BOTH)

    val data = content
    val sections = data?.sections ?: emptyList()

    // The whole office flattened once: headers interleaved with lines, each
    // line carrying a global index for cross-section tap selection.
    val rows = remember(sections) {
        buildList {
            var flat = 0
            sections.forEachIndexed { si, sec ->
                add(SeatatRow.Header(sec, si))
                sec.lines.forEach { line ->
                    add(SeatatRow.Line(line, si, flat))
                    flat++
                }
            }
        }
    }
    val flatLines = remember(rows) { rows.filterIsInstance<SeatatRow.Line>() }
    // List index of a section's header; +1 below accounts for the toggle item.
    fun headerListIndex(sectionIndex: Int): Int =
        rows.indexOfFirst { it is SeatatRow.Header && it.sectionIndex == sectionIndex } + 1

    val listState = rememberLazyListState()
    var sheetOpen by rememberSaveable { mutableStateOf(false) }

    // A search result (seatat?sec=...) lands on its section — once.
    var landed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(rows) {
        if (!landed && initialSectionId != null && rows.isNotEmpty()) {
            sections.indexOfFirst { it.id == initialSectionId }.takeIf { it >= 0 }?.let {
                listState.scrollToItem(headerListIndex(it))
            }
            landed = true
        }
    }

    // Live line selection across the whole office; -1 = none.
    var selA by rememberSaveable { mutableIntStateOf(-1) }
    var selB by rememberSaveable { mutableIntStateOf(-1) }
    val selRange = com.agpeya.app.ui.reading.flatSelectionRange(selA, selB)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.seatatTitle,
                subtitle = "ዘሌሊት ወዘነግህ",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { sheetOpen = true }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.List,
                            contentDescription = s.contents,
                            modifier = Modifier.size(IconSize.medium),
                        )
                    }
                    // Shares the section currently at the top of the screen.
                    val visibleSection = sections.getOrNull(
                        (rows.getOrNull(
                            (listState.firstVisibleItemIndex - 1).coerceAtLeast(0),
                        )?.let { if (it is SeatatRow.Header) it.sectionIndex else (it as? SeatatRow.Line)?.sectionIndex })
                            ?: 0,
                    )
                    com.agpeya.app.ui.common.ReaderToolsMenu(
                        fontStep = fontStep,
                        maxFontStep = FONT_STEPS_SP.lastIndex,
                        onFontChange = { step -> scope.launch { SettingsRepository.setFontStep(context, step) } },
                        shareEnabled = visibleSection != null,
                        sharePayload = {
                            visibleSection?.let {
                                com.agpeya.app.ui.common.SharePayload(
                                    body = it.lines.joinToString("\n\n") { line -> shareLine(line, lang) },
                                    kicker = s.seatatTitle,
                                    title = sectionTitle(it, lang),
                                )
                            }
                        },
                    )
                },
            )
        },
    ) { innerPadding ->
        if (data == null) {
            LoadingPanel(Modifier.padding(innerPadding))
            return@Scaffold
        }
        if (sections.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                StatePanel(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = s.contentUnavailable,
                    body = s.contentMissingBody,
                )
            }
            return@Scaffold
        }

        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.screen),
            ) {
                item(key = "lang") {
                    Spacer(Modifier.height(8.dp))
                    LangModeToggle(
                        lang = lang,
                        bothLabel = s.seatatLangBoth,
                        geezLabel = s.wudaseLangGeez,
                        amharicLabel = s.wudaseLangAmharic,
                    ) { mode -> scope.launch { SettingsRepository.setSeatatLang(context, mode) } }
                    Spacer(Modifier.height(6.dp))
                }
                items(rows.size, key = { rows[it].key }) { i ->
                    when (val row = rows[i]) {
                        is SeatatRow.Header -> SectionHeader(row.section, lang)
                        is SeatatRow.Line -> PairedLine(
                            line = row.line,
                            lang = lang,
                            bodyFontSp = bodyFontSp,
                            selected = row.flatIndex in selRange,
                            onTap = {
                                val (a, b) = com.agpeya.app.ui.reading.advanceFlatSelection(selA, row.flatIndex)
                                selA = a
                                selB = b
                            },
                        )
                    }
                }
                item { Spacer(Modifier.height(48.dp)) }
            }

            val selBody = if (selRange.isEmpty()) null
            else flatLines.filter { it.flatIndex in selRange }
                .joinToString("\n\n") { shareLine(it.line, lang) }
                .ifBlank { null }
            // The selection's home section names the share.
            val selTitle = flatLines.firstOrNull { it.flatIndex == selRange.first }
                ?.let { sections.getOrNull(it.sectionIndex) }
                ?.let { sectionTitle(it, lang) }
            com.agpeya.app.ui.reading.SelectionShareBar(
                visible = selA >= 0,
                onDismiss = { selA = -1; selB = -1 },
                shareText = selBody?.let { listOfNotNull(selTitle, it).joinToString("\n\n") },
                shareImage = selBody?.let {
                    com.agpeya.app.ui.common.SharePayload(body = it, kicker = s.seatatTitle, title = selTitle)
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    // ይዘት — the contents sheet. The one navigation surface for 43 sections:
    // tap a title, land on it, keep reading.
    if (sheetOpen) {
        ModalBottomSheet(onDismissRequest = { sheetOpen = false }) {
            LazyColumn(contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.sm)) {
                items(sections.size, key = { sections[it].id }) { i ->
                    val sec = sections[i]
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                sheetOpen = false
                                scope.launch { listState.scrollToItem(headerListIndex(i)) }
                            }
                            .padding(vertical = Spacing.md, horizontal = Spacing.xs),
                    ) {
                        Text(
                            sec.titleGe.ifBlank { sec.titleAm },
                            style = MaterialTheme.typography.titleSmall.inReadingFont(),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        if (sec.titleAm.isNotBlank() && sec.titleAm != sec.titleGe) {
                            Text(
                                sec.titleAm,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(Spacing.xxl)) }
            }
        }
    }
}

private fun sectionTitle(section: SeatatSection, lang: SeatatLang): String = when (lang) {
    SeatatLang.AMHARIC -> section.titleAm.ifBlank { section.titleGe }
    else -> section.titleGe.ifBlank { section.titleAm }
}

/** One line in the language mode's shape — pairs joined for sharing. The `*`
 *  typo flag is an in-app review mark; it never travels into shared text. */
private fun shareLine(line: SeatatLine, lang: SeatatLang): String = when (lang) {
    SeatatLang.BOTH -> if (line.am.isBlank()) line.ge
    else if (line.ge.isBlank()) line.am
    else "${line.ge}\n${line.am}"
    SeatatLang.GEEZ -> line.ge.ifBlank { line.am }
    SeatatLang.AMHARIC -> line.am.ifBlank { line.ge }
}.replace("*", "")

/**
 * A word in seatat.json may carry a trailing `*` — the content's "this word
 * needs checking" flag for a suspected typo or an unverified spelling. The
 * asterisk renders small, raised and red so a reviewer can spot it instantly,
 * while the prayer text around it stays untouched.
 */
private fun typoFlagged(text: String, flagColor: androidx.compose.ui.graphics.Color) =
    buildAnnotatedString {
        text.split("*").forEachIndexed { i, part ->
            if (i > 0) {
                withStyle(
                    SpanStyle(color = flagColor, baselineShift = BaselineShift.Superscript),
                ) { append("*") }
            }
            append(part)
        }
    }

/** A section heading inside the continuous office: gold title, Amharic under
 *  it in paired mode — the same rank the ግጻዌ page gives its movements. */
@Composable
private fun SectionHeader(section: SeatatSection, lang: SeatatLang) {
    Column(Modifier.fillMaxWidth().padding(top = Spacing.xxl, bottom = Spacing.md)) {
        Text(
            text = sectionTitle(section, lang),
            style = MaterialTheme.typography.titleMedium.inReadingFont(),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (lang == SeatatLang.BOTH && section.titleAm.isNotBlank() && section.titleAm != section.titleGe) {
            Text(
                text = section.titleAm,
                style = MaterialTheme.typography.labelMedium
                    .inReadingFont()
                    .copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
    }
}

/**
 * One paired line: the Ge'ez as the prayer, its Amharic directly beneath —
 * smaller, italic, slightly indented, muted — close enough that the pair
 * reads as a unit. In a single-language mode the shown text takes the full
 * reading width and the primary style. A line with no Ge'ez (the መቅድም's
 * Amharic prose) renders its Amharic as the primary text in every mode.
 */
@Composable
private fun PairedLine(
    line: SeatatLine,
    lang: SeatatLang,
    bodyFontSp: Int,
    selected: Boolean,
    onTap: () -> Unit,
) {
    val primaryStyle = readingBodyStyle(bodyFontSp)
    // The translation stays visually subordinate: ~85% of the reading size,
    // italic, muted — near its Ge'ez line but never competing with it.
    val translationStyle = readingBodyStyle(bodyFontSp).copy(
        fontSize = scaledReadingSp(bodyFontSp) * 0.85f,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Normal,
    )
    androidx.compose.foundation.text.selection.SelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                    else androidx.compose.ui.graphics.Color.Transparent,
                )
                .clickable(onClick = onTap)
                .padding(bottom = 18.dp),
        ) {
            val flag = MaterialTheme.colorScheme.error
            val primary: String
            val under: String?
            when {
                line.ge.isBlank() -> { primary = line.am; under = null }
                lang == SeatatLang.GEEZ -> { primary = line.ge; under = null }
                lang == SeatatLang.AMHARIC -> { primary = line.am.ifBlank { line.ge }; under = null }
                else -> { primary = line.ge; under = line.am.takeIf { it.isNotBlank() } }
            }
            Text(
                text = typoFlagged(primary, flag),
                style = primaryStyle,
                color = MaterialTheme.colorScheme.onBackground,
            )
            under?.let {
                Text(
                    text = typoFlagged(it, flag),
                    style = translationStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 3.dp),
                )
            }
        }
    }
}

/** Three-way language mode, same visual language as the ውዳሴ toggle. */
@Composable
private fun LangModeToggle(
    lang: SeatatLang,
    bothLabel: String,
    geezLabel: String,
    amharicLabel: String,
    onSelect: (SeatatLang) -> Unit,
) {
    val options = listOf(
        SeatatLang.BOTH to bothLabel,
        SeatatLang.GEEZ to geezLabel,
        SeatatLang.AMHARIC to amharicLabel,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { (mode, label) ->
            val isSel = mode == lang
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isSel) sinqColors.hero else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.inReadingFont(),
                    color = if (isSel) sinqColors.onHero else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
