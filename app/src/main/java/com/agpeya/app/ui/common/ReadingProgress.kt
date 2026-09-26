package com.agpeya.app.ui.common

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Geometry of one laid-out reading item, independent of Compose for regression tests. */
internal data class ReadingViewportItem(val index: Int, val offset: Int, val size: Int)

/**
 * Text-weighted progress at the viewport's lower edge. Long sections count in
 * proportion to their text; controls have weight zero. The fraction inside a
 * visible section uses its actual measured height rather than just its index.
 */
internal fun readingFraction(weights: List<Int>, visible: List<ReadingViewportItem>, viewportEnd: Int): Double {
    val total = weights.sumOf { it.coerceAtLeast(0).toLong() }
    if (total == 0L || visible.isEmpty()) return 0.0
    val prefix = weights.runningFold(0L) { sum, weight -> sum + weight.coerceAtLeast(0) }
    return visible.maxOf { item ->
        when {
            item.index < 0 -> 0.0
            item.index >= weights.size -> 1.0
            item.size <= 0 -> prefix[item.index].toDouble() / total
            else -> {
                val fraction = ((viewportEnd - item.offset).toDouble() / item.size).coerceIn(0.0, 1.0)
                (prefix[item.index] + weights[item.index].coerceAtLeast(0) * fraction) / total
            }
        }
    }.coerceIn(0.0, 1.0)
}

internal fun pageReadingFraction(weights: List<Int>, page: Int, fraction: Double): Double {
    val total = weights.sumOf { it.coerceAtLeast(0).toLong() }
    if (total == 0L || page !in weights.indices) return 0.0
    return (weights.take(page).sumOf { it.coerceAtLeast(0).toLong() } +
        weights[page].coerceAtLeast(0) * fraction.coerceIn(0.0, 1.0)) / total
}

internal fun reachedReadingHalfway(progress: Double, interacted: Boolean): Boolean =
    interacted && progress.isFinite() && progress >= 0.5

/** Programmatic restore/navigation is not a reading gesture. */
@Composable
internal fun rememberReadingGesture(source: InteractionSource, key: String): Boolean {
    var interacted by remember(key, source) { mutableStateOf(false) }
    LaunchedEffect(source, key) {
        source.interactions.collect { if (it is DragInteraction.Start) interacted = true }
    }
    return interacted
}

/** One successful write per reader/day. Failed writes retry on subsequent scrolling. */
@Composable
internal fun rememberHalfwayRead(key: String, onRead: suspend () -> Unit): (Double) -> Unit {
    var complete by rememberSaveable(key) { mutableStateOf(false) }
    var saving by remember(key) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val latestWrite by rememberUpdatedState(onRead)
    return remember(key) {
        { progress ->
            if (reachedReadingHalfway(progress, true) && !complete && !saving) {
                saving = true
                scope.launch {
                    try {
                        latestWrite()
                        complete = true
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Keep the reader quiet; another scroll retries rather than claiming success.
                    } finally { saving = false }
                }
            }
        }
    }
}

@Composable
internal fun ObserveReadingProgress(
    state: LazyListState,
    key: String,
    weights: List<Int>,
    enabled: Boolean = true,
    pageWasSwiped: Boolean = false,
    onProgress: (Double) -> Unit,
) {
    val dragged = rememberReadingGesture(state.interactionSource, key)
    val latestReport by rememberUpdatedState(onProgress)
    LaunchedEffect(state, key, weights, enabled, pageWasSwiped, dragged) {
        if (!enabled || !(dragged || pageWasSwiped)) return@LaunchedEffect
        snapshotFlow {
            val info = state.layoutInfo
            readingFraction(weights, info.visibleItemsInfo.map { ReadingViewportItem(it.index, it.offset, it.size) },
                info.viewportEndOffset)
        }.distinctUntilChanged().collect { latestReport(it) }
    }
}
