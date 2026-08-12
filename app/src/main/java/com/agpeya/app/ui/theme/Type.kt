package com.agpeya.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.agpeya.app.R
import com.agpeya.app.data.ReadingFont

// Bundled Ethiopic fonts — never rely on device fonts for Amharic (PLAN.md §2.6).
// Noto Sans Ethiopic for UI chrome; Abyssinica SIL (scripture-grade serif) for prayer text.
val Ethiopic = FontFamily(Font(R.font.noto_sans_ethiopic))
val Abyssinica = FontFamily(Font(R.font.abyssinica_sil))

// Reader faces the user can pick between (Settings → font). All are bundled and
// cover the full Ethiopic block including the Ge'ez numerals the readers render.
val AbayLight = FontFamily(Font(R.font.ethiopic_abay_light))
val BelaBereka = FontFamily(Font(R.font.bela_bereka))
val Zemenay = FontFamily(Font(R.font.zemenay))

fun readingFontFamily(choice: ReadingFont): FontFamily = when (choice) {
    ReadingFont.ABYSSINICA -> Abyssinica
    ReadingFont.ABAY_LIGHT -> AbayLight
    ReadingFont.BELA_BEREKA -> BelaBereka
    ReadingFont.ZEMENAY -> Zemenay
}

/**
 * The reader face in effect. Screens read this instead of naming [Abyssinica]
 * directly, so the Settings choice reaches every prayer/scripture surface.
 */
val LocalReadingFont = staticCompositionLocalOf { Abyssinica }

@Suppress("DEPRECATION")
private val NoFontPadding = PlatformTextStyle(includeFontPadding = false)

/** Even line distribution, so the requested line height is what you actually get. */
private val EvenLines = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/**
 * One Ethiopic UI style. Three things are non-negotiable for this script:
 *
 *  - the bundled face, never the device's;
 *  - font padding off and the line box centred, or Noto's tall Ethiopic metrics
 *    add invisible leading that makes rows sit off-centre against their icons;
 *  - **zero letter spacing.** Material's Latin type scale tracks labels out by
 *    up to 0.5sp, which visibly breaks Ethiopic syllables apart.
 */
private fun ethiopic(
    size: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight = FontWeight.Normal,
) = TextStyle(
    fontFamily = Ethiopic,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = 0.sp,
    platformStyle = NoFontPadding,
    lineHeightStyle = EvenLines,
)

/**
 * The full Material type scale, in Ethiopic.
 *
 * Every one of the fifteen roles is defined. The four that used to be left out —
 * `titleSmall`, `labelSmall`, `labelLarge`, `bodySmall` — are used across Home,
 * the ግጻዌ day view, search and the readers, and unset roles fall back to
 * Material's Latin defaults: those labels were rendering in the *device's*
 * fallback font, at Latin tracking, beside text set in the bundled one.
 *
 * Hierarchy is carried by size and weight in even steps, so a heading reads as a
 * heading at any system font scale rather than at one particular size.
 */
val AgpeyaTypography = Typography(
    displayLarge = ethiopic(44.sp, 56.sp, FontWeight.Bold),
    displayMedium = ethiopic(36.sp, 46.sp, FontWeight.Bold),
    displaySmall = ethiopic(30.sp, 40.sp, FontWeight.Bold),

    headlineLarge = ethiopic(30.sp, 40.sp, FontWeight.Bold),
    headlineMedium = ethiopic(26.sp, 35.sp, FontWeight.Bold),
    headlineSmall = ethiopic(22.sp, 30.sp, FontWeight.SemiBold),

    titleLarge = ethiopic(21.sp, 29.sp, FontWeight.Bold),
    titleMedium = ethiopic(17.sp, 25.sp, FontWeight.Medium),
    titleSmall = ethiopic(15.sp, 22.sp, FontWeight.Medium),

    bodyLarge = ethiopic(18.sp, 30.sp),
    bodyMedium = ethiopic(15.sp, 24.sp),
    bodySmall = ethiopic(13.sp, 20.sp),

    // Labels carry counts, kickers and captions — the app's most frequent role.
    labelLarge = ethiopic(14.sp, 20.sp, FontWeight.Medium),
    labelMedium = ethiopic(13.sp, 18.sp, FontWeight.Medium),
    labelSmall = ethiopic(12.sp, 17.sp, FontWeight.Medium),
)
