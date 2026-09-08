package com.agpeya.app.ui.books

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.BookRepository
import com.agpeya.app.model.BookIndex
import com.agpeya.app.model.BookMeta
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.reading.ReadingColumn
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont

/**
 * ሌሎች መጻሕፍት — the church books outside the 81.
 *
 * Ninety-one of them, so the shelf is grouped the way the Church groups them
 * rather than listed flat: the chant books ማኅሌት and ሰዓታት are sung from, the
 * መልክእ hymns, the ድርሳናት, the ገድላት, the ቅዳሴ and the prayers. Seventy of the
 * ninety-one are መልክእ, which is why that group needs a heading of its own —
 * without one the shelf reads as a single run of near-identical names.
 *
 * Each row says how long the book is, because these run from a thousand
 * characters to three hundred thousand.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun BookShelfScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val index by produceState(BookIndex()) { value = BookRepository.index(context) }
    val total = index.shelves.sumOf { it.books.size }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.booksTitle,
                subtitle = if (total > 0) s.booksCount(total) else s.booksSubtitle,
                onBack = onBack,
            )
        },
    ) { inner ->
        ReadingColumn(innerPadding = inner) {
            index.shelves.forEach { shelf ->
                item(key = "h_${shelf.key}") {
                    Spacer(Modifier.height(Spacing.md))
                    SectionHeader(shelf.name)
                    Text(
                        shelf.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                }
                items(shelf.books.size, key = { shelf.books[it].id }) { i ->
                    BookRow(shelf.books[i], onOpen)
                }
            }
            item { Spacer(Modifier.height(Spacing.huge)) }
        }
    }
}

/** One book: its name, then how long it is and what language it is in. */
@Composable
private fun BookRow(book: BookMeta, onOpen: (String) -> Unit) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable { onOpen(book.id) }
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                book.title,
                style = MaterialTheme.typography.titleSmall.inReadingFont(),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    if (book.chapterCount > 1) s.booksChapters(book.chapterCount)
                    else s.booksStanzas(book.blockCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Said only when it is worth saying: the shelf is Ge'ez by
                // default, so labelling every መልክእ "ግዕዝ" would be noise.
                when (book.lang) {
                    "amh" -> s.booksInAmharic
                    "mixed" -> s.booksBilingual
                    else -> null
                }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize.medium),
        )
    }
}
