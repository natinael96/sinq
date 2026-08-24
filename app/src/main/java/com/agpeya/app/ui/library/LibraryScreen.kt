package com.agpeya.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import com.agpeya.app.ui.common.AgpeyaBottomBar
import com.agpeya.app.ui.common.Tab
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing

/** ቤተ መጻሕፍት — Scripture is one entry; its categories live in its hub. */
@Composable
fun LibraryScreen(
    onOpenScriptures: () -> Unit,
    onOpenWudase: () -> Unit,
    onOpenZewotr: () -> Unit,
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
            contentPadding = PaddingValues(horizontal = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Spacer(Modifier.height(Spacing.xl))
                Text(
                    s.libraryTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    s.librarySubtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
            }
            item {
                LibraryCard(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = s.scripturesTitle,
                    subtitle = "${s.bibleTitle} · ${s.psalterTitle}",
                    onClick = onOpenScriptures,
                )
            }
            item {
                LibraryCard(icon = Icons.AutoMirrored.Outlined.MenuBook, title = s.wudaseMariam, subtitle = s.wudaseScheduleSubtitle, onClick = onOpenWudase)
            }
            // ሰዓታት is hidden for now — the bundled digitization is partial
            // (መሐረነ አብ and other portions are missing). Restore the card once
            // the content is complete; the reader, route and data are intact.
            item {
                LibraryCard(icon = Icons.AutoMirrored.Outlined.MenuBook, title = s.zewotrTselot, subtitle = s.zewotrSubtitle, onClick = onOpenZewotr)
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
    SinqCard(onClick = onClick, enabled = enabled) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(IconSize.large),
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
