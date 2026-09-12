package com.agpeya.app.ui.mahlet

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.MahletRepository
import com.agpeya.app.model.MahletOrder
import com.agpeya.app.model.MahletSeason
import com.agpeya.app.ui.common.EthiopianDate
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.reading.ReadingColumn
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.sinqColors
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * ዘመነ ጽጌ, resolved against the year.
 *
 * The season runs from መስከረም ፳፮ to ኅዳር ፮ and its services fall on its Sundays,
 * which move — so the አቋቋም books give an order for every date a Sunday can land
 * on, forty-one of them. The ግጻዌ could only ever call these "the fourth week",
 * because a printed lectionary has no way to know the year.
 *
 * This screen does know. It works out which dates are Sundays this year, shows
 * those first as the season's actual services, and keeps the rest below as what
 * they are: the orders for the years when the dates fall differently.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun MahletSeasonScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current

    val orders by produceState(emptyList<MahletOrder>()) {
        // The season spans መስከረም, ጥቅምት and ኅዳር; each month is read once.
        value = (SEASON_MONTHS.flatMap { MahletRepository.month(context, it) })
            .filter { it.season == MahletSeason.TSIGE }
            .sortedWith(compareBy({ it.month ?: 99 }, { it.day ?: 99 }))
    }

    val today = remember0()
    // Which of the season's dates are Sundays in the current Ethiopian year.
    val sundays = androidx.compose.runtime.remember(orders, today) {
        orders.filter { o ->
            val month = o.month ?: return@filter false
            val day = o.day ?: return@filter false
            gregorianOf(today.year, month, day)?.dayOfWeek == DayOfWeek.SUNDAY
        }
    }
    // An order with no day cannot be tested against a Sunday, so it belongs in
    // neither the appointed list nor the other-years list.
    val undated = androidx.compose.runtime.remember(orders) { orders.filter { it.day == null } }
    val rest = androidx.compose.runtime.remember(orders, sundays, undated) {
        orders - sundays.toSet() - undated.toSet()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.mahletTsige,
                subtitle = s.mahletTsigeSubtitle(orders.size),
                onBack = onBack,
            )
        },
    ) { inner ->
        ReadingColumn(innerPadding = inner) {
            if (sundays.isNotEmpty()) {
                item(key = "hs") {
                    Spacer(Modifier.height(Spacing.sm))
                    SectionHeader(s.mahletTsigeThisYear(today.year))
                    Text(
                        s.mahletTsigeThisYearBody,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                }
                items(sundays.size, key = { sundays[it].id }) { i ->
                    SeasonOrderRow(sundays[i], appointed = true, onOpen = onOpen)
                }
            }
            if (rest.isNotEmpty()) {
                item(key = "hr") {
                    Spacer(Modifier.height(Spacing.lg))
                    SectionHeader(s.mahletTsigeOtherYears)
                    Text(
                        s.mahletTsigeOtherYearsBody,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                }
                items(rest.size, key = { rest[it].id }) { i ->
                    SeasonOrderRow(rest[i], appointed = false, onOpen = onOpen)
                }
            }
            if (undated.isNotEmpty()) {
                item(key = "hu") {
                    Spacer(Modifier.height(Spacing.lg))
                    SectionHeader(s.mahletUndated)
                }
                items(undated.size, key = { undated[it].id }) { i ->
                    SeasonOrderRow(undated[i], appointed = false, onOpen = onOpen)
                }
            }
            item { Spacer(Modifier.height(Spacing.huge)) }
        }
    }
}

/**
 * መስከረም, ጥቅምት, ኅዳር — the three the season touches — and 0.
 *
 * Month 0 is the bucket for orders no book dates, and three ጽጌ orders sit in it:
 * the seven-year አቋቋም and the ጥቅምት and ኅዳር ones. The list screen filters every
 * ጽጌ order out of its own pages on the reasoning that the season has a door of
 * its own, and this was that door reading only 1, 2 and 3 — so those three were
 * reachable from nowhere in the app, forty-seven parts with no route in. It is
 * also why the door said ፵፩ and the page behind it said ፴፰.
 */
private val SEASON_MONTHS = listOf(1, 2, 3, 0)

@Composable
private fun remember0(): EthiopianDate =
    androidx.compose.runtime.remember { EthiopianDate.from(LocalDate.now()) }

/** The Gregorian date an Ethiopian day falls on, or null if the year has no such day. */
private fun gregorianOf(year: Int, month: Int, day: Int): LocalDate? =
    runCatching { EthiopianDate(year, month, day).toGregorian() }.getOrNull()

/** One dated order: its day, its feast, and whether the year appoints it. */
@Composable
private fun SeasonOrderRow(order: MahletOrder, appointed: Boolean, onOpen: (String) -> Unit) {
    val s = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(
                if (appointed) Modifier
                    .padding(vertical = Spacing.xxs)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                else Modifier,
            )
            .clickable { onOpen(order.id) }
            .padding(horizontal = if (appointed) Spacing.md else 0.dp, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            order.day?.let { geezNumeral(it) }.orEmpty(),
            style = MaterialTheme.typography.titleMedium.inReadingFont(),
            color = if (appointed) sinqColors.arke else MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(32.dp),
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            // The feast, not the month. Thirty-three of these rows read
            // "ጥቅምት · ፲፯ ክፍል" and nothing else, told apart only by the numeral
            // in the day column — a wall rather than a list.
            Text(
                order.feast.ifBlank {
                    order.month?.let { s.ethMonths.getOrNull(it - 1) }.orEmpty()
                },
                style = MaterialTheme.typography.titleSmall.inReadingFont(),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                listOfNotNull(
                    order.month?.let { s.ethMonths.getOrNull(it - 1) },
                    s.mahletParts(order.parts.size),
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize.medium),
        )
    }
}
