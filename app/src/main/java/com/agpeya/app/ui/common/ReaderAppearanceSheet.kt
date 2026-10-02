package com.agpeya.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.data.ReadingFont
import com.agpeya.app.data.ReadingMode
import com.agpeya.app.data.SettingsRepository
import com.agpeya.app.ui.reading.geezNumeral
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.readingFontFamily

private val FONT_OPTIONS = listOf(
    ReadingFont.ABBA_GARIMA to "አባ ገሪማ",
    ReadingFont.ABYSSINICA to "አቢሲኒካ",
    ReadingFont.BELA_BEREKA to "በላ በረካ",
    ReadingFont.ZEMENAY to "ዘመናይ",
    ReadingFont.ABAY_LIGHT to "አባይ",
)

/**
 * Dedicated reading appearance and typography sheet.
 *
 * Replaces the repetitive dropdown menu taps with an interactive, persistent
 * surface for adjusting font scale, font family, and reading layout mode with
 * live visual feedback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderAppearanceSheet(
    fontStep: Int,
    onFontStepChange: (Int) -> Unit,
    currentFont: ReadingFont,
    onFontChange: (ReadingFont) -> Unit,
    readingMode: ReadingMode? = null,
    onToggleReadingMode: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fontSteps = SettingsRepository.FONT_STEPS_SP
    val currentSp = fontSteps[fontStep.coerceIn(fontSteps.indices)]

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.huge),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.AutoStories,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(IconSize.medium),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = s.appearance,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = s.dismiss,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(IconSize.medium),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // ── Section 1: Font Size Stepper ─────────────────────────────────
            Text(
                text = "${s.readingFontTitle} · መጠን",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FilledTonalIconButton(
                    onClick = { if (fontStep > 0) onFontStepChange(fontStep - 1) },
                    enabled = fontStep > 0,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Icon(Icons.Outlined.Remove, contentDescription = "A−", modifier = Modifier.size(IconSize.medium))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$currentSp sp",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "መጠን ${geezNumeral(currentSp)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                FilledTonalIconButton(
                    onClick = { if (fontStep < fontSteps.lastIndex) onFontStepChange(fontStep + 1) },
                    enabled = fontStep < fontSteps.lastIndex,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "A+", modifier = Modifier.size(IconSize.medium))
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // Live Preview Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Text(
                    text = "አቡነ ዘበሰማያት ይትቀደስ ስምከ፤ ትምጻእ መንግሥትከ።",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = readingFontFamily(currentFont),
                        fontSize = currentSp.sp,
                        lineHeight = (currentSp * 1.5f).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(Spacing.lg))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(Modifier.height(Spacing.md))

            // ── Section 2: Font Family ───────────────────────────────────────
            Text(
                text = "የፊደል ቅርጽ (Typeface)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(Spacing.sm))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(FONT_OPTIONS) { (fontChoice, label) ->
                    val isSelected = fontChoice == currentFont
                    val family = readingFontFamily(fontChoice)
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .selectable(
                                selected = isSelected,
                                onClick = { onFontChange(fontChoice) },
                            ),
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = family,
                                    fontSize = 15.sp,
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                            )
                            if (isSelected) {
                                Spacer(Modifier.width(Spacing.xs))
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(IconSize.small),
                                )
                            }
                        }
                    }
                }
            }

            // ── Section 3: Reading Mode (Scroll vs Paged) ────────────────────
            if (readingMode != null && onToggleReadingMode != null) {
                Spacer(Modifier.height(Spacing.lg))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Spacer(Modifier.height(Spacing.md))

                Text(
                    text = s.readingModeToggle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(Spacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    val isVertical = readingMode == ReadingMode.VERTICAL
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable { if (!isVertical) onToggleReadingMode() },
                        shape = MaterialTheme.shapes.small,
                        color = if (isVertical) MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isVertical) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Outlined.SwapVert,
                                contentDescription = null,
                                tint = if (isVertical) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(IconSize.medium),
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Text(
                                text = s.readingModeVertical,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isVertical) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable { if (isVertical) onToggleReadingMode() },
                        shape = MaterialTheme.shapes.small,
                        color = if (!isVertical) MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isVertical) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.MenuBook,
                                contentDescription = null,
                                tint = if (!isVertical) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(IconSize.medium),
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Text(
                                text = s.readingModeHorizontal,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (!isVertical) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}
