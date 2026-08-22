package com.agpeya.app.ui.strings

import androidx.compose.runtime.staticCompositionLocalOf
import com.agpeya.app.ui.reading.geezNumeral

/**
 * All app-chrome strings in both languages. Prayer content (hour names, psalm
 * text, gospels) is never translated — it stays in Amharic as data.
 */
interface Strings {
    val back: String
    val tabHome: String
    val tabSearch: String
    val tabBookmarks: String
    val tabJourney: String
    val tabSettings: String
    val tabLibrary: String

    val libraryTitle: String
    val librarySubtitle: String
    val scripturesTitle: String
    val scripturesSubtitle: String
    val newTestamentLabel: String
    val oldTestamentLabel: String
    val bibleTitle: String
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
    val previousDay: String
    val nextDay: String
    // The dedicated passage page a ግጻዌ section opens on, and its two doors out.
    val goToBook: String
    val goToChapter: String
    val goToPsalm: String
    val ok: String
    val synaxariumTitle: String
    val synaxariumKicker: String
    val noSynaxariumToday: String

    /** Language of the closing ጸሎት, and the hint that tapping switches it. */
    val closingPrayerGeez: String
    val closingPrayerAmharic: String
    val closingPrayerSwitchHint: String

    val journeyTitle: String
    val todayLabel: String
    val habitsHeader: String
    val habitPrayer: String
    val habitSynaxarium: String
    val habitChurch: String
    val habitProstrate: String
    val habitBible: String
    val manageHabits: String
    val manageHabitsIntro: String
    val newHabit: String
    val habitNameLabel: String
    val less: String
    val more: String

    // The Journey metric: distinct days with prayer in the current period —
    // the Ethiopian month, or the running fast. Never a consecutive count.
    /** "23 days of prayer this month" (handles 0 with its own quiet wording). */
    fun journeyMonthLine(days: Int): String
    /** "Day 18 of ዐቢይ ጾም — prayed 16 days". Fast names are content, in Amharic. */
    fun journeyFastLine(fastName: String, dayOfFast: Int, daysPrayed: Int): String
    /** Today's candle, described for the hero line and screen readers. */
    val journeyTodayLit: String
    val journeyTodayUnlit: String
    /** Shown after one or more missed days — a welcome, never a loss notice. */
    val welcomeBack: String
    /** Per-habit summary row: "12 days this month". */
    fun daysThisMonth(n: Int): String
    /** Header over the year heatmap — the main historical view. */
    val yearJourneyHeader: String
    /** Legend label for the fasting-season wash on the heatmap. */
    val fastLegendLabel: String

    val nowPrayer: String
    val continueReading: String
    val hoursHeader: String

    val contents: String
    val readingModeToggle: String
    val bookmarkAction: String
    val highlight: String
    fun highlightColor(colorKey: String): String
    val removeHighlight: String

    val searchHint: String
    val noResults: String
    val recentSearches: String
    val clearAction: String
    // The nightly nudge. Its wording never depends on history — no streak to
    // keep, nothing to lose; the invitation is the same on day 1 and day 1000.
    val nightReminderTitle: String
    val nightReminderBody: String
    /** Body on a fasting day (a fast period or the ረቡዕ/ዓርብ rule). */
    val nightReminderFastBody: String
    /** Body on a feast; the feast name is content and stays in Amharic. */
    fun nightReminderFeastBody(feast: String): String
    /** Appended when core daily items have not yet been checked off. */
    fun nightReminderPending(items: String): String
    val nightReminderChannel: String
    val gitsaweReminderTitle: String
    val gitsaweReminderBody: String
    val gitsaweChannelName: String
    val settingsGitsaweReminder: String
    val settingsGitsaweReminderDesc: String
    val settingsNightReminder: String
    val settingsNightReminderDesc: String
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
    /** The alarm headline when the hour is known: "ሰርክ ደርሷል" — the prayer has
     *  arrived, not merely the clock. Falls back to [itsTime] unnamed. */
    fun hourArrived(hourName: String): String
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
    // ሰዓታት — the Horologion. The name is content and stays in Ge'ez script.
    val seatatTitle: String
    val seatatSubtitle: String
    /** The paired Ge'ez + Amharic mode of the ሰዓታት reader. */
    val seatatLangBoth: String
    val contentUnavailable: String
    val mementoMoriGloss: String
    val fastingTitle: String
    val fastingToday: String
    val fastingNone: String
    val fastingWeekly: String
    val fastingWeeklyNote: String
    fun fastingYearHeader(ethYear: Int): String
    fun fastingDays(n: Int): String
    fun fastingDayOf(day: Int, total: Int): String
    val restorePreviewTitle: String
    val restoreNothingNew: String
    val restoreMergeNote: String
    fun backupCreated(date: String): String
    fun backupContains(days: Int, bookmarks: Int, highlights: Int): String
    fun restoreWillAdd(days: Int, bookmarks: Int): String
    val backupTitle: String
    val backupExport: String
    val backupImport: String
    val backupSaved: String
    val backupFailed: String
    val restoreDone: String
    val restoreFailed: String
    val previousYear: String
    val nextYear: String
    val quietHours: String
    val quietHoursDesc: String
    fun quietHoursRange(from: String, to: String): String
    val filterAll: String
    /** Screen-reader state for a completed habit dot. */
    val doneLabel: String
    val currentHourBadge: String
    val previousHour: String
    val nextHour: String
    val copyAction: String
    val copiedToast: String
    val readingFontTitle: String
    val readingFontSubtitle: String
    val fontAbyssinica: String
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
    val introJourneyTitle: String
    val introJourneyBody: String
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

