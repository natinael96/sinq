package com.agpeya.app.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.ui.theme.Abyssinica
import com.agpeya.app.ui.theme.LocalMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The reminder writes itself, then rests a moment before the app opens. A tap
// moves on early for anyone who doesn't want to wait.
private const val FADE_IN_MS = 480      // the ground and its glow settle in
private const val WRITE_MS = 1700       // the pen crosses "Memento Mori"
private const val FLOURISH_MS = 460     // the underline signs it off
private const val SUBTITLE_MS = 620     // the gloss lines rise
private const val HOLD_AFTER_MS = 620L  // a breath once it's whole

private const val TITLE = "Memento Mori"

/**
 * The opening breath: *memento mori* — remember that you will die — the ancient
 * monastic reminder that frames why one prays at all. Rather than a spinner, the
 * words are written onto the screen letter by letter under a soft gold nib, then
 * underlined by hand; "Remember Death / ሞትን አስብ" settles beneath. Held a moment,
 * then the app opens.
 *
 * [onDone] is called exactly once, whether by timeout or by tap.
 */
@Composable
fun MementoMoriScreen(onDone: () -> Unit) {
    var dismissed by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }

    // 0..1 pen progress across the title; the underline stroke; the gloss fade.
    val write = remember { Animatable(0f) }
    val flourish = remember { Animatable(0f) }
    val subtitle = remember { Animatable(0f) }

    // With animations turned off system-wide, every duration collapses to zero:
    // the words appear already written and the splash simply holds its breath.
    val motion = LocalMotion.current

    fun finish() {
        if (!dismissed) {
            dismissed = true
            onDone()
        }
    }

    LaunchedEffect(Unit) {
        shown = true
        write.animateTo(1f, tween(motion.millis(WRITE_MS), easing = LinearEasing))
        // The flourish draws while the gloss rises, so the ending doesn't drag.
        launch {
            flourish.animateTo(1f, tween(motion.millis(FLOURISH_MS), easing = FastOutSlowInEasing))
        }
        subtitle.animateTo(1f, tween(motion.millis(SUBTITLE_MS), easing = FastOutSlowInEasing))
        delay(HOLD_AFTER_MS)
        finish()
    }

    val bloom by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = motion.millis(FADE_IN_MS), easing = LinearEasing),
        label = "memento-bloom",
    )

    val gold = MaterialTheme.colorScheme.secondary
    val textMeasurer = rememberTextMeasurer()
    val titleStyle = remember {
        TextStyle(
            fontFamily = Abyssinica,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 40.sp,
            letterSpacing = 0.5.sp,
        )
    }
    val titleLayout = remember(textMeasurer, titleStyle) { textMeasurer.measure(TITLE, titleStyle) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { finish() },
        contentAlignment = Alignment.Center,
    ) {
        // A faint gold bloom behind the words, the same glow the Home hero uses.
        Box(
            Modifier
                .align(Alignment.Center)
                .height(280.dp)
                .width(280.dp)
                .alpha(bloom * 0.5f)
                .background(
                    Brush.radialGradient(
                        listOf(gold.copy(alpha = 0.22f), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )

        Column(
            modifier = Modifier.padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
            ) {
                val w = titleLayout.size.width.toFloat()
                // On a narrow screen the measured title can be wider than the
                // canvas; scale the whole hand down to fit rather than clip it.
                val fit = ((size.width - 12.dp.toPx()) / w).coerceAtMost(1f)
                scale(fit, fit, pivot = Offset(size.width / 2f, size.height / 2f)) {
                    drawWriting(titleLayout, write.value, flourish.value, gold)
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "Remember Death",
                style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitle.value),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "ሞትን አስብ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitle.value),
            )
        }
    }
}

/**
 * The hand: glyphs inked in left-to-right as the pen reaches them, a warm nib
 * riding the writing head, and an underline drawn by the same motion.
 *
 * [progress] is the pen across the title (0..1), [flourish] the underline.
 */
private fun DrawScope.drawWriting(
    layout: TextLayoutResult,
    progress: Float,
    flourish: Float,
    gold: Color,
) {
    val w = layout.size.width.toFloat()
    val h = layout.size.height.toFloat()
    val underlineGap = 12.dp.toPx()
    val tx = (size.width - w) / 2f
    val ty = (size.height - h - underlineGap) / 2f
    val n = TITLE.length

    // Each glyph fades in as the pen reaches it — letters overlap slightly
    // (span > 1) so the writing reads as one flowing hand, not a row of
    // separate blinks. Clipping to a glyph's box lets it carry its own alpha
    // while we draw the single measured layout.
    val span = 1.8f
    for (i in 0 until n) {
        val local = ((progress * n) - i) / span
        val a = local.coerceIn(0f, 1f)
        val eased = a * a * (3f - 2f * a) // smoothstep
        if (eased <= 0f) continue
        val box = layout.getBoundingBox(i)
        if (box.width <= 0f) continue // spaces: the pen just travels on
        clipRect(
            left = tx + box.left,
            top = ty + box.top,
            right = tx + box.right,
            bottom = ty + box.bottom,
        ) {
            drawText(
                textLayoutResult = layout,
                color = gold.copy(alpha = eased),
                topLeft = Offset(tx, ty),
            )
        }
    }

    // The nib: a small warm glow riding the writing head, fading as the last
    // letter lands so it never lingers as a dot.
    if (progress > 0.01f) {
        val tail = ((progress - 0.86f) / 0.14f).coerceIn(0f, 1f)
        val nibAlpha = 1f - tail
        if (nibAlpha > 0f) {
            val center = Offset(tx + w * progress, ty + h * 0.62f)
            val r = 10.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(gold.copy(alpha = 0.85f * nibAlpha), Color.Transparent),
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )
        }
    }

    // A hand-drawn underline with a gentle dip, revealed by the same
    // left-to-right motion so it reads as one continuing stroke.
    if (flourish > 0f) {
        val uy = ty + h + underlineGap * 0.4f
        val x0 = tx - 6.dp.toPx()
        val x1 = tx + w + 6.dp.toPx()
        val path = Path().apply {
            moveTo(x0, uy)
            quadraticTo((x0 + x1) / 2f, uy + 5.dp.toPx(), x1, uy)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val drawn = Path()
        measure.getSegment(0f, measure.length * flourish, drawn, true)
        drawPath(
            path = drawn,
            color = gold.copy(alpha = 0.7f),
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}
