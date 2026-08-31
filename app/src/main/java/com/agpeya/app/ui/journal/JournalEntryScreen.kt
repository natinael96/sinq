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
import androidx.compose.material.icons.outlined.Check
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/** How long typing must pause before the entry is written. */
private const val AUTOSAVE_DELAY_MS = 1000L

/**
 * Writing, or re-reading, one entry.
 *
 * There is no Save button: the entry saves itself a moment after you stop
 * typing, and again as the screen closes. A tick in the header says when it is
 * safely down — without that, "is this saved?" is a question the screen gives
 * no way to answer, which is worse than a button would have been.
 *
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

    // Saved a second after typing stops, so nothing depends on leaving the
    // screen cleanly: switching apps, a crash, or the system killing the
    // process all leave the text on disk. `saved` drives the header, because a
    // journal that saves invisibly is indistinguishable from one that does not.
    var saved by remember { mutableStateOf(false) }
    LaunchedEffect(body, kind) {
        val current = entry ?: return@LaunchedEffect
        if (body == current.body && kind == current.kind) return@LaunchedEffect
        saved = false
        delay(AUTOSAVE_DELAY_MS)
        JournalRepository.save(context, current.copy(body = body.trim(), kind = kind))
        saved = body.isNotBlank()
    }

    // The last write, as the screen goes. It must NOT use the screen's own
    // scope: Compose cancels a rememberCoroutineScope at exactly the moment
    // onDispose runs, so a save launched there raced the cancellation and could
    // lose the final keystrokes. saveDetached hands it to a process-lived scope.
    val latestBody by rememberUpdatedState(body)
    val latestKind by rememberUpdatedState(kind)
    val latestEntry by rememberUpdatedState(entry)
    DisposableEffect(Unit) {
        onDispose {
            val current = latestEntry ?: return@onDispose
            JournalRepository.saveDetached(
                context,
                current.copy(body = latestBody.trim(), kind = latestKind),
            )
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
                    if (saved) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = s.entrySaved,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
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
