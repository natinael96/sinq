package com.agpeya.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
val Menbere = FontFamily(Font(R.font.menbere))

fun readingFontFamily(choice: ReadingFont): FontFamily = when (choice) {
    ReadingFont.ABYSSINICA -> Abyssinica
    ReadingFont.ABAY_LIGHT -> AbayLight
    ReadingFont.BELA_BEREKA -> BelaBereka
    ReadingFont.ZEMENAY -> Zemenay
    ReadingFont.MENBERE -> Menbere
}

/**
 * The reader face in effect. Screens read this instead of naming [Abyssinica]
 * directly, so the Settings choice reaches every prayer/scripture surface.
 */
val LocalReadingFont = staticCompositionLocalOf { Abyssinica }

// Generous line height for Ethiopic body text (~1.7x), per PLAN.md §2.6.
val AgpeyaTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = Ethiopic,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Ethiopic,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 30.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Ethiopic,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 26.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Ethiopic,
        fontSize = 18.sp,
        lineHeight = 31.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Ethiopic,
        fontSize = 15.sp,
        lineHeight = 24.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Ethiopic,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)
