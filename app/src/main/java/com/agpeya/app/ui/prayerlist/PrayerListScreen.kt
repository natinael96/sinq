package com.agpeya.app.ui.prayerlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.PrayerListRepository
import com.agpeya.app.model.PrayerPerson
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqDivider
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.common.ToggleRow
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.sinqColors
import kotlinx.coroutines.launch

/**
 * The people the user remembers in prayer — a diptych, read down.
 *
 * It was a two-column grid of cards, which is the one shape a recited list
 * cannot take: reading order zig-zagged across the pair. One column now, one
 * line per name, with the intention beside the name rather than under it so a
 * note costs no extra row.
 *
 * The two groups are the ones the ጸሎተ ማርያም at the foot already prays apart:
 * ሕያዋን and ነፍሳተ ሙታን. A group appears only when it holds someone, so a list of
 * three living people looks like a list of three names.
 *
 * Nothing carries a delete button. A swipe removes, and the snackbar puts the
 * person back where they were — these are the people someone remembers in
 * prayer, and the old ✕ deleted one on a single tap from inside the card's own
 * tap target, with no way back short of a backup file.
 */
@OptIn(ExperimentalMaterial3Api::class) // SinqTopBar wraps material3's TopAppBar
@Composable
fun PrayerListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val people by PrayerListRepository.people(context).collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }

    // Held by id rather than by value so an edit saved from the dialog does not
    // leave a stale copy on screen, and so rotation keeps the dialog open.
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val editing = people.firstOrNull { it.id == editingId }

    /** Remove, then offer the way back for as long as the snackbar stands. */
    fun removeWithUndo(person: PrayerPerson) {
        // Read the position before the removal: undo has to restore where the
        // name sat, not just the name, because the list is recited in order.
        val index = people.indexOfFirst { it.id == person.id }
        scope.launch {
            PrayerListRepository.remove(context, person.id)
            val result = snackbarHostState.showSnackbar(
                message = s.personRemoved(person.name),
                actionLabel = s.undoAction,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                PrayerListRepository.insertAt(context, person, index)
            }
        }
    }

    val living = people.filterNot { it.departed }
    val departed = people.filter { it.departed }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SinqTopBar(
                title = s.prayerListTitle,
                // How many names are being carried, without counting them.
                subtitle = people.takeIf { it.isNotEmpty() }?.let { s.prayerListCount(it.size) },
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
            Box(
                Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = Spacing.screen),
                contentAlignment = Alignment.Center,
            ) {
                StatePanel(
                    title = s.noPrayerListTitle,
                    body = s.noPrayerListBody,
                    icon = Icons.Outlined.VolunteerActivism,
                    actionLabel = s.addPerson,
                    onAction = { adding = true },
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.sm),
        ) {
            nameGroup(s.prayerListLiving, living, ::removeWithUndo) { editingId = it.id }
            nameGroup(s.prayerListDeparted, departed, ::removeWithUndo) { editingId = it.id }

            // One row, not one per group. A name typed here joins the living,
            // which is what almost every addition is; a name is moved to
            // ነፍሳተ ሙታን by opening it, where the intention is written anyway.
            item(key = "add") {
                AddNameRow(
                    onAdd = { name -> scope.launch { PrayerListRepository.add(context, name, "") } },
                )
            }

            item(key = "marian") { MarianConclusion() }
            item(key = "tail") { Spacer(Modifier.height(Spacing.xxl)) }
        }
    }

    if (adding) {
        PersonDialog(
            title = s.addPerson,
            onConfirm = { name, note, isDeparted ->
                scope.launch { PrayerListRepository.add(context, name, note, isDeparted) }
                adding = false
            },
            onDismiss = { adding = false },
        )
    }
    if (editing != null) {
        PersonDialog(
            title = s.editPerson,
            initialName = editing.name,
            initialNote = editing.note,
            initialDeparted = editing.departed,
            onConfirm = { name, note, isDeparted ->
                scope.launch { PrayerListRepository.edit(context, editing.id, name, note, isDeparted) }
                editingId = null
            },
            onRemove = {
                editingId = null
                removeWithUndo(editing)
            },
            onDismiss = { editingId = null },
        )
    }
}

