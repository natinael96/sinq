package com.agpeya.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.sinqColors
import kotlinx.coroutines.delay

/**
 * The app's shared surfaces and rows.
 *
 * Screens are expected to reach for these rather than assembling their own
 * `Surface(shape = …, border = …)` each time — that is what kept four slightly
 * different cards and three slightly different list rows alive at once.
 */

// ── Section headers ──────────────────────────────────────────────────────────

/**
 * The gold kicker that names a block of the page. Deliberately small and quiet:
 * it should orient you without competing with the content under it.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/**
 * A section header that opens and closes what follows it. The chevron rotates
 * rather than swapping glyphs, which reads as one continuous gesture instead of
 * two states.
 */
@Composable
fun CollapsibleHeader(
    text: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    /** A count or short status shown before the chevron. */
    badge: String? = null,
) {
    val motion = LocalMotion.current
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = motion.spec(Motion.standard),
        label = "chevron",
    )
    val s = LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            // The chevron's direction is the only thing telling a sighted user
            // what this tap will do; onClickLabel says the same to everyone else.
            .clickable(
                onClickLabel = if (expanded) s.collapse else s.expand,
                onClick = onToggle,
            )
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(Spacing.sm))
        }
        Icon(
            imageVector = Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(IconSize.medium)
                .rotate(rotation),
        )
    }
}

// ── Surfaces ─────────────────────────────────────────────────────────────────

/**
 * The one card in the app: same radius, same hairline, same padding, whether or
 * not it is tappable.
 *
 * It stays flat — no elevation. On the ivory ground a shadow only muddies the
 * warm paper tone, and on the dark green one it is invisible anyway; the surface
 * step and the outline already carry the separation, and neither costs a
 * shadow-rendering pass on a low-end device.
 */
@Composable
fun SinqCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    /** Draw attention to this card without changing its shape or size. */
    accented: Boolean = false,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.xl, vertical = Spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    val motion = LocalMotion.current
    val border by animateColorAsState(
        targetValue = if (accented) MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = motion.spec(Motion.standard),
        label = "cardBorder",
    )
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(contentPadding), content = content)
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, border),
            content = body,
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, border),
            content = body,
        )
    }
}

/**
 * The brand's green card — used for the one thing on a screen that matters most
 * (the prayer for now, the current streak). Rare by design: a page with two of
 * these has none.
 *
 * [glow] adds the soft gold bloom in the top corner. It is a single clipped
 * radial gradient, not a blur, so it costs nothing to draw.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glow: Boolean = true,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.xl, vertical = Spacing.lg),
    content: @Composable RowScope.() -> Unit,
) {
    val sinq = sinqColors
    val body: @Composable () -> Unit = {
        Box(Modifier.fillMaxWidth()) {
            if (glow) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 24.dp, y = (-24).dp)
                        .size(104.dp)
                        .background(
                            Brush.radialGradient(listOf(sinq.heroGlow, Color.Transparent)),
                            CircleShape,
                        ),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = sinq.hero,
            contentColor = sinq.onHero,
            content = body,
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = sinq.hero,
            contentColor = sinq.onHero,
            content = body,
        )
    }
}

/** The app's divider: one weight, one colour, everywhere. */
@Composable
fun SinqDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

// ── Rows ─────────────────────────────────────────────────────────────────────

/**
 * A plain list row: optional leading icon, a title, an optional second line, and
 * whatever belongs at the end. Height comes from the content plus a fixed
 * padding, and a 48dp floor keeps the tap target reachable however short the
 * label is.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingTint: Color? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val titleColor = if (enabled) MaterialTheme.colorScheme.onBackground
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = leadingTint ?: MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(IconSize.medium),
            )
            Spacer(Modifier.width(Spacing.lg))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}

/** A row that leads somewhere. The chevron is the only affordance it needs. */
@Composable
fun NavRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
) {
    ListRow(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        leadingIcon = leadingIcon,
        onClick = onClick,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSize.medium),
        )
    }
}

/**
 * A setting you turn on or off. The whole row toggles — a 56dp-wide switch is a
 * poor target compared to the full width of the screen — and the switch itself
 * is marked decorative so a screen reader announces the row once, not twice.
 */
@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            // `toggleable`, not `clickable`: it carries the on/off state into the
            // accessibility tree, so TalkBack announces "switch, on" for the row.
            // A plain clickable would have announced a button with no state —
            // which is exactly what silencing the Switch below would have cost.
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(Spacing.md))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

// ── Selection ────────────────────────────────────────────────────────────────

/**
 * A selectable pill — the ግጻዌ office switcher, a scripture chapter. The fill and
 * the label cross-fade between states so selection reads as a change rather than
 * a repaint, and the row is marked `selected` for screen readers so the state
 * isn't carried by colour alone.
 */
@Composable
fun SelectPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotion.current
    val background by animateColorAsState(
        targetValue = if (selected) sinqColors.hero else Color.Transparent,
        animationSpec = motion.spec(Motion.fast),
        label = "pillBackground",
    )
    val border by animateColorAsState(
        targetValue = if (selected) sinqColors.hero else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = motion.spec(Motion.fast),
        label = "pillBorder",
    )
    val content by animateColorAsState(
        targetValue = if (selected) sinqColors.onHero else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motion.spec(Motion.fast),
        label = "pillContent",
    )
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, border, CircleShape)
            // `selectable` (not `clickable`) so the selected state reaches
            // TalkBack — it must never be carried by the fill colour alone.
            .selectable(selected = selected, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

// ── States ───────────────────────────────────────────────────────────────────

/**
 * Loading. The spinner waits [afterMillis] before appearing: content that
 * resolves in 40ms should look instant, not flash a spinner at the user.
 */
@Composable
fun LoadingPanel(modifier: Modifier = Modifier, afterMillis: Long = 150) {
    val show by produceState(initialValue = false) {
        delay(afterMillis)
        value = true
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (show) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/**
 * Nothing here, or something went wrong.
 *
 * [body] is expected to say what happened and what to do about it — and, when
 * an operation failed, whether the user's own data is affected. A stack trace or
 * an error code is never the message.
 */
@Composable
fun StatePanel(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.height(Spacing.lg))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        if (body != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.md))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

// ── Chrome ───────────────────────────────────────────────────────────────────

/**
 * The app bar every secondary screen uses: title, an optional second line for
 * context (a date, a chapter), a back arrow, and actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SinqTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    /** A third line, tinted gold — a liturgical season, a feast. */
    accentLine: String? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    titleContent: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val s = LocalStrings.current
    TopAppBar(
        modifier = modifier,
        title = {
            if (titleContent != null) {
                titleContent()
            } else {
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    if (accentLine != null) {
                        Text(
                            accentLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                        )
                    }
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        modifier = Modifier.size(IconSize.medium),
                    )
                }
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
