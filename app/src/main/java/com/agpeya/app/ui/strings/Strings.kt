package com.agpeya.app.ui.strings

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * All app-chrome strings in both languages. Prayer content (hour names, psalm
 * text, gospels) is never translated — it stays in Amharic as data.
 */
interface Strings {
    val back: String
    val tabHome: String
    val tabSearch: String
    val tabBookmarks: String
    val tabStreak: String
    val tabSettings: String
    val tabLibrary: String

    val libraryTitle: String
    val librarySubtitle: String
    val scripturesTitle: String
    val scripturesSubtitle: String
    val newTestamentLabel: String
    val chapterUnit: String
    val bookGroupGospels: String
    val bookGroupActs: String
    val bookGroupPaul: String
    val bookGroupCatholic: String
    val bookGroupRevelation: String

    val gitsaweTitle: String
    val gitsaweKicker: String
    val srcDaily: String
    val srcSeasonal: String
    val srcMonthly: String
    val noGitsaweToday: String
    val gitsaweOpenNotAvailable: String
    val gitsaweChangeDay: String
    val ok: String
    val synaxariumTitle: String
    val synaxariumKicker: String
    val noSynaxariumToday: String

    val streaksTitle: String
    val currentStreakLabel: String
    val todayLabel: String
    val habitsHeader: String
    val habitPrayer: String
    val habitChurch: String
    val habitProstrate: String
    val habitBible: String
    val manageHabits: String
    val manageHabitsIntro: String
    val newHabit: String
    val habitNameLabel: String
    val less: String
    val more: String
    val streakCurrent: String
    val streakBest: String
    fun daysUnit(n: Int): String

    val nowPrayer: String
    val continueReading: String
    val hoursHeader: String

    val contents: String
    val readingModeToggle: String
    val bookmarkAction: String
    val highlight: String
    val removeHighlight: String

    val searchHint: String
    val noResults: String
    val recentSearches: String
    val clearAction: String
    val streakReminderTitle: String
    val streakReminderBody: String
    val streakChannelName: String
    val gitsaweReminderTitle: String
    val gitsaweReminderBody: String
    val gitsaweChannelName: String
    val settingsGitsaweReminder: String
    val settingsGitsaweReminderDesc: String
    val settingsStreakReminder: String
    val settingsStreakReminderDesc: String
    val notifDisabledTitle: String
    val notifDisabledBody: String

    val bookmarksTitle: String
    val noBookmarksTitle: String
    val noBookmarksBody: String
    val removeAction: String
    val bookmarkGroupScripture: String
    val bookmarkGroupSynaxarium: String

    val settingsTitle: String
    val appearance: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val keepScreenOn: String
    val keepScreenOnDesc: String
    val languageLabel: String
    val langSystem: String
    val langAmharic: String
    val langEnglish: String
    val customizePrayers: String
    val reminderModes: String
    val about: String
    val alarmSection: String
    val alertSoundVibrate: String
    val alertSoundOnly: String
    val alertVibrateOnly: String
    val alertSilent: String
    val soundLabel: String
    val soundAlarm: String
    val soundRingtone: String
    val soundNotification: String

    // The About page body is deliberately not part of this interface — it ships in English only,
    // so it lives on EnglishStrings alone. `about` above is the Settings row label and stays translated.

    val modesTitle: String
    val startFromAgpeya: String
    val startEmpty: String
    val remindersNotFiring: String
    val deleteModeTitle: String
    fun deleteModeBody(name: String, count: Int): String
    val delete: String
    val cancel: String
    val builtInBadge: String
    fun remindersOn(count: Int): String
    val newModeName: String

    val modeNameLabel: String
    val resetTimes: String
    val addReminder: String
    val prayerLabel: String
    val timeLabel: String
    val daysLabel: String
    val everyDay: String
    val save: String
    val noDaySelected: String
    val daysSummaryDaily: String

