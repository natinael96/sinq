package com.agpeya.app.ui.fathers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.agpeya.app.data.FathersRepository
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.sinqColors

/**
 * What the Fathers say about one verse: the extracts the bundled corpus anchors
 * at or before it, each headed by the Father it is credited to.
 *
 * The heading names the anchor rather than the verse that was tapped, because
 * the Fathers expound passages and the reader should see which passage this is.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FathersScreen(
    book: String,
    bookName: String,
    chapter: Int,
    verse: Int,
    reference: String,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    var state by remember(book, chapter, verse) {
        mutableStateOf<FathersRepository.Commentary?>(null)
    }
    var loading by remember(book, chapter, verse) { mutableStateOf(true) }

    LaunchedEffect(book, chapter, verse) {
        loading = true
        state = FathersRepository.forVerse(context, book, chapter, verse)
        loading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.fathersTitle,
                subtitle = reference,
                onBack = onBack,
            )
        },
    ) { inner ->
        val found = state
        when {
            loading -> Column(
                Modifier.fillMaxSize().padding(inner),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }

            found == null || found.passages.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(inner).padding(Spacing.xl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    s.fathersEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                item {
                    Text(
                        if (found.wholeChapter) s.fathersOnChapter(bookName, found.chapter)
                        else s.fathersOnVerse(bookName, found.chapter, found.verse),
                        style = MaterialTheme.typography.labelLarge,
                        color = sinqColors.arke,
                    )
                }
                items(found.passages) { passage ->
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                    ) {
                        if (passage.author.isNotBlank()) {
                            Text(
                                passage.author,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = sinqColors.arke,
                            )
                        }
                        Text(
                            passage.text,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                item {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        HorizontalDivider()
                        Text(
                            found.passages.map { it.work }.distinct().joinToString("  ·  "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
