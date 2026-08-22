package com.agpeya.app.ui.gitsawe

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.DayReadings
import com.agpeya.app.data.GitsaweLinks
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.data.MisbakLanguage
import com.agpeya.app.data.ReadingTarget
import com.agpeya.app.data.ScriptureRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.model.GitsaweReading
import com.agpeya.app.model.GitsaweService
import com.agpeya.app.model.GitsaweServices
import com.agpeya.app.model.VerseRef
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.common.formatEthiopianWithGregorian
import com.agpeya.app.ui.common.liturgicalSeasonLabel
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.SelectPill
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.strings.Strings
import java.time.LocalDate
import kotlinx.coroutines.launch

/** One reading office the user can switch to on a given day. */
private data class Source(val label: String, val subtitle: String?, val services: GitsaweServices)

/**
 * የዕለቱ ግጻዌ — today's lectionary. Offers whatever offices fall on the date
 * (daily, the movable-season reading via [GitsaweRepository]'s Bahre Hasab, and
 * the monthly reading) as a switch. Each reading taps through to its own
 * focused passage page ([onOpenReading] carries the target and the role label
 * for that page's header); a reading with no bundled page is shown but not
 * tappable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitsaweScreen(
    onBack: () -> Unit,
    onOpenReading: (ReadingTarget, String) -> Unit,
    onOpenSynaxarium: (Long) -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val misbakLanguage by SettingsRepository.misbakLanguage(context)
        .collectAsState(initial = MisbakLanguage.GEEZ)
    // The day being viewed, as an epoch-day so it survives rotation.
    var epochDay by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(epochDay)
    val readings by produceState<DayReadings?>(initialValue = null, epochDay) {
        value = GitsaweRepository.readingsFor(context, LocalDate.ofEpochDay(epochDay))
    }

    val data = readings
    val sources = remember(data) {
        if (data == null) emptyList() else buildList {
            data.daily?.let { add(Source(s.srcDaily, it.title, it)) }
            data.seasonal.forEach { add(Source(s.srcSeasonal, it.title, it)) }
            data.monthly.forEach { add(Source(s.srcMonthly, it.title ?: it.raw, it)) }
        }
    }
    var selected by rememberSaveable(epochDay, sources.size) { mutableIntStateOf(0) }
    val active = sources.getOrNull(selected.coerceIn(0, (sources.size - 1).coerceAtLeast(0)))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.gitsaweKicker,
                accentLine = liturgicalSeasonLabel(date, s),
                onBack = onBack,
                actions = {
                    com.agpeya.app.ui.common.ShareMenuAction(enabled = active != null, payload = {
                        active?.let {
                            com.agpeya.app.ui.common.SharePayload(
                                body = gitsaweShareBody(it.services),
                                kicker = s.gitsaweKicker,
                                title = it.subtitle ?: s.gitsaweTitle,
                                dateLabel = formatEthiopian(date, s),
                            )
                        }
                    })
                    IconButton(onClick = { showPicker = true }) {
                        Icon(
                            Icons.Outlined.EditCalendar,
                            contentDescription = s.gitsaweChangeDay,
                            modifier = Modifier.size(IconSize.medium),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (data == null) {
            LoadingPanel(Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = Spacing.screen),
            ) {
                item(key = "day") {
                    DayNavigator(
                        date = date,
                        onPrevious = { epochDay-- },
                        onNext = { epochDay++ },
                        onToday = { epochDay = LocalDate.now().toEpochDay() },
                        onPick = { showPicker = true },
                    )
                }
                item(key = "sinksar") {
                    Spacer(Modifier.height(Spacing.sm))
                    SynaxariumCard(onClick = { onOpenSynaxarium(epochDay) })
                }
                if (active == null) {
                    // Daily coverage is partial by design — say what's missing
                    // and that the day itself is fine, rather than showing a
                    // bare line of grey text.
                    item(key = "empty") {
                        StatePanel(
                            icon = Icons.Outlined.EditCalendar,
                            title = s.noGitsaweToday,
                            actionLabel = s.gitsaweChangeDay,
                            onAction = { showPicker = true },
                        )
                    }
                } else {
                    if (sources.size > 1) {
                        item(key = "switcher") { SourceSwitcher(sources, selected) { selected = it } }
                    }
                    active.subtitle?.let { sub ->
                        item(key = "sub") {
                            // The day's own heading is content, not chrome: set in
                            // the reading face, and selectable like a reading.
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(
                                    sub,
                                    style = MaterialTheme.typography.titleMedium
                                        .inReadingFont(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
                                )
                            }
                        }
                    }
                    val setMisbakLanguage: (MisbakLanguage) -> Unit = { language ->
                        scope.launch { SettingsRepository.setMisbakLanguage(context, language) }
                    }
                    active.services.negh?.let { svc -> serviceSection("ነግህ", svc, s, misbakLanguage, setMisbakLanguage, onOpenReading) }
                    active.services.kidassie?.let { svc -> serviceSection("ቅዳሴ", svc, s, misbakLanguage, setMisbakLanguage, onOpenReading) }
                    val chants = dayChants(active.services)
                    if (chants.isNotEmpty()) {
                        item(key = "kidase") { KidaseSection(chants) }
                    }
                }
                item { Spacer(Modifier.height(Spacing.huge)) }
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = epochDay * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { epochDay = it / 86_400_000L }
                    showPicker = false
                }) { Text(s.ok) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(s.cancel) }
            },
        ) { DatePicker(state = pickerState) }
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
    misbakLanguage: MisbakLanguage,
    onMisbakLanguage: (MisbakLanguage) -> Unit,
    onOpenReading: (ReadingTarget, String) -> Unit,
) {
    item(key = "svc_$label") {
        ServiceHeader(label)
    }
    for ((role, pick) in ROLE_LABELS) {
        pick(service).forEachIndexed { i, reading ->
            item(key = "${label}_${role}_$i") {
                ReadingRow(role, reading, s, misbakLanguage, onMisbakLanguage, onOpenReading)
            }
        }
    }
    // The service's chant names are gathered into the Kidase section at the
    // foot of the page, where they read as the day's ቅዳሴ rather than a
    // stray line of labels under the readings.
}

/**
 * Names one movement of the day — ነግህ, ቅዳሴ, and the Kidase coda — at the same
 * rank as every other section header in the app: gold, bold, with the rule
 * carrying the line to the margin.
 */