    val customizeTitle: String
    val customizeIntro: String
    val resetLayout: String
    val showSection: String
    val hideSection: String
    val moveUp: String
    val moveDown: String

    val alarmPrayerTime: String
    val openPrayer: String
    val dismiss: String
    val reminderReached: String
    val itsTime: String
    val openShort: String
    val donePrompt: String
    val yesAction: String
    val shareAction: String

    // Psalter page + Home library row
    val psalterTitle: String
    val wholePsalter: String
    val dailyPsalms: String
    val comingSoon: String
    val zewotrTselot: String
    val wudaseMariam: String
    val wudaseLangAmharic: String
    val wudaseLangGeez: String
    val contentUnavailable: String
    fun psalmRange(from: Int, to: Int): String
    val snooze: String
    val addPsalm: String
    val choosePsalm: String
    val remove: String
    val manageHours: String
    val newHour: String
    val hourNameLabel: String
    val rename: String
    val manageHoursIntro: String


    // Profile (local only)
    val profileSection: String
    val yourNameLabel: String
    val christianNameLabel: String
    val introNameTitle: String
    val introNameBody: String
    fun greeting(name: String): String

    // First-launch intro
    val introTitle: String
    val introBody: String
    val introOfflineTitle: String
    val introOfflineBody: String
    val introRemindersTitle: String
    val introRemindersBody: String
    val introStreakTitle: String
    val introStreakBody: String
    val introPsalterTitle: String
    val introPsalterBody: String
    val tutorial: String
    val tutorialAskTitle: String
    val tutorialAskBody: String
    val showTutorial: String
    val gotIt: String
    val getStarted: String
    val next: String
    val skip: String

    // Battery-optimization help
    val batteryHelp: String
    val batteryHelpIntro: String
    val batteryStepUnrestrict: String
    val batteryStepUnrestrictBody: String
    val batteryStepAutostart: String
    val batteryStepAutostartBody: String
    val openSettings: String
    val remindersNotFiringTitle: String

    /** Day-of-week short labels, Monday..Sunday (ISO order). */
    val dayLabels: List<String>

    /** Full weekday names, Monday..Sunday (ISO order). */
    val weekdayNames: List<String>

    /** Ethiopian month names, መስከረም..ጳጉሜን (1..13). */
    val ethMonths: List<String>

    /** Ethiopian era suffix (ዓ.ም). */
    val eraSuffix: String

    fun habitsCount(n: Int): String
}

object AmharicStrings : Strings {
    override val back = "ተመለስ"
    override val tabHome = "ቤት"
    override val tabSearch = "ፍለጋ"
    override val tabBookmarks = "ምልክቶች"
    override val tabStreak = "ጉዞ"
    override val tabSettings = "ቅንብር"
    override val tabLibrary = "ቤተ መጻሕፍት"

    override val libraryTitle = "ቤተ መጻሕፍት"
    override val librarySubtitle = "የጸሎትና የቅዱሳት መጻሕፍት ስብስብ"
    override val scripturesTitle = "ቅዱሳት መጻሕፍት"
    override val scripturesSubtitle = "የአዲስ ኪዳን መጻሕፍት"
    override val newTestamentLabel = "አዲስ ኪዳን"
    override val chapterUnit = "ምዕራፍ"
    override val bookGroupGospels = "ወንጌላት"
    override val bookGroupActs = "ግብረ ሐዋርያት"
    override val bookGroupPaul = "መልእክታተ ጳውሎስ"
    override val bookGroupCatholic = "ማኅበራዊ መልእክታት"
    override val bookGroupRevelation = "ራዕይ"

