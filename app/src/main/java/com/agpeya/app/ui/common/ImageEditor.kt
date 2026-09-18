package com.agpeya.app.ui.common

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The card editor: a preview, the frame it is drawn in, and a sheet of the few
 * things worth changing.
 *
 * The person reaching for this is sharing a verse, not designing, so the unit
 * of choice is a **preset** — a whole [CardSpec] under a name — and the tabs
 * behind it exist for the one time in ten that the preset is nearly right. Most
 * people should open this, look, and press share.
 *
 * Three rules the layout keeps:
 *
 *  - **The preview never moves.** It holds the same share of the window at every
 *    frame, so switching square to story changes the card and not the page. A
 *    control that moves its own target under the finger is the usual failure of
 *    editors like this.
 *  - **The frame sits beside the card**, because it is the one decision that
 *    changes everything else, and putting it on the card's own edge says so
 *    without needing a label.
 *  - **The sheet is never squeezed out.** Measured against the window rather
 *    than the screen, and turned side-by-side when the window is short — which
 *    is every landscape phone, every split-screen, and a folded inner display.
 *
 * Rendered by [PassageShare.preview], which is the export path drawn small —
 * never a second renderer, because a second renderer is a promise the export
 * can quietly break.
 */
@Composable
fun ImageEditorDialog(
    payload: SharePayload,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()

    var spec by rememberSaveable(stateSaver = CardSpecSaver) {
        mutableStateOf(CardSpec.from(payload))
    }
    var preview by remember { mutableStateOf<PassageShare.Preview?>(null) }
    var rendering by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbars = remember { SnackbarHostState() }

    // The preview is redrawn off the main thread whenever the spec changes.
    // Width in pixels rather than dp: this is a bitmap, and asking for one
    // wider than it will be drawn is paying to throw pixels away.
    val density = LocalDensity.current
    val previewPx = with(density) { PREVIEW_WIDTH.roundToPx() }
    LaunchedEffect(spec, payload.body) {
        rendering = true
        // A slider reports a change per frame, and a card takes longer to draw
        // than a frame. LaunchedEffect cancels on every new spec, so a delay
        // here means only the value a drag settles on is ever drawn.
        delay(PREVIEW_DEBOUNCE_MS)
        preview = PassageShare.preview(context, payload, spec, previewPx)
        rendering = false
    }

    // Deliberately no recycle() anywhere in here. asImageBitmap() hands the
    // bitmap to Compose without copying it, and it stays referenced by a
    // display list that the render thread replays on its own schedule —
    // recycling it out from under that is a crash on a thread this code cannot
    // see. These are preview-sized and the collector can have them.

    fun share() {
        scope.launch {
            busy = true
            try {
                if (PassageShare.share(context, payload, null, spec)) onDismiss()
                else snackbars.showSnackbar(s.shareFailed)
            } finally { busy = false }
        }
    }

    fun save() {
        scope.launch {
            busy = true
            try {
                val ok = PassageShare.save(context, payload, null, spec)
                snackbars.showSnackbar(if (ok) s.imageSaved else s.imageSaveFailed)
            } finally { busy = false }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            // The activity runs edge-to-edge; a full-screen dialog that let the
            // decor inset it would be inset twice, or differently by OEM. It
            // owns its insets below, once, with safeDrawing.
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    // Measured against this window, not the screen. screenHeightDp
                    // is wrong in split-screen, wrong on a folded inner display,
                    // and it is what used to leave the sheet 14dp in landscape.
                    val windowHeight = maxHeight
                    val shortWindow = windowHeight < SIDE_BY_SIDE_BELOW

                    Column(Modifier.fillMaxSize()) {
                        EditorBar(
                            busy = busy,
                            onDismiss = onDismiss,
                            onSave = ::save,
                            onShare = ::share,
                        )
                        HorizontalDivider()

                        if (shortWindow) {
                            // Landscape, split-screen, a folded phone: the card
                            // takes the left half and the sheet keeps its full
                            // height on the right, instead of being crushed to
                            // nothing under a stage sized for a tall window.
                            Row(Modifier.fillMaxSize()) {
                                Column(
                                    Modifier.weight(1f).fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Stage(
                                        preview = preview,
                                        rendering = rendering,
                                        spec = spec,
                                        s = s,
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                    )
                                    FrameRow(spec) { spec = it }
                                }
                                Column(Modifier.weight(1f).fillMaxHeight()) {
                                    Sheet(
                                        tab = tab,
                                        onTab = { tab = it },
                                        spec = spec,
                                        chosenSize = preview?.bodySize,
                                        onChange = { spec = it },
                                    )
                                }
                            }
                        } else {
                            Stage(
                                preview = preview,
                                rendering = rendering,
                                spec = spec,
                                s = s,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(windowHeight * STAGE_SHARE),
                            )
                            FrameRow(spec) { spec = it }
                            HorizontalDivider()
                            Sheet(
                                tab = tab,
                                onTab = { tab = it },
                                spec = spec,
                                chosenSize = preview?.bodySize,
                                onChange = { spec = it },
                            )
                        }
                    }
                }
                SnackbarHost(snackbars, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

/* ── the bar ─────────────────────────────────────────────────────────────── */

@Composable
private fun EditorBar(
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
) {
    val s = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().padding(
            start = Spacing.sm, end = Spacing.screen,
            top = Spacing.xs, bottom = Spacing.xs,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = s.dismiss,
                modifier = Modifier.size(IconSize.medium),
            )
        }
        Text(
            s.editorTitle,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).padding(start = Spacing.xs),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            IconButton(enabled = !busy, onClick = onSave) {
                Icon(
                    Icons.Outlined.SaveAlt,
                    contentDescription = s.saveImage,
                    modifier = Modifier.size(IconSize.medium),
                )
            }
        }
        Button(enabled = !busy, onClick = onShare) {
            // The button carries its own waiting state rather than throwing a
            // toast behind the dialog. The label stays put so the target does
            // not resize under the finger mid-press.
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(IconSize.small),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.small),
                )
            }
            Spacer(Modifier.width(Spacing.xs))
            Text(s.shareAction)
        }
    }
}

