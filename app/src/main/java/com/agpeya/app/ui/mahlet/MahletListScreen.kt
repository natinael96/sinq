package com.agpeya.app.ui.mahlet

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.MahletRepository
import com.agpeya.app.model.MahletIndex
import com.agpeya.app.model.MahletKind
import com.agpeya.app.model.MahletOrder
import com.agpeya.app.model.MahletOrderMeta
import com.agpeya.app.model.MahletSeason
import com.agpeya.app.ui.common.SelectPill
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.sinqColors
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The whole year of ማኅሌት, in the order it is sung.
 *
 * A hundred and forty-two orders is too many for one list to be read down, so
 * the screen is built to be *entered* rather than scrolled: today's order sits
 * at the top when there is one, a month strip moves the year under the thumb,
 * and ዘመነ ጽጌ — forty-one of the hundred and forty-two — folds into a single row
 * rather than burying ጥቅምት under twenty-nine near-identical dates.
 *
 * One row is a feast, not a service. A feast is given a ዋዜማ the evening before
 * and the ማኅሌት at dawn, so listing those apart broke each feast in half and
 * made the index twice as long as the book.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun MahletListScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onOpenSeason: () -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val index by produceState(MahletIndex()) { value = MahletRepository.index(context) }
    val today by produceState(emptyList<MahletOrder>()) {
        value = MahletRepository.ordersOn(context, LocalDate.now())
    }

    // The dated months, in the year's order. Month 0 holds the orders no book
    // dates; it goes last, under its own heading rather than a month's name.
    val months = remember(index) {
        index.months.sortedBy { if (it.month == 0) 99 else it.month }
    }
    val total = remember(index) { index.months.sumOf { it.orders.size } }
    val tsigeCount = remember(index) {
        index.months.sumOf { m -> m.orders.count { it.season == MahletSeason.TSIGE } }
    }

    // Where each month's header lands, so the strip can jump to it. Counted the
    // same way the list is built, which is the only way the two stay in step.
    val anchors = remember(months, today, tsigeCount) {
        var row = if (today.isEmpty()) 0 else 1
        buildMap {
            months.forEach { m ->
                val dated = m.orders.filter { it.season != MahletSeason.TSIGE }
                val groups = groupByFeast(dated)
                if (groups.isEmpty() && m.month != TSIGE_HOME) return@forEach
                put(m.month, row)
                row += 1 + groups.size + (if (m.month == TSIGE_HOME) 1 else 0)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.mahletTitle,
                subtitle = if (total > 0) s.mahletOrders(total) else s.mahletSubtitle,
                onBack = onBack,
            )
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            if (months.size > 1) {
                MonthStrip(
                    months = months.filter { anchors.containsKey(it.month) },
                    onJump = { month ->
                        anchors[month]?.let { scope.launch { listState.scrollToItem(it) } }
                    },
                )
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = Spacing.screen),
            ) {
                if (today.isNotEmpty()) {
                    item(key = "today") { TodayOrder(today, onOpen) }
                }
                months.forEach { m ->
                    val dated = m.orders.filter { it.season != MahletSeason.TSIGE }
                    val groups = groupByFeast(dated)
                    if (groups.isEmpty() && m.month != TSIGE_HOME) return@forEach
                    item(key = "h${m.month}") {
                        Spacer(Modifier.height(Spacing.lg))
                        SectionHeader(
                            if (m.month == 0) s.mahletUndated
                            else s.ethMonths.getOrNull(m.month - 1).orEmpty(),
                        )
                    }
                    // ዘመነ ጽጌ sits in ጥቅምት, where most of its Sundays fall, and
                    // opens its own screen: forty-one dated orders in a month
                    // list would be the month.
                    if (m.month == TSIGE_HOME && tsigeCount > 0) {
                        item(key = "tsige") { SeasonRow(tsigeCount, onOpenSeason) }
                    }
                    items(groups.size, key = { groups[it].first().id }) { i ->
                        FeastRow(groups[i], onOpen)
                    }
                }
                item { Spacer(Modifier.height(Spacing.huge)) }
            }
        }
    }
}

/** ዘመነ ጽጌ's Sundays fall mostly in ጥቅምት, so its door sits there. */
private const val TSIGE_HOME = 2

/**
 * Orders of the same feast on the same day, together.
 *
 * The book gives a feast its ዋዜማ on the eve and its ማኅሌት at dawn, and names
 * them so nearly alike that two rows read as a repetition rather than a pair.
 */
private fun groupByFeast(orders: List<MahletOrderMeta>): List<List<MahletOrderMeta>> =
    orders.groupBy { it.feast.trim() to it.day }
        .toList()
        .sortedBy { (key, _) -> key.second ?: 99 }
        .map { (_, group) -> group.sortedBy { MahletKind.rank(it.kind) } }

/** A horizontally scrolling strip of the months that hold orders. */
@Composable
private fun MonthStrip(months: List<com.agpeya.app.model.MahletMonth>, onJump: (Int) -> Unit) {
    val s = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        months.forEach { m ->
            SelectPill(
                label = if (m.month == 0) s.mahletUndated
                else s.ethMonths.getOrNull(m.month - 1).orEmpty(),
                selected = false,
                onClick = { onJump(m.month) },
            )
        }
    }
}

/**
 * Today's order, when the year appoints one.
 *
 * The reason the whole book exists is to be sung on a particular morning, and
 * on most mornings there is nothing — so when there is, it is the first thing
 * on the screen rather than something to be found by scrolling to the month.
 */
@Composable
private fun TodayOrder(orders: List<MahletOrder>, onOpen: (String) -> Unit) {
    val s = LocalStrings.current
    val first = orders.first()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = Spacing.md)
            .clip(RoundedCornerShape(16.dp))
            .background(sinqColors.hero)
            .clickable { onOpen(first.id) }
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
    ) {
        Text(
            s.mahletToday,
            style = MaterialTheme.typography.labelSmall,
            color = sinqColors.onHeroGold,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            first.feast,
            style = MaterialTheme.typography.titleMedium.inReadingFont(),
            color = sinqColors.onHero,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            orders.forEach { o ->
                Text(
                    "${s.mahletKindLabel(o.kind)}  ${geezNumeral(o.parts.size)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = sinqColors.onHeroMuted,
                )
            }
        }
    }
}

/** ዘመነ ጽጌ, as one door rather than forty-one rows. */
@Composable
private fun SeasonRow(count: Int, onClick: () -> Unit) {
    val s = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                s.mahletTsige,
                style = MaterialTheme.typography.titleSmall.inReadingFont(),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                s.mahletTsigeSubtitle(count),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(IconSize.medium),
        )
    }
}

/** One feast: its day in Ge'ez, its name, and a chip for each service. */
@Composable
private fun FeastRow(group: List<MahletOrderMeta>, onOpen: (String) -> Unit) {
    val s = LocalStrings.current
    val first = group.first()
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clickable { onOpen(first.id) }
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The day leads, because that is how a liturgical index is set and it
        // gives the eye a column to run down.
        Text(
            first.day?.let { geezNumeral(it) }.orEmpty(),
            style = MaterialTheme.typography.titleMedium.inReadingFont(),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(34.dp),
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                first.feast,
                style = MaterialTheme.typography.titleSmall.inReadingFont(),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                group.forEach { o ->
                    Text(
                        "${s.mahletKindLabel(o.kind)} ${geezNumeral(o.parts)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Said only where it is true: most orders come from the scanned
                // book, and labelling those would be labelling everything.
                first.source?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize.medium),
        )
    }
}
