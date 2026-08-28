package com.agpeya.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.BahreHasab
import com.agpeya.app.ui.common.*
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.*
import java.time.LocalDate

private const val FUTURE_YEAR_COUNT = 25

internal fun bahreHasabYearsFrom(currentYear: Int): IntRange =
    currentYear..currentYear + FUTURE_YEAR_COUNT

internal data class BahreHasabYear(
    val year: Int,
    val ameteAlem: Int,
    val evangelist: String,
    val wenber: Int,
    val abekte: Int,
    val metqi: Int,
    val observances: List<Pair<String, LocalDate>>,
)

internal fun calculateBahreHasabYear(year: Int) = BahreHasabYear(
    year,
    BahreHasab.ameteAlem(year),
    when (BahreHasab.evangelist(year)) {
        1 -> "ማቴዎስ"; 2 -> "ማርቆስ"; 3 -> "ሉቃስ"; else -> "ዮሐንስ"
    },
    BahreHasab.wenber(year),
    BahreHasab.abekte(year),
    BahreHasab.metqi(year),
    listOf(
        "ጾመ ነነዌ" to BahreHasab.nineveh(year),
        "ዐቢይ ጾም" to BahreHasab.greatLentStart(year),
        "ደብረ ዘይት" to BahreHasab.debreZeit(year),
        "ሆሣዕና" to BahreHasab.hosanna(year),
        "ስቅለት" to BahreHasab.siklet(year),
        "ትንሣኤ" to BahreHasab.fasika(year),
        "ርክበ ካህናት" to BahreHasab.rikbeKahnat(year),
        "ዕርገት" to BahreHasab.ascension(year),
        "ጰራቅሊጦስ" to BahreHasab.pentecost(year),
        "ጾመ ሐዋርያት" to BahreHasab.apostlesFast(year),
    ),
)

/** Live computus explorer: current Ethiopian year plus 25 future years. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BahreHasabReferenceScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val today by rememberCurrentDate()
    val currentYear = remember(today) { EthiopianDate.from(today).year }
    val years = remember(currentYear) {
        bahreHasabYearsFrom(currentYear).map(::calculateBahreHasabYear)
    }
    var selectedYear by rememberSaveable(currentYear) { mutableIntStateOf(currentYear) }
    val selected = years.first { it.year == selectedYear }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.bahreHasabTitle,
                subtitle = s.bahreHasabRange,
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.fillMaxSize().widthIn(max = ReadingMaxWidth)
                    .verticalScroll(rememberScrollState()).padding(vertical = Spacing.md),
            ) {
                YearRail(years, selectedYear, currentYear) { selectedYear = it }
                Spacer(Modifier.height(Spacing.lg))
                YearHero(selected, selected.year == currentYear)
                Spacer(Modifier.height(Spacing.xl))
                SectionLabel(s.bahreHasabCycleValues)
                Spacer(Modifier.height(Spacing.sm))
                CycleValues(selected)
                Spacer(Modifier.height(Spacing.xl))
                SectionLabel(s.bahreHasabMovableDates)
                Spacer(Modifier.height(Spacing.sm))
                ObservanceGrid(selected.observances)
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

@Composable
private fun YearRail(
    years: List<BahreHasabYear>, selectedYear: Int, currentYear: Int, onSelect: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        years.forEach { year ->
            val chosen = year.year == selectedYear
            val current = year.year == currentYear
            Surface(
                color = when {
                    chosen -> MaterialTheme.colorScheme.secondary
                    current -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                },
                contentColor = if (chosen) MaterialTheme.colorScheme.onSecondary
                    else MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.semantics { selected = chosen }
                    .clickable(role = Role.Tab) { onSelect(year.year) },
            ) {
                Text(
                    geezNumeral(year.year),
                    Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.labelLarge.inReadingFont(),
                    fontWeight = if (chosen || current) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun YearHero(year: BahreHasabYear, current: Boolean) {
    val s = LocalStrings.current
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Column(Modifier.padding(Spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
            if (current) {
                Text(s.bahreHasabCurrentYear, color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(Spacing.xs))
            }
            Text(
                "\${geezNumeral(year.year)} \${s.eraSuffix}",
                style = MaterialTheme.typography.headlineMedium.inReadingFont(),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "\${year.evangelist} · \${s.bahreHasabFasika} \${formatEthiopianWithGregorian(year.observances[5].second, s)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CycleValues(year: BahreHasabYear) {
    val values = listOf(
        "ዓመተ ዓለም" to geezNumeral(year.ameteAlem),
        "ወንጌላዊ" to year.evangelist,
        "ወንበር" to geezNumeral(year.wenber),
        "አበቅቴ" to geezNumeral(year.abekte),
        "መጥቅዕ" to geezNumeral(year.metqi),
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        values.forEach { (label, value) ->
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary)
                    Text(value, style = MaterialTheme.typography.titleMedium.inReadingFont())
                }
            }
        }
    }
}

@Composable
private fun ObservanceGrid(observances: List<Pair<String, LocalDate>>) {
    val s = LocalStrings.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        observances.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                pair.forEach { (name, date) ->
                    Surface(
                        Modifier.weight(1f), shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(Modifier.padding(Spacing.md)) {
                            Text(name, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(Spacing.xs))
                            Text(formatEthiopianWithGregorian(date, s),
                                style = MaterialTheme.typography.bodyMedium.inReadingFont())
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, Modifier.padding(horizontal = Spacing.screen),
        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
}
