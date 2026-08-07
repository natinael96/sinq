package com.agpeya.app.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.common.AgpeyaBottomBar
import com.agpeya.app.ui.common.Tab
import com.agpeya.app.ui.strings.LocalStrings

/** ቤተ መጻሕፍት — the library hub: the Psalter, the scriptures, and (soon) more. */
@Composable
fun LibraryScreen(
    onOpenPsalter: () -> Unit,
    onOpenScriptures: () -> Unit,
    onOpenWudase: () -> Unit,
    onSelectTab: (Tab) -> Unit,
) {
    val s = LocalStrings.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AgpeyaBottomBar(current = Tab.LIBRARY, onSelect = onSelectTab) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(20.dp))
                Text(s.libraryTitle, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    s.librarySubtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
            }
            item {
                LibraryCard(
                    icon = Icons.Outlined.LibraryMusic,
                    title = s.psalterTitle,
                    subtitle = "፻፶ መዝሙራት",
                    onClick = onOpenPsalter,
                )
            }
            item {
                LibraryCard(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = s.scripturesTitle,
                    subtitle = s.scripturesSubtitle,
                    onClick = onOpenScriptures,
                )
            }
            item {
                LibraryCard(icon = Icons.Outlined.MenuBook, title = s.wudaseMariam, subtitle = "ሰኞ–እሑድ", onClick = onOpenWudase)
            }
            item {
                LibraryCard(icon = Icons.Outlined.MenuBook, title = s.zewotrTselot, subtitle = s.comingSoon, enabled = false, onClick = {})
            }
        }
    }
}

@Composable
private fun LibraryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontStyle = if (enabled) FontStyle.Normal else FontStyle.Italic,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
