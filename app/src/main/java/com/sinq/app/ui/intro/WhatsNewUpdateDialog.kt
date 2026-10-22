package com.sinq.app.ui.intro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sinq.app.ui.reading.geezNumeral
import com.sinq.app.ui.settings.releaseHistory
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.IconSize
import com.sinq.app.ui.theme.Spacing
import com.sinq.app.ui.theme.inReadingFont

/**
 * Pop-up update dialog shown once on launch when a new version is installed.
 * Displays headline improvements dynamically from the latest release notes in [releaseHistory].
 */
@Composable
fun WhatsNewUpdateDialog(
    versionName: String,
    onDismiss: () -> Unit,
    onOpenTour: (() -> Unit)? = null,
) {
    val s = LocalStrings.current
    val isAmharic = s.isAmharic

    val currentRelease = releaseHistory.firstOrNull { it.version == versionName }
        ?: releaseHistory.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.NewReleases,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(IconSize.medium),
                    )
                }
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (isAmharic) "ምን አዲስ ነገር አለ?" else "What's New in Sinq",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.xxs))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = "v$versionName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                if (currentRelease != null) {
                    Text(
                        text = if (isAmharic) currentRelease.titleAm else currentRelease.title,
                        style = MaterialTheme.typography.titleSmall.inReadingFont(),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    val changes = if (isAmharic) currentRelease.changesAm else currentRelease.changes
                    changes.forEachIndexed { index, change ->
                        WhatsNewItem(
                            numeral = if (isAmharic) geezNumeral(index + 1) else "${index + 1}",
                            description = change,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(if (isAmharic) "እሺ / ተረድቻለሁ" else "Got It")
            }
        },
        dismissButton = if (onOpenTour != null) {
            {
                TextButton(onClick = onOpenTour) {
                    Text(s.whatsNewTour)
                }
            }
        } else null,
    )
}

@Composable
private fun WhatsNewItem(
    numeral: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(26.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = numeral,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.inReadingFont(),
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp,
            modifier = Modifier
                .weight(1f)
                .padding(top = 3.dp),
        )
    }
}
