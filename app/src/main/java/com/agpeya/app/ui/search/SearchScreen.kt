package com.agpeya.app.ui.search

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.search.AmharicSearch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenResult: (AmharicSearch.Result) -> Unit,
) {
    val context = LocalContext.current
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<AmharicSearch.Result>>(emptyList()) }
    val recent by UserDataRepository.recentSearches(context).collectAsState(initial = emptyList())
    // A typed reference ("መዝሙር 23", "ሉቃስ 10") jumps straight to the page.
    var reference by remember { mutableStateOf<AmharicSearch.Result?>(null) }
    var filter by rememberSaveable { mutableStateOf<AmharicSearch.Source?>(null) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            reference = null
        } else {
            delay(180) // debounce
            reference = AmharicSearch.referenceMatch(
                context,
                query,
                AmharicSearch.Labels(
                    psalter = s.psalterTitle,
                    scripture = s.scripturesTitle,
                    synaxarium = s.synaxariumTitle,
                    wudase = s.wudaseMariam,
                ),
            )
            results = AmharicSearch.search(
                context,
                query,
                AmharicSearch.Labels(
                    psalter = s.psalterTitle,
                    scripture = s.scripturesTitle,
                    synaxarium = s.synaxariumTitle,
                    wudase = s.wudaseMariam,
                ),
            )
        }
    }

    fun open(result: AmharicSearch.Result) {
        scope.launch { UserDataRepository.addRecentSearch(context, query) }
        onOpenResult(result)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(s.tabSearch, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(s.searchHint) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Search,
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                shape = MaterialTheme.shapes.large,
            )
            Spacer(Modifier.height(8.dp))

            when {
                // Idle: offer recent searches to tap.
                query.trim().length < 2 && recent.isNotEmpty() -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text(
                            s.recentSearches,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        TextButton(onClick = { scope.launch { UserDataRepository.clearRecentSearches(context) } }) {
                            Text(s.clearAction, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(recent) { term ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { query = term }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(term, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
                query.trim().length >= 2 && results.isEmpty() && reference == null -> {
                    Spacer(Modifier.height(40.dp))
                    Text(
                        text = s.noResults,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
                else -> {
                    // Counts drive the filter row; grouping keeps a big corpus
                    // from burying a single prayer match.
                    val counts = remember(results) { results.groupingBy { it.source }.eachCount() }
                    val shown = remember(results, filter) {
                        if (filter == null) results else results.filter { it.source == filter }
                    }
                    val grouped = remember(shown) { shown.groupBy { it.source } }

                    SourceFilters(counts, filter) { filter = it }

                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        reference?.let { ref ->
                            item(key = "ref") {
                                ReferenceRow(ref) { open(ref) }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                        SOURCE_ORDER.forEach { source ->
                            val rows = grouped[source].orEmpty()
                            if (rows.isEmpty()) return@forEach
                            item(key = "h_$source") {
                                Text(
                                    text = sourceTitle(source, s),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                )
                            }
                            items(
                                rows,
                                key = { "${it.source}:${it.targetId}:${it.targetIndex}" },
                            ) { r ->
                                ResultRow(r) { open(r) }
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(result: AmharicSearch.Result, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            SourceTag(result.sourceLabel)
            Spacer(Modifier.width(8.dp))
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = highlighted(result),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

/** Small pill naming where the result came from (an hour, or the Psalter). */
@Composable
private fun SourceTag(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** Snippet with the matched span bolded and accented. */
@Composable
private fun highlighted(result: AmharicSearch.Result) = buildAnnotatedString {
    val text = result.snippet
    val start = result.snippetMatchStart
    val end = start + result.snippetMatchLen
    if (start in 0..text.length && end in start..text.length) {
        append(text.substring(0, start))
        pushStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold))
        append(text.substring(start, end))
        pop()
        append(text.substring(end))
    } else {
        append(text)
    }
}

/** Fixed display order, so results don't reshuffle between queries. */
private val SOURCE_ORDER = listOf(
    AmharicSearch.Source.HOUR,
    AmharicSearch.Source.PSALTER,
    AmharicSearch.Source.SCRIPTURE,
    AmharicSearch.Source.SYNAXARIUM,
    AmharicSearch.Source.WUDASE,
)

private fun sourceTitle(source: AmharicSearch.Source, s: com.agpeya.app.ui.strings.Strings): String =
    when (source) {
        AmharicSearch.Source.HOUR -> s.hoursHeader
        AmharicSearch.Source.PSALTER -> s.psalterTitle
        AmharicSearch.Source.SCRIPTURE -> s.scripturesTitle
        AmharicSearch.Source.SYNAXARIUM -> s.synaxariumTitle
        AmharicSearch.Source.WUDASE -> s.wudaseMariam
    }

/** Filter row: "All" plus one chip per corpus that actually matched. */
@Composable
private fun SourceFilters(
    counts: Map<AmharicSearch.Source, Int>,
    active: AmharicSearch.Source?,
    onSelect: (AmharicSearch.Source?) -> Unit,
) {
    if (counts.size < 2) return   // nothing to filter between
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        item {
            FilterChipRow(s.filterAll, counts.values.sum(), active == null) { onSelect(null) }
        }
        SOURCE_ORDER.filter { counts.containsKey(it) }.forEach { source ->
            item {
                FilterChipRow(sourceTitle(source, s), counts[source] ?: 0, active == source) {
                    onSelect(if (active == source) null else source)
                }
            }
        }
    }
}

@Composable
private fun FilterChipRow(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("$label  $count", style = MaterialTheme.typography.labelMedium) },
    )
}

/** A typed reference resolved to a page — shown above the text matches. */
@Composable
private fun ReferenceRow(result: AmharicSearch.Result, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                result.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                result.sourceLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
