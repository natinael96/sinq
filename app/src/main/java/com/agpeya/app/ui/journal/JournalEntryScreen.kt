package com.agpeya.app.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.JournalRepository
import com.agpeya.app.model.JournalEntry
import com.agpeya.app.model.JournalKind
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.formatEthiopian
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Writing, or re-reading, one entry.
 *
 * There is no Save button. The entry is written when the screen leaves, which
 * is how a notebook behaves — you do not commit a page, you simply close it.
 * An entry left blank is deleted rather than stored, so the month view never
 * claims a day was written on when it was not.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun JournalEntryScreen(
    entryId: String?,
    initialKind: JournalKind,
    anchorRoute: String?,
    anchorLabel: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    // No screenshots, and nothing in the recents thumbnail.
    SecureScreen()

    var entry by remember { mutableStateOf<JournalEntry?>(null) }
    var body by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(initialKind) }
    var deleting by remember { mutableStateOf(false) }

    // Load an existing entry, or mint a draft stamped with today's Church day.
    LaunchedEffect(entryId) {
        val loaded = entryId?.let { JournalRepository.byId(context, it) }
            ?: JournalRepository.draft(
                context = context,
                kind = initialKind,
                anchorRoute = anchorRoute,
                anchorLabel = anchorLabel,
            )
        entry = loaded
        body = loaded.body
        kind = loaded.kind
    }

    // Persist on the way out — including a system back, a process death, or the
    // user switching apps. rememberUpdatedState keeps the effect reading the
    // latest text rather than whatever was typed when it was first composed.
    val latestBody by rememberUpdatedState(body)
    val latestKind by rememberUpdatedState(kind)
    val latestEntry by rememberUpdatedState(entry)
    DisposableEffect(Unit) {
        onDispose {
            val current = latestEntry ?: return@onDispose
            scope.launch {
                JournalRepository.save(
                    context,
                    current.copy(body = latestBody.trim(), kind = latestKind),
                )
            }
        }
    }

    val loaded = entry
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.journalTitle,
                subtitle = loaded?.localDate?.let { formatEthiopian(it, s) },
                // The feast or fast the day carried, in the app's gold accent —
                // the reason this entry reads as more than a dated note later.
                accentLine = listOfNotNull(
                    loaded?.context?.monthlyFeast,
                    loaded?.context?.fast,
                ).joinToString(" · ").takeIf { it.isNotBlank() },
                onBack = onBack,
                actions = {
                    if (loaded != null && entryId != null) {
                        IconButton(onClick = { deleting = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = s.delete,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    JournalKind.REFLECTION to s.journalKindReflection,
                    JournalKind.PASSAGE to s.journalKindPassage,
                    JournalKind.CONFESSION_DRAFT to s.journalKindConfession,
                ).forEach { (choice, label) ->
                    FilterChip(
                        selected = kind == choice,
                        onClick = { kind = choice },
                        label = { Text(label, maxLines = 1) },
                    )
                }
            }
            // Said plainly, at the moment the kind is chosen, because it is the
            // one kind whose handling the person needs to be able to rely on.
            if (kind == JournalKind.CONFESSION_DRAFT) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    s.journalKindConfessionNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            loaded?.anchorLabel?.let {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                placeholder = { Text(s.entryBodyHint) },
                modifier = Modifier.fillMaxWidth().height(400.dp),
            )
            Spacer(Modifier.height(Spacing.huge))
        }
    }

    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(s.delete) },
            text = { Text(s.deleteEntryConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false
                    val id = loaded?.id
                    // Clear the body first so the save-on-dispose cannot write
                    // the entry back after the delete has run.
                    body = ""
                    entry = null
                    scope.launch {
                        if (id != null) JournalRepository.delete(context, id)
                        onBack()
                    }
                }) { Text(s.delete) }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text(s.cancel) } },
        )
    }
}
