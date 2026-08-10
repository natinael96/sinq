package com.agpeya.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.ScriptureRepository
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.data.UserDataRepository
import com.agpeya.app.model.Bookmark
import com.agpeya.app.model.ScriptureBook
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.scaledReadingSp
import com.agpeya.app.ui.theme.LocalReadingFont
import com.agpeya.app.ui.theme.readingBodyStyle
import kotlinx.coroutines.launch

private val FONT_STEPS_SP = listOf(17, 19, 22, 25, 29)

/**
 * A New-Testament book reader. Opens at [initialChapter] (and scrolls to
 * [initialStart], tinting [initialStart]..[initialEnd]) when arrived at from a
 * ግጻዌ reading link; otherwise chapter 1. A chapter strip switches chapters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptureReaderScreen(
    bookKey: String,
    initialChapter: Int = 1,
    initialStart: Int = -1,
    initialEnd: Int = -1,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val book by produceState<ScriptureBook?>(initialValue = null, bookKey) {
        value = ScriptureRepository.book(context, bookKey)
    }
    val fontStep by SettingsRepository.fontStep(context).collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]

    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())
    val bookmarkedIds = remember(bookmarks) {
        bookmarks.filter { it.hourId == "scripture_library" }.mapTo(HashSet()) { it.sectionId }
    }

    val b = book ?: run {
        // Keep a back arrow visible: if the book never loads (stale bookmark,
        // bad key), the spinner would otherwise trap the user on this screen.
        Box(Modifier.fillMaxSize()) {
            IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
            }
            androidx.compose.material3.CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }

    // Clamp into the book's real range: a few bundled ግጻዌ citations name chapters
    // that don't exist (e.g. Mark 17). Landing on the nearest real chapter — with
    // the title, chapter strip, and bookmark all agreeing — beats silently showing
    // chapter 1 under the cited number. The highlight only fires when the clamped
    // chapter still equals the cited one, so a bad citation never tints wrong verses.
    var chapter by rememberSaveable(bookKey) {
        mutableIntStateOf(initialChapter.coerceIn(1, b.chapters.size))
    }
    val current = remember(b, chapter) { b.chapters.find { it.chapter == chapter } ?: b.chapters.first() }
    // Only the chapter we arrived on shows the cited-verse tint. The cited range
    // is snapped onto verse numbers that actually exist: the Amharic source merges
    // some verses (so e.g. v17 may not appear) and a few citations run past the
    // chapter's end — both would otherwise scroll somewhere yet highlight nothing.
    val highlightRange = remember(current, chapter) {
        val verses = current.verses
        if (chapter != initialChapter || initialStart <= 0 || verses.isEmpty()) IntRange.EMPTY
        else {
            val lastN = verses.last().n
            val startN = verses.firstOrNull { it.n >= initialStart }?.n ?: lastN
            val endN = (if (initialEnd > 0) initialEnd else initialStart).coerceIn(startN, lastN)
            startN..endN
        }
    }
    // Verses grouped into rows: the cited run becomes one row, everything else
    // stays a row of its own, so the highlight can be drawn around the group.
    val rows = remember(current, highlightRange) {
        buildList {
            val vs = current.verses
            var i = 0
            while (i < vs.size) {
                if (vs[i].n in highlightRange) {
                    var j = i
                    while (j + 1 < vs.size && vs[j + 1].n in highlightRange) j++
                    add(vs.subList(i, j + 1))
                    i = j + 1
                } else {
                    add(listOf(vs[i]))
                    i++
                }
            }
        }
    }
    val listState = rememberLazyListState()

    // Land on the cited verse when opened from a reading link — once. Without the
    // guard, paging away and back to this chapter re-ran the jump mid-reading.
    var landed by rememberSaveable(bookKey) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(current, highlightRange, rows) {
        if (!landed && !highlightRange.isEmpty()) {
            // Index into `rows`, not verses — the cited run collapses into one row.
            val idx = rows.indexOfFirst { row -> row.any { it.n >= highlightRange.first } }
            if (idx >= 0) listState.scrollToItem(idx + 1)   // +1 for the chapter-strip header item
            landed = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(b.nameAm, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${s.chapterUnit} ${geezNumeral(chapter)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
                actions = {
                    val sectionId = "scripture:$bookKey:$chapter"
                    val marked = sectionId in bookmarkedIds
                    IconButton(onClick = {
                        val heading = "${b.nameAm} ${s.chapterUnit} ${geezNumeral(chapter)}"
                        val body = heading + "\n\n" + current.verses.joinToString("\n") {
                            "${geezNumeral(it.n)}  ${it.text}"
                        }
                        com.agpeya.app.ui.common.Sharing.share(context, body, heading)
                    }) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = s.shareAction,
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            UserDataRepository.toggleBookmark(
                                context,
                                Bookmark(
                                    hourId = "scripture_library",
                                    hourName = s.bookmarkGroupScripture,
                                    sectionId = sectionId,
                                    title = "${b.nameAm} ${s.chapterUnit} ${geezNumeral(chapter)}",
                                    route = "scripture/$bookKey/$chapter",
                                ),
                            )
                        }
                    }) {
                        Icon(
                            imageVector = if (marked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (marked) s.removeAction else s.bookmarkAction,
                            tint = if (marked) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    com.agpeya.app.ui.reading.FontSizeActions(
                        fontStep = fontStep,
                        maxStep = FONT_STEPS_SP.lastIndex,
                    ) { step -> scope.launch { SettingsRepository.setFontStep(context, step) } }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 22.dp),
        ) {
            item(key = "chapters") {
                ChapterStrip(
                    count = b.chapters.size,
                    selected = chapter,
                    onSelect = { chapter = it },
                )
                Spacer(Modifier.height(8.dp))
            }
            // The cited verses are emitted as ONE row so the citation reads as a
            // single tinted block instead of a stack of separate boxes.
            items(rows, key = { it.first().n }) { row ->
                val tinted = row.first().n in highlightRange
                val body = @Composable { verse: com.agpeya.app.model.ScriptureVerse ->
                    val annotated = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = scaledReadingSp(bodyFontSp) * 0.58f,
                                baselineShift = BaselineShift.Superscript,
                            )
                        ) { append(geezNumeral(verse.n)) }
                        append("  ")
                        append(verse.text)
                    }
                    Text(
                        text = annotated,
                        style = readingBodyStyle(bodyFontSp),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                if (tinted) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f))
                            .border(1.5.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(10.dp))
                            .padding(vertical = 4.dp),
                    ) { row.forEach { body(it) } }
                } else {
                    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) { row.forEach { body(it) } }
                }
            }
            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}

@Composable
private fun ChapterStrip(count: Int, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(count) { i ->
            val n = i + 1
            val isSel = n == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .then(
                        if (isSel) Modifier.background(MaterialTheme.colorScheme.primary)
                        else Modifier.border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    )
                    .clickable { onSelect(n) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    geezNumeral(n),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
