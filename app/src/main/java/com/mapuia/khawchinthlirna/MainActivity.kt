package com.mapuia.khawchinthlirna

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.mapuia.khawchinthlirna.ui.theme.KhawchinThlirnaTheme
import com.mapuia.khawchinthlirna.util.AppLog

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            AppLog.d(TAG, "Notification permission granted")
            subscribeToWeatherAlerts()
        } else {
            AppLog.d(TAG, "Notification permission denied")
            // User denied, notifications won't work but app still functions
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Request notification permission on app start (Android 13+)
        askNotificationPermission()

        setContent {
            KhawchinThlirnaTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }
    
    /**
     * Request POST_NOTIFICATIONS permission for Android 13+ (API 33+)
     * On earlier versions, permission is granted at install time
     */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    AppLog.d(TAG, "Notification permission already granted")
                    subscribeToWeatherAlerts()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // User has previously denied - still request, system will show dialog
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // First time asking - request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 and below - permission granted at install
            subscribeToWeatherAlerts()
        }
    }
    
    /**
     * Subscribe to FCM topics for weather alerts.
     * Topics used (Firebase free tier allows unlimited topic subscriptions):
     * - "weather_alerts" - general weather alerts
     * - "severe_weather" - severe/dangerous weather (cyclones, floods, etc.)
     * - "mizoram" - regional alerts for Mizoram
     */
    private fun subscribeToWeatherAlerts() {
        val messaging = FirebaseMessaging.getInstance()
        
        // Subscribe to severe weather alerts (high priority)
        messaging.subscribeToTopic("severe_weather")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    AppLog.d(TAG, "Subscribed to severe_weather topic")
                } else {
                    AppLog.w(TAG, "Failed to subscribe to severe_weather topic")
                }
            }
        
        // Subscribe to general weather alerts
        messaging.subscribeToTopic("weather_alerts")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    AppLog.d(TAG, "Subscribed to weather_alerts topic")
                }
            }
        
        // FCM token is managed by KhawchinFCMService.onNewToken()
        // No need to log here - avoid exposing tokens in logs
    }
}
