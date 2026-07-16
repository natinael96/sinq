package com.agpeya.app.ui.psalter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.Section
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Abyssinica
import java.time.DayOfWeek
import java.time.LocalDate

private val FONT_STEPS_SP = listOf(17, 19, 22, 25, 29)

/** Traditional weekday division of the Psalter; Sunday is not yet defined. */
private fun dailyRange(day: DayOfWeek): IntRange? = when (day) {
    DayOfWeek.MONDAY -> 1..30
    DayOfWeek.TUESDAY -> 31..60
    DayOfWeek.WEDNESDAY -> 61..80
    DayOfWeek.THURSDAY -> 81..110
    DayOfWeek.FRIDAY -> 111..130
    DayOfWeek.SATURDAY -> 131..150
    DayOfWeek.SUNDAY -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PsalterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val psalms by produceState(emptyList<Section>()) { value = ContentRepository.psalter(context) }
    val fontStep by SettingsRepository.fontStep(context)
        .collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]

    var daily by remember { mutableStateOf(true) }
    val today = remember { LocalDate.now() }
    val range = remember(today) { dailyRange(today.dayOfWeek) }
    val shown = remember(psalms, daily, range) {
        if (!daily) psalms
        else range?.let { r -> psalms.filter { it.number in r } } ?: emptyList()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(s.psalterTitle, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = daily,
                    onClick = { daily = true },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text(s.dailyPsalms) }
                SegmentedButton(
                    selected = !daily,
                    onClick = { daily = false },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text(s.wholePsalter) }
            }

            if (daily && range == null) {
                // Sunday's division isn't defined yet.
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        s.comingSoon,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = Abyssinica),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                return@Column
            }

            if (daily && range != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    s.psalmRange(range.first, range.last),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(shown.size, key = { shown[it].id }) { i ->
                    PsalmView(section = shown[i], bodyFontSp = bodyFontSp)
                }
                item { Spacer(Modifier.height(56.dp)) }
            }
        }
    }
}

@Composable
private fun PsalmView(section: Section, bodyFontSp: Int) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(36.dp))
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = Abyssinica),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
        section.subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Abyssinica),
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(
            modifier = Modifier.width(32.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(14.dp))
        val markerColor = MaterialTheme.colorScheme.secondary
        val body = remember(section.id, bodyFontSp, markerColor) {
            buildAnnotatedString {
                section.verses.forEachIndexed { i, verse ->
                    if (i > 0) append("\n")
                    withStyle(
                        SpanStyle(
                            color = markerColor,
                            fontSize = (bodyFontSp * 0.58f).sp,
                            baselineShift = BaselineShift.Superscript,
                        )
                    ) { append(geezNumeral(section.firstVerse + i)) }
                    append(" ")
                    append(verse)
                }
            }
        }
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = Abyssinica,
                fontSize = bodyFontSp.sp,
                lineHeight = (bodyFontSp * 1.85f).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
