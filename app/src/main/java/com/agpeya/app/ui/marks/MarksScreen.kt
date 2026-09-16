package com.agpeya.app.ui.marks

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.HighlightRepository
import com.agpeya.app.data.JournalRepository
import com.agpeya.app.data.MarksRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.ui.common.ListRow
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.sinqColors
import kotlinx.coroutines.launch

/**
 * ምልክቶቼ — one home for every mark.
 *
 * Bookmarks, highlights and the notes written from a passage were in three
 * places, two of which had no list at all: a highlight could only be found by
 * remembering where it was. Three tabs, one export, and every row is the
 * citation with two lines of the text under it — resolved from the bundle each
 * time, never stored, so a mark cannot go stale against a corrected text.
 */
@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun MarksScreen(
    onBack: () -> Unit,
    onOpen: (hourId: String, sectionIndex: Int, sectionId: String) -> Unit,
    onOpenRoute: (route: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    var query by rememberSaveable { mutableStateOf("") }
    var colorFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())
    val highlightMap by HighlightRepository.highlights(context).collectAsState(initial = emptyMap())
    var exportTab by rememberSaveable { mutableIntStateOf(0) }
    var exportResult by remember { mutableStateOf<Boolean?>(null) }
    val names by SettingsRepository.highlightNames(context).collectAsState(initial = emptyMap())
    val highlights by produceState(emptyList<MarksRepository.Mark>(), highlightMap, s) {
        value = MarksRepository.highlights(context, highlightMap, s.psalmName, s.psalterTitle)
    }

    val exporter = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = marksExportText(
                if (exportTab == 0) bookmarks else emptyList(),
                if (exportTab == 1) highlights else emptyList(), emptyList(), names, s,
            )
            exportResult = runCatching {
                val stream = context.contentResolver.openOutputStream(uri) ?: error("Cannot open export")
                stream.use { it.write(text.toByteArray()) }
            }.isSuccess
        }
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.marksTitle,
                onBack = onBack,
                actions = {
                    TextButton(onClick = {
                        if (tab == 2) onOpenRoute("settings/records")
                        else { exportTab = tab; exporter.launch("marks-${java.time.LocalDate.now()}.txt") }
                    }) {
                        Text(if (tab == 2) s.backupExport else s.marksExport, style = MaterialTheme.typography.labelLarge)
                    }
                },
            )
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                listOf(s.marksTabBookmarks, s.marksTabHighlights, s.marksTabNotes)
                    .forEachIndexed { index, label ->
                        Tab(
                            selected = tab == index,
                            onClick = { tab = index },
                            text = { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1) },
                        )
                    }
            }
            if (tab == 2) {
                com.agpeya.app.ui.journal.JournalAccess(onBack) {
                    val notes by JournalRepository.fromPassages(context).collectAsState(initial = emptyList())
                    var noteQuery by remember { mutableStateOf("") }
                    Column {
                    androidx.compose.material3.OutlinedTextField(value = noteQuery, onValueChange = { noteQuery = it },
                        label = { Text(s.tabSearch) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen))
                    LazyColumn(contentPadding = PaddingValues(Spacing.screen)) {
                        item { Text(s.exportJournalWarning, style = MaterialTheme.typography.bodySmall) }
                        if (notes.isEmpty()) item { StatePanel(title = s.journalEmpty) }
                        noteTab(notes.filter { it.body.contains(noteQuery, ignoreCase = true) || it.anchorLabel.orEmpty().contains(noteQuery, ignoreCase = true) }, onOpenRoute)
                    }
                    }
                }
                return@Column
            }
            androidx.compose.material3.OutlinedTextField(value = query, onValueChange = { query = it },
                label = { Text(s.tabSearch) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen))
            if (tab == 1) androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.screen), horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { androidx.compose.material3.FilterChip(selected = colorFilter == null,
                    onClick = { colorFilter = null }, label = { Text(s.filterAll) }) }
                items(HighlightRepository.COLOR_KEYS.size) { index ->
                    val color = HighlightRepository.COLOR_KEYS[index]
                    androidx.compose.material3.FilterChip(selected = colorFilter == color,
                        onClick = { colorFilter = if (colorFilter == color) null else color },
                        label = { Text(names[color]?.takeIf { it.isNotBlank() } ?: s.highlightColor(color)) })
                }
            }
            val shownBookmarks = bookmarks.filter { it.title.contains(query, ignoreCase = true) || it.hourName.contains(query, ignoreCase = true) }
            val shownHighlights = highlights.filter {
                (colorFilter == null || it.colorKey == colorFilter) &&
                    (it.citation.contains(query, ignoreCase = true) || it.snippet.contains(query, ignoreCase = true))
            }
            val empty = when (tab) {
                0 -> shownBookmarks.isEmpty()
                1 -> shownHighlights.isEmpty()
                else -> true
            }
            if (empty) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    StatePanel(
                        icon = Icons.Outlined.BookmarkBorder,
                        title = if (query.isNotBlank() || colorFilter != null) s.noResults else if (tab == 0) s.marksTabBookmarks else s.marksTabHighlights,
                        body = s.noMarksBody,
                    )
                }
                return@Column
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
            ) {
                when (tab) {
                    0 -> bookmarkTab(shownBookmarks, s, onOpen, onOpenRoute) { bm ->
                        scope.launch {
                            UserDataRepository.removeBookmark(context, bm.hourId, bm.sectionId)
                            if (snackbar.showSnackbar(s.remove, s.undoAction) == androidx.compose.material3.SnackbarResult.ActionPerformed)
                                UserDataRepository.mergeBookmarks(context, listOf(bm))
                        }
                    }
                    1 -> markTab(shownHighlights, names, s, onOpenRoute) { mark ->
                        scope.launch {
                            HighlightRepository.setHighlights(context, mark.keys, null)
                            if (snackbar.showSnackbar(s.remove, s.undoAction) == androidx.compose.material3.SnackbarResult.ActionPerformed)
                                HighlightRepository.setHighlights(context, mark.keys, mark.colorKey)
                        }
                    }

                }
                item { Spacer(Modifier.height(Spacing.xxl)) }
            }
        }
    }
    exportResult?.let { ok ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { exportResult = null },
            title = { Text(if (ok) s.backupSaved else s.backupFailed) },
            confirmButton = { TextButton(onClick = { exportResult = null }) { Text(s.ok) } },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.bookmarkTab(
    bookmarks: List<com.agpeya.app.model.Bookmark>,
    s: com.agpeya.app.ui.strings.Strings,
    onOpen: (String, Int, String) -> Unit,
    onOpenRoute: (String) -> Unit,
    onRemove: (com.agpeya.app.model.Bookmark) -> Unit,
) {
    // Grouped by source, and the header resolves its label live: the stored
    // hourName is a creation-time snapshot, so one hour can carry two of them.
    bookmarks.groupBy { it.hourId }.forEach { (hourId, items) ->
        item(key = "h_$hourId") {
            Spacer(Modifier.height(Spacing.md))
            SectionHeader(
                when (hourId) {
                    com.agpeya.app.ui.psalter.PSALTER_BOOKMARK_ID -> s.psalterTitle
                    "scripture_library" -> s.bookmarkGroupScripture
                    "sinksar_verse" -> s.bookmarkGroupSynaxarium
                    else -> items.last().hourName
                },
            )
            Spacer(Modifier.height(Spacing.xs))
        }
        items(items.size, key = { "$hourId|${items[it].sectionId}" }) { i ->
            val bm = items[i]
            ListRow(
                title = bm.title,
                subtitle = bm.subtitle,
                onClick = {
                    if (bm.route != null) onOpenRoute(bm.route) else onOpen(bm.hourId, bm.sectionIndex, bm.sectionId)
                },
            ) {
                RemoveButton(s.removeAction) { onRemove(bm) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.markTab(
    marks: List<MarksRepository.Mark>,
    names: Map<String, String>,
    s: com.agpeya.app.ui.strings.Strings,
    onOpenRoute: (String) -> Unit,
    onRemove: (MarksRepository.Mark) -> Unit,
) {
    marks.groupBy { it.group }.forEach { (group, items) ->
        item(key = "g_$group") {
            Spacer(Modifier.height(Spacing.md))
            SectionHeader(group)
            Spacer(Modifier.height(Spacing.xs))
        }
        items(items.size, key = { "$group|${items[it].citation}|${items[it].colorKey}" }) { i ->
            MarkRow(
                mark = items[i],
                colorName = items[i].colorKey?.let { key ->
                    names[key]?.takeIf { it.isNotBlank() } ?: s.highlightColor(key)
                },
                removeLabel = s.removeAction,
                onOpen = { items[i].route?.let(onOpenRoute) },
                onRemove = { onRemove(items[i]) },
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.noteTab(
    notes: List<com.agpeya.app.model.JournalEntry>,
    onOpenRoute: (String) -> Unit,
) {
    items(notes.size, key = { notes[it].id }) { i ->
        val entry = notes[i]
        ListRow(
            title = entry.anchorLabel.orEmpty().ifBlank { entry.preview },
            subtitle = entry.preview.takeIf { it.isNotBlank() },
            onClick = { onOpenRoute("journal/entry?id=${android.net.Uri.encode(entry.id)}") },
        )
    }
}

/** The citation, the colour it was painted in, and two lines of the text. */
@Composable
private fun MarkRow(
    mark: MarksRepository.Mark,
    colorName: String?,
    removeLabel: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val sinq = sinqColors
    ListRow(
        title = mark.citation,
        subtitle = mark.snippet.takeIf { it.isNotBlank() },
        onClick = onOpen,
        leadingIcon = mark.colorKey?.let { androidx.compose.material.icons.Icons.Filled.Circle },
        leadingTint = mark.colorKey?.let { sinq.highlight(it).copy(alpha = 1f) },
    ) {
        Column(horizontalAlignment = Alignment.End) {
            colorName?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        RemoveButton(removeLabel, onRemove)
    }
}

@Composable
private fun RemoveButton(label: String, onRemove: () -> Unit) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    IconButton(onClick = {
        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        onRemove()
    }) {
        Icon(
            Icons.Outlined.Close,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize.small),
        )
    }
}

/**
 * Every mark as one plain-text file: citation, colour, and the text itself.
 *
 * Plain text and nothing else. This is a reading record, not a backup — the
 * backup already carries the marks in a form the app can restore, and a file a
 * person opens on a laptop should be readable there without this app.
 */
internal fun marksExportText(
    bookmarks: List<com.agpeya.app.model.Bookmark>,
    highlights: List<MarksRepository.Mark>,
    notes: List<com.agpeya.app.model.JournalEntry>,
    names: Map<String, String>,
    s: com.agpeya.app.ui.strings.Strings,
): String = buildString {
    appendLine(s.marksTitle)
    appendLine()
    if (bookmarks.isNotEmpty()) {
        appendLine(s.marksTabBookmarks)
        bookmarks.forEach { bm ->
            appendLine("· ${bm.title}")
            bm.subtitle?.takeIf { it.isNotBlank() }?.let { appendLine("  $it") }
        }
        appendLine()
    }
    if (highlights.isNotEmpty()) {
        appendLine(s.marksTabHighlights)
        highlights.forEach { mark ->
            val colour = mark.colorKey?.let { names[it]?.takeIf { n -> n.isNotBlank() } ?: s.highlightColor(it) }
            appendLine("· " + listOfNotNull(mark.citation, colour).joinToString("  ·  "))
            if (mark.snippet.isNotBlank()) appendLine("  ${mark.snippet}")
        }
        appendLine()
    }
    if (notes.isNotEmpty()) {
        appendLine(s.marksTabNotes)
        notes.forEach { entry ->
            appendLine("· " + listOfNotNull(entry.anchorLabel, entry.date).joinToString("  ·  "))
            if (entry.body.isNotBlank()) appendLine("  ${entry.body.replace("\n", "\n  ")}")
        }
    }
}
