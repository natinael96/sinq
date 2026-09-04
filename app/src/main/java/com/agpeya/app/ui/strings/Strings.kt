package com.agpeya.app.ui.strings

import androidx.compose.runtime.staticCompositionLocalOf
import com.agpeya.app.ui.reading.geezNumeral

/**
 * All app-chrome strings in both languages. Prayer content (hour names, psalm
 * text, gospels) is never translated — it stays in Amharic as data.
 */
interface Strings {
    /** True for the Amharic chrome — lets bilingual UI (What's New, the memento
     *  mori gloss) pick the right member of a language pair. */
    val isAmharic: Boolean
    val back: String
    val tabHome: String
    val tabSearch: String
    val tabBookmarks: String
    val tabJourney: String
    val tabSettings: String
    val tabLibrary: String

    val libraryTitle: String
    val librarySubtitle: String
    val wudaseScheduleSubtitle: String
    val zewotrSubtitle: String
    val scripturesTitle: String
    val scripturesSubtitle: String
    val bahreHasabTitle: String
    val bahreHasabSubtitle: String
    val bahreHasabRange: String
    val bahreHasabCurrentYear: String
    val bahreHasabCycleValues: String
    val bahreHasabMovableDates: String
    val bahreHasabFasika: String
    val annualTable: String
    val sundayCycleTitle: String
    val sundayCycleSubtitle: String
    val supplicationLabel: String
    fun memorialDay(day: Int): String
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
    /** The ስንክሳር of a day other than today, e.g. "የሐምሌ 8 ስንክሳር". */
    fun synaxariumFor(dateLabel: String): String

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
    val showFullPsalms: String
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
    val prayerSettingsTitle: String
    val remindersSettingsTitle: String
    // The Reminders page's three groups: what arrives daily, what concerns
    // giving, and how any of it is allowed to sound.
    val remindersGroupDaily: String
    val remindersGroupGiving: String
    val remindersGroupSound: String
    val remindersOff: String
    val noBackupYet: String
    val backedUpToday: String
    val backedUpYesterday: String
    fun backedUpDays(days: Long): String
    /** The text-size stepper. Distinct from the font NAME above it. */
    val fontSizeLabel: String
    val lineSpacingLabel: String
    val textAlignmentLabel: String
    val alignJustified: String
    val alignLeft: String
    val alignRight: String
    val alignCenter: String
    val lineCompact: String
    val lineNormal: String
    val lineRelaxed: String
    val startTimeLabel: String
    val endTimeLabel: String
    val lastBackupLabel: String
    val backgroundRestrictedTitle: String
    val backgroundRestrictedBody: String
    val allowBackground: String
    val addName: String
    val addChristianName: String
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
    val whatsNew: String
    val licensesTitle: String
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
    /** The Psalter's Sunday state: no daily division is appointed — a fact, not a promise. */
    val noSundayDivision: String
    /** Generic in-progress label while a card's content loads. */
    val loadingLabel: String
    val zewotrTselot: String
    val wudaseMariam: String
    val wudaseLangAmharic: String
    val wudaseLangGeez: String
    val contentUnavailable: String
    val retryAction: String
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
    /** Warning: [count] reminders are timed inside the quiet window. */
    fun quietHoursConflict(count: Int): String
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
    val expandedState: String
    val collapsedState: String

    // Settings groups. The screen is long enough that it has to be scannable by
    // heading rather than by reading every row.
    val settingsGroupReading: String
    val settingsGroupPrayer: String
    val prayerLevelTitle: String
    val prayerLevelDescription: String
    val prayerLevelPsalm50Description: String
    val prayerLevelBeginningDescription: String
    val prayerLevelGrowthDescription: String
    val prayerLevelSteadfastDescription: String
    val prayerLevelFullDescription: String
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
    val saveImage: String
    val imageSaved: String
    val imagePreparing: String
    val imageSaveFailed: String
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
    // ── ማስታወሻ (journal) ──────────────────────────────────────────────────────

    val journalTitle: String
    val journalSubtitle: String
    val journalEmpty: String
    val journalTodayHeader: String
    val newEntry: String
    val entryBodyHint: String
    val journalKindReflection: String
    val journalKindPassage: String
    val journalKindConfession: String
    val journalKindConfessionNote: String
    val confessedAction: String
    val confessedConfirm: String
    val deleteEntryConfirm: String
    /** The Church's day an entry was written on, joined for the entry header. */
    fun writtenOn(date: String): String
    val journalMonthHeader: String
    /** Shown in the entry header once the text is on disk. */
    val entrySaved: String
    /** Reader menu: start a journal entry about the passage on screen. */
    val writeAboutThis: String

    // The passphrase gate.
    val journalLockTitle: String
    val journalLockPrompt: String
    val journalSetPassphrase: String
    val journalChangePassphrase: String
    val journalRemovePassphrase: String
    val passphraseLabel: String
    val passphraseConfirmLabel: String
    val passphraseMismatch: String
    val passphraseWrong: String
    val passphraseTooShort: String
    val passphraseNoRecovery: String
    val unlockAction: String

    // The export picker.
    val exportChooseTitle: String
    val exportChooseBody: String
    val exportSectionHabits: String
    val exportSectionBookmarks: String
    val exportSectionHighlights: String
    val exportSectionPrayerList: String
    val exportSectionSetup: String
    val exportSectionOfferings: String
    val exportSectionJournal: String
    val exportJournalWarning: String
    val exportNothingChosen: String
    val continueAction: String
    // ── አስራት and ስዕለት ───────────────────────────────────────────────────────
    //
    // The two obligations the app keeps a record of. Amounts are shown in a
    // currency the person names themselves, so the label is a string, not a
    // hardcoded ብር.

    val settingsTitheTitle: String
    val settingsTitheDesc: String
    val settingsVowTitle: String
    val settingsVowDesc: String
    val titheReminderTitle: String
    val titheReminderBody: String
    val titheChannelName: String
    val vowReminderTitle: String
    val vowReminderBody: String
    val vowChannelName: String
    /** Notification body for a vow that still owes [amount]. */
    fun vowReminderOwing(amount: String): String
    val reminderNameHintTithe: String

    /** Currency shown until the person names their own. */
    val currencyDefault: String
    val currencyLabel: String

    // The አስራት page: a reckoning over a period, then the ledger behind it.
    val titheTitle: String
    val titheIntro: String
    val periodMonth: String
    val periodYear: String
    val titheIncome: String
    val titheDue: String
    val titheGiven: String
    val titheOwed: String
    val titheSurplus: String
    val titheSettledNote: String
    val tithePercentLabel: String
    val addIncome: String
    val addGiven: String
    val incomeLabel: String
    val givenLabel: String
    val amountLabel: String
    val noteLabel: String
    val dateLabel: String
    val titheLedgerHeader: String
    val noTitheEntries: String
    val titheRemindersRow: String
    /** Subtitle of the reminders row: how many are switched on. */
    fun remindersOnCount(count: Int): String
    val previousPeriod: String
    val nextPeriod: String

