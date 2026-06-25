package com.mapuia.khawchinthlirna.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.mapuia.khawchinthlirna.MainActivity
import com.mapuia.khawchinthlirna.R
import com.mapuia.khawchinthlirna.data.WeatherCache
import com.mapuia.khawchinthlirna.data.WeatherConstants
import com.mapuia.khawchinthlirna.data.WeatherRepository
import com.mapuia.khawchinthlirna.data.model.DailyArrays
import com.mapuia.khawchinthlirna.data.model.WeatherDoc
import com.mapuia.khawchinthlirna.data.preferences.PreferencesManager
import com.mapuia.khawchinthlirna.data.preferences.SelectedLocationMode
import com.mapuia.khawchinthlirna.service.NotificationChannels
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class DailyWeatherNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val preferencesManager = PreferencesManager(applicationContext)
        val notificationsEnabled = preferencesManager.notificationsEnabledFlow.first()
        if (!notificationsEnabled) return Result.success()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        return try {
            val language = preferencesManager.languageFlow.first()
            val isMizo = language == "mz"
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build()
            val repository = WeatherRepository(
                db = FirebaseFirestore.getInstance(),
                cache = WeatherCache(applicationContext),
                httpClient = httpClient,
            )

            val selected = preferencesManager.selectedLocationFlow.first()
            val homeLocation = preferencesManager.homeLocationFlow.first()
            val lastLocation = preferencesManager.lastLocationFlow.first()

            val targetGridId = when {
                selected.mode == SelectedLocationMode.MANUAL && !selected.gridId.isNullOrBlank() -> selected.gridId
                lastLocation != null -> {
                    String.format(
                        Locale.US,
                        "%.2f_%.2f",
                        ((lastLocation.first * 100).roundToInt() / 100.0),
                        ((lastLocation.second * 100).roundToInt() / 100.0),
                    )
                }
                homeLocation != null -> homeLocation.first
                else -> WeatherConstants.DEFAULT_GRID_ID
            } ?: WeatherConstants.DEFAULT_GRID_ID

            val fallbackLocationName = when {
                selected.mode == SelectedLocationMode.MANUAL -> selected.gridName
                homeLocation != null -> homeLocation.second
                else -> null
            }

            val weather = repository.getWeatherByGridId(targetGridId, forceServer = true)
            if (weather != null) {
                showDailyNotification(weather, fallbackLocationName, isMizo)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showDailyNotification(
        weather: WeatherDoc,
        fallbackLocationName: String?,
        isMizo: Boolean,
    ) {
        val current = weather.getCurrentWeather()
        val daily = weather.daily
        val locationName = fallbackLocationName ?: weather.gridId ?: if (isMizo) "Hmun khat" else "Selected area"

        val title = if (isMizo) {
            "Khawchin zing tin"
        } else {
            "Daily weather update"
        }
        val body = buildSummaryBody(weather, current?.temp?.roundToInt(), daily, locationName, isMizo)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_dashboard", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            7001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.GENERAL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(7001, notification)
    }

    private fun buildSummaryBody(
        weather: WeatherDoc,
        currentTemp: Int?,
        daily: DailyArrays?,
        locationName: String,
        isMizo: Boolean,
    ): String {
        val shortTermText = if (isMizo) {
            weather.shortTerm?.rainTimeline?.summaryMz
        } else {
            weather.shortTerm?.rainTimeline?.summaryEn
        }
        val rainProb = daily?.rainProb?.firstOrNull()
        val tempText = currentTemp?.let { "${it}C" } ?: "--"
        val rainText = when {
            !shortTermText.isNullOrBlank() -> shortTermText
            (rainProb ?: 0) >= 70 -> if (isMizo) "Ruah sur thei dinhmun a sang." else "High chance of rain today."
            (rainProb ?: 0) >= 40 -> if (isMizo) "Ruah sur thei dinhmun a awm." else "Some chance of rain today."
            else -> if (isMizo) "Ruah sur thei dinhmun a hniam." else "Low chance of rain today."
        }

        return if (isMizo) {
            "$locationName: $tempText, $rainText"
        } else {
            "$locationName: $tempText, $rainText"
        }
    }
}

