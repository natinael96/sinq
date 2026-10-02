package com.agpeya.app.ui.mahlet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.MahletComputus
import com.agpeya.app.data.MahletRepository
import com.agpeya.app.model.MahletIndex
import com.agpeya.app.model.MahletKind
import com.agpeya.app.model.MahletMonth
import com.agpeya.app.model.MahletOrder
import com.agpeya.app.model.MahletOrderMeta
import com.agpeya.app.model.MahletSeason
import com.agpeya.app.ui.common.EthiopianDate
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.formatEthiopianShort
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion
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
@OptIn(ExperimentalMaterial3Api::class)
fun MahletListScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onOpenSeason: () -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val indexLoad = com.agpeya.app.ui.common.rememberContentLoad { MahletRepository.index(context) }
    val index = indexLoad.value ?: MahletIndex()
    if (com.agpeya.app.ui.common.contentLoadScreen(indexLoad, s.mahletTitle, onBack, index.months.isEmpty())) return
    val currentDate by com.agpeya.app.ui.common.rememberCurrentDate()
    val today by produceState(emptyList<MahletOrder>(), currentDate) {
        value = MahletRepository.ordersOn(context, currentDate)
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = isSearching) {
        isSearching = false
        query = ""
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            runCatching { searchFocusRequester.requestFocus() }
        }
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
            if (isSearching) {
                SinqTopBar(
                    title = "",
                    onBack = {
                        isSearching = false
                        query = ""
                    },
                    titleContent = {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(
                                    s.mahletSearchHint,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.secondary,
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        )
                    },
                    actions = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = s.clearAction,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(IconSize.medium),
                                )
                            }
                        }
                    },
                )
            } else {
                SinqTopBar(
                    title = s.mahletTitle,
                    subtitle = if (total > 0) s.mahletOrders(total) else s.mahletSubtitle,
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = s.tabSearch,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(IconSize.medium),
                            )
                        }
                    },
                )
            }
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            if (isSearching) {
                if (query.isNotBlank()) {
                    SearchResults(hits, onOpen)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.screen),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(
                            text = s.mahletSearchHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.xxl),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                MonthStrip(
                    months = pages,
                    selected = pager.currentPage,
                    onJump = { i -> scope.launch { pager.animateScrollToPage(i) } },
                )
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val m = pages[page]
                    val groups = remember(m) {
                        groupByFeast(m.orders.filter { it.season != MahletSeason.TSIGE })
                    }
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.xs),
                    ) {
                        if (today.isNotEmpty() && m.month == EthiopianDate.from(LocalDate.now()).month) {
                            item(key = "today") {
                                TodayOrder(today, onOpen)
                                Spacer(Modifier.height(Spacing.xs))
                            }
                        }
                        if (m.month == TSIGE_HOME && tsigeCount > 0) {
                            item(key = "tsige") {
                                SeasonRow(tsigeCount, onOpenSeason)
                                Spacer(Modifier.height(Spacing.xs))
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
private fun SearchResults(hits: List<MahletOrderMeta>, onOpen: (String) -> Unit) {
    val s = LocalStrings.current
    if (hits.isEmpty()) {
        MonthHasNothing(s.noResults)
        return
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.xs),
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
@OptIn(ExperimentalMaterial3Api::class)
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
    months: List<MahletMonth>,
    selected: Int,
    onJump: (Int) -> Unit,
) {
    val s = LocalStrings.current
    val scroll = rememberScrollState()
    LaunchedEffect(selected) {
        val approx = (selected * 96) - 96
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
            val label = if (m.month == 0) s.mahletUndated
            else s.ethMonths.getOrNull(m.month - 1).orEmpty()
            MonthPill(
                label = label,
                count = m.orders.size,
                selected = i == selected,
                onClick = { onJump(i) },
            )
        }
    }
}

/**
 * Liturgical month selector pill with Ethiopian month title and Ge'ez count badge.
 */
@Composable
private fun MonthPill(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val background by animateColorAsState(
        targetValue = if (selected) sinqColors.hero else MaterialTheme.colorScheme.surface,
        animationSpec = motion.spec(Motion.fast),
        label = "monthPillBg",
    )
    val border by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        animationSpec = motion.spec(Motion.fast),
        label = "monthPillBorder",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) sinqColors.onHero else MaterialTheme.colorScheme.onSurface,
        animationSpec = motion.spec(Motion.fast),
        label = "monthPillText",
    )
    val badgeBg = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
    else MaterialTheme.colorScheme.surfaceVariant
    val badgeColor = if (selected) sinqColors.onHeroGold
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(22.dp))
            .selectable(selected = selected, onClick = onClick)
            .padding(start = Spacing.md, end = Spacing.sm, top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            maxLines = 1,
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(badgeBg)
                    .padding(horizontal = Spacing.xs, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = geezNumeral(count),
                    style = MaterialTheme.typography.labelSmall.inReadingFont(),
                    color = badgeColor,
                )
            }
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            orders.forEach { o ->
                Surface(
                    onClick = { onOpen(o.id) },
                    shape = RoundedCornerShape(8.dp),
                    color = sinqColors.onHero.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, sinqColors.onHeroGold.copy(alpha = 0.35f)),
                    modifier = Modifier.heightIn(min = 40.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Text(
                            text = s.mahletKindLabel(o.kind),
                            style = MaterialTheme.typography.labelMedium,
                            color = sinqColors.onHero,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = geezNumeral(o.parts.size),
                            style = MaterialTheme.typography.labelSmall.inReadingFont(),
                            color = sinqColors.onHeroGold,
                        )
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = sinqColors.onHeroGold.copy(alpha = 0.8f),
                            modifier = Modifier.size(IconSize.small),
                        )
                    }
                }
            }
        }
    }
}