    // The ስዕለት page.
    val vowsTitle: String
    val vowsIntro: String
    val addVow: String
    val noVows: String
    val vowNameLabel: String
    val vowNameHint: String
    val vowPledgeLabel: String
    val vowPledged: String
    val vowGiven: String
    val vowRemaining: String
    val vowSettled: String
    val vowOneTime: String
    val vowOneTimeDesc: String
    val recordPayment: String
    val vowPaymentsHeader: String
    val noVowPayments: String
    val deleteVowConfirm: String

    // Feast-anchored cadences, for a vow kept on a saint's day.
    val scheduleYearly: String
    val scheduleFeast: String
    /** Summary for a yearly schedule, e.g. "በየዓመቱ መስከረም 17". */
    fun yearlyOn(month: String, day: Int): String
    val chooseFeast: String
    val feastMovableNote: String
    /** The saint kept on this day of every Ethiopian month. */
    fun monthlyFeastOf(name: String): String
    val noSpecialReminders: String

    /** Header of the widget's second card: tomorrow's ግጻዌ, for preparing. */
    val tomorrowLabel: String

    /** The widget card's action line; an arrow is appended in the layout. */
    val readGitsawe: String

    // ── ንስሐ ዝግጅት (confession preparation) ───────────────────────────────────
    //
    // Nothing here is scored or streaked: the examination is read, not filled,
    // and its only product is a confession draft that discharge deletes.

    val confessionPrepTitle: String
    val confessionPrepDesc: String
    /** Step indicator, e.g. "፪ / ፭". */
    fun confessionPrepStepOf(step: Int, total: Int): String
    val confessionPrepStart: String
    val confessionPrepNoteHint: String
    val confessionPrepReviewHeader: String
    val confessionPrepNothingNoted: String
    val confessionPrepSave: String
    val confessionPrepSavedTitle: String
    val confessionPrepOpenDraft: String
    /** Leads the draft body so it is never blank; a dated fact, not a score. */
    val confessionPrepStamp: String
    /** Offered after discharge: was a ቀኖና received? */
    val confessionPrepPenancePrompt: String

    // ── ቀኖና (penance) ────────────────────────────────────────────────────────

    val settingsPenanceTitle: String
    val settingsPenanceDesc: String
    val penanceTitle: String
    val penanceIntro: String
    val penanceAdd: String
    val noPenances: String
    val penanceNameLabel: String
    val penanceNameHint: String
    val penanceKindLabel: String
    val penanceKindProstrations: String
    val penanceKindFastingDays: String
    val penanceKindAlms: String
    val penanceKindPrayers: String
    val penanceKindOther: String
    val penanceQuotaLabel: String
    val penanceDone: String
    val penanceRemaining: String
    val penanceSettled: String
    val penanceLogProgress: String
    val penanceProgressHeader: String
    val noPenanceProgress: String
    val deletePenanceConfirm: String
    /** Deliberately generic: a ቀኖና's label never rides in a notification. */
    val penanceReminderTitle: String
    val penanceReminderBody: String
    val penanceChannelName: String
    val penancePrivacyNote: String

    // ── ንባብ (reading plan) ──────────────────────────────────────────────────
    //
    // A supplement to the ግጻዌ, never a replacement: the lectionary is what the
    // Church appoints, and the plan reads what it does not reach.

    val readingTitle: String
    val readingIntro: String
    val readingChoose: String
    val readingStart: String
    val readingStop: String
    val readingStopConfirm: String
    /** Home card and screen header, e.g. "ቀን ፵፫". */
    fun readingDayLabel(day: String): String
    val readingTodayHeader: String
    val readingGitsaweHeader: String
    val readingWithGitsawe: String
    val readingMarkDone: String
    val readingDone: String
    val readingAllDays: String
    /** Footer line: days read in the current period. Never a streak. */
    fun readingDaysRead(count: Int): String
    val readingBehindTitle: String
    val readingCatchToday: String
    val readingCatchOldest: String
    val readingRedistribute: String
    /** e.g. "፫፻፷ ቀን · በቀን ፫ ምዕራፍ". */
    fun readingPlanMeta(days: String, perDay: String): String
    val readingNoPlan: String

    // ── የአዲስ እትም ማሳወቂያ (update notice) ─────────────────────────────────────
    //
    // The app's only network-facing feature, and the only strings that mention
    // the internet at all.

    /** The Home line, e.g. "አዲስ እትም · 1.7.0". */
    fun updateAvailable(version: String): String
    val updateDownload: String
    val updateDismiss: String
    val settingsUpdateCheck: String
    val settingsUpdateCheckDesc: String
    /** About row when the check is off, or nothing has been found yet. */
    val updateNoneFound: String

    // ── ቁርባን ዝግጅት (communion preparation) ──────────────────────────────────

    val kurbanPrepTitle: String
    val kurbanPrepDesc: String
    val kurbanChecklistHeader: String
    val kurbanStatusHeader: String
    val kurbanConfessionPending: String
    val kurbanPenanceUnsettled: String
    val kurbanPrePrayersHeader: String
    val kurbanPostPrayersHeader: String
}

object AmharicStrings : Strings {
    override val isAmharic = true
    override val back = "ተመለስ"
    override val tabHome = "ቤት"
    override val tabSearch = "ፍለጋ"
    override val tabBookmarks = "ምልክቶች"
    override val tabJourney = "ጉዞ"
    override val tabSettings = "ቅንብር"
    override val tabLibrary = "ቤተ መጻሕፍት"

    override val libraryTitle = "ቤተ መጻሕፍት"
    override val wudaseScheduleSubtitle = "ሰኞ–እሑድ"
    override val zewotrSubtitle = "የዕለት ጸሎቶች"
    override val librarySubtitle = "የጸሎትና የቅዱሳት መጻሕፍት ስብስብ"
    override val scripturesTitle = "ቅዱሳት መጻሕፍት"
    override val scripturesSubtitle = "የአዲስ ኪዳን መጻሕፍት"
    override val bahreHasabTitle = "ባሕረ ሐሳብ"
    override val bahreHasabSubtitle = "የበዓላትና የአጽዋማት ዓመታዊ ሠንጠረዥ"
    override val bahreHasabRange = "የአሁኑ ዓመት እና ቀጣዮቹ ፳፭ ዓመታት"
    override val bahreHasabCurrentYear = "የአሁኑ ዓመት"
    override val bahreHasabCycleValues = "የዓመቱ ስሌት"
    override val bahreHasabMovableDates = "ተንቀሳቃሽ በዓላትና አጽዋማት"
    override val bahreHasabFasika = "ትንሣኤ"
    override val annualTable = "ዓመታዊ ሠንጠረዥ"
    override val sundayCycleTitle = "ግጻዌ ዘሰናብት ወመዝሙር"
    override val sundayCycleSubtitle = "የዕለቱ የሰንበት ሥርዓት"
    override val supplicationLabel = "መስተበቍዕ"
    override fun memorialDay(day: Int) = "$day ቀን"
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
    override fun synaxariumFor(dateLabel: String) = "የ$dateLabel ስንክሳር"

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
    override val showFullPsalms = "ሙሉ መዝሙራት"
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

