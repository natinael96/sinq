package com.agpeya.app.ui.reading

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.HighlightRepository
import com.agpeya.app.model.Section
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.scaledReadingSp
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.readingBodyStyle

/**
 * Section rendering shared by the hour reader and the Psalter: title with
 * bookmark toggle, subtitle, and per-verse text with tap-to-highlight.
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
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleLarge.inReadingFont(),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = LocalStrings.current.bookmarkAction,
                    tint = if (isBookmarked) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        section.subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.inReadingFont(),
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.width(32.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
        Spacer(Modifier.height(16.dp))
        VerseText(
            section = section,
            bodyFontSp = bodyFontSp,
            highlights = highlights,
            onVerseTap = onVerseTap,
            citedRange = citedRange,
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
) {
    val markerColor = MaterialTheme.colorScheme.secondary
    val style = readingBodyStyle(bodyFontSp)
    // One Text per verse so each carries its own highlight background and tap
    // target. (No SelectionContainer — it would swallow the verse taps.)
    Column(Modifier.fillMaxWidth()) {
        section.verses.forEachIndexed { i, verse ->
            val verseNumber = section.firstVerse + i
            section.verseHeaders[verseNumber]?.let { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.titleSmall.inReadingFont(),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (i == 0) 0.dp else 20.dp, bottom = 6.dp),
                    textAlign = TextAlign.Center,
                )
            }
            val verseKey = HighlightRepository.verseKey(section.id, verseNumber)
            val cited = verseNumber in citedRange
            // A reading opened from ግጻዌ tints its cited verses in gold; a user's
            // own highlight still wins if they've coloured the verse themselves.
            val bg = highlightColor(highlights[verseKey]).takeIf { it != Color.Transparent }
                ?: if (cited) MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f) else Color.Transparent
            val markerSize = scaledReadingSp(bodyFontSp) * 0.58f
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .then(
                        if (cited) Modifier.border(
                            1.5.dp,
                            MaterialTheme.colorScheme.secondary,
                            RoundedCornerShape(8.dp),
                        ) else Modifier
                    )
                    .pointerInput(verseKey) {
                        detectTapGestures(onTap = { onVerseTap(verseKey) })
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** Semi-transparent highlight tints that read well on both light and dark backgrounds. */
internal fun highlightColor(key: String?): Color = when (key) {
    "yellow" -> Color(0x55E8C46B)
    "green" -> Color(0x554CAF50)
    "blue" -> Color(0x552196F3)
    "pink" -> Color(0x55E0529C)
    else -> Color.Transparent
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

@Composable
internal fun HighlightBar(
    visible: Boolean,
    currentColor: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        val s = LocalStrings.current
        Surface(
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HighlightRepository.COLOR_KEYS.forEach { key ->
                    val selected = key == currentColor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(highlightSwatch(key))
                            .then(
                                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable { onPick(key) },
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onPick(null) }) {
                    Icon(
                        Icons.Outlined.FormatColorReset,
                        contentDescription = s.removeHighlight,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = s.contents,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun highlightSwatch(key: String?): Color = when (key) {
    "yellow" -> Color(0xFFE8C46B)
    "green" -> Color(0xFF4CAF50)
    "blue" -> Color(0xFF2196F3)
    "pink" -> Color(0xFFE0529C)
    else -> Color.Gray
}
