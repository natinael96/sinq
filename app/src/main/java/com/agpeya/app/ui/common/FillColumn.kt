package com.agpeya.app.ui.common

import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A column that ends where the screen ends.
 *
 * A plain [androidx.compose.foundation.layout.Column] is as tall as the things
 * in it, which on a page meant to be taken in at a glance leaves the tall phone
 * with a band of empty ground at the foot and the short phone with a scrollbar.
 * This one is given the height it is meant to fill — [targetHeight], the height
 * of the viewport — and spends it:
 *
 *  1. Every block is measured at the height its own content asks for. That
 *     height is its floor, and nothing is ever put below it, so a long feast
 *     name or a large type size takes the room it needs rather than being
 *     squeezed.
 *  2. What is left over is shared among the blocks marked [FillColumnScope.grow],
 *     in proportion to their weight. A block that reaches its ceiling stops
 *     taking, and what it could not take is offered again to the others.
 *  3. Whatever the ceilings hold back is spent on the gaps between the blocks,
 *     evenly. A card is not improved by being made enormous; the page is
 *     improved by breathing.
 *
 * When the floors alone are taller than [targetHeight] — a small screen at a
 * large font — the column simply reports that greater height. Put it in a
 * [androidx.compose.foundation.verticalScroll] container and it will scroll
 * then, and only then: at every other size the content is exactly the height of
 * the viewport, so there is nothing to scroll.
 *
 * Growing blocks are measured at a fixed height, which is what makes
 * `Modifier.weight` work *inside* them. It also means they must not contain a
 * `SubcomposeLayout` (`BoxWithConstraints`, `LazyColumn`), since their floor is
 * read from intrinsic measurement and subcomposition cannot answer that.
 */
@Composable
fun FillColumn(
    targetHeight: Dp,
    modifier: Modifier = Modifier,
    gap: Dp = 0.dp,
    content: @Composable FillColumnScope.() -> Unit,
) {
    Layout(
        content = { FillColumnScopeInstance.content() },
        modifier = modifier,
    ) { measurables, constraints ->
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val gapPx = gap.roundToPx()
        val count = measurables.size
        val slots = (count - 1).coerceAtLeast(0)
        val target = targetHeight.roundToPx().coerceAtLeast(0)

        val grow = measurables.map { it.parentData as? GrowData }
        val placeables = arrayOfNulls<Placeable>(count)
        val floors = IntArray(count)
        val weights = FloatArray(count)
        val ceilings = IntArray(count) { Int.MAX_VALUE }

        // A fixed block is measured now and is done. A growing block is only
        // asked how short it could be; it is measured once, below, at the
        // height it ends up with — a measurable may not be measured twice.
        val ownHeight = Constraints(minWidth = width, maxWidth = width)
        for (i in 0 until count) {
            val g = grow[i]
            if (g == null) {
                val placeable = measurables[i].measure(ownHeight)
                placeables[i] = placeable
                floors[i] = placeable.height
            } else {
                floors[i] = measurables[i].minIntrinsicHeight(width)
                weights[i] = g.weight
                ceilings[i] =
                    if (g.max == Dp.Infinity) Int.MAX_VALUE
                    else g.max.roundToPx().coerceAtLeast(floors[i])
            }
        }

        val free = target - floors.sum() - gapPx * slots
        val heights = if (free > 0) growHeights(floors, weights, ceilings, free) else floors.copyOf()

        for (i in 0 until count) {
            if (grow[i] == null) continue
            val placeable = measurables[i].measure(Constraints.fixed(width, heights[i]))
            placeables[i] = placeable
            heights[i] = placeable.height
        }

        val used = heights.sum() + gapPx * slots
        val height = maxOf(target, used).coerceIn(constraints.minHeight, constraints.maxHeight)
        // Anything the ceilings refused becomes air between the blocks, shared
        // out to the last pixel so the final block sits exactly on the foot.
        val spare = (height - used).coerceAtLeast(0)
        val each = if (slots > 0) spare / slots else 0
        val odd = if (slots > 0) spare % slots else 0

        layout(width, height) {
            var y = 0
            for (i in 0 until count) {
                placeables[i]?.placeRelative(0, y)
                y += heights[i]
                if (i < count - 1) y += gapPx + each + if (i < odd) 1 else 0
            }
        }
    }
}

/** The receiver of [FillColumn]'s content, which is where [grow] comes from. */
@LayoutScopeMarker
@Immutable
interface FillColumnScope {
    /**
     * Take a share of the height left over, proportional to [weight], and stop
     * at [max]. Without this a block keeps the height its content asks for.
     */
    fun Modifier.grow(weight: Float, max: Dp = Dp.Infinity): Modifier
}

private object FillColumnScopeInstance : FillColumnScope {
    override fun Modifier.grow(weight: Float, max: Dp): Modifier {
        require(weight > 0f) { "grow weight must be positive, was $weight" }
        return this.then(GrowData(weight, max))
    }
}

private class GrowData(val weight: Float, val max: Dp) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@GrowData
}

/**
 * Share [free] pixels among the blocks that grow, and say what is left.
 *
 * Kept apart from the layout, and pure, because this is the part with the
 * arithmetic in it: a weighted share, a ceiling that some blocks hit and others
 * do not, and a second offer of whatever the capped ones turned down. The
 * result is the height of each block; the caller finds what went unspent by
 * subtracting from the target.
 *
 * [floors] is never read down. [weights] is 0 for a block that does not grow.
 * [ceilings] is [Int.MAX_VALUE] for a block with no ceiling.
 */
internal fun growHeights(
    floors: IntArray,
    weights: FloatArray,
    ceilings: IntArray,
    free: Int,
): IntArray {
    val heights = floors.copyOf()
    if (free <= 0) return heights
    var remaining = free
    val open = BooleanArray(heights.size) { weights[it] > 0f && ceilings[it] > heights[it] }

    // One pass can close at most one block, so the number of blocks is the most
    // passes that can do anything; the loop leaves early on every real page.
    repeat(heights.size) {
        if (remaining <= 0) return heights
        var totalWeight = 0f
        for (i in open.indices) if (open[i]) totalWeight += weights[i]
        if (totalWeight <= 0f) return heights

        var spent = 0
        for (i in open.indices) {
            if (!open[i]) continue
            val share = (remaining * weights[i] / totalWeight).toInt()
            val room = ceilings[i] - heights[i]
            val take = minOf(share, room)
            heights[i] += take
            spent += take
            if (take < share || heights[i] >= ceilings[i]) open[i] = false
        }
        // Rounding leaves a few pixels that no share is big enough to claim.
        // They are not worth another pass — they go to the gaps.
        if (spent == 0) return heights
        remaining -= spent
    }
    return heights
}