    override val searchHint = "በጸሎቶችና በመዝሙራት ውስጥ ይፈልጉ"
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
    override val gitsaweReminderBody = "የዛሬን ምንባብ ይመልከቱ"
    override val gitsaweChannelName = "የዕለቱ ግጻዌ ማስታወሻ"
    override val settingsGitsaweReminder = "የዕለቱ ግጻዌ ማስታወሻ"
    override val settingsGitsaweReminderDesc = "በየቀኑ ጠዋት የዕለቱን የግጻዌ ምንባብ ያስታውስዎታል"
    override val settingsNightReminder = "የሌሊት ማስታወሻ"
    override val settingsNightReminderDesc = "ጸሎት ቢመዘገብም በየሌሊቱ ስንክሳርን፣ ቤተ ክርስቲያንንና ስግደትን ያስታውስዎታል"
    override val notifDisabledTitle = "ማሳወቂያዎች ጠፍተዋል"
    override val notifDisabledBody = "ማንቂያዎችዎ እንዲደርሱዎት የመተግበሪያውን ማሳወቂያዎች ከቅንብሮች ያብሩ።"

    override val bookmarksTitle = "ምልክቶች"
    override val noBookmarksTitle = "ገና ምልክት አላደረጉም"
    override val noBookmarksBody = "በንባብ ገጹ ላይ ያለውን የምልክት ምልክት በመንካት ጸሎቶችን ያስቀምጡ"
    override val removeAction = "አስወግድ"
    override val bookmarkGroupScripture = "ቅዱሳት መጻሕፍት"
    override val bookmarkGroupSynaxarium = "ስንክሳር"

    override val settingsTitle = "ቅንብሮች"
    override val prayerSettingsTitle = "ጸሎት"
    override val remindersSettingsTitle = "ማስታወሻዎች"
    override val remindersGroupDaily = "ዕለታዊ"
    override val remindersGroupGiving = "ምጽዋትና ስዕለት"
    override val remindersGroupSound = "ድምፅና ጸጥታ"
    override val remindersOff = "ጠፍቷል"
    override val noBackupYet = "እስካሁን ምትኬ የለም"
    override val backedUpToday = "ዛሬ ምትኬ ተቀምጧል"
    override val backedUpYesterday = "ትናንት ምትኬ ተቀምጧል"
    override fun backedUpDays(days: Long) = "ከ$days ቀናት በፊት ምትኬ ተቀምጧል"
    override val fontSizeLabel = "የፊደል መጠን"
    override val lineSpacingLabel = "የመስመር ክፍተት"
    override val textAlignmentLabel = "የጽሑፍ አሰላለፍ"
    override val alignJustified = "በሁለቱም ጠርዝ"
    override val alignLeft = "በግራ"
    override val alignRight = "በቀኝ"
    override val alignCenter = "መሃል"
    override val lineCompact = "ጠባብ"
    override val lineNormal = "መደበኛ"
    override val lineRelaxed = "ሰፊ"
    override val startTimeLabel = "መጀመሪያ"
    override val endTimeLabel = "መጨረሻ"
    override val lastBackupLabel = "የመጨረሻ ምትኬ"
    override val backgroundRestrictedTitle = "የጀርባ እንቅስቃሴ ተገድቧል"
    override val backgroundRestrictedBody = "የጸሎት ማስታወሻዎች በሰዓቱ እንዲደርሱ የጀርባ እንቅስቃሴን ይፍቀዱ።"
    override val allowBackground = "የጀርባ እንቅስቃሴን ፍቀድ"
    override val addName = "ስም ጨምር"
    override val addChristianName = "የክርስትና ስም ጨምር"
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
    override val whatsNew = "ምን አዲስ ነገር አለ"
    override val licensesTitle = "ፈቃዶች እና ምንጮች"
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
    override val noSundayDivision = "ለእሑድ የተመደበ የዕለት ክፍል የለም"
    override val loadingLabel = "በመጫን ላይ…"
    override val zewotrTselot = "ዘወትር ጸሎት"
    override val wudaseMariam = "ውዳሴ ማርያም"
    override val wudaseLangAmharic = "አማርኛ"
    override val wudaseLangGeez = "ግዕዝ"
    override val contentUnavailable = "ይዘቱን ማግኘት አልተቻለም"
    override val retryAction = "እንደገና ይሞክሩ"
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
    override val quietHoursDesc = "በሌሊት ማንኛውም ማስታወሻ ድምፅ አያሰማም"
    override fun quietHoursRange(from: String, to: String) = "ከ$from እስከ $to ድምፅ የለም"
    override fun quietHoursConflict(count: Int) =
        "$count ማስታወሻ በዚህ ክፍተት ውስጥ ተይዟል፤ በጸጥታው ጊዜ አይደርስዎትም።"
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
    override val choosePsalm = "መዝሙር ይምረጡ"
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
    override val batteryStepUnrestrict = "የባትሪ ገደብ ያንሱ"
    override val batteryStepUnrestrictBody = "ቅንብሮች → ባትሪ → ይህን መተግበሪያ ያልተገደበ ያድርጉ (Unrestricted)።"
    override val batteryStepAutostart = "በራስ ማስጀመር ይፍቀዱ"
    override val batteryStepAutostartBody = "Xiaomi/Samsung ላሉ ስልኮች Autostart ይፍቀዱ፤ ከ«የሚተኙ መተግበሪያዎች» ያስወግዱ።"
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
    override val expandedState = "ተከፍቷል"
    override val collapsedState = "ተዘግቷል"

    override val settingsGroupReading = "ንባብ"
    override val settingsGroupPrayer = "ጸሎትና ማስታወሻ"
    override val prayerLevelTitle = "የጸሎት ደረጃ"
    override val prayerLevelDescription = "እንደ ጊዜዎና አቅምዎ የሚነበቡትን መዝሙራት ብዛት ይምረጡ፤ ወንጌል ሁልጊዜ ይነበባል።"
    override val prayerLevelPsalm50Description = "መዝሙር ፶ ብቻ፤ ከዚያ ሌሎች ጸሎቶችና ወንጌል"
    override val prayerLevelBeginningDescription = "በእያንዳንዱ ሰዓት 3 መዝሙራት፤ በሌሊት 7"
    override val prayerLevelGrowthDescription = "በእያንዳንዱ ሰዓት 7 መዝሙራት፤ በሌሊት 14"
    override val prayerLevelSteadfastDescription = "በእያንዳንዱ ሰዓት 10 መዝሙራት፤ በሌሊት 24"
    override val prayerLevelFullDescription = "ለእያንዳንዱ ሰዓት የተመደቡት መዝሙራት በሙሉ"
    override val settingsGroupData = "መረጃ"
    override val settingsGroupMore = "ተጨማሪ"

