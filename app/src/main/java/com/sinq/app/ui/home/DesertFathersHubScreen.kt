package com.sinq.app.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinq.app.data.DailyQuoteRepository
import com.sinq.app.data.UserDataRepository
import com.sinq.app.model.DailyQuote
import com.sinq.app.model.DesertFatherGlossaryTerm
import com.sinq.app.model.toBookmark
import com.sinq.app.ui.common.SinqDivider
import com.sinq.app.ui.common.SinqTopBar
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.IconSize
import com.sinq.app.ui.theme.Spacing
import com.sinq.app.ui.theme.inReadingFont
import kotlinx.coroutines.launch

private data class ProminentFather(
    val nameAm: String,
    val nameEn: String,
)

private val PROMINENT_FATHERS = listOf(
    ProminentFather("አባ ፖይመን", "Abba Poemen"),
    ProminentFather("አባ መቃርስ ዓቢይ", "Abba Macarius"),
    ProminentFather("አባ እንጦንስ ዓቢይ", "Abba Antony"),
    ProminentFather("አባ ሙሴ ፀሊም", "Abba Moses"),
    ProminentFather("አባ አርሳኒዎስ", "Abba Arsenius"),
    ProminentFather("አባ ዮሐንስ ሐጺር", "Abba John the Dwarf"),
    ProminentFather("አባ ሲሶይ", "Abba Sisoes"),
    ProminentFather("አባ አጋቶን", "Abba Agathon"),
    ProminentFather("እመ አብርሃ ስንክሊቲካ", "Amma Syncletica"),
    ProminentFather("ቅዱስ ባስልዮስ ዓቢይ", "Basil the Great"),
    ProminentFather("አባ ቢሳርዮን", "Abba Bessarion"),
    ProminentFather("አባ ዳንኤል", "Abba Daniel"),
)

/**
 * Builds rich annotated string where {word explanation} tags are formatted as
 * graceful italicized monastic glosses.
 */
