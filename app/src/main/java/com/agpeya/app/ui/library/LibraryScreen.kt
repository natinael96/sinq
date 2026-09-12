package com.agpeya.app.ui.library

import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
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
import com.agpeya.app.ui.common.Tab
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.AutoStories

/** ቤተ መጻሕፍት — Scripture is one entry; its categories live in its hub. */
@Composable
fun LibraryScreen(
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
        // The bar belongs to the host that holds all four tabs, and it carries
        // its own navigation-bar padding; this page only insets for the status bar.
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
                // Every other card here is a shelf; this is a commitment being
                // kept, so it reports itself rather than repeating a sentence.
                com.agpeya.app.ui.reading.ReadingHeroCard(onOpen = onOpenReading)
            }
            item {
                LibraryCard(icon = Icons.Outlined.Favorite, title = s.wudaseMariam, subtitle = s.wudaseScheduleSubtitle, onClick = onOpenWudase)
            }
            item {
                // 1.6 MB and 366 days whose only doors were today's ግጻዌ and a
                // search hit — the book was not browsable at all.
                LibraryCard(
                    icon = Icons.Outlined.AutoStories,
                    title = s.synaxariumTitle,
                    subtitle = s.synaxariumLibrarySubtitle,
                    onClick = onOpenSynaxarium,
                )
            }
            item {
                // The scanned church books, ማኅሌት among them now: it is a book
                // of the Church like the rest, and it was the only one with a
                // door of its own on this page. ዘወትር ጸሎት had a card here too,
                // opening the ውዳሴ ማርያም screen at its daily section — one text
                // behind two doors.
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
                // What the reader has left in the books, beside the books.
                LibraryCard(
                    icon = Icons.Outlined.BookmarkBorder,
                    title = s.marksTitle,
                    subtitle = "${s.marksTabBookmarks} · ${s.marksTabHighlights} · ${s.marksTabNotes}",
                    onClick = onOpenMarks,
                )
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