    override val backupFailedBody =
        "ምትኬው አልተቀመጠም። በመተግበሪያው ውስጥ ያለው መረጃዎ እንደነበረ አለ። ሌላ ቦታ ወይም ሌላ ስም መርጠው እንደገና ይሞክሩ።"
    override val restoreFailedBody =
        "ፋይሉ አልተነበበም። ያለዎት ጉዞ፣ ምልክቶችና ማድመቂያዎች አልተነኩም። የስንቅ ምትኬ ፋይል (.json) መሆኑን አረጋግጠው እንደገና ይሞክሩ።"
    override val contentMissingTitle = "ይህ ክፍል አልተገኘም"
    override val contentMissingBody =
        "ጽሑፉ በዚህ እትም ውስጥ የለም። የቀሩት ክፍሎች እንደተለመደው ይሠራሉ።"

    override val shareAsImage = "እንደ ምስል አጋራ"
    override val saveImage = "ምስሉን አስቀምጥ"
    override val imageSaved = "ምስሉ በPictures/Sinq ተቀምጧል"
    override val imagePreparing = "ምስል በመዘጋጀት ላይ…"
    override val imageSaveFailed = "ምስሉን ማስቀመጥ አልተቻለም። እንደገና ይሞክሩ።"
    override val shareFailed = "ማጋራት አልተቻለም። እንደገና ይሞክሩ።"
    override val kidaseHeader = "የዕለቱ ቅዳሴ"

    override val prayerListTitle = "የጸሎት ዝርዝር"
    override val addPerson = "ሰው ጨምር"
    override val editPerson = "አስተካክል"
    override val personNameLabel = "ስም"
    override val prayerNoteLabel = "ማስታወሻ (አማራጭ)"
    override val noPrayerListTitle = "ገና ማንም አልተጨመረም"
    override val noPrayerListBody = "በጸሎት የሚያስቧቸውን ሰዎች እዚህ ይጨምሩ።"

    override val settingsAlmsReminder = "የምጽዋት ማስታወሻ"
    override val settingsAlmsReminderDesc = "በመረጡት ቀን ምጽዋት እንዲሰጡ ያስታውስዎታል"
    override val settingsRepentReminder = "የንስሐ ማስታወሻ"
    override val settingsRepentReminderDesc = "ንስሐ እንዲገቡና ቅዱስ ቁርባን እንዲቀበሉ ያስታውስዎታል"
    override val almsReminderTitle = "የምጽዋት ቀን"
    override val almsReminderBody = "ዛሬ ምጽዋት የሚሰጡበት ቀን ነው።"
    override val almsChannelName = "የምጽዋት ማስታወሻ"
    override val repentReminderTitle = "ንስሐ"
    override val repentReminderBody = "ንስሐ መግባትን ያስቡ፤ ለቅዱስ ቁርባን ይዘጋጁ።"
    override val repentChannelName = "የንስሐ ማስታወሻ"

    override val settingsBreathReminder = "የሕሊና ጸሎት"
    override val settingsBreathReminderDesc = "በቀን አንዴ፣ በሰዓታት መካከል ባልታሰበ ጊዜ አጭር ጸሎት ያስታውስዎታል"
    override val breathReminderTitle = "ለአፍታ ይጸልዩ"
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

    override val journalTitle = "ማስታወሻ"
    override val journalSubtitle = "የዕለቱን ሐሳብ ይጻፉ"
    override val journalEmpty = "ገና ምንም አልተጻፈም።"
    override val journalTodayHeader = "የዛሬ"
    override val newEntry = "አዲስ ማስታወሻ"
    override val entryBodyHint = "ዛሬ ምን ሆነ? ምን ተሰማዎት?"
    override val journalKindReflection = "ሐሳብ"
    override val journalKindPassage = "ከምንባብ"
    override val journalKindConfession = "የንስሐ መዘጋጃ"
    override val journalKindConfessionNote =
        "ይህ ከመሣሪያዎ አይወጣም፤ በምትኬም ውስጥ አይገባም። ንስሐ ከገቡ በኋላ ይሰረዛል።"
    override val confessedAction = "ንስሐ ገብቻለሁ"
    override val confessedConfirm = "ሁሉም የንስሐ መዘጋጃዎች ይሰረዛሉ። ይህ አይመለስም።"
    override val deleteEntryConfirm = "ይህ ማስታወሻ ይሰረዛል።"
    override fun writtenOn(date: String) = "የተጻፈው፦ $date"
    override val journalMonthHeader = "የወሩ ማስታወሻዎች"
    override val entrySaved = "ተቀምጧል"
    override val writeAboutThis = "ስለዚህ ጻፍ"

    override val journalLockTitle = "ማስታወሻ ተቆልፏል"
    override val journalLockPrompt = "ለመክፈት የይለፍ ቃልዎን ያስገቡ"
    override val journalSetPassphrase = "የይለፍ ቃል አዘጋጅ"
    override val journalChangePassphrase = "የይለፍ ቃል ቀይር"
    override val journalRemovePassphrase = "ቁልፉን አንሳ"
    override val passphraseLabel = "የይለፍ ቃል"
    override val passphraseConfirmLabel = "እንደገና ያስገቡ"
    override val passphraseMismatch = "ሁለቱ አይመሳሰሉም"
    override val passphraseWrong = "የይለፍ ቃሉ ተሳስቷል"
    override val passphraseTooShort = "ቢያንስ 4 ፊደል ይሁን"
    override val passphraseNoRecovery =
        "ይህን የይለፍ ቃል ከረሱት የሚመልስበት መንገድ የለም። ማስታወሻዎችዎም አይከፈቱም።"
    override val unlockAction = "ክፈት"

    override val exportChooseTitle = "ምን ይቀመጥ?"
    override val exportChooseBody = "ወደ ፋይል የሚወጣውን ይምረጡ።"
    override val exportSectionHabits = "የጉዞ መዝገብ"
    override val exportSectionBookmarks = "ዕልባቶች"
    override val exportSectionHighlights = "ማድመቂያዎች"
    override val exportSectionPrayerList = "የጸሎት ዝርዝር"
    override val exportSectionSetup = "ቅንብሮችና ሰዓታት"
    override val exportSectionOfferings = "አስራትና ስዕለት"
    override val exportSectionJournal = "ማስታወሻ"
    override val exportJournalWarning =
        "ማስታወሻዎቹ በፋይሉ ውስጥ በግልጽ ይጻፋሉ፤ ፋይሉን የከፈተ ሁሉ ያነባቸዋል። የንስሐ መዘጋጃዎችና ቀኖና ግን አይወጡም።"
    override val exportNothingChosen = "ቢያንስ አንዱን ይምረጡ"
    override val continueAction = "ቀጥል"

