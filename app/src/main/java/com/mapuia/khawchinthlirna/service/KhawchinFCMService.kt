package com.mapuia.khawchinthlirna.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mapuia.khawchinthlirna.MainActivity
import com.mapuia.khawchinthlirna.R
import com.mapuia.khawchinthlirna.data.local.KhawchinDatabase
import com.mapuia.khawchinthlirna.data.local.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging service for handling push notifications.
 * 
 * IMPORTANT: This service handles DATA messages, which are delivered even when
 * the app is in the background or killed. This is critical for weather alerts.
 * 
 * Notification messages are handled by the system when app is in background,
 * but DATA messages always come to this service.
 * 
 * Backend should send DATA-only messages for weather alerts:
 * {
 *   "to": "/topics/severe_weather",
 *   "data": {
 *     "type": "severe_weather",
 *     "title": "⚠️ Cyclone Warning",
 *     "body": "Cyclone approaching Mizoram..."
 *   }
 * }
 */
class KhawchinFCMService : FirebaseMessagingService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val TAG = "KhawchinFCM"
        private const val WEATHER_ALERT_ID = 1001
        private const val SEVERE_WEATHER_ID = 1002
        private const val REPORT_VERIFIED_ID = 1003
        private const val UPVOTE_ID = 1004
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received")
        // Update token in Firestore
        scope.launch {
            updateFCMToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "Message received from: ${message.from}")
        Log.d(TAG, "Data payload: ${message.data}")

        val data = message.data
        val notificationType = data["type"] ?: "general"

        // Store notification in local database
        scope.launch {
            storeNotification(message)
        }

        // Show notification based on type
        // This works even when app is in background/killed because we use DATA messages
        when (notificationType) {
            "weather_alert" -> showWeatherAlert(message)
            "severe_weather" -> showSevereWeatherAlert(message)
            "cyclone_warning" -> showCycloneWarning(message)
            "flood_warning" -> showFloodWarning(message)
            "report_verified" -> showReportVerified(message)
            "badge_earned" -> showBadgeEarned(message)
            "upvote" -> showUpvoteNotification(message)
            else -> showGeneralNotification(message)
        }
    }

    private fun showWeatherAlert(message: RemoteMessage) {
        val title = message.data["title"] ?: getString(R.string.weather_alert)
        val body = message.data["body"] ?: message.notification?.body ?: ""
        val gridId = message.data["gridId"]

        showNotification(
            channelId = NotificationChannels.WEATHER_ALERTS,
            notificationId = WEATHER_ALERT_ID,
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_HIGH,
            extraData = mapOf("gridId" to gridId)
        )
    }

    private fun showSevereWeatherAlert(message: RemoteMessage) {
        val title = message.data["title"] ?: "⚠️ ${getString(R.string.severe_weather_alert)}"
        val body = message.data["body"] ?: message.notification?.body ?: ""
        val gridId = message.data["gridId"]

        showNotification(
            channelId = NotificationChannels.SEVERE_WEATHER,
            notificationId = SEVERE_WEATHER_ID,
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_MAX,
            extraData = mapOf("gridId" to gridId),
            playSound = true,
            vibrate = true
        )
    }
    
    private fun showCycloneWarning(message: RemoteMessage) {
        val cycloneName = message.data["cyclone_name"] ?: ""
        val title = message.data["title"] ?: "🌀 Cyclone Warning: $cycloneName"
        val body = message.data["body"] ?: ""
        val gridId = message.data["gridId"]

        showNotification(
            channelId = NotificationChannels.SEVERE_WEATHER,
            notificationId = SEVERE_WEATHER_ID,
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_MAX,
            extraData = mapOf("gridId" to gridId, "cyclone_name" to cycloneName),
            playSound = true,
            vibrate = true
        )
    }
    
    private fun showFloodWarning(message: RemoteMessage) {
        val title = message.data["title"] ?: "🌊 Flood Warning"
        val body = message.data["body"] ?: ""
        val gridId = message.data["gridId"]

        showNotification(
            channelId = NotificationChannels.SEVERE_WEATHER,
            notificationId = SEVERE_WEATHER_ID + 1,
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_MAX,
            extraData = mapOf("gridId" to gridId),
            playSound = true,
            vibrate = true
        )
    }

    private fun showReportVerified(message: RemoteMessage) {
        val title = message.data["title"] ?: getString(R.string.report_verified_title)
        val body = message.data["body"] ?: getString(R.string.report_verified_body)

        showNotification(
            channelId = NotificationChannels.REPORT_UPDATES,
            notificationId = REPORT_VERIFIED_ID,
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    private fun showBadgeEarned(message: RemoteMessage) {
        val title = message.data["title"] ?: "🏆 ${getString(R.string.badge_earned_title)}"
        val badgeName = message.data["badgeName"] ?: ""
        val body = message.data["body"] ?: getString(R.string.badge_earned_body, badgeName)

        showNotification(
            channelId = NotificationChannels.ACHIEVEMENTS,
            notificationId = System.currentTimeMillis().toInt(),
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    private fun showUpvoteNotification(message: RemoteMessage) {
        val title = message.data["title"] ?: getString(R.string.upvote_title)
        val body = message.data["body"] ?: getString(R.string.upvote_body)

        showNotification(
            channelId = NotificationChannels.REPORT_UPDATES,
            notificationId = UPVOTE_ID,
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_LOW
        )
    }

    private fun showGeneralNotification(message: RemoteMessage) {
        val notification = message.notification
        val title = notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = notification?.body ?: message.data["body"] ?: ""

        showNotification(
            channelId = NotificationChannels.GENERAL,
            notificationId = System.currentTimeMillis().toInt(),
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_DEFAULT
        )
    }

    private fun showNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        priority: Int,
        extraData: Map<String, String?> = emptyMap(),
        playSound: Boolean = false,
        vibrate: Boolean = false
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            extraData.forEach { (key, value) ->
                if (value != null) {
                    putExtra(key, value)
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        // For severe weather alerts - use default alarm sound and long vibration
        if (playSound) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(alarmSound)
        }
        
        if (vibrate) {
            // Long vibration pattern for urgent alerts
            builder.setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
        }
        
        val notification = builder.build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private suspend fun storeNotification(message: RemoteMessage) {
        try {
            val db = KhawchinDatabase.getInstance(applicationContext)
            val notification = NotificationEntity(
                title = message.notification?.title ?: message.data["title"] ?: "",
                body = message.notification?.body ?: message.data["body"] ?: "",
                type = message.data["type"] ?: "general",
                data = message.data.toString(),
                receivedAt = System.currentTimeMillis()
            )
            db.notificationDao().insertNotification(notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun updateFCMToken(token: String) {
        try {
            val prefs = getSharedPreferences("khawchin_prefs", Context.MODE_PRIVATE)
            val userId = prefs.getString("user_id", null) ?: return

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcm_token", token)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Notification channel definitions
 */
object NotificationChannels {
    const val WEATHER_ALERTS = "weather_alerts"
    const val SEVERE_WEATHER = "severe_weather"
    const val REPORT_UPDATES = "report_updates"
    const val ACHIEVEMENTS = "achievements"
    const val GENERAL = "general"

    /**
     * Create all notification channels
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Weather Alerts Channel
            val weatherAlertsChannel = NotificationChannel(
                WEATHER_ALERTS,
                context.getString(R.string.channel_weather_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_weather_alerts_desc)
            }

            // Severe Weather Channel
            val severeWeatherChannel = NotificationChannel(
                SEVERE_WEATHER,
                context.getString(R.string.channel_severe_weather),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_severe_weather_desc)
                enableVibration(true)
            }

            // Report Updates Channel
            val reportUpdatesChannel = NotificationChannel(
                REPORT_UPDATES,
                context.getString(R.string.channel_report_updates),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_report_updates_desc)
            }

            // Achievements Channel
            val achievementsChannel = NotificationChannel(
                ACHIEVEMENTS,
                context.getString(R.string.channel_achievements),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_achievements_desc)
            }

            // General Channel
            val generalChannel = NotificationChannel(
                GENERAL,
                context.getString(R.string.channel_general),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_general_desc)
            }

            notificationManager.createNotificationChannels(
                listOf(
                    weatherAlertsChannel,
                    severeWeatherChannel,
                    reportUpdatesChannel,
                    achievementsChannel,
                    generalChannel
                )
            )
        }
    }
}
