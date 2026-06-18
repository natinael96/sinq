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
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
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
    SEARCH("search", Icons.Outlined.Search),
    BOOKMARKS("bookmarks", Icons.Outlined.Bookmarks),
    SETTINGS("settings", Icons.Outlined.Settings),
}

/** Floating pill navigation — part of the green & gold restyle. */
@Composable
fun AgpeyaBottomBar(current: Tab, onSelect: (Tab) -> Unit) {
    val s = LocalStrings.current
    val label: (Tab) -> String = {
        when (it) {
            Tab.HOME -> s.tabHome
            Tab.SEARCH -> s.tabSearch
            Tab.BOOKMARKS -> s.tabBookmarks
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
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Tab.entries.forEach { tab ->
                    val selected = tab == current
                    val tint = if (selected) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { if (!selected) onSelect(tab) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Icon(tab.icon, contentDescription = label(tab), tint = tint)
                        Text(label(tab), style = MaterialTheme.typography.labelMedium, color = tint)
                    }
                }
            }
        }
    }
}
