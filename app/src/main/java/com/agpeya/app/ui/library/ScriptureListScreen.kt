package com.agpeya.app.ui.library

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agpeya.app.data.ScriptureRepository
import com.agpeya.app.model.ScriptureBookMeta
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.theme.Spacing

/**
 * Where each book sits in the Church's own catalogue.
 *
 * `canon.json` comes from the upstream Bible data, which files the books
 * beyond the Hebrew thirty-nine as one undifferentiated "deuterocanonical"
 * block and leaves the books of church order with no section at all. The EOTC
 * has no such block: ፍትሐ ነገሥት አንቀጽ ፪ enumerates forty-six ብሉይ ኪዳን and
 * thirty-five ሐዲስ ኪዳን, and every one of these books is counted inside a
 * section that already exists here — several of them inside another book.
 *
 * - ኩፋሌ is counted as one with ኦሪት ዘፍጥረት, and ሄኖክ is read beside it.
 * - The seventeen histories run "from ኢያሱ ወልደ ነዌ to ዜና አይሁድ", which is why
 *   ዮሴፍ ወልደ ኮርዮን is here and not among the books of order, and why that
 *   group is eight books rather than nine.
 * - ባሮክ, ተረፈ ኤርምያስ and the letter are counted inside ትንቢተ ኤርምያስ; ሶስና,
 *   ሠለስቱ ደቂቅ and ተረፈ ዳንኤል inside ትንቢተ ዳንኤል.
 * - ወደ ዕብራውያን is the first of the fourteen Pauline epistles, not a catholic
 *   one; the upstream catalogue follows the Protestant ordering there.
 *
 * Corrected here rather than in the asset, which `tools/extract_bible_editions.py`
 * copies wholesale from the upstream repository on every rebuild.
 */
private val EOTC_SECTION: Map<String, String> = buildMap {
    listOf("kufale", "enoch").forEach { put(it, "Pentateuch") }
    listOf(
        "tobit", "yodit", "esther-greek",
        "1-maccabees", "2-maccabees", "3-maccabees",
        "1-maccabees-greek", "2-maccabees-greek",
        "ezra-sutuel", "ezra-kalie", "1-esdras", "2-esdras",
        JOSIPPON,
    ).forEach { put(it, "Historical") }
    listOf("wisdom-of-solomon", "sirach", "admonition", "prayer-of-manasseh")
        .forEach { put(it, "PoetryWisdom") }
    listOf("baruch", "teref-ermias", "jeremys-letter", "seleste-dekik", "susanna", "teref-daniel")
        .forEach { put(it, "MajorProphet") }
    put("hebrews", "PaulineEpistle")
}

/** ዮሴፍ ወልደ ኮርዮን — ዜና አይሁድ, the last of the ብሉይ ኪዳን histories. */
internal const val JOSIPPON = "josippon"

/**
 * Which testament a book is listed under. Only Josippon moves: the bundle
 * files it after ራእየ ዮሐንስ with the books of order.
 */
internal fun canonTestament(book: ScriptureBookMeta): String =
    if (book.key == JOSIPPON) "old" else book.testament

/**
 * Which heading a book sits under: the Church's own placement where the
 * upstream catalogue differs, the catalogue's otherwise, and the books of
 * church order for what it leaves unplaced.
 */
internal fun canonSectionKey(book: ScriptureBookMeta): String =
    EOTC_SECTION[book.key] ?: book.section.ifBlank { "ChurchOrder" }

/** One testament from the unified Amharic 1980 Bible, grouped by canon section. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptureListScreen(testament: String, onBack: () -> Unit, onOpenBook: (String) -> Unit) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val books by produceState<List<ScriptureBookMeta>>(initialValue = emptyList()) {
        value = ScriptureRepository.books(context)
    }
    // ብሉይ carries the deuterocanon with it — the books are part of this canon,
    // and tagged as their own testament they matched neither list and so had no
    // door at all. Books are already in canonical order, so the groups are too.
    val groups = books
        .filter {
            val t = canonTestament(it)
            t == testament || (testament == "old" && t == "deuterocanonical")
        }
        .groupBy(::canonSectionKey)
        .toList()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.scripturesTitle,
                subtitle = if (testament == "old") s.oldTestamentLabel else s.newTestamentLabel,
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.sm),
        ) {
            groups.forEach { (label, groupBooks) ->
                if (groupBooks.isEmpty()) return@forEach
                item(key = "h_$label") {
                    SectionHeader(
                        text = s.canonSection(label),
                        modifier = Modifier.padding(top = Spacing.xl, bottom = Spacing.xs),
                    )
                }
                items(groupBooks, key = { it.key }) { book ->
                    BookRow(book = book, unit = s.chapterUnit, onClick = { onOpenBook(book.key) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                }
            }
            item { Spacer(Modifier.height(Spacing.screen)) }
        }
    }
}

@Composable
private fun BookRow(book: ScriptureBookMeta, unit: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(book.nameAm, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${geezNumeral(book.chapters)} $unit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
