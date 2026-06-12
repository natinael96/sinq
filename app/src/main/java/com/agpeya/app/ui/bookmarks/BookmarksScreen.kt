package com.agpeya.app.ui.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.model.Bookmark
import com.agpeya.app.ui.common.AgpeyaBottomBar
import com.agpeya.app.ui.common.Tab
import kotlinx.coroutines.launch

@Composable
fun BookmarksScreen(
    onSelectTab: (Tab) -> Unit,
    onOpen: (hourId: String, sectionIndex: Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())
    val grouped = bookmarks.groupBy { it.hourId to it.hourName }
    val s = com.agpeya.app.ui.strings.LocalStrings.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { AgpeyaBottomBar(current = Tab.BOOKMARKS, onSelect = onSelectTab) },
    ) { innerPadding ->
        if (bookmarks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = s.noBookmarksTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = s.noBookmarksBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            item {
                Text(
                    text = s.bookmarksTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
            }
            grouped.forEach { (hour, items) ->
                item(key = "h_${hour.first}") {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = hour.second,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                items(items.size, key = { items[it].sectionId }) { i ->
                    BookmarkRow(
                        bookmark = items[i],
                        onOpen = { onOpen(items[i].hourId, items[i].sectionIndex) },
                        onRemove = {
                            scope.launch { UserDataRepository.removeBookmark(context, items[i].sectionId) }
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BookmarkRow(bookmark: Bookmark, onOpen: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            bookmark.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Outlined.BookmarkRemove,
                contentDescription = "አስወግድ",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
