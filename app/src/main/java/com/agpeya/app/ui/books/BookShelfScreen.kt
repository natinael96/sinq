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
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.reading.ReadingColumn
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont

/**
 * ሌሎች መጻሕፍት — the church books outside the 81, as six shelves.
 *
 * Ninety-six of them, and seventy are መልክእ: listed on one page, the shelf was a
 * single run of near-identical names that no heading could break up, and the
 * five smaller groups sat below it where nobody scrolled. So the shelf is now
 * the six groups themselves — the way the Church divides them — and each opens
 * its own page. The count on each row is the reason to tap it or not.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun BookShelfScreen(
    onBack: () -> Unit,
    onOpenShelf: (String) -> Unit,
    onOpenMahlets: () -> Unit = {},
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val index by produceState(BookIndex()) { value = BookRepository.index(context) }
    val total = index.shelves.sumOf { it.books.size }
    val mahletOrders by produceState(0) {
        value = runCatching { com.agpeya.app.data.MahletRepository.months(context).sumOf { it.orders.size } }
            .getOrDefault(0)
    }

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
            // ማኅሌት is a book of the Church, not a section of the app, and it
            // used to be the only one reached from ቤት's own page rather than
            // from the shelf the rest of them sit on. Its orders are counted
            // where the shelves count their books.
            item(key = "mahlet") {
                ShelfRow(
                    name = s.mahletTitle,
                    subtitle = s.mahletSubtitle,
                    count = mahletOrders,
                    onOpen = onOpenMahlets,
                )
            }
            items(index.shelves.size, key = { index.shelves[it].key }) { i ->
                val shelf = index.shelves[i]
                ShelfRow(
                    name = shelf.name,
                    subtitle = shelf.subtitle,
                    count = shelf.books.size,
                    onOpen = { onOpenShelf(shelf.key) },
                )
            }
            item { Spacer(Modifier.height(Spacing.huge)) }
        }
    }
}

/**
 * One shelf's books, on its own page.
 *
 * An unknown [shelfKey] draws an empty page under the group name rather than
 * failing: a stale back-stack entry after a content update is not worth a crash.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun BookShelfPageScreen(shelfKey: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val index by produceState(BookIndex()) { value = BookRepository.index(context) }
    val shelf = index.shelves.firstOrNull { it.key == shelfKey }
    val books = shelf?.books.orEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = shelf?.name ?: s.booksTitle,
                subtitle = shelf?.let {
                    if (books.isEmpty()) it.subtitle
                    else "${it.subtitle}  ·  ${s.booksCount(books.size)}"
                },
                onBack = onBack,
            )
        },
    ) { inner ->
        ReadingColumn(innerPadding = inner) {
            items(books.size, key = { books[it].id }) { i -> BookRow(books[i], onOpen) }
            item { Spacer(Modifier.height(Spacing.huge)) }
        }
    }
}

/** One group: its name and what is in it, then how many books that is. */
@Composable
private fun ShelfRow(name: String, subtitle: String, count: Int, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onOpen)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.titleSmall.inReadingFont(),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.width(Spacing.sm))
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize.medium),
        )
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
