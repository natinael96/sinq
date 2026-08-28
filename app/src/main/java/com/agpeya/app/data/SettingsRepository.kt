package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class ReadingMode { VERTICAL, HORIZONTAL }
enum class ThemeChoice { SYSTEM, LIGHT, DARK }
enum class Language { SYSTEM, AMHARIC, ENGLISH }
enum class PrayerLevel { PSALM_50, BEGINNING, GROWTH, STEADFAST, FULL }
enum class ReadingLineSpacing(val multiplier: Float) { COMPACT(1.3f), NORMAL(1.5f), RELAXED(1.75f) }

/**
 * Which bundled Ethiopic face renders the prayer text. [ABYSSINICA] is the
 * scripture-grade default; the rest are Ethiopic faces from Font.et. Stored by
 * name, so adding or removing a face never corrupts an existing preference.
 */
enum class ReadingFont { ABYSSINICA, ABAY_LIGHT, BELA_BEREKA, ZEMENAY }

/**
 * A nightly window in which reminders stay silent.
 *
 * Times are minutes past midnight. A window that wraps midnight — the normal
 * case — simply has [startMinute] greater than [endMinute].
 */
data class QuietHours(
    val enabled: Boolean = false,
    val startMinute: Int = 22 * 60,
    val endMinute: Int = 6 * 60,
) {
    /** True when [minuteOfDay] falls inside the window. */
    fun covers(minuteOfDay: Int): Boolean {
        if (!enabled) return false
        if (startMinute == endMinute) return false            // zero-length window
        return if (startMinute < endMinute) {
            minuteOfDay >= startMinute && minuteOfDay < endMinute
        } else {
            minuteOfDay >= startMinute || minuteOfDay < endMinute   // wraps midnight
        }
    }
}

/** How a fired reminder alerts: ring + vibrate, ring only, vibrate only, or silent. */
enum class AlarmAlert { SOUND_VIBRATE, SOUND_ONLY, VIBRATE_ONLY, SILENT }
/** Which sound a ringing alarm plays. */
enum class AlarmSound { ALARM, RINGTONE, NOTIFICATION }
/** How the ሰዓታት reader shows its bilingual text: paired lines, or one language. */
enum class SeatatLang { BOTH, GEEZ, AMHARIC }
/** Language used only for Gitsawe Misbak/Psalm readings. */
enum class MisbakLanguage { GEEZ, AMHARIC }

/** User preferences: reading mode, font size, theme, keep-screen-on. */
object SettingsRepository {

    @Serializable
    data class BackupSettings(
        val readingMode: String = ReadingMode.VERTICAL.name,
        val fontStep: Int = DEFAULT_FONT_STEP,
        val theme: String = ThemeChoice.SYSTEM.name,
        val readingFont: String = ReadingFont.ABYSSINICA.name,
        val lineSpacing: String = ReadingLineSpacing.NORMAL.name,
        val keepScreenOn: Boolean = true,
        val language: String = Language.SYSTEM.name,
        val prayerLevel: String = PrayerLevel.FULL.name,
        val alarmAlert: String = AlarmAlert.SOUND_VIBRATE.name,
        val alarmSound: String = AlarmSound.ALARM.name,
        val seatatLanguage: String = SeatatLang.BOTH.name,
        val misbakLanguage: String = MisbakLanguage.GEEZ.name,
        val profileName: String = "",
        val christianName: String = "",
        val journeyReminder: Boolean = true,
        val journeyReminderMinute: Int = DEFAULT_STREAK_REMINDER_MIN,
        val gitsaweReminder: Boolean = true,
        val breathReminder: Boolean = true,
        val quietEnabled: Boolean = false,
        val quietStart: Int = 22 * 60,
        val quietEnd: Int = 6 * 60,
        val almsReminders: List<com.agpeya.app.model.SpecialReminder> = emptyList(),
        val repentanceReminders: List<com.agpeya.app.model.SpecialReminder> = emptyList(),
    )

