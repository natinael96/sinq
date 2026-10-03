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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Church
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.ContentRepository
import com.agpeya.app.data.HoursRepository
import com.agpeya.app.model.Hour
import com.agpeya.app.ui.common.EthiopianDate
import com.agpeya.app.ui.common.HeroCard
import com.agpeya.app.ui.common.SectionHeader
import com.agpeya.app.ui.common.SinqCard
import com.agpeya.app.ui.common.rememberCurrentDate
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import com.agpeya.app.ui.theme.sinqColors
import java.time.LocalTime

/**
 * ቤተ መጻሕፍት (The Scriptorium / Liturgical Library).
 *
 * Designed as a sacred illuminated library of the Ethiopian Orthodox Tewahedo Church.
 * Features an active devotion resume hero, the 81-book biblical canon codex strip,
 * canonical hours with live liturgical badges, and direct access to Mahlet, Sinksar,
 * Wudase Maryam, and the Church books.
 */
@Composable
fun LibraryScreen(
    onSearch: () -> Unit,
    onOpenScriptures: () -> Unit,
    onOpenWudase: () -> Unit,
    onOpenBahreHasab: () -> Unit,
    onOpenBooks: () -> Unit,
    onOpenSynaxarium: () -> Unit,
    onOpenReading: () -> Unit,
    onOpenMarks: () -> Unit,
    onOpenMahlets: () -> Unit = {},
    onOpenHour: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val s = LocalStrings.current
    val today by rememberCurrentDate()

    // ── Liturgical Date & Hour Computations ──────────────────────────────
    val ethDate = remember(today) { EthiopianDate.from(today) }
    val ethMonthName = s.ethMonths.getOrElse(ethDate.month - 1) { "" }
    val ethDateLabel = if (ethMonthName.isNotBlank()) {
        "$ethMonthName ${geezNumeral(ethDate.day)}"
    } else {
        "${ethDate.month}/${ethDate.day}"
    }

    val weekdayName = remember(today, s.isAmharic) {
        when (today.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> if (s.isAmharic) "ሰኞ" else "Monday"
            java.time.DayOfWeek.TUESDAY -> if (s.isAmharic) "ማክሰኞ" else "Tuesday"
            java.time.DayOfWeek.WEDNESDAY -> if (s.isAmharic) "ረቡዕ" else "Wednesday"
            java.time.DayOfWeek.THURSDAY -> if (s.isAmharic) "ሐሙስ" else "Thursday"
            java.time.DayOfWeek.FRIDAY -> if (s.isAmharic) "ዓርብ" else "Friday"
            java.time.DayOfWeek.SATURDAY -> if (s.isAmharic) "ቅዳሜ" else "Saturday"
            java.time.DayOfWeek.SUNDAY -> if (s.isAmharic) "እሑድ" else "Sunday"
        }
    }

    val currentHourNow = remember { LocalTime.now().hour }
    val suggestedHourId = remember(currentHourNow) { ContentRepository.suggestedHourId(currentHourNow) }
    val suggestedHourLoad = produceState<Hour?>(initialValue = null, suggestedHourId) {
        value = HoursRepository.hourById(context, suggestedHourId)
    }
    val suggestedHour = suggestedHourLoad.value

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // ── Top Title Bar ─────────────────────────────────────────────
            item {
                Spacer(Modifier.height(Spacing.xl))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = s.libraryTitle,
                            style = MaterialTheme.typography.headlineMedium.inReadingFont(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = s.librarySubtitle,
                            style = MaterialTheme.typography.labelMedium.inReadingFont(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onSearch) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = s.tabSearch,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
            }

            // ── Scriptorium Quick-Resume Active Devotion Hero ──────────────
            item {
                ScriptoriumQuickResumeHero(
                    hour = suggestedHour,
                    hourId = suggestedHourId,
                    onOpenHour = { onOpenHour(suggestedHourId) },
                    onOpenReading = onOpenReading,
                    isAmharic = s.isAmharic,
                )
            }

            // ── ፹፩ዱ ቅዱሳት መጻሕፍት (Canon Codex Strip) ─────────────────────
            item {
                SectionHeader(
                    text = if (s.isAmharic) "፹፩ዱ ቅዱሳት መጻሕፍት" else "The 81 Canonical Books",
                    modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
                )
            }
            item {
                CanonCodexGrid(
                    onOpenScriptures = onOpenScriptures,
                    isAmharic = s.isAmharic,
                )
            }

            // ── ፩. የጸሎትና የማኅሌት መጻሕፍት ───────────────────────────────
            item {
                SectionHeader(
                    text = if (s.isAmharic) "የጸሎትና የማኅሌት መጻሕፍት" else "Liturgical & Prayer Office",
                    modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
                )
            }
            item {
                IlluminatedLibraryCard(
                    icon = Icons.Outlined.NightsStay,
                    title = if (s.isAmharic) "መጽሐፈ ሰዓታት" else "Horologium (Canonical Hours)",
                    subtitle = if (s.isAmharic) "፯ቱ የጸሎት ሰዓታት ከነሥርዓቱ" else "The Seven Canonical Hours",
                    badge = suggestedHour?.name?.takeIf { it.isNotBlank() }?.let {
                        if (s.isAmharic) "የአሁኑ፡ $it" else "Current: $it"
                    },
                    onClick = onOpenReading,
                )
            }
            item {
                IlluminatedLibraryCard(
                    icon = Icons.Outlined.Church,
                    title = s.mahletTitle,
                    subtitle = s.mahletSubtitle,
                    badge = if (s.isAmharic) "፻፹፩ ማኅሌታት" else "181 Feasts",
                    onClick = onOpenMahlets,
                )
            }
            item {
                IlluminatedLibraryCard(
                    icon = Icons.Outlined.Favorite,
                    title = s.wudaseMariam,
                    subtitle = s.wudaseScheduleSubtitle,
                    badge = if (s.isAmharic) "የዛሬው፡ $weekdayName" else "Today: $weekdayName",
                    onClick = onOpenWudase,
                )
            }
            item {
                com.agpeya.app.ui.reading.ReadingHeroCard(onOpen = onOpenReading)
            }

            // ── ፪. የቤተ ክርስቲያን መጻሕፍትና ትውፊት ─────────────────────────
            item {
                SectionHeader(
                    text = if (s.isAmharic) "የቤተ ክርስቲያን መጻሕፍትና ትውፊት" else "Church Tradition & Calendar",
                    modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.xs),
                )
            }
            item {
                IlluminatedLibraryCard(
                    icon = Icons.Outlined.AutoStories,
                    title = s.synaxariumTitle,
                    subtitle = s.synaxariumLibrarySubtitle,
                    badge = ethDateLabel,
                    onClick = onOpenSynaxarium,
                )
            }
            item {
                IlluminatedLibraryCard(
                    icon = Icons.Outlined.AutoStories,
                    title = s.booksTitle,
                    subtitle = s.booksSubtitle,
                    onClick = onOpenBooks,
                )
            }
            item {
                IlluminatedLibraryCard(
                    icon = Icons.Outlined.CalendarMonth,
                    title = s.bahreHasabTitle,
                    subtitle = s.bahreHasabSubtitle,
                    onClick = onOpenBahreHasab,
                )
            }

            // ── ፫. የግል ምልክቶችና ድርድር ────────────────────────────────
            item {
                SectionHeader(
                    text = if (s.isAmharic) "የግል ምልክቶችና ድርድር" else "Reader's Marks & Ribbons",
                    modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.xs),
                )
            }
            item {
                IlluminatedLibraryCard(
                    icon = Icons.Outlined.BookmarkBorder,
                    title = s.marksTitle,
                    subtitle = "${s.marksTabBookmarks} · ${s.marksTabHighlights} · ${s.marksTabNotes}",
                    onClick = onOpenMarks,
                )
            }

            item {
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
}

