package com.agpeya.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import com.agpeya.app.ui.theme.Spacing

private const val FUTURE_YEAR_COUNT = 25

internal fun bahreHasabYearsFrom(currentYear: Int): IntRange =
    currentYear..currentYear + FUTURE_YEAR_COUNT

internal data class BahreHasabYear(
    val year: Int,
    val ameteAlem: Int,
    val evangelist: String,
    val medeb: Int,
    val wenber: Int,
    val abekte: Int,
    val metqi: Int,
    val metqiDate: EthiopianDate,
    val mebajaHamer: Int,
    val observances: List<Pair<String, LocalDate>>,
)

internal fun calculateBahreHasabYear(year: Int) = BahreHasabYear(
    year = year,
    ameteAlem = BahreHasab.ameteAlem(year),
    evangelist = when (BahreHasab.evangelist(year)) {
        1 -> "ማቴዎስ"; 2 -> "ማርቆስ"; 3 -> "ሉቃስ"; else -> "ዮሐንስ"
    },
    medeb = BahreHasab.medeb(year),
    wenber = BahreHasab.wenber(year),
    abekte = BahreHasab.abekte(year),
    metqi = BahreHasab.metqi(year),
    metqiDate = BahreHasab.metqiDate(year),
    mebajaHamer = BahreHasab.mebajaHamer(year),
    observances = listOf(
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

/** Live computus explorer and calculator for any Ethiopian year. */
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
    val selected = remember(selectedYear) { calculateBahreHasabYear(selectedYear) }
    var showCustomYearDialog by rememberSaveable { mutableStateOf(false) }
    var customYearText by rememberSaveable { mutableStateOf("") }

    if (showCustomYearDialog) {
        AlertDialog(
            onDismissRequest = { showCustomYearDialog = false },
            title = { Text(if (s.isAmharic) "የባሕረ ሐሳብ ዓመት አስላ" else "Calculate Computus Year") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        if (s.isAmharic) "ማንኛውንም የኢትዮጵያ ዓመት ያስገቡ፦" else "Enter any Ethiopian calendar year:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = customYearText,
                        onValueChange = { customYearText = it.filter(Char::isDigit).take(4) },
                        placeholder = { Text("2018") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val y = customYearText.toIntOrNull()
                        if (y != null && y in 1..9999) {
                            selectedYear = y
                            showCustomYearDialog = false
                        }
                    },
                    enabled = customYearText.toIntOrNull() != null,
                ) {
                    Text(if (s.isAmharic) "አስላ" else "Calculate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomYearDialog = false }) {
                    Text(s.cancel)
                }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.bahreHasabTitle,
                subtitle = s.bahreHasabRange,
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        customYearText = selectedYear.toString()
                        showCustomYearDialog = true
                    }) {
                        Icon(
                            Icons.Outlined.Calculate,
                            contentDescription = if (s.isAmharic) "ዓመት ምረጥ" else "Pick year",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.fillMaxSize().widthIn(max = ReadingMaxWidth)
                    .verticalScroll(rememberScrollState()).padding(vertical = Spacing.md),
            ) {
                YearRail(
                    years = years,
                    selectedYear = selectedYear,
                    currentYear = currentYear,
                    onOpenCalculator = {
                        customYearText = selectedYear.toString()
                        showCustomYearDialog = true
                    },
                    onSelect = { selectedYear = it },
                )
                Spacer(Modifier.height(Spacing.lg))
                YearHero(selected, selected.year == currentYear)
                Spacer(Modifier.height(Spacing.xl))
                SectionLabel(s.bahreHasabCycleValues)
                Spacer(Modifier.height(Spacing.sm))
                CycleValues(selected)
                Text(
                    if (s.isAmharic) "ዓመተ ዓለም፦ የኢትዮጵያ ዓመት + ፶፭፻።\nወንጌላዊ፦ ዓመተ ዓለም ÷ ፬ ቀሪው (፩=ማቴዎስ፣ ፪=ማርቆስ፣ ፫=ሉቃስ፣ ፬/0=ዮሐንስ)።\nመደብ፦ ዓመተ ዓለም ÷ ፲፱ ቀሪው፤ ወንበር፦ መደብ - ፩።\nአበቅቴ፦ (ወንበር × ፲፩) ÷ ፴ ቀሪው፤ መጥቅዕ፦ (ወንበር × ፲፱) ÷ ፴ ቀሪው።\nመባጃ ሐመር፦ ዕለተ መጥቅዕ + የዕለቱ ተውሳክ።"
                    else "Amete Alem: Ethiopian year + 5,500.\nEvangelist: Amete Alem % 4 (1=Matthew, 2=Mark, 3=Luke, 0=John).\nMedeb: Amete Alem % 19. Wenber: Medeb − 1.\nAbekte: (Wenber × 11) % 30. Metqi: (Wenber × 19) % 30.\nMebaja Hamer: Metqi day + weekday Tewsak.",
                    Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.md),
                    style = MaterialTheme.typography.bodySmall,
                )
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
    years: List<BahreHasabYear>,
    selectedYear: Int,
    currentYear: Int,
    onOpenCalculator: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val s = LocalStrings.current
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.clickable(role = Role.Button, onClick = onOpenCalculator),
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Outlined.Calculate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    if (s.isAmharic) "ዓመት ምረጥ" else "Pick year",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        val displayedYears = remember(years, selectedYear) {
            if (years.any { it.year == selectedYear }) years
            else (listOf(calculateBahreHasabYear(selectedYear)) + years).sortedBy { it.year }
        }
        displayedYears.forEach { year ->
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
    val sinq = sinqColors
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
        shape = RoundedCornerShape(24.dp),
        // The app's own hero green, not colorScheme.primary — in dark theme that
        // resolves to a pale mint that sits outside the green-and-gold palette
        // every other surface here is built from.
        color = sinq.hero,
        contentColor = sinq.onHero,
    ) {
        Column(Modifier.padding(Spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
            if (current) {
                Text(s.bahreHasabCurrentYear, color = sinq.onHeroMuted,
                    style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(Spacing.xs))
            }
            Text(
                "${geezNumeral(year.year)} ${s.eraSuffix}",
                style = MaterialTheme.typography.headlineMedium.inReadingFont(),
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                "${year.evangelist} · ${s.bahreHasabFasika} ${formatEthiopianWithGregorian(year.observances[5].second, s)}",
                style = MaterialTheme.typography.bodyMedium,
                color = sinq.onHeroMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CycleValues(year: BahreHasabYear) {
    val s = LocalStrings.current
    val metqiDateStr = "${s.ethMonths[year.metqiDate.month - 1]} ${geezNumeral(year.metqiDate.day)}"
    val values = listOf(
        (if (s.isAmharic) "ዓመተ ዓለም" else "Amete Alem · ዓመተ ዓለም") to "${geezNumeral(year.ameteAlem)} (${year.ameteAlem})",
        (if (s.isAmharic) "ወንጌላዊ" else "Evangelist · ወንጌላዊ") to year.evangelist,
        (if (s.isAmharic) "መደብ" else "Medeb · መደብ") to geezNumeral(year.medeb),
        (if (s.isAmharic) "ወንበር" else "Wenber · ወንበር") to geezNumeral(year.wenber),
        (if (s.isAmharic) "አበቅቴ" else "Abekte · አበቅቴ") to geezNumeral(year.abekte),
        (if (s.isAmharic) "መጥቅዕ" else "Metqi · መጥቅዕ") to geezNumeral(year.metqi),
        (if (s.isAmharic) "ዕለተ መጥቅዕ" else "Metqi Date · ዕለተ መጥቅዕ") to metqiDateStr,
        (if (s.isAmharic) "መባጃ ሐመር" else "Mebaja Hamer · መባጃ ሐመር") to geezNumeral(year.mebajaHamer),
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
    val density = androidx.compose.ui.platform.LocalDensity.current
    val window = androidx.compose.ui.platform.LocalWindowInfo.current
    val width = with(density) { window.containerSize.width.toDp() }
    val columns = if (width < 360.dp || density.fontScale > 1.2f) 1 else 2
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        observances.chunked(columns).forEach { pair ->
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
                if (columns == 2 && pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, Modifier.padding(horizontal = Spacing.screen),
        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
}
