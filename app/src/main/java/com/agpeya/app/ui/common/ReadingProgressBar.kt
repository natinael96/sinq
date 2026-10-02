package com.agpeya.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.theme.LocalMotion
import com.agpeya.app.ui.theme.Motion

/**
 * Hairline progress bar along the top of reader surfaces.
 * Fills in gold as the user reads or scrolls through the liturgical content.
 */
@Composable
fun ReadingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
    visible: Boolean = true,
) {
    if (!visible) return
    val motion = LocalMotion.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = motion.spec(Motion.standard),
        label = "readingProgress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .height(2.5.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        if (animatedProgress > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(2.5.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)),
            )
        }
    }
}

/**
 * Derives a smooth reading progress fraction (0f..1f) from a [LazyListState].
 * Seamlessly interpolates between list items using scroll offset.
 */
@Composable
fun rememberLazyListProgress(
    state: LazyListState,
    headerOffset: Int = 0,
): Float {
    return remember(state, headerOffset) {
        derivedStateOf {
            val layout = state.layoutInfo
            val total = layout.totalItemsCount
            val visible = layout.visibleItemsInfo
            if (total <= 1 || visible.isEmpty()) 0f
            else if (visible.size >= total) {
                // If all items fit within the viewport
                val last = visible.last()
                if (last.offset + last.size <= layout.viewportEndOffset &&
                    state.firstVisibleItemIndex == 0 &&
                    state.firstVisibleItemScrollOffset == 0
                ) {
                    0f
                } else {
                    val remaining = (total - visible.size).coerceAtLeast(1)
                    val base = (state.firstVisibleItemIndex - headerOffset).coerceAtLeast(0).toFloat() / remaining
                    base.coerceIn(0f, 1f)
                }
            } else {
                val lastVisible = visible.last()
                if (lastVisible.index >= total - 1 && lastVisible.offset + lastVisible.size <= layout.viewportEndOffset) {
                    1f
                } else {
                    val last = (total - visible.size).coerceAtLeast(1)
                    val base = (state.firstVisibleItemIndex - headerOffset).coerceAtLeast(0).toFloat() / last
                    val firstVisible = visible.firstOrNull()
                    val offset = if (firstVisible != null && firstVisible.size > 0) {
                        (state.firstVisibleItemScrollOffset.toFloat() / firstVisible.size) / last
                    } else 0f
                    (base + offset).coerceIn(0f, 1f)
                }
            }
        }
    }.value
}