    /** Gregorian month abbreviations, January..December. */
    val gregorianMonths: List<String>

    /** Whether to spell Gregorian months out; false falls back to dd/MM/yyyy. */
    val usesGregorianMonthNames: Boolean

    /** Liturgical season names, keyed by BahreHasab's season keys. */
    fun seasonName(key: String): String?
    fun seasonWithWeek(name: String, week: Int): String

    fun habitsCount(n: Int): String

    // Interaction labels a screen reader needs but a sighted user reads from
    // the chevron's direction alone.
    val expand: String
    val collapse: String

    // Settings groups. The screen is long enough that it has to be scannable by
    // heading rather than by reading every row.
    val settingsGroupReading: String
    val settingsGroupPrayer: String
    val settingsGroupData: String
    val settingsGroupMore: String

    /**
     * Failure messages. Each one answers three questions in order: what
     * happened, whether the user's own data is affected, and what to do next.
     * A file picker returning a broken URI is not the user's problem to parse.
     */
    val backupFailedBody: String
    val restoreFailedBody: String
    val contentMissingTitle: String
    val contentMissingBody: String

    /** The "share this passage as a PNG card" action, next to copy/share. */
    val shareAsImage: String
    val shareFailed: String

    /** የዕለቱ ቅዳሴ — the day's appointed anaphora, at the foot of the ግጻዌ. */
    val kidaseHeader: String

    // Prayer list — people to remember in prayer.
    val prayerListTitle: String
    val addPerson: String
    val editPerson: String
    val personNameLabel: String
    val prayerNoteLabel: String
    val noPrayerListTitle: String
    val noPrayerListBody: String

    // Scheduled intentions (ምጽዋት / ንስሐ): reminders configured in Settings.
    // Deliberately not habits — nothing is recorded or streaked.
    val settingsAlmsReminder: String
    val settingsAlmsReminderDesc: String
    val settingsRepentReminder: String
    val settingsRepentReminderDesc: String
    val almsReminderTitle: String
    val almsReminderBody: String
    val almsChannelName: String
    val repentReminderTitle: String
    val repentReminderBody: String
    val repentChannelName: String

    // የመሃል ጸሎት — the once-a-day nudge to pray, not read, at an unplanned
    // moment between the hours. The prayer itself is content and never
    // translated; only this chrome is.
    val settingsBreathReminder: String
    val settingsBreathReminderDesc: String
    val breathReminderTitle: String
    val breathChannelName: String

    /** The next day a scheduled intention's reminder will fire. */
    fun nextDue(date: String): String

    /** The schedule row + editor: how often a special habit is due. */
    val scheduleLabel: String
    val scheduleWeekly: String
    val scheduleEveryOtherDay: String
    val scheduleMonthly: String
    /** Summary for a monthly schedule; [day] is an ETHIOPIAN month day (1..30). */
    fun monthlyOnDay(day: Int): String

    // A person can keep several ምጽዋት / ንስሐ reminders, each with its own name.
    val addSpecialReminder: String
    val reminderNameLabel: String
    val reminderNameHintAlms: String
    val reminderNameHintRepent: String
    val untitledReminder: String
    val noSpecialReminders: String