@Composable
fun rememberAnnotatedQuote(
    rawQuote: String,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    glossColor: Color = MaterialTheme.colorScheme.secondary,
): AnnotatedString = remember(rawQuote, textColor, glossColor) {
    buildAnnotatedString {
        append("«")
        val pattern = Regex("""\{([^}]+)\}""")
        var lastIndex = 0
        for (match in pattern.findAll(rawQuote)) {
            val range = match.range
            if (range.first > lastIndex) {
                withStyle(SpanStyle(color = textColor)) {
                    append(rawQuote.substring(lastIndex, range.first).trimEnd())
                }
            }
            append(" ")
            withStyle(
                SpanStyle(
                    color = glossColor,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            ) {
                append("[${match.groupValues[1].trim()}]")
            }
            append(" ")
            lastIndex = range.last + 1
        }
        if (lastIndex < rawQuote.length) {
            withStyle(SpanStyle(color = textColor)) {
                append(rawQuote.substring(lastIndex).trimStart())
            }
        }
        append("»")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesertFathersHubScreen(
    onBack: () -> Unit,
    onReflectInJournal: (DailyQuote) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = LocalStrings.current
    val isAmharic = s.isAmharic
    val gold = MaterialTheme.colorScheme.secondary
    val keyboard = LocalSoftwareKeyboardController.current

    val quotesLoad = com.sinq.app.ui.common.rememberContentLoad {
        DailyQuoteRepository.quotes(context)
    }
    val allQuotes = quotesLoad.value.orEmpty()
    val bookmarks by UserDataRepository.bookmarks(context).collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFather by remember { mutableStateOf<ProminentFather?>(null) }
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var onlyBookmarked by remember { mutableStateOf(false) }
    var showGlossarySheet by remember { mutableStateOf(false) }

    // Distinct Greek Sections across the dataset
    val sections = remember(allQuotes) {
        allQuotes.mapNotNull { it.section }.distinct()
    }

    val filteredQuotes = remember(allQuotes, searchQuery, selectedFather, selectedSection, onlyBookmarked, bookmarks) {
        val query = searchQuery.trim().lowercase()
        allQuotes.filter { quote ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                quote.authorEn.lowercase().contains(query) ||
                    quote.authorAm.contains(query) ||
                    quote.quote.lowercase().contains(query)
            }

            val matchesFather = if (selectedFather == null) {
                true
            } else {
                quote.authorAm.contains(selectedFather!!.nameAm) ||
                    quote.authorEn.contains(selectedFather!!.nameEn, ignoreCase = true)
            }

            val matchesSection = if (selectedSection == null) {
                true
            } else {
                quote.section.equals(selectedSection, ignoreCase = true)
            }

            val matchesBookmark = if (!onlyBookmarked) {
                true
            } else {
                bookmarks.any { it.hourId == "daily_quote" && it.sectionId == quote.quoteId }
            }

            matchesQuery && matchesFather && matchesSection && matchesBookmark
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.desertFathersHubTitle,
                subtitle = s.desertFathersHubSubtitle,
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { showGlossarySheet = true },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoStories,
                            contentDescription = s.monasticGlossaryTitle,
                            tint = gold,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .imePadding(),
                contentPadding = PaddingValues(bottom = Spacing.xxl),
            ) {
                // ── Header Controls: Search & Monastic Filter Chips ──────────────
                item(key = "header_controls") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screen)
                            .padding(top = Spacing.xs, bottom = Spacing.sm),
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(s.searchHint) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            trailingIcon = if (searchQuery.isNotBlank()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Filled.Clear,
                                            contentDescription = s.cancel,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            } else null,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )

                        Spacer(Modifier.height(Spacing.sm))

                        // Horizontal Filter Chips: All, Saved, Prominent Fathers
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedFather == null && selectedSection == null && !onlyBookmarked,
                                    onClick = {
                                        selectedFather = null
                                        selectedSection = null
                                        onlyBookmarked = false
                                    },
                                    label = { Text(if (isAmharic) "ሁሉም" else "All") },
                                )
                            }
                            item {
                                FilterChip(
                                    selected = onlyBookmarked,
                                    onClick = {
                                        onlyBookmarked = !onlyBookmarked
                                        if (onlyBookmarked) {
                                            selectedFather = null
                                            selectedSection = null
                                        }
                                    },
                                    label = { Text(if (isAmharic) "★ የተቀመጡ" else "★ Saved") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = gold.copy(alpha = 0.16f),
                                        selectedLabelColor = gold,
                                    ),
                                )
                            }
                            items(PROMINENT_FATHERS, key = { it.nameAm }) { father ->
                                val isSelected = selectedFather == father
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedFather = if (isSelected) null else father
                                        if (selectedFather != null) {
                                            onlyBookmarked = false
                                            selectedSection = null
                                        }
                                    },
                                    label = { Text(if (isAmharic) father.nameAm else father.nameEn) },
                                )
                            }
                        }

                        // Secondary Row: Greek Sections (Alpha to Omega)
                        if (sections.isNotEmpty()) {
                            Spacer(Modifier.height(Spacing.xxs))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
                            ) {
                                items(sections, key = { it }) { sec ->
                                    val isSecSelected = selectedSection == sec
                                    FilterChip(
                                        selected = isSecSelected,
                                        onClick = {
                                            selectedSection = if (isSecSelected) null else sec
                                            if (selectedSection != null) {
                                                selectedFather = null
                                                onlyBookmarked = false
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = sec,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    SinqDivider()
                }

                // ── Empty State ──────────────────────────────────────────────────
                if (filteredQuotes.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.xxl),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = s.noResults,
                                style = MaterialTheme.typography.bodyMedium.inReadingFont(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // ── Manuscript Feed ──────────────────────────────────────────────
                itemsIndexed(filteredQuotes, key = { _, quote -> quote.quoteId }) { _, quote ->
                    val author = if (isAmharic) quote.authorAm.ifBlank { quote.authorEn } else quote.authorEn
                    val isBookmarked = bookmarks.any { it.hourId == "daily_quote" && it.sectionId == quote.quoteId }
                    val annotatedQuote = rememberAnnotatedQuote(
                        rawQuote = quote.quote.trim(),
                        textColor = MaterialTheme.colorScheme.onBackground,
                        glossColor = gold,
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screen, vertical = Spacing.md),
                    ) {
                        // Header Row: Monastic cross + Father name + Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = "☩",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = gold,
                                )
                                Spacer(Modifier.width(Spacing.xs))
                                Text(
                                    text = author,
                                    style = MaterialTheme.typography.titleSmall.inReadingFont(),
                                    fontWeight = FontWeight.Bold,
                                    color = gold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            // Badges: Saying number & Day
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                if (!quote.sayingNumberGeez.isNullOrBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = gold.copy(alpha = 0.12f),
                                    ) {
                                        Text(
                                            text = if (isAmharic) "ምክር ${quote.sayingNumberGeez}" else "Saying #${quote.sayingNumber ?: quote.sayingNumberGeez}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = gold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }

                                if (quote.dayGeez.isNotBlank()) {
                                    Text(
                                        text = if (isAmharic) "ቀን ${quote.dayGeez}" else "Day ${quote.day}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(Spacing.xs))

                        // Quote text in sacred reading font with illuminated glosses
                        Text(
                            text = annotatedQuote,
                            style = MaterialTheme.typography.bodyLarge.inReadingFont(),
                            lineHeight = 24.sp,
                        )

                        Spacer(Modifier.height(Spacing.xs))

                        // Liturgical Action Dock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val subtitleText = quote.sectionGeez ?: quote.section ?: "Apophthegmata Patrum"
                            Text(
                                text = "Apophthegmata Patrum · $subtitleText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
                            ) {
                                // Bookmark Button
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            UserDataRepository.toggleBookmark(
                                                context,
                                                quote.toBookmark(s.dailyQuoteChannelName),
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = s.bookmarkAction,
                                        tint = if (isBookmarked) gold else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(IconSize.small),
                                    )
                                }

                                // Share Button
                                IconButton(
                                    onClick = {
                                        val sayingLabel = if (!quote.sayingNumberGeez.isNullOrBlank()) " (#${quote.sayingNumber ?: quote.sayingNumberGeez})" else ""
                                        val shareText = "«${quote.quote.trim()}»\n— $author$sayingLabel (Apophthegmata Patrum)\n\nስንቅ (Sinq)"
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, s.shareAction))
                                    },
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = s.shareAction,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(IconSize.small),
                                    )
                                }

                                // Copy Button
                                IconButton(
                                    onClick = {
                                        val clip = ClipData.newPlainText(
                                            "Desert Father Quote",
                                            "«${quote.quote.trim()}»\n— $author",
                                        )
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(clip)
                                        Toast.makeText(context, s.copyAction, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = s.copyAction,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(IconSize.small),
                                    )
                                }

                                // Reflect in Journal
                                IconButton(
                                    onClick = { onReflectInJournal(quote) },
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.EditNote,
                                        contentDescription = s.noteAction,
                                        tint = gold,
                                        modifier = Modifier.size(IconSize.small),
                                    )
                                }
                            }
                        }
                    }
                    SinqDivider()
                }
            }
        }
    }

    // ── Monastic Glossary Bottom Sheet ───────────────────────────────────────────
    if (showGlossarySheet) {
        val glossaryTerms = remember { DailyQuoteRepository.glossary(context) }
        MonasticGlossarySheet(
            terms = glossaryTerms,
            isAmharic = isAmharic,
            onDismiss = { showGlossarySheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonasticGlossarySheet(
    terms: List<DesertFatherGlossaryTerm>,
    isAmharic: Boolean,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    val gold = MaterialTheme.colorScheme.secondary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.lg),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.monasticGlossaryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = s.monasticGlossarySubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = gold,
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = s.dismiss,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))
            SinqDivider()
            Spacer(Modifier.height(Spacing.sm))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                terms.forEach { term ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "☩",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = gold,
                                )
                                Spacer(Modifier.width(Spacing.xs))
                                Text(
                                    text = term.term,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = gold,
                                )
                            }
                            Spacer(Modifier.height(Spacing.xxs))
                            if (term.meaningAm.isNotBlank()) {
                                Text(
                                    text = term.meaningAm,
                                    style = MaterialTheme.typography.bodyMedium.inReadingFont(),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp,
                                )
                            }
                            if (term.meaningEn.isNotBlank() && (!isAmharic || term.meaningAm.isBlank())) {
                                Spacer(Modifier.height(Spacing.xxs))
                                Text(
                                    text = term.meaningEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
