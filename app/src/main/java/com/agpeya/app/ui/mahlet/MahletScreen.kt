package com.agpeya.app.ui.mahlet

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.Mahlet
import com.agpeya.app.model.MahletVerse
import com.agpeya.app.ui.common.Passage
import com.agpeya.app.ui.common.SelectPill
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.reading.ReadingColumn
import com.agpeya.app.ui.reading.SelectionBar
import com.agpeya.app.ui.reading.advanceFlatSelection
import com.agpeya.app.ui.reading.flatSelectionRange
import com.agpeya.app.ui.theme.readingBodyStyle
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import androidx.compose.foundation.layout.width
import com.agpeya.app.ui.theme.sinqColors

/**
 * ሥርዓተ ማኅሌት — one feast's order of service, sung in order.
 *
 * The book gives each feast a ዋዜማ the evening before and a ነግሥ at dawn, and
 * writes each as a numbered run of movements: ነግሥ, ዚቅ, ወረብ, አመላለስ, መልክአ ሥላሴ
 * and the rest, each with its Ge'ez verse. That is the same shape as an hour of
 * the Agpeya, so it reads the same way and the selection bar comes with it — a
 * ዚቅ copies, shares and becomes a card like any other passage.
 *
 * 227 KB of this has been in the bundle since the lectionary was imported,
 * parsed on demand and dropped, because nothing ever asked for it.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun MahletScreen(
    subFeastKey: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val fontStep by SettingsRepository.fontStep(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = SettingsRepository.FONT_STEPS_SP[
        fontStep.coerceIn(0, SettingsRepository.FONT_STEPS_SP.lastIndex),
    ]

    // The whole feast, not just the order that was tapped: ዋዜማ and ነግሥ belong
    // to one night and one morning, so they are two tabs rather than two pages.
    val pair by produceState(emptyList<GitsaweRepository.FeastMahlets>(), subFeastKey) {
        value = GitsaweRepository.mahletsOfFeastFor(context, subFeastKey)
    }
    val orders = pair.firstOrNull()?.orders.orEmpty()
    var tab by rememberSaveable(subFeastKey) { mutableIntStateOf(0) }
    val shown = orders.getOrNull(tab.coerceIn(0, (orders.size - 1).coerceAtLeast(0)))

    // Live paragraph selection, the same two-key model every reader uses.
    var selA by rememberSaveable(subFeastKey, tab) { mutableIntStateOf(-1) }
    var selB by rememberSaveable(subFeastKey, tab) { mutableIntStateOf(-1) }
    val selRange = flatSelectionRange(selA, selB)
    val parts = shown?.mahlet?.detail.orEmpty()
    val selBody = if (selRange.isEmpty()) null
    else parts.filterIndexed { i, _ -> i in selRange }
        .joinToString("\n\n") { it.verse }
        .ifBlank { null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.mahletTitle,
                subtitle = pair.firstOrNull()?.feastName,
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
                            shown?.mahlet?.title,
                            parts.getOrNull(selRange.first)?.key,
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
                                    // 35 times over, so the tab and the header
                                    // would read as the same word.
                                    label = if (order.isEve) s.mahletVigil else s.mahletTitle,
                                    selected = index == tab,
                                    onClick = { tab = index; selA = -1; selB = -1 },
                                )
                            }
                        }
                    }
                }
                item(key = "title") {
                    Text(
                        shown?.mahlet?.title.orEmpty(),
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
                    MahletPart(
                        part = parts[index],
                        ordinal = index + 1,
                        total = parts.size,
                        bodyFontSp = bodyFontSp,
                        selected = index in selRange,
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
 * Half the book is these: ዚቅ 187 times, ወረብ 136, አመላለስ 42, out of 732 parts.
 * They are also the shortest thing in it — a median of 66 characters against
 * 101 to 129 for everything else — because they answer rather than carry. Set
 * as peers of the movements they follow, they made the page a flat list of a
 * word repeated; set as what they are, the movements carry the structure.
 */
private val REFRAINS = Regex("ዚቅ|ወረብ|አመላለስ|እመላለስ")

/**
 * One part: its name, then its verse.
 *
 * The name is the whole navigation, and it cannot do that job alone — inside a
 * single order one name repeats up to thirteen times, so the ordinal goes
 * beside it. The name is in the rubric colour, which is what the printed book
 * and the Ethiopic manuscripts before it use to mark a section.
 */
@Composable
private fun MahletPart(
    part: MahletVerse,
    ordinal: Int,
    total: Int,
    bodyFontSp: Int,
    selected: Boolean,
    onTap: () -> Unit,
) {
    val s = LocalStrings.current
    val refrain = REFRAINS.containsMatchIn(part.key)
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .background(
                if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f)
                else androidx.compose.ui.graphics.Color.Transparent,
            )
            .padding(
                start = if (refrain) Spacing.lg else Spacing.sm,
                end = Spacing.sm,
                top = if (refrain) Spacing.xs else Spacing.md,
                bottom = Spacing.xs,
            ),
    ) {
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
        }
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            part.verse,
            style = readingBodyStyle(bodyFontSp).let {
                if (refrain) it.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) else it
            },
            color = if (refrain) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Used by the list screen to preview an order without opening it. */
internal fun Mahlet.partCount(): Int = detail.size
