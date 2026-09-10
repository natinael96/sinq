package com.agpeya.app.ui.mahlet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import com.agpeya.app.data.BookRepository
import com.agpeya.app.data.MahletRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.MahletKind
import com.agpeya.app.model.MahletOrder
import com.agpeya.app.model.MahletPart
import com.agpeya.app.ui.common.Passage
import com.agpeya.app.ui.common.SelectPill
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.reading.ReadingColumn
import com.agpeya.app.ui.reading.SelectionBar
import com.agpeya.app.ui.reading.advanceFlatSelection
import com.agpeya.app.ui.reading.flatSelectionRange
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.readingBodyStyle
import com.agpeya.app.ui.theme.sinqColors

/**
 * One feast's order of service, sung in order.
 *
 * The book writes each order as a numbered run of movements — ነግሥ, ዚቅ, ወረብ,
 * አመላለስ, መልክአ ሥላሴ — each with its Ge'ez verse. That is the same shape as an
 * hour of the Agpeya, so it reads the same way and the selection bar comes with
 * it: a ዚቅ copies, shares and becomes a card like any other passage.
 *
 * Where a movement is named after a book on the ሌሎች መጻሕፍት shelf, its name is a
 * door: the ማኅሌት gives the stanza the feast appoints, and the shelf has the
 * whole hymn.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun MahletScreen(
    orderId: String,
    onBack: () -> Unit,
    onOpenBook: (String, Int, Int) -> Unit = { _, _, _ -> },
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val fontStep by SettingsRepository.fontStep(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = SettingsRepository.FONT_STEPS_SP[
        fontStep.coerceIn(0, SettingsRepository.FONT_STEPS_SP.lastIndex),
    ]

    // The whole feast, not only the order that was tapped: the ዋዜማ and the
    // ማኅሌት belong to one night and one morning, so they are two tabs rather
    // than two pages a reader has to go back and forth between.
    val orders by produceState(emptyList<MahletOrder>(), orderId) {
        val opened = MahletRepository.order(context, orderId) ?: return@produceState
        val sameFeast = MahletRepository.month(context, opened.month ?: 0)
            .filter { it.feast.trim() == opened.feast.trim() && it.day == opened.day }
            .sortedBy { MahletKind.rank(it.kind) }
        value = sameFeast.ifEmpty { listOf(opened) }
    }
    var tab by rememberSaveable(orderId) { mutableIntStateOf(0) }
    val shown = orders.getOrNull(tab.coerceIn(0, (orders.size - 1).coerceAtLeast(0)))
    // Which text of the order is on the page: -1 the book's own (or the edition
    // standing in for it), n the nth further edition. An edition replaces the
    // text wholesale — it is never read on after the book's.
    var edition by rememberSaveable(orderId, tab) { mutableIntStateOf(-1) }
    val editions = shown?.versions.orEmpty()
    val parts = if (edition in editions.indices) editions[edition].parts else shown?.parts.orEmpty()

    // Resolved once per feast rather than per part: 137 of the book's movements
    // are named after a book on the shelf. Keyed by name AND opening words, not
    // by name alone — one feast can sing several stanzas of the same መልክእ, and
    // each of them opens the hymn at a different place.
    val bookByPart by produceState(emptyMap<String, BookRepository.BookLocation>(), orders) {
        val wanted = orders.flatMap { it.parts + it.versions.flatMap { v -> v.parts } }
            .filter { it.key.isNotBlank() && it.verse.isNotBlank() }
            .distinctBy { partAnchor(it.key, it.verse) }
        value = wanted.mapNotNull { part ->
            BookRepository.byPartVerse(context, part.key, part.verse)
                ?.let { partAnchor(part.key, part.verse) to it }
        }.toMap()
    }

    var selA by rememberSaveable(orderId, tab) { mutableIntStateOf(-1) }
    var selB by rememberSaveable(orderId, tab) { mutableIntStateOf(-1) }
    val selRange = flatSelectionRange(selA, selB)
    val selBody = if (selRange.isEmpty()) null
    else parts.filterIndexed { i, _ -> i in selRange }
        .joinToString("\n\n") { it.verse }
        .ifBlank { null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.mahletTitle,
                subtitle = shown?.feast,
                // Which book this order came from, when it is not the spine —
                // or which edition is open, since that is a different text.
                accentLine = editions.getOrNull(edition)?.let { it.title ?: s.mahletEdition(edition + 1) }
                    ?: shown?.source,
                onBack = onBack,
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
                            shown?.feast,
                            parts.getOrNull(selRange.first)?.key?.takeIf { k -> k.isNotBlank() },
                        ).joinToString("  ·  "),
                    )
                },
                imageKicker = s.mahletTitle,
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize()) {
            ReadingColumn(innerPadding = inner) {
                if (orders.size > 1) {
                    item(key = "tabs") {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            orders.forEachIndexed { index, order ->
                                SelectPill(
                                    // Not ነግሥ: that is also the name of a part,
                                    // sixty times over, so the tab and a header
                                    // inside it would read as the same word.
                                    label = if (order.kind == MahletKind.MAHLET) s.mahletTitle
                                    else s.mahletKindLabel(order.kind),
                                    selected = index == tab,
                                    onClick = { tab = index; selA = -1; selB = -1 },
                                )
                            }
                        }
                    }
                }
                // The editions, where there are any: the book's text and then
                // each Telegram edition, to be read one in place of another.
                // The merge is explicit that they are choices — a version may
                // be a fragment, and none is the preferred wording — so they
                // are pills, not a longer page.
                if (editions.isNotEmpty()) {
                    item(key = "editions") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = Spacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            SelectPill(
                                label = shown?.source ?: s.mahletBookText,
                                selected = edition < 0,
                                onClick = { edition = -1; selA = -1; selB = -1 },
                            )
                            // Editions count among themselves and translations
                            // among themselves: "እትም ፪" is the second Ge'ez
                            // text, "ትርጉም ፪" the second Amharic, and the
                            // number never skips because the other kind sat
                            // between.
                            val nTranslations = editions.count { it.translation }
                            var nthEdition = 0
                            var nthTranslation = 0
                            editions.forEachIndexed { index, v ->
                                val label = if (v.translation) {
                                    nthTranslation += 1
                                    s.mahletTranslation(nthTranslation, nTranslations)
                                } else {
                                    nthEdition += 1
                                    s.mahletEdition(nthEdition)
                                }
                                SelectPill(
                                    label = label,
                                    selected = index == edition,
                                    onClick = { edition = index; selA = -1; selB = -1 },
                                )
                            }
                        }
                    }
                }
                item(key = "title") {
                    Text(
                        shown?.feast.orEmpty(),
                        style = MaterialTheme.typography.titleMedium.inReadingFont(),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                    Text(
                        s.mahletParts(parts.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = Spacing.md),
                    )
                }
                items(parts.size, key = { "$tab:$edition:$it" }) { index ->
                    MahletPartRow(
                        part = parts[index],
                        ordinal = index + 1,
                        total = parts.size,
                        bodyFontSp = bodyFontSp,
                        selected = index in selRange,
                        location = bookByPart[partAnchor(parts[index].key, parts[index].verse)],
                        onOpenBook = onOpenBook,
                        onTap = {
                            val (a, b) = advanceFlatSelection(selA, index)
                            selA = a
                            selB = b
                        },
                    )
                }
                item { Spacer(Modifier.height(Spacing.huge)) }
            }
        }
    }
}

/**
 * The three that recur between movements rather than carrying one.
 *
 * Half the book is these — ዚቅ four hundred times, ወረብ two hundred and eighty —
 * and they are also the shortest thing in it, because they answer rather than
 * carry. Set as peers of the movements they follow, they made the page a flat
 * list of one word repeated; set as what they are, the movements carry the
 * structure and the eye can find them.
 */
