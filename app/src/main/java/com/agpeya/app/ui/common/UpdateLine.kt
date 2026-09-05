package com.agpeya.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing

/**
 * A hairline at the very top of ቤት saying a newer release exists.
 *
 * Deliberately the quietest thing on the screen: no fill, no icon, a single
 * gold dot and a rule beneath it. It sits above the date header so it never
 * pushes into the day itself, and it is the only place in the app that speaks
 * about the app rather than about prayer — so it says its piece in one line
 * and offers a way to be rid of it.
 *
 * The row stays one line at every font scale ([TextOverflow.Ellipsis], no
 * wrapping): a notice that grows into a paragraph would start competing with
 * the prayer beneath it, which is exactly backwards.
 */
@Composable
fun UpdateLine(
    version: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .height(28.dp)
            .clickable(onClick = onOpen)
            .padding(start = Spacing.screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
        Text(
            s.updateAvailable(version),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            s.updateDownload,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
        )
        // A 48dp-wide target on a line this short. The strip is deliberately
        // 28dp tall — the whole point of it — so the guideline's 48dp cannot be
        // met vertically without becoming the banner it was meant not to be.
        // Width is free, so the × takes all of it and is easy to hit.
        Box(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onDismiss)
                .semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = s.updateDismiss,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(IconSize.small),
            )
        }
    }
    // The rule is the whole of the decoration: it separates the notice from the
    // day without boxing it in, which is the app's habit everywhere else.
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
            .clearAndSetSemantics { },
    )
}

/**
 * The one-time question, in the same hairline as the answer.
 *
 * Opt-in is only honest if it is actually offered. Left to a switch in
 * Settings the check is never found, and a notice nobody has enabled is a
 * notice nobody receives — which is what happened between 1.7.0 and 1.7.2.
 *
 * Asked once, on ቤት, in the quietest shape the app has. Either answer is
 * final: there is no third launch where it asks again, and Settings still has
 * the switch for anyone who changes their mind.
 */
@Composable
fun UpdateConsentLine(
    onAnswer: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .height(28.dp)
            .padding(start = Spacing.screen),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
        Text(
            s.updateAsk,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        ConsentAnswer(s.updateAskYes, MaterialTheme.colorScheme.secondary) { onAnswer(true) }
        ConsentAnswer(s.updateAskNo, MaterialTheme.colorScheme.onSurfaceVariant) { onAnswer(false) }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
            .clearAndSetSemantics { },
    )
}

@Composable
private fun ConsentAnswer(label: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
    }
}
