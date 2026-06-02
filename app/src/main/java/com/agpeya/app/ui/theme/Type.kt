package com.agpeya.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.agpeya.app.R

// Bundled Ethiopic fonts — never rely on device fonts for Amharic (PLAN.md §2.6).
// Noto Sans Ethiopic for UI chrome; Abyssinica SIL (scripture-grade serif) for prayer text.
val Ethiopic = FontFamily(Font(R.font.noto_sans_ethiopic))
val Abyssinica = FontFamily(Font(R.font.abyssinica_sil))

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
