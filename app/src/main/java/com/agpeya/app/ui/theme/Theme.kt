package com.agpeya.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.agpeya.app.data.ThemeChoice

// Green & gold identity (decision: reference-inspired restyle).
// Deep liturgical green carries the ground; gold is the voice of what matters —
// counts, streaks, verse numbers, your place. Sage marks completed states.
private val GreenDeep = Color(0xFF0E3B31)   // light-theme primary / brand green
private val GreenCard = Color(0xFF124136)   // dark-theme hero card
private val GreenGroundDark = Color(0xFF0B3129)
private val GreenSurfaceDark = Color(0xFF10382F)
private val GreenVariantDark = Color(0xFF1B4A3E)
private val GoldDark = Color(0xFFE4BC5A)    // gold on dark ground
private val GoldLight = Color(0xFFA67F2E)   // deeper gold for light ground
private val Ivory = Color(0xFFF2EDDE)
private val IvoryGround = Color(0xFFEFEDE2)
private val IvorySurface = Color(0xFFF7F5EB)
private val IvoryVariant = Color(0xFFE3E0D1)
private val InkLight = Color(0xFF1D2B24)
private val MutedLight = Color(0xFF5D6B60)
private val MutedDark = Color(0xFF9DBBAD)

private val LightColors = lightColorScheme(
    primary = GreenDeep,
    onPrimary = Ivory,
    primaryContainer = Color(0xFFD8E2D6),
    onPrimaryContainer = InkLight,
    secondary = GoldLight,
    onSecondary = Ivory,
    secondaryContainer = Color(0xFFEADFC0),
    onSecondaryContainer = Color(0xFF3E2F0D),
    background = IvoryGround,
    onBackground = InkLight,
    surface = IvorySurface,
    onSurface = InkLight,
    surfaceVariant = IvoryVariant,
    onSurfaceVariant = MutedLight,
)

// The primary look: immersive deep green, ivory ink, gold accents.
private val DarkColors = darkColorScheme(
    primary = GreenCard,
    onPrimary = Ivory,
    primaryContainer = Color(0xFF1A5748),
    onPrimaryContainer = Ivory,
    secondary = GoldDark,
    onSecondary = Color(0xFF123829),
    secondaryContainer = Color(0xFF4F3F17),
    onSecondaryContainer = Ivory,
    background = GreenGroundDark,
    onBackground = Ivory,
    surface = GreenSurfaceDark,
    onSurface = Ivory,
    surfaceVariant = GreenVariantDark,
    onSurfaceVariant = MutedDark,
)

@Composable
fun AgpeyaTheme(
    themeChoice: ThemeChoice = ThemeChoice.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeChoice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AgpeyaTypography,
        content = content,
    )
}
