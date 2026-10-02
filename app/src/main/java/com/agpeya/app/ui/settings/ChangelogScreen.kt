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
        version = "1.5.1",
        title = "Guaranteed lockscreen quote notifications and streamlined home layout",
        titleAm = "አስተማማኝ የመቆለፊያ ገጽ የአበው ምክር እና የቀለለ የመነሻ ገጽ ገጽታ",
        changes = listOf(
            "Configured guaranteed lockscreen visibility for daily quote notifications across all Android devices.",
            "Enabled daily quote lockscreen notification by default and automated startup sync.",
            "Removed annual reading plan box from home screen to restore spacious liturgical proportions to Now and Gitsawe cards.",
        ),
        changesAm = listOf(
            "የአበው ምክር ማሳወቂያ በሁሉም የአንድሮይድ ስልኮች መቆለፊያ ገጽ ላይ ያለምንም እንቅፋት እንዲታይ ተደርጓል።",
            "የአበው ምክር የመቆለፊያ ገጽ ማሳወቂያ በነባሪነት የበራ እንዲሆንና አፑ ሲከፈት ወዲያው እንዲዘጋጅ ተደርጓል።",
            "የዓመታዊ ንባብ ሳጥን ከመነሻ ገጽ እንዲነሳ ተደርጎ ለሰዓታትና ለግጻዌ ካርዶች ሰፊና ምቹ ቦታ ተሰጥቷል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.0",
        title = "Desert Fathers Daily Quotes, Lockscreen widgets, and privacy-first notifications",
        titleAm = "የአበው ምክር ዕለታዊ ቃል፣ የመቆለፊያ ገጽ መተግበሪያ (Widget) እና አስተማማኝ ማሳወቂያዎች",
        changes = listOf(
            "Added 844 Desert Fathers sayings (Apophthegmata Patrum) with continuous multi-year rotation.",
            "Introduced home and lockscreen widget for daily patristic contemplation.",
            "Configured lockscreen visibility policy: Daily Quote and Gitsawe visible; alarms and habits kept private.",
            "Added hairline quote row on Home screen opening a rich contemplation sheet with sharing and journaling.",
            "Customizable daily quote notification schedule and toggle in Settings.",
            "Refined reading plan overview with visual section badges and bookmark indicators.",
        ),
        changesAm = listOf(
            "፰፻፵፬ የአበው ምክሮች (ዜና አበው) በዓመታት ዑደት እንዳይደጋገሙ ተደርገው ተካተዋል።",
            "በመነሻ እና በመቆለፊያ ገጽ ላይ የሚቀመጥ ዕለታዊ የአበው ምክር መተግበሪያ (Widget) ተዘጋጅቷል።",
            "የመቆለፊያ ገጽ ማሳወቂያዎች ደህንነት ተስተካክሏል፦ የአበው ምክር እና ግጻዌ ብቻ በመቆለፊያ ገጽ ይታያሉ።",
            "በዋናው ገጽ ላይ የአበው ምክር ስስ መስመር ተካቷል፤ ሲነካም ቃሉን፣ ማጋሪያ እና የማስታወሻ መጻፊያ ያቀርባል።",
            "በቅንብሮች ውስጥ የአበው ምክር ማሳወቂያ ሰዓት እና ማብሪያ/ማጥፊያ ተካቷል።",
            "የንባብ ገጽ ዝርዝር በምዕራፍ መለያዎችና በዕልባት ምልክቶች ይበልጥ ተሻሽሏል።",
        ),
    ),
    ReleaseNote(
        version = "1.4.2",
        title = "Sinksar punctuation preference, clean prayer list division, and text corrections",
        titleAm = "የስንክሳር ሥርዓተ ነጥብ ምርጫ፣ የቀለለ የጸሎት ዝርዝር እና የጽሑፍ ማስተካከያ",
        changes = listOf(
            "Added Sinksar word-spacing colon (፡) toggle in Settings under Reading & Appearance.",
            "Replaced Living and Departed headers in Prayer List with a clean dividing line.",
            "Corrected commemoration alignment for Meskerem 21 in Sinksar.",
        ),
        changesAm = listOf(
            "በቅንብሮች ውስጥ የስንክሳር ሁለት ነጥብ (፡) ማብሪያና ማጥፊያ ተካቷል።",
            "በጸሎት ዝርዝር ውስጥ የነበሩትን አርዕስቶች በማስቀረት በስሱ መስመር ተከፍለዋል።",
            "የመስከረም ፳፩ የስንክሳር ምንባብ ማስተካከያ ተደርጓል።",
        ),
    ),
    ReleaseNote(
        version = "1.4.1",
        title = "Streamlined settings hierarchy and home screen alert choice",
        titleAm = "የቅንብሮች አደረጃጀት ማሻሻያ እና በዋናው ገጽ ላይ የቀረበ የማንቂያ ምርጫ",
        changes = listOf(
            "Unified Settings into Appearance & Reading, Prayer & Reminders, and Data & About.",
            "Inlined font picker dialog, text size stepper, and prayer level selector.",
            "Surfaced 3-option alert style (Alarm, Vibrate, Notification) on Home and in Settings.",
        ),
        changesAm = listOf(
            "ቅንብሮች በ3 ግልጽ ክፍሎች (ንባብ፣ ጸሎትና ማስታወሻ፣ መረጃ) ተደራጅተዋል።",
            "የፊደል መምረጫ፣ የፊደል መጠን ማስተካከያ እና የሰዓታት መጠን በቀጥታ በቅንብሮች ገጽ ተካተዋል።",
            "የ3 አማራጮች የማንቂያ ምርጫ (ደወል፣ ንዝረት፣ ማሳወቂያ) በዋናው ገጽና በቅንብሮች ተካቷል።",
        ),
    ),
    ReleaseNote(
        version = "1.4.0",
        title = "Punctuation preservation, top bar language selector, and reminder setup",
        titleAm = "የሥርዓተ ነጥብ ጥበቃ፣ የቋንቋ መምረጫ ወደላይ ማዛወር እና የማስታወሻ ማስተካከያ",
        changes = listOf(
            "Preserved traditional Ethiopic punctuation and wordspaces across Sinksar and Melkea hymns.",
            "Relocated Amharic and Ge'ez language toggles to the top app bar.",
            "Streamlined onboarding into a single overview and profile setup page.",
            "Added reminder verification sheet and 3-choice alert styling (Ringing, Vibrate, Notification).",
        ),
        changesAm = listOf(
            "በስንክሳርና በመልክአ መልክዕ ጽሑፎች ላይ የነበሩ የኢትዮጵያ ሥርዓተ ነጥቦችና ቃላት መለያዎች ተጠብቀዋል።",
            "የአማርኛና የግዕዝ ቋንቋ መቀየሪያዎች ወደ ላይኛው አርዕስት አሞሌ ተዛውረዋል።",
            "የመጀመሪያ ገጽ ቅንብር ወደ አንድ ቀላል ገጽ ተጠቃሏል።",
            "የማስታወሻ ፈቃዶች ማረጋገጫ እና የ3 አማራጮች የማንቂያ ምርጫ ተካቷል።",
        ),
    ),
    ReleaseNote(
        version = "1.3.2",
        title = "Reader stepper refactoring and interactive modifier trimming",
        titleAm = "የንባብ ገጽ መቆጣጠሪያ ማስተካከል እና የአላስፈላጊ ማሻሻያዎች ቅነሳ",
        changes = listOf(
            "Inlined prayer hour stepper directly into reader, simplifying component hierarchy.",
            "Trimmed redundant sizing modifiers across buttons and language toggles.",
        ),
        changesAm = listOf(
            "የሰዓታት ንባብ መቆጣጠሪያው በቀጥታ በዋናው ገጽ እንዲካተት ተደርጓል።",
            "በአዝራሮችና በቋንቋ መቀየሪያዎች ላይ የነበሩ ድጋሚ ማስተካከያዎች ተወግደዋል።",
        ),
    ),
    ReleaseNote(
        version = "1.3.1",
        title = "UI/UX accessibility and standardized button design system",
        titleAm = "የተደራሽነት ማሻሻያ እና የተቀናጀ የአዝራሮች ስርዓት",
        changes = listOf(
            "Standardized button design system (SinqPrimaryButton, SinqOutlinedButton, SinqDestructiveButton, SinqStepperButton).",
            "Enforced 48dp minimum interactive touch targets across toggles and steppers.",
            "Elevated primary actions in forms and confirmation dialogs with semantic error styling for deletions.",
            "Replaced raw text glyphs with accessible vector Material icons.",
        ),
        changesAm = listOf(
            "የተቀናጀ የአዝራር ንድፍ ስርዓት (ዋና፣ የተሰመረባቸው፣ የማስጠንቀቂያ እና የመቆጣጠሪያ አዝራሮች)።",
            "ለመንካት ምቹ የሆኑ የ48dp ዝቅተኛ የንክኪ ስፋት መስፈርቶች ተተግብረዋል።",
            "በመገናኛ ሳጥኖችና ቅጾች ውስጥ ዋና ዋና አዝራሮች ጎልተው እንዲታዩና ማጥፋት በቀይ ቀለም እንዲለይ ተደርጓል።",
            "የጽሑፍ ምልክቶች በይፋዊና ውብ በሆኑ የቬክተር ምስሎች (Material Icons) ተተክተዋል።",
        ),
    ),
    ReleaseNote(
        version = "1.3.0",
        title = "Unified reader navigation and embedded Melkea hymns",
        titleAm = "የተቀናጀ የንባብ ዳሰሳ እና የመልክዕ ጸሎታት ውህደት",
        changes = listOf(
            "Unified single-line navigation bar across Wudase Maryam and Sinksar with horizontal portion track.",
            "Integrated Melkea Maryam and Melkea Yesus directly into Wudase Maryam for seamless reading.",
            "Minimalist single-button edition toggle for Amharic and Ge'ez.",
            "Preserved traditional liturgical Ge'ez punctuation and stanza numbering across Melkea hymns.",
        ),
        changesAm = listOf(
            "በውዳሴ ማርያምና በስንክሳር የተስተካከለ፣ በአንድ መስመር የተቀናጀ እና የሚንሸራሸር የክፍል መምረጫ ባር።",
            "መልክአ ማርያም እና መልክአ ኢየሱስ በቀጥታ በውዳሴ ማርያም የንባብ ገጽ ውስጥ ተካተዋል።",
            "በቀላልና ውብ መልክ የተቀየሰ የግእዝና የአማርኛ መቀየሪያ ነጠላ አዝራር።",
            "በመልክአ መልክዕ ጸሎታት ውስጥ ትክክለኛ የግእዝ ስርዓተ ነጥቦች እና የቁጥር አሰካክ ተሟልተዋል።",
        ),
    ),
    ReleaseNote(
        version = "1.2.1",
        title = "Codebase refinement and performance optimizations",
        titleAm = "የኮድ ማፅዳት እና የአፈጻጸም ማሻሻያ",
        changes = listOf(
            "Removed unused legacy code scaffolding to streamline app footprint.",
            "Optimized numeric input parsing and regex caching across settings.",
        ),
        changesAm = listOf(
            "የመተግበሪያውን ቅልጥፍና ለመጨመር ያገለገሉ አላስፈላጊ ኮዶች ተወግደዋል።",
            "በቅንብሮች ውስጥ የቁጥር ግብዓት ትንተና እና ማህደረ-ትውስታ አጠቃቀም ተሻሽሏል።",
        ),
    ),
    ReleaseNote(
        version = "1.2.0",
        title = "Bilingual Sinksar across all 366 days",
        titleAm = "የተሟላ የ፫፻፷፮ ቀናት ስንክሳር በአማርኛና በግእዝ",
        changes = listOf(
            "Complete aligned bilingual Sinksar across all 366 days of the Ethiopian year in both Amharic and Ge'ez.",
            "Commemorations with numbered narrative paragraphs, Arke salutations, and liturgical rubrication.",
            "Integrated into the home lectionary and library with full search support and bookmarks.",
        ),
        changesAm = listOf(
            "የተሟላ የ፫፻፷፮ቱ ቀናት ስንክሳር በአማርኛና በግእዝ ትይዩ እትሞች።",
            "በቁጥር የተከፋፈሉ ታሪኮች፣ አርኬዎች፣ እና የቀይ ቀለም ስሞች ያካተተ የተሟላ የንባብ ገጽ።",
            "በመነሻ ገጽ፣ በቤተ መጻሕፍት፣ በዕለታዊ ፍለጋ እና በዕልባቶች የተዋሃደ።",
        ),
    ),
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
