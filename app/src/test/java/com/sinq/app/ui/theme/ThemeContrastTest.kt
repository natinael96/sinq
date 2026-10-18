package com.sinq.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Computes WCAG 2.1 contrast ratios for every foreground/background token pair
 * in the Sinq theme across Light, Dark, and Vigil Black modes.
 *
 * Requirements:
 * - Body text tokens: >= 4.5:1
 * - Large text / UI component tokens: >= 3.0:1
 */
class ThemeContrastTest {

    private fun relativeLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            return if (c <= 0.04045f) {
                c / 12.92
            } else {
                Math.pow(((c + 0.055) / 1.055), 2.4)
            }
        }
        val r = channel(color.red)
        val g = channel(color.green)
        val b = channel(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(fg: Color, bg: Color): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun assertBodyContrast(name: String, fg: Color, bg: Color) {
        val ratio = contrastRatio(fg, bg)
        assertTrue(
            "WCAG AA Body Contrast Failed for $name: ratio was %.2f:1 (required >= 4.5:1)".format(ratio),
            ratio >= 4.5
        )
    }

    private fun assertLargeTextContrast(name: String, fg: Color, bg: Color) {
        val ratio = contrastRatio(fg, bg)
        assertTrue(
            "WCAG AA Large Text Contrast Failed for $name: ratio was %.2f:1 (required >= 3.0:1)".format(ratio),
            ratio >= 3.0
        )
    }

    @Test
    fun testLightColorSchemeContrast() {
        val c = LightColors
        assertBodyContrast("Light: onBackground on background", c.onBackground, c.background)
        assertBodyContrast("Light: onSurface on surface", c.onSurface, c.surface)
        assertBodyContrast("Light: onSurfaceVariant on surface", c.onSurfaceVariant, c.surface)
        assertBodyContrast("Light: onPrimary on primary", c.onPrimary, c.primary)
        assertBodyContrast("Light: onSecondary on secondary", c.onSecondary, c.secondary)
        assertBodyContrast("Light: onTertiary on tertiary", c.onTertiary, c.tertiary)
        assertBodyContrast("Light: onError on error", c.onError, c.error)
        assertBodyContrast("Light: onPrimaryContainer on primaryContainer", c.onPrimaryContainer, c.primaryContainer)
        assertBodyContrast("Light: onSecondaryContainer on secondaryContainer", c.onSecondaryContainer, c.secondaryContainer)
        assertBodyContrast("Light: onTertiaryContainer on tertiaryContainer", c.onTertiaryContainer, c.tertiaryContainer)
        assertBodyContrast("Light: onErrorContainer on errorContainer", c.onErrorContainer, c.errorContainer)
        assertLargeTextContrast("Light: secondary on background", c.secondary, c.background)
    }

    @Test
    fun testLightSinqBrandContrast() {
        val s = LightSinq
        assertBodyContrast("Light Sinq: onHero on hero", s.onHero, s.hero)
        assertLargeTextContrast("Light Sinq: onHeroMuted on hero", s.onHeroMuted, s.hero)
        assertLargeTextContrast("Light Sinq: onHeroGold on hero", s.onHeroGold, s.hero)
        assertLargeTextContrast("Light Sinq: arke on background", s.arke, LightColors.background)
    }

    @Test
    fun testDarkColorSchemeContrast() {
        val c = DarkColors
        assertBodyContrast("Dark: onBackground on background", c.onBackground, c.background)
        assertBodyContrast("Dark: onSurface on surface", c.onSurface, c.surface)
        assertBodyContrast("Dark: onSurfaceVariant on surface", c.onSurfaceVariant, c.surface)
        assertBodyContrast("Dark: onPrimary on primary", c.onPrimary, c.primary)
        assertBodyContrast("Dark: onSecondary on secondary", c.onSecondary, c.secondary)
        assertBodyContrast("Dark: onTertiary on tertiary", c.onTertiary, c.tertiary)
        assertBodyContrast("Dark: onError on error", c.onError, c.error)
        assertBodyContrast("Dark: onPrimaryContainer on primaryContainer", c.onPrimaryContainer, c.primaryContainer)
        assertBodyContrast("Dark: onSecondaryContainer on secondaryContainer", c.onSecondaryContainer, c.secondaryContainer)
        assertBodyContrast("Dark: onTertiaryContainer on tertiaryContainer", c.onTertiaryContainer, c.tertiaryContainer)
        assertBodyContrast("Dark: onErrorContainer on errorContainer", c.onErrorContainer, c.errorContainer)
        assertLargeTextContrast("Dark: secondary on background", c.secondary, c.background)
    }

    @Test
    fun testDarkSinqBrandContrast() {
        val s = DarkSinq
        assertBodyContrast("Dark Sinq: onHero on hero", s.onHero, s.hero)
        assertLargeTextContrast("Dark Sinq: onHeroMuted on hero", s.onHeroMuted, s.hero)
        assertLargeTextContrast("Dark Sinq: onHeroGold on hero", s.onHeroGold, s.hero)
        assertLargeTextContrast("Dark Sinq: arke on background", s.arke, DarkColors.background)
    }

    @Test
    fun testVigilBlackColorSchemeContrast() {
        val c = VigilBlackColors
        assertBodyContrast("VigilBlack: onBackground on background", c.onBackground, c.background)
        assertBodyContrast("VigilBlack: onSurface on surface", c.onSurface, c.surface)
        assertBodyContrast("VigilBlack: onSurfaceVariant on surface", c.onSurfaceVariant, c.surface)
        assertBodyContrast("VigilBlack: onPrimary on primary", c.onPrimary, c.primary)
        assertBodyContrast("VigilBlack: onSecondary on secondary", c.onSecondary, c.secondary)
        assertBodyContrast("VigilBlack: onTertiary on tertiary", c.onTertiary, c.tertiary)
        assertBodyContrast("VigilBlack: onError on error", c.onError, c.error)
        assertBodyContrast("VigilBlack: onPrimaryContainer on primaryContainer", c.onPrimaryContainer, c.primaryContainer)
        assertBodyContrast("VigilBlack: onSecondaryContainer on secondaryContainer", c.onSecondaryContainer, c.secondaryContainer)
        assertBodyContrast("VigilBlack: onTertiaryContainer on tertiaryContainer", c.onTertiaryContainer, c.tertiaryContainer)
        assertBodyContrast("VigilBlack: onErrorContainer on errorContainer", c.onErrorContainer, c.errorContainer)
        assertLargeTextContrast("VigilBlack: secondary on background", c.secondary, c.background)
    }

    @Test
    fun testVigilBlackSinqBrandContrast() {
        val s = VigilBlackSinq
        assertBodyContrast("VigilBlack Sinq: onHero on hero", s.onHero, s.hero)
        assertLargeTextContrast("VigilBlack Sinq: onHeroMuted on hero", s.onHeroMuted, s.hero)
        assertLargeTextContrast("VigilBlack Sinq: onHeroGold on hero", s.onHeroGold, s.hero)
        assertLargeTextContrast("VigilBlack Sinq: arke on background", s.arke, VigilBlackColors.background)
    }
}
