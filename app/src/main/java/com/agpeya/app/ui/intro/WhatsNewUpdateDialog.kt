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
    onOpenTour: (() -> Unit)? = null,
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
                    title = if (isAmharic) "የታመቀ የጸሎት ሰሌዳ እና የተሟላ ተደራሽነት" else "Accessible Heatmap & Day Inspector",
                    description = if (isAmharic)
                        "ለአይን ስውራን (TalkBack) በጸሎት ሰሌዳው ላይ የተሟላ የዕለታትና የጾም መረጃ ተካቷል፤ የቀናት መቀያየሪያ ቀስቶች ለመንካት ምቹ ሆነዋል።"
                    else
                        "Full 365-day screen-reader announcements with fasting details, and enlarged inspection navigation chevrons for smooth day browsing.",
                )
                WhatsNewItem(
                    numeral = if (isAmharic) "፪" else "2",
                    title = if (isAmharic) "የገጾች ቅርጽ እና የክፍተት ማስተካከያ" else "Screen Hierarchy & Margins",
                    description = if (isAmharic)
                        "በዋናው ገጽ ላይ የግጻዌ ካርድ ቅርጽ ተስተካክሎ የሰዓታት ካርዱ ዋነኛ ትኩረት እንዲሆን ተደርጓል፤ በሁሉም የማስተካከያ ገጾች ላይ የጎን ክፍተቶች በወጥ መልክ ወደ ፳፬ ዲፒ ተስተካክለዋል።"
                    else
                        "Refactored the Home Gitsawe card so prayer hours reign supreme, and standardized horizontal margins across all settings to 24dp.",
                )
                WhatsNewItem(
                    numeral = if (isAmharic) "፫" else "3",
                    title = if (isAmharic) "የቤተ ክርስቲያን ሥርዓት ምልክቶችና የንባብ ማሻሻያ" else "Liturgical Symbols & Reading Polish",
                    description = if (isAmharic)
                        "በውዳሴ ማርያምና በመዝሙረ ዳዊት ላይ የቤተክርስቲያንና የመጽሐፍ ቅዱስ ሥርዓት ምልክቶች ተተክተዋል፤ በፍለጋ እና በንባብ ገጽ ላይ ያሉ ክፍተቶች ተስተካክለዋል።"
                    else
                        "Replaced secular icons on Wudase Maryam and Psalter with liturgical Church and codex emblems, and smoothed search debounce typing.",
                )
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
