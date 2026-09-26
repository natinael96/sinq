package com.agpeya.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.Spacing

/** One release, carrying both languages; the screen picks by [Strings.isAmharic]. */
private data class ReleaseNote(
    val version: String,
    val title: String,
    val titleAm: String,
    val changes: List<String>,
    val changesAm: List<String>,
)

private val releaseHistory = listOf(
    ReleaseNote(
        version = "1.0.0",
        title = "Official Google Play release",
        titleAm = "የመጀመሪያው ይፋዊ የGoogle Play እትም",
        changes = listOf(
            "Complete liturgical corpus: 32 canonical Melkea hymns, 25-office Seatat (Horologium) with speaker roles, and 181 Mahlet feast orders.",
            "Canonical scriptures: Full 81-book Ethiopian Orthodox Bible, complete Psalter (መዝሙረ ዳዊት), and liturgical calendar lectionary (ግጻዌ).",
            "Refined liturgical reader: rubrication in traditional red ink, speaker indicators, anatomical focus kickers, custom Ge'ez typography, and offline prayers.",
            "Optimized for Android 16 (targetSdk 36): built on the latest APIs for security, battery life, and performance.",
            "Google Play readiness: in-app privacy policy access, rating and feedback options, and notification management.",
        ),
        changesAm = listOf(
            "የተሟላ የጸሎትና የምስጋና ሥርዓት፦ ፴፪ቱ መልክአ መልክዕ፣ ፳፭ቱ የሰዓታት ቢጋር ከአንባቢና መላሽ ድምፅ ጋር፣ እንዲሁም ፩፻፹፩ የበዓላት ማኅሌት ሥርዓት።",
            "ቀኖናዊ ቅዱሳት መጻሕፍት፦ ፹፩ዱ መጽሐፍ ቅዱስ፣ ሙሉ መዝሙረ ዳዊት፣ እና ዓመታዊው የንባብ ግጻዌ።",
            "የተዋበ የንባብ ገጽ፦ ባህላዊ የቀይ ቀለም ጽሕፈት (ቀይ ጽሑፍ)፣ የስምሪት አመልካቾች፣ ግዕዝ ፊደላትና ከመስመር ውጭ የሚሠራ።",
            "ለአንድሮይድ 16 (targetSdk 36) የተዘጋጀ፦ ለአዳዲስ ስልኮች ፍጥነት፣ የባትሪ ቆጣቢነትና ደህንነት የተሻሻለ።",
            "የGoogle Play ዝግጁነት፦ የግላዊነት ፖሊሲ፣ ደረጃ የመስጠትና ግብረመልስ አማራጮች።",
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onBack: () -> Unit, onOpenTour: () -> Unit = {}) {
    val s = LocalStrings.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.whatsNew, onBack = onBack) },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "tour") {
                com.agpeya.app.ui.common.NavRow(s.whatsNewTour, onOpenTour)
            }
            items(releaseHistory, key = { it.version }) { release ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(
                            text = "v${release.version}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = if (s.isAmharic) release.titleAm else release.title,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        val changes = if (s.isAmharic) release.changesAm else release.changes
                        changes.forEach { change ->
                            Text(
                                text = "•  $change",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