    /** Header of the widget's second card: tomorrow's ግጻዌ, for preparing. */
    val tomorrowLabel: String

    /** The widget card's action line; an arrow is appended in the layout. */
    val readGitsawe: String
}

object AmharicStrings : Strings {
    override val back = "ተመለስ"
    override val tabHome = "ቤት"
    override val tabSearch = "ፍለጋ"
    override val tabBookmarks = "ምልክቶች"
    override val tabJourney = "ጉዞ"
    override val tabSettings = "ቅንብር"
    override val tabLibrary = "ቤተ መጻሕፍት"

    override val libraryTitle = "ቤተ መጻሕፍት"
    override val librarySubtitle = "የጸሎትና የቅዱሳት መጻሕፍት ስብስብ"
    override val scripturesTitle = "ቅዱሳት መጻሕፍት"
    override val scripturesSubtitle = "የአዲስ ኪዳን መጻሕፍት"
    override val newTestamentLabel = "አዲስ ኪዳን"
    override val oldTestamentLabel = "ቀዳማዊ ኪዳን"
    override val bibleTitle = "መጽሐፍ ቅዱስ"
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
    override val previousDay = "ያለፈው ቀን"
    override val nextDay = "የሚቀጥለው ቀን"
    override val goToBook = "መጽሐፉን ክፈት"
    override val goToChapter = "ምዕራፉን ክፈት"
    override val goToPsalm = "መዝሙሩን ክፈት"
    override val ok = "እሺ"
    override val synaxariumTitle = "ስንክሳር"
    override val synaxariumKicker = "የዕለቱ ስንክሳር"
    override val noSynaxariumToday = "ለዛሬ የተመዘገበ ስንክሳር የለም"

    override val closingPrayerGeez = "ግዕዝ"
    override val closingPrayerAmharic = "አማርኛ"
    override val closingPrayerSwitchHint = "ቋንቋ ለመቀየር ይንኩ"

    override val journeyTitle = "ጉዞ"
    override val todayLabel = "ዛሬ"
    override val habitsHeader = "ልማዶች"
    override val habitPrayer = "ጸሎት"
    override val habitSynaxarium = "ስንክሳር"
    override val habitChurch = "ቤተ ክርስቲያን"
    override val habitProstrate = "ስግደት"
    override val habitBible = "የዕለት ንባብ"
    override val manageHabits = "ልማዶች አስተካክል"
    override val manageHabitsIntro = "ልማዶችን ይጨምሩ፣ ስም ይቀይሩ፣ ደርድሩ ወይም ይደብቁ።"
    override val newHabit = "አዲስ ልማድ"
    override val habitNameLabel = "የልማዱ ስም"
    override val less = "ያነሰ"
    override val more = "የበዛ"

    override fun journeyMonthLine(days: Int) =
        if (days <= 0) "በዚህ ወር ገና አልጸለዩም"
        else "በዚህ ወር ${geezNumeral(days)} ቀን ጸልየዋል"
    override fun journeyFastLine(fastName: String, dayOfFast: Int, daysPrayed: Int) =
        if (daysPrayed <= 0) "የ$fastName ${geezNumeral(dayOfFast)}ኛ ቀን"
        else "የ$fastName ${geezNumeral(dayOfFast)}ኛ ቀን — ${geezNumeral(daysPrayed)} ቀን ጸልየዋል"
    override val journeyTodayLit = "ዛሬ ጸልየዋል"
    override val journeyTodayUnlit = "የዛሬው ሻማ ይጠብቃል"
    override val welcomeBack = "ተመልሰዋል — ዛሬ ይጀምሩ"
    override fun daysThisMonth(n: Int) = "በዚህ ወር $n ቀን"
    override val yearJourneyHeader = "የዓመቱ ጉዞ"
    override val fastLegendLabel = "ጾም"

    override val nowPrayer = "የአሁኑ ሰዓት ጸሎት"
    override val continueReading = "ቀጥል"
    override val hoursHeader = "ሰዓታት"

    override val contents = "ይዘት"
    override val readingModeToggle = "የንባብ ሁነታ"
    override val bookmarkAction = "ምልክት አድርግ"
    override val highlight = "አድምቅ"
    override fun highlightColor(colorKey: String) = when (colorKey) {
        "yellow" -> "ቢጫ"
        "green" -> "አረንጓዴ"
        "blue" -> "ሰማያዊ"
        "pink" -> "ሮዝ"
        else -> colorKey
    }
    override val removeHighlight = "ማድመቅ አስወግድ"

