package com.agpeya.app.ui.reading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.HighlightRepository
import com.agpeya.app.model.Section
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.readingVerseGap
import com.agpeya.app.ui.theme.scaledReadingSp
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.readingBodyStyle
import com.agpeya.app.ui.theme.sinqColors
import kotlinx.coroutines.launch

/**
 * Section rendering shared by the hour reader, the Psalter and the scripture
 * readers — one reading language across all of them.
 *
 * The layout follows what praying actually looks like: the title sits alone and
 * genuinely centred, and the text runs uninterrupted beneath it to the end of
 * the section. Bookmarking is the only control, and it stays at the head, muted
 * until it is set — nothing sits between the title and the first verse, and
 * nothing waits at the foot asking to be tapped.
 */
@Composable
internal fun SectionView(
    section: Section,
    bodyFontSp: Int,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    highlights: Map<String, String>,
    onVerseTap: (String) -> Unit,
    citedRange: IntRange = IntRange.EMPTY,
    selectedRange: IntRange = IntRange.EMPTY,
) {
    val s = LocalStrings.current
    val haptics = LocalHapticFeedback.current
    val motion = LocalMotion.current
    val bookmarkTint by animateColorAsState(
        // Muted, but not below the 3:1 an icon control needs against the page.
        targetValue = if (isBookmarked) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        animationSpec = motion.spec(Motion.standard),
        label = "bookmarkTint",
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(Spacing.huge))
        Box(Modifier.fillMaxWidth()) {
            Column(
                // Room for the bookmark on either side, so the title is centred
                // on the page rather than on "the title plus its buttons".
                modifier = Modifier.fillMaxWidth().padding(horizontal = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleLarge.inReadingFont(),
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                )
                section.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.inReadingFont(),
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleBookmark()
                },
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isBookmarked) s.removeAction else s.bookmarkAction,
                    tint = bookmarkTint,
                    modifier = Modifier.size(IconSize.medium),
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        HorizontalDivider(
            modifier = Modifier.width(32.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(Spacing.lg))
        VerseText(
            section = section,
            bodyFontSp = bodyFontSp,
            highlights = highlights,
            onVerseTap = onVerseTap,
            citedRange = citedRange,
            selectedRange = selectedRange,
        )
    }
}

@Composable
internal fun VerseText(
    section: Section,
    bodyFontSp: Int,
    highlights: Map<String, String>,
    onVerseTap: (String) -> Unit,
    citedRange: IntRange = IntRange.EMPTY,
    /** The user's live verse selection in THIS section (tap start, tap end);
     *  tinted stronger than a saved highlight so the pending run is unmissable. */
    selectedRange: IntRange = IntRange.EMPTY,
) {
    val markerColor = MaterialTheme.colorScheme.secondary
    val style = readingBodyStyle(bodyFontSp)
    val markerSize = scaledReadingSp(bodyFontSp) * 0.58f
    val citedShape = RoundedCornerShape(10.dp)
    val sinq = sinqColors
    // Verses have to stay visibly apart at 29sp as well as at 17sp, so the gap
    // scales with the text instead of being a fixed 4dp.
    val verseGap = readingVerseGap(bodyFontSp)

    // A ግጻዌ citation covers a contiguous run of verses, so it's drawn as ONE
    // tinted, bordered block rather than a separate box per verse. Verses are
    // still individual Texts inside it, keeping their own tap target and any
    // user highlight. (No SelectionContainer — it would swallow the verse taps.)
    @Composable
    fun verseLine(verseNumber: Int, verse: String, insideCitation: Boolean) {
        val verseKey = HighlightRepository.verseKey(section.id, verseNumber)
        val own = sinq.highlight(highlights[verseKey])
        val bg = when {
            // The live selection wins over a saved highlight: what the share
            // will carry has to be readable at a glance while picking.
            verseNumber in selectedRange -> markerColor.copy(alpha = 0.28f)
            own != Color.Transparent -> own
            insideCitation -> Color.Transparent          // the block behind it carries the tint
            else -> Color.Transparent
        }
        val annotated = remember(verse, verseNumber, markerColor, markerSize) {
            buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = markerColor,
                        fontSize = markerSize,
                        baselineShift = BaselineShift.Superscript,
                    )
                ) { append(geezNumeral(verseNumber)) }
                append(" ")
                append(verse)
            }
        }
        Text(
            text = annotated,
            style = style,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = verseGap / 2)
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .pointerInput(verseKey) {
                    detectTapGestures(onTap = { onVerseTap(verseKey) })
                }
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }

    Column(Modifier.fillMaxWidth()) {
        var i = 0
        while (i < section.verses.size) {
            val verseNumber = section.firstVerse + i
            val header = section.verseHeaders[verseNumber]
            if (header != null) {
                Text(
                    text = header,
                    style = MaterialTheme.typography.titleSmall.inReadingFont(),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (i == 0) 0.dp else Spacing.xl, bottom = Spacing.sm),
                    textAlign = TextAlign.Center,
                )
            }
            if (verseNumber in citedRange) {
                // Take the whole cited run — stopping at a stanza header, which
                // has to break out of the block to stay centred on its own.
                val start = i
                var end = i
                while (end + 1 < section.verses.size &&
                    (section.firstVerse + end + 1) in citedRange &&
                    section.verseHeaders[section.firstVerse + end + 1] == null
                ) end++
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xxs)
                        .clip(citedShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f), citedShape)
                        .padding(vertical = Spacing.xs),
                ) {
                    for (k in start..end) {
                        verseLine(section.firstVerse + k, section.verses[k], insideCitation = true)
                    }
                }
                i = end + 1
            } else {
                verseLine(verseNumber, section.verses[i], insideCitation = false)
                i++
            }
        }
    }
}

