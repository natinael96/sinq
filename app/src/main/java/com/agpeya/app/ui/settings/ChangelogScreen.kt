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
        version = "1.5.0",
        title = "Ready for the Play Store",
        titleAm = "ለፕሌይ ስቶር ዝግጁ",
        changes = listOf(
            "Prayer alarms ring as a true alarm notification — same exact timing, snooze, and dismiss, with no background service; alarms on Android 12 are now exact too.",
            "The ግጻዌ widget always shows today and fits any size you resize it to.",
            "A new Licenses & sources page in Settings credits every bundled text and font.",
            "All Amharic text now addresses you in the polite plural, and What's New reads in Amharic.",
            "Restoring a damaged backup can no longer cause crashes, and search uses far less memory.",
        ),
        changesAm = listOf(
            "የጸሎት ማንቂያዎች እንደ እውነተኛ የማንቂያ ማሳወቂያ ይጮኻሉ — ያው ትክክለኛ ሰዓት፣ ማሸለብና መዝጋት፤ በአንድሮይድ 12 ላይም ማንቂያዎች አሁን ትክክለኛ ናቸው።",
            "የግጻዌ ዊጀቱ ሁልጊዜ ዛሬን ያሳያል፤ በማንኛውም መጠን ሲስተካከልም ይመጥናል።",
            "አዲሱ «ፈቃዶች እና ምንጮች» ገጽ በቅንብሮች ውስጥ እያንዳንዱን የታጨቀ ጽሑፍና ቅርጸ-ቁምፊ ያመሰግናል።",
            "ሁሉም የአማርኛ ጽሑፍ አሁን በአክብሮት ብዙ ቁጥር ያናግርዎታል፤ «ምን አዲስ ነገር አለ»ም በአማርኛ ይነበባል።",
            "የተበላሸ ምትኬን መመለስ ከእንግዲህ ብልሽት አያመጣም፤ ፍለጋም በጣም ያነሰ ማህደረ ትውስታ ይጠቀማል።",
        ),
    ),
    ReleaseNote(
        version = "1.4.0",
        title = "The Psalter and the ግጻዌ agree",
        titleAm = "መዝሙረ ዳዊትና ግጻዌው ተስማምተዋል",
        changes = listOf(
            "The Psalter now follows the Ge'ez (LXX) numbering, so መዝሙር ፶ is the Miserere and every ግጻዌ citation opens the psalm it names.",
            "Slide the ግጻዌ page left or right to turn to the neighbouring day; a permanent ዛሬ button returns to today.",
            "Toggling Full Psalms inside an hour no longer crashes the paged reader.",
            "The prayer list's Marian conclusion spelling was corrected (ጳጳሳት).",
            "Manage hours is simpler: the up/down reordering arrows are gone.",
        ),
        changesAm = listOf(
            "መዝሙረ ዳዊት አሁን የግዕዝ (LXX) አቆጣጠርን ይከተላል፤ መዝሙር ፶ የንስሐው መዝሙር ነው፣ እያንዳንዱ የግጻዌ ጥቅስም የሚጠራውን መዝሙር ይከፍታል።",
            "የግጻዌውን ገጽ ወደ ግራ ወይም ወደ ቀኝ በማንሸራተት ወደ አጎራባች ቀን ይሂዱ፤ ቋሚው «ዛሬ» አዝራር ወደ ዛሬ ይመልሳል።",
            "በሰዓት ውስጥ «ሙሉ መዝሙራት»ን መቀያየር ከእንግዲህ ገጽ-በገጽ አንባቢውን አያቋርጥም።",
            "የጸሎት ዝርዝሩ የማርያም መዝጊያ አጻጻፍ ተስተካክሏል (ጳጳሳት)።",
            "«ሰዓታት አስተካክል» ቀለል ብሏል፤ የላይ/ታች መደርደሪያ ቀስቶች ተነስተዋል።",
        ),
    ),
    ReleaseNote(
        version = "1.3.2",
        title = "Bahre Hasab renders correctly",
        titleAm = "ባሕረ ሐሳብ በትክክል ይታያል",
        changes = listOf(
            "The year card shows the real year, evangelist, and Fasika date instead of template text.",
            "Ge'ez numerals beyond 199 render in proper positional notation (e.g. ፳፻፲፰).",
        ),
        changesAm = listOf(
            "የዓመቱ ካርድ በአብነት ጽሑፍ ፈንታ እውነተኛውን ዓመት፣ ወንጌላዊውንና የትንሣኤን ቀን ያሳያል።",
            "ከ199 በላይ የሆኑ የግዕዝ ቁጥሮች በትክክለኛ አጻጻፍ ይታያሉ (ለምሳሌ ፳፻፲፰)።",
        ),
    ),
    ReleaseNote(
        version = "1.3.1",
        title = "A focused, more accessible reading experience",
        titleAm = "የተረጋጋና ይበልጥ ተደራሽ የንባብ ተሞክሮ",
        changes = listOf(
            "The ግጻዌ widget follows the day automatically — today's readings by day, tomorrow's from 19:00.",
            "Scripture, Synaxarium, and Wudase Maryam readers share the reading-alignment setting and tablet-friendly widths.",
            "Bahre Hasab became a live year explorer covering the current Ethiopian year plus the next 25.",
            "Touch targets and widget text sizes were raised to accessibility minimums.",
        ),
        changesAm = listOf(
            "የግጻዌ ዊጀቱ ቀኑን በራሱ ይከተላል — በቀን የዕለቱ ምንባቦች፣ ከ19:00 ጀምሮ ደግሞ የነገው።",
            "የቅዱሳት መጻሕፍት፣ የስንክሳርና የውዳሴ ማርያም አንባቢዎች የንባብ አሰላለፍ ቅንብሩንና ለታብሌት ምቹ ስፋቶችን ይጋራሉ።",
            "ባሕረ ሐሳብ የአሁኑን ዓመትና ቀጣዮቹን ፳፭ ዓመታት የሚሸፍን ሕያው የዓመት አሳሽ ሆኗል።",
            "የመንኪያ ቦታዎችና የዊጀት ጽሑፍ መጠኖች ወደ ተደራሽነት ዝቅተኛ መስፈርቶች ከፍ ብለዋል።",
        ),
    ),
    ReleaseNote(
        version = "1.3.0",
        title = "The complete source-backed ግጻዌ",
        titleAm = "በምንጭ የተደገፈ ሙሉ ግጻዌ",
        changes = listOf(
            "Movable readings for Nineveh, Great Lent, Ascension, and the other computus seasons.",
            "Sunday ግጻዌ readings and hymns, with valid citations opening directly in Scripture.",
            "The printed Bahre Hasab reference table in the Library.",
            "Justified, left, and center text-alignment controls for reading, and a new Sinq launcher mark.",
        ),
        changesAm = listOf(
            "ለጾመ ነነዌ፣ ለዐቢይ ጾም፣ ለዕርገትና ለሌሎቹ በባሕረ ሐሳብ ለሚንቀሳቀሱ ወቅቶች የተመደቡ ምንባቦች።",
            "የሰንበት ግጻዌ ምንባቦችና መዝሙሮች፤ ትክክለኛ ጥቅሶች በቀጥታ በቅዱሳት መጻሕፍት ውስጥ ይከፈታሉ።",
            "የታተመው የባሕረ ሐሳብ ማጣቀሻ ሠንጠረዥ በቤተ መጻሕፍት ውስጥ ገብቷል።",
            "ለንባብ የሁለቱም ጠርዝ፣ የግራና የመሃል አሰላለፍ መቆጣጠሪያዎች፣ እንዲሁም አዲስ የስንቅ ማስጀመሪያ ምልክት።",
        ),
    ),
    ReleaseNote(
        version = "1.2.0",
        title = "The complete fixed-cycle ግጻዌ",
        titleAm = "ሙሉው የቋሚ ዑደት ግጻዌ",
        changes = listOf(
            "Every Ethiopian calendar day now has a fixed-cycle ግጻዌ entry — all 366 month-days, including leap-year Pagumen 6.",
            "The evening office (ሠርክ) appears after ነግህ and ቅዳሴ and is included when sharing the day.",
            "Printed but malformed citations stay readable without becoming broken links.",
        ),
        changesAm = listOf(
            "እያንዳንዱ የኢትዮጵያ ዘመን አቆጣጠር ቀን አሁን የቋሚ ዑደት ግጻዌ አለው — ሁሉም 366 ቀናት፣ የዘመነ ዮሐንስ ጳጉሜን 6ን ጨምሮ።",
            "የሠርክ ሥርዓት ከነግህና ከቅዳሴ በኋላ ይታያል፤ ቀኑን ሲያጋሩም ይካተታል።",
            "የታተሙ ግን የተዛቡ ጥቅሶች የተሰበሩ አገናኞች ሳይሆኑ ተነባቢ ሆነው ይቆያሉ።",
        ),
    ),
    ReleaseNote(
        version = "1.1.1",
        title = "Clearer prayer levels",
        titleAm = "ይበልጥ ግልጽ የጸሎት ደረጃዎች",
        changes = listOf(
            "Every prayer level now explains exactly how many Psalms it includes.",
            "Descriptions appear directly in the compact level selector in Amharic or English.",
        ),
        changesAm = listOf(
            "እያንዳንዱ የጸሎት ደረጃ ስንት መዝሙራት እንደሚያካትት አሁን በትክክል ያብራራል።",
            "መግለጫዎቹ በደረጃ መምረጫው ውስጥ በአማርኛ ወይም በእንግሊዝኛ በቀጥታ ይታያሉ።",
        ),
    ),
    ReleaseNote(
        version = "1.1.0",
        title = "A cleaner reading journey",
        titleAm = "ይበልጥ የጠራ የንባብ ጉዞ",
        changes = listOf(
            "Journey and Gitsawe pages are now denser, clearer, and easier to scan.",
            "Reading titles adapt to narrow screens and larger accessibility text.",
            "Share readings as clean text or paginated images without cutting off long passages.",
            "Save reading images directly to the gallery on Android 10 and newer.",
            "Backups now include layouts, custom modes and hours, preferences, and reminder settings.",
            "Date rollover, reminder reliability, touch targets, and private-data protection were improved.",
        ),
        changesAm = listOf(
            "የጉዞና የግጻዌ ገጾች አሁን የተሰበሰቡ፣ ግልጽና በቀላሉ የሚቃኙ ናቸው።",
            "የንባብ ርዕሶች ለጠባብ ማያ ገጾችና ለትልቅ የተደራሽነት ጽሑፍ ይስማማሉ።",
            "ረጅም ክፍሎች ሳይቆረጡ ምንባቦችን እንደ ንጹህ ጽሑፍ ወይም ገጽ-በገጽ ምስሎች ያጋሩ።",
            "በአንድሮይድ 10 እና ከዚያ በላይ የንባብ ምስሎችን በቀጥታ ወደ ጋለሪ ያስቀምጡ።",
            "ምትኬዎች አሁን አቀማመጦችን፣ ብጁ ሁነታዎችንና ሰዓታትን፣ ምርጫዎችንና የማስታወሻ ቅንብሮችን ያካትታሉ።",
            "የቀን መሸጋገር፣ የማስታወሻ አስተማማኝነት፣ የመንኪያ ቦታዎችና የግል መረጃ ጥበቃ ተሻሽለዋል።",
        ),
    ),
    ReleaseNote(
        version = "1.0.5",
        title = "More dependable reminders",
        titleAm = "ይበልጥ አስተማማኝ ማስታወሻዎች",
        changes = listOf(
            "Supporting reminders use notification-friendly alarms instead of unnecessary exact alarms.",
            "Prayer alarms retain precise scheduling when the user explicitly creates them.",
        ),
        changesAm = listOf(
            "ረዳት ማስታወሻዎች አላስፈላጊ ትክክለኛ-ሰዓት ማንቂያዎችን ሳይሆን ለማሳወቂያ ተስማሚ ማንቂያዎችን ይጠቀማሉ።",
            "የጸሎት ማንቂያዎች ራስዎ ሲፈጥሯቸው ትክክለኛ የሰዓት አያያዛቸውን ይጠብቃሉ።",
        ),
    ),
    ReleaseNote(
        version = "1.0.4",
        title = "Library and Journey fixes",
        titleAm = "የቤተ መጻሕፍትና የጉዞ እርማቶች",
        changes = listOf(
            "Improved navigation and presentation across the Library and Journey areas.",
        ),
        changesAm = listOf(
            "በቤተ መጻሕፍትና በጉዞ ክፍሎች የተሻሻለ አሰሳና አቀራረብ።",
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
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
