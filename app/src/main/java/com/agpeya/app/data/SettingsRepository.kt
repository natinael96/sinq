package com.agpeya.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

enum class ReadingMode { VERTICAL, HORIZONTAL }
enum class ThemeChoice { SYSTEM, LIGHT, DARK }
enum class Language { SYSTEM, AMHARIC, ENGLISH }

/** How a fired reminder alerts: ring + vibrate, ring only, vibrate only, or silent. */
enum class AlarmAlert { SOUND_VIBRATE, SOUND_ONLY, VIBRATE_ONLY, SILENT }
/** Which sound a ringing alarm plays. */
enum class AlarmSound { ALARM, RINGTONE, NOTIFICATION }

/** User preferences: reading mode, font size, theme, keep-screen-on. */
object SettingsRepository {

    private val KEY_READING_MODE = stringPreferencesKey("reading_mode")
    private val KEY_FONT_STEP = intPreferencesKey("font_step")
    private val KEY_THEME = stringPreferencesKey("theme")
    private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    private val KEY_LANGUAGE = stringPreferencesKey("language")
    private val KEY_ALARM_ALERT = stringPreferencesKey("alarm_alert")
    private val KEY_ALARM_SOUND = stringPreferencesKey("alarm_sound")
    private val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
    private val KEY_NAME = stringPreferencesKey("profile_name")
    private val KEY_CHRISTIAN_NAME = stringPreferencesKey("profile_christian_name")

    const val DEFAULT_FONT_STEP = 1

    fun readingMode(context: Context): Flow<ReadingMode> =
        context.settingsDataStore.data.map {
            runCatching { ReadingMode.valueOf(it[KEY_READING_MODE] ?: "") }
                .getOrDefault(ReadingMode.VERTICAL)
        }

    suspend fun setReadingMode(context: Context, mode: ReadingMode) {
        context.settingsDataStore.edit { it[KEY_READING_MODE] = mode.name }
    }

    fun fontStep(context: Context): Flow<Int> =
        context.settingsDataStore.data.map { it[KEY_FONT_STEP] ?: DEFAULT_FONT_STEP }

    suspend fun setFontStep(context: Context, step: Int) {
        context.settingsDataStore.edit { it[KEY_FONT_STEP] = step }
    }

    fun theme(context: Context): Flow<ThemeChoice> =
        context.settingsDataStore.data.map {
            runCatching { ThemeChoice.valueOf(it[KEY_THEME] ?: "") }
                .getOrDefault(ThemeChoice.SYSTEM)
        }

    suspend fun setTheme(context: Context, choice: ThemeChoice) {
        context.settingsDataStore.edit { it[KEY_THEME] = choice.name }
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

    fun onboarded(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[KEY_ONBOARDED] ?: false }

    suspend fun setOnboarded(context: Context) {
        context.settingsDataStore.edit { it[KEY_ONBOARDED] = true }
    }
}