/* ── the stage ───────────────────────────────────────────────────────────── */

@Composable
private fun Stage(
    preview: PassageShare.Preview?,
    rendering: Boolean,
    spec: CardSpec,
    s: Strings,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .heightIn(min = 120.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            preview != null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(Spacing.sm),
            ) {
                Image(
                    bitmap = preview.bitmap.asImageBitmap(),
                    // The card is the whole subject of this screen. Without
                    // this a reader on TalkBack can work every control and
                    // never learn what any of them did.
                    contentDescription = s.editorPreviewOf(
                        frame = frameName(spec.shape, s),
                        ground = groundName(spec.ground, s),
                        pages = preview.pages,
                    ),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.height(Spacing.xs))
                // Pages and the chosen size: both are consequences of choices
                // made below, and neither is visible in a picture of page one.
                Text(
                    "${s.editorPages(preview.pages)} · ${preview.bodySize.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            rendering -> CircularProgressIndicator(
                Modifier.size(28.dp).semantics { contentDescription = s.imagePreparing },
            )
            else -> Text(
                s.shareFailed,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}

@Composable
private fun FrameRow(spec: CardSpec, onChange: (CardSpec) -> Unit) {
    val s = LocalStrings.current
    ChoiceRow {
        Choice(s.frameCard, spec.shape == ImageShape.CARD) {
            onChange(spec.copy(shape = ImageShape.CARD))
        }
        Choice(s.frameSquare, spec.shape == ImageShape.SQUARE) {
            onChange(spec.copy(shape = ImageShape.SQUARE))
        }
        Choice(s.frameStory, spec.shape == ImageShape.STORY) {
            onChange(spec.copy(shape = ImageShape.STORY))
        }
    }
}

/* ── the sheet ───────────────────────────────────────────────────────────── */

@Composable
private fun Sheet(
    tab: Int,
    onTab: (Int) -> Unit,
    spec: CardSpec,
    chosenSize: Float?,
    onChange: (CardSpec) -> Unit,
) {
    val s = LocalStrings.current
    val tabs = listOf(s.editorStyle, s.editorGround, s.editorType, s.editorLayout, s.editorMark)
    Column(Modifier.fillMaxSize()) {
        // Scrollable, not fixed: five labels divided into a phone's width give
        // each about 72dp, and at a raised system font size the Amharic ones
        // truncate mid-word.
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = Spacing.md) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = tab == index,
                    onClick = { onTab(index) },
                    text = {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                    },
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.sm),
        ) {
            when (tab) {
                0 -> StylePanel(spec, chosenSize, onChange)
                1 -> GroundPanel(spec, onChange)
                2 -> TypePanel(spec, onChange)
                3 -> LayoutPanel(spec, onChange)
                else -> MarkPanel(spec, onChange)
            }
        }
    }
}