    override val gitsaweTitle = "የዕለቱ ግጻዌ"
    override val gitsaweKicker = "ግጻዌ ዘዕለት"
    override val srcDaily = "ዕለታዊ"
    override val srcSeasonal = "ወቅታዊ"
    override val srcMonthly = "ወርኃዊ"
    override val noGitsaweToday = "ለዛሬ የተመዘገበ ግጻዌ የለም"
    override val gitsaweOpenNotAvailable = "ይህ ምንባብ ገና የለም"
    override val gitsaweChangeDay = "ቀን ቀይር"
    override val ok = "እሺ"
    override val synaxariumTitle = "ስንክሳር"
    override val synaxariumKicker = "የዕለቱ ስንክሳር"
    override val noSynaxariumToday = "ለዛሬ የተመዘገበ ስንክሳር የለም"

    override val streaksTitle = "ጉዞ"
    override val currentStreakLabel = "የአሁኑ ጉዞ"
    override val todayLabel = "ዛሬ"
    override val habitsHeader = "ልማዶች"
    override val habitPrayer = "ጸሎት"
    override val habitChurch = "ቤተ ክርስቲያን"
    override val habitProstrate = "ስግደት"
    override val habitBible = "የዕለት ንባብ"
    override val manageHabits = "ልማዶች አስተካክል"
    override val manageHabitsIntro = "ልማዶችን ይጨምሩ፣ ስም ይቀይሩ፣ ደርድሩ ወይም ይደብቁ።"
    override val newHabit = "አዲስ ልማድ"
    override val habitNameLabel = "የልማዱ ስም"
    override val less = "ያነሰ"
    override val more = "የበዛ"
    override val streakCurrent = "አሁን"
    override val streakBest = "ከፍተኛ"
    override fun daysUnit(n: Int) = "$n ቀናት"

    override val nowPrayer = "የአሁኑ ሰዓት ጸሎት"
    override val continueReading = "ቀጥል"
    override val hoursHeader = "ሰዓታት"

    override val contents = "ይዘት"
    override val readingModeToggle = "የንባብ ሁነታ"
    override val bookmarkAction = "ምልክት አድርግ"
    override val highlight = "አድምቅ"
    override val removeHighlight = "ማድመቅ አስወግድ"

    override val searchHint = "በጸሎቶችና በመዝሙራት ውስጥ ፈልግ"
    override val noResults = "ምንም አልተገኘም"
    override val recentSearches = "የቅርብ ጊዜ ፍለጋዎች"
    override val clearAction = "አጽዳ"
    override val streakReminderTitle = "ዛሬን መዝግብ"
    override val streakReminderBody = "የዛሬን ጸሎትና ልማድ ሳትሞላ እንዳትተኛ"
    override val streakChannelName = "የሌሊት ማስታወሻ"
    override val gitsaweReminderTitle = "የዕለቱ ግጻዌ"
    override val gitsaweReminderBody = "የዛሬን ምንባብ ተመልከት"
    override val gitsaweChannelName = "የዕለቱ ግጻዌ ማስታወሻ"
    override val settingsGitsaweReminder = "የዕለቱ ግጻዌ ማስታወሻ"
    override val settingsGitsaweReminderDesc = "በየቀኑ ጠዋት የዕለቱን ግጻዌ ምንባብ አስታውሰኝ"
    override val settingsStreakReminder = "የሌሊት ማስታወሻ"
    override val settingsStreakReminderDesc = "በ21፡30 ዛሬን እንድትሞላ ያስታውስሃል"
    override val notifDisabledTitle = "ማሳወቂያዎች ጠፍተዋል"
    override val notifDisabledBody = "ማንቂያዎችህ እንዲደርሱህ የመተግበሪያውን ማሳወቂያዎች ከቅንብሮች አብራ።"

    override val bookmarksTitle = "ምልክቶች"
    override val noBookmarksTitle = "ገና ምልክት አላደረጉም"
    override val noBookmarksBody = "በንባብ ገጹ ላይ ያለውን የምልክት ምልክት በመንካት ጸሎቶችን ያስቀምጡ"
    override val removeAction = "አስወግድ"
    override val bookmarkGroupScripture = "ቅዱሳት መጻሕፍት"
    override val bookmarkGroupSynaxarium = "ስንክሳር"

