package com.agpeya.app.ui.common

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agpeya.app.ui.strings.LocalStrings
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
 * Two rules the layout keeps:
 *
 *  - **The preview never moves.** It occupies the same band of the screen at
 *    every frame, so switching square to story changes the card and not the
 *    page. A control that moves its own target under the finger is the usual
 *    failure of editors like this.
 *  - **The frame sits between the card and the sheet**, because it is the one
 *    decision that changes everything below it, and putting it on the card's
 *    own edge says so without needing a label.
 *
 * Rendered by [PassageShare.preview], which is the export path scaled down —
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

    // The preview is redrawn off the main thread whenever the spec changes.
    // Width in pixels rather than dp: this is a bitmap, and asking for one
    // wider than the screen is paying to throw pixels away.
    val density = LocalDensity.current
    val previewPx = with(density) { 260.dp.roundToPx() }
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {

                // ── Bar: leave, title, and the action this screen exists for ──
                Row(
                    Modifier.fillMaxWidth().padding(
                        start = Spacing.sm, end = Spacing.screen, top = Spacing.xs,
                        bottom = Spacing.xs,
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
                        IconButton(
                            enabled = !busy,
                            onClick = {
                                scope.launch {
                                    busy = true
                                    try { PassageShare.save(context, payload, s, spec) } finally { busy = false }
                                }
                            },
                        ) {
                            Icon(
                                Icons.Outlined.SaveAlt,
                                contentDescription = s.saveImage,
                                modifier = Modifier.size(IconSize.medium),
                            )
                        }
                    }
                    Button(
                        enabled = !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                Toast.makeText(context, s.imagePreparing, Toast.LENGTH_SHORT).show()
                                try {
                                    if (PassageShare.share(context, payload, s, spec)) onDismiss()
                                } finally { busy = false }
                            }
                        },
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.small),
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(s.shareAction)
                    }
                }
                HorizontalDivider()

                // ── The stage. Fixed height: the card changes, the page does not ──
                val tall = LocalConfiguration.current.screenHeightDp
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp)
                        .height((tall * 0.42f).dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    val shot = preview
                    when {
                        shot != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            androidx.compose.foundation.Image(
                                bitmap = shot.bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .padding(Spacing.md)
                                    .heightIn(max = (tall * 0.34f).dp),
                            )
                            // Pages and the chosen size, because both are
                            // consequences of choices made below and neither is
                            // visible in a picture of page one.
                            Text(
                                "${s.editorPages(shot.pages)} · ${shot.bodySize.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        rendering -> CircularProgressIndicator(Modifier.size(28.dp))
                        else -> Text(
                            s.shareFailed,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // ── The frame, on the card's own edge ─────────────────────────
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.screen, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Choice(s.frameCard, spec.shape == ImageShape.CARD) {
                        spec = spec.copy(shape = ImageShape.CARD)
                    }
                    Choice(s.frameSquare, spec.shape == ImageShape.SQUARE) {
                        spec = spec.copy(shape = ImageShape.SQUARE)
                    }
                    Choice(s.frameStory, spec.shape == ImageShape.STORY) {
                        spec = spec.copy(shape = ImageShape.STORY)
                    }
                }
                HorizontalDivider()

                // ── The sheet ─────────────────────────────────────────────────
                val tabs = listOf(
                    s.editorStyle, s.editorGround, s.editorType, s.editorLayout, s.editorMark,
                )
                TabRow(selectedTabIndex = tab) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = tab == index,
                            onClick = { tab = index },
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
                        .padding(horizontal = Spacing.screen, vertical = Spacing.sm)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                ) {
                    when (tab) {
                        0 -> StylePanel(spec, preview?.bodySize) { spec = it }
                        1 -> GroundPanel(spec) { spec = it }
                        2 -> TypePanel(spec) { spec = it }
                        3 -> LayoutPanel(spec) { spec = it }
                        else -> MarkPanel(spec) { spec = it }
                    }
                }
            }
        }
    }
}

/** Long enough to swallow a drag, short enough not to feel like lag. */
private const val PREVIEW_DEBOUNCE_MS = 120L

/* ── panels ─────────────────────────────────────────────────────────────── */

@Composable
private fun StylePanel(spec: CardSpec, chosenSize: Float?, onChange: (CardSpec) -> Unit) {
    val s = LocalStrings.current
    val presets = listOf(
        CardPreset.VERSE to s.presetVerse,
        CardPreset.PASSAGE to s.presetPassage,
        CardPreset.STORY to s.presetStory,
        CardPreset.FEAST to s.presetFeast,
        CardPreset.PLAIN to s.presetPlain,
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        presets.forEach { (preset, label) ->
            // A preset does not layer on top of the current card: choosing one
            // sets the whole spec, keeping only the ground, which is the one
            // thing that reads as the reader's own rather than the preset's.
            Choice(label, spec.preset == preset && spec.sizeOverride == null) {
                onChange(CardSpec.of(preset, CardSpec(ground = spec.ground)))
            }
        }
    }
    Spacer(Modifier.height(Spacing.sm))

    // Size: says the app already chose, and what it chose. Dragging takes it
    // off auto; the word puts it back. That is the whole relationship between
    // the automatic decision and the manual one.
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(s.editorSize, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (spec.sizeOverride == null) {
            TextButton(onClick = {}, enabled = false) {
                Text("${s.editorSizeAuto} · ${chosenSize?.toInt() ?: "—"}")
            }
        } else {
            TextButton(onClick = { onChange(spec.copy(sizeOverride = null)) }) {
                Text(s.editorSizeAuto)
            }
        }
    }
    Slider(
        value = spec.sizeOverride ?: chosenSize ?: BODY_MIN,
        onValueChange = { onChange(spec.copy(sizeOverride = it)) },
        valueRange = BODY_MIN..BODY_MAX,
        steps = ((BODY_MAX - BODY_MIN) / BODY_STEP).toInt() - 1,
    )

    Text(s.editorAir, style = MaterialTheme.typography.bodyMedium)
    Slider(
        value = spec.spacing,
        onValueChange = { onChange(spec.copy(spacing = it)) },
        valueRange = 1.4f..2.0f,
        steps = 5,
    )
}

@Composable
private fun GroundPanel(spec: CardSpec, onChange: (CardSpec) -> Unit) {
    val s = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        listOf(
            ImageGround.GREEN to s.groundGreen,
            ImageGround.IVORY to s.groundIvory,
            ImageGround.NIGHT to s.groundNight,
        ).forEach { (ground, label) ->
            Choice(label, spec.ground == ground) { onChange(spec.copy(ground = ground)) }
        }
    }
}

@Composable
private fun TypePanel(spec: CardSpec, onChange: (CardSpec) -> Unit) {
    val s = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
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
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        listOf(
            BlockPosition.TOP to s.positionTop,
            BlockPosition.CENTER to s.positionCenter,
            BlockPosition.BOTTOM to s.positionBottom,
        ).forEach { (position, label) ->
            Choice(label, spec.position == position) { onChange(spec.copy(position = position)) }
        }
    }
    Spacer(Modifier.height(Spacing.xs))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
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

/* ── two controls, used everywhere above ─────────────────────────────────── */

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
        colors = FilterChipDefaults.filterChipColors(),
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
