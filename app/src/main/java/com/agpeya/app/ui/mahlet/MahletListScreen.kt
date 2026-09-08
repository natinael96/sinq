package com.agpeya.app.ui.mahlet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont

/**
 * The whole book of ማኅሌት, in the order the year sings it.
 *
 * One row is a feast, not a service. The book gives a feast a ዋዜማ the evening
 * before and the ማኅሌት at dawn, so listing those separately broke each feast in
 * half and made the index twice as long as the book: 37 rows where there are
 * 22 things. Each row now carries its services beside it.
 *
 * The Ethiopian day leads the row, because that is how a liturgical index is
 * set and it gives the eye a column to run down. The two movable feasts have no
 * day and sit in a group of their own at the foot.
 *
 * The three sub-feasts the transcription has not reached are listed and marked
 * rather than hidden. Hiding them would say the book has nothing there, which
 * is not true — it has an order of service and this app does not have it.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun MahletListScreen(onBack: () -> Unit, onOpen: (subFeastKey: String) -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val feasts by produceState(emptyList<GitsaweRepository.FeastMahlets>()) {
        value = GitsaweRepository.mahletsByFeast(context)
    }
    val byMonth = feasts.groupBy { it.feast?.monthNum }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.mahletTitle, subtitle = s.mahletSubtitle, onBack = onBack) },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            // Dated months first, in the year's order; the movable feasts last,
            // headed by their own name rather than a month they do not sit in.
            val groups = byMonth.entries.sortedBy { it.key ?: 99 }
            groups.forEach { (monthNum, items) ->
                item(key = "h_${monthNum ?: 0}") {
                    Spacer(Modifier.height(Spacing.md))
                    SectionHeader(
                        monthNum?.let { s.ethMonths.getOrNull(it - 1) }
                            ?: items.firstOrNull()?.feastName.orEmpty(),
                    )
                }
                items(items.size, key = { items[it].orders.first().subFeast.key }) { i ->
                    FeastRow(feast = items[i], showDay = monthNum != null, onOpen = onOpen)
                }
            }
            item { Spacer(Modifier.height(Spacing.xxl)) }
        }
    }
}

/** One feast: its day, its name, and a chip for each service it appoints. */
@Composable
private fun FeastRow(
    feast: GitsaweRepository.FeastMahlets,
    showDay: Boolean,
    onOpen: (String) -> Unit,
) {
    val s = LocalStrings.current
    val first = feast.orders.firstOrNull() ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (feast.complete) Modifier.clickable { onOpen(first.subFeast.key) } else Modifier,
            )
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDay) {
            Text(
                text = feast.feast?.day?.let { geezNumeral(it) }.orEmpty(),
                style = MaterialTheme.typography.titleMedium.inReadingFont(),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(34.dp),
            )
            Spacer(Modifier.width(Spacing.md))
        }
        Column(Modifier.weight(1f)) {
            Text(
                feast.feastName.orEmpty(),
                style = MaterialTheme.typography.titleSmall.inReadingFont(),
                color = if (feast.complete) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xxs))
            if (feast.complete) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    feast.orders.forEach { order ->
                        Text(
                            "${if (order.isEve) s.mahletVigil else s.mahletDawn} ${geezNumeral(order.mahlet.detail.size)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text(
                    s.mahletNone,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (feast.complete) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(IconSize.medium),
            )
        }
    }
}
