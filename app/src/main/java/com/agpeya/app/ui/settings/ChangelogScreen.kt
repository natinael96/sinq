package com.agpeya.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agpeya.app.ui.common.SinqTopBar
import com.agpeya.app.ui.strings.LocalStrings
import com.agpeya.app.ui.theme.IconSize
import com.agpeya.app.ui.theme.Spacing
import com.agpeya.app.ui.theme.inReadingFont

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
        version = "1.6.0",
        title = "Unified manuscript heatmap plate, zero-gap Journey header, annual devotion progress, and liturgical micro-marks",
        titleAm = "የተዋሃደ የብራና ሰሌዳ ሙቀት-ሰንጠረዥ፣ የተስተካከለ የጉዞ ርእስ፣ ዓመታዊ የጸሎት እድገትና የበዓላት ምልክቶች",
        changes = listOf(
            "Eliminated dead space in the Journey screen heatmap header by unifying the year switcher, date picker, and devotion count directly into the manuscript plate.",
            "Added an annual devotion progress bar measuring completed prayer days against total eligible calendar days.",
            "Enriched heatmap cells with sacred illumination micro-crosses (✦) for full prayer days (level 3+), today's radiant gold beacon ring, and subtle month boundary separators.",
            "Expanded compact Home heatmap to 12 weeks with today beacon rings and holy day devotion indicators.",
        ),
        changesAm = listOf(
            "በጉዞ ገጽ ላይ የነበረው ክፍተት ተወግዶ የዓመተ ምሕረት መቀያየሪያው፣ የቀን መምረጫውና የተጸለዩ ቀናት ቍጥር በብራና ሰሌዳው አናት ላይ በአንድነት ተካተዋል።",
            "በዓመቱ ውስጥ ከተጸለዩት ቀናት አንጻር አጠቃላይ የጸሎት እድገትን የሚያሳይ የወርቅ መስመር ተካቷል።",
            "በጸሎት ሰሌዳው ላይ ሙሉ የጸሎት ቀናትን የሚያመላክቱ የመስቀል ቅርጾች (✦)፣ የዛሬ ቀን ማድመቂያና የወራት መለያ መስመሮች ተጨምረዋል።",
            "በዋናው ገጽ ላይ ያለው አጭር የጸሎት ሰሌዳ ወደ ፲፪ ሳምንታት አድጎ የዛሬና የሙሉ ጸሎት ቀናት መለያዎችን አካቷል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.14",
        title = "Comprehensive UI, design, and accessibility polish across home hero, habit heatmaps, reading, and search",
        titleAm = "የገጾች ቅርጽ፣ የተደራሽነት እና የውበት ማሻሻያ በዋናው፣ በጉዞ፣ በንባብና በፍለጋ ገጾች ላይ",
        changes = listOf(
            "Refactored Gitsawe card on Home to an accented Sinq card, allowing the prayer Now card to reign as the singular visual hero.",
            "Standardized horizontal page margins across settings and customization screens to the canonical Spacing.screen (24dp) design token.",
            "Added rich TalkBack accessibility descriptions to both compact and year heatmaps, and enlarged inspection chevrons to 44dp.",
            "Replaced secular icons on Wudase Maryam and LXX Psalter with authentic liturgical Church and codex emblems.",
            "Guarded center dots in the compact heatmap to eliminate visual moiré while keeping the full-year view compact.",
            "Eliminated search result loading flicker during debounce typing, merged TalkBack announcements for Ge'ez numeral gutters, and scaled reading divider air dynamically.",
        ),
        changesAm = listOf(
            "በዋናው ገጽ ላይ የግጻዌ ካርድ ቅርጽ ተስተካክሎ የሰዓታት ካርዱ ዋነኛ ትኩረት እንዲሆን ተደርጓል።",
            "በሁሉም የማስተካከያ ገጾች ላይ የጎን ክፍተቶች በወጥ መልክ ወደ ፳፬ ዲፒ (Spacing.screen) ተስተካክለዋል።",
            "ለአይን ስውራን ተጠቃሚዎች (TalkBack) በጸሎት ሰሌዳው ላይ የተሟላ የዕለታትና የጾም መረጃ ተካቷል፤ የቀናት መቀያየሪያ ቀስቶችም ለመንካት አመቺ ሆነዋል።",
            "በውዳሴ ማርያምና በመዝሙረ ዳዊት ላይ የነበሩ ምልክቶች በቤተክርስቲያንና በመጽሐፍ ቅዱስ ሥርዓት ምልክቶች ተተክተዋል።",
            "በታመቀው የዓመት የጸሎት ሰሌዳ ላይ የነጥቦች መደራረብ ተስተካክሎ ይበልጥ ጥርት ብሎ እንዲታይ ተደርጓል።",
            "በፍለጋ ገጽ ላይ በሚተየብበት ወቅት የውጤቶች መጥፋት ተስተካክሏል፤ የንባብ ገጽ መስመር ክፍተቶችም በትልልቅ ፊደላት ላይ እንዲመጣጠኑ ተደርገዋል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.13",
        title = "Compact Journey page prayer strips and habit rows, button-like Today manage control, and animated dawn horizon logo on Memento Mori",
        titleAm = "የታመቀ የጉዞ ገጽ የሰዓታትና የሥርዓታት ረድፎች፣ ጎልቶ የሚታይ የማስተካከያ አዝራር፣ እና በሞትን አስብ ገጽ ላይ የተካተተ የንጋት አርማና መስቀል",
        changes = listOf(
            "Compacted prayer hour strips to a sleek 34dp height with tight vertical padding to reduce screen gaps.",
            "Compacted habit rows to 36dp height with crisp 16dp status rings and tighter text margins.",
            "Transformed the Today section 'Manage' text into a noticeable, subtle button pill with liturgical gold accent and bold typography.",
            "Enhanced the Memento Mori opening breath with the animated dawn horizon line, rising sun dome, central cross, and 6 unfurling sun rays matching the Sinq emblem.",
        ),
        changesAm = listOf(
            "በጉዞ ገጽ ላይ የሰዓታት ጸሎት ረድፎች ቁመት ወደ ፴፬ ዲፒ በመቀነስ አላስፈላጊ ክፍተቶች ታምቀዋል።",
            "የዕለታዊ ሥርዓታት ረድፎች ቁመት ወደ ፴፮ ዲፒ እና ክብ መመልከቻው ወደ ፲፮ ዲፒ በመቀነስ ይበልጥ የታመቀ ሆኗል።",
            "በዛሬው ክፍል ስር የሚገኘው 'ማስተካከያ' ጎልቶ በሚታይና በሚያምር የወርቅ ቃና ባለው የአዝራር ቅርጽ ተሻሽሏል።",
            "የመክፈቻው 'ሞትን አስብ' ገጽ በስንቅ የንጋት አድማስ መስመር፣ የፀሐይ መውጣት፣ የመስቀል ከፍታ እና የጨረሮች እንቅስቃሴ በሚያምር አኒሜሽን ተውቧል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.12",
        title = "Ethiopian Beeswax candle in gentle stillness, Sacred Manuscript Folio heatmap plate with fasting dot and perforations, and compact interactive day inspection",
        titleAm = "የኢትዮጵያ ንብ ሰም ጧፍ በእርጋታ፣ የብራና ሰሌዳ የጸሎት ግጻዌ ከጾም ነጥብና ከተበሳሱ ጠርዞች ጋር፣ እና የተሟላ የዕለታት መረጃ",
        changes = listOf(
            "Reimagined the daily prayer candle with Ethiopian church beeswax (ቱዋፍ) organic depth, crater pool, glowing ember tip, and gentle stillness harmonic sway and convection breathing.",
            "Enclosed Home and Journey prayer heatmaps in a traditional Ethiopian manuscript folio (የብራና ሰሌዳ) with gold fillet framing, corner rosettes (፨), center sacred dot (●), and perforated/dashed fasting edges.",
            "Selecting any day on the Journey year heatmap immediately opens the day inspection card showing canonical hours prayed, habits completed, active fasts, and written reflections with day navigation chevrons.",
            "Streamlined Journey heatmap footprint to a compact 11dp cell pitch, lowering grid height by ~40% and surfacing 5–6 months on screen simultaneously.",
        ),
        changesAm = listOf(
            "የዕለቱ የጸሎት ጧፍ በኢትዮጵያ ቤተ ክርስቲያን የንብ ሰም (ቱዋፍ) ቅርጽ፣ የሰም ኩሬ፣ የሚበራ የፈትል ፍም እና በእርጋታ በሚወዛወዝ ህያው ነበልባል ተሻሽሏል።",
            "የመነሻ እና የጉዞ የጸሎት ግጻዌ በባህላዊ የብራና ሰሌዳ ክፈፍ፣ የማዕዘን ፨ ምልክቶች፣ የጾም ነጥብ (●) እና የተበሳሱ ጠርዞች ተውቧል።",
            "በዓመቱ ግጻዌ ላይ ማንኛውንም ቀን በመጫን የተጸለዩትን የሰዓታት ጸሎታት፣ የተፈጸሙ መንፈሳዊ ልምዶችን፣ የጾም ስሞችን እና የማስታወሻ ጽሑፎችን በቀጥታ መመልከት ይቻላል።",
            "የጉዞ ግጻዌ ቁመቱ በ፵% በመቀነሱ ፭ እስከ ፮ ወራትን በአንድ ጊዜ በስክሪኑ ላይ ያለ መጣበብ በግልጽ ለማየት ያስችላል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.11",
        title = "Synaxarium Arke correction, Wudase Amlak prayer book, and English NKJV Bible integration",
        titleAm = "የስንክሳር አርኬ እርማት፣ የውዳሴ አምላክ የጸሎት መጽሐፍ እና የእንግሊዝኛ መጽሐፍ ቅዱስ (NKJV) ማካተት",
        changes = listOf(
            "Audited, verified, and corrected all 1,017 Synaxarium Arke strophes across the entire liturgical year against authentic church manuscripts.",
            "Added the complete 8 daily prayer sections of Wudase Amlak (ውዳሴ አምላክ በአማርኛ) under the Church Books (የጸሎት መጻሕፍት) shelf.",
            "Wired English New King James Version (NKJV) Bible translation with bilingual parallel reader toggles, verse selection, and global search.",
            "Added a What's New update dialog highlighting major liturgical content additions upon version update.",
        ),
        changesAm = listOf(
            "በዓመቱ ሙሉ የሚገኙት ፩ሺህ፲፯ቱ የስንክሳር አርኬዎች ከትክክለኛ የቤተ ክርስቲያን የብራና ቅጂዎች ጋር ተገናዝበው ሙሉ በሙሉ ተስተካክለዋል።",
            "ከዘወትር እስከ እሑድ ያሉትን ፰ቱን የውዳሴ አምላክ የጸሎት ክፍሎች በቤተ ክርስቲያን መጻሕፍት (የጸሎት መጻሕፍት) ስር ተካተዋል።",
            "የእንግሊዝኛ መጽሐፍ ቅዱስ (NKJV) ተካቷል፤ በአማርኛና በእንግሊዝኛ ጎን ለጎን ለማንበብና ለማነጻጸር ያስችላል።",
            "በአዳዲስ እትሞች የተካተቱ ዋና ዋና መንፈሳዊ ይዘቶችን የሚያሳይ 'ምን አዲስ ነገር አለ' ማሳወቂያ ተካቷል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.10",
        title = "Future date checklist reminders, Scripture Psalms reader with highlights, and web guide styling",
        titleAm = "የወደፊት ቀናት የማረጋገጫ ማስታወሻዎች፣ የመዝሙረ ዳዊት ቅዱሳት መጻሕፍት ንባብ ከቀለም ምልክት ጋር፣ እና የድረ-ገጽ መመሪያ",
        changes = listOf(
            "Restricted future-date journal entries to checklists with liturgical guidance banner, preserving reflections for the present day while supporting advance task planning.",
            "Tapping checklist reminder notifications now deep-links directly to the target entry and date in the Journal.",
            "Accessing Psalms from Books & Scripture now opens the Scripture Reader with verse selection, 4-color highlights, bookmarks, notes, and copying, while devotional prayer retains the classic Psalter.",
            "Polished the web liturgical manual and user guide with responsive sidebar, real-time search, and bilingual navigation.",
        ),
        changesAm = listOf(
            "የወደፊት ቀናት ማስታወሻዎች ለማረጋገጫ ዝርዝር (Checklist) ብቻ ክፍት ሆነው ተገቢውን መንፈሳዊ መመሪያ እንዲያሳዩ ተደርጓል።",
            "የማረጋገጫ ዝርዝር ማስታወሻ ሲደርስ ማሳወቂያውን በመጫን በቀጥታ ወደ ተዘጋጀለት ቀን እና ማስታወሻ ገጽ ይገባል።",
            "በመጻሕፍት ክፍል የሚገኘው መዝሙረ ዳዊት ልክ እንደ ሌሎች የቅዱሳት መጻሕፍት ክፍሎች በቁጥር መምረጥ፣ በ፬ ቀለማት ማድመቅ፣ ማስታወሻ መያዝ እና መቅዳት እንዲያስችል ተደርጓል።",
            "የድረ-ገጽ የተጠቃሚ መመሪያው የጎን ማውጫ፣ የቀጥታ ፍለጋ እና የሁለቱም ቋንቋዎች ድጋፍ ተሟልቶለታል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.9",
        title = "Home Psalter button polish, Wudase Maryam stability, and web manual redesign",
        titleAm = "የመነሻ ገጽ መዝሙረ ዳዊት አዝራር ማስተካከያ፣ የውዳሴ ማርያም ጥንካሬ እና የተሟላ የድረ-ገጽ መመሪያ",
        changes = listOf(
            "Polished Home screen Psalter button label to always display 'መዝሙረ ዳዊት' alongside the daily psalm range caption.",
            "Hardened Wudase Maryam screen against page index boundary errors and ensured reliable initial section navigation from Home.",
            "Completely redesigned and styled the web User Guide & Liturgical Manual (guide.html) with a responsive 2-column layout, sticky sidebar, real-time search, bilingual toggle, and unified site navigation.",
        ),
        changesAm = listOf(
            "በመነሻ ገጽ ላይ የሚገኘው የመዝሙረ ዳዊት አዝራር ስም በትክክል 'መዝሙረ ዳዊት' እና የዕለቱን ምዕራፍ እንዲያሳይ ተስተካክሏል።",
            "የውዳሴ ማርያም ገጽ ከመነሻ ገጽ ሲከፈት ሊያጋጥም የሚችለው መቋረጥ ተፈትቶ አስተማማኝ እንዲሆን ተደርጓል።",
            "የድረ-ገጽ የተጠቃሚ መመሪያው (guide.html) በሁለት ዓምድ የተደራጀ ዘመናዊ ቅርጽ፣ የቀጥታ ፍለጋ፣ የቋንቋ መቀየሪያና የድረ-ገጽ ማውጫ እንዲኖረው ተደርጎ በድጋሚ ተሰናድቷል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.8",
        title = "Light mode default, Library codex structure, Church books typography polish, and Journal refinement",
        titleAm = "የብርሃናማ ገጽታ ቅድመ-ምርጫ፣ የቤተ መጻሕፍት ቀኖናዊ አደረጃጀት፣ የመጻሕፍት ፊደላት ማስተካከያ እና የማስታወሻ ማሻሻያ",
        changes = listOf(
            "Configured light mode (Sacred Ivory / Parchment) as the app-wide initial default theme on first launch.",
            "Reverted Library screen to its clean canonical single ቅዱሳት መጻሕፍት entry pointing to the full 81-book canonical structure.",
            "Standardized typography across Church book shelves (ShelfRow and BookRow) to clean titleMedium, eliminating bold reading fonts on buttons.",
            "Removed daily feast names from Journal entry cards and headers to keep prayer reflections clean and focused.",
            "Synchronized liturgical user manual (guide.html) with website navigation and site generator.",
        ),
        changesAm = listOf(
            "መተግበሪያው ለመጀመሪያ ጊዜ ሲከፈት በነባሪነት የብርሃናማ (Sacred Ivory) ገጽታ እንዲጠቀም ተደርጓል።",
            "የቤተ መጻሕፍት ገጽ ወደ ነጠላ 'ቅዱሳት መጻሕፍት' መግቢያ ተመልሶ የ፹፩ዱን መጻሕፍት ሙሉ ቀኖና እንዲያሳይ ተደርጓል።",
            "በሌሎች መጻሕፍት መደርደሪያ ላይ የነበሩት ደማቅ የብራና ፊደላት ተስተካክለው ከመተግበሪያው መደበኛ ፊደላት ጋር እንዲጣጣሙ ተደርጓል።",
            "በማስታወሻ ገጽ ላይ የዕለቱን በዓል ስም የማሳየቱ አሠራር ተወግዷል።",
            "የድረ-ገጽ አጠቃቀም መመሪያው (guide.html) ከድረ-ገጹ ማውጫና አገናኞች ጋር ሙሉ በሙሉ ተገናኝቷል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.7",
        title = "Interactive heatmap day inspection card, direct Horologium reader jump, and habit management",
        titleAm = "የዓመቱ የጉዞ ካርታ ዝርዝር መመልከቻ፣ የቀጥታ የሰዓታት ጸሎት ንባብ እና የልማዶች አስተዳደር ማሻሻያ",
        changes = listOf(
            "Added an interactive Day Inspection Card below the year heatmap showing canonical hours, kept habits, active fasts, and a button to view that day's journal entry.",
            "Long-pressing an hour strip chip or tapping a chip in the day inspector opens the Horologium reader directly to that hour's prayers.",
            "Added a discreet አስተካክል (Manage) text button in the Habits section header for quick access to habit configuration.",
            "Added habit cadence guidance advising daily disciplines for habits and Journal checklists for periodic tasks, with optional checklist reminders.",
        ),
        changesAm = listOf(
            "በዓመቱ የጉዞ ካርታ ላይ የተመረጠውን ዕለት የተከናወኑ የሰዓታት ጸሎታት፣ ልማዶች፣ አጽዋማት እና የተጻፈ ማስታወሻ በቀጥታ የሚያሳይ ዝርዝር ካርድ ተዘጋጅቷል።",
            "የሰዓታት መምረጫውን ረዘም ላለ ጊዜ በመጫን ወይም በዝርዝር ካርዱ ላይ ያለውን ሰዓት በመንካት በቀጥታ ወደ ሰዓቱ ጸሎት ንባብ መግባት ይቻላል።",
            "በልማዶች ርዕስ ስር ልማዶችን በቀጥታ ለማስተካከል የሚያስችል 'አስተካክል' የሚል አዝራር ተጨምሯል።",
            "ልማዶች የዕለት ተዕለት እንዲሆኑና አልፎ አልፎ ለሚከናወኑ በማስታወሻ ዝርዝር እንዲጠቀሙ የሚጠቁም መመሪያ እና የማስታወሻ ማሳሰቢያ ተካቷል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.6",
        title = "Unified home hairline dividers, HeroCard feedback in settings, and horizontal journal day strip",
        titleAm = "የተጣጣመ የመነሻ ገጽ መስመሮች፣ የተሻሻለ የአስተያየት መስጫ ገጽ እና የማስታወሻ ዕለታት አግድም ዝውውር",
        changes = listOf(
            "Replaced the 30-day square grid on the Journal screen with an interactive, horizontally scrollable day strip with weekday labels and Ge'ez numerals.",
            "Merged the Today journey row and Desert Father saying on the Home dashboard into a single hairline block sharing a middle divider line.",
            "Adopted the canonical Gitsawe HeroCard design for the Settings feedback card with liturgical contrast across light and dark themes.",
            "Closed accessibility and touch target gaps across Manage Hours, Marks, Penance, Search, About, and Special Habits.",
        ),
        changesAm = listOf(
            "በማስታወሻ ገጽ ላይ የወሩን ዕለታት ወደ ጎን በማሸብለል በቀላሉ ለመመልከትና ወደ ተጻፈበት ማስታወሻ በቀጥታ ለመሄድ የሚያስችል አግድም ዝውውር ተዘጋጅቷል።",
            "የመነሻ ገጽ ላይ 'ዛሬ' እና 'የአበው ምክር' የተባሉት ክፍሎች መስመሮቻቸው ተዋህደው በአንድ ማዕከላዊ መስመር እንዲለያዩ ተደርጓል።",
            "በቅንብሮች ውስጥ የሚገኘው የአስተያየት መስጫ ገጽ በመነሻ ገጽ በሚገኘው የግብጸዋ ካርድ ዲዛይን መሰረት ተስተካክሏል።",
            "የሰዓታት አስተዳደር፣ ምልክቶች፣ የንስሐ እና የፍለጋ ገጾች የንክኪ እና የአጠቃቀም ምቾታቸው ተሻሽሏል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.5",
        title = "Settings feedback card, horizontal journal day strip, and clock picker alignment",
        titleAm = "ጎልቶ የወጣ የአስተያየት መስጫ ገጽ፣ የማስታወሻ ዕለታት አግድም ዝውውር እና የሰዓት መምረጫ ማስተካከያ",
        changes = listOf(
            "Replaced nested settings row with a bold, dedicated liturgical feedback card with direct action button.",
            "Replaced the 30-day square grid on the Journal screen with an interactive, horizontally scrollable day strip with weekday labels and Ge'ez numerals.",
            "Made Ethiopian year heatmap cells directly clickable and added a calendar picker button.",
            "Fixed clock time picker alignment and edge-clipping across all reminder and alarm dialogs.",
        ),
        changesAm = listOf(
            "በቅንብሮች ውስጥ አስተያየት በቀላሉ ለመስጠት የሚያስችል ልዩ፣ ውብና ጎልቶ የሚታይ የወርቅና አረንጓዴ ቀለም መድረክ ተዘጋጅቷል።",
            "በማስታወሻ ገጽ ላይ የወሩን ዕለታት ወደ ጎን በማሸብለል በቀላሉ ለመመልከትና ወደ ተጻፈበት ማስታወሻ በቀጥታ ለመሄድ የሚያስችል አግድም ዝውውር ተዘጋጅቷል።",
            "የዓመቱን የጸሎት ሁኔታ የሚያሳየው ሠንጠረዥ እያንዳንዱ ዕለት ሲነካ እንዲመረጥ ተደርጎ የቀን መቁጠሪያ አዶ ተካቷል።",
            "የማንቂያና የጸሎት ማሳሰቢያ ሰዓት መምረጫው ጠርዝ ሳይቆረጥና ሳይዛነፍ በትክክል እንዲታይ ተስተካክሏል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.4",
        title = "Distraction-free prayer mode, universal reading progress bars, and Mahlet screen redesign",
        titleAm = "ያለ ትኩረት የሚከፋፍል የጸሎት ሁነታ፣ የንባብ ሂደት ማሳያ እና የማኅሌት ገጽ ማሻሻያ",
        changes = listOf(
            "Integrated system bar immersion and soft auto-hide of controls after 3s of stillness or on scroll for Hourly Prayers, Daily Psalms, and Wudase Maryam.",
            "Added subtle 2dp golden reading progress bars across all scripture and devotional reading screens.",
            "Redesigned Mahlet index with header action search, Ethiopian month pills with Ge'ez count badges, and 48dp liturgical service buttons.",
        ),
        changesAm = listOf(
            "ለሰዓታት ጸሎት፣ ለዕለቱ ዳዊት እና ለውዳሴ ማርያም ትኩረትን የሚሰርቁ የስልክ ምልክቶችን (ሰዓት፣ ባትሪ፣ መልዕክቶች) የሚሰውር ሙሉ የጸሎት ሁነታ ተዘጋጅቷል።",
            "በሁሉም የንባብ ክፍሎች (መዝሙረ ዳዊት፣ ስንክሳር፣ መጽሐፍ ቅዱስ፣ ውዳሴ ማርያም፣ መጻሕፍት፣ ግጻዌ) የንባብ ሂደትን የሚያሳይ ረቂቅ ወርቃማ መስመር ተካቷል።",
            "የሥርዓተ ማኅሌት ማውጫ የፍለጋ አዶ በራስጌው ላይ በማድረግ፣ የኢትዮጵያ ወራትን በግዕዝ ቁጥር ባጅ በማስጌጥ እና ለዋዜማና ማኅሌት ምቹ ቁልፎችን በማዘጋጀት ተሻሽሏል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.3",
        title = "Settings hub reorganization, daily quote bookmarks, and UI card refinements",
        titleAm = "የቅንብሮች አደረጃጀት፣ የአበው ምክር ዕልባቶች እና የተሻሻሉ የመረጃ ካርዶች",
        changes = listOf(
            "Reorganized settings into 4 dedicated category hubs (Preferences, Notifications, Records, Appearance) with live status summaries.",
            "Enclosed records and copy format rows into styled SinqCard containers with semantic leading icons.",
            "Added bookmarking for Desert Fathers daily quotes with direct bookmark navigation and journaling.",
            "Added source attribution citing catenabible.com in the daily quotes sheet and share text.",
        ),
        changesAm = listOf(
            "ቅንብሮች በ4 ዋና ዋና ክፍሎች (ምርጫዎች፣ ማሳሰቢያዎች፣ መዝገብ፣ ገጽታ) ከነቀጥታ ሁኔታ መግለጫቸው ተደራጅተዋል።",
            "የመዝገብና የቅዳ ቅንብሮች ረድፎች በመለያ አዶዎችና በካርድ ቅርጽ ውብ ሆነው ተዘጋጅተዋል።",
            "ለዕለታዊ የአበው ምክር የዕልባት ድጋፍ እና በቀጥታ ወደ ማስታወሻ የመጻፍ ዕድል ተጨምሯል።",
            "ለአበው ምክር የድረ-ገጽ ምንጭ (catenabible.com) በመተግበሪያውና በማጋሪያ ጽሑፍ ላይ ተካቷል።",
        ),
    ),
    ReleaseNote(
        version = "1.5.2",
        title = "Compact keyguard widget support for lockscreen customization",
        titleAm = "ለስልክ መቆለፊያ ገጽ የሚስማማ የታመቀ የአበው ምክር መተግበሪያ (Widget)",
        changes = listOf(
            "Configured 2×1 compact cell dimensions and reduced minimum widget height for Samsung One UI and Android keyguard lockscreen editors.",
        ),
        changesAm = listOf(
            "የአበው ምክር መተግበሪያ (Widget) በሳምሰንግ እና በሌሎች ስልኮች የመቆለፊያ ገጽ ላይ በሚገባ እንዲቀመጥ የመጠኑ ልኬት ተስተካክሏል።",
        ),
    ),
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
    val isAmharic = s.isAmharic

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SinqTopBar(title = s.whatsNew, onBack = onBack) },
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // ── Release History Cards ───────────────────────────────────────
            items(releaseHistory, key = { it.version }) { release ->
                val isLatest = release.version == releaseHistory.first().version
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        1.dp,
                        if (isLatest) MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isLatest) MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isLatest) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                ),
                            ) {
                                Text(
                                    text = "v${release.version}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLatest) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
                                )
                            }

                            if (isLatest) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        text = if (isAmharic) "የአሁኑ እትም" else "Current",
                                        style = MaterialTheme.typography.labelSmall.inReadingFont(),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp),
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isAmharic) release.titleAm else release.title,
                            style = MaterialTheme.typography.titleMedium.inReadingFont(),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )

                        val changes = if (isAmharic) release.changesAm else release.changes
                        changes.forEach { change ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 7.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary),
                                )
                                Text(
                                    text = change,
                                    style = MaterialTheme.typography.bodyMedium.inReadingFont(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
