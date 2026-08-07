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
import com.agpeya.app.model.ScriptureBook
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Abyssinica

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
    val book by produceState<ScriptureBook?>(initialValue = null, bookKey) {
        value = ScriptureRepository.book(context, bookKey)
    }
    val fontStep by SettingsRepository.fontStep(context).collectAsState(initial = SettingsRepository.DEFAULT_FONT_STEP)
    val bodyFontSp = FONT_STEPS_SP[fontStep.coerceIn(0, FONT_STEPS_SP.lastIndex)]

    val b = book ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        }
        return
    }

    var chapter by rememberSaveable(bookKey) { mutableIntStateOf(initialChapter.coerceAtLeast(1)) }
    // Only the chapter we arrived on shows the cited-verse tint.
    val highlightRange = remember(chapter) {
        if (chapter == initialChapter && initialStart > 0) initialStart..(if (initialEnd > 0) initialEnd else initialStart)
        else IntRange.EMPTY
    }
    val current = remember(b, chapter) { b.chapters.find { it.chapter == chapter } ?: b.chapters.first() }
    val listState = rememberLazyListState()

    // Land on the cited verse when opened from a reading link.
    androidx.compose.runtime.LaunchedEffect(current, highlightRange) {
        if (!highlightRange.isEmpty()) {
            val idx = current.verses.indexOfFirst { it.n >= highlightRange.first }
            if (idx >= 0) listState.scrollToItem(idx + 1)   // +1 for the chapter-strip header item
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
            items(current.verses, key = { it.n }) { verse ->
                val tinted = verse.n in highlightRange
                val annotated = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = (bodyFontSp * 0.58f).sp,
                            baselineShift = BaselineShift.Superscript,
                        )
                    ) { append(geezNumeral(verse.n)) }
                    append("  ")
                    append(verse.text)
                }
                Text(
                    text = annotated,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Abyssinica,
                        fontSize = bodyFontSp.sp,
                        lineHeight = (bodyFontSp * 1.85f).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (tinted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                            else androidx.compose.ui.graphics.Color.Transparent,
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
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