    override val searchHint = "በጸሎቶችና በመዝሙራት ውስጥ ፈልግ"
    override val noResults = "ምንም አልተገኘም"
    override val recentSearches = "የቅርብ ጊዜ ፍለጋዎች"
    override val clearAction = "አጽዳ"
    override val nightReminderTitle = "ሰርክ ደርሷል"
    override val nightReminderBody = "ዕለቱን በጸሎት ዝጉ"
    override val nightReminderFastBody = "ጾሙ በጸሎት ይታጀብ"
    override fun nightReminderFeastBody(feast: String) = "$feast — ዕለቱን በጸሎት ዝጉ"
    override fun nightReminderPending(items: String) = "ዛሬ ያልተመዘገቡ፦ $items"
    override val nightReminderChannel = "የሌሊት ማስታወሻ"
    override val gitsaweReminderTitle = "የዕለቱ ግጻዌ"
    override val gitsaweReminderBody = "የዛሬን ምንባብ ተመልከት"
    override val gitsaweChannelName = "የዕለቱ ግጻዌ ማስታወሻ"
    override val settingsGitsaweReminder = "የዕለቱ ግጻዌ ማስታወሻ"
    override val settingsGitsaweReminderDesc = "በየቀኑ ጠዋት የዕለቱን ግጻዌ ምንባብ አስታውሰኝ"
    override val settingsNightReminder = "የሌሊት ማስታወሻ"
    override val settingsNightReminderDesc = "ጸሎት ቢመዘገብም ስንክሳርን፣ ቤተ ክርስቲያንንና ስግደትን ለማስታወስ በየሌሊቱ ይልካል"
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
    override fun hourArrived(hourName: String) = "$hourName ደርሷል"
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
    override val seatatTitle = "ሰዓታት"
    override val seatatSubtitle = "Seatat — ዘሌሊት ወዘነግህ፣ ግዕዝ ከአማርኛ ጋር"
    override val seatatLangBoth = "ግዕዝና አማርኛ"
    override val contentUnavailable = "ይዘቱን ማግኘት አልተቻለም"
    override val mementoMoriGloss = "ሞትን አስብ"
    override val fastingTitle = "አጽዋማት"
    override val fastingToday = "ዛሬ"
    override val fastingNone = "ጾም የለም"
    override val fastingWeekly = "ጾመ ድህነት (ረቡዕ/ዓርብ)"
    override val fastingWeeklyNote = "ረቡዕና ዓርብ ዓመቱን ሙሉ የጾም ቀናት ናቸው፤ ከትንሣኤ እስከ ጰራቅሊጦስ ባለው ሃምሳ ቀን ውስጥ ግን አይጾምም።"
    override fun fastingYearHeader(ethYear: Int) = "የ$ethYear ዓ.ም አጽዋማት"
    override fun fastingDays(n: Int) = "$n ቀን"
    override fun fastingDayOf(day: Int, total: Int) = "$day ኛ ቀን ከ$total"
    override val restorePreviewTitle = "ከምትኬ መልስ?"
    override val restoreNothingNew = "አዲስ ነገር የለም — ሁሉም አስቀድሞ አለ።"
    override val restoreMergeNote = "መመለስ የነበረውን አያጠፋም፤ የሚጨምር ብቻ ነው።"
    override fun backupCreated(date: String) = "የተሠራበት፦ $date"
    override fun backupContains(days: Int, bookmarks: Int, highlights: Int) =
        "$days ቀናት · $bookmarks ምልክቶች · $highlights ማድመቆች"
    override fun restoreWillAdd(days: Int, bookmarks: Int) =
        "$days ቀናትና $bookmarks ምልክቶች ይጨመራሉ።"
    override val backupTitle = "ምትኬ"
    override val backupExport = "ምትኬ አስቀምጥ"
    override val backupImport = "ከምትኬ መልስ"
    override val backupSaved = "ምትኬው ተቀምጧል።"
    override val backupFailed = "ምትኬውን ማስቀመጥ አልተቻለም።"
    override val restoreDone = "ከምትኬው ተመልሷል።"
    override val restoreFailed = "ፋይሉን ማንበብ አልተቻለም።"
    override val previousYear = "ያለፈው ዓመት"
    override val nextYear = "የሚቀጥለው ዓመት"
    override val quietHours = "ጸጥታ ሰዓታት"
    override val quietHoursDesc = "በሌሊት ማሳሰቢያዎች ድምፅ አያሰሙም"
    override fun quietHoursRange(from: String, to: String) = "ከ$from እስከ $to ድምፅ የለም"
    override val filterAll = "ሁሉም"
    override val doneLabel = "ተጠናቋል"
    override val currentHourBadge = "አሁን"
    override val previousHour = "ቀዳሚ"
    override val nextHour = "ቀጣይ"
    override val copyAction = "ቅዳ"
    override val copiedToast = "ተቀድቷል"
    override val readingFontTitle = "የንባብ ፊደል"
    override val readingFontSubtitle = "የጸሎት ጽሑፍ ፊደል"
    override val fontAbyssinica = "አቢሲኒካ"
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
    override val introJourneyTitle = "ጉዞ"
    override val introJourneyBody = "የጸሎት ሕይወትዎን ቀን በቀን ይመልከቱ — በቤተ ክርስቲያን ዓመት ውስጥ።"
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
    override val gregorianMonths = listOf(
        "ጃንዩ", "ፌብሩ", "ማርች", "ኤፕሪ", "ሜይ", "ጁን", "ጁላይ", "ኦገስ", "ሴፕቴ", "ኦክቶ", "ኖቬም", "ዲሴም",
    )
    override val usesGregorianMonthNames = false
    override fun seasonName(key: String): String? = when (key) {
        "neneweTsom" -> "ጾመ ነነዌ"
        "abiyTsom" -> "ዐቢይ ጾም"
        "holy_thursday" -> "ጸሎተ ሐሙስ"
        "erget" -> "ዕርገት"
        "tnsae" -> "ትንሣኤ"
        else -> null
    }
    override fun seasonWithWeek(name: String, week: Int) = "$name · $week ኛ ሳምንት"