    private val KEY_READING_MODE = stringPreferencesKey("reading_mode")
    private val KEY_FONT_STEP = intPreferencesKey("font_step")
    private val KEY_FONT_STEP_V2 = intPreferencesKey("font_step_v2")
    private val KEY_FONT_SIZE_SP = intPreferencesKey("font_size_sp_v3")
    private val KEY_THEME = stringPreferencesKey("theme")
    private val KEY_READING_FONT = stringPreferencesKey("reading_font")
    private val KEY_READING_LINE_SPACING = stringPreferencesKey("reading_line_spacing")
    private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    private val KEY_LANGUAGE = stringPreferencesKey("language")
    private val KEY_PRAYER_LEVEL = stringPreferencesKey("prayer_level")
    private val KEY_ALARM_ALERT = stringPreferencesKey("alarm_alert")
    private val KEY_ALARM_SOUND = stringPreferencesKey("alarm_sound")
    private val KEY_SEATAT_LANG = stringPreferencesKey("seatat_lang")
    private val KEY_MISBAK_LANGUAGE = stringPreferencesKey("misbak_language")
    private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
    private val KEY_NAME = stringPreferencesKey("profile_name")
    private val KEY_CHRISTIAN_NAME = stringPreferencesKey("profile_christian_name")
    private val KEY_STREAK_REMINDER = booleanPreferencesKey("streak_reminder")
    private val KEY_STREAK_REMINDER_TIME = intPreferencesKey("streak_reminder_min")

    /** 21:30 — the historical fixed time, kept as the default. */
    const val DEFAULT_STREAK_REMINDER_MIN = 21 * 60 + 30
    private val KEY_GITSAWE_REMINDER = booleanPreferencesKey("gitsawe_reminder")
    // Legacy single-reminder keys — read only, to migrate the one old alms /
    // repentance reminder into the first entry of the new lists below.
    private val KEY_ALMS_REMINDER = booleanPreferencesKey("alms_reminder")
    private val KEY_ALMS_REMINDER_TIME = intPreferencesKey("alms_reminder_min")
    private val KEY_ALMS_SCHEDULE = stringPreferencesKey("alms_schedule")
    private val KEY_REPENTANCE_REMINDER = booleanPreferencesKey("repentance_reminder")
    private val KEY_REPENTANCE_REMINDER_TIME = intPreferencesKey("repentance_reminder_min")
    private val KEY_REPENTANCE_SCHEDULE = stringPreferencesKey("repentance_schedule")

    // The lists of custom reminders, one JSON list per intention.
    private val KEY_ALMS_REMINDERS = stringPreferencesKey("alms_reminders_v2")
    private val KEY_REPENTANCE_REMINDERS = stringPreferencesKey("repentance_reminders_v2")
    // Entry ids last armed, so the scheduler can cancel ones that were removed.
    private val KEY_ALMS_SCHEDULED_IDS = stringPreferencesKey("alms_scheduled_ids")
    private val KEY_REPENTANCE_SCHEDULED_IDS = stringPreferencesKey("repentance_scheduled_ids")

    /** 09:00 — morning of the chosen alms day, before the day fills up. */
    const val DEFAULT_ALMS_REMINDER_MIN = 9 * 60

    /** 19:00 — the eve; ንስሐ and communion preparation are an evening's work. */
    const val DEFAULT_REPENTANCE_REMINDER_MIN = 19 * 60
    private val KEY_BREATH_REMINDER = booleanPreferencesKey("breath_reminder")
    private val KEY_BREATH_LAST_FIRED = stringPreferencesKey("breath_last_fired_day")
    private val KEY_LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
    private val KEY_QUIET_ENABLED = booleanPreferencesKey("quiet_enabled")
    private val KEY_QUIET_START = intPreferencesKey("quiet_start_min")
    private val KEY_QUIET_END = intPreferencesKey("quiet_end_min")

