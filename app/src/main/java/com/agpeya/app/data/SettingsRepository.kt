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

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class ReadingMode { VERTICAL, HORIZONTAL }
enum class ThemeChoice { SYSTEM, LIGHT, DARK }
enum class Language { SYSTEM, AMHARIC, ENGLISH }

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

/** User preferences: reading mode, font size, theme, keep-screen-on. */
object SettingsRepository {

    private val KEY_READING_MODE = stringPreferencesKey("reading_mode")
    private val KEY_FONT_STEP = intPreferencesKey("font_step")
    private val KEY_FONT_STEP_V2 = intPreferencesKey("font_step_v2")
    private val KEY_THEME = stringPreferencesKey("theme")
    private val KEY_READING_FONT = stringPreferencesKey("reading_font")
    private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    private val KEY_LANGUAGE = stringPreferencesKey("language")
    private val KEY_ALARM_ALERT = stringPreferencesKey("alarm_alert")
    private val KEY_ALARM_SOUND = stringPreferencesKey("alarm_sound")
    private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
    private val KEY_NAME = stringPreferencesKey("profile_name")
    private val KEY_CHRISTIAN_NAME = stringPreferencesKey("profile_christian_name")
    private val KEY_STREAK_REMINDER = booleanPreferencesKey("streak_reminder")
    private val KEY_STREAK_REMINDER_TIME = intPreferencesKey("streak_reminder_min")

    /** 21:30 — the historical fixed time, kept as the default. */
    const val DEFAULT_STREAK_REMINDER_MIN = 21 * 60 + 30
    private val KEY_GITSAWE_REMINDER = booleanPreferencesKey("gitsawe_reminder")
    private val KEY_ALMS_REMINDER = booleanPreferencesKey("alms_reminder")
    private val KEY_ALMS_REMINDER_TIME = intPreferencesKey("alms_reminder_min")
    private val KEY_ALMS_SCHEDULE = stringPreferencesKey("alms_schedule")
    private val KEY_REPENTANCE_REMINDER = booleanPreferencesKey("repentance_reminder")
    private val KEY_REPENTANCE_REMINDER_TIME = intPreferencesKey("repentance_reminder_min")
    private val KEY_REPENTANCE_SCHEDULE = stringPreferencesKey("repentance_schedule")

    /** 09:00 — morning of the chosen alms day, before the day fills up. */
    const val DEFAULT_ALMS_REMINDER_MIN = 9 * 60

    /** 19:00 — the eve; ንስሐ and communion preparation are an evening's work. */
    const val DEFAULT_REPENTANCE_REMINDER_MIN = 19 * 60
    private val KEY_BREATH_REMINDER = booleanPreferencesKey("breath_reminder")
    private val KEY_BREATH_LAST_FIRED = stringPreferencesKey("breath_last_fired_day")
    private val KEY_LAST_PRAYER_RECORDED_AT = longPreferencesKey("last_prayer_recorded_at")
    private val KEY_QUIET_ENABLED = booleanPreferencesKey("quiet_enabled")
    private val KEY_QUIET_START = intPreferencesKey("quiet_start_min")
    private val KEY_QUIET_END = intPreferencesKey("quiet_end_min")

    /**
     * The reading sizes A−/A+ step through, shared by every reader. The stored
     * preference is an INDEX into this list, so entries may only ever be added
     * at the ends — inserting in the middle silently changes every saved size.
     * 13/15 were prepended in 0.9.9 (the old floor of 17 was still large);
     * [KEY_FONT_STEP_V2] holds the index into this list, and an old
     * [KEY_FONT_STEP] value (an index into the 17..29 tail) migrates on read
     * by the +2 offset, so nobody's saved size changes.
     */
    val FONT_STEPS_SP = listOf(13, 15, 17, 19, 22, 25, 29)

