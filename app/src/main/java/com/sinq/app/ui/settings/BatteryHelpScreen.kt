package com.sinq.app.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sinq.app.reminders.ReminderScheduler
import com.sinq.app.ui.common.SinqTopBar
import com.sinq.app.ui.strings.LocalStrings
import com.sinq.app.ui.theme.Spacing

private enum class OemBrand {
    TECNO,
    INFINIX,
    XIAOMI,
    SAMSUNG,
    GENERIC,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BatteryHelpScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    var selectedBrand by remember {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val defaultBrand = when {
            manufacturer.contains("tecno") -> OemBrand.TECNO
            manufacturer.contains("infinix") -> OemBrand.INFINIX
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> OemBrand.XIAOMI
            manufacturer.contains("samsung") -> OemBrand.SAMSUNG
            else -> OemBrand.GENERIC
        }
        mutableStateOf(defaultBrand)
    }

    val canExactAlarm = remember { ReminderScheduler.canScheduleExact(context) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SinqTopBar(title = s.remindersNotFiringTitle, onBack = onBack)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = s.batteryHelpIntro,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (!canExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(Modifier.height(Spacing.md))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = s.exactAlarmWarningTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = s.exactAlarmWarningBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Button(
                            onClick = { openExactAlarmSettings(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(s.grantExactAlarmPermission)
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "Device manufacturer",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(Spacing.xs))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OemBrand.values().forEach { brand ->
                    val label = when (brand) {
                        OemBrand.TECNO -> s.oemTecnoTitle
                        OemBrand.INFINIX -> s.oemInfinixTitle
                        OemBrand.XIAOMI -> s.oemXiaomiTitle
                        OemBrand.SAMSUNG -> s.oemSamsungTitle
                        OemBrand.GENERIC -> s.oemGenericTitle
                    }
                    FilterChip(
                        selected = selectedBrand == brand,
                        onClick = { selectedBrand = brand },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val (brandTitle, brandSteps) = when (selectedBrand) {
                        OemBrand.TECNO -> s.oemTecnoTitle to s.oemTecnoSteps
                        OemBrand.INFINIX -> s.oemInfinixTitle to s.oemInfinixSteps
                        OemBrand.XIAOMI -> s.oemXiaomiTitle to s.oemXiaomiSteps
                        OemBrand.SAMSUNG -> s.oemSamsungTitle to s.oemSamsungSteps
                        OemBrand.GENERIC -> s.oemGenericTitle to s.oemGenericSteps
                    }

                    Text(
                        text = brandTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = brandSteps,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { openOemSettings(context, selectedBrand) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(s.openSettings)
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            OutlinedButton(
                onClick = { openAppSettings(context) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(s.openSettings + " (App Info)")
            }
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.onFailure {
            openAppSettings(context)
        }
    } else {
        openAppSettings(context)
    }
}

private fun openOemSettings(context: Context, brand: OemBrand) {
    val success = runCatching {
        val intent = when (brand) {
            OemBrand.XIAOMI -> {
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                )
            }
            OemBrand.TECNO, OemBrand.INFINIX -> {
                Intent().setComponent(
                    ComponentName(
                        "com.transsion.phonemaster",
                        "com.transsion.phonemaster.MainActivity"
                    )
                )
            }
            OemBrand.SAMSUNG -> {
                Intent().setComponent(
                    ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                )
            }
            OemBrand.GENERIC -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
            }
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    if (!success) {
        openAppSettings(context)
    }
}

private fun openAppSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
