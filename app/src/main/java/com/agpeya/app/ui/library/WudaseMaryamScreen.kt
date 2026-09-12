package com.agpeya.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.VolunteerActivism
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.WudaseRepository
import com.agpeya.app.model.WudaseContent
import com.agpeya.app.model.WudaseSection
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.common.rememberCurrentDate
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.ReadingMaxWidth
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.readingBodyStyle
import kotlinx.coroutines.launch
import java.time.LocalDate

private val FONT_STEPS_SP = com.agpeya.app.data.SettingsRepository.FONT_STEPS_SP

/** ውዳሴ ማርያም — the Praise of Mary, one portion per weekday, in Amharic (default)
 *  or Ge'ez via a toggle. [initialSectionId] preselects a section (the ዘወትር ጸሎት
 *  card opens the same reader on the daily prayer); otherwise today's portion. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WudaseMaryamScreen(
    onBack: () -> Unit,
    initialSectionId: String? = null,
    onOpenBook: (String) -> Unit = {},
    onOpenPrayerList: () -> Unit = {},
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()

    var loadAttempt by rememberSaveable { mutableIntStateOf(0) }
    val contentResult by produceState<Result<WudaseContent>?>(initialValue = null, loadAttempt) {
        value = runCatching { WudaseRepository.load(context) }
    }
    val fontStep by SettingsRepository.fontStep(context).collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]

    var geez by rememberSaveable { mutableStateOf(false) }

    val data = contentResult?.getOrNull()
    val sections = data?.sections ?: emptyList()

    // Default to today's weekday portion (1=Mon … 7=Sun), else the first section.
    // -1 means "no explicit choice yet" so a rotation never resets a picked day.
    val today by rememberCurrentDate()
    val todayIndex = remember(sections, today) {
        val wd = today.dayOfWeek.value
        sections.indexOfFirst { it.weekday == wd }.takeIf { it >= 0 } ?: 0
    }
    val initialIndex = remember(sections, initialSectionId) {
        initialSectionId?.let { id -> sections.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
    }
    // Opening ውዳሴ ማርያም lands on ጸሎት ዘዘወትር, not on the weekday portion. It is
    // the prayer said every day, so it is the one most openings are for; the
    // day's own portion is a tap away on the ቀጥል door at the foot of it.
    val dailyIndex = remember(sections) {
        sections.indexOfFirst { it.id == "daily" }.takeIf { it >= 0 } ?: 0
    }
    // መልክአ ማርያም and መልክአ ኢየሱስ belong beside ውዳሴ ማርያም, but they are books and
    // they are already on the shelf. Linked rather than copied: one text, in one
    // place, reached from both. Resolved by title through the folded key the
    // generator wrote, because hard-coding the ids would break the day the
    // content pipeline rehashes them.
    val companions by produceState(emptyList<Pair<String, String>>()) {
        value = listOf("መልክአ ማርያም", "መልክአ ኢየሱስ").mapNotNull { title ->
            com.agpeya.app.data.BookRepository.byPartName(context, title)
                ?.let { title to it.id }
        }
    }
    // The portions are pages: ውዳሴ ማርያም is read a portion at a time, one after
    // another, so swiping turns one the way it turns a tab. The strip above the
    // text still picks any of them directly — this is the gesture the reading
    // already implies.
    val pager = androidx.compose.foundation.pager.rememberPagerState(
        pageCount = { sections.size },
    )
    val selected = pager.currentPage.coerceIn(0, (sections.size - 1).coerceAtLeast(0))
    // The opening portion can only be chosen once the sections are loaded, and
    // only once: after that the reader's own page stands.
    var landed by rememberSaveable { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(sections.size) {
        if (!landed && sections.isNotEmpty()) {
            pager.scrollToPage(initialIndex ?: dailyIndex)
            landed = true
        }
    }
    val goToSection: (Int) -> Unit = { i -> scope.launch { pager.animateScrollToPage(i) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.wudaseMariam,
                onBack = onBack,
                actions = {
                    // The same pill the Psalter carries, in the same corner:
                    // two editions, one control, named for the edition it
                    // switches to. It was a full-width segmented bar at the top
                    // of the text, which cost a row of reading on every open.
                    com.agpeya.app.ui.common.EditionToggle(geez = geez) { geez = !geez }
                    // The whole portion being read, in the language being read.
                    val shown = sections.getOrNull(selected.coerceIn(0, (sections.size - 1).coerceAtLeast(0)))
                    com.agpeya.app.ui.common.ReaderToolsMenu(
                        fontStep = fontStep,
                        maxFontStep = FONT_STEPS_SP.lastIndex,
                        onFontChange = { step -> scope.launch { SettingsRepository.setFontStep(context, step) } },
                        shareEnabled = shown != null,
                        sharePayload = {
                            shown?.let {
                                com.agpeya.app.ui.common.SharePayload(
                                    body = (if (geez) it.ge else it.am).joinToString("\n\n"),
                                    kicker = s.wudaseMariam,
                                    title = if (geez) it.titleGe else it.titleAm,
                                )
                            }
                        },
                    )
                },
            )
        },
    ) { innerPadding ->
        if (contentResult == null) {
            LoadingPanel(Modifier.padding(innerPadding))
            return@Scaffold
        }
        if (data == null) {
            StatePanel(
                title = s.contentUnavailable,
                body = s.contentMissingBody,
                actionLabel = s.retryAction,
                onAction = { loadAttempt++ },
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        if (sections.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                StatePanel(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = s.contentUnavailable,
                    body = s.contentMissingBody,
                )
            }
            return@Scaffold
        }

        val section = sections.getOrNull(selected)
        // Live stanza selection: tap anchors, next tap moves the end; -1 = none.
        var selA by rememberSaveable(selected, geez) { mutableIntStateOf(-1) }
        var selB by rememberSaveable(selected, geez) { mutableIntStateOf(-1) }
        val selRange = com.agpeya.app.ui.reading.flatSelectionRange(selA, selB)
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            // The next portion is composed before it is reached, so a swipe
            // uncovers text rather than a blank that fills in late.
            beyondViewportPageCount = 1,
            key = { sections[it].id },
        ) { page ->
        val pageSection = sections[page]
        // Each portion keeps its own place in the text, so turning back to one
        // returns to where it was left rather than to its first line.
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().widthIn(max = ReadingMaxWidth),
            contentPadding = PaddingValues(horizontal = Spacing.screen),
        ) {
            item(key = "days") {
                Spacer(Modifier.height(Spacing.sm))
                SectionStrip(
                    sections = sections,
                    selected = selected,
                    companions = companions,
                    onSelect = goToSection,
                    onOpenBook = onOpenBook,
                )
                Spacer(Modifier.height(Spacing.sm))
            }
            item(key = "title") {
                Text(
                    text = if (geez) pageSection.titleGe else pageSection.titleAm,
                    style = MaterialTheme.typography.titleMedium.inReadingFont(),
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp),
                )
            }
            val stanzas = if (geez) pageSection.ge else pageSection.am
            items(stanzas.size, key = { "st_$it" }) { i ->
                // The highlight belongs to the portion being read, not to the
                // one sliding past it under the finger.
                val lit = page == selected && i in selRange
                // Long-press still gives native character selection; a tap
                // anchors/extends the stanza run for the share bar below.
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        text = stanzas[i],
                        style = readingBodyStyle(bodyFontSp),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (lit) MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .semantics { this.selected = lit }
                            .clickable {
                                val (a, bSel) = com.agpeya.app.ui.reading.advanceFlatSelection(selA, i)
                                selA = a
                                selB = bSel
                            }
                            .padding(bottom = 16.dp),
                    )
                }
            }
            // Where to go on from here, as one ቀጥል strip. Landing on ጸሎት
            // ዘዘወትር means the day's own portion needs a door, and it names the
            // day it opens rather than saying "continue" and leaving you to
            // find out. ይወድስዋ መላእክት gets a second one: the praise ends and the
            // names you carry are prayed next, which is what የጸሎት ዝርዝር holds.
            val todaySection = sections.getOrNull(todayIndex)
            val showToday = todaySection != null && page != todayIndex
            val showPrayerList = pageSection.id == "yiwedsewa_melaekt"
            if (showToday || showPrayerList) {
                item(key = "doors") {
                    Spacer(Modifier.height(Spacing.lg))
                    com.agpeya.app.ui.common.SinqDivider()
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        s.continueReading,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        if (showToday && todaySection != null) {
                            com.agpeya.app.ui.common.DoorChip(
                                icon = Icons.Outlined.Today,
                                label = "${s.todayLabel}  ·  ${todaySection.label}",
                                onClick = { goToSection(todayIndex) },
                            )
                        }
                        if (showPrayerList) {
                            com.agpeya.app.ui.common.DoorChip(
                                icon = Icons.Outlined.VolunteerActivism,
                                label = s.prayerListTitle,
                                onClick = onOpenPrayerList,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(Spacing.huge)) }
        }
        }
        val stanzasNow = if (section == null) emptyList() else (if (geez) section.ge else section.am)
        val selBody = if (selRange.isEmpty()) null
        else stanzasNow.filterIndexed { i, _ -> i in selRange }.joinToString("\n\n").ifBlank { null }
        val sectionTitle = section?.let { if (geez) it.titleGe else it.titleAm }
        com.agpeya.app.ui.reading.SelectionBar(
            visible = selA >= 0,
            onDismiss = { selA = -1; selB = -1 },
            // A paragraph reader: no verse numbers to print, and no colour row.
            passage = selBody?.let {
                com.agpeya.app.ui.common.Passage(
                    verses = listOf(null to it),
                    citation = listOfNotNull(s.wudaseMariam, sectionTitle).joinToString("  ·  "),
                )
            },
            imageKicker = s.wudaseMariam,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        }
    }
}



/**
 * The portions, as a strip: ጸሎት ዘዘወትር, the seven days, አንቀጸ ብርሃን, ይወድስዋ መላእክት,
 * and then the two መልክእ that belong beside them.
 *
 * መልክአ ማርያም and መልክአ ኢየሱስ are books and live on the shelf, so these two chips
 * leave the screen rather than selecting a portion. They are ringed rather than
 * filled and carry a book, which is how the app already marks a door that goes
 * somewhere else. They used to be two rows at the very foot of the text, which
 * meant scrolling a whole portion to find out they were there.
 */
@Composable
private fun SectionStrip(
    sections: List<WudaseSection>,
    selected: Int,
    companions: List<Pair<String, String>>,
    onSelect: (Int) -> Unit,
    onOpenBook: (String) -> Unit,
) {
    val stripState = androidx.compose.foundation.lazy.rememberLazyListState()
    // Keep the highlighted chip on screen — Sunday and the appended prayers sit
    // past the fold on narrow devices.
    androidx.compose.runtime.LaunchedEffect(selected) {
        if (selected in sections.indices) stripState.animateScrollToItem(selected)
    }
    LazyRow(
        state = stripState,
        modifier = Modifier.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(sections) { i, sec ->
            val isSel = i == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .then(
                        if (isSel) Modifier.background(com.agpeya.app.ui.theme.sinqColors.hero)
                        else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    sec.label,
                    style = MaterialTheme.typography.labelLarge.inReadingFont(),
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSel) com.agpeya.app.ui.theme.sinqColors.onHero else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(companions.size, key = { "bk_${companions[it].second}" }) { i ->
            val (title, id) = companions[i]
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable { onOpenBook(id) }
                    .semantics { role = Role.Button }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge.inReadingFont(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
