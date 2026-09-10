package com.agpeya.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.sinqColors
import kotlin.math.roundToInt

enum class Tab(val route: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME("home", Icons.Outlined.Home, Icons.Filled.Home),
    // A path, not a flame: the tab is about where you have walked, not what
    // you might lose. (Route "journey" replaced "streak" with the rename.)
    JOURNEY("journey", Icons.Outlined.Route, Icons.Filled.Route),
    LIBRARY("library", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    SETTINGS("settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}

/** How much wider the tab you are on is than the three you are not. */
private const val EXPANSION = 2.4f

/** The bar's own height, before its margins. Tall enough to tap, no taller. */
private val SLOT = 48.dp

/**
 * A floating card of dark glass, with the tab you are on lit inside it.
 *
 * The card is the one it always was — the same corner, the same margin, the
 * same place on the page — but it is now cut from the hero's deep green in both
 * themes rather than from the page's own surface. On ivory a pale bar with a
 * pale gold mark had nothing to be seen against; against dark glass the gold
 * reads at a glance, and light and dark now show the same bar.
 *
 * The tab you are on does three things at once, so the current position
 * survives greyscale, colour-blindness and a glance: the glyph fills, a warm
 * light rises inside a lozenge behind it, and the lozenge opens sideways to say
 * the tab's name in full — which is the only way ቤተ መጻሕፍት fits without
 * crowding the other three. Everything moves on one short tween, and on a
 * device with animations turned off it simply arrives.
 *
 * The light is drawn, not lit by anything: no candle is shown, only what a
 * candle would do. And the glass is a tint, not a blur — the bar takes its own
 * height in the [androidx.compose.material3.Scaffold] rather than floating over
 * the page, so nothing passes beneath it to refract, and so the pages that
 * measure the room they have left (ቤት fills it exactly) still measure right.
 */
@Composable
fun AgpeyaBottomBar(current: Tab, onSelect: (Tab) -> Unit) {
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
    Box(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = Spacing.md)
            .padding(bottom = Spacing.sm, top = Spacing.xs),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = sinq.hero,
            contentColor = sinq.onHero,
            border = BorderStroke(1.dp, sinq.onHeroGold.copy(alpha = 0.20f)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    // What makes it read as glass rather than as paint: a lit
                    // top edge, and the light falling away down the face. The
                    // Surface clips it to the corner for us.
                    .drawBehind {
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.06f),
                                0.55f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.18f),
                            ),
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.16f),
                            start = Offset(0f, 0.5f),
                            end = Offset(size.width, 0.5f),
                            strokeWidth = 1f,
                        )
                    }
                    .padding(Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
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
    }
}

@Composable
private fun RowScope.TabSlot(
    tab: Tab,
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val motion = LocalMotion.current
    val sinq = sinqColors
    // One number drives the whole slot: its share of the width, the light
    // behind it, the tint of the glyph, and how much of the name is shown.
    val lit by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = motion.spec(Motion.standard),
        label = "tabLit",
    )
    val gold = sinq.onHeroGold
    Box(
        modifier = Modifier
            .weight(1f + EXPANSION * lit)
            .height(SLOT)
            .clip(CircleShape)
            .drawBehind {
                if (lit <= 0f) return@drawBehind
                // A lamp under the glass: the source sits below the lozenge's
                // lower edge, so the light climbs the face and fades out
                // before the top of it.
                val corner = CornerRadius(size.height / 2f)
                drawRoundRect(
                    brush = Brush.radialGradient(
                        0.00f to gold.copy(alpha = 0.58f * lit),
                        0.34f to gold.copy(alpha = 0.26f * lit),
                        0.62f to gold.copy(alpha = 0.08f * lit),
                        1.00f to Color.Transparent,
                        center = Offset(size.width / 2f, size.height * 1.16f),
                        radius = size.height * 1.35f,
                    ),
                    cornerRadius = corner,
                )
                drawRoundRect(
                    color = gold.copy(alpha = 0.34f * lit),
                    cornerRadius = corner,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            // Role.Tab tells a screen reader this is one of a set of tabs and
            // reads its selected state; the name has to be given here because
            // only the selected tab shows it on screen.
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = { if (!selected) onSelect() },
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.icon,
                contentDescription = null,
                tint = lerp(sinq.onHeroMuted, gold, lit),
                modifier = Modifier.size(IconSize.medium),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = gold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .graphicsLayer { alpha = lit }
                    // Measured at its full width and then shown a fraction of
                    // it, so the name is uncovered as the lozenge opens rather
                    // than being squeezed into whatever room there is.
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity),
                        )
                        val gap = Spacing.sm.roundToPx()
                        val width = ((placeable.width + gap) * lit).roundToInt().coerceAtLeast(0)
                        layout(width, placeable.height) { placeable.place(gap, 0) }
                    },
            )
        }
    }
}
