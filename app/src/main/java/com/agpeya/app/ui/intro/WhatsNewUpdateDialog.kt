package com.agpeya.app.ui.intro

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
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing

/**
 * Pop-up update dialog shown once on launch when a new version is installed.
 * Summarizes headline improvements (Arke fix, Wudase Amlak, English Bible).
 */
@Composable
fun WhatsNewUpdateDialog(
    versionName: String,
    onDismiss: () -> Unit,
    onOpenTour: () -> Unit,
) {
    val s = LocalStrings.current
    val isAmharic = s.isAmharic

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
                WhatsNewItem(
                    numeral = if (isAmharic) "፩" else "1",
                    title = if (isAmharic) "የስንክሳር አርኬ ተስተካክሏል" else "Synaxarium Arke Hymns Fixed",
                    description = if (isAmharic)
                        "በዓመቱ ሙሉ የሚገኙት ፩ሺህ፲፯ቱ የስንክሳር አርኬዎች ከትክክለኛ የቤተ ክርስቲያን የብራና ቅጂዎች ጋር ተገናዝበው ሙሉ በሙሉ ተስተካክለዋል።"
                    else
                        "All 1,017 Synaxarium Arke strophes across the entire liturgical year have been audited and corrected to match authentic church manuscripts.",
                )
                WhatsNewItem(
                    numeral = if (isAmharic) "፪" else "2",
                    title = if (isAmharic) "ውዳሴ አምላክ በአማርኛ ተካቷል" else "Wudase Amlak Prayer Book Added",
                    description = if (isAmharic)
                        "ከዘወትር እስከ እሑድ ያሉትን ፰ቱን የውዳሴ አምላክ የጸሎት ክፍሎች በቤተ ክርስቲያን መጻሕፍት (የጸሎት መጻሕፍት) ስር ያገኛሉ።"
                    else
                        "The complete 8 daily prayer sections of Wudase Amlak (Praise of God) are now fully available under Church Books.",
                )
                WhatsNewItem(
                    numeral = if (isAmharic) "፫" else "3",
                    title = if (isAmharic) "የእንግሊዝኛ መጽሐፍ ቅዱስ (NKJV)" else "English Bible (NKJV) Wired",
                    description = if (isAmharic)
                        "የእንግሊዝኛ መጽሐፍ ቅዱስ (NKJV) ተካቷል፤ በአማርኛና በእንግሊዝኛ ጎን ለጎን ለማንበብና ለማነጻጸር ያስችላል።"
                    else
                        "The English New King James Version (NKJV) is now wired with parallel reading toggles and search.",
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(if (isAmharic) "እሺ / ተረድቻለሁ" else "Got It")
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenTour) {
                Text(s.whatsNewTour)
            }
        },
    )
}

@Composable
private fun WhatsNewItem(
    numeral: String,
    title: String,
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
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
