package ru.tgmaksim.activium.utilities.datastore

import android.content.Context

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

private const val SETTINGS_DATASTORE_NAME = "settings"
private val Context.settingsDataStore by preferencesDataStore(name = SETTINGS_DATASTORE_NAME)

object SettingsManager {
    private lateinit var appContext: Context

    private val KEY_SESSION_ID = stringPreferencesKey("session_id")
    private val KEY_FIREBASE_MESSAGING_TOKEN = stringPreferencesKey("firebase_messaging_token")
    private val KEY_ACTIVE_CHILD_ID = longPreferencesKey("active_child_id")
    private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
    private val KEY_EA_NOTIFICATIONS = booleanPreferencesKey("ea_notifications")
    private val KEY_BEFORE_SCHEDULE = intPreferencesKey("before_schedule")
    private val KEY_AFTER_SCHEDULE = intPreferencesKey("after_schedule")

    private const val DEFAULT_ACTIVE_CHILD_ID = -1L
    private const val DEFAULT_DARK_THEME = false
    private const val DEFAULT_EA_NOTIFICATIONS = false
    private const val DEFAULT_BEFORE_SCHEDULE = 3
    private const val DEFAULT_AFTER_SCHEDULE = 3

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun getSessionId(): String? {
        return appContext.settingsDataStore.data.first()[KEY_SESSION_ID]
    }

    suspend fun setSessionId(value: String?) {
        appContext.settingsDataStore.edit { prefs ->
            if (value == null) {
                prefs.remove(KEY_SESSION_ID)
            } else {
                prefs[KEY_SESSION_ID] = value
            }
        }
    }

    fun sessionIdFlow(): Flow<String?> {
        return appContext.settingsDataStore.data.map { prefs ->
            prefs[KEY_SESSION_ID]
        }
    }

    suspend fun getFirebaseMessagingToken(): String? {
        return appContext.settingsDataStore.data.first()[KEY_FIREBASE_MESSAGING_TOKEN]
    }

    suspend fun setFirebaseMessagingToken(value: String?) {
        appContext.settingsDataStore.edit { prefs ->
            if (value == null) {
                prefs.remove(KEY_FIREBASE_MESSAGING_TOKEN)
            } else {
                prefs[KEY_FIREBASE_MESSAGING_TOKEN] = value
            }
        }
    }

    fun firebaseMessagingTokenFlow(): Flow<String?> {
        return appContext.settingsDataStore.data.map { prefs ->
            prefs[KEY_FIREBASE_MESSAGING_TOKEN]
        }
    }

    suspend fun getActiveChildId(): Long {
        return appContext.settingsDataStore.data.first()[KEY_ACTIVE_CHILD_ID] ?: DEFAULT_ACTIVE_CHILD_ID
    }

    suspend fun setActiveChildId(value: Long) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[KEY_ACTIVE_CHILD_ID] = value
        }
    }

    fun activeChildIdFlow(): Flow<Long> {
        return appContext.settingsDataStore.data.map { prefs ->
            prefs[KEY_ACTIVE_CHILD_ID] ?: DEFAULT_ACTIVE_CHILD_ID
        }
    }

    suspend fun getDarkTheme(): Boolean {
        return appContext.settingsDataStore.data.first()[KEY_DARK_THEME] ?: DEFAULT_DARK_THEME
    }

    suspend fun setDarkTheme(value: Boolean) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[KEY_DARK_THEME] = value
        }
    }

    fun darkThemeFlow(): Flow<Boolean> {
        return appContext.settingsDataStore.data.map { prefs ->
            prefs[KEY_DARK_THEME] ?: DEFAULT_DARK_THEME
        }
    }

    suspend fun getEaNotifications(): Boolean {
        return appContext.settingsDataStore.data.first()[KEY_EA_NOTIFICATIONS] ?: DEFAULT_EA_NOTIFICATIONS
    }

    suspend fun setEaNotifications(value: Boolean) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[KEY_EA_NOTIFICATIONS] = value
        }
    }

    fun eaNotificationsFlow(): Flow<Boolean> {
        return appContext.settingsDataStore.data.map { prefs ->
            prefs[KEY_EA_NOTIFICATIONS] ?: DEFAULT_EA_NOTIFICATIONS
        }
    }

    suspend fun getBeforeSchedule(): Int {
        return appContext.settingsDataStore.data.first()[KEY_BEFORE_SCHEDULE] ?: DEFAULT_BEFORE_SCHEDULE
    }

    suspend fun setBeforeSchedule(value: Int) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[KEY_BEFORE_SCHEDULE] = value
        }
    }

    fun beforeScheduleFlow(): Flow<Int> {
        return appContext.settingsDataStore.data.map { prefs ->
            prefs[KEY_BEFORE_SCHEDULE] ?: DEFAULT_BEFORE_SCHEDULE
        }
    }

    suspend fun getAfterSchedule(): Int {
        return appContext.settingsDataStore.data.first()[KEY_AFTER_SCHEDULE] ?: DEFAULT_AFTER_SCHEDULE
    }

    suspend fun setAfterSchedule(value: Int) {
        appContext.settingsDataStore.edit { prefs ->
            prefs[KEY_AFTER_SCHEDULE] = value
        }
    }

    fun afterScheduleFlow(): Flow<Int> {
        return appContext.settingsDataStore.data.map { prefs ->
            prefs[KEY_AFTER_SCHEDULE] ?: DEFAULT_AFTER_SCHEDULE
        }
    }

    suspend fun clearAll() {
        appContext.settingsDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun snapshot(): SettingsSnapshot {
        val prefs = appContext.settingsDataStore.data.first()
        return SettingsSnapshot(
            sessionId = prefs[KEY_SESSION_ID],
            activeChildId = prefs[KEY_ACTIVE_CHILD_ID] ?: DEFAULT_ACTIVE_CHILD_ID,
            darkTheme = prefs[KEY_DARK_THEME] ?: DEFAULT_DARK_THEME,
            eaNotifications = prefs[KEY_EA_NOTIFICATIONS] ?: DEFAULT_EA_NOTIFICATIONS,
            beforeSchedule = prefs[KEY_BEFORE_SCHEDULE] ?: DEFAULT_BEFORE_SCHEDULE,
            afterSchedule = prefs[KEY_AFTER_SCHEDULE] ?: DEFAULT_AFTER_SCHEDULE
        )
    }
}
