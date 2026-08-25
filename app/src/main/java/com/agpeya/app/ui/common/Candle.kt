package com.agpeya.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion

/**
 * A candle for *today*, and only today: lit when prayer was recorded, waiting
 * when it wasn't. It never grows, brightens, or dims with history — the flame
 * is the same on the first day back as on the hundredth in a row, which is the
 * whole point.
 *
 * Drawn rather than iconed so the flame can fade in gently (one soft
 * scale-and-fade when today's first prayer lands, honouring reduce-motion via
 * [LocalMotion]) and so an unlit candle reads as "ready", not as an error state.
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
    val flame by animateFloatAsState(
        targetValue = if (lit) 1f else 0f,
        animationSpec = motion.spec(Motion.slow),
        label = "candleFlame",
    )
    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Body: the lower half, a slim rounded pillar.
        val bodyTop = h * 0.52f
        val bodyWidth = w * 0.42f
        drawPath(
            Path().apply {
                addRoundRect(
                    RoundRect(
                        Rect(cx - bodyWidth / 2f, bodyTop, cx + bodyWidth / 2f, h),
                        CornerRadius(w * 0.08f),
                    ),
                )
            },
            color = bodyColor,
        )

        // Wick.
        drawLine(
            color = bodyColor,
            start = Offset(cx, bodyTop),
            end = Offset(cx, h * 0.42f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round,
        )

        if (flame > 0.01f) {
            // Flame: a leaf-shaped teardrop above the wick, plus a faint halo.
            val fh = h * 0.36f * flame
            val fw = w * 0.34f * flame
            val base = h * 0.44f
            val tipY = base - fh
            val midY = base - fh * 0.42f
            drawCircle(
                color = flameColor.copy(alpha = 0.18f * flame),
                radius = fh * 0.75f,
                center = Offset(cx, (tipY + base) / 2f),
            )
            drawPath(
                Path().apply {
                    moveTo(cx, tipY)
                    quadraticTo(cx + fw / 2f, midY, cx, base)
                    quadraticTo(cx - fw / 2f, midY, cx, tipY)
                    close()
                },
                color = flameColor.copy(alpha = flame),
            )
        } else {
            // Unlit: a small open ring where the flame would sit — ready, not absent.
            drawCircle(
                color = bodyColor.copy(alpha = 0.5f),
                radius = w * 0.09f,
                center = Offset(cx, h * 0.32f),
                style = Stroke(width = w * 0.045f),
            )
        }
    }
}
