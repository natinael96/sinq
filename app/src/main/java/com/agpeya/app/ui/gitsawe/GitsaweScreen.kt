package com.agpeya.app.ui.gitsawe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.DayReadings
import com.agpeya.app.data.GitsaweLinks
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.data.ReadingTarget
import com.agpeya.app.model.GitsaweReading
import com.agpeya.app.model.GitsaweService
import com.agpeya.app.model.GitsaweServices
import com.agpeya.app.model.VerseRef
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.strings.Strings
import java.time.LocalDate

/** One reading office the user can switch to on a given day. */
private data class Source(val label: String, val subtitle: String?, val services: GitsaweServices)

/**
 * የዕለቱ ግጻዌ — today's lectionary. Offers whatever offices fall on the date
 * (daily, the movable-season reading via [GitsaweRepository]'s Bahre Hasab, and
 * the monthly reading) as a switch. Each reading taps through to the Psalter or
 * the scripture reader; a reading with no bundled page is shown but not tappable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitsaweScreen(onBack: () -> Unit, onOpenReading: (ReadingTarget) -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val today = remember { LocalDate.now() }
    val readings by produceState<DayReadings?>(initialValue = null) {
        value = GitsaweRepository.readingsFor(context, today)
    }

    val data = readings
    val sources = remember(data) {
        if (data == null) emptyList() else buildList {
            data.daily?.let { add(Source(s.srcDaily, it.title, it)) }
            data.seasonal.forEach { add(Source(s.srcSeasonal, it.title, it)) }
            data.monthly.forEach { add(Source(s.srcMonthly, it.title ?: it.raw, it)) }
        }
    }
    var selected by rememberSaveable(sources.size) { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(s.gitsaweTitle, style = MaterialTheme.typography.titleLarge)
                        Text(
                            formatEthiopian(today, s),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        when {
            data == null -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary) }

            sources.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    s.noGitsaweToday,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            else -> {
                val active = sources[selected.coerceIn(0, sources.lastIndex)]
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 22.dp),
                ) {
                    if (sources.size > 1) {
                        item(key = "switcher") {
                            SourceSwitcher(sources, selected) { selected = it }
                        }
                    }
                    active.subtitle?.let { sub ->
                        item(key = "sub") {
                            Text(
                                sub,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                            )
                        }
                    }
                    active.services.negh?.let { svc ->
                        serviceSection("ነግህ", svc, s, onOpenReading)
                    }
                    active.services.kidassie?.let { svc ->
                        serviceSection("ቅዳሴ", svc, s, onOpenReading)
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

private val ROLE_LABELS = listOf(
    "ምስባክ" to { s: GitsaweService -> s.msbak },
    "ወንጌል" to { s: GitsaweService -> s.wengel },
    "፩ ዲያቆን" to { s: GitsaweService -> s.firstDeacon },
    "፪ ዲያቆን" to { s: GitsaweService -> s.secondDeacon },
    "ካህን" to { s: GitsaweService -> s.secondKahn },
)

private fun androidx.compose.foundation.lazy.LazyListScope.serviceSection(
    label: String,
    service: GitsaweService,
    s: Strings,
    onOpenReading: (ReadingTarget) -> Unit,
) {
    item(key = "svc_$label") {
        Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.width(10.dp))
            androidx.compose.material3.HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
    for ((role, pick) in ROLE_LABELS) {
        pick(service).forEachIndexed { i, reading ->
            item(key = "${label}_${role}_$i") {
                ReadingRow(role, reading, s, onOpenReading)
            }
        }
    }
    service.kidassie.takeIf { it.isNotEmpty() }?.let { chants ->
        item(key = "${label}_chants") {
            Text(
                chants.joinToString("  ·  "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun ReadingRow(role: String, reading: GitsaweReading, s: Strings, onOpenReading: (ReadingTarget) -> Unit) {
    val verse = reading.verse
    val target = verse?.let { GitsaweLinks.target(it) }
    val clickable = target != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (clickable) Modifier.clickable { onOpenReading(target!!) } else Modifier)
            .padding(vertical = 11.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            role,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(62.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                .padding(vertical = 4.dp, horizontal = 6.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                verse?.bookTitle ?: "—",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (clickable) verseRef(verse!!) else s.gitsaweOpenNotAvailable,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (clickable) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "መዝሙረ ዳዊት ፷፬፥፲፩–፲፪" — book with Ge'ez chapter:verse(range). */
private fun verseRef(v: VerseRef): String = buildString {
    v.bookTitle?.let { append(it); append(" ") }
    v.chapter?.let { append(geezNumeral(it)) }
    v.start?.let { append("፥"); append(geezNumeral(it)) }
    v.end?.let { append("–"); append(geezNumeral(it)) }
}

@Composable
private fun SourceSwitcher(sources: List<Source>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sources.size) { i ->
            val isSel = i == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .then(
                        if (isSel) Modifier.background(MaterialTheme.colorScheme.primary)
                        else Modifier.border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    )
                    .clickable { onSelect(i) }
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text(
                    sources[i].label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