    override val settingsTitle = "ቅንብሮች"
    override val appearance = "ገጽታ"
    override val themeSystem = "ስርዓት"
    override val themeLight = "ብርሃን"
    override val themeDark = "ጨለማ"
    override val keepScreenOn = "ማያ ገጽ እንዳይጠፋ"
    override val keepScreenOnDesc = "በንባብ ጊዜ ማያ ገጹ በርቶ ይቆያል"
    override val languageLabel = "ቋንቋ"
    override val langSystem = "ስርዓት"
    override val langAmharic = "አማርኛ"
    override val langEnglish = "English"
    override val customizePrayers = "ጸሎቶችን አስተካክል"
    override val reminderModes = "የጸሎት ማንቂያ ሁነታዎች"
    override val about = "ስለ መተግበሪያው"
    override val alarmSection = "ማንቂያ"
    override val alertSoundVibrate = "ድምፅና ንዝረት"
    override val alertSoundOnly = "ድምፅ ብቻ"
    override val alertVibrateOnly = "ንዝረት ብቻ"
    override val alertSilent = "ጸጥታ"
    override val soundLabel = "ድምፅ"
    override val soundAlarm = "ማንቂያ"
    override val soundRingtone = "የስልክ ድምፅ"
    override val soundNotification = "ማሳወቂያ"

    override val modesTitle = "የማንቂያ ሁነታዎች"
    override val startFromAgpeya = "ከነባር ጀምር"
    override val startEmpty = "ባዶ ጀምር"
    override val remindersNotFiring = "ማንቂያ አይሰራም?"
    override val deleteModeTitle = "ሁነታውን ይሰረዝ?"
    override fun deleteModeBody(name: String, count: Int) = "«$name» ከነ $count ማንቂያዎቹ ይሰረዛል።"
    override val delete = "ሰርዝ"
    override val cancel = "ተወው"
    override val builtInBadge = "ቋሚ ሁነታ"
    override fun remindersOn(count: Int) = "$count ማንቂያ በርቷል"
    override val newModeName = "አዲስ ሁነታ"

    override val modeNameLabel = "የሁነታው ስም"
    override val resetTimes = "ወደ ቀድሞ መልስ"
    override val addReminder = "ማንቂያ ጨምር"
    override val prayerLabel = "ጸሎት"
    override val timeLabel = "ሰዓት"
    override val daysLabel = "ቀናት"
    override val everyDay = "በየቀኑ"
    override val save = "አስቀምጥ"
    override val noDaySelected = "ቀን አልተመረጠም"
    override val daysSummaryDaily = "በየቀኑ"

    override val customizeTitle = "ጸሎቶችን አስተካክል"
    override val customizeIntro = "በእያንዳንዱ ጸሎት ውስጥ የትኞቹ ክፍሎች እንደሚታዩና ቅደም ተከተላቸውን ይምረጡ።"
    override val resetLayout = "ዳግም አስጀምር"
    override val showSection = "አሳይ"
    override val hideSection = "ደብቅ"
    override val moveUp = "ወደ ላይ"
    override val moveDown = "ወደ ታች"

    override val alarmPrayerTime = "የጸሎት ሰዓት"
    override val openPrayer = "ጸሎቱን ክፈት"
    override val dismiss = "አጥፋ"
    override val reminderReached = "የጸሎት ሰዓት ደርሷል"
    override val itsTime = "ጊዜው ደርሷል"
    override val openShort = "ክፈት"
    override val donePrompt = "ጨርሰዋል?"
    override val yesAction = "አዎ"
    override val shareAction = "አጋራ"

