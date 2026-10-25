package com.sinq.app.ui.reading

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.AgpeyaTheme
import com.sinq.app.ui.theme.LocalMotion
import com.sinq.app.ui.theme.LocalReadingFont
import com.sinq.app.ui.theme.sinqColors

/** State of progress for the [ReadingCard]. */
enum class ReadingProgressState {
    NOT_STARTED,
    IN_PROGRESS,
    FINISHED,
}

/** Data bundle passed to [ReadingCard]. */
data class ReadingCardPlan(
    val planLine: String,
    val title: String,
    val chapterLine: String,
    val progress: Float = 0f,
    val state: ReadingProgressState = when {
        progress >= 1f -> ReadingProgressState.FINISHED
        progress > 0f -> ReadingProgressState.IN_PROGRESS
        else -> ReadingProgressState.NOT_STARTED
    },
)

/**
 * Animated candle progress bar featuring a 1dp muted track, 2dp gold fill,
 * and a 12dp wide x 20dp tall organic candle flame that flickers and sways at the fill end.
 */
@Composable
fun CandleProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = LocalMotion.current.enabled,
) {
    val sinq = sinqColors
    val clampedProgress = progress.coerceIn(0f, 1f)

    // On first show, animate fill and flame from 0 to target progress (600ms, decelerate)
    val animProgress = remember { Animatable(if (motionEnabled) 0f else clampedProgress) }
    LaunchedEffect(clampedProgress, motionEnabled) {
        if (motionEnabled) {
            animProgress.animateTo(
                targetValue = clampedProgress,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            )
        } else {
            animProgress.snapTo(clampedProgress)
        }
    }

    // Flicker: infinite transition, scaleY 0.95 to 1.05, ~1dp sway, 1200-1600ms, pivot at base
    val infiniteTransition = rememberInfiniteTransition(label = "candleProgressFlicker")
    val scaleYAnim by if (motionEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1350, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "candleScaleY",
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val swayAnim by if (motionEnabled) {
        infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1450, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "candleSway",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    // Reusable Path instances to avoid allocations in draw loop
    val outerPath = remember { Path() }
    val innerPath = remember { Path() }

    val trackColor = sinq.onHeroMuted.copy(alpha = 0.25f)
    val fillColor = sinq.onHeroGold
    val flameTip = Color(0xFFFFFDE7)
    val flameGold = sinq.onHeroGold
    val flameOrange = Color(0xFFFF9800)
    val flameAmber = Color(0xFFE64A19)
    val paleYellow = Color(0xFFFFF9C4)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 26.dp)
            .graphicsLayer { clip = false }
            .semantics { hideFromAccessibility() }, // flame marked decorative
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f) return@Canvas

        val flameW = 12.dp.toPx()
        val flameH = 20.dp.toPx()
        val baselineY = h - 2.dp.toPx() // base rests on the progress line

        // Clamp flame position so it never clips at the card edges
        val halfFlame = flameW / 2f
        val currentProgress = animProgress.value
        val rawX = currentProgress * w
        val flameX = rawX.coerceIn(halfFlame, w - halfFlame)

        // 1. 1dp muted track
        drawLine(
            color = trackColor,
            start = Offset(0f, baselineY),
            end = Offset(w, baselineY),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // 2. 2dp gold fill up to progress value
        val fillEndX = flameX
        if (fillEndX > 0f) {
            drawLine(
                color = fillColor,
                start = Offset(0f, baselineY),
                end = Offset(fillEndX, baselineY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // 3. Flame drawing (orange body, pale yellow core, soft orange glow)
        val swayPx = swayAnim * 1.dp.toPx()
        val tipX = flameX + swayPx
        // scaleY 0.95 to 1.05 pivoted at baseline
        val tipY = baselineY - flameH * scaleYAnim

        // 3a. Soft orange glow behind flame at ~0.22 alpha
        val glowRadius = 14.dp.toPx()
        val flameMidY = (tipY + baselineY) / 2f
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to flameOrange.copy(alpha = 0.22f),
                0.45f to flameOrange.copy(alpha = 0.12f),
                0.80f to flameOrange.copy(alpha = 0.03f),
                1.0f to flameOrange.copy(alpha = 0f),
                center = Offset(flameX, flameMidY),
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = Offset(flameX, flameMidY),
            blendMode = BlendMode.Plus,
        )

        // 3b. Outer flame body: cubicTo teardrop with gold-orange-amber gradient
        outerPath.reset()
        outerPath.moveTo(flameX, baselineY)
        outerPath.cubicTo(
            x1 = flameX - flameW * 0.58f,
            y1 = baselineY - (baselineY - tipY) * 0.30f,
            x2 = flameX - flameW * 0.45f + swayPx * 0.3f,
            y2 = baselineY - (baselineY - tipY) * 0.70f,
            x3 = tipX,
            y3 = tipY,
        )
        outerPath.cubicTo(
            x1 = flameX + flameW * 0.50f + swayPx * 0.3f,
            y1 = baselineY - (baselineY - tipY) * 0.70f,
            x2 = flameX + flameW * 0.60f,
            y2 = baselineY - (baselineY - tipY) * 0.28f,
            x3 = flameX,
            y3 = baselineY,
        )
        outerPath.close()

        drawPath(
            path = outerPath,
            brush = Brush.verticalGradient(
                colors = listOf(flameTip, flameGold, flameOrange, flameAmber),
                startY = tipY,
                endY = baselineY,
            ),
        )

        // 3c. Inner core: pale yellow core fading to edges
        val coreW = flameW * 0.48f
        val coreTipY = baselineY - (baselineY - tipY) * 0.58f
        val coreTipX = flameX + swayPx * 0.7f

        innerPath.reset()
        innerPath.moveTo(flameX, baselineY - 0.5.dp.toPx())
        innerPath.cubicTo(
            x1 = flameX - coreW * 0.55f,
            y1 = baselineY - (baselineY - coreTipY) * 0.30f,
            x2 = flameX - coreW * 0.38f,
            y2 = baselineY - (baselineY - coreTipY) * 0.75f,
            x3 = coreTipX,
            y3 = coreTipY,
        )
        innerPath.cubicTo(
            x1 = flameX + coreW * 0.40f,
            y1 = baselineY - (baselineY - coreTipY) * 0.75f,
            x2 = flameX + coreW * 0.55f,
            y2 = baselineY - (baselineY - coreTipY) * 0.30f,
            x3 = flameX,
            y3 = baselineY - 0.5.dp.toPx(),
        )
        innerPath.close()

        drawPath(
            path = innerPath,
            brush = Brush.radialGradient(
                colors = listOf(Color.White, paleYellow, paleYellow.copy(alpha = 0f)),
                center = Offset(flameX, baselineY - (baselineY - coreTipY) * 0.45f),
                radius = (baselineY - coreTipY) * 0.75f,
            ),
        )
    }
}

/**
 * "Continue reading" card for the Books / Library screen.
 *
 * Dark green, 18dp corners, 16dp padding, adaptively sized, full width.
 * Single merged semantics node, Role.Button, with flame marked decorative.
 * Adapts to any screen size, density, and font scale without clipping.
 */
@Composable
fun ReadingCard(
    plan: ReadingCardPlan,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val sinq = sinqColors
    val readingFont = LocalReadingFont.current

    // Cap font scale inside this card at 1.3 so large system text cannot break the layout
    val currentDensity = LocalDensity.current
    val systemFontScale = currentDensity.fontScale
    val cappedDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = systemFontScale.coerceAtMost(1.3f),
        )
    }

    val buttonText = when (plan.state) {
        ReadingProgressState.NOT_STARTED -> if (s.isAmharic) "ንባብ ጀምር" else "Start reading"
        ReadingProgressState.IN_PROGRESS -> if (s.isAmharic) "ቀጥል" else "Continue"
        ReadingProgressState.FINISHED -> if (s.isAmharic) "እንደገና አንብብ" else "Read again"
    }

    val effectiveProgress = when (plan.state) {
        ReadingProgressState.NOT_STARTED -> 0f
        ReadingProgressState.FINISHED -> 1f
        ReadingProgressState.IN_PROGRESS -> plan.progress.coerceIn(0f, 1f)
    }

    val baseTextStyle = remember(readingFont) {
        TextStyle(
            fontFamily = readingFont,
            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
        )
    }

    CompositionLocalProvider(LocalDensity provides cappedDensity) {
        Surface(
            onClick = onContinue,
            shape = RoundedCornerShape(18.dp),
            color = sinq.hero,
            contentColor = sinq.onHero,
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(min = 140.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                },
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            ) {
                // If width is under 360dp or font scale is above 1.3, stack caption above full-width button
                val isNarrow = maxWidth < 360.dp
                val isLargeFont = systemFontScale > 1.3f
                val stackBottomRow = isNarrow || isLargeFont

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Top: plan line (12sp, muted) and Ge'ez title (21sp serif, explicit lineHeight)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = plan.planLine,
                            style = baseTextStyle.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = sinq.onHeroMuted,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = plan.title,
                            style = baseTextStyle.copy(
                                fontSize = 21.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = sinq.onHero,
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Candle progress rule
                    CandleProgress(
                        progress = effectiveProgress,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Bottom: caption (weight 1f, maxLines = 2) and gold pill button (heightIn(min = 44.dp))
                    if (stackBottomRow) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = plan.chapterLine,
                                style = baseTextStyle.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = sinq.onHeroMuted,
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Button(
                                onClick = onContinue,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = sinq.onHeroGold,
                                    contentColor = sinq.hero,
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .heightIn(min = 44.dp),
                            ) {
                                Text(
                                    text = buttonText,
                                    style = baseTextStyle.copy(
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = sinq.hero,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = plan.chapterLine,
                                style = baseTextStyle.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = sinq.onHeroMuted,
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )

                            Button(
                                onClick = onContinue,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = sinq.onHeroGold,
                                    contentColor = sinq.hero,
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .heightIn(min = 44.dp),
                            ) {
                                Text(
                                    text = buttonText,
                                    style = baseTextStyle.copy(
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = sinq.hero,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val previewPlanInProgress = ReadingCardPlan(
    planLine = "ዓመታዊ ንባብ",
    title = "ኦሪት ዘፍጥረት ፩–፫ (የመጀመሪያው መጽሐፍ)",
    chapterLine = "ምዕራፍ ፫ / ፶ · ፲፭ ደቂቃ በቀን",
    progress = 0.42f,
    state = ReadingProgressState.IN_PROGRESS,
)

@Preview(name = "ReadingCard - 320dp · fontScale 1.0", widthDp = 320, fontScale = 1.0f, showBackground = true, backgroundColor = 0xFFEFEDE2)
@Composable
fun ReadingCardPreview320Scale10() {
    AgpeyaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ReadingCard(
                plan = previewPlanInProgress,
                onContinue = {},
            )
        }
    }
}

@Preview(name = "ReadingCard - 320dp · fontScale 1.3", widthDp = 320, fontScale = 1.3f, showBackground = true, backgroundColor = 0xFFEFEDE2)
@Composable
fun ReadingCardPreview320Scale13() {
    AgpeyaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ReadingCard(
                plan = previewPlanInProgress,
                onContinue = {},
            )
        }
    }
}

@Preview(name = "ReadingCard - 320dp · fontScale 2.0", widthDp = 320, fontScale = 2.0f, showBackground = true, backgroundColor = 0xFFEFEDE2)
@Composable
fun ReadingCardPreview320Scale20() {
    AgpeyaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ReadingCard(
                plan = previewPlanInProgress,
                onContinue = {},
            )
        }
    }
}

@Preview(name = "ReadingCard - 411dp · fontScale 1.0", widthDp = 411, fontScale = 1.0f, showBackground = true, backgroundColor = 0xFFEFEDE2)
@Composable
fun ReadingCardPreview411Scale10() {
    AgpeyaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ReadingCard(
                plan = previewPlanInProgress,
                onContinue = {},
            )
        }
    }
}

@Preview(name = "ReadingCard - 411dp · fontScale 1.3", widthDp = 411, fontScale = 1.3f, showBackground = true, backgroundColor = 0xFFEFEDE2)
@Composable
fun ReadingCardPreview411Scale13() {
    AgpeyaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ReadingCard(
                plan = previewPlanInProgress,
                onContinue = {},
            )
        }
    }
}

