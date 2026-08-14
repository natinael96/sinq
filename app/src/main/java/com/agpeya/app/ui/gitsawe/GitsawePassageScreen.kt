package com.agpeya.app.ui.gitsawe

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.ScriptureRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.common.SharePayload
import com.agpeya.app.ui.common.ShareMenuAction
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.reading.FontSizeActions
import com.agpeya.app.ui.reading.ReadingColumn
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.readingBodyStyle
import com.agpeya.app.ui.theme.readingVerseGap
import com.agpeya.app.ui.theme.scaledReadingSp
import kotlinx.coroutines.launch

private val FONT_STEPS_SP = SettingsRepository.FONT_STEPS_SP

/** One verse of the resolved passage, ready to draw. */
private data class PassageVerse(val n: Int, val text: String, val header: String? = null)

/** The resolved passage: where it is, and the verses themselves. */
private data class Passage(
    val bookName: String,
    val refLine: String,
    val verses: List<PassageVerse>,
)

/**
 * The page a ግጻዌ section opens on: only the cited passage, in the reading
 * face — no full reader, no chapter strip, no highlight to hunt for. The role
 * (ምስባክ, ወንጌል …) sits in the top bar as the page's liturgical context, and
 * two rows at the foot lead out: the book, and the chapter that holds the
 * passage. Psalms resolve from the bundled Psalter, everything else from the
 * bundled New Testament — the same sources the full readers use.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GitsawePassageScreen(
    psalm: Int,
    bookKey: String?,
    chapter: Int,
    start: Int,
    end: Int,
    role: String?,
    onBack: () -> Unit,
    onOpenBook: () -> Unit,
    onOpenChapter: () -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val fontStep by SettingsRepository.fontStep(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]

    // `loaded` distinguishes "still resolving" from "genuinely not bundled":
    // the first shows the spinner, the second an honest empty state.
    val state by produceState<Pair<Boolean, Passage?>>(false to null, psalm, bookKey, chapter, start, end) {
        value = true to resolvePassage(context, psalm, bookKey, chapter, start, end)
    }
    val (loaded, passage) = state
    val isPsalm = psalm >= 1

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = passage?.bookName ?: "",
                subtitle = passage?.refLine,
                // The lectionary role is the page's liturgical context — the
                // same gold accent line the ግጻዌ page itself uses for seasons.
                accentLine = role?.takeIf { it.isNotBlank() },
                onBack = onBack,
                actions = {
                    ShareMenuAction(enabled = passage != null, payload = {
                        passage?.let { p ->
                            SharePayload(
                                body = p.verses.joinToString("\n") { "${geezNumeral(it.n)}  ${it.text}" },
                                kicker = role?.takeIf { it.isNotBlank() } ?: s.gitsaweKicker,
                                title = "${p.bookName} ${p.refLine}",
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
        when {
            !loaded -> LoadingPanel(Modifier.padding(innerPadding))
            passage == null -> Column(Modifier.padding(innerPadding)) {
                StatePanel(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = s.gitsaweOpenNotAvailable,
                )
            }
            else -> {
                val verseGap = readingVerseGap(bodyFontSp)
                ReadingColumn(innerPadding = innerPadding) {
                    item(key = "top") { Spacer(Modifier.height(Spacing.sm)) }
                    items(passage.verses.size, key = { passage.verses[it].n }) { i ->
                        val verse = passage.verses[i]
                        verse.header?.let { header ->
                            // A stanza heading (Psalm 118's acrostic letters),
                            // kept when its verse falls inside the citation.
                            Text(
                                header,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
                            )
                        }
                        val annotated = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = scaledReadingSp(bodyFontSp) * 0.58f,
                                    baselineShift = BaselineShift.Superscript,
                                ),
                            ) { append(geezNumeral(verse.n)) }
                            append("  ")
                            append(verse.text)
                        }
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                text = annotated,
                                style = readingBodyStyle(bodyFontSp),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = verseGap / 2),
                            )
                        }
                    }
                    // The two doors out, at the same rank as every other NavRow.
                    item(key = "doors") {
                        Spacer(Modifier.height(Spacing.xl))
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(Spacing.sm))
                        NavRow(
                            title = s.goToBook,
                            subtitle = passage.bookName,
                            onClick = onOpenBook,
                        )
                        NavRow(
                            title = if (isPsalm) s.goToPsalm else s.goToChapter,
                            subtitle = "${passage.bookName} ${geezNumeral(if (isPsalm) psalm else chapter)}",
                            onClick = onOpenChapter,
                        )
                        Spacer(Modifier.height(Spacing.huge))
                    }
                }
            }
        }
    }
}

/**
 * Resolve the cited verses from the bundled content. Ranges are clamped the
 * way [ScriptureRepository.passage] clamps them — a citation past the real end
 * shows what exists rather than an empty page.
 */
private suspend fun resolvePassage(
    context: android.content.Context,
    psalm: Int,
    bookKey: String?,
    chapter: Int,
    start: Int,
    end: Int,
): Passage? {
    if (psalm >= 1) {
        val section = ContentRepository.psalter(context).find { it.number == psalm } ?: return null
        val total = section.verses.size
        if (total == 0) return null
        val lo: Int
        val hi: Int
        if (start >= 1) {
            lo = start.coerceAtMost(total)
            // No explicit end = through the psalm's last verse, same as the
            // NT rule in ScriptureRepository.passage — never a single verse
            // unless the citation itself says so.
            hi = (if (end >= 1) end else total).coerceIn(lo, total)
        } else {
            lo = 1; hi = total
        }
        val verses = (lo..hi).map { n ->
            PassageVerse(n = n, text = section.verses[n - 1], header = section.verseHeaders[n])
        }
        return Passage(
            bookName = "መዝሙረ ዳዊት",
            refLine = refLine(psalm, if (start >= 1) lo else null, if (start >= 1) hi else null),
            verses = verses,
        )
    }
    bookKey ?: return null
    val book = ScriptureRepository.book(context, bookKey) ?: return null
    val cited = ScriptureRepository.passage(
        context, bookKey, chapter,
        start.takeIf { it >= 1 }, end.takeIf { it >= 1 },
    ) ?: return null
    if (cited.isEmpty()) return null
    return Passage(
        bookName = book.nameAm,
        refLine = refLine(
            chapter,
            cited.first().n.takeIf { start >= 1 },
            cited.last().n.takeIf { start >= 1 && cited.size > 1 },
        ),
        verses = cited.map { PassageVerse(it.n, it.text) },
    )
}

/** "፷፬፥፲፩–፲፪" — Ge'ez chapter፥verse(–verse), verses omitted for a whole chapter. */
private fun refLine(chapter: Int, lo: Int?, hi: Int?): String = buildString {
    append(geezNumeral(chapter))
    if (lo != null) {
        append("፥"); append(geezNumeral(lo))
        if (hi != null && hi != lo) { append("–"); append(geezNumeral(hi)) }
    }
}
