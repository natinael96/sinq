package com.sinq.app.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinq.app.data.DailyQuoteRepository
import com.sinq.app.data.UserDataRepository
import com.sinq.app.model.DailyQuote
import com.sinq.app.model.toBookmark
import com.sinq.app.ui.common.SinqDivider
import com.sinq.app.ui.common.SinqTopBar
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.IconSize
import com.sinq.app.ui.theme.Spacing
import com.sinq.app.ui.theme.inReadingFont
import kotlinx.coroutines.launch

private val PROMINENT_FATHERS_AM = listOf(
    "አባ ፖይመን",
    "አባ መቃርስ ዓቢይ",
    "አባ እንጦንስ ዓቢይ",
    "አባ ሙሴ ፀሊም",
    "አባ አርሳኒዎስ",
    "አባ ዮሐንስ ሐጺር",
    "አባ ሲሶይ",
    "አባ አጋቶን",
    "እመ አብርሃ ስንክሊቲካ",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesertFathersHubScreen(
    onBack: () -> Unit,
    onReflectInJournal: (DailyQuote) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val isAmharic = s.isAmharic
    val gold = MaterialTheme.colorScheme.secondary
    val keyboard = LocalSoftwareKeyboardController.current

    val quotesLoad = com.sinq.app.ui.common.rememberContentLoad {
        DailyQuoteRepository.quotes(context)
    }
    val allQuotes = quotesLoad.value.orEmpty()
    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedAuthor by remember { mutableStateOf<String?>(null) }
    var onlyBookmarked by remember { mutableStateOf(false) }

    val filteredQuotes = remember(allQuotes, searchQuery, selectedAuthor, onlyBookmarked, bookmarks) {
        val query = searchQuery.trim().lowercase()
        allQuotes.filter { quote ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                quote.authorEn.lowercase().contains(query) ||
                    quote.authorAm.contains(query) ||
                    quote.quote.lowercase().contains(query)
            }

            val matchesAuthor = if (selectedAuthor == null) {
                true
            } else {
                quote.authorAm.contains(selectedAuthor!!) ||
                    quote.authorEn.contains(selectedAuthor!!, ignoreCase = true)
            }

            val matchesBookmark = if (!onlyBookmarked) {
                true
            } else {
                bookmarks.any { it.hourId == "daily_quote" && it.sectionId == quote.quoteId }
            }

            matchesQuery && matchesAuthor && matchesBookmark
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.desertFathersHubTitle,
                subtitle = s.desertFathersHubSubtitle,
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .imePadding(),
                contentPadding = PaddingValues(bottom = Spacing.xxl),
            ) {
            // ── Header Controls: Search & Monastic Filter Chips ──────────────
            item(key = "header_controls") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screen)
                        .padding(top = Spacing.xs, bottom = Spacing.sm),
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(s.searchHint) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                        trailingIcon = if (searchQuery.isNotBlank()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = s.cancel,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        } else null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )

                    Spacer(Modifier.height(Spacing.sm))

                    // Horizontal Filter Chips
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        item {
                            FilterChip(
                                selected = selectedAuthor == null && !onlyBookmarked,
                                onClick = {
                                    selectedAuthor = null
                                    onlyBookmarked = false
                                },
                                label = { Text(if (isAmharic) "ሁሉም" else "All") },
                            )
                        }
                        item {
                            FilterChip(
                                selected = onlyBookmarked,
                                onClick = {
                                    onlyBookmarked = !onlyBookmarked
                                    if (onlyBookmarked) selectedAuthor = null
                                },
                                label = { Text(if (isAmharic) "★ የተቀመጡ" else "★ Saved") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = gold.copy(alpha = 0.16f),
                                    selectedLabelColor = gold,
                                ),
                            )
                        }
                        items(PROMINENT_FATHERS_AM, key = { it }) { authorName ->
                            val isSelected = selectedAuthor == authorName
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedAuthor = if (isSelected) null else authorName
                                    if (selectedAuthor != null) onlyBookmarked = false
                                },
                                label = { Text(authorName) },
                            )
                        }
                    }
                }
                SinqDivider()
            }

            // ── Empty State ──────────────────────────────────────────────────
            if (filteredQuotes.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xxl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = s.noResults,
                            style = MaterialTheme.typography.bodyMedium.inReadingFont(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── Unboxed Manuscript Feed with Hairline Dividers ────────────────
            itemsIndexed(filteredQuotes, key = { _, quote -> quote.quoteId }) { _, quote ->
                val author = if (isAmharic) quote.authorAm.ifBlank { quote.authorEn } else quote.authorEn
                val isBookmarked = bookmarks.any { it.hourId == "daily_quote" && it.sectionId == quote.quoteId }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screen, vertical = Spacing.md),
                ) {
                    // Header Row: Monastic cross + Father name + Day badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = "☩",
                                style = MaterialTheme.typography.labelMedium,
                                color = gold,
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Text(
                                text = author,
                                style = MaterialTheme.typography.titleSmall.inReadingFont(),
                                fontWeight = FontWeight.Bold,
                                color = gold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (quote.dayGeez.isNotBlank()) {
                            Text(
                                text = if (isAmharic) "ቀን ${quote.dayGeez}" else "Day ${quote.day}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(Spacing.xs))

                    // Quote text in sacred reading font
                    Text(
                        text = "«${quote.quote.trim()}»",
                        style = MaterialTheme.typography.bodyLarge.inReadingFont(),
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 24.sp,
                    )

                    Spacer(Modifier.height(Spacing.xs))

                    // Liturgical Action Dock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Apophthegmata Patrum",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
                        ) {
                            // Bookmark Button
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        UserDataRepository.toggleBookmark(
                                            context,
                                            quote.toBookmark(s.dailyQuoteChannelName),
                                        )
                                    }
                                },
                                modifier = Modifier.size(44.dp),
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = s.bookmarkAction,
                                    tint = if (isBookmarked) gold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(IconSize.small),
                                )
                            }

                            // Share Button
                            IconButton(
                                onClick = {
                                    val shareText = "«${quote.quote.trim()}»\n— $author (Apophthegmata Patrum)\n\nስንቅ (Sinq)"
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, s.shareAction))
                                },
                                modifier = Modifier.size(44.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = s.shareAction,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(IconSize.small),
                                )
                            }

                            // Copy Button
                            IconButton(
                                onClick = {
                                    val clip = ClipData.newPlainText(
                                        "Desert Father Quote",
                                        "«${quote.quote.trim()}»\n— $author",
                                    )
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(clip)
                                    Toast.makeText(context, s.copyAction, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(44.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = s.copyAction,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(IconSize.small),
                                )
                            }

                            // Reflect in Journal
                            IconButton(
                                onClick = { onReflectInJournal(quote) },
                                modifier = Modifier.size(44.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EditNote,
                                    contentDescription = s.noteAction,
                                    tint = gold,
                                    modifier = Modifier.size(IconSize.small),
                                )
                            }
                        }
                    }
                }
                SinqDivider()
            }
        }
    }
}
}
