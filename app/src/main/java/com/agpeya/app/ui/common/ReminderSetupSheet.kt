package com.agpeya.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing

/**
 * Bottom sheet guiding the user to grant notification permissions and exempt
 * the app from aggressive battery optimizations so alarms fire reliably.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSetupSheet(
    notificationsReady: Boolean,
    batteryReady: Boolean,
    onEnableNotifications: () -> Unit,
    onReviewBattery: () -> Unit,
    onNotNow: () -> Unit,
    onDismiss: () -> Unit,
) {
    val s = LocalStrings.current
    val allReady = notificationsReady && batteryReady
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.screen)
                .padding(bottom = Spacing.lg),
        ) {
            Text(
                s.reminderSetupTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                s.reminderSetupBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.lg))
            ReminderSetupRow(
                icon = Icons.Outlined.Notifications,
                title = s.reminderSetupNotifications,
                body = s.reminderSetupNotificationsBody,
                complete = notificationsReady,
                action = s.enableAction,
                onAction = onEnableNotifications,
            )
            Spacer(Modifier.height(Spacing.sm))
            ReminderSetupRow(
                icon = Icons.Outlined.BatterySaver,
                title = s.reminderSetupBattery,
                body = s.reminderSetupBatteryBody,
                complete = batteryReady,
                action = s.allowBackground,
                onAction = onReviewBattery,
            )
            Spacer(Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (allReady) Button(onClick = onNotNow) { Text(s.continueAction) }
                else TextButton(onClick = onNotNow) { Text(s.notNow) }
            }
        }
    }
}

@Composable
fun ReminderSetupRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    complete: Boolean,
    action: String,
    onAction: () -> Unit,
) {
    val s = LocalStrings.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                if (complete) Icons.Outlined.CheckCircle else icon,
                contentDescription = null,
                tint = if (complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(IconSize.medium),
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (complete) {
                Text(s.doneLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            } else {
                FilledTonalButton(onClick = onAction) { Text(action) }
            }
        }
    }
}