    override val settingsTitheTitle = "አስራት"
    override val settingsTitheDesc = "ገቢዎን መዝግበው አስራትዎን ይከታተሉ"
    override val settingsVowTitle = "ስዕለት"
    override val settingsVowDesc = "የተሳሉትን በበዓል ቀን ያስታውሱ፤ አፈጻጸሙንም ይከታተሉ"
    override val titheReminderTitle = "የአስራት ቀን"
    override val titheReminderBody = "አስራትዎን የሚያወጡበት ቀን ነው።"
    override val titheChannelName = "የአስራት ማስታወሻ"
    override val vowReminderTitle = "ስዕለት"
    override val vowReminderBody = "ዛሬ የተሳሉትን የሚፈጽሙበት ቀን ነው።"
    override val vowChannelName = "የስዕለት ማስታወሻ"
    override fun vowReminderOwing(amount: String) = "ዛሬ የስዕለትዎ ቀን ነው። ቀሪ፦ $amount"
    override val reminderNameHintTithe = "ለምሳሌ፦ የወር አስራት"

    override val currencyDefault = "ብር"
    override val currencyLabel = "ገንዘብ"

    override val titheTitle = "አስራት"
    override val titheIntro = "ገቢዎን ሲመዘግቡ አሥራቱ ራሱ ይሰላል፤ የሰጡትንም መዝግበው ቀሪውን ያዩታል።"
    override val periodMonth = "ወር"
    override val periodYear = "ዓመት"
    override val titheIncome = "ገቢ"
    override val titheDue = "የሚገባ"
    override val titheGiven = "የተሰጠ"
    override val titheOwed = "ቀሪ"
    override val titheSurplus = "ትርፍ"
    override val titheSettledNote = "የዚህ ጊዜ አስራት ተሟልቷል።"
    override val tithePercentLabel = "የአስራት ድርሻ (%)"
    override val addIncome = "ገቢ መዝግብ"
    override val addGiven = "የሰጡትን መዝግብ"
    override val incomeLabel = "ገቢ"
    override val givenLabel = "የተሰጠ"
    override val amountLabel = "መጠን"
    override val noteLabel = "ማስታወሻ (አማራጭ)"
    override val dateLabel = "ቀን"
    override val titheLedgerHeader = "መዝገብ"
    override val noTitheEntries = "ገና ምንም አልተመዘገበም።"
    override val titheRemindersRow = "የአስራት ማስታወሻዎች"
    override fun remindersOnCount(count: Int) =
        if (count == 0) "ማስታወሻ የለም" else "$count ማስታወሻ በሥራ ላይ"
    override val previousPeriod = "ያለፈው"
    override val nextPeriod = "ቀጣዩ"

    override val vowsTitle = "ስዕለት"
    override val vowsIntro = "የተሳሉትን ስዕለት ከበዓል ቀን ጋር አስረው ያስቀምጡ፤ ሲፈጽሙም ይመዝግቡ።"
    override val addVow = "ስዕለት ጨምር"
    override val noVows = "ገና ስዕለት አልተጨመረም።"
    override val vowNameLabel = "ስም"
    override val vowNameHint = "ለምሳሌ፦ ለቅዱስ ገብርኤል"
    override val vowPledgeLabel = "የተሳሉት መጠን (አማራጭ)"
    override val vowPledged = "የተሳሉት"
    override val vowGiven = "የተከፈለ"
    override val vowRemaining = "ቀሪ"
    override val vowSettled = "ተፈጽሟል"
    override val vowOneTime = "አንድ ጊዜ ብቻ"
    override val vowOneTimeDesc = "ከተፈጸመ በኋላ ማስታወሱ ይቆማል"
    override val recordPayment = "ክፍያ መዝግብ"
    override val vowPaymentsHeader = "የተፈጸመ"
    override val noVowPayments = "ገና አልተፈጸመም።"
    override val deleteVowConfirm = "ይህ ስዕለት ከነመዝገቡ ይሰረዛል።"

    override val scheduleYearly = "በዓመት"
    override val scheduleFeast = "በበዓል"
    override fun yearlyOn(month: String, day: Int) = "በየዓመቱ $month $day"
    override val chooseFeast = "በዓል ይምረጡ"
    override val feastMovableNote = "በባሕረ ሓሳብ በየዓመቱ ይሰላል"
    override fun monthlyFeastOf(name: String) = "የዕለቱ በዓል፦ $name"
    override val noSpecialReminders = "ገና ማስታወሻ አልተጨመረም።"
    override val tomorrowLabel = "ነገ"
    override val readGitsawe = "ግጻዌውን ክፈት"

    // ── ንስሐ ዝግጅት ────────────────────────────────────────────────────────────

    override val confessionPrepTitle = "የንስሐ ዝግጅት"
    override val confessionPrepDesc = "ልብን መርምሮ ለንስሐ መዘጋጀት"
    override fun confessionPrepStepOf(step: Int, total: Int) = "$step / $total"
    override val confessionPrepStart = "መመርመር ጀምር"
    override val confessionPrepNoteHint = "የሚያስታውሱት ካለ ይጻፉ (አማራጭ)"
    override val confessionPrepReviewHeader = "የተጻፈባቸው ክፍሎች"
    override val confessionPrepNothingNoted = "ምንም አልተጻፈም፤ መመርመሩ ብቻ ይመዘገባል።"
    override val confessionPrepSave = "እንደ ረቂቅ አስቀምጥ"
    override val confessionPrepSavedTitle = "ተቀምጧል"
    override val confessionPrepOpenDraft = "ረቂቁን ክፈት"
    override val confessionPrepStamp = "የኅሊና ምርመራ ተደርጓል።"
    override val confessionPrepPenancePrompt = "ከንስሐ አባትዎ ቀኖና ተቀብለዋል? ለማስታወስ ይመዝግቡት።"

    // ── ቀኖና ─────────────────────────────────────────────────────────────────

    override val settingsPenanceTitle = "ቀኖና"
    override val settingsPenanceDesc = "የተቀበሉትን ቀኖና ይከታተሉ፤ በዚህ መሣሪያ ብቻ ይቀራል"
    override val penanceTitle = "ቀኖና"
    override val penanceIntro = "ከንስሐ አባትዎ የተቀበሉትን ቀኖና እዚህ ይያዙ። እስኪፈጸም ያስታውሰዎታል፤ ከተፈጸመ በኋላ ዝም ይላል።"
    override val penanceAdd = "ቀኖና ጨምር"
    override val noPenances = "ምንም ቀኖና የለም"
    override val penanceNameLabel = "መግለጫ (አማራጭ)"
    override val penanceNameHint = "ለምሳሌ፦ ፵ ስግደት"
    override val penanceKindLabel = "ዓይነት"
    override val penanceKindProstrations = "ስግደት"
    override val penanceKindFastingDays = "ጾም (ቀናት)"
    override val penanceKindAlms = "ምጽዋት"
    override val penanceKindPrayers = "ጸሎት"
    override val penanceKindOther = "ሌላ"
    override val penanceQuotaLabel = "መጠን"
    override val penanceDone = "የተፈጸመ"
    override val penanceRemaining = "የቀረ"
    override val penanceSettled = "ተፈጽሟል"
    override val penanceLogProgress = "መዝግብ"
    override val penanceProgressHeader = "የተመዘገበ"
    override val noPenanceProgress = "ገና ምንም አልተመዘገበም"
    override val deletePenanceConfirm = "ይህ ቀኖና ይሰረዛል። አይመለስም።"
    override val penanceReminderTitle = "ቀኖና"
    override val penanceReminderBody = "የያዙት ቀኖና አለ።"
    override val penanceChannelName = "የቀኖና ማስታወሻ"
    override val penancePrivacyNote = "ቀኖና በመጠባበቂያ ቅጂ ውስጥ አይካተትም፤ በዚህ መሣሪያ ብቻ ይቀራል።"