    override fun habitsCount(n: Int) = "$n ልማዶች"

    override val expand = "ክፈት"
    override val collapse = "ዝጋ"

    override val settingsGroupReading = "ንባብ"
    override val settingsGroupPrayer = "ጸሎትና ማስታወሻ"
    override val settingsGroupData = "መረጃ"
    override val settingsGroupMore = "ተጨማሪ"

    override val backupFailedBody =
        "ምትኬው አልተቀመጠም። በመተግበሪያው ውስጥ ያለው መረጃህ እንደነበረ አለ። ሌላ ቦታ ወይም ሌላ ስም መርጠህ እንደገና ሞክር።"
    override val restoreFailedBody =
        "ፋይሉ አልተነበበም። ያለህ ጉዞ፣ ምልክቶችና ማድመቂያዎች አልተነኩም። የስንቅ ምትኬ ፋይል (.json) መሆኑን አረጋግጠህ እንደገና ሞክር።"
    override val contentMissingTitle = "ይህ ክፍል አልተገኘም"
    override val contentMissingBody =
        "ጽሑፉ በዚህ እትም ውስጥ የለም። የቀሩት ክፍሎች እንደተለመደው ይሠራሉ።"

    override val shareAsImage = "እንደ ምስል አጋራ"
    override val shareFailed = "ማጋራት አልተቻለም። እንደገና ይሞክሩ።"
    override val kidaseHeader = "የዕለቱ ቅዳሴ"

    override val prayerListTitle = "የጸሎት ዝርዝር"
    override val addPerson = "ሰው ጨምር"
    override val editPerson = "አስተካክል"
    override val personNameLabel = "ስም"
    override val prayerNoteLabel = "ማስታወሻ (አማራጭ)"
    override val noPrayerListTitle = "ገና ማንም አልተጨመረም"
    override val noPrayerListBody = "በጸሎት የምታስባቸውን ሰዎች እዚህ ጨምር።"

    override val settingsAlmsReminder = "የምጽዋት ማስታወሻ"
    override val settingsAlmsReminderDesc = "በመረጥከው ቀን ምጽዋት እንድትሰጥ ያስታውስሃል"
    override val settingsRepentReminder = "የንስሐ ማስታወሻ"
    override val settingsRepentReminderDesc = "ንስሐ እንድትገባና ቅዱስ ቁርባን እንድትቀበል ያስታውስሃል"
    override val almsReminderTitle = "የምጽዋት ቀን"
    override val almsReminderBody = "ዛሬ ምጽዋት የምትሰጥበት ቀን ነው።"
    override val almsChannelName = "የምጽዋት ማስታወሻ"
    override val repentReminderTitle = "ንስሐ"
    override val repentReminderBody = "ንስሐ መግባትን አስብ፤ ለቅዱስ ቁርባን ተዘጋጅ።"
    override val repentChannelName = "የንስሐ ማስታወሻ"

