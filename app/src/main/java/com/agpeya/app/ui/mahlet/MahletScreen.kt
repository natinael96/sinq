package com.agpeya.app.ui.mahlet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    onOpenBook: (String) -> Unit = {},
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
            .sortedBy { it.kind != MahletKind.VIGIL }
        value = sameFeast.ifEmpty { listOf(opened) }
    }
    var tab by rememberSaveable(orderId) { mutableIntStateOf(0) }
    val shown = orders.getOrNull(tab.coerceIn(0, (orders.size - 1).coerceAtLeast(0)))
    val parts = shown?.parts.orEmpty()

    // Resolved once per feast rather than per part: 137 of the book's movements
    // are named after a book on the shelf.
    val bookByPart by produceState(emptyMap<String, String>(), orders) {
        val names = orders.flatMap { it.parts }.map { it.key }.filter { it.isNotBlank() }.distinct()
        value = names.mapNotNull { name ->
            BookRepository.byPartName(context, name)?.let { name to it.id }
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
                // Which book this order came from, when it is not the spine.
                accentLine = shown?.source,
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
                                    label = if (order.kind == MahletKind.VIGIL) s.mahletVigil
                                    else s.mahletTitle,
                                    selected = index == tab,
                                    onClick = { tab = index; selA = -1; selB = -1 },
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
                items(parts.size, key = { "$tab:$it" }) { index ->
                    MahletPartRow(
                        part = parts[index],
                        ordinal = index + 1,
                        total = parts.size,
                        bodyFontSp = bodyFontSp,
                        selected = index in selRange,
                        bookId = bookByPart[parts[index].key],
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
    bookId: String?,
    onOpenBook: (String) -> Unit,
    onTap: () -> Unit,
) {
    val s = LocalStrings.current
    val refrain = part.key.isNotBlank() && REFRAINS.containsMatchIn(part.key)
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
                if (bookId != null) {
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        s.booksFullHymn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        modifier = Modifier.clickable { onOpenBook(bookId) },
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xxs))
        }
        Text(
            part.verse,
            style = readingBodyStyle(bodyFontSp).let {
                if (refrain) it.copy(fontStyle = FontStyle.Italic) else it
            },
            color = if (refrain) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onBackground,
        )
    }
}
