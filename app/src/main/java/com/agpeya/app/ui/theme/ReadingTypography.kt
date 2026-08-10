package com.agpeya.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * One place to build the text styles used by every reader (prayers, Psalter,
 * scripture, ስንክሳር, ውዳሴ ማርያም), so the bundled faces stay optically consistent.
 *
 * Two things make this necessary:
 *
 *  1. **The faces are not the same size at the same `sp`.** Their x-heights
 *     differ by up to 19% (Menbere 0.570 em vs Abyssinica 0.480 em), so a page
 *     set at 19sp looks noticeably bigger or smaller purely from the choice of
 *     font. [opticalScale] normalizes that against Abyssinica, the default.
 *
 *  2. **Their vertical metrics differ even more** (total line box ranges from
 *     0.88 em to 1.72 em). Left alone, Android derives line spacing from those
 *     metrics and the same `lineHeight` yields visibly different leading per
 *     font. Turning off the legacy font padding and centering the line box makes
 *     the requested `lineHeight` authoritative instead of advisory.
 */

/** Per-face size correction, normalized to Abyssinica SIL's x-height (0.480 em). */
private fun opticalScale(family: FontFamily): Float = when (family) {
    AbayLight -> 1.05f     // x-height 0.459 em — runs small
    BelaBereka -> 0.96f    // 0.500 em
    Zemenay -> 0.91f       // 0.528 em
    Menbere -> 0.84f       // 0.570 em — runs large
    else -> 1f             // Abyssinica SIL, the baseline
}

/**
 * Leading for Ethiopic body text, as a multiple of the font size. Ethiopic needs
 * more air than Latin, but the previous 1.85–2.0 range left prayer pages feeling
 * sparse and pushed long ስንክሳር readings well past a screenful.
 */
const val READING_LINE_HEIGHT = 1.62f

/** Tighter leading for headings and single-line labels, which don't need the air. */
const val TITLE_LINE_HEIGHT = 1.35f

/** Even line distribution with the legacy font padding removed. */
private val EvenLines = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

@Suppress("DEPRECATION")
private val NoFontPadding = PlatformTextStyle(includeFontPadding = false)

/**
 * The body style for reader text at [fontSp] (the user's font-size step), in the
 * face they picked. [lineHeightMultiplier] can be raised for verse-like text
 * that benefits from extra air.
 */
@Composable
@ReadOnlyComposable
fun readingBodyStyle(
    fontSp: Int,
    lineHeightMultiplier: Float = READING_LINE_HEIGHT,
): TextStyle {
    val family = LocalReadingFont.current
    val size = fontSp * opticalScale(family)
    return TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = (size * lineHeightMultiplier).sp,
        platformStyle = NoFontPadding,
        lineHeightStyle = EvenLines,
    )
}

/** Reader text sized relative to the body — titles, labels, superscript numerals. */
@Composable
@ReadOnlyComposable
fun readingStyle(
    fontSp: Int,
    scale: Float,
    lineHeightMultiplier: Float = TITLE_LINE_HEIGHT,
): TextStyle = readingBodyStyle(fontSp, lineHeightMultiplier).let {
    val size = it.fontSize * scale
    it.copy(fontSize = size, lineHeight = size * lineHeightMultiplier)
}

/** The rendered size of [fontSp] in the current face — for callers that need to
 *  size something (a numeral gutter, a superscript) against the running text. */
@Composable
@ReadOnlyComposable
fun scaledReadingSp(fontSp: Int): TextUnit = (fontSp * opticalScale(LocalReadingFont.current)).sp

/**
 * Re-cast a Material type-scale style (a section title, a psalm heading) in the
 * reader's face. Applies the same optical correction and even line distribution
 * as the body, so a heading in Menbere doesn't tower over one in Abay Light.
 */
@Composable
@ReadOnlyComposable
fun TextStyle.inReadingFont(lineHeightMultiplier: Float = TITLE_LINE_HEIGHT): TextStyle {
    val family = LocalReadingFont.current
    val size = fontSize * opticalScale(family)
    return copy(
        fontFamily = family,
        fontSize = size,
        lineHeight = size * lineHeightMultiplier,
        platformStyle = NoFontPadding,
        lineHeightStyle = EvenLines,
    )
}
