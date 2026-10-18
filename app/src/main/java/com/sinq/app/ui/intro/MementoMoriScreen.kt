package com.sinq.app.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinq.app.R
import com.sinq.app.ui.strings.AmharicStrings
import com.sinq.app.ui.strings.EnglishStrings
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.Abyssinica
import com.sinq.app.ui.theme.LocalMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.sinq.app.ui.theme.IconSize
import com.sinq.app.ui.theme.Spacing
import com.sinq.app.ui.theme.Waldba
import com.sinq.app.ui.theme.sinqColors

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
 * underlined by hand; the gloss ([Strings.mementoMoriGloss] in the app language,
 * its counterpart beneath) settles under it. Held a moment, then the app opens.
 *
 * [onDone] is called exactly once, whether by timeout or by tap.
 */
@Composable
fun MementoMoriScreen(onDone: () -> Unit) {
    val s = LocalStrings.current
    var dismissed by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }

    // 0..1 dawn logo progress; pen progress across the title; underline; gloss fade.
    val dawnAnim = remember { Animatable(0f) }
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
        launch {
            dawnAnim.animateTo(1f, tween(motion.millis(850), easing = FastOutSlowInEasing))
        }
        write.animateTo(1f, tween(motion.millis(WRITE_MS), easing = LinearEasing))
        // The flourish draws while the gloss rises, so the ending doesn't drag.
        launch {
            flourish.animateTo(1f, tween(motion.millis(FLOURISH_MS), easing = FastOutSlowInEasing))
        }
        subtitle.animateTo(1f, tween(motion.millis(SUBTITLE_MS), easing = FastOutSlowInEasing))
        delay(motion.millis(HOLD_AFTER_MS.toInt()).toLong())
        finish()
    }

    val bloom by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = motion.millis(FADE_IN_MS), easing = LinearEasing),
        label = "memento-bloom",
    )

    val gold = MaterialTheme.colorScheme.secondary
    val arke = sinqColors.arke
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
                onClickLabel = s.dismiss,
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
            // 1. Animated Dawn Horizon & Rising Cross Logo (matches loading_preview.html)
            AnimatedDawnLogo(
                progress = dawnAnim.value,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .size(width = 82.dp, height = 70.dp)
                    .alpha(bloom),
            )
            Spacer(Modifier.height(Spacing.sm))

            // 2. "Memento Mori" live calligraphy & rubricated knotwork underline
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .semantics { contentDescription = TITLE; heading() },
            ) {
                val w = titleLayout.size.width.toFloat()
                // On a narrow screen the measured title can be wider than the
                // canvas; scale the whole hand down to fit rather than clip it.
                val fit = ((size.width - 12.dp.toPx()) / w).coerceAtMost(1f)
                scale(fit, fit, pivot = Offset(size.width / 2f, size.height / 2f)) {
                    drawWriting(titleLayout, write.value, flourish.value, gold, arke)
                }
            }

            Spacer(Modifier.height(Spacing.xl))
            // 3. The gloss leads in the app language; its counterpart settles quietly beneath.
            Text(
                text = s.mementoMoriGloss,
                style = MaterialTheme.typography.bodyLarge.let {
                    if (s.isAmharic) it.copy(fontFamily = Waldba, fontSize = it.fontSize * 1.35f)
                    else it
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitle.value),
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = if (s.isAmharic) EnglishStrings.mementoMoriGloss else AmharicStrings.mementoMoriGloss,
                style = MaterialTheme.typography.bodyMedium.let {
                    if (s.isAmharic) it.copy(letterSpacing = 1.sp)
                    else it.copy(fontFamily = Waldba, fontSize = it.fontSize * 1.35f)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitle.value),
            )
        }

        // The version resting quietly at the foot of the page.
        Text(
            text = appVersion(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.huge)
                .alpha(subtitle.value),
        )
    }
}

