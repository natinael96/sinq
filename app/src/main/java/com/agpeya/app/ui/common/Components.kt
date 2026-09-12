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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont
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
 * The gold heading that names a block of the page. Set at [SectionTitleStyle] —
 * a step up from the labels around it, and bold — so a long scroll reads as a
 * sequence of named blocks rather than one undifferentiated column.
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
            style = SectionTitleStyle,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/**
 * The one type role for section names, shared by [SectionHeader] and
 * [CollapsibleHeader] so a collapsible block sits at the same rank as a plain
 * one instead of looking like a lesser heading with a chevron.
 */
private val SectionTitleStyle: androidx.compose.ui.text.TextStyle
    @Composable get() = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)

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
            style = SectionTitleStyle,
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
/**
 * The app's name, small, at the top of a root page.
 *
 * Not a headline — a mark. The full-size wordmark went in the compaction and
 * took the pages' top edge with it: with nothing above the first line, ቤት and
 * ጉዞ read as content pushed against the status bar. This gives the eye
 * somewhere to start for about 22 dp instead of the 40 the headline cost.
 */
/**
 * Open this app's notification settings, falling back to the app's own page.
 *
 * `ACTION_APP_NOTIFICATION_SETTINGS` only exists from Oreo, and minSdk here is
 * 23, so on API 23–25 the unguarded call throws ActivityNotFoundException and
 * takes the screen down. Those versions have no per-app notification page at
 * all; the app detail page is where the setting lives.
 */
fun openNotificationSettings(context: android.content.Context) {
    val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null),
        )
    }
    runCatching { context.startActivity(intent) }
}

/**
 * Open a link in whatever the reader browses with.
 *
 * Wrapped because a device can have no browser at all — a stripped ROM, a
 * managed profile, an emulator — and an unhandled ACTION_VIEW takes the screen
 * down. A link that quietly does nothing is a poor outcome; a crash is a worse
 * one, and there is nothing useful to say in between.
 */
fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)),
        )
    }
}
/** Where the app sends people who have something to tell the maintainer. */
const val FEEDBACK_URL = "https://sinq.natinael96.tech/feedback.html"

@Composable
fun SinqWordmark(modifier: Modifier = Modifier) {
    Text(
        text = "ስንቅ",
        style = MaterialTheme.typography.titleLarge.inReadingFont(),
        color = MaterialTheme.colorScheme.secondary,
        maxLines = 1,
        modifier = modifier,
    )
}

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
 * The brand's green gradient card — reserved for primary daily actions such as
 * the prayer for now and today's Gitsawe.
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
    val glowColor = sinq.heroGlow
    val body: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!glow) Modifier else Modifier.drawWithCache {
                        // The same bloom as before — a 104dp circle hung off the
                        // top-end corner, clipped by the card — but drawn rather
                        // than laid out. As a child it had a size of its own, so
                        // a card asked how short it could be answered 104dp,
                        // which is not true of the card and matters now that
                        // FillColumn asks exactly that question.
                        val radius = 52.dp.toPx()
                        val centre = Offset(size.width - 28.dp.toPx(), 28.dp.toPx())
                        val brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = centre,
                            radius = radius,
                        )
                        onDrawBehind { drawCircle(brush, radius = radius, center = centre) }
                    },
                ),
            // Given more height than the text needs — which is what FillColumn
            // does on a tall screen — the text sits in the middle of the card
            // rather than at the top of it. At its own height this is a no-op.
            contentAlignment = Alignment.CenterStart,
        ) {
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
            // sm, not md: a one-line row is held at 48 dp by heightIn either
            // way, so this only tightens the two-line rows, which are most of
            // ቅንብሮች — 69 dp down to 61.
            .padding(vertical = Spacing.sm),
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
 * The edition pill in a reader's title bar.
 *
 * It carries the name of the edition it will switch *to*, because a button that
 * names where it is going is the one people press. Shared by the Psalter and
 * ውዳሴ ማርያም: the same two editions, so the same control in the same corner.
 */
@Composable
fun EditionToggle(geez: Boolean, onToggle: () -> Unit) {
    val s = LocalStrings.current
    val other = if (geez) s.wudaseLangAmharic else s.wudaseLangGeez
    Box(
        modifier = Modifier
            .padding(end = Spacing.xs)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f), CircleShape)
            .clickable(
                onClickLabel = s.psalterEditionSwitch(other),
                onClick = onToggle,
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            other,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
        )
    }
}

/**
 * A door at the foot of a reading: where to go on from here.
 *
 * Chips rather than rows, because a stack of full-width rows made the foot of a
 * short passage longer than the passage, and chips wrap so a third one costs a
 * line only when it needs one.
 *
 * One that stays inside the app is filled in the page's gold; one that leaves is
 * left unfilled, ringed in the page's own outline, and carries the mark that
 * says so. That is the distinction plain rows did not draw.
 */
@Composable
fun DoorChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    external: Boolean = false,
) {
    val s = LocalStrings.current
    val gold = MaterialTheme.colorScheme.secondary
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (external) Color.Transparent else gold.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = if (external) MaterialTheme.colorScheme.outlineVariant else gold.copy(alpha = 0.42f),
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            // The app's own floor for anything you tap, chip or row alike.
            .heightIn(min = 48.dp)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(icon, contentDescription = null, tint = gold, modifier = Modifier.size(IconSize.small))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
        if (external) {
            Icon(
                Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = s.opensOutside,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

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
            .heightIn(min = 48.dp)
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = if (subtitle == null && accentLine == null) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (accentLine != null) {
                        Text(
                            accentLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
