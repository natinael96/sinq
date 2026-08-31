package com.agpeya.app.ui.hours

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.HoursRepository
import com.agpeya.app.model.Hour
import com.agpeya.app.model.HoursConfig
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageHoursScreen(onBack: () -> Unit, onEditHour: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val config by HoursRepository.config(context).collectAsState(initial = HoursConfig())
    val builtIn by produceState(emptyList<Hour>()) { value = ContentRepository.hours(context) }
    val hours = remember(builtIn, config) { HoursRepository.merge(builtIn, config, includeHidden = true) }

    var renaming by remember { mutableStateOf<Hour?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(title = s.manageHours, onBack = onBack)
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
                    s.manageHoursIntro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
            items(hours.size, key = { hours[it].id }) { index ->
                val hour = hours[index]
                val isCustom = hour.id.startsWith("custom_")
                HourManageRow(
                    hour = hour,
                    hidden = hour.id in config.hidden,
                    isCustom = isCustom,
                    onOpen = { onEditHour(hour.id) },
                    onToggleHidden = { scope.launch { HoursRepository.setHidden(context, hour.id, hour.id !in config.hidden) } },
                    onRename = { renaming = hour },
                    onDelete = { scope.launch { HoursRepository.deleteCustomHour(context, hour.id) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { creating = true }) { Text("＋ ${s.newHour}") }
            }
        }
    }

    renaming?.let { hour ->
        NameDialog(
            title = s.rename,
            initial = hour.name,
            confirm = s.save,
            cancel = s.cancel,
            onConfirm = { name ->
                scope.launch { HoursRepository.renameHour(context, hour.id, name) }
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    if (creating) {
        NameDialog(
            title = s.newHour,
            initial = "",
            confirm = s.save,
            cancel = s.cancel,
            onConfirm = { name ->
                scope.launch {
                    val hour = HoursRepository.addCustomHour(context, name)
                    creating = false
                    onEditHour(hour.id)
                }
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun HourManageRow(
    hour: Hour,
    hidden: Boolean,
    isCustom: Boolean,
    onOpen: () -> Unit,
    onToggleHidden: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleHidden) {
            Icon(
                imageVector = if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (hidden) s.showSection else s.hideSection,
                tint = if (hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = hour.name,
            style = MaterialTheme.typography.titleMedium,
            color = if (hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
                .padding(vertical = 14.dp),
        )
        IconButton(onClick = onRename) {
            Icon(Icons.Outlined.Edit, contentDescription = s.rename, modifier = Modifier.size(20.dp))
        }
        if (isCustom) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Close, contentDescription = s.remove, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirm: String,
    cancel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
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
                label = { Text(s.hourNameLabel) },
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim()) }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancel) } },
    )
}