@Preview(name = "ReadingCard - 411dp · fontScale 2.0", widthDp = 411, fontScale = 2.0f, showBackground = true, backgroundColor = 0xFFEFEDE2)
@Composable
fun ReadingCardPreview411Scale20() {
    AgpeyaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ReadingCard(
                plan = previewPlanInProgress,
                onContinue = {},
            )
        }
    }
}

@Preview(name = "ReadingCard - Not Started", showBackground = true, backgroundColor = 0xFFEFEDE2)
@Composable
fun ReadingCardNotStartedPreview() {
    AgpeyaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ReadingCard(
                plan = ReadingCardPlan(
                    planLine = "ዓመታዊ ንባብ",
                    title = "መጽሐፍ ቅዱስን በዓመት አንብበው ይጨርሱ",
                    chapterLine = "በቀን ፲፭ ደቂቃ",
                    progress = 0f,
                    state = ReadingProgressState.NOT_STARTED,
                ),
                onContinue = {},
            )
        }
    }
}

@Preview(name = "ReadingCard - Finished", showBackground = true, backgroundColor = 0xFFEFEDE2)
@Composable
fun ReadingCardFinishedPreview() {
    AgpeyaTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ReadingCard(
                plan = ReadingCardPlan(
                    planLine = "የዳዊት ንባብ",
                    title = "መዝሙረ ዳዊት ተጠናቋል",
                    chapterLine = "፻፶ ምዕራፎች ተጠናቀዋል",
                    progress = 1f,
                    state = ReadingProgressState.FINISHED,
                ),
                onContinue = {},
            )
        }
    }
}
