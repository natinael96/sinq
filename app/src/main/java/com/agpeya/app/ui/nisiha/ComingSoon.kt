package com.agpeya.app.ui.nisiha

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agpeya.app.ui.common.StatePanel
import com.agpeya.app.ui.strings.LocalStrings

/**
 * Stands in for a screen whose text is not written yet.
 *
 * ንስሐ and ቁርባን are taught by the Church, not drafted by an app: their content
 * waits on a ሊቅ rather than on code. The screens exist and are reachable so the
 * shape of what is coming is visible, and they say plainly that nothing is
 * there yet — which is better than inventing something to fill them with.
 *
 * Driven by the bundled content being empty, so adding the text is all it takes
 * to turn the real screen on. No code change, no release gate.
 *
 * Uses [StatePanel] rather than its own column: "nothing here yet" is the same
 * kind of state as empty or unavailable, and the app already has one voice for
 * those.
 */
@Composable
fun ComingSoon(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        StatePanel(
            title = s.comingSoon,
            body = s.comingSoonBody,
            icon = Icons.Outlined.Schedule,
        )
    }
}
