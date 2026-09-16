package com.agpeya.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptureHubScreen(onResume: (String) -> Unit, onSearch: () -> Unit, onBack: () -> Unit, onOpenOldTestament: () -> Unit, onOpenNewTestament: () -> Unit, onOpenPsalms: () -> Unit) {
    val s = LocalStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val lastBook by com.agpeya.app.data.SettingsRepository.lastScriptureBook(context).collectAsState(initial = null)
    val chapters by com.agpeya.app.data.SettingsRepository.lastChapters(context).collectAsState(initial = emptyMap())
    val books = com.agpeya.app.ui.common.rememberContentLoad { com.agpeya.app.data.ScriptureRepository.books(context) }.value.orEmpty()
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { SinqTopBar(title = s.scripturesTitle, onBack = onBack) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.lg)) {
            item { com.agpeya.app.ui.common.ListRow(title = s.tabSearch, onClick = onSearch) }
            lastBook?.let { key -> books.firstOrNull { it.key == key }?.let { book ->
                item { com.agpeya.app.ui.common.ListRow(title = "${book.nameAm} ${chapters[key] ?: 1}",
                    onClick = { onResume(key) }) }
            } }
            item { SectionHeader(s.bibleTitle) }
            item { HubRow(Icons.AutoMirrored.Outlined.MenuBook, s.oldTestamentLabel, "${s.langAmharic} 1980", onOpenOldTestament) }
            item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            item { HubRow(Icons.AutoMirrored.Outlined.MenuBook, s.newTestamentLabel, "${s.langAmharic} 1980", onOpenNewTestament) }
            item { Spacer(Modifier.height(Spacing.xl)); SectionHeader(s.psalterTitle) }
            item { HubRow(Icons.Outlined.LibraryMusic, s.psalterTitle, "${s.wudaseLangAmharic} 1980 · ${s.wudaseLangGeez} 1980", onOpenPsalms) }
        }
    }
}

@Composable
private fun HubRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
    }
}
