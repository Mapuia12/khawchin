package com.mapuia.khawchinthlirna.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Cached weather data entity
 */
@Entity(tableName = "weather_cache")
data class CachedWeatherEntity(
    @PrimaryKey
    val gridId: String,
    val gridName: String,
    val stationName: String?,
    val temperature: Double?,
    val humidity: Int?,
    val windSpeed: Double?,
    val windDirection: String?,
    val condition: String?,
    val conditionCode: Int?,
    val feelsLike: Double?,
    val uvIndex: Double?,
    val visibility: Double?,
    val pressure: Double?,
    val cloudCover: Int?,
    val precipitationChance: Int?,
    val dailyMinTemp: Double?,
    val dailyMaxTemp: Double?,
    val sunrise: String?,
    val sunset: String?,
    val localTime: String?,
    val timezone: String?,
    val rawHourlyDataJson: String?, // JSON serialized hourly data
    val lastUpdated: Long, // Server update time
    val cachedAt: Long = System.currentTimeMillis() // When we cached it
)

/**
 * Cached user report entity (pending upload or cached from server)
 */
@Entity(tableName = "user_reports")
data class CachedReportEntity(
    @PrimaryKey
    val reportId: String,
    val userId: String,
    val gridId: String,
    val gridName: String,
    val lat: Double,
    val lng: Double,
    val condition: String,
    val intensity: String,
    val temperature: Int?,
    val humidity: Int?,
    val windSpeed: String?,
    val windDirection: String?,
    val visibility: String?,
    val note: String?,
    val timestamp: Long,
    val isSynced: Boolean = false, // Whether it's been uploaded
    val syncError: String? = null, // Error message if sync failed
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val isVerified: Boolean = false
)

/**
 * Cached hourly forecast entity
 */
@Entity(tableName = "hourly_forecast", primaryKeys = ["gridId", "hour"])
data class CachedHourlyEntity(
    val gridId: String,
    val hour: Int, // 0-23
    val temperature: Double?,
    val humidity: Int?,
    val windSpeed: Double?,
    val conditionCode: Int?,
    val precipitationChance: Int?,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * User preferences stored locally
 */
@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey
    val key: String,
    val value: String
)

/**
 * Favorite locations entity
 */
@Entity(tableName = "favorite_locations")
data class FavoriteLocationEntity(
    @PrimaryKey
    val gridId: String,
    val gridName: String,
    val lat: Double,
    val lng: Double,
    val isHome: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Notification history entity
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    val type: String, // "weather_alert", "report_verified", "badge_earned", etc.
    val data: String?, // JSON data
    val isRead: Boolean = false,
    val receivedAt: Long = System.currentTimeMillis()
)

/**
 * Type converters for Room
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        return value?.let {
            val type = object : TypeToken<List<Long>>() {}.type
            gson.fromJson(it, type)
        }
    }
}