    const val DEFAULT_FONT_STEP = 3 // 19sp — the same default size as before 0.9.9

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
            it[KEY_FONT_STEP_V2]
                ?: it[KEY_FONT_STEP]?.plus(2)   // old index into the 17..29 tail
                ?: DEFAULT_FONT_STEP
        }

    suspend fun setFontStep(context: Context, step: Int) {
        context.settingsDataStore.edit { it[KEY_FONT_STEP_V2] = step.coerceIn(0, FONT_STEPS_SP.lastIndex) }
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

    /** Epoch millis of the last recorded prayer hour; 0 if never. */
    fun lastPrayerRecordedAtBlocking(context: Context): Long =
        runCatching {
            kotlinx.coroutines.runBlocking {
                context.settingsDataStore.data.map { it[KEY_LAST_PRAYER_RECORDED_AT] ?: 0L }.first()
            }
        }.getOrDefault(0L)

    suspend fun setLastPrayerRecordedAt(context: Context, epochMillis: Long) {
        context.settingsDataStore.edit { it[KEY_LAST_PRAYER_RECORDED_AT] = epochMillis }
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

    /** Reminder to give alms on the chosen days. Off until the user opts in. */
    fun almsReminder(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_ALMS_REMINDER] ?: false }

    suspend fun setAlmsReminder(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[KEY_ALMS_REMINDER] = value }
    }

    fun almsReminderTime(context: Context): Flow<Int> =
        context.settingsDataStore.data.map { it[KEY_ALMS_REMINDER_TIME] ?: DEFAULT_ALMS_REMINDER_MIN }

    suspend fun setAlmsReminderTime(context: Context, minuteOfDay: Int) {
        context.settingsDataStore.edit {
            it[KEY_ALMS_REMINDER_TIME] = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        }
    }

    fun almsReminderTimeBlocking(context: Context): Int =
        runCatching { kotlinx.coroutines.runBlocking { almsReminderTime(context).first() } }
            .getOrDefault(DEFAULT_ALMS_REMINDER_MIN)

    fun almsSchedule(context: Context): Flow<com.agpeya.app.model.HabitSchedule> =
        context.settingsDataStore.data.map {
            decodeSchedule(it[KEY_ALMS_SCHEDULE], com.agpeya.app.model.HabitSchedule.DEFAULT_ALMS)
        }

    suspend fun setAlmsSchedule(context: Context, value: com.agpeya.app.model.HabitSchedule) {
        context.settingsDataStore.edit { it[KEY_ALMS_SCHEDULE] = scheduleJson.encodeToString(value) }
    }

    fun almsScheduleBlocking(context: Context): com.agpeya.app.model.HabitSchedule =
        runCatching { kotlinx.coroutines.runBlocking { almsSchedule(context).first() } }
            .getOrDefault(com.agpeya.app.model.HabitSchedule.DEFAULT_ALMS)

    /** Reminder to repent and prepare for communion. ON by default. */
    fun repentanceReminder(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_REPENTANCE_REMINDER] ?: true }

    suspend fun setRepentanceReminder(context: Context, value: Boolean) {
        context.settingsDataStore.edit { it[KEY_REPENTANCE_REMINDER] = value }
    }

    fun repentanceReminderTime(context: Context): Flow<Int> =
        context.settingsDataStore.data.map {
            it[KEY_REPENTANCE_REMINDER_TIME] ?: DEFAULT_REPENTANCE_REMINDER_MIN
        }

    suspend fun setRepentanceReminderTime(context: Context, minuteOfDay: Int) {
        context.settingsDataStore.edit {
            it[KEY_REPENTANCE_REMINDER_TIME] = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        }
    }

    fun repentanceReminderTimeBlocking(context: Context): Int =
        runCatching { kotlinx.coroutines.runBlocking { repentanceReminderTime(context).first() } }
            .getOrDefault(DEFAULT_REPENTANCE_REMINDER_MIN)

    fun repentanceSchedule(context: Context): Flow<com.agpeya.app.model.HabitSchedule> =
        context.settingsDataStore.data.map {
            decodeSchedule(
                it[KEY_REPENTANCE_SCHEDULE],
                com.agpeya.app.model.HabitSchedule.DEFAULT_REPENTANCE,
            )
        }

    suspend fun setRepentanceSchedule(context: Context, value: com.agpeya.app.model.HabitSchedule) {
        context.settingsDataStore.edit {
            it[KEY_REPENTANCE_SCHEDULE] = scheduleJson.encodeToString(value)
        }
    }

    fun repentanceScheduleBlocking(context: Context): com.agpeya.app.model.HabitSchedule =
        runCatching { kotlinx.coroutines.runBlocking { repentanceSchedule(context).first() } }
            .getOrDefault(com.agpeya.app.model.HabitSchedule.DEFAULT_REPENTANCE)

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
