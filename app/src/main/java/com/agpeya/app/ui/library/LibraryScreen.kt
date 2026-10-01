package com.agpeya.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing

/** ቤተ መጻሕፍት — Scripture is one entry; its categories live in its hub. */
@Composable
fun LibraryScreen(
    onSearch: () -> Unit,
    onOpenScriptures: () -> Unit,
    onOpenWudase: () -> Unit,
    onOpenBahreHasab: () -> Unit,
    onOpenBooks: () -> Unit,
    onOpenSynaxarium: () -> Unit,
    onOpenReading: () -> Unit,
    onOpenMarks: () -> Unit,
) {
    val s = LocalStrings.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item {
                Spacer(Modifier.height(Spacing.xl))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
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
                    }
                    IconButton(onClick = onSearch) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = s.tabSearch,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
            }

            // ── ፩. ቅዱሳት መጻሕፍትና ጸሎት ───────────────────────────────────
            item {
                SectionHeader(
                    text = if (s.isAmharic) "ቅዱሳት መጻሕፍትና ጸሎት" else "Scriptures & Devotion",
                    modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs),
                )
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
                com.agpeya.app.ui.reading.ReadingHeroCard(onOpen = onOpenReading)
            }
            item {
                LibraryCard(
                    icon = Icons.Outlined.Favorite,
                    title = s.wudaseMariam,
                    subtitle = s.wudaseScheduleSubtitle,
                    onClick = onOpenWudase,
                )
            }

            // ── ፪. የቤተ ክርስቲያን መጻሕፍትና ትውፊት ─────────────────────────
            item {
                SectionHeader(
                    text = if (s.isAmharic) "የቤተ ክርስቲያን መጻሕፍት" else "Church Tradition",
                    modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.xs),
                )
            }
            item {
                LibraryCard(
                    icon = Icons.Outlined.AutoStories,
                    title = s.synaxariumTitle,
                    subtitle = s.synaxariumLibrarySubtitle,
                    onClick = onOpenSynaxarium,
                )
            }
            item {
                LibraryCard(
                    icon = Icons.Outlined.AutoStories,
                    title = s.booksTitle,
                    subtitle = s.booksSubtitle,
                    onClick = onOpenBooks,
                )
            }
            item {
                LibraryCard(
                    icon = Icons.Outlined.CalendarMonth,
                    title = s.bahreHasabTitle,
                    subtitle = s.bahreHasabSubtitle,
                    onClick = onOpenBahreHasab,
                )
            }

            // ── ፫. የግል ምልክቶች ─────────────────────────────────────────
            item {
                SectionHeader(
                    text = if (s.isAmharic) "የግል ምልክቶች" else "Reader's Marks",
                    modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.xs),
                )
            }
            item {
                LibraryCard(
                    icon = Icons.Outlined.BookmarkBorder,
                    title = s.marksTitle,
                    subtitle = "${s.marksTabBookmarks} · ${s.marksTabHighlights} · ${s.marksTabNotes}",
                    onClick = onOpenMarks,
                )
            }

            item {
                Spacer(Modifier.height(Spacing.xxl))
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
    SinqCard(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
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
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(IconSize.small),
            )
        }
    }
}
