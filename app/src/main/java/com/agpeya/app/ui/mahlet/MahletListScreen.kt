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
import com.agpeya.app.data.MahletComputus
import com.agpeya.app.data.MahletRepository
import com.agpeya.app.model.MahletIndex
import com.agpeya.app.model.MahletKind
import com.agpeya.app.model.MahletOrder
import com.agpeya.app.model.MahletOrderMeta
import com.agpeya.app.model.MahletSeason
import com.agpeya.app.ui.common.EthiopianDate
import com.agpeya.app.ui.common.SelectPill
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.formatEthiopianShort
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.sinqColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close

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

    val index by produceState(MahletIndex()) { value = MahletRepository.index(context) }
    val today by produceState(emptyList<MahletOrder>()) {
        value = MahletRepository.ordersOn(context, LocalDate.now())
    }
    var query by rememberSaveable { mutableStateOf("") }

    // The dated months, in the year's order. Month 0 holds the orders no book
    // dates; it goes last, under its own heading rather than a month's name.
    val months = remember(index) {
        index.months.sortedBy { if (it.month == 0) 99 else it.month }
    }
    val total = remember(index) { index.months.sumOf { it.orders.size } }
    val tsigeCount = remember(index) {
        index.months.sumOf { m -> m.orders.count { it.season == MahletSeason.TSIGE } }
    }

    // A month earns a page if it has dated orders of its own, or if it is the
    // one that carries the ዘመነ ጽጌ door.
    val pages = remember(months, tsigeCount) {
        months.filter {
            groupByFeast(it.orders.filter { o -> o.season != MahletSeason.TSIGE }).isNotEmpty() ||
                (it.month == TSIGE_HOME && tsigeCount > 0)
        }
    }
    if (pages.isEmpty()) {
        MahletEmpty(s.mahletTitle, onBack)
        return
    }

    // Open on the month the year is actually in, so the book arrives where the
    // reader is rather than at መስከረም every time.
    val startPage = remember(pages) {
        val m = EthiopianDate.from(LocalDate.now()).month
        pages.indexOfFirst { it.month == m }.takeIf { it >= 0 } ?: 0
    }
    val pager = rememberPagerState(initialPage = startPage) { pages.size }

    val hits = remember(index, query) { searchOrders(index, query) }

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
            MahletSearchField(query, { query = it }, Modifier.padding(horizontal = Spacing.screen))

            if (query.isNotBlank()) {
                // Searching leaves the months behind: a feast is looked up by
                // name, and which month it falls in is the answer, not the way in.
                SearchResults(hits, onOpen)
                return@Column
            }

            // The strip is the pager's own tab row now: tapping moves the pages
            // and swiping moves the strip, so the two can never disagree about
            // which month is showing — which is what the old one could not say
            // at all, every pill being drawn unselected.
            MonthStrip(
                months = pages,
                selected = pager.currentPage,
                onJump = { i -> scope.launch { pager.animateScrollToPage(i) } },
            )
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                val m = pages[page]
                val groups = remember(m) {
                    groupByFeast(m.orders.filter { it.season != MahletSeason.TSIGE })
                }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Spacing.screen),
                ) {
                    // Today's order rides on the month it belongs to rather
                    // than above the pager, so swiping away from ጥቅምት does not
                    // leave a card for a ጥቅምት feast hanging over ኅዳር.
                    if (today.isNotEmpty() && m.month == EthiopianDate.from(LocalDate.now()).month) {
                        item(key = "today") { TodayOrder(today, onOpen) }
                    }
                    if (m.month == TSIGE_HOME && tsigeCount > 0) {
                        item(key = "tsige") {
                            Spacer(Modifier.height(Spacing.sm))
                            SeasonRow(tsigeCount, onOpenSeason)
                        }
                    }
                    items(groups.size, key = { groups[it].first().id }) { i ->
                        FeastRow(groups[i], onOpen)
                    }
                    if (groups.isEmpty()) {
                        item(key = "none") { MonthHasNothing(s.mahletNone) }
                    }
                    item { Spacer(Modifier.height(Spacing.huge)) }
                }
            }
        }
    }
}

