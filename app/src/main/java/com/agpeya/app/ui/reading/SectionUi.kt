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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.SaveAlt
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.agpeya.app.ui.common.Passage
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.automirrored.outlined.MenuBook

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
    highlightNamespace: String? = null,
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
            highlightNamespace = highlightNamespace,
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
    highlightNamespace: String? = null,
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
        val verseKey = HighlightRepository.verseKey(section.id, verseNumber, highlightNamespace)
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
 * The bar that appears when a unit of text is selected.
 *
 * One bar for every reader — the lesson YouVersion teaches best. Tap a verse in
 * the Bible, a psalm in ዳዊት, a verse inside ጸሎተ ነግህ, a paragraph of ስንክሳር, and
 * the same actions appear in the same order, so nobody has to learn which
 * reader they are standing in. Readers whose unit is a paragraph simply have no
 * colour row; everything below it is identical.
 *
 * It slides up from the bottom edge — the shortest possible distance — and
 * every action closes it, so it is never something to dismiss twice.
 *
 * The citation sits at the top, where it does two jobs: it says what is
 * selected, and it shows where a run stops, since a selection cannot cross a
 * section boundary.
 */
@Composable
internal fun SelectionBar(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** The selection itself; null leaves the bar with nothing to act on. */
    passage: com.agpeya.app.ui.common.Passage? = null,
    /** Null hides the colour row — the paragraph readers. */
    currentColor: String? = null,
    onPick: ((String?) -> Unit)? = null,
    /** Names the source on the image card: the hour, the Psalter, መጽሐፍ ቅዱስ. */
    imageKicker: String? = null,
    onBookmark: (() -> Unit)? = null,
    onWriteNote: (() -> Unit)? = null,
    /** The edition's cross references for the selection; empty hides the action. */
    crossRefs: List<com.agpeya.app.data.CrossReference.Ref> = emptyList(),
    onOpenRef: (String) -> Unit = {},
) {
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(motion.spec(Motion.standard)) { it } + fadeIn(motion.spec(Motion.fast)),
        exit = slideOutVertically(motion.spec(Motion.fast)) { it } + fadeOut(motion.spec(Motion.fast)),
    ) {
        val s = LocalStrings.current
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val format by com.agpeya.app.data.SettingsRepository.copyFormat(ctx)
            .collectAsState(initial = com.agpeya.app.ui.common.CopyFormat())
        val names by com.agpeya.app.data.SettingsRepository.highlightNames(ctx)
            .collectAsState(initial = emptyMap())
        Surface(
            modifier = Modifier.navigationBarsPadding(),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = passage?.let { com.agpeya.app.ui.common.PassageFormat.heading(it) }.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = s.dismiss,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.medium),
                        )
                    }
                }
                if (onPick != null) {
                    HighlightRow(
                        currentColor = currentColor,
                        names = names,
                        onPick = onPick,
                    )
                }
                if (passage != null) {
                    val text = com.agpeya.app.ui.common.PassageFormat.text(passage, format)
                    val payload = com.agpeya.app.ui.common.SharePayload(
                        body = com.agpeya.app.ui.common.PassageFormat.body(
                            passage,
                            // The card is a picture of the text, so it keeps its
                            // verse numbers whatever the copy format says.
                            format.copy(verseNumbers = true),
                        ),
                        kicker = imageKicker,
                        title = com.agpeya.app.ui.common.PassageFormat.heading(passage),
                    )
                    // The references live behind their own action rather than
                    // under every verse. 22,905 verses carry them, so a marker
                    // on each would be a second text running beside the first —
                    // which is why no phone Bible has done it that way since
                    // YouVersion moved them behind a tap.
                    var refsOpen by androidx.compose.runtime.saveable.rememberSaveable {
                        androidx.compose.runtime.mutableStateOf(false)
                    }
                    if (refsOpen && crossRefs.isNotEmpty()) {
                        CrossRefSheet(
                            refs = crossRefs,
                            onDismiss = { refsOpen = false },
                            onOpen = { route -> refsOpen = false; onDismiss(); onOpenRef(route) },
                        )
                    }
                    val imageBusy = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    // The shape and ground are chosen when the image is asked
                    // for, not before: it is a decision about where it is going.
                    var imageSheet by androidx.compose.runtime.saveable.rememberSaveable {
                        androidx.compose.runtime.mutableStateOf(false)
                    }
                    var saving by androidx.compose.runtime.saveable.rememberSaveable {
                        androidx.compose.runtime.mutableStateOf(false)
                    }
                    if (imageSheet) {
                        ImageOptionsDialog(
                            onDismiss = { imageSheet = false },
                            onChoose = { shape, ground ->
                                imageSheet = false
                                val shaped = payload.copy(shape = shape, ground = ground)
                                val save = saving
                                onDismiss()
                                if (!imageBusy.value) scope.launch {
                                    imageBusy.value = true
                                    try {
                                        if (save) com.agpeya.app.ui.common.PassageShare.save(ctx, shaped, s)
                                        else com.agpeya.app.ui.common.PassageShare.share(ctx, shaped, s)
                                    } finally {
                                        imageBusy.value = false
                                    }
                                }
                            },
                        )
                    }
                    SelectionActions(
                        buildList {
                            onBookmark?.let {
                                add(SelectionAct(s.markAction, Icons.Outlined.BookmarkBorder) { onDismiss(); it() })
                            }
                            onWriteNote?.let {
                                add(SelectionAct(s.noteAction, Icons.Outlined.EditNote) { onDismiss(); it() })
                            }
                            if (crossRefs.isNotEmpty()) {
                                add(
                                    SelectionAct(
                                        s.crossRefsAction(crossRefs.size),
                                        Icons.AutoMirrored.Outlined.MenuBook,
                                    ) { refsOpen = true },
                                )
                            }
                            add(
                                SelectionAct(s.copyAction, Icons.Outlined.ContentCopy) {
                                    com.agpeya.app.ui.common.Sharing.copy(ctx, text, s)
                                    onDismiss()
                                },
                            )
                            add(
                                SelectionAct(s.shareAction, Icons.Outlined.Share) {
                                    com.agpeya.app.ui.common.Sharing.share(ctx, text, strings = s)
                                    onDismiss()
                                },
                            )
                            add(
                                SelectionAct(s.shareAsImage, Icons.Outlined.Image) {
                                    saving = false
                                    imageSheet = true
                                },
                            )
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                add(
                                    SelectionAct(s.saveImage, Icons.Outlined.SaveAlt) {
                                        saving = true
                                        imageSheet = true
                                    },
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * The four colours, each under the name the reader gave it.
 *
 * Olive Tree's lesson: a colour is only worth having if it means something, and
 * what it means is the reader's to decide. Unnamed, a swatch keeps the colour's
 * own name, which is at least honest.
 */
@Composable
private fun HighlightRow(
    currentColor: String?,
    names: Map<String, String>,
    onPick: (String?) -> Unit,
) {
    val s = LocalStrings.current
    val haptics = LocalHapticFeedback.current
    val sinq = sinqColors
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        HighlightRepository.COLOR_KEYS.forEach { key ->
            val selected = key == currentColor
            val label = names[key]?.takeIf { it.isNotBlank() } ?: s.highlightColor(key)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .semantics { contentDescription = "${s.highlight}: $label" }
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onPick(key)
                    }
                    .padding(vertical = Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(highlightSwatch(key, sinq.highlight(key)))
                        .then(
                            if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier
                        ),
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(
            modifier = Modifier
                .width(48.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable { onPick(null) }
                .padding(vertical = Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.FormatColorReset,
                contentDescription = s.removeHighlight,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

/**
 * Three shapes and two grounds, asked for at the moment the image is.
 *
 * Square is what Telegram and Instagram previews crop to; story is what people
 * post. The typography and the colophon do not change with either — the card is
 * still ስንቅ in all three.
 */
@Composable
private fun ImageOptionsDialog(
    onDismiss: () -> Unit,
    onChoose: (com.agpeya.app.ui.common.ImageShape, com.agpeya.app.ui.common.ImageGround) -> Unit,
) {
    val s = LocalStrings.current
    var ground by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf(com.agpeya.app.ui.common.ImageGround.GREEN)
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.shareAsImage) },
        text = {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    listOf(
                        com.agpeya.app.ui.common.ImageGround.GREEN to s.imageGroundGreen,
                        com.agpeya.app.ui.common.ImageGround.IVORY to s.imageGroundIvory,
                    ).forEach { (value, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = ground == value,
                            onClick = { ground = value },
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                listOf(
                    com.agpeya.app.ui.common.ImageShape.CARD to s.imageShapeCard,
                    com.agpeya.app.ui.common.ImageShape.SQUARE to s.imageShapeSquare,
                    com.agpeya.app.ui.common.ImageShape.STORY to s.imageShapeStory,
                ).forEach { (shape, label) ->
                    com.agpeya.app.ui.common.ListRow(title = label, onClick = { onChoose(shape, ground) })
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(s.cancel) }
        },
    )
}

/**
 * The swatch shown in the picker. Opaque, unlike the tint it applies: a 35%
 * yellow drawn on the sheet would read as "off" rather than as a colour choice.
 */
private fun highlightSwatch(key: String?, tint: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color =
    if (tint == androidx.compose.ui.graphics.Color.Transparent) androidx.compose.ui.graphics.Color.Gray
    else tint.copy(alpha = 1f)

/**
 * The verse's cross references, as chips that open where they point.
 *
 * Citations only, in the Church's own numerals — the target's text is a second
 * tap, not a preview, so the sheet stays short enough to leave the verse it
 * came from on screen.
 */
@Composable
private fun CrossRefSheet(
    refs: List<com.agpeya.app.data.CrossReference.Ref>,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val s = LocalStrings.current
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.crossRefsTitle) },
        text = {
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                refs.forEach { ref ->
                    androidx.compose.material3.SuggestionChip(
                        onClick = { onOpen(ref.route) },
                        label = {
                            Text(
                                com.agpeya.app.data.Citation.of(ref.bookName, ref.chapter, ref.verse, ref.verse),
                                maxLines = 1,
                            )
                        },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(s.dismiss) }
        },
    )
}

/** One action in the bar. */
private data class SelectionAct(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun ColumnScope.SelectionActions(actions: List<SelectionAct>) {
    if (actions.isEmpty()) return
    // Five across is the most a phone holds at a 56 dp target; beyond that they
    // split into two balanced rows rather than shrinking.
    val perRow = if (actions.size <= 5) actions.size else (actions.size + 1) / 2
    actions.chunked(perRow).forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            row.forEach { act ->
                SelectionAction(act.label, act.icon, act.onClick, Modifier.weight(1f))
            }
            // A short last row keeps its buttons the width of the row above.
            repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun SelectionAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize.medium),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The int-pair selection used by paragraph/verse readers without section ids:
 *  -1 anchors nothing; a tap anchors, later taps move the end. Order-free. */
internal fun advanceFlatSelection(anchor: Int, tapped: Int): Pair<Int, Int> =
    if (anchor < 0) tapped to -1 else anchor to tapped

internal fun flatSelectionRange(anchor: Int, end: Int): IntRange =
    if (anchor < 0) IntRange.EMPTY
    else minOf(anchor, if (end < 0) anchor else end)..maxOf(anchor, if (end < 0) anchor else end)

/**
 * The selection as a [Passage] — the verses with their own numbers, and the
 * Church's name for them.
 *
 * [citationFor] lets a reader that knows its book and chapter name the exact
 * range ("የሉቃስ ወንጌል ፲፥፴፰–፵፪"); the hours cannot, since a section there is a
 * passage the prayer book chose, and its own reference is already the answer.
 */
internal fun versePassage(
    sections: List<Section>,
    verseKey: String?,
    endKey: String? = null,
    edition: String? = null,
    citationFor: (Section, IntRange) -> String? = { section, _ -> shareHeading(section) },
): Passage? {
    val (section, range) = resolveSelection(sections, verseKey, endKey) ?: return null
    val verses = range.mapNotNull { n ->
        section.verses.getOrNull(n - section.firstVerse)?.let { n to it }
    }
    if (verses.isEmpty()) return null
    return Passage(
        verses = verses.map { (n, text) -> n as Int? to text },
        citation = citationFor(section, range),
        edition = edition,
    )
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
internal fun selectionKeys(
    sections: List<Section>,
    startKey: String?,
    endKey: String?,
    namespaceFor: (Section) -> String? = { null },
): List<String> {
    val (section, range) = resolveSelection(sections, startKey, endKey) ?: return emptyList()
    return range.mapNotNull { n ->
        if (section.verses.getOrNull(n - section.firstVerse) != null)
            HighlightRepository.verseKey(section.id, n, namespaceFor(section))
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

/** Title and reference, unless the reference only repeats the title — a psalm
 *  is named "መዝሙር ፩" in both, and "መዝሙር ፩ — መዝሙር ፩" is not a heading. */
private fun shareHeading(section: Section): String =
    listOfNotNull(
        section.title,
        section.reference?.takeIf { it.isNotBlank() && it != section.title },
    ).joinToString(" — ")


