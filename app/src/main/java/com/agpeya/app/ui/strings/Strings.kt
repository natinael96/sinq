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
    val tabSettings: String

    val nowPrayer: String
    val continueReading: String

    val contents: String
    val readingModeToggle: String
    val bookmarkAction: String
    val highlight: String
    val removeHighlight: String

    val searchHint: String
    val noResults: String

    val bookmarksTitle: String
    val noBookmarksTitle: String
    val noBookmarksBody: String
    val removeAction: String

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

    val aboutTagline: String
    val aboutSourceTitle: String
    val aboutSourceBody: String
    val aboutFontTitle: String
    val aboutFontBody: String
    val aboutPrivacyTitle: String
    val aboutPrivacyBody: String

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
    val snooze: String
    val addPsalm: String
    val choosePsalm: String
    val remove: String
    val manageHours: String
    val newHour: String
    val hourNameLabel: String
    val rename: String
    val manageHoursIntro: String


    // First-launch intro
    val introTitle: String
    val introBody: String
    val introOfflineTitle: String
    val introOfflineBody: String
    val introRemindersTitle: String
    val introRemindersBody: String
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
}

object AmharicStrings : Strings {
    override val back = "ተመለስ"
    override val tabHome = "ቤት"
    override val tabSearch = "ፍለጋ"
    override val tabBookmarks = "ምልክቶች"
    override val tabSettings = "ቅንብር"

    override val nowPrayer = "የአሁኑ ሰዓት ጸሎት"
    override val continueReading = "ቀጥል"

    override val contents = "ይዘት"
    override val readingModeToggle = "የንባብ ሁነታ"
    override val bookmarkAction = "ምልክት አድርግ"
    override val highlight = "አድምቅ"
    override val removeHighlight = "ማድመቅ አስወግድ"

    override val searchHint = "በጸሎቶች ውስጥ ፈልግ"
    override val noResults = "ምንም አልተገኘም"

    override val bookmarksTitle = "ምልክቶች"
    override val noBookmarksTitle = "ገና ምልክት አላደረጉም"
    override val noBookmarksBody = "በንባብ ገጹ ላይ ያለውን የምልክት ምልክት በመንካት ጸሎቶችን ያስቀምጡ"
    override val removeAction = "አስወግድ"

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

    override val aboutTagline = "የኦርቶዶክስ ተዋሕዶ የጸሎት ሰዓታት — ሙሉ በሙሉ ከበይነመረብ ውጭ የሚሰራ።"
    override val aboutSourceTitle = "የጽሑፍ ምንጭ"
    override val aboutSourceBody = "መዝሙራትና ወንጌላት ከ80-weahadu ክፍት ምንጭ የአማርኛ መጽሐፍ ቅዱስ የተወሰዱ ናቸው።"
    override val aboutFontTitle = "ቅርጸ-ቁምፊ"
    override val aboutFontBody = "Abyssinica SIL እና Noto Sans Ethiopic — በ SIL Open Font License 1.1።"
    override val aboutPrivacyTitle = "ግላዊነት"
    override val aboutPrivacyBody = "ይህ መተግበሪያ ምንም መረጃ አይሰበስብም። ምንም የበይነመረብ ፍቃድ የለውም።"

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
    override val snooze = "አሳድር"
    override val addPsalm = "መዝሙር ጨምር"
    override val choosePsalm = "መዝሙር ምረጥ"
    override val remove = "አስወግድ"
    override val manageHours = "ሰዓታት አስተካክል"
    override val newHour = "አዲስ ሰዓት"
    override val hourNameLabel = "የሰዓቱ ስም"
    override val rename = "ስም ቀይር"
    override val manageHoursIntro = "ሰዓታትን ይጨምሩ፣ ስም ይቀይሩ፣ ደርድሩ ወይም ይደብቁ። ክፍሎችን ለማስተካከል ሰዓቱን ይንኩ።"


    override val introTitle = "እንኳን ደህና መጡ"
    override val introBody = "የጸሎት ሰዓታት — መዝሙራትና ወንጌላት በአንድ ቦታ።"
    override val introOfflineTitle = "ከበይነመረብ ውጭ"
    override val introOfflineBody = "ሙሉ በሙሉ ከመስመር ውጭ ይሰራል። ምንም መረጃ አይሰበሰብም።"
    override val introRemindersTitle = "ማስታወሻዎች"
    override val introRemindersBody = "የራስዎን የጸሎት ሰዓታት ይምረጡ፤ በሚፈልጉት ጊዜ ያስታውሱ።"
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
}

object EnglishStrings : Strings {
    override val back = "Back"
    override val tabHome = "Home"
    override val tabSearch = "Search"
    override val tabBookmarks = "Bookmarks"
    override val tabSettings = "Settings"

    override val nowPrayer = "Prayer for now"
    override val continueReading = "Continue"

    override val contents = "Contents"
    override val readingModeToggle = "Reading mode"
    override val bookmarkAction = "Bookmark"
    override val highlight = "Highlight"
    override val removeHighlight = "Remove highlight"

    override val searchHint = "Search the prayers"
    override val noResults = "Nothing found"

    override val bookmarksTitle = "Bookmarks"
    override val noBookmarksTitle = "No bookmarks yet"
    override val noBookmarksBody = "Tap the bookmark icon on the reading page to save prayers"
    override val removeAction = "Remove"

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

    override val aboutTagline = "The Orthodox Tewahedo hours of prayer — fully offline."
    override val aboutSourceTitle = "Text source"
    override val aboutSourceBody = "Psalms and gospels are drawn from the 80-weahadu open-source Amharic Bible."
    override val aboutFontTitle = "Fonts"
    override val aboutFontBody = "Abyssinica SIL and Noto Sans Ethiopic — under the SIL Open Font License 1.1."
    override val aboutPrivacyTitle = "Privacy"
    override val aboutPrivacyBody = "This app collects no data and has no internet permission."

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
    override val snooze = "Snooze"
    override val addPsalm = "Add psalm"
    override val choosePsalm = "Choose a psalm"
    override val remove = "Remove"
    override val manageHours = "Manage hours"
    override val newHour = "New hour"
    override val hourNameLabel = "Hour name"
    override val rename = "Rename"
    override val manageHoursIntro = "Add, rename, reorder or hide hours. Tap an hour to edit its sections."


    override val introTitle = "Welcome"
    override val introBody = "The hours of prayer — psalms and gospels, in one place."
    override val introOfflineTitle = "Works offline"
    override val introOfflineBody = "Fully offline. No data is collected, ever."
    override val introRemindersTitle = "Reminders"
    override val introRemindersBody = "Choose your own times and be reminded when you want."
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
}

val LocalStrings = staticCompositionLocalOf<Strings> { AmharicStrings }
