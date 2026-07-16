package com.agpeya.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.strings.LocalStrings

enum class Tab(val route: String, val icon: ImageVector) {
    HOME("home", Icons.Outlined.Home),
    STREAK("streak", Icons.Outlined.LocalFireDepartment),
    SETTINGS("settings", Icons.Outlined.Settings),
}

/** Floating pill navigation — part of the green & gold restyle. */
@Composable
fun AgpeyaBottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val s = LocalStrings.current
    val label: (Tab) -> String = {
        when (it) {
            Tab.HOME -> s.tabHome
            Tab.STREAK -> s.tabStreak
            Tab.SETTINGS -> s.tabSettings
        }
    }
    Box(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 14.dp)
            .padding(bottom = 10.dp, top = 6.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Tab.entries.forEach { tab ->
                    val selected = tab == current
                    val tint = if (selected) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                    // Equal share of the width on every screen size; labels never wrap.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { if (!selected) onSelect(tab) }
                            .padding(vertical = 4.dp),
                    ) {
                        Icon(tab.icon, contentDescription = label(tab), tint = tint)
                        Text(
                            label(tab),
                            style = MaterialTheme.typography.labelMedium,
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
