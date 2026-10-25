package com.sinq.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sinq.app.ui.common.SectionHeader
import com.sinq.app.ui.common.SinqCard
import com.sinq.app.ui.common.SinqTopBar
import com.sinq.app.ui.reading.geezNumeral
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.IconSize
import com.sinq.app.ui.theme.Spacing
import com.sinq.app.ui.theme.inReadingFont

/**
 * ፹፩ዱ ቅዱሳት መጻሕፍት (The 81 Books of the Ethiopian Orthodox Tewahedo Church).
 *
 * Distinctly organizes the canon into Old Testament (46 books), New Testament
 * (35 books), and Davidic Psalter with Canticles, honoring the broader canon
 * (Enoch, Jubilees, Meqabyan 1–3, Clement, and Didascalia).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptureHubScreen(
    onSearch: () -> Unit,
    onBack: () -> Unit,
    onOpenOldTestament: () -> Unit,
    onOpenNewTestament: () -> Unit,
    onOpenPsalms: () -> Unit,
) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val booksLoad = com.sinq.app.ui.common.rememberContentLoad { com.sinq.app.data.ScriptureRepository.books(context) }
    val books = booksLoad.value.orEmpty()
    val otCount = books.count { canonTestament(it) == "old" }.takeIf { it > 0 } ?: 39
    val ntCount = books.count { canonTestament(it) == "new" }.takeIf { it > 0 } ?: 27
    val totalCount = otCount + ntCount

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.scripturesTitle,
                subtitle = if (s.isAmharic) "${geezNumeral(totalCount)}ቱ ቅዱሳት መጻሕፍት" else "$totalCount Books of Holy Scripture",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = s.tabSearch,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                SectionHeader(
                    text = if (s.isAmharic) "መጽሐፍ ቅዱስ" else s.bibleTitle,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
            }

            // ── ብሉይ ኪዳን (Old Testament) ──────────────────────────────────────────
            item {
                ScriptureCodexCard(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    countGe = geezNumeral(otCount),
                    countLabel = if (s.isAmharic) "${geezNumeral(otCount)} መጻሕፍት" else "$otCount Books",
                    title = s.oldTestamentLabel,
                    subtitle = if (s.isAmharic) {
                        "ኦሪት፣ የታሪክ፣ የጥበብና የትንቢት መጻሕፍት"
                    } else {
                        "Law, History, Poetry & Prophets"
                    },
                    editionTag = "${s.langAmharic} 1962",
                    onClick = onOpenOldTestament,
                )
            }

            // ── ሐዲስ ኪዳን (New Testament) ──────────────────────────────────────────
            item {
                ScriptureCodexCard(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    countGe = geezNumeral(ntCount),
                    countLabel = if (s.isAmharic) "${geezNumeral(ntCount)} መጻሕፍት" else "$ntCount Books",
                    title = s.newTestamentLabel,
                    subtitle = if (s.isAmharic) {
                        "፬ቱ ወንጌላት፣ የሐዋርያት ሥራ፣ መልእክታትና ራእይ"
                    } else {
                        "Four Gospels, Acts, Epistles & Revelation"
                    },
                    editionTag = "${s.langAmharic} 1962",
                    onClick = onOpenNewTestament,
                )
            }

            item {
                Spacer(Modifier.height(Spacing.sm))
                SectionHeader(
                    text = if (s.isAmharic) "መዝሙረ ዳዊትና ነቢያት" else s.psalterTitle,
                    modifier = Modifier.padding(bottom = Spacing.xs),
                )
            }

            // ── መዝሙረ ዳዊት (150 Psalms + 15 Canticles) ──────────────────────
            item {
                ScriptureCodexCard(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    countGe = "፻፶",
                    countLabel = if (s.isAmharic) "፻፶ መዝሙራት" else "150 Psalms",
                    title = s.psalterTitle,
                    subtitle = if (s.isAmharic) {
                        "፻፶ መዝሙራትና ፲፭ቱ የነቢያት ጸሎት"
                    } else {
                        "150 Psalms & 15 Biblical Canticles"
                    },
                    editionTag = "${s.wudaseLangAmharic} 1980 · ${s.wudaseLangGeez} 1980",
                    onClick = onOpenPsalms,
                )
            }

            // ── Canonical Tradition Summary Note ─────────────────────────────
            item {
                Spacer(Modifier.height(Spacing.sm))
                CanonNoticeCard(isAmharic = s.isAmharic)
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
private fun ScriptureCodexCard(
    icon: ImageVector,
    countGe: String,
    countLabel: String,
    title: String,
    subtitle: String,
    editionTag: String,
    onClick: () -> Unit,
) {
    SinqCard(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Ge'ez Numeral Codex Emblem
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = countGe,
                    style = MaterialTheme.typography.titleMedium.inReadingFont(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.inReadingFont(),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.labelSmall.inReadingFont(),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.inReadingFont(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = editionTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(IconSize.small),
            )
        }
    }
}

@Composable
private fun CanonNoticeCard(isAmharic: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "✝",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = if (isAmharic) {
                    "የመጽሐፍ ቅዱስ ጽሑፍ ከ፲፱፻፶፬ቱ የአማርኛ ቅጂ የተዘጋጀ ሲሆን፣ ፴፱ የብሉይ ኪዳን እና ፳፯ የሐዲስ ኪዳን በድምሩ ፷፮ መጻሕፍትን ያካትታል። መዝሙረ ዳዊት ደግሞ ከቀደመው የ፹፩ዱ ቀኖና ተጠብቆ ይገኛል።"
                } else {
                    "Holy Scripture is drawn from the 1962 publication, comprising 66 books: 39 books of the Old Testament and 27 of the New Testament. The Psalter is preserved from the traditional LXX canon."
                },
                style = MaterialTheme.typography.bodySmall.inReadingFont(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.25f,
            )
        }
    }
}
