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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
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
import androidx.compose.ui.semantics.Role
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

/**
 * Section rendering shared by the hour reader, the Psalter and the scripture
 * readers — one reading language across all of them.
 *
 * The layout follows what praying actually looks like: the title sits alone and
 * genuinely centred, the text runs uninterrupted beneath it, and the two
 * controls are placed where each is needed. Bookmarking is a *before* action, so
 * it stays at the head of the section, muted until it is set. Marking a section
 * read is an *after* action, so it waits at the foot — which also means nothing
 * sits between the title and the first verse.
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
    /** Today's read-state for this section; null hides the control entirely
     *  (the Psalter and scripture aren't part of the daily hour cycle). */
    isRead: Boolean? = null,
    onToggleRead: () -> Unit = {},
) {
    val s = LocalStrings.current
    val haptics = LocalHapticFeedback.current
    val motion = LocalMotion.current
    val bookmarkTint by animateColorAsState(
        targetValue = if (isBookmarked) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
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
        )
        if (isRead != null) {
            Spacer(Modifier.height(Spacing.lg))
            MarkReadButton(isRead = isRead, onToggle = onToggleRead)
        }
    }
}

/**
 * The one control at the foot of a section. Quiet when the section is unread —
 * it should not compete with the last verse — and gold once it is done, so
 * scrolling back through an hour shows at a glance how far you got.
 */
@Composable
private fun MarkReadButton(isRead: Boolean, onToggle: () -> Unit) {
    val s = LocalStrings.current
    val motion = LocalMotion.current
    val haptics = LocalHapticFeedback.current
    val tint by animateColorAsState(
        targetValue = if (isRead) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        animationSpec = motion.spec(Motion.standard),
        label = "readTint",
    )
    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .clickable(
                role = Role.Checkbox,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle()
                },
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .semantics { contentDescription = s.markRead },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isRead) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(IconSize.small),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(s.markRead, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
internal fun VerseText(
    section: Section,
    bodyFontSp: Int,
    highlights: Map<String, String>,
    onVerseTap: (String) -> Unit,
    citedRange: IntRange = IntRange.EMPTY,
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
internal fun verseShareText(sections: List<Section>, verseKey: String?): String? {
    if (verseKey == null) return null
    val sectionId = verseKey.substringBeforeLast(':')
    val number = verseKey.substringAfterLast(':').toIntOrNull() ?: return null
    val section = sections.firstOrNull { it.id == sectionId } ?: return null
    val verse = section.verses.getOrNull(number - section.firstVerse) ?: return null
    return "${section.title}\n${geezNumeral(number)}  $verse"
}
