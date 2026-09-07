package com.agpeya.app.ui.mahlet

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.DayMahlet
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.ui.common.ListRow
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing

/**
 * The whole book of ማኅሌት, in the order the year sings it.
 *
 * Grouped by Ethiopian month, ዋዜማ before ነግሥ within a feast, so a singer can
 * find a service without waiting for its day to come round.
 *
 * The three sub-feasts the transcription has not reached yet are listed and
 * marked rather than hidden. Hiding them would say the book has nothing there,
 * which is not true — it has an order of service and this app does not have it.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun MahletListScreen(onBack: () -> Unit, onOpen: (subFeastKey: String) -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val all by produceState(emptyList<DayMahlet>()) {
        value = GitsaweRepository.allMahlets(context)
    }
    val byMonth = all.groupBy { it.feast?.monthNum }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.mahletTitle, subtitle = s.mahletSubtitle, onBack = onBack) },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            byMonth.forEach { (monthNum, items) ->
                item(key = "h_${monthNum ?: 0}") {
                    Spacer(Modifier.height(Spacing.md))
                    // A movable feast belongs to no month; it is headed by its
                    // own name instead of a month it does not sit in.
                    SectionHeader(
                        monthNum?.let { s.ethMonths.getOrNull(it - 1) }
                            ?: items.firstOrNull()?.feast?.amharicName.orEmpty(),
                    )
                    Spacer(Modifier.height(Spacing.xs))
                }
                items(items.size, key = { items[it].subFeast.key }) { i ->
                    val entry = items[i]
                    val parts = entry.mahlet.detail.size
                    ListRow(
                        title = entry.subFeast.amharicName,
                        subtitle = listOfNotNull(
                            if (entry.isEve) s.mahletVigil else s.mahletDawn,
                            if (parts == 0) s.mahletNone else s.mahletParts(parts),
                        ).joinToString("  ·  "),
                        enabled = parts > 0,
                        onClick = if (parts > 0) ({ onOpen(entry.subFeast.key) }) else null,
                    )
                }
            }
            item { Spacer(Modifier.height(Spacing.xxl)) }
        }
    }
}