/** A header and its names, or nothing at all when no one is in the group. */
private fun LazyListScope.nameGroup(
    label: String,
    people: List<PrayerPerson>,
    onRemove: (PrayerPerson) -> Unit,
    onEdit: (PrayerPerson) -> Unit,
) {
    if (people.isEmpty()) return
    item(key = "head_$label") {
        val s = LocalStrings.current
        Spacer(Modifier.height(Spacing.md))
        SectionHeader(label) {
            Text(
                s.countMark(people.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(Modifier.height(Spacing.xxs))
    }
    items(people, key = { it.id }) { person ->
        SwipeToRemove(onRemove = { onRemove(person) }) {
            PersonRow(person = person, onClick = { onEdit(person) })
        }
    }
}

/**
 * Swipe a name away, leftwards only.
 *
 * Only from the end: a rightward swipe is the system's back gesture on most
 * phones, and losing a name to a mistaken back is exactly what this screen
 * should not do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToRemove(onRemove: () -> Unit, content: @Composable () -> Unit) {
    val s = LocalStrings.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRemove()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    s.remove,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = Spacing.md),
                )
            }
        },
        content = { content() },
    )
}

/**
 * One name on one line, with its intention beside it.
 *
 * Name and note are a single piece of text so the line truncates as one and the
 * two cannot fight over the width. A long intention is cut here and read in
 * full when the name is opened.
 */
@Composable
private fun PersonRow(person: PrayerPerson, onClick: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                append(person.name)
                person.note.takeIf { it.isNotBlank() }?.let {
                    withStyle(SpanStyle(color = muted)) { append("  ·  $it") }
                }
            },
            style = MaterialTheme.typography.bodyLarge.inReadingFont(),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The fast way in: a name, and nothing else to fill in.
 *
 * Adding used to be one modal round trip per name, which is the wrong shape for
 * the first use of the screen, when several names are entered at once. The field
 * stays open and focused after each name so the next one is just typing.
 */
@Composable
private fun AddNameRow(onAdd: (String) -> Unit) {
    val s = LocalStrings.current
    var open by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    Spacer(Modifier.height(Spacing.sm))
    SinqDivider()
    if (!open) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.small)
                .clickable { open = true }
                .padding(vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                s.addPerson,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        return
    }

    fun commit() {
        val name = draft.trim()
        if (name.isNotEmpty()) {
            onAdd(name)
            draft = ""
        } else {
            open = false
            keyboard?.hide()
        }
    }

    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        singleLine = true,
        label = { Text(s.personNameLabel) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.sm)
            .focusRequester(focus),
    )
    androidx.compose.runtime.LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = { draft = ""; open = false; keyboard?.hide() }) { Text(s.cancel) }
        Spacer(Modifier.width(Spacing.xs))
        TextButton(enabled = draft.isNotBlank(), onClick = { commit() }) { Text(s.save) }
    }
}

private const val MARIAN_INVOCATION = "ጸሎታ ለማርያም ወስእለታ"
private val MARIAN_LINES = listOf(
    "ጸሎታ ለማርያም ወስእለታ ያድነነ እመዓተ ወልዳ።",
    "ጸሎታ ለማርያም ወስእለታ ለርእሰ ሊቃነ ጳጳሳት አባ እገሌ ይዕቀቦ እመዓተ ወልዳ።",
    "ጸሎታ ለማርያም ወስእለታ ለብፁዕ ሊቀ ጳጳስ አባ እገሌ ይዕቀቦ እመዓተ ወልዳ።",
    "ጸሎታ ለማርያም ወስእለታ ለሀገሪትነ ኢትዮጵያ ይዕቀባ እመዓተ ወልዳ።",
    "ጸሎታ ለማርያም ወስእለታ ለሕዝበ ክርስቲያን ይዕቀቦሙ እመዓተ ወልዳ።",
    "ጸሎታ ለማርያም ወስእለታ ለነፍሳተ ሙታን ያድኖን እመዓተ ወልዳ።",
)

/**
 * The prayer said over the names, under a rule rather than inside a card: it is
 * the text of the page, not an object on it.
 *
 * The invocation is [sinqColors.arke], the liturgical red the መልክእ reader,
 * ማኅሌት and ስንክሳር all use. It was `colorScheme.error` — the colour this app
 * tints every remove icon with — which put sacred text in the danger colour.
 */
@Composable
private fun MarianConclusion() {
    val red = sinqColors.arke
    val body = MaterialTheme.colorScheme.onBackground
    Column(Modifier.fillMaxWidth().padding(top = Spacing.xl)) {
        SinqDivider()
        Spacer(Modifier.height(Spacing.md))
        Text(
            "ጸሎተ ማርያም",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(Spacing.sm))
        SelectionContainer {
            Text(
                buildAnnotatedString {
                    MARIAN_LINES.forEachIndexed { index, line ->
                        withStyle(SpanStyle(color = red, fontWeight = FontWeight.SemiBold)) {
                            append(MARIAN_INVOCATION)
                        }
                        withStyle(SpanStyle(color = body)) { append(line.removePrefix(MARIAN_INVOCATION)) }
                        if (index != MARIAN_LINES.lastIndex) append("\n\n")
                    }
                },
                style = MaterialTheme.typography.bodyMedium.inReadingFont().copy(lineHeight = 23.sp),
            )
        }
    }
}

/**
 * One dialog for both add and edit: a name, an optional intention, and which of
 * the two groups the name is prayed in. Editing also offers the way out, since
 * no row carries a delete button any more and a swipe is not something a first
 * visit teaches you.
 */
@Composable
private fun PersonDialog(
    title: String,
    onConfirm: (name: String, note: String, departed: Boolean) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = "",
    initialNote: String = "",
    initialDeparted: Boolean = false,
    onRemove: (() -> Unit)? = null,
) {
    val s = LocalStrings.current
    // Saveable so a rotation mid-entry keeps what was typed; these were plain
    // remember, and turning the phone lost the name and closed the dialog.
    var name by rememberSaveable { mutableStateOf(initialName) }
    var note by rememberSaveable { mutableStateOf(initialNote) }
    var departed by rememberSaveable { mutableStateOf(initialDeparted) }
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
                Spacer(Modifier.height(Spacing.sm))
                ToggleRow(
                    title = s.prayerListDeparted,
                    checked = departed,
                    onCheckedChange = { departed = it },
                )
                if (onRemove != null) {
                    TextButton(onClick = onRemove) {
                        Text(s.remove, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name.trim(), note.trim(), departed) }) {
                Text(s.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
