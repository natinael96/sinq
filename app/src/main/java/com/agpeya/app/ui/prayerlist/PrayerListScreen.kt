package com.agpeya.app.ui.prayerlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.PrayerListRepository
import com.agpeya.app.model.PrayerPerson
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import kotlinx.coroutines.launch

/**
 * The people the user remembers in prayer, arranged like a compact diptych.
 * Tap a person to change what's written; the ✕ lets them go.
 */
@OptIn(ExperimentalMaterial3Api::class) // SinqTopBar wraps material3's TopAppBar
@Composable
fun PrayerListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val people by PrayerListRepository.people(context).collectAsState(initial = emptyList())

    var editing by remember { mutableStateOf<PrayerPerson?>(null) }
    var adding by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.prayerListTitle,
                onBack = onBack,
                actions = {
                    IconButton(onClick = { adding = true }) {
                        Icon(
                            Icons.Outlined.PersonAddAlt,
                            contentDescription = s.addPerson,
                            modifier = Modifier.size(IconSize.medium),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 148.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (people.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(Spacing.xl))
                        StatePanel(
                            title = s.noPrayerListTitle,
                            body = s.noPrayerListBody,
                            icon = Icons.Outlined.VolunteerActivism,
                            actionLabel = s.addPerson,
                            onAction = { adding = true },
                        )
                    }
                }
            } else {
                items(people, key = { it.id }) { person ->
                    PrayerPersonCard(
                        person = person,
                        removeLabel = s.remove,
                        onEdit = { editing = person },
                        onRemove = { scope.launch { PrayerListRepository.remove(context, person.id) } },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "marian-conclusion") {
                MarianConclusion()
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(Spacing.xxl)) }
        }
    }

    if (adding) {
        PersonDialog(
            title = s.addPerson,
            onConfirm = { name, note ->
                scope.launch { PrayerListRepository.add(context, name, note) }
                adding = false
            },
            onDismiss = { adding = false },
        )
    }
    val editingPerson = editing
    if (editingPerson != null) {
        PersonDialog(
            title = s.editPerson,
            initialName = editingPerson.name,
            initialNote = editingPerson.note,
            onConfirm = { name, note ->
                scope.launch { PrayerListRepository.edit(context, editingPerson.id, name, note) }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun PrayerPersonCard(
    person: PrayerPerson,
    removeLabel: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onEdit,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Box(Modifier.fillMaxWidth().padding(start = Spacing.md, top = Spacing.md, bottom = Spacing.md, end = Spacing.xs)) {
            Column(Modifier.fillMaxWidth().padding(end = 32.dp)) {
                Text(
                    person.name,
                    style = MaterialTheme.typography.titleSmall.inReadingFont().copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                person.note.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall.inReadingFont().copy(lineHeight = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp).align(Alignment.TopEnd)) {
                Icon(Icons.Outlined.Close, contentDescription = removeLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(IconSize.small))
            }
        }
    }
}

private const val MARIAN_INVOCATION = "ጸሎታ ለማርያም ወስእለታ"
private val MARIAN_LINES = listOf(
    "ጸሎታ ለማርያም ወስእለታ ያድነነ እመዓተ ወልዳ።",
    "ጸሎታ ለማርያም ወስእለታ ለርእሰ ሊቃነ ጠጣሳት አባ እገሌ ይዕቀቦ እመዓተ ወልዳ።",
    "ጸሎታ ለማርያም ወስእለታ ለብፁዕ ሊቀ ጠጣስ አባ እገሌ ይዕቀቦ እመዓተ ወልዳ።",
    "ጸሎታ ለማርያም ወስእለታ ለሀገሪትነ ኢትዮጵያ ይዕቀባ እመዓተ ወልዳ።",
    "ጸሎታ ለማርያም ወስእለታ ለሕዝበ ክርስቲያን ይዕቀቦሙ እመዓተ ወልዳ።",
    "ጸሎታ ለማርያም ወስእለታ ለነፍሳተ ሙታን ያድኖን እመዓተ ወልዳ።",
)

@Composable
private fun MarianConclusion() {
    val accent = MaterialTheme.colorScheme.error
    val body = MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(
                "ጸሎተ ማርያም",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(Spacing.md))
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    buildAnnotatedString {
                        MARIAN_LINES.forEachIndexed { index, line ->
                            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) { append(MARIAN_INVOCATION) }
                            withStyle(SpanStyle(color = body)) { append(line.removePrefix(MARIAN_INVOCATION)) }
                            if (index != MARIAN_LINES.lastIndex) append("\n\n")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium.inReadingFont().copy(lineHeight = 23.sp),
                )
            }
        }
    }
}

/** One dialog for both add and edit: a name, and an optional intention under it. */
@Composable
private fun PersonDialog(
    title: String,
    onConfirm: (name: String, note: String) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = "",
    initialNote: String = "",
) {
    val s = LocalStrings.current
    var name by remember { mutableStateOf(initialName) }
    var note by remember { mutableStateOf(initialNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(s.personNameLabel) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    minLines = 2,
                    maxLines = 4,
                    label = { Text(s.prayerNoteLabel) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name.trim(), note.trim()) }) {
                Text(s.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
