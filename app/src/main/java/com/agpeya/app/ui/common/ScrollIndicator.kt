package com.agpeya.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.theme.Spacing
import kotlinx.coroutines.delay

/** How long the mark lingers after the reader's finger stops. */
private const val LINGER_MS = 900L

/**
 * Where you are in a long reading, as a mark down the right edge.
 *
 * A chapter of ሥርዓተ ቅዳሴ runs to two thousand paragraphs and a መልክእ to forty
 * stanzas, and until now nothing on the page said whether you were near the
 * start of one or the end. This says it the way a thumb in the pages of a book
 * says it — proportion, not a number.
 *
 * It appears while the list is moving and fades once it stops, because the
 * question it answers is one a reader asks in the act of scrolling. A mark
 * that never went away would sit permanently beside prayer text, competing
 * with it for the edge of every page. Pass [alwaysVisible] to keep it.
 *
 * Position is measured in items rather than pixels: the rows are of wildly
 * different heights — a one-line heading against a fifteen-line stanza — but
 * a reader reads the mark as "roughly how far", and item count answers that
 * honestly without the cost of measuring every row that has not been drawn.
 *
 * Hidden from accessibility: it is a picture of the scroll position, and a
 * screen reader already announces that far better than a 3dp bar can.
 */
@Composable
fun ScrollIndicator(
    state: LazyListState,
    modifier: Modifier = Modifier,
    alwaysVisible: Boolean = false,
) {
    val info = state.layoutInfo
    val total = info.totalItemsCount
    val visible = info.visibleItemsInfo.size
    // Nothing to say when everything already fits on the screen.
    if (total == 0 || visible == 0 || visible >= total) return

    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) {
            settled = false
        } else {
            delay(LINGER_MS)
            settled = true
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (alwaysVisible || !settled) 1f else 0f,
        animationSpec = tween(durationMillis = if (settled) 420 else 120),
        label = "scrollIndicatorAlpha",
    )
    if (alpha <= 0.01f) return

    // The thumb is as tall a share of the track as the screen is of the list,
    // floored so it stays findable in a book of two thousand paragraphs.
    val thumbFraction = (visible.toFloat() / total).coerceIn(0.06f, 1f)
    val scrolled = state.firstVisibleItemIndex.toFloat() / (total - visible).coerceAtLeast(1)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = Spacing.sm)
            .semantics { hideFromAccessibility() },
    ) {
        val track = maxHeight
        val thumb = track * thumbFraction
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(y = (track - thumb) * scrolled.coerceIn(0f, 1f))
                .padding(end = Spacing.xxs)
                .width(3.dp)
                .then(Modifier.fillMaxHeight(thumbFraction))
                .clip(RoundedCornerShape(2.dp))
                .alpha(alpha * 0.5f)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant),
        )
    }
}