    // ── ንባብ ────────────────────────────────────────────────────────────────

    override val readingTitle = "ንባብ"
    override val readingIntro =
        "ግጻዌው ወንጌልንና መልእክታትን ያነብልዎታል፤ ይህ ንባብ ደግሞ ብሉይ ኪዳንንና መጻሕፍቱን ያስነብብዎታል።"
    override val readingChoose = "ንባብ ይምረጡ"
    override val readingStart = "ጀምር"
    override val readingStop = "አቁም"
    override val readingStopConfirm = "ንባቡ ይቆማል። ያነበቡት ግን ተጠብቆ ይቀራል።"
    override fun readingDayLabel(day: String) = "ቀን $day"
    override val readingTodayHeader = "የዕለቱ ንባብ"
    override val readingGitsaweHeader = "የዕለቱ ግጻዌ"
    override val readingWithGitsawe = "ከዕለቱ ግጻዌ ጋር"
    override val readingMarkDone = "አነበብኩ"
    override val readingDone = "ተነቧል"
    override val readingAllDays = "ሁሉንም ቀናት"
    override fun readingDaysRead(count: Int) = "$count ቀናት ተነብቧል"
    override val readingBehindTitle = "ያልተነበቡ ቀናት አሉ"
    override val readingCatchToday = "ዛሬ ላይ ቀጥል"
    override val readingCatchOldest = "ካልተነበበው ቀጥል"
    override val readingRedistribute = "ቀሪውን አከፋፍል"
    override fun readingPlanMeta(days: String, perDay: String) = "$days ቀን · በቀን $perDay ምዕራፍ"
    override val readingNoPlan = "ገና ንባብ አልጀመሩም"

    // ── የአዲስ እትም ማሳወቂያ ────────────────────────────────────────────────────

    override fun updateAvailable(version: String) = "አዲስ እትም · $version"
    override val updateDownload = "አውርድ"
    override val updateDismiss = "ዝጋ"
    override val settingsUpdateCheck = "አዲስ እትም ፈልግ"
    override val settingsUpdateCheckDesc =
        "በቀን አንዴ ጊትሀብን ይጠይቃል። የመተግበሪያው ብቸኛ የኢንተርኔት አገልግሎት ነው።"
    override val updateNoneFound = "አዲስ እትም የለም"

    // ── ቁርባን ዝግጅት ──────────────────────────────────────────────────────────

    override val kurbanPrepTitle = "የቁርባን ዝግጅት"
    override val kurbanPrepDesc = "ሥርዓተ መቅረቢያውና ጸሎቶቹ"
    override val kurbanChecklistHeader = "ሥርዓተ መቅረቢያ"
    override val kurbanStatusHeader = "ዝግጅት"
    override val kurbanConfessionPending = "ያልተናዘዙት የንስሐ ረቂቅ አለ።"
    override val kurbanPenanceUnsettled = "ያልተፈጸመ ቀኖና አለ።"
    override val kurbanPrePrayersHeader = "ጸሎት ዘቅድመ ቁርባን"
    override val kurbanPostPrayersHeader = "ጸሎት ዘድኅረ ቁርባን"
}

object EnglishStrings : Strings {
    override val isAmharic = false
    override val back = "Back"
    override val tabHome = "Home"
    override val tabSearch = "Search"
    override val tabBookmarks = "Bookmarks"
    override val tabJourney = "Journey"
    override val tabSettings = "Settings"
    override val tabLibrary = "Library"

    override val libraryTitle = "Library"
    override val wudaseScheduleSubtitle = "Monday–Sunday prayers"
    override val zewotrSubtitle = "Daily prayers"
    override val librarySubtitle = "Prayers and holy scriptures"
    override val scripturesTitle = "Scriptures"
    override val scripturesSubtitle = "The New Testament"
    override val bahreHasabTitle = "Bahre Hasab"
    override val bahreHasabSubtitle = "Annual feasts and fasts table"
    override val bahreHasabRange = "Current year and the next 25 years"
    override val bahreHasabCurrentYear = "Current Ethiopian year"
    override val bahreHasabCycleValues = "Cycle values"
    override val bahreHasabMovableDates = "Movable feasts and fasts"
    override val bahreHasabFasika = "Fasika"
    override val annualTable = "Annual table"
    override val sundayCycleTitle = "Sunday Gitsawe and hymns"
    override val sundayCycleSubtitle = "Sunday readings for the day"
    override val supplicationLabel = "Supplication"
    override fun memorialDay(day: Int) = "Day $day"
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
    override fun synaxariumFor(dateLabel: String) = "Synaxarium for $dateLabel"

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
    override val showFullPsalms = "Full Psalms"
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
    override val settingsGitsaweReminderDesc = "Reminds you of today's Gitsawe reading each morning"
    override val settingsNightReminder = "Nightly reminder"
    override val settingsNightReminderDesc = "Reminds you each evening — even after prayer is marked — of the Synaxarium, church, and prostrations"
    override val notifDisabledTitle = "Notifications are off"
    override val notifDisabledBody = "Turn on notifications in settings so your reminders can reach you."

    override val bookmarksTitle = "Bookmarks"
    override val noBookmarksTitle = "No bookmarks yet"
    override val noBookmarksBody = "Tap the bookmark icon on the reading page to save prayers"
    override val removeAction = "Remove"
    override val bookmarkGroupScripture = "Scriptures"
    override val bookmarkGroupSynaxarium = "Synaxarium"

