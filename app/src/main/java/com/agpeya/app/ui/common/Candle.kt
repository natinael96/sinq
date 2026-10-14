package com.agpeya.app.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion

/**
 * Ethiopian Church Beeswax (ቱዋፍ / T'waf) candle in "🕊️ Gentle Stillness (Calm)".
 *
 * Lit when prayer was recorded for today, waiting when it wasn't.
 * Features an organic beeswax pillar with a crater pool, wick with ember tip,
 * and a gently breathing, softly swaying living flame with an incandescent
 * white-hot core and halo aura (honouring reduce-motion via [LocalMotion]).
 */
@Composable
fun Candle(
    lit: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    bodyColor: Color,
    flameColor: Color,
) {
    val motion = LocalMotion.current
    val flameTransition by animateFloatAsState(
        targetValue = if (lit) 1f else 0f,
        animationSpec = motion.spec(Motion.slow),
        label = "candleFlame",
    )

    val isAnimated = lit && motion.enabled
    val infiniteTransition = rememberInfiniteTransition(label = "candleGentleStillness")

    val swayAngle by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = -2.2f,
            targetValue = 2.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "candleSway",
        )
    } else {
        animateFloatAsState(0f, label = "candleSwayFixed")
    }

    val stretchY by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "candleStretch",
        )
    } else {
        animateFloatAsState(1f, label = "candleStretchFixed")
    }

    val auraPulse by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0.14f,
            targetValue = 0.24f,
            animationSpec = infiniteRepeatable(
                animation = tween(3200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "candleAura",
        )
    } else {
        animateFloatAsState(0.18f, label = "candleAuraFixed")
    }

    val emberPulse by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "emberPulse",
        )
    } else {
        animateFloatAsState(0.85f, label = "emberPulseFixed")
    }

    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Body: Ethiopian Church Beeswax (ቱዋፍ / T'waf) pillar
        val bodyTop = h * 0.52f
        val bodyWidth = w * 0.44f
        val bodyHeight = h - bodyTop

        // Beeswax shading: combines bodyColor with natural warm beeswax depth
        val waxGradient = Brush.horizontalGradient(
            colors = listOf(
                bodyColor.copy(alpha = 0.82f),
                bodyColor,
                bodyColor.copy(alpha = 0.72f),
            ),
            startX = cx - bodyWidth / 2f,
            endX = cx + bodyWidth / 2f,
        )

        drawRoundRect(
            brush = waxGradient,
            topLeft = Offset(cx - bodyWidth / 2f, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(w * 0.08f),
        )

        // Wax pool crater top
        val craterRadiusX = bodyWidth / 2f
        val craterRadiusY = h * 0.035f
        drawOval(
            color = bodyColor.copy(alpha = 0.9f),
            topLeft = Offset(cx - craterRadiusX, bodyTop - craterRadiusY),
            size = Size(craterRadiusX * 2, craterRadiusY * 2),
        )

        // Wick
        val wickTop = h * 0.42f
        drawLine(
            color = Color(0xFF1F1A14),
            start = Offset(cx, bodyTop),
            end = Offset(cx, wickTop),
            strokeWidth = (w * 0.06f).coerceAtLeast(1.5f),
            cap = StrokeCap.Round,
        )

        if (flameTransition > 0.01f) {
            // Ember tip on wick
            drawCircle(
                color = Color(0xFFFF3D00).copy(alpha = emberPulse * flameTransition),
                radius = (w * 0.045f).coerceAtLeast(1.2f),
                center = Offset(cx, wickTop),
            )

            // Living Flame Assembly with Gentle Stillness
            val fh = h * 0.38f * flameTransition
            val fw = w * 0.34f * flameTransition
            val base = h * 0.44f
            val tipY = base - fh
            val midY = base - fh * 0.42f

            // Radial golden aura
            drawCircle(
                color = Color(0xFFFFB300).copy(alpha = auraPulse * flameTransition),
                radius = fh * 0.85f,
                center = Offset(cx, (tipY + base) / 2f),
            )

            rotate(degrees = swayAngle, pivot = Offset(cx, base)) {
                scale(scaleX = 1f, scaleY = stretchY, pivot = Offset(cx, base)) {
                    // Outer Golden Teardrop Flame
                    val outerPath = Path().apply {
                        moveTo(cx, tipY)
                        quadraticTo(cx + fw / 2f, midY, cx, base)
                        quadraticTo(cx - fw / 2f, midY, cx, tipY)
                        close()
                    }
                    val flameBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFDE7),
                            Color(0xFFFFD54F),
                            Color(0xFFFF9800),
                            Color(0xFFE64A19),
                        ),
                        startY = tipY,
                        endY = base,
                    )
                    drawPath(outerPath, brush = flameBrush, alpha = flameTransition)

                    // Inner Incandescent White-Hot Heart Core
                    val coreH = fh * 0.52f
                    val coreW = fw * 0.42f
                    val coreBase = base - fh * 0.05f
                    val coreTipY = coreBase - coreH
                    val coreMidY = coreBase - coreH * 0.4f
                    val innerPath = Path().apply {
                        moveTo(cx, coreTipY)
                        quadraticTo(cx + coreW / 2f, coreMidY, cx, coreBase)
                        quadraticTo(cx - coreW / 2f, coreMidY, cx, coreTipY)
                        close()
                    }
                    val coreBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFFFFDE7),
                            Color(0xFFFFE082),
                        ),
                        startY = coreTipY,
                        endY = coreBase,
                    )
                    drawPath(innerPath, brush = coreBrush, alpha = 0.95f * flameTransition)

                    // Blue oxidation skirt base
                    drawOval(
                        color = Color(0x882196F3).copy(alpha = 0.65f * flameTransition),
                        topLeft = Offset(cx - fw * 0.28f, base - fh * 0.06f),
                        size = Size(fw * 0.56f, fh * 0.12f),
                    )
                }
            }
        } else {
            // Unlit state: ready open ring
            drawCircle(
                color = bodyColor.copy(alpha = 0.45f),
                radius = w * 0.09f,
                center = Offset(cx, h * 0.32f),
                style = Stroke(width = (w * 0.045f).coerceAtLeast(1.5f)),
            )
        }
    }
}
