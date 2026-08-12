package com.agpeya.app.ui.habits

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.HabitsRepository
import com.agpeya.app.model.HabitsState
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageHabitsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val state by HabitsRepository.state(context).collectAsState(initial = HabitsState())
    val ids = HabitsRepository.orderedHabitIds(state, includeHidden = true)

    var renamingId by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }

    fun move(from: Int, to: Int) {
        val list = ids.toMutableList().apply { add(to, removeAt(from)) }
        scope.launch { HabitsRepository.setOrder(context, list) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(title = s.manageHabits, onBack = onBack)
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                Text(
                    s.manageHabitsIntro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
            items(ids.size, key = { ids[it] }) { index ->
                val id = ids[index]
                val hidden = id in state.hidden
                val isCustom = id.startsWith("custom_")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { scope.launch { HabitsRepository.setHidden(context, id, id !in state.hidden) } }) {
                        Icon(
                            imageVector = if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (hidden) s.showSection else s.hideSection,
                            tint = if (hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = habitName(id, state, s),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                    )
                    IconButton(onClick = { renamingId = id }) {
                        Icon(Icons.Outlined.Edit, contentDescription = s.rename, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { move(index, index - 1) }, enabled = index > 0) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp, contentDescription = s.moveUp, modifier = Modifier.size(20.dp),
                            tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    IconButton(onClick = { move(index, index + 1) }, enabled = index < ids.size - 1) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown, contentDescription = s.moveDown, modifier = Modifier.size(20.dp),
                            tint = if (index < ids.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    if (isCustom) {
                        IconButton(onClick = { scope.launch { HabitsRepository.deleteCustomHabit(context, id) } }) {
                            Icon(Icons.Outlined.Close, contentDescription = s.remove, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { creating = true }) { Text("＋ ${s.newHabit}") }
            }
        }
    }

    val editingId = renamingId
    if (editingId != null) {
        HabitNameDialog(
            title = s.rename,
            initial = habitName(editingId, state, s),
            onConfirm = { name ->
                scope.launch { HabitsRepository.renameHabit(context, editingId, name) }
                renamingId = null
            },
            onDismiss = { renamingId = null },
        )
    }
    if (creating) {
        HabitNameDialog(
            title = s.newHabit,
            initial = "",
            onConfirm = { name ->
                scope.launch { HabitsRepository.addCustomHabit(context, name) }
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun HabitNameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(s.habitNameLabel) },
            )
        },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim()) }) { Text(s.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
