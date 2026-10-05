package com.agpeya.app.ui.intro

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Church
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.model.Tour
import com.agpeya.app.model.TourPage
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
import kotlinx.coroutines.launch

/**
 * Pixel-perfect What's New showcase page in Sinq (ስንቅ).
 *
 * Presents major liturgical additions and architectural improvements
 * with authentic sacred Ethiopian Orthodox aesthetics:
 * - Deep liturgical green & bronzed gold accents
 * - Illuminated concentric emblem badge with Ge'ez numeral counter
 * - Liturgical category tag chip
 * - High-contrast readable typography in traditional font
 * - Prominent "Try It" feature CTA button
 * - Animated pill pager indicator and smooth swipe navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewTour(
    tour: Tour,
    onDone: () -> Unit,
    onOpenRoute: ((String) -> Unit)? = null,
) {
    val s = LocalStrings.current
    val isAmharic = s.isAmharic
    val pages = tour.pages

    if (pages.isEmpty()) {
        EmptyWhatsNewScreen(
            version = tour.version,
            isAmharic = isAmharic,
            onDone = onDone,
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isLast = pagerState.currentPage == pages.size - 1
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(
                            text = tour.title.pick(isAmharic).ifBlank {
                                if (isAmharic) "ምን አዲስ ነገር አለ?" else "What's New in Sinq"
                            },
                            style = MaterialTheme.typography.titleMedium.inReadingFont(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (tour.version.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                            ) {
                                Text(
                                    text = "v${tour.version}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = if (isAmharic) "ዝጋ" else "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (!isLast) {
                        TextButton(onClick = onDone) {
                            Text(
                                text = s.skip,
                                style = MaterialTheme.typography.labelLarge.inReadingFont(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            TourBottomBar(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                isLast = isLast,
                isAmharic = isAmharic,
                skipLabel = s.skip,
                backLabel = s.back,
                nextLabel = s.next,
                gotItLabel = s.gotIt,
                onBack = {
                    scope.launch {
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                onNext = {
                    scope.launch {
                        if (!isLast) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            onDone()
                        }
                    }
                },
                onSkip = onDone,
                onSelectPage = { page ->
                    scope.launch { pagerState.animateScrollToPage(page) }
                },
            )
        },
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.screen),
            pageSpacing = Spacing.md,
        ) { pageIndex ->
            TourPageContent(
                page = pages[pageIndex],
                pageIndex = pageIndex,
                isAmharic = isAmharic,
                onOpenRoute = onOpenRoute,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TourPageContent(
    page: TourPage,
    pageIndex: Int,
    isAmharic: Boolean,
    onOpenRoute: ((String) -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // ── 1. Illuminated Sacred Emblem ────────────────────────────────────
        val icon = pickTourIcon(page.route, page.kicker.pick(false))
        Box(
            modifier = Modifier.size(88.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Outer golden ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                        CircleShape,
                    ),
            )
            // Inner concentric halo
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(34.dp),
                )
            }
            // Ge'ez numeral badge in bottom-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = geezNumeral(pageIndex + 1),
                    style = MaterialTheme.typography.labelSmall.inReadingFont(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        // ── 2. Liturgical Kicker Tag ────────────────────────────────────────
        val kicker = page.kicker.pick(isAmharic)
        if (kicker.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
            ) {
                Text(
                    text = kicker,
                    style = MaterialTheme.typography.labelMedium.inReadingFont(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = 3.dp),
                )
            }
            Spacer(Modifier.height(Spacing.sm))
        }

        // ── 3. Feature Headline Title ───────────────────────────────────────
        Text(
            text = page.title.pick(isAmharic),
            style = MaterialTheme.typography.titleLarge.inReadingFont(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.lg))

        // ── 4. Illuminated Content Card ─────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    text = page.body.pick(isAmharic),
                    style = MaterialTheme.typography.bodyMedium.inReadingFont(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                )

                // Contextual Highlights tags
                val tags = pickFeatureHighlights(page.route, isAmharic)
                if (tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.inReadingFont(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 5. Contextual Action Button ("Try It" / "Open Feature") ─────────
        if (page.route != null && onOpenRoute != null && !page.actionLabel.isBlank) {
            Spacer(Modifier.height(Spacing.lg))
            Button(
                onClick = { onOpenRoute(page.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = page.actionLabel.pick(isAmharic),
                        style = MaterialTheme.typography.labelLarge.inReadingFont(),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.small),
                    )
                }
            }
        }
    }
}

@Composable
private fun TourBottomBar(
    pageCount: Int,
    currentPage: Int,
    isLast: Boolean,
    isAmharic: Boolean,
    skipLabel: String,
    backLabel: String,
    nextLabel: String,
    gotItLabel: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onSelectPage: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen)
            .padding(top = Spacing.xs, bottom = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Page indicator label & animated dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pageCount) { i ->
                val active = i == currentPage
                val width by animateDpAsState(
                    targetValue = if (active) 24.dp else 8.dp,
                    label = "dotWidth",
                )
                val color by animateColorAsState(
                    targetValue = if (active) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    label = "dotColor",
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = width, height = 8.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onSelectPage(i) },
                )
            }
        }

        Spacer(Modifier.height(Spacing.xs))

        Text(
            text = if (isAmharic)
                "ገጽ ${geezNumeral(currentPage + 1)} ከ ${geezNumeral(pageCount)}"
            else
                "Page ${currentPage + 1} of $pageCount",
            style = MaterialTheme.typography.labelSmall.inReadingFont(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Spacing.md))

        // Navigation controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (currentPage > 0) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.small),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(backLabel, style = MaterialTheme.typography.labelLarge.inReadingFont())
                }
            } else {
                TextButton(onClick = onSkip) {
                    Text(
                        skipLabel,
                        style = MaterialTheme.typography.labelLarge.inReadingFont(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isLast) {
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Text(
                        gotItLabel,
                        style = MaterialTheme.typography.labelLarge.inReadingFont(),
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.small),
                    )
                }
            } else {
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        nextLabel,
                        style = MaterialTheme.typography.labelLarge.inReadingFont(),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.small),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyWhatsNewScreen(
    version: String,
    isAmharic: Boolean,
    onDone: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isAmharic) "ምን አዲስ ነገር አለ?" else "What's New in Sinq",
                        style = MaterialTheme.typography.titleMedium.inReadingFont(),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = if (isAmharic) "ዝጋ" else "Close",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.NewReleases,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = if (isAmharic) "አዲስ የተለወጠ ነገር የለም" else "No New Updates",
                style = MaterialTheme.typography.titleLarge.inReadingFont(),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(Spacing.sm))

            Text(
                text = if (isAmharic)
                    "ለእትም v$version ዝርዝር የጉብኝት ገጾች አልተዘጋጁም። ሙሉውን የለውጥ ታሪክ በቅንብሮች ስር ማግኘት ይችላሉ።"
                else
                    "No feature tour is available for v$version. View the full changelog under Settings.",
                style = MaterialTheme.typography.bodyMedium.inReadingFont(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.xl))

            Button(
                onClick = onDone,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (isAmharic) "ተመለስ" else "Back")
            }
        }
    }
}

/** Picks a contextually relevant icon based on route and kicker keyword. */
private fun pickTourIcon(route: String?, kicker: String): ImageVector {
    val r = route?.lowercase().orEmpty()
    val k = kicker.lowercase()
    return when {
        r.contains("synaxarium") || k.contains("synaxarium") || k.contains("ስንክሳር") -> Icons.Outlined.AutoStories
        r.contains("book") || k.contains("book") || k.contains("መጻሕፍት") || k.contains("ውዳሴ") -> Icons.Outlined.Church
        r.contains("scripture") || r.contains("bible") || k.contains("scripture") || k.contains("መጽሐፍ ቅዱስ") -> Icons.Outlined.Translate
        else -> Icons.Outlined.NewReleases
    }
}

/** Picks feature highlight pills based on route and locale. */
private fun pickFeatureHighlights(route: String?, isAmharic: Boolean): List<String> {
    val r = route?.lowercase().orEmpty()
    return when {
        r.contains("synaxarium") -> if (isAmharic) {
            listOf("፩ሺህ፲፯ አርኬዎች", "የብራና ቅጂ እርማት", "፫፻፷፮ ዕለታት")
        } else {
            listOf("1,017 Arke Strophes", "Manuscript Audited", "366 Liturgical Days")
        }
        r.contains("book") -> if (isAmharic) {
            listOf("፰ የጸሎት ክፍሎች", "ዘወትር እስከ እሑድ", "የጸሎት መጻሕፍት")
        } else {
            listOf("8 Daily Sections", "Mon–Sun Cycle", "Church Books")
        }
        r.contains("scripture") -> if (isAmharic) {
            listOf("NKJV እንግሊዝኛ", "ጎን ለጎን ንባብ", "ሁለት ቋንቋ")
        } else {
            listOf("English NKJV", "Parallel Reader", "Bilingual Toggle")
        }
        else -> emptyList()
    }
}
