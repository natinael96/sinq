package com.agpeya.app.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.agpeya.app.ui.strings.LocalStrings

enum class Tab(val route: String, val icon: ImageVector) {
    HOME("home", Icons.Outlined.Home),
    SEARCH("search", Icons.Outlined.Search),
    BOOKMARKS("bookmarks", Icons.Outlined.Bookmarks),
    SETTINGS("settings", Icons.Outlined.Settings),
}

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
    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == current,
                onClick = { if (tab != current) onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = label(tab)) },
                label = { Text(label(tab), style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        }
    }
}
