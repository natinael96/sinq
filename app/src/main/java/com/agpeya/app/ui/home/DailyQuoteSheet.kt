package com.agpeya.app.ui.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.model.DailyQuote
import com.agpeya.app.model.toBookmark
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Bottom sheet displaying today's Desert Father saying for contemplation,
 * with quick actions to bookmark, share and journal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyQuoteSheet(
    quote: DailyQuote,
    onDismiss: () -> Unit,
    onReflectInJournal: (DailyQuote) -> Unit,
    isBookmarked: Boolean? = null,
    onToggleBookmark: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val gold = MaterialTheme.colorScheme.secondary

    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())
    val bookmarked = isBookmarked ?: remember(bookmarks, quote.quoteId) {
        bookmarks.any { it.hourId == "daily_quote" && it.sectionId == quote.quoteId }
    }
    val toggle: () -> Unit = onToggleBookmark ?: {
        scope.launch {
            UserDataRepository.toggleBookmark(context, quote.toBookmark(s.dailyQuoteChannelName))
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.lg),
        ) {
            // Header Row: Badge & Actions (Bookmark, Close)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(gold.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "☩ ${s.dailyQuoteChannelName}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = gold,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = toggle, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = s.bookmarkAction,
                            tint = if (bookmarked) gold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.small),
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = s.dismiss,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.small),
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xs))

            // Author name
            Text(
                text = quote.authorAm.ifBlank { quote.authorEn },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (quote.authorEn.isNotBlank() && quote.authorAm.isNotBlank()) {
                Text(
                    text = quote.authorEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(Spacing.md))

            // Quote Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .background(gold),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = Spacing.md, vertical = Spacing.md)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = "«${quote.quote}»",
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            text = "Apophthegmata Patrum  ·  የአበው ምክር",
                            style = MaterialTheme.typography.labelSmall,
                            color = gold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            // Action Buttons: Share & Journal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(
                    onClick = { shareQuote(context, quote) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.small),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(s.shareAction)
                }

                Button(
                    onClick = {
                        onDismiss()
                        onReflectInJournal(quote)
                    },
                    modifier = Modifier.weight(1.3f),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor = MaterialTheme.colorScheme.background,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.small),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = "በማስታወሻ ጻፍ",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun shareQuote(context: Context, quote: DailyQuote) {
    val author = quote.authorAm.ifBlank { quote.authorEn }
    val text = "«${quote.quote}»\n\n— $author (የአበው ምክር)\n\nስንቅ (Sinq)"
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}