/* ── panels ─────────────────────────────────────────────────────────────── */

@Composable
private fun StylePanel(spec: CardSpec, chosenSize: Float?, onChange: (CardSpec) -> Unit) {
    val s = LocalStrings.current
    // Held so the slider does not start at the floor and jump to the real size
    // the moment the first preview resolves.
    var lastAuto by remember { mutableStateOf(BODY_MIN) }
    if (chosenSize != null && spec.sizeOverride == null) lastAuto = chosenSize

    ChoiceRow {
        listOf(
            CardPreset.VERSE to s.presetVerse,
            CardPreset.PASSAGE to s.presetPassage,
            CardPreset.STORY to s.presetStory,
            CardPreset.FEAST to s.presetFeast,
            CardPreset.PLAIN to s.presetPlain,
        ).forEach { (preset, label) ->
            // Selected while the card still *is* the preset. Any change at all
            // deselects it — size, air, margin alike — rather than size alone,
            // which made the chip mean two different things.
            val untouched = spec == CardSpec.of(preset, CardSpec(ground = spec.ground))
            Choice(label, untouched) {
                onChange(CardSpec.of(preset, CardSpec(ground = spec.ground)))
            }
        }
    }
    Spacer(Modifier.height(Spacing.sm))

    // Says the app already chose, and what it chose. Dragging takes it off
    // auto; the button puts it back. That is the whole relationship between
    // the automatic decision and the manual one.
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(s.editorSize, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (spec.sizeOverride == null) {
            // A value, not a dead control. This was a disabled TextButton,
            // which looked pressable, was not, and announced "disabled".
            Text(
                "${s.editorSizeAuto} · ${chosenSize?.toInt() ?: lastAuto.toInt()}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TextButton(onClick = { onChange(spec.copy(sizeOverride = null)) }) {
                Text(s.editorSizeAuto)
            }
        }
    }
    Slider(
        value = spec.sizeOverride ?: lastAuto,
        onValueChange = { onChange(spec.copy(sizeOverride = it)) },
        valueRange = BODY_MIN..BODY_MAX,
        steps = ((BODY_MAX - BODY_MIN) / BODY_STEP).toInt() - 1,
        modifier = Modifier.semantics { contentDescription = s.editorSize },
    )

    Text(s.editorAir, style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = spec.spacing,
        onValueChange = { onChange(spec.copy(spacing = it)) },
        valueRange = 1.4f..2.0f,
        steps = 5,
        modifier = Modifier.semantics { contentDescription = s.editorAir },
    )
}

@Composable
private fun GroundPanel(spec: CardSpec, onChange: (CardSpec) -> Unit) {
    val s = LocalStrings.current
    ChoiceRow {
        ImageGround.entries.forEach { ground ->
            Choice(groundName(ground, s), spec.ground == ground) {
                onChange(spec.copy(ground = ground))
            }
        }
    }
}

@Composable
private fun TypePanel(spec: CardSpec, onChange: (CardSpec) -> Unit) {
    val s = LocalStrings.current
    ChoiceRow {
        listOf(
            // The reader's own alignment words, not a second vocabulary for
            // the same idea in the same app.
            CardAlign.START to s.alignLeft,
            CardAlign.CENTER to s.alignCenter,
            CardAlign.JUSTIFY to s.alignJustified,
        ).forEach { (align, label) ->
            Choice(label, spec.align == align) { onChange(spec.copy(align = align)) }
        }
    }
}