    override val psalterTitle = "መዝሙረ ዳዊት"
    override val wholePsalter = "ሙሉ"
    override val dailyPsalms = "የዕለቱ"
    override val comingSoon = "በቅርቡ..."
    override val zewotrTselot = "ዘወትር ጸሎት"
    override val wudaseMariam = "ውዳሴ ማርያም"
    override val wudaseLangAmharic = "አማርኛ"
    override val wudaseLangGeez = "ግዕዝ"
    override val contentUnavailable = "ይዘቱን ማግኘት አልተቻለም"
    override fun psalmRange(from: Int, to: Int) = "መዝሙር $from–$to"
    override val snooze = "አሳድር"
    override val addPsalm = "መዝሙር ጨምር"
    override val choosePsalm = "መዝሙር ምረጥ"
    override val remove = "አስወግድ"
    override val manageHours = "ሰዓታት አስተካክል"
    override val newHour = "አዲስ ሰዓት"
    override val hourNameLabel = "የሰዓቱ ስም"
    override val rename = "ስም ቀይር"
    override val manageHoursIntro = "ሰዓታትን ይጨምሩ፣ ስም ይቀይሩ፣ ደርድሩ ወይም ይደብቁ። ክፍሎችን ለማስተካከል ሰዓቱን ይንኩ።"


    override val profileSection = "መገለጫ"
    override val yourNameLabel = "ስም"
    override val christianNameLabel = "የክርስትና ስም (አማራጭ)"
    override val introNameTitle = "ማን እንበልዎ?"
    override val introNameBody = "ስምዎ በስልክዎ ላይ ብቻ ይቀመጣል።"
    override fun greeting(name: String) = "ሰላም፣ $name"

    override val introTitle = "እንኳን ደህና መጡ"
    override val introBody = "የጸሎት ሰዓታት — መዝሙራትና ወንጌላት በአንድ ቦታ።"
    override val introOfflineTitle = "ከበይነመረብ ውጭ"
    override val introOfflineBody = "ሙሉ በሙሉ ከመስመር ውጭ ይሰራል። ምንም መረጃ አይሰበሰብም።"
    override val introRemindersTitle = "ማስታወሻዎች"
    override val introRemindersBody = "የራስዎን የጸሎት ሰዓታት ይምረጡ፤ በሚፈልጉት ጊዜ ያስታውሱ።"
    override val introStreakTitle = "ተከታታይነት"
    override val introStreakBody = "የጸሎትዎንና የልማዶችዎን ተከታታይነት በየቀኑ ይከታተሉ።"
    override val introPsalterTitle = "መዝሙረ ዳዊት"
    override val introPsalterBody = "ሁሉም 150 መዝሙራት — በየቀኑ ተከፋፍለው፣ ሁልጊዜ ከመስመር ውጭ።"
    override val tutorial = "እንዴት እንደሚሠራ"
    override val tutorialAskTitle = "አጭር ማብራሪያ ይፈልጋሉ?"
    override val tutorialAskBody = "ዋና ዋና ባህሪያትን በፍጥነት እናሳይዎ።"
    override val showTutorial = "አሳየኝ"
    override val gotIt = "ገባኝ"
    override val getStarted = "ጀምር"
    override val next = "ቀጥል"
    override val skip = "ዝለል"

    override val batteryHelp = "ማስታወሻ አይሰራም?"
    override val batteryHelpIntro = "አንዳንድ ስልኮች ባትሪ ለመቆጠብ መተግበሪያዎችን ያቆማሉ። ማስታወሻዎች በሰዓቱ እንዲሰሩ የሚከተሉትን ያድርጉ።"
    override val batteryStepUnrestrict = "ባትሪ ገደብ አንሳ"
    override val batteryStepUnrestrictBody = "ቅንብሮች → ባትሪ → ይህን መተግበሪያ ያልተገደበ አድርግ (Unrestricted)።"
    override val batteryStepAutostart = "በራስ ማስጀመር ፍቀድ"
    override val batteryStepAutostartBody = "Xiaomi/Samsung ላሉ ስልኮች Autostart ፍቀድ፤ ከ«የሚተኙ መተግበሪያዎች» አስወግድ።"
    override val openSettings = "ቅንብሮችን ክፈት"
    override val remindersNotFiringTitle = "ማስታወሻ አይሰራም?"

