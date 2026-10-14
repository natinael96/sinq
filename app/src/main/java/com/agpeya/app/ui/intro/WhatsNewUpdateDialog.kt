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
                    title = if (isAmharic) "የኢትዮጵያ ንብ ሰም ጧፍ (ቱዋፍ)" else "Ethiopian Beeswax Candle",
                    description = if (isAmharic)
                        "የዕለቱ የጸሎት ጧፍ በኢትዮጵያ ቤተ ክርስቲያን የንብ ሰም ተፈጥሯዊ ቀለም፣ የሰም ኩሬ እና በእርጋታ በሚወዛወዝ ህያው ነበልባል ተሻሽሏል።"
                    else
                        "Reimagined the daily prayer candle with authentic Ethiopian church beeswax (ቱዋፍ), glowing wick ember, and gentle stillness flame physics.",
                )
                WhatsNewItem(
                    numeral = if (isAmharic) "፪" else "2",
                    title = if (isAmharic) "የብራና ሰሌዳ የጸሎት ግጻዌ" else "Sacred Manuscript Folio Heatmap",
                    description = if (isAmharic)
                        "የመነሻ እና የጉዞ የጸሎት ግጻዌ በባህላዊ የብራና ሰሌዳ ክፈፍ፣ የማዕዘን ፨ ምልክቶች፣ የጾም ነጥብ (●) እና የተበሳሱ ጠርዞች ተውቧል።"
                    else
                        "Prayer heatmaps on Home and Journey are enclosed in a traditional manuscript folio with gold fillet framing, sacred dot (●), and perforated fasting edges.",
                )
                WhatsNewItem(
                    numeral = if (isAmharic) "፫" else "3",
                    title = if (isAmharic) "የተሟላ የዕለታት መረጃና የታመቀ ግጻዌ" else "Compact Heatmap & Day Inspection",
                    description = if (isAmharic)
                        "የዓመቱን ግጻዌ ቁመት በማመቅ ፭-፮ ወራትን በአንድ ጊዜ ማሳየት ተችሏል፤ ማንኛውንም ቀን በመጫን የተጸለዩትን ጸሎታትና የጾም ስሞችን መመልከት ይቻላል።"
                    else
                        "Lowered Journey heatmap height by 40% showing 5–6 months at once; tap any day to inspect canonical hours, habits, and fast details.",
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