@Composable
private fun ServiceHeader(label: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = Spacing.xxl, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.width(Spacing.md))
        androidx.compose.material3.HorizontalDivider(
            Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/**
 * የዕለቱ ቅዳሴ — which anaphora is sung today, closing the page as a natural
 * continuation of the readings above it. Chant names are content: set in the
 * reading face, selectable, marked with the ፨ section sign rather than an icon
 * the tradition never used.
 */
@Composable
private fun KidaseSection(chants: List<String>) {
    val s = LocalStrings.current
    Column(Modifier.fillMaxWidth()) {
        ServiceHeader(s.kidaseHeader)
        chants.forEach { name ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "፨",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(Spacing.md))
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleSmall.inReadingFont(),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingRow(
    role: String,
    reading: GitsaweReading,
    s: Strings,
    misbakLanguage: MisbakLanguage,
    onMisbakLanguage: (MisbakLanguage) -> Unit,
    onOpenReading: (ReadingTarget, String) -> Unit,
) {
    val context = LocalContext.current
    val verse = reading.verse
    val target = verse?.let { GitsaweLinks.target(it) }
    val clickable = target != null
    val isMisbak = role == "ምስባክ" && target is ReadingTarget.Psalm
    val preview by produceState<String?>(null, target, misbakLanguage) {
        value = when (val t = target) {
            is ReadingTarget.Psalm -> {
                val section = ScriptureRepository.psalms(
                    context,
                    geez = isMisbak && misbakLanguage == MisbakLanguage.GEEZ,
                ).find { it.number == t.number }
                section?.verses?.let { verses ->
                    val lo = (t.startVerse ?: 1).coerceIn(1, verses.size.coerceAtLeast(1))
                    val hi = (t.endVerse ?: (lo + 1)).coerceIn(lo, verses.size)
                    verses.subList(lo - 1, hi).joinToString(" ")
                }
            }
            is ReadingTarget.NtPassage -> ScriptureRepository.passage(
                context, t.bookKey, t.chapter, t.start, t.end,
            )?.take(2)?.joinToString(" ") { it.text }
            null -> reading.text?.geez ?: reading.text?.amharic
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.sm)
            .then(if (clickable) Modifier.clickable { onOpenReading(target!!, role) } else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = if (isMisbak) MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, if (isMisbak) MaterialTheme.colorScheme.secondary.copy(alpha = 0.32f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(role, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        if (verse != null) verseRef(verse) else "—",
                        style = MaterialTheme.typography.titleSmall.inReadingFont(),
                        color = if (clickable) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isMisbak) {
                    SelectPill(
                        label = if (misbakLanguage == MisbakLanguage.GEEZ) s.wudaseLangGeez else s.wudaseLangAmharic,
                        selected = true,
                        onClick = {
                            onMisbakLanguage(
                                if (misbakLanguage == MisbakLanguage.GEEZ) MisbakLanguage.AMHARIC else MisbakLanguage.GEEZ,
                            )
                        },
                    )
                } else if (clickable) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            preview?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium.inReadingFont(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!clickable) {
                Spacer(Modifier.height(Spacing.xs))
                Text(s.gitsaweOpenNotAvailable, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DayNavigator(date: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit, onPick: () -> Unit) {
    val s = LocalStrings.current
    val today = remember { LocalDate.now() }
    Column(Modifier.fillMaxWidth().padding(top = Spacing.md, bottom = Spacing.sm), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(s.gitsaweTitle, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Text(
            formatEthiopianWithGregorian(date, s),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xs))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, s.previousDay) }
            TextButton(onClick = if (date == today) onPick else onToday) {
                Text(if (date == today) s.gitsaweChangeDay else s.todayLabel)
            }
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, s.nextDay) }
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
private fun SynaxariumCard(onClick: () -> Unit) {
    val s = LocalStrings.current
    SinqCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    s.synaxariumKicker,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    s.synaxariumTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
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
}

/** Which office the day's readings are being shown from (daily / seasonal / monthly). */
@Composable
private fun SourceSwitcher(sources: List<Source>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier.padding(vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(sources.size) { i ->
            SelectPill(
                label = sources[i].label,
                selected = i == selected,
                onClick = { onSelect(i) },
            )
        }
    }
}

/** The day's readings as plain text — the payload body for the share actions.
 *  Context (title, date) travels in the payload's own fields, not repeated here. */
private fun gitsaweShareBody(services: GitsaweServices): String = buildString {
    fun service(label: String, svc: GitsaweService?) {
        svc ?: return
        val lines = ROLE_LABELS.flatMap { (role, pick) ->
            pick(svc).mapNotNull { r -> r.verse?.let { "$role  ${verseRef(it)}" } }
        }
        if (lines.isEmpty()) return
        if (isNotEmpty()) append("\n")
        append(label); append("\n")
        lines.forEach { append(it); append("\n") }
    }
    service("ነግህ", services.negh)
    service("ቅዳሴ", services.kidassie)
    dayChants(services).takeIf { it.isNotEmpty() }?.let {
        if (isNotEmpty()) append("\n")
        append(it.joinToString("\n"))
    }
}

/** The day's appointed ቅዳሴ chants, across both services, deduplicated. */
private fun dayChants(services: GitsaweServices?): List<String> =
    ((services?.negh?.kidassie ?: emptyList()) + (services?.kidassie?.kidassie ?: emptyList()))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
