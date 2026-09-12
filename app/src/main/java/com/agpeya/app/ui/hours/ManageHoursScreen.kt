package com.agpeya.app.ui.hours

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.HoursRepository
import com.agpeya.app.data.ModesRepository
import com.agpeya.app.model.Hour
import com.agpeya.app.model.HoursConfig
import com.agpeya.app.model.ModesState
import com.agpeya.app.model.ReminderEntry
import com.agpeya.app.reminders.ReminderScheduler
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.modes.EntryEditor
import com.agpeya.app.ui.modes.daysSummary
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ሰዓታት — every hour of the day, and everything about it, on one page.
 *
 * This used to be two screens in two branches of Settings. Whether an hour
 * existed at all was set here, under ጸሎት; what time it rang was set four taps
 * away under ማንቂያዎች, beneath a heading about sound. Nothing linked them, and
 * the second silently dropped any hour the first had hidden — so an hour could
 * be switched on in one place and be incapable of ringing because of the other.
 *
 * Now a row is the whole hour: the time it rings, its name, the days it keeps,
 * and one switch. Everything rarer — rename, hide, reorder, delete — is behind
 * the row's own menu, where it does not compete with the two things a reader
 * actually came to change.
 *
 * The times belong to the active mode. Modes still exist for anyone keeping
 * more than one rule of prayer, and the row at the top says which is in force
 * and opens them; but a reader who never thinks about modes never has to.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageHoursScreen(
    onBack: () -> Unit,
    onEditHour: (String) -> Unit,
    onOpenModes: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val config by HoursRepository.config(context).collectAsState(initial = HoursConfig())
    val modes by ModesRepository.state(context)
        .collectAsState(initial = ModesState(ModesRepository.BUILT_IN_ID, emptyList()))
    val builtIn by produceState(emptyList<Hour>()) { value = ContentRepository.hours(context) }
    val hours = remember(builtIn, config) { HoursRepository.merge(builtIn, config, includeHidden = true) }
    val active = modes.activeMode

    var renaming by remember { mutableStateOf<Hour?>(null) }
    var creating by remember { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf<Pair<Hour, ReminderEntry>?>(null) }

    fun reschedule() {
        scope.launch {
            runCatching {
                val names = HoursRepository.visibleHours(context).associate { it.id to it.name }
                ReminderScheduler.rescheduleAll(context, names)
            }
        }
    }

    /** The active mode's entry for an hour, or a fresh one at a sensible time. */
    fun entryFor(hour: Hour): ReminderEntry =
        active?.entries?.firstOrNull { it.hourId == hour.id }
            ?: ReminderEntry(
                id = UUID.randomUUID().toString(),
                hourId = hour.id,
                hour = 6,
                minute = 0,
            )

    fun save(entry: ReminderEntry) {
        val modeId = active?.id ?: return
        scope.launch {
            ModesRepository.upsertEntry(context, modeId, entry)
            reschedule()
        }
    }

    fun move(from: Int, to: Int) {
        val ids = hours.map { it.id }.toMutableList().apply { add(to, removeAt(from)) }
        scope.launch { HoursRepository.setOrder(context, ids) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.manageHours, onBack = onBack) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                Text(
                    s.manageHoursIntro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(Spacing.sm))
                // Which rule of prayer these times belong to. Quiet unless the
                // reader keeps more than one, and never in the way if not.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenModes)
                        .padding(horizontal = 8.dp, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${s.modesTitle} · ${active?.name.orEmpty()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            items(hours.size, key = { hours[it].id }) { index ->
                val hour = hours[index]
                val hidden = hour.id in config.hidden
                val entry = active?.entries?.firstOrNull { it.hourId == hour.id }
                HourRow(
                    hour = hour,
                    hidden = hidden,
                    entry = entry,
                    isCustom = hour.id.startsWith("custom_"),
                    onOpenTime = { editingTime = hour to entryFor(hour) },
                    onOpenSections = { onEditHour(hour.id) },
                    onToggleRing = { on -> save(entryFor(hour).copy(enabled = on)) },
                    onToggleHidden = {
                        scope.launch {
                            HoursRepository.setHidden(context, hour.id, !hidden)
                            reschedule()
                        }
                    },
                    onRename = { renaming = hour },
                    onDelete = {
                        scope.launch {
                            HoursRepository.deleteCustomHour(context, hour.id)
                            reschedule()
                        }
                    },
                    onMoveUp = { move(index, index - 1) }.takeIf { index > 0 },
                    onMoveDown = { move(index, index + 1) }.takeIf { index < hours.size - 1 },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item {
                Spacer(Modifier.height(Spacing.md))
                TextButton(onClick = { creating = true }) { Text("＋ ${s.newHour}") }
                Spacer(Modifier.height(Spacing.huge))
            }
        }
    }

    editingTime?.let { (hour, entry) ->
        ModalBottomSheet(onDismissRequest = { editingTime = null }) {
            Text(
                hour.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = Spacing.sm),
            )
            EntryEditor(
                entry = entry,
                hours = hours,
                // The hour is the row that was tapped; offering a picker here
                // would let a row edit some other row's time.
                canPickHour = false,
                canDelete = false,
                onSave = { save(it.copy(enabled = true)); editingTime = null },
                onDelete = { editingTime = null },
            )
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
                creating = false
                scope.launch {
                    val hour = HoursRepository.addCustomHour(context, name)
                    onEditHour(hour.id)
                }
            },
            onDismiss = { creating = false },
        )
    }
}

/**
 * One hour, whole: when it rings, what it is called, and whether it rings.
 *
 * The four icon buttons this row used to carry — hide, rename, up, down — are
 * behind the menu now. They are things a reader does once, and they were
 * crowding out the time, which is the thing they came for.
 */
@Composable
private fun HourRow(
    hour: Hour,
    hidden: Boolean,
    entry: ReminderEntry?,
    isCustom: Boolean,
    onOpenTime: () -> Unit,
    onOpenSections: () -> Unit,
    onToggleRing: (Boolean) -> Unit,
    onToggleHidden: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
) {
    val s = LocalStrings.current
    var menuOpen by remember { mutableStateOf(false) }
    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The time, and the tap that changes it. A hidden hour shows none: it
        // is out of the day altogether, and a time would be a promise the
        // scheduler does not keep.
        Text(
            text = if (hidden) "—" else entry?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "--:--",
            style = MaterialTheme.typography.titleMedium,
            color = if (hidden || entry?.enabled != true) dim else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .width(62.dp)
                .clickable(enabled = !hidden, onClick = onOpenTime)
                .padding(vertical = 10.dp),
        )
        Column(
            Modifier
                .weight(1f)
                .clickable(onClick = onOpenSections)
                .padding(vertical = 8.dp),
        ) {
            Text(
                hour.name,
                style = MaterialTheme.typography.titleSmall,
                color = if (hidden) dim else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                when {
                    hidden -> s.hiddenHourNote
                    entry == null -> s.daysSummaryDaily
                    else -> daysSummary(entry.days, s.dayLabels, s.daysSummaryDaily, s.noDaySelected)
                },
                style = MaterialTheme.typography.labelSmall,
                color = dim,
            )
        }
        Switch(
            checked = !hidden && entry?.enabled == true,
            onCheckedChange = onToggleRing,
            enabled = !hidden,
        )
        androidx.compose.foundation.layout.Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = s.moreActions, modifier = Modifier.size(IconSize.medium))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (hidden) s.showSection else s.hideSection) },
                    leadingIcon = {
                        Icon(if (hidden) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff, null)
                    },
                    onClick = { menuOpen = false; onToggleHidden() },
                )
                DropdownMenuItem(
                    text = { Text(s.rename) },
                    leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                    onClick = { menuOpen = false; onRename() },
                )
                DropdownMenuItem(
                    text = { Text(s.moveUp) },
                    leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, null) },
                    enabled = onMoveUp != null,
                    onClick = { menuOpen = false; onMoveUp?.invoke() },
                )
                DropdownMenuItem(
                    text = { Text(s.moveDown) },
                    leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, null) },
                    enabled = onMoveDown != null,
                    onClick = { menuOpen = false; onMoveDown?.invoke() },
                )
                if (isCustom) {
                    DropdownMenuItem(
                        text = { Text(s.remove, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Close, null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
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
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank(),
            ) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancel) } },
    )
}