/** The running app's own versionName, for the foot of the opening page. */
@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("")
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
    arke: Color,
) {
    val w = layout.size.width.toFloat()
    val h = layout.size.height.toFloat()
    val underlineGap = 16.dp.toPx()
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

    // Liturgical Rubrication Underline (ቀይ ሐረግ) with Lalibela Cross Knot
    if (flourish > 0f) {
        val uy = ty + h + underlineGap
        val cx = size.width / 2f
        val ribbonSpan = (w * 0.95f).coerceAtLeast(180.dp.toPx())
        val xLeft = cx - ribbonSpan / 2f
        val xRight = cx + ribbonSpan / 2f
        val crossHalfWidth = 14.dp.toPx()

        // 1. Left intertwined ribbons (from center outward or leftward)
        val leftRedPath = Path().apply {
            moveTo(cx - crossHalfWidth, uy)
            cubicTo(
                cx - crossHalfWidth - ribbonSpan * 0.12f, uy - 6.dp.toPx(),
                cx - crossHalfWidth - ribbonSpan * 0.28f, uy + 6.dp.toPx(),
                cx - crossHalfWidth - ribbonSpan * 0.40f, uy,
            )
            cubicTo(
                cx - crossHalfWidth - ribbonSpan * 0.48f, uy - 6.dp.toPx(),
                xLeft + ribbonSpan * 0.10f, uy + 5.dp.toPx(),
                xLeft, uy,
            )
        }
        val leftGoldPath = Path().apply {
            moveTo(cx - crossHalfWidth, uy)
            cubicTo(
                cx - crossHalfWidth - ribbonSpan * 0.12f, uy + 6.dp.toPx(),
                cx - crossHalfWidth - ribbonSpan * 0.28f, uy - 6.dp.toPx(),
                cx - crossHalfWidth - ribbonSpan * 0.40f, uy,
            )
            cubicTo(
                cx - crossHalfWidth - ribbonSpan * 0.48f, uy + 6.dp.toPx(),
                xLeft + ribbonSpan * 0.10f, uy - 5.dp.toPx(),
                xLeft, uy,
            )
        }

        // 2. Right intertwined ribbons
        val rightRedPath = Path().apply {
            moveTo(cx + crossHalfWidth, uy)
            cubicTo(
                cx + crossHalfWidth + ribbonSpan * 0.12f, uy - 6.dp.toPx(),
                cx + crossHalfWidth + ribbonSpan * 0.28f, uy + 6.dp.toPx(),
                cx + crossHalfWidth + ribbonSpan * 0.40f, uy,
            )
            cubicTo(
                cx + crossHalfWidth + ribbonSpan * 0.48f, uy - 6.dp.toPx(),
                xRight - ribbonSpan * 0.10f, uy + 5.dp.toPx(),
                xRight, uy,
            )
        }
        val rightGoldPath = Path().apply {
            moveTo(cx + crossHalfWidth, uy)
            cubicTo(
                cx + crossHalfWidth + ribbonSpan * 0.12f, uy + 6.dp.toPx(),
                cx + crossHalfWidth + ribbonSpan * 0.28f, uy - 6.dp.toPx(),
                cx + crossHalfWidth + ribbonSpan * 0.40f, uy,
            )
            cubicTo(
                cx + crossHalfWidth + ribbonSpan * 0.48f, uy + 6.dp.toPx(),
                xRight - ribbonSpan * 0.10f, uy - 5.dp.toPx(),
                xRight, uy,
            )
        }

        // Draw Left ribbons with flourish progress
        val leftMeasureRed = PathMeasure().apply { setPath(leftRedPath, false) }
        val drawnLeftRed = Path()
        leftMeasureRed.getSegment(0f, leftMeasureRed.length * flourish, drawnLeftRed, true)
        drawPath(
            path = drawnLeftRed,
            color = arke.copy(alpha = 0.95f),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
        )

        val leftMeasureGold = PathMeasure().apply { setPath(leftGoldPath, false) }
        val drawnLeftGold = Path()
        leftMeasureGold.getSegment(0f, leftMeasureGold.length * flourish, drawnLeftGold, true)
        drawPath(
            path = drawnLeftGold,
            color = gold.copy(alpha = 0.85f),
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
        )

        // Draw Right ribbons with flourish progress
        val rightMeasureRed = PathMeasure().apply { setPath(rightRedPath, false) }
        val drawnRightRed = Path()
        rightMeasureRed.getSegment(0f, rightMeasureRed.length * flourish, drawnRightRed, true)
        drawPath(
            path = drawnRightRed,
            color = arke.copy(alpha = 0.95f),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round),
        )

        val rightMeasureGold = PathMeasure().apply { setPath(rightGoldPath, false) }
        val drawnRightGold = Path()
        rightMeasureGold.getSegment(0f, rightMeasureGold.length * flourish, drawnRightGold, true)
        drawPath(
            path = drawnRightGold,
            color = gold.copy(alpha = 0.85f),
            style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
        )

        // 3. Center Emblem: Geometric Lalibela Cross Knot (pops in as flourish matures)
        val crossProgress = ((flourish - 0.25f) / 0.75f).coerceIn(0f, 1f)
        if (crossProgress > 0f) {
            val crossScale = crossProgress * crossProgress * (3f - 2f * crossProgress)
            scale(crossScale, crossScale, pivot = Offset(cx, uy)) {
                // Vertical notched bar
                val vW = 5.dp.toPx()
                val vH = 18.dp.toPx()
                drawRect(
                    color = arke,
                    topLeft = Offset(cx - vW / 2f, uy - vH / 2f),
                    size = androidx.compose.ui.geometry.Size(vW, vH),
                )
                // Notches on top/bottom
                val notchW = 7.dp.toPx()
                val notchH = 2.dp.toPx()
                drawRect(
                    color = arke,
                    topLeft = Offset(cx - notchW / 2f, uy - vH / 2f),
                    size = androidx.compose.ui.geometry.Size(notchW, notchH),
                )
                drawRect(
                    color = arke,
                    topLeft = Offset(cx - notchW / 2f, uy + vH / 2f - notchH),
                    size = androidx.compose.ui.geometry.Size(notchW, notchH),
                )

                // Horizontal notched bar
                val hW = 18.dp.toPx()
                val hH = 5.dp.toPx()
                drawRect(
                    color = arke,
                    topLeft = Offset(cx - hW / 2f, uy - hH / 2f),
                    size = androidx.compose.ui.geometry.Size(hW, hH),
                )
                // Notches on left/right
                val notchSideW = 2.dp.toPx()
                val notchSideH = 7.dp.toPx()
                drawRect(
                    color = arke,
                    topLeft = Offset(cx - hW / 2f, uy - notchSideH / 2f),
                    size = androidx.compose.ui.geometry.Size(notchSideW, notchSideH),
                )
                drawRect(
                    color = arke,
                    topLeft = Offset(cx + hW / 2f - notchSideW, uy - notchSideH / 2f),
                    size = androidx.compose.ui.geometry.Size(notchSideW, notchSideH),
                )

                // Gold border outline for the cross
                val strokeW = 1.dp.toPx()
                drawRect(
                    color = gold,
                    topLeft = Offset(cx - vW / 2f, uy - vH / 2f),
                    size = androidx.compose.ui.geometry.Size(vW, vH),
                    style = Stroke(strokeW),
                )
                drawRect(
                    color = gold,
                    topLeft = Offset(cx - hW / 2f, uy - hH / 2f),
                    size = androidx.compose.ui.geometry.Size(hW, hH),
                    style = Stroke(strokeW),
                )

                // Central gold diamond core
                val diamondPath = Path().apply {
                    moveTo(cx, uy - 5.dp.toPx())
                    lineTo(cx + 5.dp.toPx(), uy)
                    lineTo(cx, uy + 5.dp.toPx())
                    lineTo(cx - 5.dp.toPx(), uy)
                    close()
                }
                drawPath(diamondPath, color = gold)

                // Ruby core center dot
                drawCircle(
                    color = arke,
                    radius = 1.6.dp.toPx(),
                    center = Offset(cx, uy),
                )
            }
        }

        // 4. Glint light sweep across the underline ribbon
        if (flourish > 0.6f) {
            val glintProgress = ((flourish - 0.6f) / 0.4f).coerceIn(0f, 1f)
            val glintX = xLeft + ribbonSpan * glintProgress
            val glintAlpha = (1f - ((glintProgress - 0.5f) * 2f).let { it * it }).coerceIn(0f, 1f)
            if (glintAlpha > 0f) {
                val glintRadius = 14.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.8f * glintAlpha),
                            gold.copy(alpha = 0.4f * glintAlpha),
                            Color.Transparent,
                        ),
                        center = Offset(glintX, uy),
                        radius = glintRadius,
                    ),
                    radius = glintRadius,
                    center = Offset(glintX, uy),
                )
            }
        }
    }
}