    suspend fun backupSettings(context: Context): BackupSettings {
        val prefs = context.settingsDataStore.data.first()
        return BackupSettings(
            readingMode = prefs[KEY_READING_MODE] ?: ReadingMode.VERTICAL.name,
            fontStep = fontStep(context).first(),
            theme = prefs[KEY_THEME] ?: ThemeChoice.SYSTEM.name,
            readingFont = prefs[KEY_READING_FONT] ?: ReadingFont.ABYSSINICA.name,
            lineSpacing = prefs[KEY_READING_LINE_SPACING] ?: ReadingLineSpacing.NORMAL.name,
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: true,
            language = prefs[KEY_LANGUAGE] ?: Language.SYSTEM.name,
            prayerLevel = prefs[KEY_PRAYER_LEVEL] ?: PrayerLevel.FULL.name,
            alarmAlert = prefs[KEY_ALARM_ALERT] ?: AlarmAlert.SOUND_VIBRATE.name,
            alarmSound = prefs[KEY_ALARM_SOUND] ?: AlarmSound.ALARM.name,
            seatatLanguage = prefs[KEY_SEATAT_LANG] ?: SeatatLang.BOTH.name,
            misbakLanguage = prefs[KEY_MISBAK_LANGUAGE] ?: MisbakLanguage.GEEZ.name,
            profileName = prefs[KEY_NAME] ?: "",
            christianName = prefs[KEY_CHRISTIAN_NAME] ?: "",
            journeyReminder = prefs[KEY_STREAK_REMINDER] ?: true,
            journeyReminderMinute = prefs[KEY_STREAK_REMINDER_TIME] ?: DEFAULT_STREAK_REMINDER_MIN,
            gitsaweReminder = prefs[KEY_GITSAWE_REMINDER] ?: true,
            breathReminder = prefs[KEY_BREATH_REMINDER] ?: true,
            quietEnabled = prefs[KEY_QUIET_ENABLED] ?: false,
            quietStart = prefs[KEY_QUIET_START] ?: 22 * 60,
            quietEnd = prefs[KEY_QUIET_END] ?: 6 * 60,
            almsReminders = readReminders(
                prefs, KEY_ALMS_REMINDERS, KEY_ALMS_REMINDER, false,
                KEY_ALMS_REMINDER_TIME, DEFAULT_ALMS_REMINDER_MIN,
                KEY_ALMS_SCHEDULE, com.agpeya.app.model.HabitSchedule.DEFAULT_ALMS,
            ),
            repentanceReminders = readReminders(
                prefs, KEY_REPENTANCE_REMINDERS, KEY_REPENTANCE_REMINDER, true,
                KEY_REPENTANCE_REMINDER_TIME, DEFAULT_REPENTANCE_REMINDER_MIN,
                KEY_REPENTANCE_SCHEDULE, com.agpeya.app.model.HabitSchedule.DEFAULT_REPENTANCE,
            ),
        )
    }