/** Feast name, part key or day, folded the way the app's search folds. */
private fun searchOrders(index: MahletIndex, query: String): List<MahletOrderMeta> {
    val q = com.agpeya.app.search.AmharicSearch.fold(query.trim())
    if (q.isBlank()) return emptyList()
    return index.months
        .sortedBy { if (it.month == 0) 99 else it.month }
        .flatMap { m -> m.orders.map { m.month to it } }
        .filter { (_, o) -> com.agpeya.app.search.AmharicSearch.fold(o.feast).contains(q) }
        .sortedWith(compareBy({ (_, o) -> o.day ?: 99 }))
        .map { (_, o) -> o }
}

@Composable
private fun MahletSearchField(value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValue,
        placeholder = { Text(s.mahletSearchHint, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(
                androidx.compose.material.icons.Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(IconSize.medium),
            )
        },
        trailingIcon = if (value.isNotBlank()) ({
            androidx.compose.material3.IconButton(onClick = { onValue("") }) {
                Icon(
                    androidx.compose.material.icons.Icons.Outlined.Close,
                    contentDescription = s.clearAction,
                    modifier = Modifier.size(IconSize.medium),
                )
            }
        }) else null,
        singleLine = true,
        modifier = modifier.fillMaxWidth().padding(top = Spacing.sm),
    )
}

@Composable
private fun SearchResults(hits: List<MahletOrderMeta>, onOpen: (String) -> Unit) {
    val s = LocalStrings.current
    if (hits.isEmpty()) {
        MonthHasNothing(s.noResults)
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screen),
    ) {
        items(hits.size, key = { hits[it].id }) { i -> FeastRow(listOf(hits[i]), onOpen) }
        item { Spacer(Modifier.height(Spacing.huge)) }
    }
}

/** Said on the three mornings in four that appoint nothing. */
@Composable
private fun MonthHasNothing(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xxl),
        textAlign = TextAlign.Center,
    )
}

/** A blank index is a broken install, not an empty month; say so rather than nothing. */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun MahletEmpty(title: String, onBack: () -> Unit) {
    val s = LocalStrings.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = title, onBack = onBack) },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) { MonthHasNothing(s.mahletNone) }
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
private fun MonthStrip(
    months: List<com.agpeya.app.model.MahletMonth>,
    selected: Int,
    onJump: (Int) -> Unit,
) {
    val s = LocalStrings.current
    val scroll = rememberScrollState()
    // Keep the lit month on screen. Fourteen pills run about three times the
    // width of a phone, so the one that matters is usually off it.
    LaunchedEffect(selected) {
        val approx = (selected * 88) - 96
        scroll.animateScrollTo(approx.coerceIn(0, scroll.maxValue))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(horizontal = Spacing.screen, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        months.forEachIndexed { i, m ->
            SelectPill(
                label = if (m.month == 0) s.mahletUndated
                else s.ethMonths.getOrNull(m.month - 1).orEmpty(),
                selected = i == selected,
                onClick = { onJump(i) },
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
            // Each kind opens its own order. These read as targets and were
            // not: the row always opened the ዋዜማ, because vigil sorts first,
            // so on the fifty feasts that have both, a reader after the morning
            // ማኅሌት always landed on last night's service.
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                group.forEach { o ->
                    Text(
                        "${s.mahletKindLabel(o.kind)} ${geezNumeral(o.parts)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (group.size > 1) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = if (group.size > 1) {
                            Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onOpen(o.id) }
                                .padding(horizontal = Spacing.xs, vertical = 2.dp)
                        } else Modifier,
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
                // A feast the book could not date has no day column to fill,
                // so it says when it falls this year instead — which is the
                // question a reader with a ሆሣዕና order in front of them has.
                first.movable?.let { key ->
                    val thisYear = remember(key) {
                        MahletComputus.dateOf(key, EthiopianDate.from(LocalDate.now()).year)
                    }
                    thisYear?.let {
                        Text(
                            s.mahletThisYearOn(formatEthiopianShort(it, s)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
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
