package com.mapuia.khawchinthlirna.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mapuia.khawchinthlirna.data.model.WeatherDoc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.weatherCacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_cache")

/**
 * Offline cache for last-known successful weather doc.
 *
 * We store as JSON for strict mapping + forward compatibility.
 */
class WeatherCache(
    private val appContext: Context,
    private val gson: Gson = GsonBuilder().serializeNulls().create(),
) {
    private object Keys {
        val LAST_WEATHER_JSON = stringPreferencesKey("last_weather_json")
        val LAST_GRID_ID = stringPreferencesKey("last_grid_id")
        val LAST_FETCH_EPOCH_MS = longPreferencesKey("last_fetch_epoch_ms")
    }

    val cachedWeather: Flow<CachedWeather?> =
        appContext.weatherCacheDataStore.data.map { prefs ->
            val json = prefs[Keys.LAST_WEATHER_JSON] ?: return@map null
            val gridId = prefs[Keys.LAST_GRID_ID]
            val lastFetchMs = prefs[Keys.LAST_FETCH_EPOCH_MS] ?: 0L

            runCatching { gson.fromJson(json, WeatherDoc::class.java) }.getOrNull()?.let { doc ->
                CachedWeather(
                    gridId = gridId,
                    fetchedAtEpochMs = lastFetchMs,
                    doc = doc,
                )
            }
        }

    suspend fun save(gridId: String?, doc: WeatherDoc) {
        appContext.weatherCacheDataStore.edit { prefs ->
            prefs[Keys.LAST_WEATHER_JSON] = gson.toJson(doc)
            gridId?.let { prefs[Keys.LAST_GRID_ID] = it }
            prefs[Keys.LAST_FETCH_EPOCH_MS] = System.currentTimeMillis()
        }
    }
}

data class CachedWeather(
    val gridId: String?,
    val fetchedAtEpochMs: Long,
    val doc: WeatherDoc,
) {
    /**
     * Check if cache has expired based on CACHE_EXPIRY_MINUTES constant.
     * Returns true if cache is older than the expiry threshold.
     */
    fun isExpired(): Boolean {
        val expiryMs = WeatherConstants.CACHE_EXPIRY_MINUTES * 60 * 1000
        return System.currentTimeMillis() - fetchedAtEpochMs > expiryMs
    }
    
    /**
     * Get cache age in minutes for UI display.
     */
    fun getAgeMinutes(): Long {
        return (System.currentTimeMillis() - fetchedAtEpochMs) / (60 * 1000)
    }
    
    /**
     * Check if the weather data itself is stale (backend hasn't updated).
     * Uses the 'generated' field from the document.
     * Returns hours since data was generated, or null if can't determine.
     */
    fun getDataAgeHours(): Long? {
        val generated = doc.generated ?: return null
        return try {
            val instant = java.time.Instant.parse(generated)
            java.time.Duration.between(instant, java.time.Instant.now()).toHours()
        } catch (_: Exception) { null }
    }
    
    /**
     * Check if backend data is stale (more than 12 hours old).
     * This indicates the EC2 cron job may not be running.
     */
    fun isDataStale(): Boolean {
        val ageHours = getDataAgeHours() ?: return false
        return ageHours > 12 // Backend should update at least twice per day
    }
}

