package com.agpeya.app.ui.gitsawe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.agpeya.app.ui.theme.IconSize
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
import com.agpeya.app.data.MisbakLanguage
import com.agpeya.app.data.ScriptureRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.SharePayload
import com.agpeya.app.ui.common.DoorChip
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.StatePanel
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
 * unified Bible bundle — the same source the full reader uses.
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
    /**
     * The ምስባክ exactly as the ግጻዌ prints it, and its Amharic where the book
     * gives one. When this is present it *is* the passage: see [resolvePassage]
     * for why the cited range is not.
     */
    chant: String? = null,
    chantAmharic: String? = null,
    onBack: () -> Unit,
    /** Opens Catena's page for the cited verse, inside the app. */
    onOpenCatena: (route: String) -> Unit = {},
    onWriteNote: (route: String, label: String) -> Unit,
    onOpenBook: () -> Unit,
    onOpenChapter: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val fontStep by SettingsRepository.fontStep(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]
    val misbakLanguage by SettingsRepository.misbakLanguage(context)
        .collectAsState(initial = MisbakLanguage.GEEZ)
    // Read here, not in the click lambda: sinqColors is a @Composable getter.
    val tabToolbar = com.agpeya.app.ui.theme.sinqColors.hero
    val tabOnToolbar = com.agpeya.app.ui.theme.sinqColors.onHero

    // `loaded` distinguishes "still resolving" from "genuinely not bundled":
    // the first shows the spinner, the second an honest empty state.
    val state by produceState<Pair<Boolean, Passage?>>(
        false to null, psalm, bookKey, chapter, start, end, misbakLanguage, chant, chantAmharic,
    ) {
        // The ምስባክ the ግጻዌ prints wins over the verses it cites.
        //
        // A ምስባክ is three lines of chant. It begins part-way through a verse,
        // ends part-way through another, drops the clauses between, and follows
        // its own recension — መዓልተ where the Psalter has መዐልተ, and elsewhere a
        // different word order altogether. Slicing whole verses out of the
        // Psalter for it therefore gives the wrong text and too much of it: over
        // the year's 1,312 citable ምስባክ the sliced range is half again as long
        // as the chant, more than twice as long in one of every six, and at
        // መዝሙር ፶ it runs 592 characters where the chant is 68.
        //
        // The citation is still right, and still worth keeping — it is what the
        // doors at the foot of this page and the Catena link are built from. It
        // just is not the text.
        // Only the ምስባክ. The ወንጌል and the deacons' readings print an incipit —
        // one opening phrase — and for those the cited range really is the text.
        val printed = if (role == MSBAK_ROLE) {
            if (misbakLanguage == MisbakLanguage.GEEZ) chant else chantAmharic ?: chant
        } else null
        value = true to (
            printed?.takeIf { it.isNotBlank() }?.let { text ->
                Passage(
                    bookName = "መዝሙረ ዳዊት".takeIf { psalm >= 1 } ?: "",
                    refLine = refLine(psalm, start.takeIf { it >= 1 }, end.takeIf { it >= 1 }),
                    // n = 0 draws no number: the ምስባክ is sung as three lines,
                    // and the lines are not whole verses to number.
                    verses = text.split("።").map { it.trim() }.filter { it.isNotEmpty() }
                        .map { PassageVerse(n = 0, text = "$it ።") },
                )
            } ?: resolvePassage(
                context, psalm, bookKey, chapter, start, end,
                psalmGeez = misbakLanguage == MisbakLanguage.GEEZ,
            )
            )
    }
    val (loaded, passage) = state
    val isPsalm = psalm >= 1

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = passage?.bookName ?: "",
                // The lectionary role is the page's liturgical context — the
                // same gold accent line the ግጻዌ page itself uses for seasons.
                accentLine = passage?.refLine?.let { ref ->
                    listOfNotNull(ref, role?.takeIf { it.isNotBlank() }).joinToString(" · ")
                },
                onBack = onBack,
                actions = {
                    com.agpeya.app.ui.common.ReaderToolsMenu(
                        fontStep = fontStep,
                        maxFontStep = FONT_STEPS_SP.lastIndex,
                        onFontChange = { step -> scope.launch { SettingsRepository.setFontStep(context, step) } },
                        // The passage is named in the note so it still reads
                        // as being about something years later, and the route
                        // takes the person straight back to it.
                        onWriteNote = passage?.let { p ->
                            {
                                onWriteNote(
                                    "gitsawePassage?psalm=$psalm&book=${bookKey.orEmpty()}" +
                                        "&chapter=$chapter&start=$start&end=$end&role=${role.orEmpty()}",
                                    listOfNotNull(p.bookName, p.refLine).joinToString(" · "),
                                )
                            }
                        },
                        secondaryActionLabel = if (isPsalm) {
                            if (misbakLanguage == MisbakLanguage.GEEZ) s.wudaseLangAmharic else s.wudaseLangGeez
                        } else null,
                        onSecondaryAction = if (isPsalm) ({
                            scope.launch {
                                SettingsRepository.setMisbakLanguage(
                                    context,
                                    if (misbakLanguage == MisbakLanguage.GEEZ) MisbakLanguage.AMHARIC else MisbakLanguage.GEEZ,
                                )
                            }
                        }) else null,
                        shareEnabled = passage != null,
                        sharePayload = {
                            passage?.let { p ->
                                SharePayload(
                                    body = p.verses.joinToString("\n") { "${geezNumeral(it.n)}  ${it.text}" },
                                    kicker = role?.takeIf { it.isNotBlank() } ?: s.gitsaweKicker,
                                    title = "${p.bookName} ${p.refLine}",
                                )
                            }
                        },
                    )
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
                                modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xxs),
                            )
                        }
                        val annotated = buildAnnotatedString {
                            // A ምስባክ line carries no number — see resolvePassage.
                            if (verse.n > 0) {
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = scaledReadingSp(bodyFontSp) * 0.58f,
                                        baselineShift = BaselineShift.Superscript,
                                    ),
                                ) { append(geezNumeral(verse.n)) }
                                append("  ")
                            }
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
                    // Where to go on from here, as a ቀጥል strip rather than a
                    // stack of rows: two doors on a psalm and three on a
                    // gospel, which as full-width rows made the foot of a
                    // short passage longer than the passage. Chips wrap, so
                    // the third one costs a line only when it needs one.
                    item(key = "doors") {
                        Spacer(Modifier.height(Spacing.lg))
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
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
                            if (isPsalm) {
                                DoorChip(
                                    icon = Icons.Outlined.AutoStories,
                                    label = "${s.goToPsalm} ${geezNumeral(psalm)}",
                                    onClick = { onOpenChapter(misbakLanguage == MisbakLanguage.GEEZ) },
                                )
                            } else {
                                DoorChip(
                                    icon = Icons.Outlined.AutoStories,
                                    label = "${s.goToChapter} ${geezNumeral(chapter)}",
                                    onClick = { onOpenChapter(false) },
                                )
                                DoorChip(
                                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                                    label = s.goToBook,
                                    onClick = onOpenBook,
                                )
                            }
                            // The Fathers on the first verse cited. The Psalter is
                            // numbered as the Church numbers it and Catena numbers
                            // it as the Masoretic text does, so the psalm carries a
                            // translation that CatenaLink holds; a book Catena does
                            // not reach simply has no chip.
                            val catena = if (isPsalm) {
                                com.agpeya.app.data.CatenaLink.url("psalms", psalm, start.coerceAtLeast(1))
                            } else {
                                bookKey?.let {
                                    com.agpeya.app.data.CatenaLink.url(it, chapter, start.coerceAtLeast(1))
                                }
                            }
                            catena?.let { url ->
                                DoorChip(
                                    icon = Icons.Outlined.HistoryEdu,
                                    label = s.commentaryAction,
                                    external = true,
                                    onClick = {
                                        onOpenCatena(
                                            com.agpeya.app.data.CatenaLink.route(
                                                url,
                                                passage?.let { "${it.bookName} ${it.refLine}" }.orEmpty(),
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.xl))
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
/** The lectionary's own label for the chant, as the ግጻዌ tables spell it. */
private const val MSBAK_ROLE = "ምስባክ"

private suspend fun resolvePassage(
    context: android.content.Context,
    psalm: Int,
    bookKey: String?,
    chapter: Int,
    start: Int,
    end: Int,
    psalmGeez: Boolean,
): Passage? {
    if (psalm >= 1) {
        val section = ScriptureRepository.psalms(context, geez = psalmGeez)
            .find { it.number == psalm } ?: return null
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