    override val dayLabels = listOf("ሰ", "ማ", "ረ", "ሐ", "ዓ", "ቅ", "እ")
    override val weekdayNames = listOf("ሰኞ", "ማክሰኞ", "ረቡዕ", "ሐሙስ", "ዓርብ", "ቅዳሜ", "እሑድ")
    override val ethMonths = listOf(
        "መስከረም", "ጥቅምት", "ኅዳር", "ታኅሣሥ", "ጥር", "የካቲት",
        "መጋቢት", "ሚያዝያ", "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜን",
    )
    override val eraSuffix = "ዓ.ም"
    override fun habitsCount(n: Int) = "$n ልማዶች"
}

object EnglishStrings : Strings {
    override val back = "Back"
    override val tabHome = "Home"
    override val tabSearch = "Search"
    override val tabBookmarks = "Bookmarks"
    override val tabStreak = "Streak"
    override val tabSettings = "Settings"
    override val tabLibrary = "Library"

    override val libraryTitle = "Library"
    override val librarySubtitle = "Prayers and holy scriptures"
    override val scripturesTitle = "Scriptures"
    override val scripturesSubtitle = "The New Testament"
    override val newTestamentLabel = "New Testament"
    override val chapterUnit = "ch."
    override val bookGroupGospels = "Gospels"
    override val bookGroupActs = "Acts"
    override val bookGroupPaul = "Pauline Epistles"
    override val bookGroupCatholic = "General Epistles"
    override val bookGroupRevelation = "Revelation"

    override val gitsaweTitle = "Today's Gitsawe"
    override val gitsaweKicker = "Gitsawe of the day"
    override val srcDaily = "Daily"
    override val srcSeasonal = "Seasonal"
    override val srcMonthly = "Monthly"
    override val noGitsaweToday = "No Gitsawe recorded for today"
    override val gitsaweOpenNotAvailable = "This reading isn't available yet"
    override val gitsaweChangeDay = "Change day"
    override val ok = "OK"
    override val synaxariumTitle = "Synaxarium"
    override val synaxariumKicker = "Today's Synaxarium"
    override val noSynaxariumToday = "No synaxarium recorded for today"

    override val streaksTitle = "Streaks"
    override val currentStreakLabel = "Current streak"
    override val todayLabel = "Today"
    override val habitsHeader = "Habits"
    override val habitPrayer = "Prayer"
    override val habitChurch = "Church"
    override val habitProstrate = "Prostration"
    override val habitBible = "Daily Bible"
    override val manageHabits = "Manage habits"
    override val manageHabitsIntro = "Add, rename, reorder or hide habits."
    override val newHabit = "New habit"
    override val habitNameLabel = "Habit name"
    override val less = "Less"
    override val more = "More"
    override val streakCurrent = "now"
    override val streakBest = "best"
    override fun daysUnit(n: Int) = "$n days"

    override val nowPrayer = "Prayer for now"
    override val continueReading = "Continue"
    override val hoursHeader = "Hours"

    override val contents = "Contents"
    override val readingModeToggle = "Reading mode"
    override val bookmarkAction = "Bookmark"
    override val highlight = "Highlight"
    override val removeHighlight = "Remove highlight"

