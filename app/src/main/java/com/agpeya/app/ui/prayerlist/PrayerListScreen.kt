package com.agpeya.app.ui.prayerlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import com.agpeya.app.data.PrayerListRepository
import com.agpeya.app.model.PrayerPerson
import com.agpeya.app.ui.common.ListRow
import com.agpeya.app.ui.common.SinqDivider
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * The people the user remembers in prayer. Deliberately plain: a list of names
 * with an optional intention under each, in the order they were added — no
 * cards, no reordering, no ceremony. Tap a person to change what's written;
 * the ✕ lets them go.
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
        if (people.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Spacer(Modifier.height(Spacing.huge))
                StatePanel(
                    title = s.noPrayerListTitle,
                    body = s.noPrayerListBody,
                    icon = Icons.Outlined.VolunteerActivism,
                    actionLabel = s.addPerson,
                    onAction = { adding = true },
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.sm),
            ) {
                items(people, key = { it.id }) { person ->
                    Column(Modifier.animateItem()) {
                        ListRow(
                            title = person.name,
                            subtitle = person.note.ifBlank { null },
                            onClick = { editing = person },
                        ) {
                            IconButton(onClick = { scope.launch { PrayerListRepository.remove(context, person.id) } }) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = s.remove,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(IconSize.small),
                                )
                            }
                        }
                        if (person.id != people.lastOrNull()?.id) SinqDivider()
                    }
                }
                item { Spacer(Modifier.height(Spacing.xxl)) }
            }
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
