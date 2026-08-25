package com.agpeya.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing

private data class ReleaseNote(
    val version: String,
    val title: String,
    val changes: List<String>,
)

private val releaseHistory = listOf(
    ReleaseNote(
        version = "1.1.0",
        title = "A cleaner reading journey",
        changes = listOf(
            "Journey and Gitsawe pages are now denser, clearer, and easier to scan.",
            "Reading titles adapt to narrow screens and larger accessibility text.",
            "Share readings as clean text or paginated images without cutting off long passages.",
            "Save reading images directly to the gallery on Android 10 and newer.",
            "Backups now include layouts, custom modes and hours, preferences, and reminder settings.",
            "Date rollover, reminder reliability, touch targets, and private-data protection were improved.",
        ),
    ),
    ReleaseNote(
        version = "1.0.5",
        title = "More dependable reminders",
        changes = listOf(
            "Supporting reminders use notification-friendly alarms instead of unnecessary exact alarms.",
            "Prayer alarms retain precise scheduling when the user explicitly creates them.",
        ),
    ),
    ReleaseNote(
        version = "1.0.4",
        title = "Library and Journey fixes",
        changes = listOf(
            "Improved navigation and presentation across the Library and Journey areas.",
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.whatsNew, onBack = onBack) },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(releaseHistory, key = { it.version }) { release ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(
                            text = "v${release.version}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(release.title, style = MaterialTheme.typography.titleMedium)
                        release.changes.forEach { change ->
                            Text(
                                text = "•  $change",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