    suspend fun restoreSettings(context: Context, value: BackupSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_READING_MODE] = runCatching { ReadingMode.valueOf(value.readingMode) }.getOrDefault(ReadingMode.VERTICAL).name
            prefs[KEY_FONT_SIZE_SP] = FONT_STEPS_SP[value.fontStep.coerceIn(FONT_STEPS_SP.indices)]
            prefs[KEY_THEME] = runCatching { ThemeChoice.valueOf(value.theme) }.getOrDefault(ThemeChoice.SYSTEM).name
            prefs[KEY_READING_FONT] = runCatching { ReadingFont.valueOf(value.readingFont) }.getOrDefault(ReadingFont.ABYSSINICA).name
            prefs[KEY_READING_LINE_SPACING] = runCatching { ReadingLineSpacing.valueOf(value.lineSpacing) }.getOrDefault(ReadingLineSpacing.NORMAL).name
            prefs[KEY_KEEP_SCREEN_ON] = value.keepScreenOn
            prefs[KEY_LANGUAGE] = runCatching { Language.valueOf(value.language) }.getOrDefault(Language.SYSTEM).name
            prefs[KEY_PRAYER_LEVEL] = runCatching { PrayerLevel.valueOf(value.prayerLevel) }.getOrDefault(PrayerLevel.FULL).name
            prefs[KEY_ALARM_ALERT] = runCatching { AlarmAlert.valueOf(value.alarmAlert) }.getOrDefault(AlarmAlert.SOUND_VIBRATE).name
            prefs[KEY_ALARM_SOUND] = runCatching { AlarmSound.valueOf(value.alarmSound) }.getOrDefault(AlarmSound.ALARM).name
            prefs[KEY_SEATAT_LANG] = runCatching { SeatatLang.valueOf(value.seatatLanguage) }.getOrDefault(SeatatLang.BOTH).name
            prefs[KEY_MISBAK_LANGUAGE] = runCatching { MisbakLanguage.valueOf(value.misbakLanguage) }.getOrDefault(MisbakLanguage.GEEZ).name
            prefs[KEY_NAME] = value.profileName.take(200)
            prefs[KEY_CHRISTIAN_NAME] = value.christianName.take(200)
            prefs[KEY_STREAK_REMINDER] = value.journeyReminder
            prefs[KEY_STREAK_REMINDER_TIME] = value.journeyReminderMinute.coerceIn(0, 1439)
            prefs[KEY_GITSAWE_REMINDER] = value.gitsaweReminder
            prefs[KEY_BREATH_REMINDER] = value.breathReminder
            prefs[KEY_QUIET_ENABLED] = value.quietEnabled
            prefs[KEY_QUIET_START] = value.quietStart.coerceIn(0, 1439)
            prefs[KEY_QUIET_END] = value.quietEnd.coerceIn(0, 1439)
            prefs[KEY_ALMS_REMINDERS] = scheduleJson.encodeToString(value.almsReminders.take(100))
            prefs[KEY_REPENTANCE_REMINDERS] = scheduleJson.encodeToString(value.repentanceReminders.take(100))
        }
    }

    /**
     * The reading sizes A−/A+ step through, shared by every reader. The stored
     * preference is an INDEX into this list, so entries may only ever be added
     * at the ends — inserting in the middle silently changes every saved size.
     * 13/15 were prepended in 0.9.9 (the old floor of 17 was still large);
     * [KEY_FONT_STEP_V2] holds the index into this list, and an old
     * [KEY_FONT_STEP] value (an index into the 17..29 tail) migrates on read
     * by the +2 offset, so nobody's saved size changes.
     */
    private val LEGACY_FONT_STEPS_SP = listOf(13, 15, 17, 19, 22, 25, 29)
    val FONT_STEPS_SP = listOf(16, 18, 20, 22, 25, 28)

    const val DEFAULT_FONT_STEP = 1 // 18sp, nearest to the historical 19sp default

    fun readingMode(context: Context): Flow<ReadingMode> =
        context.settingsDataStore.data.map {
            runCatching { ReadingMode.valueOf(it[KEY_READING_MODE] ?: "") }
                .getOrDefault(ReadingMode.VERTICAL)
        }

    suspend fun setReadingMode(context: Context, mode: ReadingMode) {
        context.settingsDataStore.edit { it[KEY_READING_MODE] = mode.name }
    }

    fun fontStep(context: Context): Flow<Int> =
        context.settingsDataStore.data.map {
            val savedSp = it[KEY_FONT_SIZE_SP] ?: run {
                val legacyIndex = it[KEY_FONT_STEP_V2]
                    ?: it[KEY_FONT_STEP]?.plus(2)
                    ?: 3 // historical 19sp default in LEGACY_FONT_STEPS_SP
                LEGACY_FONT_STEPS_SP[legacyIndex.coerceIn(0, LEGACY_FONT_STEPS_SP.lastIndex)]
            }
            FONT_STEPS_SP.indices.minByOrNull { index -> kotlin.math.abs(FONT_STEPS_SP[index] - savedSp) }
                ?: DEFAULT_FONT_STEP
        }

    suspend fun setFontStep(context: Context, step: Int) {
        val safe = step.coerceIn(0, FONT_STEPS_SP.lastIndex)
        context.settingsDataStore.edit { it[KEY_FONT_SIZE_SP] = FONT_STEPS_SP[safe] }
    }

    fun theme(context: Context): Flow<ThemeChoice> =
        context.settingsDataStore.data.map {
            runCatching { ThemeChoice.valueOf(it[KEY_THEME] ?: "") }
                .getOrDefault(ThemeChoice.SYSTEM)
        }

    suspend fun setTheme(context: Context, choice: ThemeChoice) {
        context.settingsDataStore.edit { it[KEY_THEME] = choice.name }
    }

    /** The face used for prayer/scripture body text. */
    fun readingFont(context: Context): Flow<ReadingFont> =
        context.settingsDataStore.data.map {
            runCatching { ReadingFont.valueOf(it[KEY_READING_FONT] ?: "") }
                .getOrDefault(ReadingFont.ABYSSINICA)
        }

    suspend fun setReadingFont(context: Context, font: ReadingFont) {
        context.settingsDataStore.edit { it[KEY_READING_FONT] = font.name }
    }

    fun readingLineSpacing(context: Context): Flow<ReadingLineSpacing> =
        context.settingsDataStore.data.map {
            runCatching { ReadingLineSpacing.valueOf(it[KEY_READING_LINE_SPACING] ?: "") }
                .getOrDefault(ReadingLineSpacing.NORMAL)
        }

    suspend fun setReadingLineSpacing(context: Context, value: ReadingLineSpacing) {
        context.settingsDataStore.edit { it[KEY_READING_LINE_SPACING] = value.name }
    }

    /** The ሰዓታት reader's language mode. Paired Ge'ez + Amharic by default. */
    fun seatatLang(context: Context): Flow<SeatatLang> =
        context.settingsDataStore.data.map {
            runCatching { SeatatLang.valueOf(it[KEY_SEATAT_LANG] ?: "") }.getOrDefault(SeatatLang.BOTH)
        }

    suspend fun setSeatatLang(context: Context, lang: SeatatLang) {
        context.settingsDataStore.edit { it[KEY_SEATAT_LANG] = lang.name }
    }

    fun misbakLanguage(context: Context): Flow<MisbakLanguage> =
        context.settingsDataStore.data.map {
            runCatching { MisbakLanguage.valueOf(it[KEY_MISBAK_LANGUAGE] ?: "") }
                .getOrDefault(MisbakLanguage.GEEZ)
        }

    suspend fun setMisbakLanguage(context: Context, language: MisbakLanguage) {
        context.settingsDataStore.edit { it[KEY_MISBAK_LANGUAGE] = language.name }
    }

    fun keepScreenOn(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_KEEP_SCREEN_ON] ?: true }

    suspend fun setKeepScreenOn(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[KEY_KEEP_SCREEN_ON] = value }
    }

    fun language(context: Context): Flow<Language> =
        context.settingsDataStore.data.map {
            runCatching { Language.valueOf(it[KEY_LANGUAGE] ?: "") }.getOrDefault(Language.SYSTEM)
        }

    suspend fun setLanguage(context: Context, value: Language) {
        context.settingsDataStore.edit { it[KEY_LANGUAGE] = value.name }
        com.agpeya.app.widget.GitsaweWidgetProvider.refreshAll(context)
    }

    fun prayerLevel(context: Context): Flow<PrayerLevel> =
        context.settingsDataStore.data.map {
            runCatching { PrayerLevel.valueOf(it[KEY_PRAYER_LEVEL] ?: "") }
                .getOrDefault(PrayerLevel.FULL)
        }

    suspend fun setPrayerLevel(context: Context, value: PrayerLevel) {
        context.settingsDataStore.edit { it[KEY_PRAYER_LEVEL] = value.name }
    }

    fun alarmAlert(context: Context): Flow<AlarmAlert> =
        context.settingsDataStore.data.map {
            runCatching { AlarmAlert.valueOf(it[KEY_ALARM_ALERT] ?: "") }.getOrDefault(AlarmAlert.SOUND_VIBRATE)
        }

    suspend fun setAlarmAlert(context: Context, value: AlarmAlert) {
        context.settingsDataStore.edit { it[KEY_ALARM_ALERT] = value.name }
    }

    /** Blocking read for use inside the alarm service. */
    fun alarmAlertBlocking(context: Context): AlarmAlert =
        runCatching {
            kotlinx.coroutines.runBlocking { alarmAlert(context).first() }
        }.getOrDefault(AlarmAlert.SOUND_VIBRATE)

    fun alarmSound(context: Context): Flow<AlarmSound> =
        context.settingsDataStore.data.map {
            runCatching { AlarmSound.valueOf(it[KEY_ALARM_SOUND] ?: "") }.getOrDefault(AlarmSound.ALARM)
        }

    suspend fun setAlarmSound(context: Context, value: AlarmSound) {
        context.settingsDataStore.edit { it[KEY_ALARM_SOUND] = value.name }
    }

    fun lastBackupAt(context: Context): Flow<Long> =
        context.settingsDataStore.data.map { it[KEY_LAST_BACKUP_AT] ?: 0L }

    suspend fun setLastBackupAt(context: Context, epochMillis: Long) {
        context.settingsDataStore.edit { it[KEY_LAST_BACKUP_AT] = epochMillis }
    }

    fun alarmSoundBlocking(context: Context): AlarmSound =
        runCatching {
            kotlinx.coroutines.runBlocking { alarmSound(context).first() }
        }.getOrDefault(AlarmSound.ALARM)

    // Local profile — never leaves the device (the app has no network access).
    fun profileName(context: Context): Flow<String> =
        context.settingsDataStore.data.map { it[KEY_NAME] ?: "" }

    suspend fun setProfileName(context: Context, value: String) {
        context.settingsDataStore.edit { it[KEY_NAME] = value.trim() }
    }

    fun christianName(context: Context): Flow<String> =
        context.settingsDataStore.data.map { it[KEY_CHRISTIAN_NAME] ?: "" }

    suspend fun setChristianName(context: Context, value: String) {
        context.settingsDataStore.edit { it[KEY_CHRISTIAN_NAME] = value.trim() }
    }

    /** Nightly nudge to fill in today's streak. On by default, at [streakReminderTime]. */
    fun streakReminder(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_STREAK_REMINDER] ?: true }

    suspend fun setStreakReminder(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[KEY_STREAK_REMINDER] = value }
    }

    /** When the nightly streak nudge fires, as minutes into the day. */
    fun streakReminderTime(context: Context): Flow<Int> =
        context.settingsDataStore.data.map { it[KEY_STREAK_REMINDER_TIME] ?: DEFAULT_STREAK_REMINDER_MIN }

    suspend fun setStreakReminderTime(context: Context, minuteOfDay: Int) {
        context.settingsDataStore.edit {
            it[KEY_STREAK_REMINDER_TIME] = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        }
    }

    /** For the scheduler, which arms alarms outside any coroutine (same shape as
     *  [alarmAlertBlocking]); falls back to the 21:30 default on any failure. */
    fun streakReminderTimeBlocking(context: Context): Int =
        runCatching {
            kotlinx.coroutines.runBlocking { streakReminderTime(context).first() }
        }.getOrDefault(DEFAULT_STREAK_REMINDER_MIN)

    /** Morning nudge (~06:00) with today's ግጻዌ reading heading. On by default. */
    fun gitsaweReminder(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_GITSAWE_REMINDER] ?: true }

    suspend fun setGitsaweReminder(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[KEY_GITSAWE_REMINDER] = value }
    }

    // ---- የመሃል ጸሎት — the in-between breath prayer -----------------------------
    //
    // Once a day, at a random moment between the prayer hours, a one-line
    // nudge to pray. The scheduler needs three facts: whether the nudge is on,
    // when a prayer hour was last recorded (the window opens 10 minutes after
    // it), and the last day the nudge fired (so it never fires twice).

    /** The once-a-day in-between prayer nudge. On by default. */
    fun breathReminder(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_BREATH_REMINDER] ?: true }

    suspend fun setBreathReminder(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[KEY_BREATH_REMINDER] = value }
    }

    fun breathReminderBlocking(context: Context): Boolean =
        runCatching { kotlinx.coroutines.runBlocking { breathReminder(context).first() } }
            .getOrDefault(true)

    /** "yyyy-MM-dd" of the last day the nudge fired; empty if never. */
    fun breathLastFiredDayBlocking(context: Context): String =
        runCatching {
            kotlinx.coroutines.runBlocking {
                context.settingsDataStore.data.map { it[KEY_BREATH_LAST_FIRED] ?: "" }.first()
            }
        }.getOrDefault("")

    suspend fun setBreathLastFiredDay(context: Context, day: String) {
        context.settingsDataStore.edit { it[KEY_BREATH_LAST_FIRED] = day }
    }

    // ---- Special-habit reminders (ምጽዋት / ንስሐ) ------------------------------
    //
    // Each has an on/off switch, a time of day, and a HabitSchedule saying
    // which days it is due. Blocking variants exist for the scheduler, which
    // arms alarms outside any coroutine (same shape as streakReminderTime).

    private val scheduleJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    private fun decodeSchedule(raw: String?, fallback: com.agpeya.app.model.HabitSchedule) =
        raw?.let {
            runCatching {
                scheduleJson.decodeFromString<com.agpeya.app.model.HabitSchedule>(it)
            }.getOrNull()
        } ?: fallback

    // Decode a stored list, or migrate the one legacy reminder into a single
    // entry when the list has never been written. On a fresh install there is
    // no legacy key either, so the migrated entry simply carries the built-in
    // defaults (alms: Sunday 09:00, off; repentance: Saturday 19:00, on).
    private fun readReminders(
        prefs: androidx.datastore.preferences.core.Preferences,
        listKey: androidx.datastore.preferences.core.Preferences.Key<String>,
        legacyEnabledKey: androidx.datastore.preferences.core.Preferences.Key<Boolean>,
        legacyEnabledDefault: Boolean,
        legacyTimeKey: androidx.datastore.preferences.core.Preferences.Key<Int>,
        legacyTimeDefault: Int,
        legacyScheduleKey: androidx.datastore.preferences.core.Preferences.Key<String>,
        legacyScheduleDefault: com.agpeya.app.model.HabitSchedule,
    ): List<com.agpeya.app.model.SpecialReminder> {
        prefs[listKey]?.let { raw ->
            return runCatching {
                scheduleJson.decodeFromString<List<com.agpeya.app.model.SpecialReminder>>(raw)
            }.getOrDefault(emptyList())
        }
        return listOf(
            com.agpeya.app.model.SpecialReminder(
                id = "migrated",
                label = "",
                schedule = decodeSchedule(prefs[legacyScheduleKey], legacyScheduleDefault),
                minute = prefs[legacyTimeKey] ?: legacyTimeDefault,
                enabled = prefs[legacyEnabledKey] ?: legacyEnabledDefault,
            ),
        )
    }

    fun almsReminders(context: Context): Flow<List<com.agpeya.app.model.SpecialReminder>> =
        context.settingsDataStore.data.map {
            readReminders(
                it, KEY_ALMS_REMINDERS,
                KEY_ALMS_REMINDER, false,
                KEY_ALMS_REMINDER_TIME, DEFAULT_ALMS_REMINDER_MIN,
                KEY_ALMS_SCHEDULE, com.agpeya.app.model.HabitSchedule.DEFAULT_ALMS,
            )
        }

    fun repentanceReminders(context: Context): Flow<List<com.agpeya.app.model.SpecialReminder>> =
        context.settingsDataStore.data.map {
            readReminders(
                it, KEY_REPENTANCE_REMINDERS,
                KEY_REPENTANCE_REMINDER, true,
                KEY_REPENTANCE_REMINDER_TIME, DEFAULT_REPENTANCE_REMINDER_MIN,
                KEY_REPENTANCE_SCHEDULE, com.agpeya.app.model.HabitSchedule.DEFAULT_REPENTANCE,
            )
        }

    suspend fun setAlmsReminders(context: Context, list: List<com.agpeya.app.model.SpecialReminder>) {
        context.settingsDataStore.edit { it[KEY_ALMS_REMINDERS] = scheduleJson.encodeToString(list) }
    }

    suspend fun setRepentanceReminders(
        context: Context,
        list: List<com.agpeya.app.model.SpecialReminder>,
    ) {
        context.settingsDataStore.edit {
            it[KEY_REPENTANCE_REMINDERS] = scheduleJson.encodeToString(list)
        }
    }

    fun almsRemindersBlocking(context: Context): List<com.agpeya.app.model.SpecialReminder> =
        runCatching { kotlinx.coroutines.runBlocking { almsReminders(context).first() } }
            .getOrDefault(emptyList())

    fun repentanceRemindersBlocking(context: Context): List<com.agpeya.app.model.SpecialReminder> =
        runCatching { kotlinx.coroutines.runBlocking { repentanceReminders(context).first() } }
            .getOrDefault(emptyList())

    /** Whether any alms entry is enabled — drives the Settings state subtitle. */
    fun almsReminder(context: Context): Flow<Boolean> =
        almsReminders(context).map { list -> list.any { it.enabled } }

    /** Whether any repentance entry is enabled. */
    fun repentanceReminder(context: Context): Flow<Boolean> =
        repentanceReminders(context).map { list -> list.any { it.enabled } }

    // The entry ids the scheduler last armed, so it can cancel any that the
    // person has since deleted or disabled. Stored as a newline-joined string.
    fun scheduledIds(
        context: Context,
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
    ): Set<String> =
        runCatching {
            kotlinx.coroutines.runBlocking {
                context.settingsDataStore.data.first()[key]
                    ?.split('\n')?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
            }
        }.getOrDefault(emptySet())

    fun setScheduledIds(
        context: Context,
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
        ids: Set<String>,
    ) {
        runCatching {
            kotlinx.coroutines.runBlocking {
                context.settingsDataStore.edit { it[key] = ids.joinToString("\n") }
            }
        }
    }

    val KEY_ALMS_SCHEDULED_IDS_PUBLIC get() = KEY_ALMS_SCHEDULED_IDS
    val KEY_REPENTANCE_SCHEDULED_IDS_PUBLIC get() = KEY_REPENTANCE_SCHEDULED_IDS

    // ---- Quiet hours --------------------------------------------------------
    //
    // A window in which reminders stay silent. Stored as minutes past midnight
    // so a window that crosses midnight (22:00 → 06:00) is just start > end.

    /** Minutes past midnight; defaults to 22:00–06:00, off unless enabled. */
    fun quietHours(context: Context): Flow<QuietHours> =
        context.settingsDataStore.data.map {
            QuietHours(
                enabled = it[KEY_QUIET_ENABLED] ?: false,
                startMinute = it[KEY_QUIET_START] ?: (22 * 60),
                endMinute = it[KEY_QUIET_END] ?: (6 * 60),
            )
        }

    suspend fun setQuietHours(context: Context, value: QuietHours) {
        context.settingsDataStore.edit {
            it[KEY_QUIET_ENABLED] = value.enabled
            it[KEY_QUIET_START] = value.startMinute.coerceIn(0, 24 * 60 - 1)
            it[KEY_QUIET_END] = value.endMinute.coerceIn(0, 24 * 60 - 1)
        }
    }

    /** Blocking read for the alarm receiver, which has no coroutine scope. */
    fun quietHoursBlocking(context: Context): QuietHours =
        runCatching { kotlinx.coroutines.runBlocking { quietHours(context).first() } }
            .getOrDefault(QuietHours())

    fun onboarded(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_ONBOARDED] ?: false }

    suspend fun setOnboarded(context: Context) {
        context.settingsDataStore.edit { it[KEY_ONBOARDED] = true }
    }
}