/**
 * Animated dawn horizon logo: horizon line drawing outward, radiant sun dome
 * rising, central cross springing upward, and 6 sun rays unfurling in sequence.
 * Matches the liturgical mark from loading_preview.html.
 */
@Composable
private fun AnimatedDawnLogo(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val vbW = 108f
        val vbH = 92f
        val scale = (size.width / vbW).coerceAtMost(size.height / vbH)
        val tx = (size.width - vbW * scale) / 2f
        val ty = (size.height - vbH * scale) / 2f

        translate(left = tx, top = ty) {
            scale(scale, scale, pivot = Offset.Zero) {
                // 1. Horizon Line: x1 = 24, x2 = 84, y = 66 (length 60, center 54)
                val horizProg = (progress / 0.45f).coerceIn(0f, 1f)
                if (horizProg > 0f) {
                    val halfLen = 30f * horizProg
                    drawLine(
                        color = color,
                        start = Offset(54f - halfLen, 66f),
                        end = Offset(54f + halfLen, 66f),
                        strokeWidth = 2.8f,
                        cap = StrokeCap.Round,
                    )
                }

                // 2. Sun Dome: M 43.5,66 A 10.5,10.5 0 0 1 64.5,66 Z
                val sunProg = ((progress - 0.15f) / 0.40f).coerceIn(0f, 1f)
                if (sunProg > 0f) {
                    val sunEase = sunProg * sunProg * (3f - 2f * sunProg)
                    scale(sunEase, sunEase, pivot = Offset(54f, 66f)) {
                        val sunPath = Path().apply {
                            moveTo(43.5f, 66f)
                            arcTo(
                                rect = androidx.compose.ui.geometry.Rect(43.5f, 55.5f, 64.5f, 76.5f),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 180f,
                                forceMoveTo = false,
                            )
                            close()
                        }
                        drawPath(sunPath, color = color)
                    }
                }

                // 3. Central Rising Cross
                // M51.2,26 h5.6 v8 h9.2 v5.6 h-9.2 v12.4 h-5.6 v-12.4 h-9.2 v-5.6 h9.2 Z
                val crossProg = ((progress - 0.25f) / 0.45f).coerceIn(0f, 1f)
                if (crossProg > 0f) {
                    val crossEase = crossProg * crossProg * (3f - 2f * crossProg)
                    val yOffset = (1f - crossEase) * 16f
                    val crossPath = Path().apply {
                        moveTo(51.2f, 26f)
                        lineTo(56.8f, 26f)
                        lineTo(56.8f, 34f)
                        lineTo(66.0f, 34f)
                        lineTo(66.0f, 39.6f)
                        lineTo(56.8f, 39.6f)
                        lineTo(56.8f, 52.0f)
                        lineTo(51.2f, 52.0f)
                        lineTo(51.2f, 39.6f)
                        lineTo(42.0f, 39.6f)
                        lineTo(42.0f, 34f)
                        lineTo(51.2f, 34f)
                        close()
                    }
                    translate(top = yOffset) {
                        scale(crossEase, crossEase, pivot = Offset(54f, 38f)) {
                            drawPath(crossPath, color = color)
                        }
                    }
                }

                // 4. 6 Sun Rays unfurling in sequence
                val rays = listOf(
                    Triple(Offset(47.7f, 52.4f), Offset(45.1f, 47.0f), 0.30f),
                    Triple(Offset(42.5f, 56.4f), Offset(37.9f, 52.5f), 0.35f),
                    Triple(Offset(39.5f, 62.1f), Offset(33.7f, 60.6f), 0.40f),
                    Triple(Offset(60.3f, 52.4f), Offset(62.9f, 47.0f), 0.45f),
                    Triple(Offset(65.5f, 56.4f), Offset(70.1f, 52.5f), 0.50f),
                    Triple(Offset(68.5f, 62.1f), Offset(74.3f, 60.6f), 0.55f),
                )
                rays.forEach { (startPt, endPt, startT) ->
                    val rProg = ((progress - startT) / 0.35f).coerceIn(0f, 1f)
                    if (rProg > 0f) {
                        val curEnd = Offset(
                            x = startPt.x + (endPt.x - startPt.x) * rProg,
                            y = startPt.y + (endPt.y - startPt.y) * rProg,
                        )
                        drawLine(
                            color = color,
                            start = startPt,
                            end = curEnd,
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }
}