    override val settingsTitle = "Settings"
    override val prayerSettingsTitle = "Prayer"
    override val remindersSettingsTitle = "Reminders"
    override val remindersGroupDaily = "Daily"
    override val remindersGroupGiving = "Giving and vows"
    override val remindersGroupSound = "Sound and silence"
    override val remindersOff = "Off"
    override val noBackupYet = "No backup yet"
    override val backedUpToday = "Backed up today"
    override val backedUpYesterday = "Backed up yesterday"
    override fun backedUpDays(days: Long) = "Backed up $days days ago"
    override val fontSizeLabel = "Text size"
    override val lineSpacingLabel = "Line spacing"
    override val textAlignmentLabel = "Text alignment"
    override val alignJustified = "Justified"
    override val alignLeft = "Align left"
    override val alignRight = "Align right"
    override val alignCenter = "Center"
    override val lineCompact = "Compact"
    override val lineNormal = "Normal"
    override val lineRelaxed = "Relaxed"
    override val startTimeLabel = "Start"
    override val endTimeLabel = "End"
    override val lastBackupLabel = "Last backup"
    override val backgroundRestrictedTitle = "Background activity is restricted"
    override val backgroundRestrictedBody = "Allow background activity so prayer reminders continue on time."
    override val allowBackground = "Allow background"
    override val addName = "Add name"
    override val addChristianName = "Add baptismal name"
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
    override val whatsNew = "What's new"
    override val licensesTitle = "Licenses & sources"
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
    override fun remindersOn(count: Int) =
        if (count == 1) "1 reminder on" else "$count reminders on"
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
    override val noSundayDivision = "No daily division is appointed for Sunday"
    override val loadingLabel = "Loading…"
    override val zewotrTselot = "ዘወትር ጸሎት"
    override val wudaseMariam = "ውዳሴ ማርያም"
    override val wudaseLangAmharic = "Amharic"
    override val wudaseLangGeez = "Ge'ez"
    override val contentUnavailable = "Content unavailable"
    override val retryAction = "Try again"
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
    override val restoreDone = "Backup restored."
    override val restoreFailed = "Could not read that file."
    override val previousYear = "Previous year"
    override val nextYear = "Next year"
    override val quietHours = "Quiet hours"
    override val quietHoursDesc = "Silence every reminder overnight"
    override fun quietHoursRange(from: String, to: String) = "Silent from $from to $to"
    override fun quietHoursConflict(count: Int) =
        if (count == 1) "1 reminder is set inside this window and will not reach you"
        else "$count reminders are set inside this window and will not reach you"
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
    override val expandedState = "Expanded"
    override val collapsedState = "Collapsed"

    override val settingsGroupReading = "Reading"
    override val settingsGroupPrayer = "Prayer and reminders"
    override val prayerLevelTitle = "Prayer level"
    override val prayerLevelDescription = "Choose the number of Psalms for your time and ability. The Gospel is always included."
    override val prayerLevelPsalm50Description = "Psalm 50 only, followed by the other prayers and Gospel"
    override val prayerLevelBeginningDescription = "3 Psalms per hour · 7 at Midnight"
    override val prayerLevelGrowthDescription = "7 Psalms per hour · 14 at Midnight"
    override val prayerLevelSteadfastDescription = "10 Psalms per hour · 24 at Midnight"
    override val prayerLevelFullDescription = "Every Psalm assigned to each hour"
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
    override val saveImage = "Save image"
    override val imageSaved = "Saved to Pictures/Sinq"
    override val imagePreparing = "Preparing image…"
    override val imageSaveFailed = "Couldn't save the image. Please try again."
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
    override val settingsAlmsReminderDesc = "Reminds you to give alms on the days you choose"
    override val settingsRepentReminder = "Repentance reminder"
    override val settingsRepentReminderDesc = "Reminds you to repent and prepare for Holy Communion"
    override val almsReminderTitle = "A day to give"
    override val almsReminderBody = "Today is your day to give alms."
    override val almsChannelName = "Almsgiving reminder"
    override val repentReminderTitle = "Repentance"
    override val repentReminderBody = "Remember repentance — and prepare for Holy Communion."
    override val repentChannelName = "Repentance reminder"

    override val settingsBreathReminder = "Prayer of the heart"
    override val settingsBreathReminderDesc = "Reminds you once a day, at an unplanned moment between the hours, with one short prayer"
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

    override val journalTitle = "Journal"
    override val journalSubtitle = "Write down the day"
    override val journalEmpty = "Nothing written yet."
    override val journalTodayHeader = "Today"
    override val newEntry = "New entry"
    override val entryBodyHint = "What happened today? What did you feel?"
    override val journalKindReflection = "Reflection"
    override val journalKindPassage = "From a reading"
    override val journalKindConfession = "Preparing for confession"
    override val journalKindConfessionNote =
        "This never leaves your device and is never put in a backup. It is deleted once you have confessed."
    override val confessedAction = "I have confessed"
    override val confessedConfirm = "Every confession draft will be deleted. This cannot be undone."
    override val deleteEntryConfirm = "This entry will be deleted."
    override fun writtenOn(date: String) = "Written on $date"
    override val journalMonthHeader = "This month"
    override val entrySaved = "Saved"
    override val writeAboutThis = "Write about this"

    override val journalLockTitle = "Journal locked"
    override val journalLockPrompt = "Enter your passphrase to open it"
    override val journalSetPassphrase = "Set a passphrase"
    override val journalChangePassphrase = "Change passphrase"
    override val journalRemovePassphrase = "Remove the lock"
    override val passphraseLabel = "Passphrase"
    override val passphraseConfirmLabel = "Enter it again"
    override val passphraseMismatch = "These do not match"
    override val passphraseWrong = "That passphrase is not right"
    override val passphraseTooShort = "Use at least 4 characters"
    override val passphraseNoRecovery =
        "If you forget this passphrase there is no way to recover it, and your journal will not open."
    override val unlockAction = "Unlock"

    override val exportChooseTitle = "What should be saved?"
    override val exportChooseBody = "Choose what goes into the file."
    override val exportSectionHabits = "Journey record"
    override val exportSectionBookmarks = "Bookmarks"
    override val exportSectionHighlights = "Highlights"
    override val exportSectionPrayerList = "Prayer list"
    override val exportSectionSetup = "Settings and hours"
    override val exportSectionOfferings = "Tithe and vows"
    override val exportSectionJournal = "Journal"
    override val exportJournalWarning =
        "Journal entries are written into the file as plain text — anyone who opens it can read them. Confession drafts and penances are never included."
    override val exportNothingChosen = "Choose at least one"
    override val continueAction = "Continue"

    override val settingsTitheTitle = "Tithe"
    override val settingsTitheDesc = "Record what you receive and track the tithe on it"
    override val settingsVowTitle = "Vows and pledges"
    override val settingsVowDesc = "Be reminded on the feast you promised, and record what you keep"
    override val titheReminderTitle = "Tithe day"
    override val titheReminderBody = "Today is the day you set your tithe aside."
    override val titheChannelName = "Tithe reminder"
    override val vowReminderTitle = "A vow to keep"
    override val vowReminderBody = "Today is the day you promised."
    override val vowChannelName = "Vow reminder"
    override fun vowReminderOwing(amount: String) = "Today is the day you promised. Still owing: $amount"
    override val reminderNameHintTithe = "e.g. Monthly tithe"

    override val currencyDefault = "Birr"
    override val currencyLabel = "Currency"