/** ዘመነ ጽጌ, as one door rather than forty-one rows. */
@Composable
private fun SeasonRow(count: Int, onClick: () -> Unit) {
    val s = LocalStrings.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                        RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "ጽጌ",
                    style = MaterialTheme.typography.titleSmall.inReadingFont(),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = s.mahletTsige,
                    style = MaterialTheme.typography.titleMedium.inReadingFont(),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = s.mahletTsigeSubtitle(count),
                    style = MaterialTheme.typography.bodySmall,
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
}

/** Liturgical button for an order service (ዋዜማ or ማኅሌት) meeting 48dp accessibility targets. */
@Composable
private fun ServiceButton(
    label: String,
    partsCount: Int,
    isVigil: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isVigil) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
    }
    val containerColor = if (isVigil) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
    }
    val contentColor = if (isVigil) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.secondary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.heightIn(min = 40.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = geezNumeral(partsCount),
                style = MaterialTheme.typography.labelSmall.inReadingFont(),
                color = contentColor.copy(alpha = 0.85f),
            )
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(IconSize.small),
            )
        }
    }
}

/** One feast: its day in Ge'ez, its name, and distinct liturgical service buttons. */
@Composable
private fun FeastRow(group: List<MahletOrderMeta>, onOpen: (String) -> Unit) {
    val s = LocalStrings.current
    val first = group.first()
    val isMulti = group.size > 1

    Surface(
        onClick = {
            val target = if (isMulti) (group.find { it.kind == MahletKind.MAHLET } ?: first) else first
            onOpen(target.id)
        },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                            RoundedCornerShape(10.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = first.day?.let { geezNumeral(it) } ?: "—",
                        style = MaterialTheme.typography.titleMedium.inReadingFont(),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = first.feast,
                        style = MaterialTheme.typography.titleMedium.inReadingFont(),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle = when {
                        first.movable != null -> {
                            val thisYear = remember(first.movable) {
                                MahletComputus.dateOf(first.movable, EthiopianDate.from(LocalDate.now()).year)
                            }
                            thisYear?.let { s.mahletThisYearOn(formatEthiopianShort(it, s)) }
                        }
                        first.source != null -> first.source
                        else -> null
                    }
                    if (subtitle != null) {
                        Spacer(Modifier.height(Spacing.xxs))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                if (!isMulti) {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(IconSize.medium),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                modifier = Modifier.fillMaxWidth(),
            ) {
                group.forEach { o ->
                    ServiceButton(
                        label = s.mahletKindLabel(o.kind),
                        partsCount = o.parts,
                        isVigil = o.kind == MahletKind.VIGIL,
                        onClick = { onOpen(o.id) },
                    )
                }
            }
        }
    }
}
