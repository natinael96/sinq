package com.agpeya.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont

/**
 * A reader's own name, and the way to move within it.
 *
 * The chapter used to be a strip of pills above the text, drawn on every screen
 * of every chapter whether or not anyone was changing chapters — and in
 * መዝሙረ ዳዊት it showed four of a hundred and fifty, so the common case was
 * scrolling it sideways. Here the chapter IS the title's second line: it says
 * where you are, and tapping it is how you go somewhere else. The row the strip
 * occupied goes back to the reading.
 *
 * [position] is what is at the top of the screen out of the whole — "፲፪ / ፵፭".
 * Where the content is countable that is a better answer to "where am I" than
 * any bar can give, and unlike the mark down the edge it is legible without
 * scrolling.
 */
@Composable
fun ReaderTitleBar(
    title: String,
    chapterLabel: String?,
    pickable: Boolean,
    onPick: () -> Unit,
    position: String? = null,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium.inReadingFont(),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (chapterLabel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = Spacing.xxs)
                        .clip(RoundedCornerShape(50))
                        .then(if (pickable) Modifier.clickable(onClick = onPick) else Modifier)
                        .padding(horizontal = if (pickable) Spacing.sm else 0.dp, vertical = 1.dp),
                ) {
                    Text(
                        chapterLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (pickable) {
                        Spacer(Modifier.width(Spacing.xs))
                        Icon(
                            Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(IconSize.small),
                        )
                    }
                }
            }
        }
        if (position != null) {
            Spacer(Modifier.width(Spacing.sm))
            Text(
                position,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/**
 * The chapters, as a list rather than a grid of numerals.
 *
 * A grid asks the reader to scan ፻፳፯-shaped glyphs, which are composed rather
 * than positional and read slower than digits do. A list gives each chapter a
 * whole row, and where the book titles its chapters — ሥርዓተ ቅዳሴ names all
 * twenty-three — says the name, because that is what someone is looking for.
 * [labelFor] returns null where a book has no titles and the numeral stands
 * alone. The list opens on the chapter being read.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChapterSheet(
    count: Int,
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
    labelFor: (Int) -> String? = { null },
) {
    val listState = rememberLazyListState()
    LaunchedEffect(current) { listState.scrollToItem((current - 2).coerceAtLeast(0)) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
            items(count) { i ->
                val selected = i == current
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(i) }
                        .background(
                            if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                            else Color.Transparent,
                        )
                        .padding(horizontal = Spacing.screen, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        geezNumeral(i + 1),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.width(48.dp),
                    )
                    Text(
                        labelFor(i).orEmpty(),
                        style = MaterialTheme.typography.bodyMedium.inReadingFont(),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            item { Spacer(Modifier.height(Spacing.huge)) }
        }
    }
}
