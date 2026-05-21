package com.mapuia.khawchinthlirna

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.mapuia.khawchinthlirna.data.preferences.PreferencesManager
import com.mapuia.khawchinthlirna.ui.theme.KhawchinThlirnaTheme
import com.mapuia.khawchinthlirna.util.AppOpenAdManager
import com.mapuia.khawchinthlirna.util.AppLog
import com.mapuia.khawchinthlirna.util.ForceUpdateManager
import com.mapuia.khawchinthlirna.worker.WorkScheduler

class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var forceUpdateManager: ForceUpdateManager
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            AppLog.d(TAG, "Notification permission granted")
            subscribeToWeatherAlerts()
            WorkScheduler.scheduleDailyWeatherSummary(this, forceReschedule = true)
        } else {
            AppLog.d(TAG, "Notification permission denied")
            WorkScheduler.cancelDailyWeatherSummary(this)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT,
            ),
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize preferences manager
        preferencesManager = PreferencesManager(this)
        forceUpdateManager = ForceUpdateManager(this)
        
        // Request notification permission on app start (Android 13+)
        askNotificationPermission()

        setContent {
            // Observe dark mode preference from DataStore
            val darkModePreference by preferencesManager.darkModeFlow.collectAsState(initial = null)
            
            // Determine actual dark mode: null = follow system
            val useDarkTheme = when (darkModePreference) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }
            
            KhawchinThlirnaTheme(darkTheme = useDarkTheme) {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        forceUpdateManager.checkOnStart()
        AppOpenAdManager.showIfEligible(this)
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
                    WorkScheduler.scheduleDailyWeatherSummary(this, forceReschedule = true)
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
            WorkScheduler.scheduleDailyWeatherSummary(this, forceReschedule = true)
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
