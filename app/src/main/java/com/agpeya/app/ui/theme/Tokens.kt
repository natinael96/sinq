package com.agpeya.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * The shared design tokens. Everything visual that repeats — a gap, a radius, an
 * icon, a duration — is named here once so screens stop inventing their own
 * variants of the same thing.
 *
 * The rule of thumb the app follows: **hierarchy comes from spacing and type,
 * not from wrapping things in boxes.** Cards are for genuine grouping; a row of
 * text with air around it is usually the calmer answer.
 */

/** The vertical/horizontal rhythm. Every gap in the app is one of these. */
object Spacing {
    /** Hairline gaps inside a single control. */
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    /** The default gap between related elements. */
    val lg = 16.dp
    val xl = 20.dp
    /** Between a section and the next one. */
    val xxl = 28.dp
    /** Between major blocks of a screen. */
    val huge = 40.dp

    /** Left/right page margin. One value for every screen in the app. */
    val screen = 24.dp
    /** Comfortable tap height for a plain list row (with its own padding). */
    val rowVertical = 14.dp
}

/** Corner radii, as a Material [Shapes] set so components pick them up for free. */
val AgpeyaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Icon sizes. Anything drawn as an icon uses one of these three. */
object IconSize {
    /** Inline with text — a check beside a label, a small candle. */
    val small = 18.dp
    /** The default: app-bar actions, row affordances, navigation. */
    val medium = 22.dp
    /** A card's identifying glyph. */
    val large = 26.dp
}

/**
 * Motion. Short, natural, and never in the way of reading.
 *
 * [Motion.enabled] is false when the user has turned animations off in the
 * system developer/accessibility settings; every spec then collapses to zero
 * duration so the app still updates instantly instead of ignoring the setting.
 */
@Immutable
class Motion(val enabled: Boolean) {

    fun <T> spec(
        durationMillis: Int = standard,
        delayMillis: Int = 0,
        easing: Easing = Easings.standard,
    ): FiniteAnimationSpec<T> = tween(
        durationMillis = if (enabled) durationMillis else 0,
        delayMillis = if (enabled) delayMillis else 0,
        easing = easing,
    )

    /** Duration in millis, already respecting the reduced-motion preference. */
    fun millis(durationMillis: Int): Int = if (enabled) durationMillis else 0

    companion object {
        /** A press ripple settling, a tint changing. */
        const val fast = 130
        /** The default: expand/collapse, selection, fades. */
        const val standard = 220
        /** A screen transition, a sheet. */
        const val slow = 300
    }
}

object Easings {
    /** Material's standard curve — decelerates into place. */
    val standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    /** For things leaving the screen. */
    val exit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    /** For things entering it. */
    val enter = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
}

val LocalMotion = compositionLocalOf { Motion(enabled = true) }

/** Reads the system animation scale; 0 means "reduce motion". */
@Composable
fun rememberMotion(): Motion {
    val context = LocalContext.current
    return remember(context) {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        }.getOrDefault(1f)
        Motion(enabled = scale > 0f)
    }
}

/**
 * Brand colours that do not map onto a Material role.
 *
 * The deep liturgical green is the app's signature *surface* — the "pray now"
 * card, the Journey hero, a selected chapter. In Material's dark scheme `primary`
 * has to be a light colour (switches, chips and progress bars are tinted with
 * it), so the hero green cannot live there. It lives here instead, identical in
 * both themes, which is exactly what makes it read as the brand.
 */
@Immutable
data class SinqColors(
    /** The signature green surface. */
    val hero: Color,
    /** Ink on [hero]. */
    val onHero: Color,
    /** Muted ink on [hero], for a kicker or a time hint. */
    val onHeroMuted: Color,
    /** The soft gold bloom in the corner of a hero card. */
    val heroGlow: Color,
    /** Completed / fulfilled — a prayed hour, a kept habit. */
    val success: Color,
    /** Verse highlight tints, tuned per theme so they read on either ground. */
    val highlightYellow: Color,
    val highlightGreen: Color,
    val highlightBlue: Color,
    val highlightPink: Color,
) {
    fun highlight(key: String?): Color = when (key) {
        "yellow" -> highlightYellow
        "green" -> highlightGreen
        "blue" -> highlightBlue
        "pink" -> highlightPink
        else -> Color.Transparent
    }
}

val LocalSinqColors = staticCompositionLocalOf<SinqColors> {
    error("SinqColors requested outside AgpeyaTheme")
}

/** `MaterialTheme.sinq` — the brand colours, alongside `MaterialTheme.colorScheme`. */
val sinqColors: SinqColors
    @Composable @ReadOnlyComposable get() = LocalSinqColors.current
