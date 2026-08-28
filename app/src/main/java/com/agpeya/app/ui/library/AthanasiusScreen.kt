package com.agpeya.app.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.agpeya.app.data.GitsaweLinks
import com.agpeya.app.data.GitsaweRepository
import com.agpeya.app.data.ReadingTarget
import com.agpeya.app.model.AthanasiusEntry
import com.agpeya.app.model.GitsaweReading
import com.agpeya.app.model.GitsaweService
import com.agpeya.app.ui.common.LoadingPanel
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont

/** Master Part 4 — explicitly selected funeral and memorial readings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthanasiusScreen(
    onBack: () -> Unit,
    onOpenReading: (ReadingTarget, String) -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val entries by produceState<List<AthanasiusEntry>?>(null) {
        value = GitsaweRepository.athanasius(context)
    }
    var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val selected = entries?.firstOrNull { it.index == selectedIndex }
    BackHandler(enabled = selected != null) { selectedIndex = null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.athanasiusTitle,
                subtitle = selected?.title ?: s.athanasiusSubtitle,
                onBack = { if (selected != null) selectedIndex = null else onBack() },
            )
        },
    ) { padding ->
        val data = entries
        if (data == null) {
            LoadingPanel(Modifier.padding(padding))
        } else if (selected == null) {
            AthanasiusContents(data, Modifier.padding(padding)) { selectedIndex = it.index }
        } else {
            AthanasiusReading(selected, Modifier.padding(padding), onOpenReading)
        }
    }
}

@Composable
private fun AthanasiusContents(
    entries: List<AthanasiusEntry>,
    modifier: Modifier,
    onOpen: (AthanasiusEntry) -> Unit,
) {
    val s = LocalStrings.current
    val labels = mapOf(
        "person" to s.athanasiusPeople,
        "riteChapter" to s.athanasiusRiteChapters,
        "burialPrayer" to s.athanasiusBurialPrayers,
        "memorial" to s.athanasiusMemorials,
    )
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        labels.forEach { (category, label) ->
            item(key = "heading_$category") {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xxs),
                )
            }
            items(entries.filter { it.category == category }, key = { it.index }) { entry ->
                SinqCard(
                    onClick = { onOpen(entry) },
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
                ) {
                    Text(entry.title, style = MaterialTheme.typography.bodyMedium.inReadingFont(), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    entry.memorialDay?.let {
                        Text(s.memorialDay(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(Spacing.xxl)) }
    }
}

@Composable
private fun AthanasiusReading(
    entry: AthanasiusEntry,
    modifier: Modifier,
    onOpenReading: (ReadingTarget, String) -> Unit,
) {
    val s = LocalStrings.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        entry.supplication?.let { text ->
            item {
                Column {
                    Text(s.supplicationLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(text, style = MaterialTheme.typography.bodyLarge.inReadingFont())
                }
            }
        }
        items(entry.labeledReadings()) { (role, reading) ->
            val target = reading.verse?.let(GitsaweLinks::target)
            Column(
                Modifier.fillMaxWidth().then(
                    if (target != null) Modifier.clickable { onOpenReading(target, role) } else Modifier,
                ).padding(vertical = Spacing.xs),
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Text(role, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        reading.verse?.bookTitle ?: reading.citation.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                reading.text?.geez?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge.inReadingFont())
                }
                reading.citation?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Spacer(Modifier.height(Spacing.xxl)) }
    }
}

private fun AthanasiusEntry.labeledReadings(): List<Pair<String, GitsaweReading>> =
    listOfNotNull(negh, kidassie, serk).flatMap(::labeledReadings)

private fun labeledReadings(service: GitsaweService): List<Pair<String, GitsaweReading>> = buildList {
    service.msbak.forEach { add("ምስባክ" to it) }
    service.wengel.forEach { add("ወንጌል" to it) }
    service.firstDeacon.forEach { add("፩ ዲያቆን" to it) }
    service.secondDeacon.forEach { add("፪ ዲያቆን" to it) }
    service.secondKahn.forEach { add("ካህን" to it) }
}