    override val settingsBreathReminder = "የመሃል ጸሎት"
    override val settingsBreathReminderDesc = "በቀን አንዴ፣ በሰዓታት መካከል ባልታሰበ ጊዜ አጭር ጸሎት ያስታውስሃል"
    override val breathReminderTitle = "ለአፍታ ጸልይ"
    override val breathChannelName = "የመሃል ጸሎት"

    override fun nextDue(date: String) = "ቀጣይ ማስታወሻ፦ $date"
    override val scheduleLabel = "ድግግሞሽ"
    override val scheduleWeekly = "በሳምንት"
    override val scheduleEveryOtherDay = "በየሁለት ቀን"
    override val scheduleMonthly = "በየወሩ"
    override fun monthlyOnDay(day: Int) = "በየወሩ በ$day ቀን"
    override val addSpecialReminder = "ማስታወሻ ጨምር"
    override val reminderNameLabel = "ስም (አማራጭ)"
    override val reminderNameHintAlms = "ለምሳሌ፦ ለቤተ ክርስቲያን"
    override val reminderNameHintRepent = "ለምሳሌ፦ የሳምንት ንስሐ"
    override val untitledReminder = "ስም የሌለው"
    override val noSpecialReminders = "ገና ማስታወሻ አልተጨመረም።"
    override val tomorrowLabel = "ነገ"
    override val readGitsawe = "ግጻዌውን ክፈት"
}

object EnglishStrings : Strings {
    override val back = "Back"
    override val tabHome = "Home"
    override val tabSearch = "Search"
    override val tabBookmarks = "Bookmarks"
    override val tabJourney = "Journey"
    override val tabSettings = "Settings"
    override val tabLibrary = "Library"

    override val libraryTitle = "Library"
    override val librarySubtitle = "Prayers and holy scriptures"
    override val scripturesTitle = "Scriptures"
    override val scripturesSubtitle = "The New Testament"
    override val newTestamentLabel = "New Testament"
    override val oldTestamentLabel = "Old Testament"
    override val bibleTitle = "Bible"
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
    override val previousDay = "Previous day"
    override val nextDay = "Next day"
    override val goToBook = "Open the book"
    override val goToChapter = "Open the chapter"
    override val goToPsalm = "Open the psalm"
    override val ok = "OK"
    override val synaxariumTitle = "Synaxarium"
    override val synaxariumKicker = "Today's Synaxarium"
    override val noSynaxariumToday = "No synaxarium recorded for today"

    override val closingPrayerGeez = "Ge'ez"
    override val closingPrayerAmharic = "Amharic"
    override val closingPrayerSwitchHint = "Tap to switch language"

    override val journeyTitle = "Journey"
    override val todayLabel = "Today"
    override val habitsHeader = "Habits"
    override val habitPrayer = "Prayer"
    override val habitSynaxarium = "Synaxarium"
    override val habitChurch = "Church"
    override val habitProstrate = "Prostration"
    override val habitBible = "Daily Bible"
    override val manageHabits = "Manage habits"
    override val manageHabitsIntro = "Add, rename, reorder or hide habits."
    override val newHabit = "New habit"
    override val habitNameLabel = "Habit name"
    override val less = "Less"
    override val more = "More"

    override fun journeyMonthLine(days: Int) = when (days) {
        0 -> "No days of prayer yet this month"
        1 -> "1 day of prayer this month"
        else -> "$days days of prayer this month"
    }
    override fun journeyFastLine(fastName: String, dayOfFast: Int, daysPrayed: Int) = when (daysPrayed) {
        0 -> "Day $dayOfFast of $fastName"
        1 -> "Day $dayOfFast of $fastName — prayed 1 day"
        else -> "Day $dayOfFast of $fastName — prayed $daysPrayed days"
    }
    override val journeyTodayLit = "Prayed today"
    override val journeyTodayUnlit = "Today's candle is waiting"
    override val welcomeBack = "You're back — begin today"
    override fun daysThisMonth(n: Int) = if (n == 1) "1 day this month" else "$n days this month"
    override val yearJourneyHeader = "The year's journey"
    override val fastLegendLabel = "Fast"