/** Scriptorium Quick-Resume Banner anchored to the current liturgical hour */
@Composable
private fun ScriptoriumQuickResumeHero(
    hour: Hour?,
    hourId: String,
    onOpenHour: () -> Unit,
    onOpenReading: () -> Unit,
    isAmharic: Boolean,
) {
    HeroCard(
        onClick = { if (hour != null) onOpenHour() else onOpenReading() },
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (isAmharic) "✦ ቀጥል (RESUME DEVOTION)" else "✦ RESUME DEVOTION",
                    style = MaterialTheme.typography.labelSmall.inReadingFont(),
                    color = sinqColors.onHeroGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    text = "↗",
                    style = MaterialTheme.typography.titleMedium,
                    color = sinqColors.onHeroGold,
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = hour?.name ?: (if (isAmharic) "መጽሐፈ ሰዓታት" else "Horologium"),
                style = MaterialTheme.typography.titleLarge.inReadingFont(),
                color = sinqColors.onHero,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(2.dp))

            val timeHintText = hour?.timeHint?.takeIf { it.isNotBlank() }
            val fallbackSub = if (isAmharic) "የዕለቱ የጸሎት ሰዓት" else "Current canonical hour of prayer"
            Text(
                text = if (timeHintText != null) {
                    if (hour?.transliteration?.isNotBlank() == true) "${hour.transliteration} · $timeHintText"
                    else timeHintText
                } else fallbackSub,
                style = MaterialTheme.typography.bodySmall.inReadingFont(),
                color = sinqColors.onHeroMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 3-Column Grid for Old Testament, New Testament, and Psalter */
@Composable
private fun CanonCodexGrid(
    onOpenScriptures: () -> Unit,
    isAmharic: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        CanonCodexTile(
            modifier = Modifier.weight(1f),
            geezNumeral = "፵፮",
            title = if (isAmharic) "ብሉይ ኪዳን" else "Old Test.",
            subtitle = if (isAmharic) "ኩፋሌና ሄኖክ" else "46 Books",
            onClick = onOpenScriptures,
        )
        CanonCodexTile(
            modifier = Modifier.weight(1f),
            geezNumeral = "፴፭",
            title = if (isAmharic) "ሐዲስ ኪዳን" else "New Test.",
            subtitle = if (isAmharic) "ቀሌምንጦስ" else "35 Books",
            onClick = onOpenScriptures,
        )
        CanonCodexTile(
            modifier = Modifier.weight(1f),
            geezNumeral = "፻፶",
            title = if (isAmharic) "መዝሙር" else "Psalter",
            subtitle = if (isAmharic) "፲፭ቱ ነቢያት" else "150 Psalms",
            onClick = onOpenScriptures,
        )
    }
}

@Composable
private fun CanonCodexTile(
    modifier: Modifier = Modifier,
    geezNumeral: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = geezNumeral,
                style = MaterialTheme.typography.titleMedium.inReadingFont(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.inReadingFont(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.inReadingFont(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Illuminated Liturgical Card with Harag gold accent border and badge */
@Composable
private fun IlluminatedLibraryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    SinqCard(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(IconSize.large),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.inReadingFont(),
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (badge != null) {
                        Spacer(Modifier.width(Spacing.xs))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall.inReadingFont(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontStyle = if (enabled) FontStyle.Normal else FontStyle.Italic,
                    ).inReadingFont(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
