package com.agpeya.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.agpeya.app.data.ThemeChoice

// Fixed brand palette (decision D6: no dynamic color).
// Deep indigo + warm gold accent; candidate from D5 mockups, easy to swap here.
private val Indigo = Color(0xFF2E2A5C)
private val IndigoLight = Color(0xFF4A4486)
private val Gold = Color(0xFFB8923E)
private val Parchment = Color(0xFFFAF6EE)
private val NightSurface = Color(0xFF141320)
private val NightText = Color(0xFFE6E1D8)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Color.White,
    background = Parchment,
    onBackground = Color(0xFF1C1A14),
    surface = Color.White,
    onSurface = Color(0xFF1C1A14),
    surfaceVariant = Color(0xFFF0EAD9),
    onSurfaceVariant = Color(0xFF4A463C),
)

// Dark theme is a flagship feature (D5): night hours are prayed in the dark.
private val DarkColors = darkColorScheme(
    primary = IndigoLight,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Color.Black,
    background = NightSurface,
    onBackground = NightText,
    surface = Color(0xFF1D1B2C),
    onSurface = NightText,
    surfaceVariant = Color(0xFF262438),
    onSurfaceVariant = Color(0xFFB5B0A4),
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
