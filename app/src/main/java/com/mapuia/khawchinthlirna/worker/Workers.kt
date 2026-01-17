package com.mapuia.khawchinthlirna.worker

import android.content.Context
import androidx.work.*
import com.google.firebase.firestore.FirebaseFirestore
import com.mapuia.khawchinthlirna.data.local.KhawchinDatabase
import com.mapuia.khawchinthlirna.widget.WeatherWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker for syncing pending reports that couldn't be uploaded immediately.
 */
class SyncReportsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = KhawchinDatabase.getInstance(applicationContext)
            val firestore = FirebaseFirestore.getInstance()

            // Get pending reports
            val pendingReports = database.reportDao().getPendingReports()

            if (pendingReports.isEmpty()) {
                return@withContext Result.success()
            }

            var successCount = 0
            var failureCount = 0

            for (report in pendingReports) {
                try {
                    // Prepare report data
                    val reportData = hashMapOf(
                        "user_id" to report.userId,
                        "grid_id" to report.gridId,
                        "grid_name" to report.gridName,
                        "lat" to report.lat,
                        "lng" to report.lng,
                        "condition" to report.condition,
                        "intensity" to report.intensity,
                        "temperature" to report.temperature,
                        "humidity" to report.humidity,
                        "wind_speed" to report.windSpeed,
                        "wind_direction" to report.windDirection,
                        "visibility" to report.visibility,
                        "note" to report.note,
                        "timestamp" to report.timestamp,
                        "is_synced" to true
                    )

                    // Upload to Firestore
                    firestore.collection("weather_reports")
                        .document(report.reportId)
                        .set(reportData)
                        .await()

                    // Mark as synced
                    database.reportDao().markAsSynced(report.reportId)
                    successCount++

                } catch (e: Exception) {
                    // Mark error
                    database.reportDao().setSyncError(report.reportId, e.message ?: "Unknown error")
                    failureCount++
                }
            }

            if (failureCount > 0 && successCount == 0) {
                Result.retry()
            } else {
                Result.success(
                    workDataOf(
                        "synced_count" to successCount,
                        "failed_count" to failureCount
                    )
                )
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

/**
 * Worker for periodically refreshing weather data
 */
class WeatherRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = KhawchinDatabase.getInstance(applicationContext)
            val firestore = FirebaseFirestore.getInstance()

            // Get home location from preferences
            val prefs = applicationContext.getSharedPreferences("khawchin_prefs", Context.MODE_PRIVATE)
            val homeGridId = prefs.getString("home_grid_id", null)

            if (homeGridId != null) {
                // Fetch latest weather for home location
                val weatherDoc = firestore.collection("weather_v69_grid")
                    .document(homeGridId)
                    .get()
                    .await()

                if (weatherDoc.exists()) {
                    // Update widget
                    val temp = weatherDoc.getDouble("current.temp_c")?.toInt()?.toString() ?: "--"
                    val conditionText = weatherDoc.getString("current.condition.text") ?: ""
                    val conditionCode = weatherDoc.getLong("current.condition.code")?.toInt()
                    val humidity = weatherDoc.getLong("current.humidity")?.toString() ?: "--"
                    val location = weatherDoc.getString("location.name") ?: "Mizoram"

                    WeatherWidgetUpdater.updateWidgetData(
                        context = applicationContext,
                        location = location,
                        temperature = temp,
                        condition = conditionText,
                        conditionEmoji = WeatherWidgetUpdater.getWeatherEmoji(conditionCode),
                        humidity = humidity,
                        lastUpdated = "Just now"
                    )
                }
            }

            // Clean up old cache
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            database.weatherCacheDao().deleteOldCache(oneDayAgo)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

/**
 * Worker for cleaning up old data
 */
class CleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = KhawchinDatabase.getInstance(applicationContext)
            
            // Delete old cached weather (older than 1 day)
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            database.weatherCacheDao().deleteOldCache(oneDayAgo)
            
            // Delete old hourly forecasts (older than 6 hours)
            val sixHoursAgo = System.currentTimeMillis() - (6 * 60 * 60 * 1000)
            database.hourlyForecastDao().deleteOldForecast(sixHoursAgo)
            
            // Delete old synced reports (older than 30 days)
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            database.reportDao().deleteOldReports(thirtyDaysAgo)
            
            // Delete old notifications (older than 7 days)
            val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
            database.notificationDao().deleteOldNotifications(sevenDaysAgo)
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

/**
 * Helper object to schedule workers
 */
object WorkScheduler {
    
    fun scheduleSyncWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncReportsWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1,
                TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "sync_reports",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }

    fun schedulePeriodicWeatherRefresh(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val refreshRequest = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(
            30, TimeUnit.MINUTES, // Repeat every 30 minutes
            5, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "weather_refresh",
                ExistingPeriodicWorkPolicy.KEEP,
                refreshRequest
            )
    }

    fun scheduleDailyCleanup(context: Context) {
        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "daily_cleanup",
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
    }

    fun cancelAllWork(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }
}