@Composable
private fun LayoutPanel(spec: CardSpec, onChange: (CardSpec) -> Unit) {
    val s = LocalStrings.current
    ChoiceRow {
        listOf(
            BlockPosition.TOP to s.positionTop,
            BlockPosition.CENTER to s.positionCenter,
            BlockPosition.BOTTOM to s.positionBottom,
        ).forEach { (position, label) ->
            Choice(label, spec.position == position) { onChange(spec.copy(position = position)) }
        }
    }
    ChoiceRow {
        listOf(
            CardMargin.TIGHT to s.marginTight,
            CardMargin.NORMAL to s.marginNormal,
            CardMargin.GENEROUS to s.marginGenerous,
        ).forEach { (margin, label) ->
            Choice(label, spec.margin == margin) { onChange(spec.copy(margin = margin)) }
        }
    }
}

@Composable
private fun MarkPanel(spec: CardSpec, onChange: (CardSpec) -> Unit) {
    val s = LocalStrings.current
    Toggle(s.editorShowDate, spec.showDate) { onChange(spec.copy(showDate = it)) }
    Toggle(s.editorShowColophon, spec.showColophon) { onChange(spec.copy(showColophon = it)) }
}

/* ── the controls, used everywhere above ─────────────────────────────────── */

/**
 * A row of mutually exclusive chips.
 *
 * [selectableGroup] is what lets a screen reader say "2 of 5" instead of
 * reading five unrelated switches, and the gap is [Spacing.sm] because 4dp
 * between two 48dp targets is below the spacing Material asks for.
 */
@Composable
private fun ChoiceRow(content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) { content() }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
        // A Material chip is 32dp tall and material3 does not expand its touch
        // target for you — verified in ChipKt, which never calls this. Twenty
        // chips across five panels were all below the 48dp minimum.
        modifier = Modifier.minimumInteractiveComponentSize(),
    )
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun frameName(shape: ImageShape, s: Strings): String = when (shape) {
    ImageShape.CARD -> s.frameCard
    ImageShape.SQUARE -> s.frameSquare
    ImageShape.STORY -> s.frameStory
}

private fun groundName(ground: ImageGround, s: Strings): String = when (ground) {
    ImageGround.GREEN -> s.groundGreen
    ImageGround.IVORY -> s.groundIvory
    ImageGround.NIGHT -> s.groundNight
}

/** Long enough to swallow a drag, short enough not to feel like lag. */
private const val PREVIEW_DEBOUNCE_MS = 120L

/** How wide the preview bitmap is rasterised. The export stays 1080. */
private val PREVIEW_WIDTH = 260.dp

/** Below this window height the card and the sheet sit side by side. */
private val SIDE_BY_SIDE_BELOW = 480.dp

/** The share of a tall window the card holds. */
private const val STAGE_SHARE = 0.42f

/* ── keeping the spec across a rotation ──────────────────────────────────── */

private val CardSpecSaver = androidx.compose.runtime.saveable.listSaver<CardSpec, Any?>(
    save = {
        listOf(
            it.preset.name, it.shape.name, it.ground.name, it.sizeOverride,
            it.spacing, it.align.name, it.position.name, it.margin.name,
            it.showDate, it.showColophon,
        )
    },
    restore = {
        CardSpec(
            preset = CardPreset.valueOf(it[0] as String),
            shape = ImageShape.valueOf(it[1] as String),
            ground = ImageGround.valueOf(it[2] as String),
            sizeOverride = it[3] as Float?,
            spacing = it[4] as Float,
            align = CardAlign.valueOf(it[5] as String),
            position = BlockPosition.valueOf(it[6] as String),
            margin = CardMargin.valueOf(it[7] as String),
            showDate = it[8] as Boolean,
            showColophon = it[9] as Boolean,
        )
    },
)
