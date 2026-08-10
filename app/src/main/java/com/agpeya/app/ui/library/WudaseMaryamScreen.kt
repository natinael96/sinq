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
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.readingBodyStyle
import kotlinx.coroutines.launch
import java.time.LocalDate

private val FONT_STEPS_SP = listOf(17, 19, 22, 25, 29)

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
            TopAppBar(
                title = { Text(s.wudaseMariam, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                actions = {
                    FontSizeActions(fontStep = fontStep, maxStep = FONT_STEPS_SP.lastIndex) { step ->
                        scope.launch { SettingsRepository.setFontStep(context, step) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        if (data == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
            return@Scaffold
        }
        if (sections.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    s.contentUnavailable,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            return@Scaffold
        }

        val section = sections.getOrNull(selected.coerceIn(0, sections.size - 1))
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        // Jump back to the top when the user picks a different section — the keys
        // are positional, so the old scroll offset would otherwise carry deep into
        // the new day's text. Keyed off a saved sentinel so rotation doesn't jump.
        var lastShown by rememberSaveable { mutableIntStateOf(-1) }
        androidx.compose.runtime.LaunchedEffect(selected) {
            if (lastShown != -1 && lastShown != selected) listState.scrollToItem(0)
            lastShown = selected
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 22.dp),
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
                    Text(
                        text = stanzas[i],
                        style = readingBodyStyle(bodyFontSp),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    )
                }
            }
            if (data.attribution.isNotBlank()) {
                item(key = "attr") {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = data.attribution,
                        style = MaterialTheme.typography.labelSmall.inReadingFont(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item { Spacer(Modifier.height(48.dp)) }
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
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
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
            .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.inReadingFont(),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        if (isSel) Modifier.background(MaterialTheme.colorScheme.primary)
                        else Modifier.border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    )
                    .clickable { onSelect(i) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    sec.label,
                    style = MaterialTheme.typography.labelLarge.inReadingFont(),
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
