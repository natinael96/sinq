package com.agpeya.app.ui.nisiha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing

/**
 * Stands in for a screen whose text is not written yet.
 *
 * ንስሐ and ቁርባን are taught by the Church, not drafted by an app: their content
 * waits on a ሊቅ rather than on code. The screens exist and are reachable so
 * the shape of what is coming is visible, and they say plainly that nothing is
 * there yet — which is better than inventing something to fill them with.
 *
 * Driven by the bundled content being empty, so adding the text is all it takes
 * to turn the real screen on. No code changes, no release gate.
 */
@Composable
fun ComingSoon(modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.screen),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            s.comingSoon,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            s.comingSoonBody,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
