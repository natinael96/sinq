package com.sinq.app.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.AgpeyaTheme
import com.sinq.app.ui.theme.IconSize
import com.sinq.app.ui.theme.LocalMotion
import com.sinq.app.ui.theme.Spacing
import com.sinq.app.ui.theme.sinqColors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tan

enum class Tab(val route: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME("home", Icons.Outlined.Home, Icons.Filled.Home),
    // A path, not a flame: the tab is about where you have walked, not what
    // you might lose. (Route "journey" replaced "streak" with the rename.)
    JOURNEY("journey", Icons.Outlined.Route, Icons.Filled.Route),
    LIBRARY("library", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    SETTINGS("settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}

/** Material's compact-to-rail breakpoint, evaluated from the current window width. */
internal fun usesNavigationRail(width: Dp): Boolean = width >= 600.dp

/**
 * A custom bar background Shape: rounded rectangle with a smooth concave dip in
 * the top edge centered at [notchCenterX].
 *
 * Uses cubic Beziers with eased shoulders for continuous, kink-free transitions.
 */
class NotchedBarShape(
    val notchCenterX: Float,
    val cornerRadius: Dp = 24.dp,
    val notchWidth: Dp = 60.dp,
    val notchDepth: Dp = 9.dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return Outline.Generic(Path())

        val r = with(density) { cornerRadius.toPx() }.coerceAtMost(h / 2f)
        val nw = with(density) { notchWidth.toPx() }
        val nd = with(density) { notchDepth.toPx() }

        if (notchCenterX <= 0f) {
            val fallbackPath = Path().apply {
                addRoundRect(RoundRect(Rect(0f, 0f, w, h), CornerRadius(r, r)))
            }
            return Outline.Generic(fallbackPath)
        }

        val minX = (r + nw / 2f).coerceAtMost(w / 2f)
        val maxX = (w - r - nw / 2f).coerceAtLeast(minX)
        val cx = notchCenterX.coerceIn(minX, maxX)
        val left = cx - nw / 2f
        val right = cx + nw / 2f

        val path = Path().apply {
            moveTo(0f, r)
            // Top-left rounded corner
            arcTo(
                rect = Rect(0f, 0f, 2 * r, 2 * r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )

            // Top edge to the start of the notch
            lineTo(left, 0f)

            // Smooth concave dip using cubic Beziers with eased shoulders
            cubicTo(
                x1 = left + nw * 0.22f, y1 = 0f,
                x2 = cx - nw * 0.16f, y2 = nd,
                x3 = cx, y3 = nd,
            )
            cubicTo(
                x1 = cx + nw * 0.16f, y1 = nd,
                x2 = right - nw * 0.22f, y2 = 0f,
                x3 = right, y3 = 0f,
            )

            // Top edge to the top-right corner
            lineTo(w - r, 0f)
            // Top-right rounded corner
            arcTo(
                rect = Rect(w - 2 * r, 0f, w, 2 * r),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )

            // Right edge to bottom-right corner
            lineTo(w, h - r)
            // Bottom-right rounded corner
            arcTo(
                rect = Rect(w - 2 * r, h - 2 * r, w, h),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )

            // Bottom edge to bottom-left corner
            lineTo(r, h)
            // Bottom-left rounded corner
            arcTo(
                rect = Rect(0f, h - 2 * r, 2 * r, h),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )

            close()
        }
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotchedBarShape) return false
        return notchCenterX == other.notchCenterX &&
            cornerRadius == other.cornerRadius &&
            notchWidth == other.notchWidth &&
            notchDepth == other.notchDepth
    }

    override fun hashCode(): Int {
        var result = notchCenterX.hashCode()
        result = 31 * result + cornerRadius.hashCode()
        result = 31 * result + notchWidth.hashCode()
        result = 31 * result + notchDepth.hashCode()
        return result
    }
}

/**
 * Realistic candle flame indicator.
 *
 * Built in 5 layers back to front:
 *  1. Outer glow (radial gradient circle, BlendMode.Plus)
 *  2. Outer flame body (asymmetric teardrop with vertical gold-orange-amber gradient)
 *  3. Inner core (smaller bright white-yellow teardrop fading to edges)
 *  4. Base cooler region (translucent cool blue)
 *  5. Wick (curved dark 1dp stroke)
 *
 * Driven by layered sine waves (2.1Hz, 3.7Hz, 5.3Hz) for organic non-repeating
 * flicker, dynamic tip lean during navigation, and reduced-motion compliance.
 */
@Composable
fun CandleFlame(
    modifier: Modifier = Modifier,
    leanAngle: Float = 0f,
    scale: Float = 1f,
    alpha: Float = 1f,
    accentColor: Color = sinqColors.onHeroGold,
    timeSeconds: Float? = null,
    motionEnabled: Boolean = LocalMotion.current.enabled,
) {
    val sinq = sinqColors
    val infiniteTransition = rememberInfiniteTransition(label = "candleFlameFlicker")
    val defaultCycleTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(60_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "flameTimeSeconds",
    )

    val time = if (!motionEnabled) 0f else (timeSeconds ?: defaultCycleTime)
    val arke = sinq.arke
    val highlightBlue = sinq.highlightBlue
    val heroColor = sinq.hero

    // Reusable Path instances to prevent allocations in draw loop
    val outerPath = remember { Path() }
    val innerPath = remember { Path() }
    val wickPath = remember { Path() }

    Canvas(
        modifier = modifier
            .size(width = 14.dp, height = 20.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val flameW = 10.dp.toPx()
        val flameH = 14.dp.toPx()

        // Multi-frequency sine waves
        val t = time.toDouble()
        val s1 = sin(2.1 * 2 * PI * t).toFloat()
        val s2 = sin(3.7 * 2 * PI * t).toFloat()
        val s3 = sin(5.3 * 2 * PI * t).toFloat()

        val sway = if (motionEnabled) (0.50f * s1 + 0.35f * s2 + 0.15f * s3) else 0f
        val stretch = if (motionEnabled) (1.0f + 0.08f * (0.6f * s2 + 0.4f * s1)) else 1.0f
        val widthFactor = if (motionEnabled) (1.0f - 0.05f * (0.6f * s2 + 0.4f * s1)) else 1.0f
        val tipSwayPx = sway * 1.5.dp.toPx()

        // Lean tilt in direction of travel
        val leanRad = Math.toRadians(leanAngle.toDouble()).toFloat()
        val leanTipOffset = (flameH * tan(leanRad.toDouble())).toFloat()

        // Give the top of the canvas 3dp headroom so flame tip and glow never clip
        val baseY = h - 2.5.dp.toPx()
        val tipX = cx + tipSwayPx + leanTipOffset
        val tipY = (baseY - (flameH - 1.5.dp.toPx()) * stretch).coerceAtLeast(2.dp.toPx())

        // 1. Outer glow: cubic ease-out radial gradient behind flame in accent color
        val glowPulse = if (motionEnabled) {
            (0.22f + 0.04f * (0.6f * s1 + 0.4f * s3)).coerceIn(0.12f, 0.30f)
        } else {
            0.22f
        }
        val glowRadius = 15.dp.toPx()
        val flameMidY = (tipY + baseY) / 2f
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to accentColor.copy(alpha = glowPulse),
                0.22f to accentColor.copy(alpha = glowPulse * 0.55f),
                0.48f to accentColor.copy(alpha = glowPulse * 0.20f),
                0.72f to accentColor.copy(alpha = glowPulse * 0.04f),
                0.90f to accentColor.copy(alpha = glowPulse * 0.01f),
                1.0f to Color.Transparent,
                center = Offset(cx, flameMidY),
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = Offset(cx, flameMidY),
            blendMode = BlendMode.Plus,
        )

        // 2. Outer flame body: teardrop with asymmetric tip and vertical gradient
        outerPath.reset()
        outerPath.moveTo(cx, baseY)
        // Left belly
        outerPath.cubicTo(
            x1 = cx - flameW * 0.62f * widthFactor,
            y1 = baseY - (baseY - tipY) * 0.28f,
            x2 = cx - flameW * 0.48f * widthFactor + tipSwayPx * 0.25f,
            y2 = baseY - (baseY - tipY) * 0.70f,
            x3 = tipX,
            y3 = tipY,
        )
        // Right belly (organic asymmetric control points)
        outerPath.cubicTo(
            x1 = cx + flameW * 0.52f * widthFactor + tipSwayPx * 0.25f,
            y1 = baseY - (baseY - tipY) * 0.68f,
            x2 = cx + flameW * 0.65f * widthFactor,
            y2 = baseY - (baseY - tipY) * 0.26f,
            x3 = cx,
            y3 = baseY,
        )
        outerPath.close()

        val warmOrange = lerp(accentColor, arke, 0.45f)
        val deepAmber = lerp(accentColor, arke, 0.80f)
        drawPath(
            path = outerPath,
            brush = Brush.verticalGradient(
                colors = listOf(accentColor, warmOrange, deepAmber),
                startY = tipY,
                endY = baseY,
            ),
        )

        // 3. Inner core: smaller teardrop inside, brighter center fading to edges
        val coreS1 = sin(2.1 * 2 * PI * t + 0.85).toFloat()
        val coreS2 = sin(3.7 * 2 * PI * t + 1.25).toFloat()
        val coreSway = if (motionEnabled) (0.55f * coreS1 + 0.45f * coreS2) else 0f
        val coreTipX = cx + coreSway * 1.0.dp.toPx() + leanTipOffset * 0.65f
        val coreTipY = baseY - (baseY - tipY) * 0.62f
        val coreW = flameW * 0.48f * widthFactor

        innerPath.reset()
        innerPath.moveTo(cx, baseY - 0.5.dp.toPx())
        innerPath.cubicTo(
            x1 = cx - coreW * 0.60f,
            y1 = baseY - (baseY - coreTipY) * 0.30f,
            x2 = cx - coreW * 0.40f,
            y2 = baseY - (baseY - coreTipY) * 0.75f,
            x3 = coreTipX,
            y3 = coreTipY,
        )
        innerPath.cubicTo(
            x1 = cx + coreW * 0.42f,
            y1 = baseY - (baseY - coreTipY) * 0.75f,
            x2 = cx + coreW * 0.58f,
            y2 = baseY - (baseY - coreTipY) * 0.30f,
            x3 = cx,
            y3 = baseY - 0.5.dp.toPx(),
        )
        innerPath.close()

        val coreCenter = lerp(accentColor, Color.White, 0.85f)
        val coreYellow = lerp(accentColor, Color.White, 0.40f).copy(alpha = 0.35f)
        drawPath(
            path = innerPath,
            brush = Brush.radialGradient(
                colors = listOf(coreCenter, coreYellow, Color.Transparent),
                center = Offset(cx, baseY - (baseY - coreTipY) * 0.45f),
                radius = (baseY - coreTipY) * 0.75f,
            ),
        )

        // 4. Base cooler region: translucent blue-ish region at very bottom of flame
        val coolBlue = highlightBlue.copy(alpha = 0.45f)
        val baseRadius = flameW * 0.42f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(coolBlue, Color.Transparent),
                center = Offset(cx, baseY),
                radius = baseRadius,
            ),
            topLeft = Offset(cx - baseRadius, baseY - 2.dp.toPx()),
            size = Size(baseRadius * 2f, 3.5.dp.toPx()),
        )

        // 5. Wick: tiny dark 1dp vertical line at the base, slightly curved
        val wickColor = heroColor.copy(alpha = 0.85f)
        wickPath.reset()
        wickPath.moveTo(cx, h)
        wickPath.quadraticTo(
            cx + 0.4.dp.toPx(),
            h - 1.5.dp.toPx(),
            cx + 0.1.dp.toPx(),
            baseY,
        )
        drawPath(
            path = wickPath,
            color = wickColor,
            style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/**
 * Redesigned bottom navigation bar featuring a custom notched card surface
 * and a realistic animated candle flame active indicator.
 */
@Composable
fun AgpeyaBottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val s = LocalStrings.current
    val sinq = sinqColors
    val motion = LocalMotion.current
    val density = LocalDensity.current

    val label: (Tab) -> String = {
        when (it) {
            Tab.HOME -> s.tabHome
            Tab.JOURNEY -> s.tabJourney
            Tab.LIBRARY -> s.tabLibrary
            Tab.SETTINGS -> s.tabSettings
        }
    }

    val flameCanvasWidth = 14.dp
    val flameCanvasHeight = 20.dp
    val notchDepth = 9.dp
    val notchWidth = 60.dp
    val cornerRadius = 24.dp
    val barSurfaceHeight = 60.dp
    val flameOverlap = 12.dp
    val totalBarHeight = barSurfaceHeight + flameOverlap // 72.dp preserved height

    var barWidthPx by remember { mutableFloatStateOf(0f) }

    // Navigation and flame motion animatables
    val notchXAnim = remember { Animatable(0f) }
    val leanAngleAnim = remember { Animatable(0f) }
    val igniteScale = remember { Animatable(1f) }
    val igniteAlpha = remember { Animatable(1f) }

    val targetX = if (barWidthPx > 0f) {
        barWidthPx * (current.ordinal + 0.5f) / Tab.entries.size
    } else 0f

    LaunchedEffect(targetX, barWidthPx) {
        if (barWidthPx <= 0f) return@LaunchedEffect
        if (notchXAnim.value <= 0f) {
            notchXAnim.snapTo(targetX)
            leanAngleAnim.snapTo(0f)
            igniteScale.snapTo(1f)
            igniteAlpha.snapTo(1f)
            return@LaunchedEffect
        }
        if (targetX == notchXAnim.value) return@LaunchedEffect

        if (motion.enabled) {
            val dir = if (targetX > notchXAnim.value) 1f else -1f
            // Flame leans in direction of travel, then settles upright with damped spring
            launch {
                leanAngleAnim.animateTo(
                    targetValue = dir * 14f,
                    animationSpec = tween(durationMillis = 110, easing = LinearOutSlowInEasing),
                )
                leanAngleAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }
            // Decelerating slide for notch and flame, with ignition pop on arrival
            launch {
                igniteScale.animateTo(
                    targetValue = 0.70f,
                    animationSpec = tween(80),
                )
                notchXAnim.animateTo(
                    targetValue = targetX,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                )
                launch {
                    igniteAlpha.snapTo(0.6f)
                    igniteAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(150),
                    )
                }
                igniteScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
        } else {
            notchXAnim.snapTo(targetX)
            leanAngleAnim.snapTo(0f)
            igniteScale.snapTo(1f)
            igniteAlpha.snapTo(1f)
        }
    }

    val barShape = remember(notchXAnim.value) {
        NotchedBarShape(
            notchCenterX = notchXAnim.value,
            cornerRadius = cornerRadius,
            notchWidth = notchWidth,
            notchDepth = notchDepth,
        )
    }

    val barGradient = remember {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.06f),
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.18f),
        )
    }

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = Spacing.md)
            .padding(bottom = Spacing.sm, top = Spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalBarHeight),
        ) {
            // 1. Notched bar surface with tabs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = flameOverlap)
                    .height(barSurfaceHeight)
                    .clip(barShape)
                    .background(sinq.hero, barShape)
                    .background(barGradient, barShape)
                    .border(BorderStroke(1.dp, sinq.onHeroGold.copy(alpha = 0.20f)), barShape)
                    .onSizeChanged { barWidthPx = it.width.toFloat() },
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Tab.entries.forEach { tab ->
                        TabSlot(
                            tab = tab,
                            label = label(tab),
                            selected = tab == current,
                            onSelect = { onSelect(tab) },
                        )
                    }
                }
            }

            // 2. Active Indicator: Candle Flame (unclipped in parent Box)
            if (barWidthPx > 0f) {
                val flameWidthPx = with(density) { flameCanvasWidth.toPx() }
                val flameTopYPx = with(density) { (flameOverlap + notchDepth - 19.5.dp).toPx() }

                CandleFlame(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .graphicsLayer {
                            translationX = notchXAnim.value - flameWidthPx / 2f
                            translationY = flameTopYPx
                            scaleX = igniteScale.value
                            scaleY = igniteScale.value
                            this.alpha = igniteAlpha.value
                        },
                    leanAngle = leanAngleAnim.value,
                    accentColor = sinq.onHeroGold,
                    motionEnabled = motion.enabled,
                )
            }
        }
    }
}

