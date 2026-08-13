package com.agpeya.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion
import com.agpeya.app.ui.theme.Spacing

enum class Tab(val route: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME("home", Icons.Outlined.Home, Icons.Filled.Home),
    // A path, not a flame: the tab is about where you have walked, not what
    // you might lose. (Route "journey" replaced "streak" with the rename.)
    JOURNEY("journey", Icons.Outlined.Route, Icons.Filled.Route),
    LIBRARY("library", Icons.Outlined.MenuBook, Icons.Filled.MenuBook),
    SETTINGS("settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}

/**
 * Floating pill navigation.
 *
 * The selected tab is marked three ways on purpose — a filled glyph, the gold
 * tint, and a soft pill behind it — so the current position survives greyscale,
 * colour-blindness, and a glance. Everything animates over a single short tween;
 * the icon swap itself is instantaneous so the tap feels immediate.
 */
@Composable
fun AgpeyaBottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val s = LocalStrings.current
    val motion = LocalMotion.current
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
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm),
            ) {
                Tab.entries.forEach { tab ->
                    val selected = tab == current
                    val tint by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = motion.spec(Motion.fast),
                        label = "tabTint",
                    )
                    val indicator by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = motion.spec(Motion.standard),
                        label = "tabIndicator",
                    )
                    // Equal share of the width on every screen size; labels never wrap.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            // Role.Tab tells a screen reader this is one of a set
                            // of tabs and reads its selected state; the visible
                            // label below supplies the name. (selectable has no
                            // onClickLabel — that parameter belongs to clickable.)
                            .selectable(
                                selected = selected,
                                role = androidx.compose.ui.semantics.Role.Tab,
                                onClick = { if (!selected) onSelect(tab) },
                            )
                            .padding(vertical = Spacing.xs),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (indicator > 0f) {
                                Box(
                                    Modifier
                                        .size(width = 40.dp, height = 26.dp)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.secondary
                                                .copy(alpha = 0.14f * indicator),
                                        ),
                                )
                            }
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(IconSize.medium),
                            )
                        }
                        Spacer(Modifier.height(Spacing.xxs))
                        Text(
                            label(tab),
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
