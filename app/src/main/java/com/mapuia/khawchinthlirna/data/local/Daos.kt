package com.mapuia.khawchinthlirna.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for weather cache operations
 */
@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE gridId = :gridId")
    suspend fun getWeather(gridId: String): CachedWeatherEntity?

    @Query("SELECT * FROM weather_cache WHERE gridId = :gridId")
    fun getWeatherFlow(gridId: String): Flow<CachedWeatherEntity?>

    @Query("SELECT * FROM weather_cache")
    fun getAllWeatherFlow(): Flow<List<CachedWeatherEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: CachedWeatherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWeather(weather: List<CachedWeatherEntity>)

    @Query("DELETE FROM weather_cache WHERE gridId = :gridId")
    suspend fun deleteWeather(gridId: String)

    @Query("DELETE FROM weather_cache WHERE cachedAt < :timestamp")
    suspend fun deleteOldCache(timestamp: Long)

    @Query("DELETE FROM weather_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM weather_cache")
    suspend fun getCacheCount(): Int
}

/**
 * DAO for user reports
 */
@Dao
interface ReportDao {
    @Query("SELECT * FROM user_reports ORDER BY timestamp DESC")
    fun getAllReportsFlow(): Flow<List<CachedReportEntity>>

    @Query("SELECT * FROM user_reports WHERE userId = :userId ORDER BY timestamp DESC")
    fun getUserReportsFlow(userId: String): Flow<List<CachedReportEntity>>

    @Query("SELECT * FROM user_reports WHERE gridId = :gridId ORDER BY timestamp DESC")
    fun getReportsForGridFlow(gridId: String): Flow<List<CachedReportEntity>>

    @Query("SELECT * FROM user_reports WHERE isSynced = 0")
    suspend fun getPendingReports(): List<CachedReportEntity>

    @Query("SELECT * FROM user_reports WHERE reportId = :reportId")
    suspend fun getReport(reportId: String): CachedReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: CachedReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReports(reports: List<CachedReportEntity>)

    @Update
    suspend fun updateReport(report: CachedReportEntity)

    @Query("UPDATE user_reports SET isSynced = 1, syncError = NULL WHERE reportId = :reportId")
    suspend fun markAsSynced(reportId: String)

    @Query("UPDATE user_reports SET syncError = :error WHERE reportId = :reportId")
    suspend fun setSyncError(reportId: String, error: String)

    @Query("DELETE FROM user_reports WHERE reportId = :reportId")
    suspend fun deleteReport(reportId: String)

    @Query("DELETE FROM user_reports WHERE timestamp < :timestamp AND isSynced = 1")
    suspend fun deleteOldReports(timestamp: Long)

    @Query("SELECT COUNT(*) FROM user_reports WHERE userId = :userId")
    suspend fun getUserReportCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM user_reports WHERE isSynced = 0")
    suspend fun getPendingCount(): Int
}

/**
 * DAO for hourly forecast
 */
@Dao
interface HourlyForecastDao {
    @Query("SELECT * FROM hourly_forecast WHERE gridId = :gridId ORDER BY hour")
    fun getHourlyForecast(gridId: String): Flow<List<CachedHourlyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyForecast(forecasts: List<CachedHourlyEntity>)

    @Query("DELETE FROM hourly_forecast WHERE gridId = :gridId")
    suspend fun clearForGrid(gridId: String)

    @Query("DELETE FROM hourly_forecast WHERE cachedAt < :timestamp")
    suspend fun deleteOldForecast(timestamp: Long)
}

/**
 * DAO for favorite locations
 */
@Dao
interface FavoriteLocationDao {
    @Query("SELECT * FROM favorite_locations ORDER BY isHome DESC, addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteLocationEntity>>

    @Query("SELECT * FROM favorite_locations WHERE isHome = 1 LIMIT 1")
    suspend fun getHomeLocation(): FavoriteLocationEntity?

    @Query("SELECT * FROM favorite_locations WHERE gridId = :gridId")
    suspend fun getFavorite(gridId: String): FavoriteLocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteLocationEntity)

    @Query("UPDATE favorite_locations SET isHome = 0")
    suspend fun clearHomeFlag()

    @Query("UPDATE favorite_locations SET isHome = 1 WHERE gridId = :gridId")
    suspend fun setAsHome(gridId: String)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteLocationEntity)

    @Query("DELETE FROM favorite_locations WHERE gridId = :gridId")
    suspend fun deleteFavoriteById(gridId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_locations WHERE gridId = :gridId)")
    suspend fun isFavorite(gridId: String): Boolean
}

/**
 * DAO for notifications
 */
@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY receivedAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY receivedAt DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Long)

    @Query("DELETE FROM notifications WHERE receivedAt < :timestamp")
    suspend fun deleteOldNotifications(timestamp: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}

/**
 * DAO for user preferences
 */
@Dao
interface UserPreferencesDao {
    @Query("SELECT value FROM user_preferences WHERE key = :key")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(preference: UserPreferencesEntity)

    @Query("DELETE FROM user_preferences WHERE key = :key")
    suspend fun remove(key: String)

    @Query("DELETE FROM user_preferences")
    suspend fun clearAll()
}