/** Material's expanded-width counterpart to [AgpeyaBottomBar]. */
@Composable
fun AgpeyaNavigationRail(current: Tab, onSelect: (Tab) -> Unit) {
    val s = LocalStrings.current
    val sinq = sinqColors
    val label: (Tab) -> String = {
        when (it) {
            Tab.HOME -> s.tabHome
            Tab.JOURNEY -> s.tabJourney
            Tab.LIBRARY -> s.tabLibrary
            Tab.SETTINGS -> s.tabSettings
        }
    }
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        header = { SinqWordmark(Modifier.padding(vertical = Spacing.lg)) },
    ) {
        Tab.entries.forEach { tab ->
            NavigationRailItem(
                selected = tab == current,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = if (tab == current) tab.selectedIcon else tab.icon,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.medium),
                    )
                },
                label = {
                    Text(
                        label(tab),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = sinq.onHeroGold,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    indicatorColor = sinq.hero,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun RowScope.TabSlot(
    tab: Tab,
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val sinq = sinqColors
    val motion = LocalMotion.current

    // Spring scale pop on selection
    val iconScale = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected && motion.enabled) {
            iconScale.snapTo(0.90f)
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        } else {
            iconScale.snapTo(1f)
        }
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.medium)
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onSelect,
            )
            .semantics { contentDescription = label }
            .padding(top = 10.dp, bottom = 6.dp, start = 4.dp, end = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Active icon with warm flame glow, scale pop, and crossfade
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                }
                .then(
                    if (selected) {
                        Modifier.drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        sinq.onHeroGold.copy(alpha = 0.16f),
                                        Color.Transparent,
                                    ),
                                    radius = size.maxDimension * 0.95f,
                                ),
                            )
                        }
                    } else Modifier
                ),
        ) {
            Crossfade(
                targetState = selected,
                animationSpec = motion.spec(150),
                label = "tabIconCrossfade",
            ) { isSelected ->
                Icon(
                    imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                    contentDescription = null,
                    tint = if (isSelected) sinq.onHeroGold else sinq.onHeroMuted,
                    modifier = Modifier.size(IconSize.medium),
                )
            }
        }

        Spacer(Modifier.height(3.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) sinq.onHeroGold else sinq.onHeroMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEFEDE2)
@Composable
fun AgpeyaBottomBarPreview() {
    AgpeyaTheme {
        AgpeyaBottomBar(current = Tab.HOME, onSelect = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E3B31)
@Composable
fun CandleFlamePreview() {
    AgpeyaTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            CandleFlame()
        }
    }
}
