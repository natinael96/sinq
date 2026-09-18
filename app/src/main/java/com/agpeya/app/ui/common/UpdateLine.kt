package com.agpeya.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing

/**
 * The strip at the very top of ቤት saying a newer release exists.
 *
 * Built to match the reminder-permissions strip directly above it — same 48 dp
 * row, same leading icon, same rule beneath — because two different treatments
 * for two notices in the same slot reads as two different kinds of thing when
 * they are the same kind of thing: the app speaking about itself, briefly,
 * above the day.
 *
 * It carries slightly more weight than that one on purpose. A missed permission
 * is a state the person can leave alone; a waiting update is an errand with an
 * end, and it used to be a grey dot and grey text that was genuinely easy to
 * scroll past. So this one has a tinted ground and its action word is set in
 * the accent at medium weight. That is the whole of the difference — still a
 * strip, still one line, still dismissible.
 *
 * The row stays one line at every font scale ([TextOverflow.Ellipsis], no
 * wrapping): a notice that grows into a paragraph would start competing with
 * the prayer beneath it, which is exactly backwards.
 */
@Composable
fun UpdateLine(
    text: String,
    action: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // The one fill on the page, and the reason the strip reads at a
                // glance. Kept to the container tone rather than the accent
                // itself, so it sits under the day without shouting over it.
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .heightIn(min = 48.dp)
                .clickable(onClick = onOpen)
                .padding(start = Spacing.screen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                Icons.Outlined.SystemUpdateAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(IconSize.small),
            )
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
            )
            // A 48 dp target on a line this short: width is free, so the ×
            // takes a full square of it and is easy to hit.
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onDismiss)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = s.updateDismiss,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(IconSize.small),
                )
            }
        }
        // The rule separates the notice from the day without boxing it in,
        // which is the app's habit everywhere else.
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
                .clearAndSetSemantics { },
        )
    }
}
