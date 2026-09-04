package com.agpeya.app.ui.nisiha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.agpeya.app.data.JournalLock
import com.agpeya.app.data.JournalRepository
import com.agpeya.app.data.NisihaRepository
import com.agpeya.app.model.ExaminationContent
import com.agpeya.app.model.JournalKind
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.journal.JournalLockGate
import com.agpeya.app.ui.journal.SecureScreen
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * የንስሐ ዝግጅት — a guided examination of conscience.
 *
 * The questions are read, not answered in the app: each section offers one
 * optional note field, and the whole walk produces a single ንስሐ draft in the
 * journal — lock-gated, never exported, deleted by "ንስሐ ገብቻለሁ". Nothing about
 * the walk is counted, scored, or remembered anywhere else: the examination is
 * before God, and the app only holds the paper.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ConfessionPrepScreen(
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenPenance: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current

    // Confession material: no screenshots, no recents thumbnail, and the same
    // passphrase gate the journal stands behind.
    SecureScreen()
    val locked by JournalLock.isLocked(context).collectAsState(initial = false)
    var unlocked by remember { mutableStateOf(false) }

    val content by produceState(ExaminationContent()) {
        value = NisihaRepository.load(context)
    }

    // 0 = intro, 1..n = sections, n+1 = review.
    var step by rememberSaveable { mutableIntStateOf(0) }
    val notes: SnapshotStateMap<String, String> = rememberSaveable(
        saver = listSaver(
            save = { map -> map.entries.flatMap { listOf(it.key, it.value) } },
            restore = { flat ->
                mutableStateMapOf<String, String>().apply {
                    flat.chunked(2).forEach { if (it.size == 2) put(it[0], it[1]) }
                }
            },
        ),
    ) { mutableStateMapOf() }
    var savedId by rememberSaveable { mutableStateOf<String?>(null) }
    // Each save mints a fresh entry id, so a second tap before the first write
    // lands would leave an orphan draft behind. One walk, one draft.
    var saving by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.confessionPrepTitle, onBack = onBack) },
    ) { inner ->
        if (locked && !unlocked) {
            Column(Modifier.fillMaxSize().padding(inner)) {
                JournalLockGate(s) { unlocked = true }
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.md),
        ) {
            val sections = content.sections
            val reviewStep = sections.size + 1

            when {
                // Kept: the quiet end state, with the two doors onward.
                savedId != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Text(s.confessionPrepSavedTitle, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        s.journalKindConfessionNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Button(onClick = { savedId?.let(onOpenEntry) }) { Text(s.confessionPrepOpenDraft) }
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        s.confessionPrepPenancePrompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedButton(onClick = onOpenPenance) { Text(s.penanceTitle) }
                }

                // No examination bundled yet: the screen exists, and says so.
                sections.isEmpty() -> ComingSoon()

                step == 0 -> {
                    if (content.intro.isNotBlank()) {
                        Text(content.intro, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(Spacing.lg))
                    }
                    Button(onClick = { step = 1 }) { Text(s.confessionPrepStart) }
                }

                step in 1..sections.size -> {
                    val section = sections[step - 1]
                    Text(
                        s.confessionPrepStepOf(step, sections.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(section.title, style = MaterialTheme.typography.titleLarge)
                    if (section.subtitle.isNotBlank()) {
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            section.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(Spacing.md))
                    // Prompts to sit with — deliberately plain text, one after
                    // another, with nothing to tick.
                    for (question in section.questions) {
                        Text(
                            question,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = Spacing.xs),
                        )
                    }
                    Spacer(Modifier.height(Spacing.md))
                    OutlinedTextField(
                        value = notes[section.id].orEmpty(),
                        onValueChange = { notes[section.id] = it },
                        label = { Text(s.confessionPrepNoteHint) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(onClick = { step -= 1 }) { Text(s.back) }
                        Button(onClick = { step += 1 }) { Text(s.continueAction) }
                    }
                }

                // `sections` is empty for the first frames after a rotation or
                // restore, while the asset reloads. Without this guard a
                // restored step would briefly match the review branch and draw
                // the wrong page — with a live save button on it.
                sections.isNotEmpty() && step >= reviewStep -> {
                    Text(s.confessionPrepReviewHeader, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(Spacing.md))
                    val noted = sections.filter { notes[it.id].orEmpty().isNotBlank() }
                    if (noted.isEmpty()) {
                        Text(
                            s.confessionPrepNothingNoted,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        for (section in noted) {
                            SinqCard {
                                Text(section.title, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(Spacing.xs))
                                Text(
                                    notes[section.id].orEmpty().trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Spacer(Modifier.height(Spacing.sm))
                        }
                    }
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        s.journalKindConfessionNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(onClick = { step -= 1 }) { Text(s.back) }
                        Button(
                            enabled = !saving,
                            onClick = {
                                if (saving) return@Button
                                saving = true
                                scope.launch {
                                    val body = NisihaRepository.buildConfessionBody(
                                        sections = sections,
                                        notes = notes,
                                        stamp = s.confessionPrepStamp,
                                    )
                                    val entry = JournalRepository.draft(
                                        context,
                                        kind = JournalKind.CONFESSION_DRAFT,
                                        anchorRoute = "confessionPrep",
                                        anchorLabel = s.confessionPrepTitle,
                                    )
                                    JournalRepository.save(context, entry.copy(body = body))
                                    savedId = entry.id
                                    saving = false
                                }
                            },
                        ) { Text(s.confessionPrepSave) }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.huge))
        }
    }
}
