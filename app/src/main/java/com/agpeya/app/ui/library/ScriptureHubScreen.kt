package com.agpeya.app.ui.library

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
import androidx.compose.material.icons.outlined.LibraryMusic
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont

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
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(
                title = s.scripturesTitle,
                subtitle = if (s.isAmharic) "፹፩ዱ ቅዱሳት መጻሕፍት" else "The 81 Books of Holy Scripture",
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

            // ── ብሉይ ኪዳን (46 Books) ──────────────────────────────────────────
            item {
                ScriptureCodexCard(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    countGe = "፵፮",
                    countLabel = if (s.isAmharic) "፵፮ መጻሕፍት" else "46 Books",
                    title = s.oldTestamentLabel,
                    subtitle = if (s.isAmharic) {
                        "ኦሪት፣ ታሪክ፣ መክብብ፣ ኩፋሌና ሄኖክን ጨምሮ"
                    } else {
                        "Torah, Historical Books, Jubilees & Enoch"
                    },
                    editionTag = "${s.langAmharic} 1980",
                    onClick = onOpenOldTestament,
                )
            }

            // ── ሐዲስ ኪዳን (35 Books) ──────────────────────────────────────────
            item {
                ScriptureCodexCard(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    countGe = "፴፭",
                    countLabel = if (s.isAmharic) "፴፭ መጻሕፍት" else "35 Books",
                    title = s.newTestamentLabel,
                    subtitle = if (s.isAmharic) {
                        "፬ቱ ወንጌላት፣ መልእክታት፣ ራእይና የቀሌምንጦስ መጻሕፍት"
                    } else {
                        "Four Gospels, Epistles, Revelation & Clement"
                    },
                    editionTag = "${s.langAmharic} 1980",
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
                    icon = Icons.Outlined.LibraryMusic,
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
                    )
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
                    "የኢትዮጵያ ኦርቶዶክስ ተዋሕዶ ቤተ ክርስቲያን ቀኖና ፹፩ (81) ቅዱሳት መጻሕፍትን ያካትታል፤ ይህም ፵፮ የብሉይ ኪዳን እና ፴፭ የሐዲስ ኪዳን መጻሕፍት ናቸው።"
                } else {
                    "The Ethiopian Orthodox Tewahedo Church canon comprises 81 canonical books: 46 books of the Old Testament and 35 of the New Testament."
                },
                style = MaterialTheme.typography.bodySmall.inReadingFont(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.25f,
            )
        }
    }
}
