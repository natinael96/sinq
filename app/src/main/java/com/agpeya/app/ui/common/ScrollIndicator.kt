package com.agpeya.app.ui.common

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Resting, while the list moves, and while a finger is on it. */
private const val ALPHA_REST = 0.22f
private const val ALPHA_SCROLL = 0.5f
private const val ALPHA_DRAG = 0.9f

/**
 * Where you are in a long reading, and a way to move through it.
 *
 * A chapter of ሥርዓተ ቅዳሴ runs to two thousand paragraphs and a መልክእ to forty
 * stanzas, and nothing on the page said whether you were near the start of one
 * or the end. A mark down the right edge says it the way a thumb in the pages
 * of a book says it — proportion, not a number — and dragging that mark moves
 * the list, which is the standard gesture for a list this long.
 *
 * It is faint but always drawn, not faded away when the list settles: a mark
 * that disappears cannot be grabbed, and half of what it is for now is being
 * grabbed. It brightens while the list moves and again under a finger.
 *
 * [label] names where a drag would land — "መዝሙር ፵፯" — in a bubble beside the
 * mark. Without one the mark still drags; it just does not say where to.
 *
 * Position is measured in items rather than pixels: the rows are of wildly
 * different heights — a one-line heading against a fifteen-line stanza — but a
 * reader reads the mark as "roughly how far", and item count answers that
 * honestly without measuring rows that have not been drawn.
 *
 * Hidden from accessibility: it is a picture of the scroll position, which a
 * screen reader already announces far better than a 3dp bar can.
 */
@Composable
fun ScrollIndicator(
    state: LazyListState,
    modifier: Modifier = Modifier,
    label: ((Int) -> String)? = null,
) {
    val info = state.layoutInfo
    val total = info.totalItemsCount
    val visible = info.visibleItemsInfo.size
    // Nothing to say when everything already fits on the screen.
    if (total == 0 || visible == 0 || visible >= total) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var dragging by remember { mutableStateOf(false) }
    // Where the drag has got to, as a fraction of the track. Held separately
    // from the list's own position so the mark follows the finger exactly
    // rather than lagging behind the scroll it is causing.
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val last = (total - visible).coerceAtLeast(1)
    val fraction = if (dragging) dragFraction
    else (state.firstVisibleItemIndex.toFloat() / last).coerceIn(0f, 1f)

    val alpha by animateFloatAsState(
        targetValue = when {
            dragging -> ALPHA_DRAG
            state.isScrollInProgress -> ALPHA_SCROLL
            else -> ALPHA_REST
        },
        animationSpec = tween(durationMillis = if (dragging) 90 else 320),
        label = "scrollIndicatorAlpha",
    )
    val barWidth by animateDpAsState(
        targetValue = if (dragging) 7.dp else 3.dp,
        animationSpec = tween(durationMillis = 120),
        label = "scrollIndicatorWidth",
    )

    // The thumb is as tall a share of the track as the screen is of the list,
    // floored so it stays findable — and grabbable — in a book of two thousand
    // paragraphs.
    val thumbFraction = (visible.toFloat() / total).coerceIn(0.06f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = Spacing.sm)
            .semantics { hideFromAccessibility() },
    ) {
        val track = maxHeight
        val thumb = track * thumbFraction
        val travel = track - thumb
        val travelPx = with(density) { travel.toPx() }.coerceAtLeast(1f)

        fun goTo(f: Float) {
            val target = (f.coerceIn(0f, 1f) * last).roundToInt().coerceIn(0, total - 1)
            scope.launch { state.scrollToItem(target) }
        }

        if (dragging && label != null) {
            val target = (fraction * last).roundToInt().coerceIn(0, total - 1)
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = travel * fraction + thumb / 2 - 16.dp)
                    .padding(end = Spacing.lg)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
            ) {
                Text(
                    label(target),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(y = travel * fraction)
                // A transparent 22dp grip around a 3dp bar: the bar is the mark,
                // this is the thing a thumb can actually land on. It stops at
                // the thumb's own height, so the rest of the right margin still
                // belongs to the verse taps underneath.
                .width(22.dp)
                .fillMaxHeight(thumbFraction)
                .pointerInput(travelPx, last, total) {
                    detectDragGestures(
                        onDragStart = {
                            dragging = true
                            dragFraction = (state.firstVisibleItemIndex.toFloat() / last)
                                .coerceIn(0f, 1f)
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, drag ->
                        change.consume()
                        dragFraction = (dragFraction + drag.y / travelPx).coerceIn(0f, 1f)
                        goTo(dragFraction)
                    }
                },
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                Modifier
                    .padding(end = Spacing.xxs)
                    .width(barWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .alpha(alpha)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}