/**
 * The A− / A+ font-size stepper for a reader's app bar. Coloured with the gold
 * secondary so it stays legible on both grounds — the TextButton default,
 * primary, is a deep green that disappears on the dark-theme background.
 */
@Composable
fun FontSizeActions(fontStep: Int, maxStep: Int, onChange: (Int) -> Unit) {
    val colors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.secondary,
        disabledContentColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f),
    )
    TextButton(onClick = { onChange(fontStep - 1) }, enabled = fontStep > 0, colors = colors) { Text("A−") }
    TextButton(onClick = { onChange(fontStep + 1) }, enabled = fontStep < maxStep, colors = colors) { Text("A+") }
}

/**
 * The bar that appears when a verse is tapped: pick a highlight, copy, share, or
 * clear. It slides up from the bottom edge — the shortest possible distance —
 * and every action closes it, so it is never something to dismiss twice.
 */
@Composable
internal fun HighlightBar(
    visible: Boolean,
    currentColor: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** The tapped verse, ready to copy or share; null hides those actions. */
    shareText: String? = null,
    /** The same verse as a card payload; null hides the share-as-image action. */
    shareImage: com.agpeya.app.ui.common.SharePayload? = null,
) {
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(motion.spec(Motion.standard)) { it } + fadeIn(motion.spec(Motion.fast)),
        exit = slideOutVertically(motion.spec(Motion.fast)) { it } + fadeOut(motion.spec(Motion.fast)),
    ) {
        val s = LocalStrings.current
        val haptics = LocalHapticFeedback.current
        val sinq = sinqColors
        Surface(
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HighlightRepository.COLOR_KEYS.forEach { key ->
                    val selected = key == currentColor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(highlightSwatch(key, sinq.highlight(key)))
                            .then(
                                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .semantics { contentDescription = s.highlight }
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPick(key)
                            },
                    )
                }
                Spacer(Modifier.weight(1f))
                if (shareText != null) {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    IconButton(onClick = { com.agpeya.app.ui.common.Sharing.copy(ctx, shareText, s) }) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = s.copyAction,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.medium),
                        )
                    }
                    IconButton(onClick = { com.agpeya.app.ui.common.Sharing.share(ctx, shareText) }) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = s.shareAction,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.medium),
                        )
                    }
                    if (shareImage != null) {
                        val scope = androidx.compose.runtime.rememberCoroutineScope()
                        IconButton(onClick = {
                            scope.launch { com.agpeya.app.ui.common.PassageShare.share(ctx, shareImage) }
                        }) {
                            Icon(
                                Icons.Outlined.Image,
                                contentDescription = s.shareAsImage,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(IconSize.medium),
                            )
                        }
                    }
                }
                IconButton(onClick = { onPick(null) }) {
                    Icon(
                        Icons.Outlined.FormatColorReset,
                        contentDescription = s.removeHighlight,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(IconSize.medium),
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = s.dismiss,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(IconSize.medium),
                    )
                }
            }
        }
    }
}

/**
 * The swatch shown in the picker. Opaque, unlike the tint it applies: a 35%
 * yellow drawn on the sheet would read as "off" rather than as a colour choice.
 */
private fun highlightSwatch(key: String?, tint: Color): Color =
    if (tint == Color.Transparent) Color.Gray else tint.copy(alpha = 1f)

/**
 * The text behind a verse key ("<sectionId>:<n>"), formatted for sharing:
 * the section title, then the verse with its Ge'ez numeral.
 */
/**
 * The share bar for readers whose text has no highlight layer (ስንክሳር, ውዳሴ,
 * the NT reader): the same slide-up surface as [HighlightBar], carrying only
 * dismiss / copy / share / share-as-image for the current selection.
 */
