package com.agpeya.app.ui.gitsawe

import androidx.compose.foundation.background
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.reading.FontSizeActions
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.scaledReadingSp
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.readingBodyStyle
import kotlinx.coroutines.launch
import java.time.LocalDate

private val FONT_STEPS_SP = listOf(17, 19, 22, 25, 29)

/** Warm liturgical red for the አርኬ hymn — distinct from the app's gold accent. */
private val ArkeRed = androidx.compose.ui.graphics.Color(0xFFF0776A)

/** The hymn is verse, not prose: a little more air than the running text. */
private const val ArkeLineHeight = 1.78f

/** The fixed closing ጸሎት is a coda — noticeably smaller and tightly set. */
private const val CLOSING_FONT_SCALE = 0.78f
private const val CLOSING_LINE_HEIGHT = 1.28f

/** ስንክሳር — the day's synaxarium commemorations for [epochDay]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynaxariumScreen(epochDay: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val date = remember(epochDay) { LocalDate.ofEpochDay(epochDay) }
    val eth = remember(date) { EthiopianDate.from(date) }
    val entries by produceState<List<SynaxariumEntry>?>(initialValue = null, epochDay) {
        value = SynaxariumRepository.forDate(context, date)
    }
    val fontStep by SettingsRepository.fontStep(context).collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]

    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())
    val bookmarkedIds = remember(bookmarks) {
        bookmarks.filter { it.hourId == "sinksar_verse" }.mapTo(HashSet()) { it.sectionId }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.synaxariumTitle, style = MaterialTheme.typography.titleLarge)
                        Text(
                            formatEthiopian(date, s),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                actions = {
                    FontSizeActions(fontStep = fontStep, maxStep = FONT_STEPS_SP.lastIndex) { step ->
                        scope.launch { SettingsRepository.setFontStep(context, step) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        val list = entries
        when {
            list == null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary) }

            list.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    s.noSynaxariumToday,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 24.dp),
            ) {
                itemsIndexed(list) { i, entry ->
                    Column(Modifier.fillMaxWidth()) {
                        if (i > 0) {
                            Spacer(Modifier.height(20.dp))
                            HorizontalDivider(
                                modifier = Modifier.padding(bottom = 16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        } else {
                            Spacer(Modifier.height(12.dp))
                        }

                        val title = cleanSynaxariumText(entry.title)
                        if (title.isNotBlank()) {
                            EntryTitle(title)
                        }

                        if (isScriptureEntry(entry.title)) {
                            // Keyed by the Ethiopian date, not the Gregorian one:
                            // the same commemoration recurs every year and should
                            // stay bookmarked across years.
                            val sectionId = "sinksar:${eth.month}-${eth.day}:$i"
                            ScriptureBody(
                                rawText = entry.text,
                                fontSp = bodyFontSp,
                                bookmarked = sectionId in bookmarkedIds,
                                onToggleBookmark = {
                                    scope.launch {
                                        UserDataRepository.toggleBookmark(
                                            context,
                                            Bookmark(
                                                hourId = "sinksar_verse",
                                                hourName = s.bookmarkGroupSynaxarium,
                                                sectionId = sectionId,
                                                title = if (title.isNotBlank()) title else s.synaxariumTitle,
                                                subtitle = snippet(entry.text),
                                                route = "synaxarium/$epochDay",
                                            ),
                                        )
                                    }
                                },
                            )
                        } else {
                            // Sequential Ge'ez numbering, except where the source
                            // carries its own list numbers — those win (and resync
                            // the counter) so meaningful numbering isn't rewritten.
                            var n = 0
                            parseSynaxarium(entry.text).forEach { para ->
                                when (para.kind) {
                                    SynaxariumParaKind.NARRATIVE -> {
                                        n = para.sourceNumber ?: (n + 1)
                                        NarrativePara(n, para.text, bodyFontSp)
                                    }
                                    SynaxariumParaKind.ARKE_LABEL -> ArkeLabel(para.text)
                                    SynaxariumParaKind.ARKE_VERSE -> ArkeVerse(para.text, bodyFontSp)
                                }
                            }
                        }
                    }
                }
                item { ClosingPrayer(bodyFontSp) }
                item { Spacer(Modifier.height(48.dp)) }
            }
        }
    }
}

/**
 * The fixed closing ጸሎት, appended once after the day's commemorations and set
 * apart by a divider. Holy names are drawn in red.
 */
@Composable
private fun ClosingPrayer(fontSp: Int) {
    // The same fixed prayer closes every day, so it reads as a coda rather than
    // content: smaller than the body and tightly leaded, to keep it from
    // claiming a screenful at the end of each ስንክሳር.
    val style = readingBodyStyle(fontSp, CLOSING_LINE_HEIGHT).let {
        it.copy(fontSize = it.fontSize * CLOSING_FONT_SCALE, lineHeight = it.lineHeight * CLOSING_FONT_SCALE)
    }
    Spacer(Modifier.height(20.dp))
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        SYNAXARIUM_CLOSING_STANZAS.forEach { stanza ->
            Text(
                text = highlightHolyNames(stanza, ArkeRed),
                style = style,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Justify,
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

/** Centered gold commemoration title. */
@Composable
private fun EntryTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.inReadingFont(),
        color = MaterialTheme.colorScheme.secondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    )
}

/** A numbered narrative paragraph: inline gold Ge'ez numeral, justified prose
 *  spanning the full width so the text block stays centered on the page. */
@Composable
private fun NarrativePara(number: Int, text: String, fontSp: Int) {
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
        textAlign = TextAlign.Justify,
        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
    )
}

/** The centered "አርኬ" heading above the hymn. */
@Composable
private fun ArkeLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.inReadingFont(),
        color = ArkeRed,
        textAlign = TextAlign.Center,
        letterSpacing = 6.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp),
    )
}

/** An arke verse — italic, centered, red. */
@Composable
private fun ArkeVerse(text: String, fontSp: Int) {
    Text(
        text = text,
        style = readingBodyStyle(fontSp, ArkeLineHeight).copy(fontStyle = FontStyle.Italic),
        color = ArkeRed,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
    )
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleBookmark) {
            Icon(
                imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (bookmarked) s.removeAction else s.bookmarkAction,
                tint = if (bookmarked) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    // A few scripture entries carry an embedded አርኬ hymn after the quotation —
    // the quote goes in the card, the hymn keeps its red centered styling below.
    val paras = parseSynaxarium(rawText)
    val quote = paras.filter { it.kind == SynaxariumParaKind.NARRATIVE }
        .joinToString("\n\n") { it.text }
    Text(
        text = quote,
        style = readingBodyStyle(fontSp),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Justify,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    )
    paras.forEach { para ->
        when (para.kind) {
            SynaxariumParaKind.ARKE_LABEL -> ArkeLabel(para.text)
            SynaxariumParaKind.ARKE_VERSE -> ArkeVerse(para.text, fontSp)
            SynaxariumParaKind.NARRATIVE -> Unit
        }
    }
}

/** A short one-line preview of an entry's body for the bookmarks list. */
private fun snippet(rawText: String): String {
    val clean = cleanSynaxariumText(rawText.replace('\n', ' '))
    return if (clean.length > 90) clean.take(89).trimEnd() + "…" else clean
}