    override val searchHint = "Search prayers & psalms"
    override val noResults = "Nothing found"
    override val recentSearches = "Recent searches"
    override val clearAction = "Clear"
    override val streakReminderTitle = "Log today"
    override val streakReminderBody = "Mark today's prayers and habits before bed"
    override val streakChannelName = "Nightly reminder"
    override val gitsaweReminderTitle = "Today's Gitsawe"
    override val gitsaweReminderBody = "See today's reading"
    override val gitsaweChannelName = "Daily Gitsawe reminder"
    override val settingsGitsaweReminder = "Daily Gitsawe reminder"
    override val settingsGitsaweReminderDesc = "Each morning, remind me of today's Gitsawe reading"
    override val settingsStreakReminder = "Nightly streak reminder"
    override val settingsStreakReminderDesc = "A 9:30 PM nudge to fill in today"
    override val notifDisabledTitle = "Notifications are off"
    override val notifDisabledBody = "Turn on notifications in settings so your reminders can reach you."

    override val bookmarksTitle = "Bookmarks"
    override val noBookmarksTitle = "No bookmarks yet"
    override val noBookmarksBody = "Tap the bookmark icon on the reading page to save prayers"
    override val removeAction = "Remove"
    override val bookmarkGroupScripture = "Scriptures"
    override val bookmarkGroupSynaxarium = "Synaxarium"

    override val settingsTitle = "Settings"
    override val appearance = "Appearance"
    override val themeSystem = "System"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val keepScreenOn = "Keep screen on"
    override val keepScreenOnDesc = "The screen stays on while reading"
    override val languageLabel = "Language"
    override val langSystem = "System"
    override val langAmharic = "አማርኛ"
    override val langEnglish = "English"
    override val customizePrayers = "Customize prayers"
    override val reminderModes = "Prayer reminder modes"
    override val about = "About"
    override val alarmSection = "Alarm"
    override val alertSoundVibrate = "Sound & vibrate"
    override val alertSoundOnly = "Sound only"
    override val alertVibrateOnly = "Vibrate only"
    override val alertSilent = "Silent"
    override val soundLabel = "Sound"
    override val soundAlarm = "Alarm"
    override val soundRingtone = "Ringtone"
    override val soundNotification = "Notification"

    // About page body — English only, by design; see the note in the Strings interface.
    val aboutTagline = "The Orthodox Tewahedo hours of prayer — fully offline."
    val aboutSourceTitle = "Text source"
    val aboutSourceBody =
        "Psalms and gospels are drawn from the 80-weahadu open-source Amharic Bible by EOTCOpenSource — " +
            "github.com/EOTCOpenSource/80-weahadu — used under the Creative Commons " +
            "Attribution-NonCommercial-NoDerivatives 4.0 International licence — " +
            "creativecommons.org/licenses/by-nc-nd/4.0. Passages are selected and arranged into the hours " +
            "of prayer; the verse text is reproduced unchanged, except that the acrostic letters of " +
            "Psalm 118 are shown as stanza headings. Provided as-is, without warranties."
    val aboutFontTitle = "Fonts"
    val aboutFontBody = "Abyssinica SIL and Noto Sans Ethiopic — under the SIL Open Font License 1.1."
    val aboutPrivacyTitle = "Privacy"
    val aboutPrivacyBody = "This app collects no data and has no internet permission."

    override val modesTitle = "Reminder modes"
    override val startFromAgpeya = "Start from default"
    override val startEmpty = "Start empty"
    override val remindersNotFiring = "Reminders not firing?"
    override val deleteModeTitle = "Delete this mode?"
    override fun deleteModeBody(name: String, count: Int) = "“$name” and its $count reminders will be deleted."
    override val delete = "Delete"
    override val cancel = "Cancel"
    override val builtInBadge = "Built-in"
    override fun remindersOn(count: Int) = "$count reminders on"
    override val newModeName = "New mode"

    override val modeNameLabel = "Mode name"
    override val resetTimes = "Reset to defaults"
    override val addReminder = "Add reminder"
    override val prayerLabel = "Prayer"
    override val timeLabel = "Time"
    override val daysLabel = "Days"
    override val everyDay = "Every day"
    override val save = "Save"
    override val noDaySelected = "No day selected"
    override val daysSummaryDaily = "Every day"

