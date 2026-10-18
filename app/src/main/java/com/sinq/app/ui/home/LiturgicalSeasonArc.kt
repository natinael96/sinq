package com.sinq.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sinq.app.data.BahreHasab
import com.sinq.app.ui.common.EthiopianDate
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.strings.Strings
import com.sinq.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SeasonProgress(
    val name: String,
    val detail: String,
    val progress: Float,
)

fun computeSeasonProgress(date: LocalDate, s: Strings): SeasonProgress {
    val eth = EthiopianDate.from(date)
    val year = eth.year
    val nineveh = BahreHasab.nineveh(year)
    val daysFromNineveh = ChronoUnit.DAYS.between(nineveh, date).toInt()

    return when {
        // Fast of Nineveh (3 days)
        daysFromNineveh in 0..2 -> {
            val day = daysFromNineveh + 1
            val title = if (s.isAmharic) "ጾመ ነነዌ" else "Fast of Nineveh"
            val detail = if (s.isAmharic) "ቀን $day ከ 3" else "Day $day of 3"
            SeasonProgress(title, detail, day / 3f)
        }
        // Great Lent (55 days): Nineveh + 14 to Nineveh + 68
        daysFromNineveh in 14..68 -> {
            val day = daysFromNineveh - 14 + 1
            val week = ((day - 1) / 7) + 1
            val title = if (s.isAmharic) "ዓቢይ ጾም" else "Great Lent"
            val detail = if (s.isAmharic) "ሳምንት $week · ቀን $day ከ 55" else "Week $week · Day $day of 55"
            SeasonProgress(title, detail, (day / 55f).coerceIn(0f, 1f))
        }
        // 50 Days of Joy (በዓለ ሃምሳ): Nineveh + 69 to Nineveh + 118
        daysFromNineveh in 69..118 -> {
            val day = daysFromNineveh - 69 + 1
            val title = if (s.isAmharic) "በዓለ ሃምሳ" else "50 Days of Joy"
            val detail = if (s.isAmharic) "ቀን $day ከ 50" else "Day $day of 50"
            SeasonProgress(title, detail, (day / 50f).coerceIn(0f, 1f))
        }
        // Season of Tsige (መስከረም 26 - ኅዳር 5 = 40 days)
        (eth.month == 1 && eth.day >= 26) || (eth.month == 2) || (eth.month == 3 && eth.day <= 5) -> {
            val day = when (eth.month) {
                1 -> eth.day - 25
                2 -> 5 + eth.day
                else -> 35 + eth.day
            }
            val title = if (s.isAmharic) "ዘመነ ጽጌ" else "Season of Flowers"
            val detail = if (s.isAmharic) "ቀን $day ከ 40" else "Day $day of 40"
            SeasonProgress(title, detail, (day / 40f).coerceIn(0f, 1f))
        }
        // Fast of the Prophets / Advent (ኅዳር 15 - ታኅሣሥ 28 = 44 days)
        (eth.month == 3 && eth.day >= 15) || (eth.month == 4 && eth.day <= 28) -> {
            val day = if (eth.month == 3) eth.day - 14 else 16 + eth.day
            val title = if (s.isAmharic) "ጾመ ነቢያት" else "Fast of the Prophets"
            val detail = if (s.isAmharic) "ቀን $day ከ 44" else "Day $day of 44"
            SeasonProgress(title, detail, (day / 44f).coerceIn(0f, 1f))
        }
        // Fast of the Assumption / Filseta (ነሐሴ 1 - ነሐሴ 16 = 16 days)
        eth.month == 12 && eth.day in 1..16 -> {
            val title = if (s.isAmharic) "ጾመ ፍልሰታ" else "Fast of the Assumption"
            val detail = if (s.isAmharic) "ቀን ${eth.day} ከ 16" else "Day ${eth.day} of 16"
            SeasonProgress(title, detail, (eth.day / 16f).coerceIn(0f, 1f))
        }
        // Pagumen (5 or 6 days)
        eth.month == 13 -> {
            val title = if (s.isAmharic) "ጳጉሜን" else "Pagumen"
            val detail = if (s.isAmharic) "ቀን ${eth.day}" else "Day ${eth.day}"
            SeasonProgress(title, detail, (eth.day / 6f).coerceIn(0f, 1f))
        }
        // Ordinary Season / Month Progress
        else -> {
            val title = s.ethMonths.getOrElse(eth.month - 1) { "ዘመን" }
            val detail = if (s.isAmharic) "ቀን ${eth.day} ከ 30" else "Day ${eth.day} of 30"
            SeasonProgress(title, detail, (eth.day / 30f).coerceIn(0f, 1f))
        }
    }
}

@Composable
fun LiturgicalSeasonArc(
    today: LocalDate,
    onClick: () -> Unit = {},
    scale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val progress = remember(today, s) { computeSeasonProgress(today, s) }

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = Spacing.sm),
        ) {
            Text(
                text = progress.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = progress.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size((36 * scale.coerceIn(1f, 1.4f)).dp),
        ) {
            CircularProgressIndicator(
                progress = { progress.progress },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                strokeWidth = 3.dp,
            )
            Text(
                text = "${(progress.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
