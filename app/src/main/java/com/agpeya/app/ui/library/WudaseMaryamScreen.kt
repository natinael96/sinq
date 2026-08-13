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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.WudaseRepository
import com.agpeya.app.model.WudaseContent
import com.agpeya.app.model.WudaseSection
import com.agpeya.app.ui.reading.FontSizeActions
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.theme.Spacing
import androidx.compose.material.icons.outlined.MenuBook
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
fun WudaseMaryamScreen(onBack: () -> Unit, initialSectionId: String? = null) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()

    val content by produceState<WudaseContent?>(initialValue = null) {
        value = WudaseRepository.load(context)
    }
    val fontStep by SettingsRepository.fontStep(context).collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]

    var geez by rememberSaveable { mutableStateOf(false) }

    val data = content
    val sections = data?.sections ?: emptyList()

    // Default to today's weekday portion (1=Mon … 7=Sun), else the first section.
    // -1 means "no explicit choice yet" so a rotation never resets a picked day.
    val todayIndex = remember(sections) {
        val wd = LocalDate.now().dayOfWeek.value
        sections.indexOfFirst { it.weekday == wd }.takeIf { it >= 0 } ?: 0
    }
    val initialIndex = remember(sections, initialSectionId) {
        initialSectionId?.let { id -> sections.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
    }
    var picked by rememberSaveable { mutableIntStateOf(-1) }
    val selected = if (picked >= 0) picked else initialIndex ?: todayIndex

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.wudaseMariam,
                onBack = onBack,
                actions = {
                    // The whole portion being read, in the language being read.
                    val shown = sections.getOrNull(selected.coerceIn(0, (sections.size - 1).coerceAtLeast(0)))
                    com.agpeya.app.ui.common.ShareMenuAction(enabled = shown != null, payload = {
                        shown?.let {
                            com.agpeya.app.ui.common.SharePayload(
                                body = (if (geez) it.ge else it.am).joinToString("\n\n"),
                                kicker = s.wudaseMariam,
                                title = if (geez) it.titleGe else it.titleAm,
                            )
                        }
                    })
                    FontSizeActions(fontStep = fontStep, maxStep = FONT_STEPS_SP.lastIndex) { step ->
                        scope.launch { SettingsRepository.setFontStep(context, step) }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (data == null) {
            LoadingPanel(Modifier.padding(innerPadding))
            return@Scaffold
        }
        if (sections.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                StatePanel(
                    icon = Icons.Outlined.MenuBook,
                    title = s.contentUnavailable,
                    body = s.contentMissingBody,
                )
            }
            return@Scaffold
        }

        val section = sections.getOrNull(selected.coerceIn(0, sections.size - 1))
        // Live stanza selection: tap anchors, next tap moves the end; -1 = none.
        var selA by rememberSaveable(selected, geez) { mutableIntStateOf(-1) }
        var selB by rememberSaveable(selected, geez) { mutableIntStateOf(-1) }
        val selRange = com.agpeya.app.ui.reading.flatSelectionRange(selA, selB)
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        // Jump back to the top when the user picks a different section — the keys
        // are positional, so the old scroll offset would otherwise carry deep into
        // the new day's text. Keyed off a saved sentinel so rotation doesn't jump.
        var lastShown by rememberSaveable { mutableIntStateOf(-1) }
        androidx.compose.runtime.LaunchedEffect(selected) {
            if (lastShown != -1 && lastShown != selected) listState.scrollToItem(0)
            lastShown = selected
        }
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = Spacing.screen),
        ) {
            item(key = "lang") {
                Spacer(Modifier.height(8.dp))
                LanguageToggle(geez = geez, amharicLabel = s.wudaseLangAmharic, geezLabel = s.wudaseLangGeez) { geez = it }
            }
            item(key = "days") {
                SectionStrip(sections, selected) { picked = it }
                Spacer(Modifier.height(6.dp))
            }
            if (section != null) {
                item(key = "title") {
                    Text(
                        text = if (geez) section.titleGe else section.titleAm,
                        style = MaterialTheme.typography.titleMedium.inReadingFont(),
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp),
                    )
                }
                val stanzas = if (geez) section.ge else section.am
                items(stanzas.size, key = { "st_$it" }) { i ->
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
                                    if (i in selRange)
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                                    else androidx.compose.ui.graphics.Color.Transparent
                                )
                                .clickable {
                                    val (a, bSel) = com.agpeya.app.ui.reading.advanceFlatSelection(selA, i)
                                    selA = a
                                    selB = bSel
                                }
                                .padding(bottom = 16.dp),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(48.dp)) }
        }
        val stanzasNow = if (section == null) emptyList() else (if (geez) section.ge else section.am)
        val selBody = if (selRange.isEmpty()) null
        else stanzasNow.filterIndexed { i, _ -> i in selRange }.joinToString("\n\n").ifBlank { null }
        val sectionTitle = section?.let { if (geez) it.titleGe else it.titleAm }
        com.agpeya.app.ui.reading.SelectionShareBar(
            visible = selA >= 0,
            onDismiss = { selA = -1; selB = -1 },
            shareText = selBody?.let { listOfNotNull(sectionTitle, it).joinToString("\n\n") },
            shareImage = selBody?.let {
                com.agpeya.app.ui.common.SharePayload(body = it, kicker = s.wudaseMariam, title = sectionTitle)
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        }
    }
}

/** Amharic ⇄ Ge'ez segmented toggle. */
@Composable
private fun LanguageToggle(geez: Boolean, amharicLabel: String, geezLabel: String, onSelect: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ToggleHalf(amharicLabel, selected = !geez, modifier = Modifier.weight(1f)) { onSelect(false) }
        ToggleHalf(geezLabel, selected = geez, modifier = Modifier.weight(1f)) { onSelect(true) }
    }
}

@Composable
private fun ToggleHalf(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) com.agpeya.app.ui.theme.sinqColors.hero else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.inReadingFont(),
            color = if (selected) com.agpeya.app.ui.theme.sinqColors.onHero else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionStrip(sections: List<WudaseSection>, selected: Int, onSelect: (Int) -> Unit) {
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
    }
}