    override val titheTitle = "Tithe"
    override val titheIntro = "Record what you receive and the tenth is worked out for you; record what you give and you can see what is left."
    override val periodMonth = "Month"
    override val periodYear = "Year"
    override val titheIncome = "Received"
    override val titheDue = "Owed"
    override val titheGiven = "Given"
    override val titheOwed = "Remaining"
    override val titheSurplus = "Beyond the tithe"
    override val titheSettledNote = "This period's tithe is fully given."
    override val tithePercentLabel = "Share of income (%)"
    override val addIncome = "Record income"
    override val addGiven = "Record giving"
    override val incomeLabel = "Received"
    override val givenLabel = "Given"
    override val amountLabel = "Amount"
    override val noteLabel = "Note (optional)"
    override val dateLabel = "Date"
    override val titheLedgerHeader = "Ledger"
    override val noTitheEntries = "Nothing recorded yet."
    override val titheRemindersRow = "Tithe reminders"
    override fun remindersOnCount(count: Int) =
        if (count == 0) "No reminders" else "$count reminder${if (count == 1) "" else "s"} on"
    override val previousPeriod = "Previous"
    override val nextPeriod = "Next"

    override val vowsTitle = "Vows and pledges"
    override val vowsIntro = "Tie a vow to the feast you promised it on, and record it when you keep it."
    override val addVow = "Add a vow"
    override val noVows = "No vows yet."
    override val vowNameLabel = "Name"
    override val vowNameHint = "e.g. For St Gabriel"
    override val vowPledgeLabel = "Amount promised (optional)"
    override val vowPledged = "Promised"
    override val vowGiven = "Kept"
    override val vowRemaining = "Remaining"
    override val vowSettled = "Fulfilled"
    override val vowOneTime = "Once only"
    override val vowOneTimeDesc = "Stops reminding once it has been kept"
    override val recordPayment = "Record what you kept"
    override val vowPaymentsHeader = "Kept so far"
    override val noVowPayments = "Nothing recorded yet."
    override val deleteVowConfirm = "This vow and its record will be deleted."

    override val scheduleYearly = "Yearly"
    override val scheduleFeast = "On a feast"
    override fun yearlyOn(month: String, day: Int) = "Every year, $month $day"
    override val chooseFeast = "Choose a feast"
    override val feastMovableNote = "Computed each year from the Bahre Hasab"
    override fun monthlyFeastOf(name: String) = "Feast of the day: $name"
    override val noSpecialReminders = "No reminders yet."
    override val tomorrowLabel = "Tomorrow"
    override val readGitsawe = "Read the Gitsawe"

    // ── Confession preparation ───────────────────────────────────────────────

    override val confessionPrepTitle = "Preparing for confession"
    override val confessionPrepDesc = "Examine the heart and prepare for confession"
    override fun confessionPrepStepOf(step: Int, total: Int) = "$step / $total"
    override val confessionPrepStart = "Begin the examination"
    override val confessionPrepNoteHint = "Write anything you want to remember (optional)"
    override val confessionPrepReviewHeader = "Sections written under"
    override val confessionPrepNothingNoted = "Nothing was written; only the examination itself is kept."
    override val confessionPrepSave = "Keep as a draft"
    override val confessionPrepSavedTitle = "Kept"
    override val confessionPrepOpenDraft = "Open the draft"
    override val confessionPrepStamp = "An examination of conscience was made."
    override val confessionPrepPenancePrompt = "Did your father confessor give you a penance? Record it so it is not forgotten."

    // ── Penance ──────────────────────────────────────────────────────────────

    override val settingsPenanceTitle = "Penance"
    override val settingsPenanceDesc = "Keep the penance you were given; it stays on this device only"
    override val penanceTitle = "Penance"
    override val penanceIntro = "Keep here the penance your father confessor gave you. It reminds you until it is finished, and then falls silent."
    override val penanceAdd = "Add a penance"
    override val noPenances = "No penance is held"
    override val penanceNameLabel = "Description (optional)"
    override val penanceNameHint = "e.g. 40 prostrations"
    override val penanceKindLabel = "Kind"
    override val penanceKindProstrations = "Prostrations"
    override val penanceKindFastingDays = "Fasting (days)"
    override val penanceKindAlms = "Alms"
    override val penanceKindPrayers = "Prayers"
    override val penanceKindOther = "Other"
    override val penanceQuotaLabel = "Measure"
    override val penanceDone = "Done"
    override val penanceRemaining = "Remaining"
    override val penanceSettled = "Fulfilled"
    override val penanceLogProgress = "Record"
    override val penanceProgressHeader = "Recorded"
    override val noPenanceProgress = "Nothing recorded yet"
    override val deletePenanceConfirm = "This penance will be deleted. This cannot be undone."
    override val penanceReminderTitle = "Penance"
    override val penanceReminderBody = "You are keeping a penance."
    override val penanceChannelName = "Penance reminder"
    override val penancePrivacyNote = "Penances are never included in a backup; they stay on this device only."

    // ── Reading plan ─────────────────────────────────────────────────────────

    override val readingTitle = "Reading"
    override val readingIntro =
        "The Gitsawe reads you the Gospels and the Epistles; this reads the Old Testament and the books it does not reach."
    override val readingChoose = "Choose a reading"
    override val readingStart = "Begin"
    override val readingStop = "Stop"
    override val readingStopConfirm = "The reading stops. What you have read is kept."
    override fun readingDayLabel(day: String) = "Day $day"
    override val readingTodayHeader = "Today's reading"
    override val readingGitsaweHeader = "Today's Gitsawe"
    override val readingWithGitsawe = "With the day's Gitsawe"
    override val readingMarkDone = "I have read it"
    override val readingDone = "Read"
    override val readingAllDays = "All days"
    override fun readingDaysRead(count: Int) = "$count days read"
    override val readingBehindTitle = "Some days are unread"
    override val readingCatchToday = "Continue from today"
    override val readingCatchOldest = "Continue from the oldest"
    override val readingRedistribute = "Spread the rest out"
    override fun readingPlanMeta(days: String, perDay: String) = "$days days · $perDay chapters a day"
    override val readingNoPlan = "No reading started yet"

    // ── Update notice ────────────────────────────────────────────────────────

    override fun updateAvailable(version: String) = "New version · $version"
    override val updateDownload = "Get it"
    override val updateDismiss = "Dismiss"
    override val settingsUpdateCheck = "Check for updates"
    override val settingsUpdateCheckDesc =
        "Asks GitHub once a day. The app's only use of the internet."
    override val updateNoneFound = "No new version"

    // ── Communion preparation ────────────────────────────────────────────────

    override val kurbanPrepTitle = "Preparing for Communion"
    override val kurbanPrepDesc = "The order of approach and its prayers"
    override val kurbanChecklistHeader = "The order of approach"
    override val kurbanStatusHeader = "Preparation"
    override val kurbanConfessionPending = "A confession draft is still waiting."
    override val kurbanPenanceUnsettled = "An unfinished penance remains."
    override val kurbanPrePrayersHeader = "Prayer before Communion"
    override val kurbanPostPrayersHeader = "Prayer after Communion"
}

val LocalStrings = staticCompositionLocalOf<Strings> { AmharicStrings }