    override val customizeTitle = "Customize prayers"
    override val customizeIntro = "Choose which sections appear in each prayer and their order."
    override val resetLayout = "Reset"
    override val showSection = "Show"
    override val hideSection = "Hide"
    override val moveUp = "Move up"
    override val moveDown = "Move down"

    override val alarmPrayerTime = "Prayer time"
    override val openPrayer = "Open prayer"
    override val dismiss = "Dismiss"
    override val reminderReached = "It is time to pray"
    override val itsTime = "It's time"
    override val openShort = "Open"
    override val donePrompt = "Done?"
    override val yesAction = "Yes"
    override val shareAction = "Share"

    override val psalterTitle = "መዝሙረ ዳዊት"
    override val wholePsalter = "All"
    override val dailyPsalms = "Today's"
    override val comingSoon = "Coming soon..."
    override val zewotrTselot = "ዘወትር ጸሎት"
    override val wudaseMariam = "ውዳሴ ማርያም"
    override val wudaseLangAmharic = "Amharic"
    override val wudaseLangGeez = "Ge'ez"
    override val contentUnavailable = "Content unavailable"
    override fun psalmRange(from: Int, to: Int) = "Psalms $from–$to"
    override val snooze = "Snooze"
    override val addPsalm = "Add psalm"
    override val choosePsalm = "Choose a psalm"
    override val remove = "Remove"
    override val manageHours = "Manage hours"
    override val newHour = "New hour"
    override val hourNameLabel = "Hour name"
    override val rename = "Rename"
    override val manageHoursIntro = "Add, rename, reorder or hide hours. Tap an hour to edit its sections."


    override val profileSection = "Profile"
    override val yourNameLabel = "Name"
    override val christianNameLabel = "Baptismal name (optional)"
    override val introNameTitle = "What should we call you?"
    override val introNameBody = "Your name is stored only on this phone."
    override fun greeting(name: String) = "Selam, $name"

    override val introTitle = "Welcome"
    override val introBody = "The hours of prayer — psalms and gospels, in one place."
    override val introOfflineTitle = "Works offline"
    override val introOfflineBody = "Fully offline. No data is collected, ever."
    override val introRemindersTitle = "Reminders"
    override val introRemindersBody = "Choose your own times and be reminded when you want."
    override val introStreakTitle = "Streaks"
    override val introStreakBody = "Track your prayer and habit streak day by day."
    override val introPsalterTitle = "The Psalter"
    override val introPsalterBody = "All 150 psalms — divided by day and always offline."
    override val tutorial = "How it works"
    override val tutorialAskTitle = "Want a quick tour?"
    override val tutorialAskBody = "We'll show you the main features in a few taps."
    override val showTutorial = "Show me"
    override val gotIt = "Got it"
    override val getStarted = "Get started"
    override val next = "Next"
    override val skip = "Skip"

    override val batteryHelp = "Reminders not firing?"
    override val batteryHelpIntro = "Some phones stop apps to save battery. To keep reminders on time, do the following."
    override val batteryStepUnrestrict = "Remove battery limits"
    override val batteryStepUnrestrictBody = "Settings → Battery → set this app to Unrestricted."
    override val batteryStepAutostart = "Allow auto-start"
    override val batteryStepAutostartBody = "On Xiaomi/Samsung, enable Autostart and remove the app from \"Sleeping apps\"."
    override val openSettings = "Open settings"
    override val remindersNotFiringTitle = "Reminders not firing?"

    override val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    override val weekdayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    override val ethMonths = listOf(
        "Meskerem", "Tikimt", "Hidar", "Tahsas", "Tir", "Yekatit",
        "Megabit", "Miyazya", "Ginbot", "Sene", "Hamle", "Nehase", "Pagume",
    )
    override val eraSuffix = "EC"
    override fun habitsCount(n: Int) = "$n habits"
}

val LocalStrings = staticCompositionLocalOf<Strings> { AmharicStrings }
