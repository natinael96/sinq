package com.agpeya.app.ui.customize

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.LayoutRepository
import com.agpeya.app.data.PrayerLayout
import com.agpeya.app.model.Hour
import com.agpeya.app.model.HourLayout
import com.agpeya.app.model.Section
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeHoursScreen(onBack: () -> Unit, onOpenHour: (String) -> Unit) {
    val context = LocalContext.current
    val hours by produceState<List<Hour>>(initialValue = emptyList()) {
        value = ContentRepository.hours(context)
    }
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(s.customizeTitle, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        ) {
            item {
                Text(
                    text = s.customizeIntro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
            }
            items(hours, key = { it.id }) { hour ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenHour(hour.id) }
                        .padding(vertical = 18.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(hour.name, style = MaterialTheme.typography.titleMedium)
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeHourScreen(hourId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hour by produceState<Hour?>(initialValue = null, hourId) {
        value = com.agpeya.app.data.HoursRepository.hourById(context, hourId)
    }
    val layout by LayoutRepository.layout(context, hourId).collectAsState(initial = HourLayout())
    val ordered by produceState(emptyList<Section>(), hour, layout) {
        val h = hour
        value = if (h == null) emptyList()
        else {
            val extras = layout.added.mapNotNull { ContentRepository.psalm(context, it) }
            PrayerLayout.ordered(h.sections, extras, layout)
        }
    }
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    var showPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    fun persistOrder(list: List<Section>) {
        scope.launch { LayoutRepository.setOrder(context, hourId, list.map { it.id }) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(hour?.name ?: "", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                actions = {
                    TextButton(onClick = { scope.launch { LayoutRepository.reset(context, hourId) } }) {
                        Text(s.resetLayout)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            items(ordered.size, key = { ordered[it].id }) { index ->
                val section = ordered[index]
                val hidden = section.id in layout.hidden
                val added = section.number != null && section.number in layout.added
                SectionEditRow(
                    section = section,
                    hidden = hidden,
                    canMoveUp = index > 0,
                    canMoveDown = index < ordered.size - 1,
                    onToggleHidden = {
                        scope.launch { LayoutRepository.toggleHidden(context, hourId, section.id) }
                    },
                    onMoveUp = {
                        val list = ordered.toMutableList().apply { add(index - 1, removeAt(index)) }
                        persistOrder(list)
                    },
                    onMoveDown = {
                        val list = ordered.toMutableList().apply { add(index + 1, removeAt(index)) }
                        persistOrder(list)
                    },
                    onRemove = if (added) {
                        { scope.launch { LayoutRepository.removePsalm(context, hourId, section.number!!) } }
                    } else null,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
            item {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { showPicker = true }) { Text("＋ ${s.addPsalm}") }
            }
        }
    }

    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }, sheetState = sheetState) {
            PsalmPicker(
                title = s.choosePsalm,
                onPick = { number ->
                    scope.launch {
                        LayoutRepository.addPsalm(context, hourId, number)
                        sheetState.hide()
                        showPicker = false
                    }
                },
            )
        }
    }
}

@Composable
private fun PsalmPicker(title: String, onPick: (Int) -> Unit) {
    val context = LocalContext.current
    val psalms by produceState(emptyList<Section>()) { value = ContentRepository.psalter(context) }
    var query by remember { mutableStateOf("") }
    val digits = query.filter { it.isDigit() }
    val filtered = if (digits.isEmpty()) psalms
    else psalms.filter { it.number.toString().startsWith(digits) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("1 – 150") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(filtered.size, key = { filtered[it].id }) { i ->
                val p = filtered[i]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { p.number?.let(onPick) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(p.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        p.subtitle?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        text = p.number?.toString().orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SectionEditRow(
    section: Section,
    hidden: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleHidden: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    val s = com.agpeya.app.ui.strings.LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleHidden) {
            Icon(
                imageVector = if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (hidden) s.showSection else s.hideSection,
                tint = if (hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (hidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer(alpha = if (hidden) 0.5f else 1f),
            )
            section.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer(alpha = if (hidden) 0.5f else 1f),
                )
            }
        }
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = s.moveUp,
                modifier = Modifier.size(20.dp),
                tint = if (canMoveUp) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = s.moveDown,
                modifier = Modifier.size(20.dp),
                tint = if (canMoveDown) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = s.remove,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
