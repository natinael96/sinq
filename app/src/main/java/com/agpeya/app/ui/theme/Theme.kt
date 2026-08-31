package com.agpeya.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.agpeya.app.data.ThemeChoice

// Green & gold identity. Deep liturgical green carries the ground; gold is the
// voice of what matters — counts, prayer days, verse numbers, your place. Sage marks
// what is completed.
//
// Two rules govern this file:
//
//  1. **Every Material role is spelled out.** Anything left unset falls back to
//     Material's purple baseline, and it shows: bottom sheets, dropdown menus,
//     date pickers and chips are all painted from `surfaceContainer*`, `outline`
//     and `error`, none of which the app used to define.
//
//  2. **`primary` obeys Material, not the brand.** Switches, chips, sliders and
//     progress bars tint themselves with `primary`, so in the dark theme it has
//     to be a *light* green or those controls vanish into the ground. The brand's
//     dark-green hero surface lives in [SinqColors.hero] instead — see Tokens.kt.

// ── Light: warm ivory ground, deep green, bronzed gold ───────────────────────
private val GreenDeep = Color(0xFF0E3B31)
private val IvoryGround = Color(0xFFEFEDE2)
private val IvorySurface = Color(0xFFF7F5EB)
private val InkLight = Color(0xFF1D2B24)
// Bronzed rather than bright: #A67F2E only reached 3.1:1 on the ivory ground,
// and gold carries small text (kickers, counts, verse numerals) all over the app.
private val GoldLight = Color(0xFF7E5F1E)
private val MutedLight = Color(0xFF56655A)

private val LightColors = lightColorScheme(
    primary = GreenDeep,
    onPrimary = IvorySurface,
    primaryContainer = Color(0xFFD3E1D8),
    onPrimaryContainer = Color(0xFF06231C),
    inversePrimary = Color(0xFF8FCBB8),

    secondary = GoldLight,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFE3C6),
    onSecondaryContainer = Color(0xFF3B2D08),

    // Sage — completion, without borrowing the gold that marks importance.
    tertiary = Color(0xFF44654F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD2E3D6),
    onTertiaryContainer = Color(0xFF152A1D),

    error = Color(0xFF8C2F26),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF7DDD9),
    onErrorContainer = Color(0xFF3A0F0B),

    background = IvoryGround,
    onBackground = InkLight,
    surface = IvorySurface,
    onSurface = InkLight,
    surfaceVariant = Color(0xFFE4E1D2),
    onSurfaceVariant = MutedLight,
    surfaceTint = GreenDeep,

    // Menus, sheets and pickers are painted from these.
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F7EF),
    surfaceContainer = Color(0xFFF4F2E7),
    surfaceContainerHigh = Color(0xFFEEECE0),
    surfaceContainerHighest = Color(0xFFE8E6D9),

    outline = Color(0xFF77826F),
    outlineVariant = Color(0xFFD9D6C6),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2A3830),
    inverseOnSurface = Color(0xFFF1EFE4),
)

// ── Dark: designed on its own terms, not an inversion ────────────────────────
// A near-black ground would make the ivory text glare at night; the deep green
// keeps the page luminance low while staying warm. Surfaces step up in small
// increments so hierarchy survives without borders everywhere.
private val GreenGroundDark = Color(0xFF0A2E27)
private val IvoryDark = Color(0xFFEDE8D9)
private val GoldDark = Color(0xFFE0BC65)

private val DarkColors = darkColorScheme(
    // Light green: this is what tints switches, chips and progress bars.
    primary = Color(0xFF86CBB1),
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF1A5041),
    onPrimaryContainer = Color(0xFFA6E8CD),
    inversePrimary = GreenDeep,

    secondary = GoldDark,
    onSecondary = Color(0xFF3A2C05),
    secondaryContainer = Color(0xFF4B3A12),
    onSecondaryContainer = Color(0xFFF6DFAB),

    tertiary = Color(0xFF9EC7AB),
    onTertiary = Color(0xFF10331F),
    tertiaryContainer = Color(0xFF2A4A35),
    onTertiaryContainer = Color(0xFFBAE3C6),

    error = Color(0xFFF0B4AC),
    onError = Color(0xFF561B14),
    errorContainer = Color(0xFF733029),
    onErrorContainer = Color(0xFFFFDAD5),

    background = GreenGroundDark,
    onBackground = IvoryDark,
    // A step above the ground, so a card still lifts off the page in the dark
    // theme instead of relying on its border alone.
    surface = Color(0xFF103A31),
    onSurface = IvoryDark,
    surfaceVariant = Color(0xFF1B4438),
    onSurfaceVariant = Color(0xFFA6C2B4),
    surfaceTint = Color(0xFF86CBB1),

    surfaceContainerLowest = Color(0xFF06231D),
    surfaceContainerLow = Color(0xFF0D342C),
    surfaceContainer = Color(0xFF103A31),
    surfaceContainerHigh = Color(0xFF154237),
    surfaceContainerHighest = Color(0xFF1B4B3E),

    outline = Color(0xFF6E8C7D),
    outlineVariant = Color(0xFF235043),
    scrim = Color(0xFF000000),
    inverseSurface = IvoryDark,
    inverseOnSurface = Color(0xFF17322A),
)

// The hero green is the same colour in both themes — that constancy is what
// makes it read as the brand rather than as "the dark surface".
private val LightSinq = SinqColors(
    hero = GreenDeep,
    onHero = Color(0xFFF4F1E4),
    onHeroMuted = Color(0xFFB9CCC0),
    heroGlow = Color(0x57E4BC5A),
    success = Color(0xFF44654F),
    // The coral that reads on the dark ground fails contrast on ivory; the
    // light theme deepens it to a brick red that still says "liturgical red".
    arke = Color(0xFFB23A2E),
    // On ivory, a light tint needs enough body to be visible without shouting.
    highlightYellow = Color(0x66E8C46B),
    highlightGreen = Color(0x554CAF50),
    highlightBlue = Color(0x552196F3),
    highlightPink = Color(0x55E0529C),
)

private val DarkSinq = SinqColors(
    hero = Color(0xFF12463A),
    onHero = Color(0xFFF0EBDC),
    onHeroMuted = Color(0xFFA9C6B6),
    heroGlow = Color(0x4DE0BC65),
    success = Color(0xFF9EC7AB),
    arke = Color(0xFFF0776A),
    // Over a dark green ground the same tints go muddy — they need to be lighter
    // and a touch more opaque to stay distinguishable from one another.
    highlightYellow = Color(0x59FFD980),
    highlightGreen = Color(0x5981E884),
    highlightBlue = Color(0x597EC4FF),
    highlightPink = Color(0x59FF88BE),
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
    CompositionLocalProvider(
        LocalSinqColors provides if (darkTheme) DarkSinq else LightSinq,
        LocalMotion provides rememberMotion(),
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AgpeyaTypography,
            shapes = AgpeyaShapes,
            content = content,
        )
    }
}