    override val nowPrayer = "Prayer for now"
    override val continueReading = "Continue"
    override val hoursHeader = "Hours"

    override val contents = "Contents"
    override val readingModeToggle = "Reading mode"
    override val bookmarkAction = "Bookmark"
    override val highlight = "Highlight"
    override fun highlightColor(colorKey: String) = colorKey.replaceFirstChar { it.uppercase() }
    override val removeHighlight = "Remove highlight"

    override val searchHint = "Search prayers & psalms"
    override val noResults = "Nothing found"
    override val recentSearches = "Recent searches"
    override val clearAction = "Clear"
    override val nightReminderTitle = "The day is ending"
    override val nightReminderBody = "Close it with prayer"
    override val nightReminderFastBody = "Let the fast be kept with prayer"
    override fun nightReminderFeastBody(feast: String) = "$feast — close the day with prayer"
    override fun nightReminderPending(items: String) = "Still to mark today: $items"
    override val nightReminderChannel = "Nightly reminder"
    override val gitsaweReminderTitle = "Today's Gitsawe"
    override val gitsaweReminderBody = "See today's reading"
    override val gitsaweChannelName = "Daily Gitsawe reminder"
    override val settingsGitsaweReminder = "Daily Gitsawe reminder"
    override val settingsGitsaweReminderDesc = "Each morning, remind me of today's Gitsawe reading"
    override val settingsNightReminder = "Nightly reminder"
    override val settingsNightReminderDesc = "Sent every evening—even after prayer is marked—to review Synaxarium, church, and prostrations"
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
        "Psalms and gospels come from the 80-weahadu Amharic Bible by EOTCOpenSource, under " +
            "CC BY-NC-ND 4.0. Verse text is unchanged; Psalm 118's acrostic letters are shown as " +
            "stanza headings. Provided as-is."
    val aboutFontTitle = "Fonts"
    val aboutFontBody =
        "Abyssinica SIL and Noto Sans Ethiopic, under the SIL Open Font License 1.1. The " +
            "selectable reading faces come from Font.et under the same licence."
    val aboutPrivacyTitle = "Privacy"
    val aboutPrivacyBody = "No data collected. No internet permission."
    val aboutLicenceTitle = "Licence"
    val aboutLicenceBody =
        "App code under the Apache License 2.0. The bundled prayer text keeps its own " +
            "terms (CC BY-NC-ND 4.0) and is not covered by that licence."

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
    override fun hourArrived(hourName: String) = "Time for $hourName"
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
    override val seatatTitle = "ሰዓታት"
    override val seatatSubtitle = "Seatat — the night & dawn office, Ge'ez with Amharic"
    override val seatatLangBoth = "Ge'ez + Amharic"
    override val contentUnavailable = "Content unavailable"
    override val mementoMoriGloss = "Remember death"
    override val fastingTitle = "Fasts"
    override val fastingToday = "Today"
    override val fastingNone = "No fast today"
    override val fastingWeekly = "Wednesday/Friday fast"
    override val fastingWeeklyNote = "Wednesdays and Fridays are fasting days year-round, except in the fifty days between Fasika and Pentecost."
    override fun fastingYearHeader(ethYear: Int) = "Fasts of $ethYear E.C."
    override fun fastingDays(n: Int) = "$n days"
    override fun fastingDayOf(day: Int, total: Int) = "Day $day of $total"
    override val restorePreviewTitle = "Restore this backup?"
    override val restoreNothingNew = "Nothing new — everything here is already on this device."
    override val restoreMergeNote = "Restoring only adds; nothing already on this device is removed."
    override fun backupCreated(date: String) = "Created $date"
    override fun backupContains(days: Int, bookmarks: Int, highlights: Int) =
        "$days days · $bookmarks bookmarks · $highlights highlights"
    override fun restoreWillAdd(days: Int, bookmarks: Int) =
        "Will add $days days and $bookmarks bookmarks."
    override val backupTitle = "Backup"
    override val backupExport = "Save a backup"
    override val backupImport = "Restore from a backup"
    override val backupSaved = "Backup saved."
    override val backupFailed = "Could not save the backup."
    override val restoreDone = "Restored from the backup."
    override val restoreFailed = "Could not read that file."
    override val previousYear = "Previous year"
    override val nextYear = "Next year"
    override val quietHours = "Quiet hours"
    override val quietHoursDesc = "Silence reminders overnight"
    override fun quietHoursRange(from: String, to: String) = "Silent from $from to $to"
    override val filterAll = "All"
    override val doneLabel = "Done"
    override val currentHourBadge = "Now"
    override val previousHour = "Previous"
    override val nextHour = "Next"
    override val copyAction = "Copy"
    override val copiedToast = "Copied"
    override val readingFontTitle = "Reading font"
    override val readingFontSubtitle = "Font for prayer text"
    override val fontAbyssinica = "Abyssinica"
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
    override val introJourneyTitle = "Journey"
    override val introJourneyBody = "See your life of prayer take shape, day by day, within the Church's year."
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
    override val gregorianMonths = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )
    override val usesGregorianMonthNames = true
    override fun seasonName(key: String): String? = when (key) {
        "neneweTsom" -> "Fast of Nineveh"
        "abiyTsom" -> "Great Lent"
        "holy_thursday" -> "Holy Thursday"
        "erget" -> "Ascension"
        "tnsae" -> "Resurrection season"
        else -> null
    }
    override fun seasonWithWeek(name: String, week: Int) = "$name · week $week"
    override fun habitsCount(n: Int) = "$n habits"

    override val expand = "Expand"
    override val collapse = "Collapse"

    override val settingsGroupReading = "Reading"
    override val settingsGroupPrayer = "Prayer and reminders"
    override val settingsGroupData = "Your data"
    override val settingsGroupMore = "More"

    override val backupFailedBody =
        "The backup wasn't saved. Everything in the app is untouched. Try again, choosing a different folder or file name."
    override val restoreFailedBody =
        "That file couldn't be read. Your prayer history, bookmarks and highlights are unchanged. Check it's a Sinq backup (.json) and try again."
    override val contentMissingTitle = "This passage isn't here"
    override val contentMissingBody =
        "The text isn't part of this edition. Everything else still works as usual."

    override val shareAsImage = "Share as image"
    override val shareFailed = "Couldn't share this passage. Please try again."
    override val kidaseHeader = "Kidase of the day"

    override val prayerListTitle = "Prayer list"
    override val addPerson = "Add person"
    override val editPerson = "Edit"
    override val personNameLabel = "Name"
    override val prayerNoteLabel = "Note (optional)"
    override val noPrayerListTitle = "No one here yet"
    override val noPrayerListBody = "Add the people you want to remember in prayer."

    override val settingsAlmsReminder = "Almsgiving reminder"
    override val settingsAlmsReminderDesc = "Remind me to give alms on the days I choose"
    override val settingsRepentReminder = "Repentance reminder"
    override val settingsRepentReminderDesc = "Remind me to repent and prepare for Holy Communion"
    override val almsReminderTitle = "A day to give"
    override val almsReminderBody = "Today is your day to give alms."
    override val almsChannelName = "Almsgiving reminder"
    override val repentReminderTitle = "Repentance"
    override val repentReminderBody = "Remember repentance — and prepare for Holy Communion."
    override val repentChannelName = "Repentance reminder"

    override val settingsBreathReminder = "Prayer between the hours"
    override val settingsBreathReminderDesc = "Once a day, at an unplanned moment between prayers, one short breath prayer"
    override val breathReminderTitle = "A moment to pray"
    override val breathChannelName = "Prayer between the hours"

    override fun nextDue(date: String) = "Next reminder: $date"
    override val scheduleLabel = "How often"
    override val scheduleWeekly = "Weekly"
    override val scheduleEveryOtherDay = "Every other day"
    override val scheduleMonthly = "Monthly"
    override fun monthlyOnDay(day: Int) = "Monthly, on day $day (Ethiopian month)"
    override val addSpecialReminder = "Add reminder"
    override val reminderNameLabel = "Name (optional)"
    override val reminderNameHintAlms = "e.g. For the church"
    override val reminderNameHintRepent = "e.g. Weekly repentance"
    override val untitledReminder = "Untitled"
    override val noSpecialReminders = "No reminders yet."
    override val tomorrowLabel = "Tomorrow"
    override val readGitsawe = "Read the Gitsawe"
}

val LocalStrings = staticCompositionLocalOf<Strings> { AmharicStrings }
