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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.reading.FontSizeActions
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.readingBodyStyle
import com.agpeya.app.ui.theme.scaledReadingSp
import com.agpeya.app.ui.theme.sinqColors
import kotlinx.coroutines.launch

private val FONT_STEPS_SP = SettingsRepository.FONT_STEPS_SP

/**
 * ሰዓታት (Seatat) — the prayers of the hours, Ge'ez-first with a line-by-line
 * Amharic translation. Each Amharic line sits directly under its Ge'ez line,
 * smaller and italic, so the page reads as one continuous prayer with an
 * inline translation — never two documents. The language mode (paired /
 * Ge'ez only / Amharic only) is persisted; the section chips (ጠዋት · ቀትር ·
 * ማታ · ሌሊት) switch hours without losing each hour's reading position.
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

    val initialIndex = remember(sections, initialSectionId) {
        initialSectionId?.let { id -> sections.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
    }
    var picked by rememberSaveable { mutableIntStateOf(-1) }
    val selected = if (picked >= 0) picked else initialIndex ?: 0

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.seatatTitle,
                onBack = onBack,
                actions = {
                    // The whole hour being read, shaped by the language mode.
                    val shown = sections.getOrNull(selected.coerceIn(0, (sections.size - 1).coerceAtLeast(0)))
                    com.agpeya.app.ui.common.ShareMenuAction(enabled = shown != null, payload = {
                        shown?.let {
                            com.agpeya.app.ui.common.SharePayload(
                                body = it.lines.joinToString("\n\n") { line -> shareLine(line, lang) },
                                kicker = s.seatatTitle,
                                title = if (lang == SeatatLang.AMHARIC) it.titleAm.ifBlank { it.titleGe } else it.titleGe,
                            )
                        }
                    })
                    FontSizeActions(fontStep = fontStep, maxStep = FONT_STEPS_SP.lastIndex) { step ->
                        scope.launch { SettingsRepository.setFontStep(context, step) }
                    }
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
                    icon = Icons.Outlined.MenuBook,
                    title = s.contentUnavailable,
                    body = s.contentMissingBody,
                )
            }
            return@Scaffold
        }

        val section = sections.getOrNull(selected.coerceIn(0, sections.size - 1))
        // Live line selection: tap anchors, next tap moves the end; -1 = none.
        var selA by rememberSaveable(selected) { mutableIntStateOf(-1) }
        var selB by rememberSaveable(selected) { mutableIntStateOf(-1) }
        val selRange = com.agpeya.app.ui.reading.flatSelectionRange(selA, selB)

        val listState = rememberLazyListState()
        // Each hour keeps its own reading position for the life of the screen:
        // the scroll is recorded per section and restored when its chip is
        // tapped again, so flipping ጠዋት → ማታ → ጠዋት lands back mid-prayer.
        val positions = remember { mutableMapOf<Int, Pair<Int, Int>>() }
        LaunchedEffect(selected) {
            val (index, offset) = positions[selected] ?: (0 to 0)
            listState.scrollToItem(index, offset)
        }
        LaunchedEffect(selected) {
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .collect { positions[selected] = it }
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
                }
                item(key = "hours") {
                    HourStrip(sections.map { it.label.ifBlank { it.id } }, selected) { picked = it }
                    Spacer(Modifier.height(6.dp))
                }
                if (section != null) {
                    item(key = "title") {
                        Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp)) {
                            Text(
                                text = if (lang == SeatatLang.AMHARIC) section.titleAm.ifBlank { section.titleGe }
                                else section.titleGe,
                                style = MaterialTheme.typography.titleMedium.inReadingFont(),
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (lang == SeatatLang.BOTH && section.titleAm.isNotBlank()) {
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
                    items(section.lines.size, key = { "ln_$it" }) { i ->
                        PairedLine(
                            line = section.lines[i],
                            lang = lang,
                            bodyFontSp = bodyFontSp,
                            selected = i in selRange,
                            onTap = {
                                val (a, b) = com.agpeya.app.ui.reading.advanceFlatSelection(selA, i)
                                selA = a
                                selB = b
                            },
                        )
                    }
                }
                item { Spacer(Modifier.height(48.dp)) }
            }

            val linesNow = section?.lines ?: emptyList()
            val selBody = if (selRange.isEmpty()) null
            else linesNow.filterIndexed { i, _ -> i in selRange }
                .joinToString("\n\n") { shareLine(it, lang) }
                .ifBlank { null }
            val sectionTitle = section?.let {
                if (lang == SeatatLang.AMHARIC) it.titleAm.ifBlank { it.titleGe } else it.titleGe
            }
            com.agpeya.app.ui.reading.SelectionShareBar(
                visible = selA >= 0,
                onDismiss = { selA = -1; selB = -1 },
                shareText = selBody?.let { listOfNotNull(sectionTitle, it).joinToString("\n\n") },
                shareImage = selBody?.let {
                    com.agpeya.app.ui.common.SharePayload(body = it, kicker = s.seatatTitle, title = sectionTitle)
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** One line in the language mode's shape — pairs joined for sharing. The `*`
 *  typo flag is an in-app review mark; it never travels into shared text. */
private fun shareLine(line: SeatatLine, lang: SeatatLang): String = when (lang) {
    SeatatLang.BOTH -> if (line.am.isBlank()) line.ge else "${line.ge}\n${line.am}"
    SeatatLang.GEEZ -> line.ge
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

/**
 * One paired line: the Ge'ez as the prayer, its Amharic directly beneath —
 * smaller, italic, slightly indented, muted — close enough that the pair
 * reads as a unit. In a single-language mode the shown text takes the full
 * reading width and the primary style.
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
            when (lang) {
                SeatatLang.GEEZ -> Text(
                    text = typoFlagged(line.ge, flag),
                    style = primaryStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                SeatatLang.AMHARIC -> Text(
                    text = typoFlagged(line.am.ifBlank { line.ge }, flag),
                    style = primaryStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                SeatatLang.BOTH -> {
                    Text(
                        text = typoFlagged(line.ge, flag),
                        style = primaryStyle,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (line.am.isNotBlank()) {
                        Text(
                            text = typoFlagged(line.am, flag),
                            style = translationStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 12.dp, top = 3.dp),
                        )
                    }
                }
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

/** ጠዋት · ቀትር · ማታ · ሌሊት — the hour chips, same shape as the ውዳሴ day strip. */
@Composable
private fun HourStrip(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val stripState = rememberLazyListState()
    LaunchedEffect(selected) {
        if (selected in labels.indices) stripState.animateScrollToItem(selected)
    }
    LazyRow(
        state = stripState,
        modifier = Modifier.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(labels) { i, label ->
            val isSel = i == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .then(
                        if (isSel) Modifier.background(sinqColors.hero)
                        else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.inReadingFont(),
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSel) sinqColors.onHero else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
