package com.agpeya.app.ui.gitsawe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.agpeya.app.ui.common.NavRow
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.rememberCurrentDate
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
    onOpenSundayCycle: (Long) -> Unit,
    initialEpochDay: Long? = null,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val misbakLanguage by SettingsRepository.misbakLanguage(context)
        .collectAsState(initial = MisbakLanguage.GEEZ)
    // The day being viewed, as an epoch-day so it survives rotation.
    var epochDay by rememberSaveable { mutableLongStateOf(initialEpochDay ?: LocalDate.now().toEpochDay()) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val currentDay by rememberCurrentDate()
    val date = LocalDate.ofEpochDay(epochDay)
    val readings by produceState<DayReadings?>(initialValue = null, epochDay) {
        value = GitsaweRepository.readingsFor(context, LocalDate.ofEpochDay(epochDay))
    }

    val data = readings
    // Keyed on the strings too, so the source labels re-localize on a language switch.
    val sources = remember(data, s) {
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    // Sliding the page horizontally turns to the neighbouring
                    // day, mirroring the arrows in the day navigator.
                    .pointerInput(Unit) {
                        var drag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { drag = 0f },
                            onDragEnd = {
                                val threshold = 48.dp.toPx()
                                if (drag <= -threshold) epochDay++
                                else if (drag >= threshold) epochDay--
                            },
                        ) { _, amount -> drag += amount }
                    },
                contentPadding = PaddingValues(horizontal = Spacing.screen),
            ) {
                item(key = "day") {
                    DayNavigator(
                        date = date,
                        today = currentDay,
                        onPrevious = { epochDay-- },
                        onNext = { epochDay++ },
                        onToday = { epochDay = currentDay.toEpochDay() },
                    )
                }
                item(key = "sinksar") {
                    SynaxariumCard(onClick = { onOpenSynaxarium(epochDay) })
                }
                if (active == null) {
                    // A load failure can still leave the day without content.
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
                                    modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.xxs),
                                )
                            }
                        }
                    }
                    val setMisbakLanguage: (MisbakLanguage) -> Unit = { language ->
                        scope.launch { SettingsRepository.setMisbakLanguage(context, language) }
                    }
                    active.services.negh?.let { svc -> serviceSection("ነግህ", svc, s, misbakLanguage, setMisbakLanguage, onOpenReading) }
                    active.services.kidassie?.let { svc -> serviceSection("ቅዳሴ", svc, s, misbakLanguage, setMisbakLanguage, onOpenReading) }
                    active.services.serk?.let { svc -> serviceSection("ሠርክ", svc, s, misbakLanguage, setMisbakLanguage, onOpenReading) }
                    val chants = dayChants(active.services)
                    if (chants.isNotEmpty()) {
                        item(key = "kidase") { KidaseSection(chants) }
                    }
                }
                if (data?.sundayCycle?.isNotEmpty() == true) {
                    item(key = "sunday_cycle") {
                        NavRow(
                            title = s.sundayCycleTitle,
                            subtitle = s.sundayCycleSubtitle,
                            onClick = { onOpenSundayCycle(epochDay) },
                            modifier = Modifier.padding(top = Spacing.lg),
                        )
                    }
                }
                item { Spacer(Modifier.height(Spacing.xl)) }
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
        Modifier.fillMaxWidth().padding(top = Spacing.xl, bottom = Spacing.xs),
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
                    .padding(vertical = Spacing.xs, horizontal = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "፨",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.width(Spacing.sm))
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
            .then(if (clickable) Modifier.clickable { onOpenReading(target!!, role) } else Modifier),
        shape = RoundedCornerShape(10.dp),
        color = if (isMisbak) MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
        else Color.Transparent,
        border = if (isMisbak) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.32f)) else null,
    ) {
        Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(role, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    if (verse != null) verseRef(verse) else reading.citation ?: "—",
                    style = MaterialTheme.typography.titleSmall.inReadingFont(),
                    color = if (clickable) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium.inReadingFont(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
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
private fun DayNavigator(date: LocalDate, today: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    val s = LocalStrings.current
    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.xxs), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, s.previousDay) }
            Text(
                text = formatEthiopianWithGregorian(date, s),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Always present: lit when the page is on today's ግጻዌ, and a
            // one-tap way home from any other day.
            SelectPill(label = s.todayLabel, selected = date == today, onClick = onToday)
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
    NavRow(title = s.synaxariumKicker, onClick = onClick)
}

/** Which office the day's readings are being shown from (daily / seasonal / monthly). */
@Composable
private fun SourceSwitcher(sources: List<Source>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier.padding(vertical = Spacing.xs),
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
    service("ሠርክ", services.serk)
    dayChants(services).takeIf { it.isNotEmpty() }?.let {
        if (isNotEmpty()) append("\n")
        append(it.joinToString("\n"))
    }
}

/** The day's appointed ቅዳሴ chants, across all available offices, deduplicated. */
private fun dayChants(services: GitsaweServices?): List<String> =
    ((services?.negh?.kidassie ?: emptyList()) +
        (services?.kidassie?.kidassie ?: emptyList()) +
        (services?.serk?.kidassie ?: emptyList()))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