private val REFRAINS = Regex("ዚቅ|ወረብ|አመላለስ|እመላለስ|ዓዲ")

/**
 * One part: its name, then its verse.
 *
 * The name is the whole navigation and cannot do that job alone — inside a
 * single order one name repeats up to thirteen times — so the ordinal goes
 * beside it. The name is in the rubric colour, which is what the printed book
 * and the Ethiopic manuscripts before it use to mark a section.
 */
@Composable
private fun MahletPartRow(
    part: MahletPart,
    ordinal: Int,
    total: Int,
    bodyFontSp: Int,
    selected: Boolean,
    location: BookRepository.BookLocation?,
    onOpenBook: (String, Int, Int) -> Unit,
    onTap: () -> Unit,
) {
    val s = LocalStrings.current
    val refrain = part.key.isNotBlank() && REFRAINS.containsMatchIn(part.key)
    // A rubric is read, not sung: smaller, muted, and without a name, since
    // the instruction is its own heading.
    val rubric = part.rubric
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .background(
                if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f)
                else Color.Transparent,
            )
            .padding(
                start = if (refrain) Spacing.lg else Spacing.sm,
                end = Spacing.sm,
                top = if (refrain) Spacing.xs else Spacing.md,
                bottom = Spacing.xs,
            ),
    ) {
        // "ወይም" over a part the book offers in place of the one before it. Set
        // as a plain rubric, the choice would read as one more thing to sing.
        if (part.alternative) {
            Text(
                s.mahletOr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = Spacing.xxs),
            )
        }
        if (part.key.isNotBlank()) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    part.key,
                    style = MaterialTheme.typography.labelMedium,
                    color = sinqColors.arke,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    s.mahletNth(ordinal, total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (location != null) {
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        s.booksFullHymn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        modifier = Modifier.clickable {
                            onOpenBook(location.book.id, location.chapter, location.block)
                        },
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xxs))
        }
        Text(
            part.verse,
            style = when {
                rubric -> MaterialTheme.typography.bodyMedium.inReadingFont()
                refrain -> readingBodyStyle(bodyFontSp).copy(fontStyle = FontStyle.Italic)
                else -> readingBodyStyle(bodyFontSp)
            },
            color = if (refrain || rubric) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * What identifies a part for the shelf lookup: its name and how it opens.
 *
 * The name alone is not enough — a feast can sing several stanzas of one መልክእ,
 * and they are the same book at different verses.
 */
private fun partAnchor(key: String, verse: String): String = key + "\u0000" + verse.take(24)
