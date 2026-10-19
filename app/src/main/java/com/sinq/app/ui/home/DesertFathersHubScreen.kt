package com.sinq.app.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinq.app.data.DailyQuoteRepository
import com.sinq.app.data.SettingsRepository
import com.sinq.app.data.UserDataRepository
import com.sinq.app.model.DailyQuote
import com.sinq.app.model.toBookmark
import com.sinq.app.ui.common.SinqCard
import com.sinq.app.ui.common.SinqTopBar
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.IconSize
import com.sinq.app.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesertFathersHubScreen(
    onBack: () -> Unit,
    onReflectInJournal: (DailyQuote) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val gold = MaterialTheme.colorScheme.secondary
    val keyboard = LocalSoftwareKeyboardController.current

    val allQuotes = remember { DailyQuoteRepository.quotesBlocking(context) }
    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())
    val randomize by SettingsRepository.randomizeDailyQuote(context).collectAsState(initial = false)

    var searchQuery by remember { mutableStateOf("") }

    val filteredQuotes = remember(allQuotes, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isBlank()) {
            allQuotes
        } else {
            allQuotes.filter { quote ->
                quote.authorEn.lowercase().contains(query) ||
                    quote.authorAm.contains(query) ||
                    quote.quote.lowercase().contains(query)
            }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.sm),
        ) {
            // ── 1. Randomized Daily Reminder Toggle ──
            item(key = "header_settings") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = s.randomizeDailyQuoteTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = gold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = s.randomizeDailyQuoteDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = randomize,
                            onCheckedChange = { next ->
                                scope.launch { SettingsRepository.setRandomizeDailyQuote(context, next) }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.md))
            }

            // ── 2. Search Field ──
            item(key = "header_search") {
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(Spacing.sm))
            }


            // ── 4. Empty State ──
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── 5. Quotes Feed ──
            items(filteredQuotes, key = { it.quoteId }) { quote ->
                val author = if (s.isAmharic) quote.authorAm.ifBlank { quote.authorEn } else quote.authorEn
                val isBookmarked = bookmarks.any { it.hourId == "daily_quote" && it.sectionId == quote.quoteId }

                SinqCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.sm),
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = author,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = gold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (quote.dayGeez.isNotBlank()) {
                                Text(
                                    text = "ቀን ${quote.dayGeez}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Spacer(Modifier.height(Spacing.xs))

                        Text(
                            text = "«${quote.quote.trim()}»",
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(Modifier.height(Spacing.sm))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Apophthegmata Patrum",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    modifier = Modifier.size(32.dp),
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
                                        val shareText = "«${quote.quote}»\n— $author (Apophthegmata Patrum)\n\nስንቅ (Sinq)"
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, s.shareAction))
                                    },
                                    modifier = Modifier.size(32.dp),
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
                                        val clip = ClipData.newPlainText("Desert Father Quote", "«${quote.quote}»\n— $author")
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(clip)
                                        Toast.makeText(context, s.copyAction, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp),
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
                                    modifier = Modifier.size(32.dp),
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
                }
            }
        }
    }
}
