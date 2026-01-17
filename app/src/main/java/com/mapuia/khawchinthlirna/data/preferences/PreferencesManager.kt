package com.mapuia.khawchinthlirna.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "khawchin_settings")

/**
 * Manages app preferences using DataStore
 */
class PreferencesManager(private val context: Context) {

    companion object {
        // Language
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        
        // Theme
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val USE_SYSTEM_THEME_KEY = booleanPreferencesKey("use_system_theme")
        
        // Notifications
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val SEVERE_WEATHER_ALERTS_KEY = booleanPreferencesKey("severe_weather_alerts")
        
        // Units
        private val TEMPERATURE_UNIT_KEY = stringPreferencesKey("temperature_unit")
        
        // Location
        private val HOME_GRID_ID_KEY = stringPreferencesKey("home_grid_id")
        private val HOME_GRID_NAME_KEY = stringPreferencesKey("home_grid_name")
        private val LAST_LAT_KEY = doublePreferencesKey("last_lat")
        private val LAST_LNG_KEY = doublePreferencesKey("last_lng")
        
        // User
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val IS_ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
        
        // Cache
        private val LAST_SYNC_TIME_KEY = longPreferencesKey("last_sync_time")
    }

    // === Language ===
    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LANGUAGE_KEY] ?: "mz"
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language
        }
    }

    // === Theme ===
    val darkModeFlow: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        if (prefs[USE_SYSTEM_THEME_KEY] == true) null
        else prefs[DARK_MODE_KEY]
    }

    suspend fun setDarkMode(darkMode: Boolean?) {
        context.dataStore.edit { prefs ->
            if (darkMode == null) {
                prefs[USE_SYSTEM_THEME_KEY] = true
            } else {
                prefs[USE_SYSTEM_THEME_KEY] = false
                prefs[DARK_MODE_KEY] = darkMode
            }
        }
    }

    // === Notifications ===
    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NOTIFICATIONS_ENABLED_KEY] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    val severeWeatherAlertsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SEVERE_WEATHER_ALERTS_KEY] ?: true
    }

    suspend fun setSevereWeatherAlerts(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SEVERE_WEATHER_ALERTS_KEY] = enabled
        }
    }

    // === Units ===
    val temperatureUnitFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TEMPERATURE_UNIT_KEY] ?: "celsius"
    }

    suspend fun setTemperatureUnit(unit: String) {
        context.dataStore.edit { prefs ->
            prefs[TEMPERATURE_UNIT_KEY] = unit
        }
    }

    // === Home Location ===
    val homeLocationFlow: Flow<Pair<String, String>?> = context.dataStore.data.map { prefs ->
        val gridId = prefs[HOME_GRID_ID_KEY]
        val gridName = prefs[HOME_GRID_NAME_KEY]
        if (gridId != null && gridName != null) Pair(gridId, gridName)
        else null
    }

    suspend fun setHomeLocation(gridId: String, gridName: String) {
        context.dataStore.edit { prefs ->
            prefs[HOME_GRID_ID_KEY] = gridId
            prefs[HOME_GRID_NAME_KEY] = gridName
        }
    }

    val lastLocationFlow: Flow<Pair<Double, Double>?> = context.dataStore.data.map { prefs ->
        val lat = prefs[LAST_LAT_KEY]
        val lng = prefs[LAST_LNG_KEY]
        if (lat != null && lng != null) Pair(lat, lng)
        else null
    }

    suspend fun setLastLocation(lat: Double, lng: Double) {
        context.dataStore.edit { prefs ->
            prefs[LAST_LAT_KEY] = lat
            prefs[LAST_LNG_KEY] = lng
        }
    }

    // === User ===
    val userIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_ID_KEY]
    }

    suspend fun setUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId != null) {
                prefs[USER_ID_KEY] = userId
            } else {
                prefs.remove(USER_ID_KEY)
            }
        }
    }

    val isOnboardingCompleteFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_ONBOARDING_COMPLETE_KEY] ?: false
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_ONBOARDING_COMPLETE_KEY] = complete
        }
    }

    // === Cache ===
    val lastSyncTimeFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LAST_SYNC_TIME_KEY] ?: 0L
    }

    suspend fun setLastSyncTime(time: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SYNC_TIME_KEY] = time
        }
    }

    // === Clear all ===
    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

/**
 * All settings combined
 */
data class AppSettings(
    val language: String = "mz",
    val darkMode: Boolean? = null, // null = system
    val notificationsEnabled: Boolean = true,
    val severeWeatherAlerts: Boolean = true,
    val temperatureUnit: String = "celsius",
    val homeGridId: String? = null,
    val homeGridName: String? = null
)
