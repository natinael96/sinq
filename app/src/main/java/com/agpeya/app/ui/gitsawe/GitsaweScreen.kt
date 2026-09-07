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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
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
import com.agpeya.app.ui.common.formatEthiopianShort
import com.agpeya.app.ui.common.formatEthiopianWithGregorian
import com.agpeya.app.ui.common.liturgicalSeasonLabel
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.SelectPill
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.HeroCard
import com.agpeya.app.ui.common.rememberCurrentDate
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.sinqColors
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.strings.Strings
import java.time.LocalDate
import kotlinx.coroutines.launch
import androidx.compose.material.icons.outlined.Image

/** One reading office the user can switch to on a given day. */
private data class Source(
    val label: String,
    val subtitle: String?,
    /** Scan pages the entry was transcribed from — the printed book's own. */
    val pages: List<Int>,
    val services: GitsaweServices,
)

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
    onOpenMahlet: (subFeastKey: String) -> Unit,
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
            data.daily?.let { add(Source(s.srcDaily, it.title, it.sourcePages, it)) }
            data.seasonal.forEach { add(Source(s.srcSeasonal, it.title, it.sourcePages, it)) }
            data.monthly.forEach { add(Source(s.srcMonthly, it.title ?: it.raw, emptyList(), it)) }
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
                    SynaxariumCard(
                        date = date,
                        today = currentDay,
                        // The day's own name, which the page used to print
                        // again in full a few dp below the hero.
                        dayTitle = active?.subtitle,
                        onClick = { onOpenSynaxarium(epochDay) },
                    )
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
                    // Where the day comes from, in one quiet line: which of the
                    // book's cycles it was taken from, and the page it is
                    // printed on. The day's own title is in the hero above.
                    item(key = "sub") {
                        Text(
                            listOfNotNull(
                                active.label,
                                active.pages.takeIf { it.isNotEmpty() }?.let(s::gitsaweSourcePage),
                            ).joinToString("  ·  "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = Spacing.xxs, bottom = Spacing.xxs),
                        )
                    }
                    val setMisbakLanguage: (MisbakLanguage) -> Unit = { language ->
                        scope.launch { SettingsRepository.setMisbakLanguage(context, language) }
                    }
                    // The anaphora names the ቅዳሴ, so it sits on the ቅዳሴ
                    // header. It used to be a section of its own at the foot
                    // of the page, four readings away from what it names.
                    val chants = dayChants(active.services).joinToString("  ·  ")
                    active.services.negh?.let { svc -> serviceSection("ነግህ", null, svc, s, misbakLanguage, setMisbakLanguage, onOpenReading) }
                    active.services.kidassie?.let { svc ->
                        serviceSection("ቅዳሴ", chants.takeIf { it.isNotEmpty() }, svc, s, misbakLanguage, setMisbakLanguage, onOpenReading)
                    }
                    active.services.serk?.let { svc -> serviceSection("ሠርክ", null, svc, s, misbakLanguage, setMisbakLanguage, onOpenReading) }
                }
                // The night's ዋዜማ and the morning's ነግሥ, on the days the book
                // appoints one. Beside the Sunday መዝሙር card, in its shape.
                if (data?.mahlets?.isNotEmpty() == true) {
                    items(data.mahlets.size, key = { "mahlet_${data.mahlets[it].subFeast.key}" }) { i ->
                        val entry = data.mahlets[i]
                        com.agpeya.app.ui.common.SinqCard(
                            onClick = { onOpenMahlet(entry.subFeast.key) },
                            accented = true,
                            modifier = Modifier.padding(top = Spacing.md),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (entry.isEve) s.mahletVigil else s.mahletDawn,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    entry.mahlet.title,
                                    style = MaterialTheme.typography.titleMedium.inReadingFont(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = Spacing.xxs),
                                )
                                Text(
                                    s.mahletParts(entry.mahlet.detail.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (data?.sundayCycle?.isNotEmpty() == true) {
                    item(key = "sunday_cycle") {
                        SundayMezmurCard(
                            entries = data.sundayCycle,
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
        // The Church's own calendar. Every date on this page is Ethiopian and
        // every number Ge'ez; "change day" used to open a Gregorian grid of
        // Arabic digits, so finding መስከረም ፩ meant knowing it is 11 September —
        // the arithmetic this app exists to do for the reader.
        com.agpeya.app.ui.common.EthiopianDatePickerDialog(
            initial = java.time.LocalDate.ofEpochDay(epochDay),
            onDismiss = { showPicker = false },
            onSelect = {
                epochDay = it.toEpochDay()
                showPicker = false
            },
        )
    }
}

/**
 * The day's readings in the order the liturgy reads them, named by the text
 * rather than by whose turn it is to read it.
 *
 * The data is unambiguous about which is which — across the bundled year
 * `firstDeacon` is Pauline 354 times out of 354, `secondDeacon` a catholic
 * epistle 366 of 366, and `secondKahn` Acts 364 of 364 — so the ቅዳሴ reads
 * ጳውሎስ, ሐዋርያ, ግብረ ሐዋርያት, then the ምስባክ is sung and the ወንጌል proclaimed.
 * ነግህ and ሠርክ carry only the psalm and the Gospel, so the same list serves
 * all three offices: the roles they do not have contribute nothing.
 */
private val ROLE_LABELS = listOf(
    "ጳውሎስ" to { s: GitsaweService -> s.firstDeacon },
    "ሐዋርያ" to { s: GitsaweService -> s.secondDeacon },
    "ግብረ ሐዋርያት" to { s: GitsaweService -> s.secondKahn },
    "ምስባክ" to { s: GitsaweService -> s.msbak },
    "ወንጌል" to { s: GitsaweService -> s.wengel },
)

private fun androidx.compose.foundation.lazy.LazyListScope.serviceSection(
    label: String,
    /** The anaphora sung at this service, when it names one. */
    note: String?,
    service: GitsaweService,
    s: Strings,
    misbakLanguage: MisbakLanguage,
    onMisbakLanguage: (MisbakLanguage) -> Unit,
    onOpenReading: (ReadingTarget, String) -> Unit,
) {
    item(key = "svc_$label") {
        ServiceHeader(label, note)
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
 * Names one movement of the day — ነግህ, ቅዳሴ, ሠርክ — at the same rank as every
 * other section header in the app: gold, bold, with the rule carrying the line
 * to the margin. [note] rides the far end of the rule, which is where the
 * day's anaphora belongs: it names the ቅዳሴ it sits on.
 */
@Composable
private fun ServiceHeader(label: String, note: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = Spacing.lg, bottom = Spacing.xs),
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
        if (!note.isNullOrBlank()) {
            Spacer(Modifier.width(Spacing.md))
            Text(
                note,
                style = MaterialTheme.typography.labelMedium.inReadingFont(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
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
    val preview by produceState<String?>(null, target, misbakLanguage, reading) {
        val printed = reading.text
        value = when {
            // The ምስባክ is a chant, and the ግጻዌ prints it — three lines, often
            // beginning part-way through a verse. Slicing the Psalter for it
            // instead pulled in whatever that edition appends to a psalm's last
            // verse: the Gloria on 147 of the 150, and at the six section ends
            // the prayers for the departed. 99 of the year's 1,123 daily ምስባክ
            // citations reach such a verse.
            isMisbak && misbakLanguage == MisbakLanguage.GEEZ -> printed?.geez
            isMisbak -> printed?.amharic
                ?: psalmPreview(context, target as ReadingTarget.Psalm, geez = false)
            else -> when (val t = target) {
                is ReadingTarget.Psalm -> psalmPreview(context, t, geez = false)
                is ReadingTarget.NtPassage -> ScriptureRepository.passage(
                    context, t.bookKey, t.chapter, t.start, t.end,
                )?.take(2)?.joinToString(" ") { it.text }
                null -> printed?.geez ?: printed?.amharic
            }
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
        // Reference and incipit, and nothing else: 60 dp a row rather than
        // 92. Nine readings is a long page already — the reader came for the
        // passage, and the row's work is to say which one and open it.
        Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)) {
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
                    // The day's chant, in the day's card, ready to send before
                    // the liturgy. It is the one thing on this page that people
                    // send every morning, and it took four taps to get out.
                    val scope = androidx.compose.runtime.rememberCoroutineScope()
                    val busy = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            val text = preview?.takeIf { it.isNotBlank() } ?: return@IconButton
                            if (busy.value) return@IconButton
                            scope.launch {
                                busy.value = true
                                try {
                                    com.agpeya.app.ui.common.PassageShare.share(
                                        context,
                                        com.agpeya.app.ui.common.SharePayload(
                                            body = text,
                                            kicker = role,
                                            title = verse?.let { verseRef(it) },
                                            dateLabel = null,
                                            shape = com.agpeya.app.ui.common.ImageShape.SQUARE,
                                        ),
                                        s,
                                    )
                                } finally {
                                    busy.value = false
                                }
                            }
                        },
                        enabled = !preview.isNullOrBlank(),
                    ) {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = s.shareAsImage,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(IconSize.small),
                        )
                    }
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
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium.inReadingFont(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // The ምስባክ is the exception: it is the chant itself, three
                    // lines as the book prints them, not an incipit.
                    maxLines = if (isMisbak) 4 else 1,
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

/**
 * The cited verses of a psalm, under the app's one range rule: a citation with
 * no end runs to the psalm's last verse, exactly as [ScriptureRepository.passage]
 * reads it. The row used to stop at two verses while the passage page ran to the
 * end — one citation, two answers.
 */
private suspend fun psalmPreview(
    context: android.content.Context,
    target: ReadingTarget.Psalm,
    geez: Boolean,
): String? {
    val verses = ScriptureRepository.psalms(context, geez = geez)
        .find { it.number == target.number }?.verses ?: return null
    if (verses.isEmpty()) return null
    val lo = (target.startVerse ?: 1).coerceIn(1, verses.size)
    val hi = (target.endVerse ?: verses.size).coerceIn(lo, verses.size)
    return verses.subList(lo - 1, hi).joinToString(" ")
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

/**
 * The day's ስንክሳር — a filled hero button, so the page's second destination
 * reads as something to press rather than another line of text. It names the
 * day actually in view: "የዕለቱ ስንክሳር" only while the page is on today.
 */
@Composable
private fun SynaxariumCard(
    date: LocalDate,
    today: LocalDate,
    dayTitle: String?,
    onClick: () -> Unit,
) {
    val s = LocalStrings.current
    val sinq = sinqColors
    HeroCard(
        onClick = onClick,
        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs),
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md),
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = sinq.onHeroMuted,
            modifier = Modifier.size(IconSize.large),
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                if (date == today) s.synaxariumKicker
                else s.synaxariumFor(formatEthiopianShort(date, s)),
                style = MaterialTheme.typography.titleMedium,
                color = sinq.onHero,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // The day's own name, on the first thing the page shows. The hero
            // used to say "ስንክሳር" and then "የዕለቱ ስንክሳር" — the same words
            // twice — while the commemoration itself sat below it.
            dayTitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall.inReadingFont(),
                    color = sinq.onHeroMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = sinq.onHeroMuted,
        )
    }
}

/**
 * The Sunday's hymn, in front of the reader rather than behind a link: the
 * incipit of every row the book prints for the day, in the reading face. The
 * card opens the full Sunday Gitsawe with its readings.
 */
@Composable
private fun SundayMezmurCard(
    entries: List<com.agpeya.app.model.SundayCycleEntry>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    SinqCard(onClick = onClick, accented = true, modifier = modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.sundayMezmurTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                entries.forEach { entry ->
                    Text(
                        entry.mezmur ?: entry.title,
                        style = MaterialTheme.typography.titleMedium.inReadingFont(),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = Spacing.xxs),
                    )
                    if (entry.mezmur != null && entry.title != entry.mezmur) {
                        Text(
                            entry.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    s.sundayCycleTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
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
/**
 * The day's ቅዳሴ, named once each.
 *
 * The lectionary writes the anaphora as free text — 120 spellings across the
 * year for thirteen anaphoras — and the three services often repeat it. The
 * header printed every variant verbatim, so one liturgy appeared under four
 * names in a week. [com.agpeya.app.data.Anaphora] resolves them.
 */
private fun dayChants(services: GitsaweServices?): List<String> =
    com.agpeya.app.data.Anaphora.allOf(
        ((services?.negh?.kidassie ?: emptyList()) +
            (services?.kidassie?.kidassie ?: emptyList()) +
            (services?.serk?.kidassie ?: emptyList()))
            .map { it.trim() }
            .filter { it.isNotEmpty() },
    ).map { it.label }
