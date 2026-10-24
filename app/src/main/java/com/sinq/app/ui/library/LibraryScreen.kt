package com.sinq.app.ui.library

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
import androidx.compose.material.icons.outlined.Church
import androidx.compose.material.icons.outlined.FormatQuote
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sinq.app.ui.common.SectionHeader
import com.sinq.app.ui.common.SinqCard
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.IconSize
import com.sinq.app.ui.theme.Spacing

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
    onOpenDesertFathersHub: () -> Unit = {},
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
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Spacer(Modifier.height(Spacing.lg))
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
                Spacer(Modifier.height(Spacing.xs))
            }

            // ── ፩. ቅዱሳት መጻሕፍትና ጸሎት ───────────────────────────────────
            item {
                SectionHeader(
                    text = if (s.isAmharic) "ቅዱሳት መጻሕፍትና ጸሎት" else "Scriptures & Devotion",
                    modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.xxs),
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
                com.sinq.app.ui.reading.ReadingHeroCard(onOpen = onOpenReading)
            }
            item {
                LibraryCard(
                    icon = Icons.Outlined.Church,
                    title = s.wudaseMariam,
                    subtitle = s.wudaseScheduleSubtitle,
                    onClick = onOpenWudase,
                )
            }

            // ── ፪. የቤተ ክርስቲያን መጻሕፍትና ትውፊት ─────────────────────────
            item {
                SectionHeader(
                    text = if (s.isAmharic) "የቤተ ክርስቲያን መጻሕፍት" else "Church Tradition",
                    modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xxs),
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
            item {
                LibraryCard(
                    icon = Icons.Outlined.FormatQuote,
                    title = s.desertFathersHubTitle,
                    subtitle = s.desertFathersLibrarySubtitle,
                    onClick = onOpenDesertFathersHub,
                )
            }

            // ── ፫. የግል ምልክቶች ─────────────────────────────────────────
            item {
                SectionHeader(
                    text = if (s.isAmharic) "የግል ምልክቶች" else "Reader's Marks",
                    modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xxs),
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
                Spacer(Modifier.height(Spacing.xl))
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
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(IconSize.medium),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontStyle = if (enabled) FontStyle.Normal else FontStyle.Italic,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