@Composable
internal fun SelectionShareBar(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    shareText: String? = null,
    shareImage: com.agpeya.app.ui.common.SharePayload? = null,
) {
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(motion.spec(Motion.standard)) { it } + fadeIn(motion.spec(Motion.fast)),
        exit = slideOutVertically(motion.spec(Motion.fast)) { it } + fadeOut(motion.spec(Motion.fast)),
    ) {
        val s = LocalStrings.current
        Surface(
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = s.cancel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(IconSize.medium),
                    )
                }
                Spacer(Modifier.weight(1f))
                val ctx = androidx.compose.ui.platform.LocalContext.current
                if (shareText != null) {
                    IconButton(onClick = { com.agpeya.app.ui.common.Sharing.copy(ctx, shareText, s) }) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = s.copyAction,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.medium),
                        )
                    }
                    IconButton(onClick = { com.agpeya.app.ui.common.Sharing.share(ctx, shareText) }) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = s.shareAction,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.medium),
                        )
                    }
                }
                if (shareImage != null) {
                    val scope = androidx.compose.runtime.rememberCoroutineScope()
                    IconButton(onClick = {
                        scope.launch { com.agpeya.app.ui.common.PassageShare.share(ctx, shareImage) }
                    }) {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = s.shareAsImage,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.medium),
                        )
                    }
                }
            }
        }
    }
}

/** The int-pair selection used by paragraph/verse readers without section ids:
 *  -1 anchors nothing; a tap anchors, later taps move the end. Order-free. */
internal fun advanceFlatSelection(anchor: Int, tapped: Int): Pair<Int, Int> =
    if (anchor < 0) tapped to -1 else anchor to tapped

internal fun flatSelectionRange(anchor: Int, end: Int): IntRange =
    if (anchor < 0) IntRange.EMPTY
    else minOf(anchor, if (end < 0) anchor else end)..maxOf(anchor, if (end < 0) anchor else end)

internal fun verseShareText(
    sections: List<Section>,
    verseKey: String?,
    endKey: String? = null,
): String? {
    val (section, range) = resolveSelection(sections, verseKey, endKey) ?: return null
    return "${section.title}\n${versesBody(section, range)}"
}

/** The verse-key pair after a tap: same section extends the run to the tapped
 *  verse ("moving the cursor"); anywhere else starts over at the tap. */
internal fun advanceSelection(start: String?, tapped: String): Pair<String, String?> =
    if (start != null && start.substringBeforeLast(':') == tapped.substringBeforeLast(':'))
        start to tapped
    else tapped to null

/** The selected verse numbers within [section], or empty when the selection
 *  lives elsewhere. Order-free: an end tapped above the anchor still selects. */
internal fun selectionRangeFor(section: Section, startKey: String?, endKey: String?): IntRange {
    if (startKey == null || startKey.substringBeforeLast(':') != section.id) return IntRange.EMPTY
    val a = startKey.substringAfterLast(':').toIntOrNull() ?: return IntRange.EMPTY
    val b = endKey?.takeIf { it.substringBeforeLast(':') == section.id }
        ?.substringAfterLast(':')?.toIntOrNull() ?: a
    return minOf(a, b)..maxOf(a, b)
}

/** Every verse key in the selection, for applying a highlight to the run. */
internal fun selectionKeys(sections: List<Section>, startKey: String?, endKey: String?): List<String> {
    val (section, range) = resolveSelection(sections, startKey, endKey) ?: return emptyList()
    return range.mapNotNull { n ->
        if (section.verses.getOrNull(n - section.firstVerse) != null)
            HighlightRepository.verseKey(section.id, n)
        else null
    }
}

private fun resolveSelection(
    sections: List<Section>,
    startKey: String?,
    endKey: String?,
): Pair<Section, IntRange>? {
    if (startKey == null) return null
    val section = sections.firstOrNull { it.id == startKey.substringBeforeLast(':') } ?: return null
    val range = selectionRangeFor(section, startKey, endKey)
    if (range.isEmpty()) return null
    return section to range
}

/** The selected verses, each keeping its Ge'ez numeral, one per line. */
private fun versesBody(section: Section, range: IntRange): String =
    range.mapNotNull { n ->
        section.verses.getOrNull(n - section.firstVerse)?.let { "${geezNumeral(n)}  $it" }
    }.joinToString("\n")

/**
 * The same verse as a [com.agpeya.app.ui.common.SharePayload] for the PNG card:
 * the section title carries the heading, [kicker] names the book it came from
 * (the hour, the Psalter), and the verse keeps its Ge'ez numeral.
 */
internal fun versePayload(
    sections: List<Section>,
    verseKey: String?,
    kicker: String?,
    endKey: String? = null,
): com.agpeya.app.ui.common.SharePayload? {
    val (section, range) = resolveSelection(sections, verseKey, endKey) ?: return null
    return com.agpeya.app.ui.common.SharePayload(
        body = versesBody(section, range),
        kicker = kicker,
        title = section.title,
    )
}
