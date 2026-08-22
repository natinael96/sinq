package com.agpeya.app.ui.library

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ScriptureRepository
import com.agpeya.app.model.ScriptureBookMeta
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.theme.Spacing

/** One testament from the unified Amharic 2000 Bible, grouped by canon section. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptureListScreen(testament: String, onBack: () -> Unit, onOpenBook: (String) -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val books by produceState<List<ScriptureBookMeta>>(initialValue = emptyList()) {
        value = ScriptureRepository.books(context)
    }
    val groups = books.filter { it.testament == testament }
        .groupBy { it.section.ifBlank { if (testament == "old") s.oldTestamentLabel else s.newTestamentLabel } }
        .toList()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.scripturesTitle,
                subtitle = if (testament == "old") s.oldTestamentLabel else s.newTestamentLabel,
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
            groups.forEach { (label, groupBooks) ->
                if (groupBooks.isEmpty()) return@forEach
                item(key = "h_$label") {
                    SectionHeader(
                        text = label,
                        modifier = Modifier.padding(top = Spacing.xl, bottom = Spacing.xs),
                    )
                }
                items(groupBooks, key = { it.key }) { book ->
                    BookRow(book = book, unit = s.chapterUnit, onClick = { onOpenBook(book.key) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BookRow(book: ScriptureBookMeta, unit: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(book.nameAm, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${geezNumeral(book.chapters)} $unit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
