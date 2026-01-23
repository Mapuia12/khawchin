package com.mapuia.khawchinthlirna

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.activity.compose.BackHandler
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.sin
import com.mapuia.khawchinthlirna.data.model.WeatherDoc
import com.mapuia.khawchinthlirna.ui.getWeatherIcon
import com.mapuia.khawchinthlirna.ui.seasonLabelMizo
import com.mapuia.khawchinthlirna.ui.windDirLabel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.koin.androidx.compose.koinViewModel
import com.mapuia.khawchinthlirna.ui.theme.GlassSurface
import com.mapuia.khawchinthlirna.ui.theme.PremiumGlassTokens
import com.mapuia.khawchinthlirna.ui.theme.getWeatherGradient
import com.mapuia.khawchinthlirna.ui.theme.getDynamicBackground
import com.mapuia.khawchinthlirna.ui.theme.WeatherColorSchemes
import com.mapuia.khawchinthlirna.ui.components.AnimatedWeatherIcon
import com.mapuia.khawchinthlirna.data.WeatherConstants
import com.mapuia.khawchinthlirna.ui.components.ColorfulTemperatureText
import com.mapuia.khawchinthlirna.ui.components.HeroTemperatureDisplay
import com.mapuia.khawchinthlirna.ui.screens.info.InfoHubScreen
import com.mapuia.khawchinthlirna.ui.screens.info.AppGuideScreen
import com.mapuia.khawchinthlirna.ui.screens.info.HowCrowdsourcingWorksScreen
import com.mapuia.khawchinthlirna.ui.screens.info.RainIntensityGuideScreen
import com.mapuia.khawchinthlirna.ui.screens.info.WeatherDataExplainedScreen
import com.mapuia.khawchinthlirna.ui.screens.report.ReportWeatherScreen
import com.mapuia.khawchinthlirna.ui.screens.report.NearbyReportsScreen
import com.mapuia.khawchinthlirna.ui.screens.UserProfileScreen
import com.mapuia.khawchinthlirna.ui.screens.SettingsScreen
import com.mapuia.khawchinthlirna.data.auth.UserProfile
import com.mapuia.khawchinthlirna.data.CrowdsourceRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import com.mapuia.khawchinthlirna.ui.components.getTemperatureColor
import com.mapuia.khawchinthlirna.ui.components.getTemperatureGlow
import androidx.compose.ui.res.painterResource

// Premium Vibrant Gradients
private val NightGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF0F0C29), // Deep Dark Purple
        Color(0xFF302B63), // Cosmic Purple
        Color(0xFF24243E), // Dark Indigo
    ),
)

/**
 * Extract HH:MM time from ISO timestamp.
 * Handles both formats:
 * - "2026-01-20T14:00" (length 16) → "14:00"
 * - "2026-01-20T14:00:00" (length 19) → "14:00" 
 * - "14:00" (already formatted) → "14:00"
 */
private fun extractTimeHHMM(timestamp: String): String {
    return when {
        // Full ISO with seconds: "2026-01-20T14:00:00"
        timestamp.contains("T") && timestamp.length >= 19 -> {
            timestamp.substring(11, 16) // Extract "HH:MM" from position 11-16
        }
        // ISO without seconds: "2026-01-20T14:00"
        timestamp.contains("T") && timestamp.length >= 16 -> {
            timestamp.substring(11, 16)
        }
        // Already in HH:MM format
        timestamp.length == 5 && timestamp.contains(":") -> timestamp
        // Fallback
        else -> timestamp.takeLast(5)
    }
}

private val DayGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF667eea), // Indigo
        Color(0xFF764ba2), // Purple
    ),
)

// Dynamic weather-based gradients for HERO BOX ONLY
private fun getWeatherHeroGradient(weatherCode: Int, isDay: Boolean): Brush {
    return when {
        // Night time - dark blue/purple
        !isDay -> Brush.verticalGradient(
            listOf(
                Color(0xFF1a1a2e),
                Color(0xFF16213e),
                Color(0xFF0f3460),
            )
        )
        // Clear sky - bright cyan/blue
        weatherCode == 0 -> Brush.verticalGradient(
            listOf(
                Color(0xFF00B4DB),
                Color(0xFF0083B0),
            )
        )
        // Rain - dark stormy
        weatherCode in 51..67 || weatherCode in 80..82 -> Brush.verticalGradient(
            listOf(
                Color(0xFF0F2027),
                Color(0xFF203A43),
                Color(0xFF2C5364),
            )
        )
        // Thunderstorm - dramatic purple
        weatherCode in 95..99 -> Brush.verticalGradient(
            listOf(
                Color(0xFF141E30),
                Color(0xFF243B55),
                Color(0xFF6441A5),
            )
        )
        // Partly cloudy - blue/purple
        weatherCode in 1..3 -> Brush.verticalGradient(
            listOf(
                Color(0xFF4776E6),
                Color(0xFF8E54E9),
            )
        )
        // Foggy/Mist
        weatherCode in 45..48 -> Brush.verticalGradient(
            listOf(
                Color(0xFF606c88),
                Color(0xFF3f4c6b),
            )
        )
        // Snow
        weatherCode in 71..77 -> Brush.verticalGradient(
            listOf(
                Color(0xFF83a4d4),
                Color(0xFFb6fbff),
            )
        )
        // Default - nice gradient
        else -> DayGradient
    }
}

private val GlassCardShape = RoundedCornerShape(PremiumGlassTokens.cornerRadius)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isDay: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassSurface(modifier = modifier, isDay = isDay, tokens = PremiumGlassTokens, content = content)
}

@Composable
private fun PremiumPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "pressScale",
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

// --- MainScreen changes ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: WeatherViewModel = koinViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val view = LocalView.current

    // Decide day/night from weather; default to "night" visuals.
    // Use helper method to support both old and new data formats
    val isDay = uiState.weather?.getCurrentWeather()?.isDay == 1

    // --- Adaptive status bar icons (Gap B) ---
    // Our background is dark in both cases. Keep status bar transparent + light icons.
    LaunchedEffect(isDay) {
        runCatching {
            val window = (context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    val pullState = rememberPullToRefreshState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) vm.onLocationPermissionGranted() else vm.onLocationPermissionDenied()
        },
    )

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) vm.onLocationPermissionGranted() else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    // Auto-refresh every 30 minutes (background refresh, non-intrusive)
    // Uses lifecycle awareness to pause when app is in background
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppInForeground by remember { mutableStateOf(true) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppInForeground = event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START
            if (event == Lifecycle.Event.ON_RESUME) {
                // Reset session tracking for interstitial ads
                com.mapuia.khawchinthlirna.util.InterstitialAdManager.onSessionStart()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(Unit) {
        val autoRefreshIntervalMs = 30 * 60 * 1000L // 30 minutes
        while (true) {
            kotlinx.coroutines.delay(autoRefreshIntervalMs)
            // Only refresh if app is in foreground to save battery
            if (isAppInForeground) {
                // Silent background refresh
                vm.refresh(isUserInitiated = false)
                // Check if interstitial ad should be shown (respects internal cooldown)
                (context as? Activity)?.let { activity ->
                    com.mapuia.khawchinthlirna.util.InterstitialAdManager.checkAutoTrigger(activity)
                }
            }
        }
    }

    var showReport by remember { mutableStateOf(false) }
    var showInfoHub by remember { mutableStateOf(false) }
    var showFullReportScreen by remember { mutableStateOf(false) }
    var showNearbyReports by remember { mutableStateOf(false) }
    var reportSubmitting by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var menuNavigateTo by remember { mutableStateOf<String?>(null) }

    // One-shot toast feedback counter
    var reportToastKey by remember { mutableIntStateOf(0) }

    // Get weather code for hero section gradient
    val weatherCode = uiState.weather?.getCurrentWeather()?.weatherCode ?: 0
    val currentHour = remember { java.time.LocalTime.now().hour }

    // Fixed app background - premium blue-purple gradient (doesn't change with weather)
    val backgroundBrush = if (isDay) DayGradient else NightGradient
    
    // Weather-based gradient only for hero box
    val heroGradient = getWeatherHeroGradient(weatherCode, isDay)

    // Coroutine scope for BackHandler - properly managed lifecycle
    val backHandlerScope = rememberCoroutineScope()

    // Back press handling - close overlays or exit with confirmation
    var backPressedOnce by remember { mutableStateOf(false) }
    
    BackHandler(enabled = true) {
        when {
            showInfoHub -> showInfoHub = false
            showFullReportScreen -> showFullReportScreen = false
            showNearbyReports -> showNearbyReports = false
            showReport -> showReport = false
            else -> {
                // Double-tap back to exit
                if (backPressedOnce) {
                    (context as? Activity)?.finish()
                } else {
                    backPressedOnce = true
                    Toast.makeText(context, "App khar nan 'Back' hmet nawn leh rawh", Toast.LENGTH_SHORT).show()
                    // Reset after 2 seconds using properly scoped coroutine
                    backHandlerScope.launch {
                        kotlinx.coroutines.delay(2000)
                        backPressedOnce = false
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0), // Let content handle insets manually
            topBar = {
                GlassHeaderBar(
                    onReport = { showFullReportScreen = true },
                    onInfoClick = { showInfoHub = true },
                    onMenuItemClick = { item ->
                        when (item) {
                            "app_guide", "crowdsourcing", "rain_guide", "weather_data" -> {
                                menuNavigateTo = item
                                showInfoHub = true
                            }
                            "theme" -> showThemeDialog = true
                        }
                    },
                )
            },
            bottomBar = {
                BannerAd(modifier = Modifier.fillMaxWidth())
            },
        ) { paddingValues ->
            PullToRefreshBox(
                state = pullState,
                isRefreshing = uiState.isRefreshing,
                onRefresh = { 
                    vm.refresh(isUserInitiated = true)
                    // Auto-trigger interstitial ad check on refresh
                    (context as? Activity)?.let { activity ->
                        com.mapuia.khawchinthlirna.util.InterstitialAdManager.checkAutoTrigger(activity)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    StatusBanner(
                        isDay = isDay,
                        permissionDenied = uiState.locationPermissionState == LocationPermissionState.DENIED,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                        onOpenSettings = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                    )

                    uiState.weather?.let { weather ->
                        // Weather Systems Alert (Cyclones with contextual messages)
                        WeatherSystemsAlertCard(
                            weather = weather,
                            userLat = uiState.userLat,
                            userLon = uiState.userLon,
                        )
                        
                        // Marine alert (primary backend alert) directly under header.
                        // Skip GREEN when cyclones are already showing (avoid duplication)
                        val hasCycloneCards = weather.weatherSystems?.bayOfBengal?.cycloneActive == true &&
                            weather.weatherSystems?.bayOfBengal?.cyclones?.isNotEmpty() == true
                        val skipGreenStrip = hasCycloneCards && weather.marineAlert.trim().uppercase() == "GREEN"
                        
                        if (!skipGreenStrip) {
                            MarineAlertStrip(marineAlert = weather.marineAlert, isDay = isDay)
                        }

                        UpstreamRainAlertCard(weather)

                        // 1. Hero Section
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(280)),
                            exit = fadeOut(animationSpec = tween(180)),
                        ) {
                            HeroSection(
                                weather = weather,
                                userLat = uiState.userLat,
                                userLon = uiState.userLon,
                                userPlaceName = uiState.userPlaceName,
                                isDay = isDay,
                                heroGradient = heroGradient,
                            )
                        }

                        // 2. Hourly Forecast
                        HourlyForecast(weather, isDay = isDay)

                        // 3. Native Ad
                        NativeAdCard(modifier = Modifier.fillMaxWidth(), isDay = isDay)

                        // 4. Nearby Reports
                        NearbyReportsCard(
                            onViewNearbyReports = { showNearbyReports = true },
                            isDay = isDay,
                        )

                        // 5. Current Conditions Grid (Wind, Rainfall, Pressure, Humidity, Visibility, Dewpoint)
                        CurrentConditionsGrid(weather, isDay = isDay)
                        
                        // 5.5. Air Quality Index (if available) - TODO: Add airQuality field to WeatherDoc when backend supports it
                        // AirQualityCard(weather, isDay = isDay)

                        // 6. Daily Forecast (7 or 10 days)
                        DailyForecastCard(weather, isDay = isDay)

                        // 7. Sunrise & Sunset
                        SunriseSunsetCard(weather, isDay = isDay)

                        // 8. Native Ad (second)
                        NativeAdCard(modifier = Modifier.fillMaxWidth(), isDay = isDay)

                        // 9. Seasonal Forecast
                        SeasonalForecastSection(weather = weather, isDay = isDay)
                        
                        // 9.5. Native Ad (third)
                        NativeAdCard(modifier = Modifier.fillMaxWidth(), isDay = isDay)
                        
                        // 10. Data Source & Accuracy Info
                        DataSourceInfo(weather = weather, isDay = isDay)
                    }

                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        if (showReport) {
            ReportDialog(
                locationAvailable = uiState.userLat != null && uiState.userLon != null,
                isSubmitting = reportSubmitting,
                onDismiss = {
                    if (!reportSubmitting) showReport = false
                },
                onSubmit = { option ->
                    if (reportSubmitting) return@ReportDialog

                    // Validate report option
                    if (option.isBlank()) {
                        Toast.makeText(context, "Please select a report option", Toast.LENGTH_SHORT).show()
                        return@ReportDialog
                    }

                    // Block submit if we don't have GPS coordinates (backend clustering needs lat/lon).
                    if (uiState.userLat == null || uiState.userLon == null) {
                        Toast.makeText(context, "Turn on GPS to submit a report", Toast.LENGTH_SHORT).show()
                        return@ReportDialog
                    }

                    reportSubmitting = true
                    vm.submitCrowdReport(
                        optionMizo = option,
                        onDone = { ok, msg ->
                            reportSubmitting = false
                            showReport = false
                            // Feedback
                            val text = if (ok) "Ka lawm e! Report thawn a hlawhtling e." else (msg ?: "Report thawn a hlawhchham")
                            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                            reportToastKey++
                            
                            // Show interstitial ad after successful report (time-based, 3 min interval)
                            // Also track user action for action-based triggering
                            if (ok) {
                                (context as? Activity)?.let { activity ->
                                    com.mapuia.khawchinthlirna.util.InterstitialAdManager.trackAction(activity)
                                }
                            }
                        },
                    )
                },
            )
        }

        // Full-featured Report Weather Screen
        if (showFullReportScreen) {
            val coroutineScope = rememberCoroutineScope()
            val crowdsourceRepo = remember { CrowdsourceRepository(FirebaseFirestore.getInstance()) }
            val authManager: com.mapuia.khawchinthlirna.data.auth.AuthManager = org.koin.compose.koinInject()
            val gamificationManager: com.mapuia.khawchinthlirna.data.auth.GamificationManager = org.koin.compose.koinInject()
            val currentUserId = authManager.userId.ifBlank { "anonymous" }
            var currentInfoScreen by remember { mutableStateOf<String?>(null) }
            
            // Handle back for nested rain_guide screen
            BackHandler(enabled = currentInfoScreen != null) {
                currentInfoScreen = null
            }
            
            when (currentInfoScreen) {
                "rain_guide" -> {
                    RainIntensityGuideScreen(
                        onBack = { currentInfoScreen = null }
                    )
                }
                else -> {
                    ReportWeatherScreen(
                        userLat = uiState.userLat,
                        userLon = uiState.userLon,
                        userId = currentUserId,
                        onBack = { showFullReportScreen = false },
                        onSubmit = { rainIntensity, skyCondition, windStrength, notes, locationName ->
                            try {
                                crowdsourceRepo.submitReport(
                                    userId = currentUserId,
                                    lat = uiState.userLat ?: 0.0,
                                    lon = uiState.userLon ?: 0.0,
                                    rainIntensity = rainIntensity,
                                    skyCondition = skyCondition,
                                    windStrength = windStrength,
                                    notes = notes,
                                    locationName = locationName,
                                    gridId = uiState.gridId,
                                )
                                // Award points and badges
                                val awardResult = gamificationManager.onReportSubmitted(
                                    userId = currentUserId,
                                    rainIntensity = rainIntensity,
                                    lat = uiState.userLat ?: 0.0,
                                    lon = uiState.userLon ?: 0.0
                                )
                                // Show badge notification if earned
                                if (awardResult.newBadges.isNotEmpty()) {
                                    val badgeName = com.mapuia.khawchinthlirna.data.auth.Badges.getNameMz(awardResult.newBadges.first())
                                    Toast.makeText(context, "🎖️ Badge thar: $badgeName! +${awardResult.pointsEarned} points", Toast.LENGTH_LONG).show()
                                } else if (awardResult.pointsEarned > 0) {
                                    Toast.makeText(context, "⭐ +${awardResult.pointsEarned} points earned!", Toast.LENGTH_SHORT).show()
                                }
                                
                                // Show interstitial ad after report submission (track action for auto-trigger)
                                (context as? Activity)?.let { activity ->
                                    com.mapuia.khawchinthlirna.util.InterstitialAdManager.trackAction(activity)
                                }
                                
                                Result.success(Unit)
                            } catch (e: Exception) {
                                Result.failure(e)
                            }
                        },
                        onNavigateToRainGuide = { currentInfoScreen = "rain_guide" }
                    )
                }
            }
        }

        // Info Hub Screen
        if (showInfoHub) {
            // Start at the menu item if navigating from hamburger menu
            var currentInfoScreen by remember { mutableStateOf(menuNavigateTo ?: "hub") }
            
            // Reset menuNavigateTo after using it
            LaunchedEffect(Unit) {
                if (menuNavigateTo != null) {
                    menuNavigateTo = null
                }
            }
            
            // Handle back for nested screens - must be declared before the when block
            BackHandler(enabled = currentInfoScreen != "hub") {
                currentInfoScreen = "hub"
            }
            
            when (currentInfoScreen) {
                "app_guide" -> {
                    AppGuideScreen(
                        onBack = { currentInfoScreen = "hub" }
                    )
                }
                "crowdsourcing" -> {
                    HowCrowdsourcingWorksScreen(
                        onBack = { currentInfoScreen = "hub" }
                    )
                }
                "rain_guide" -> {
                    RainIntensityGuideScreen(
                        onBack = { currentInfoScreen = "hub" }
                    )
                }
                "weather_data" -> {
                    WeatherDataExplainedScreen(
                        onBack = { currentInfoScreen = "hub" }
                    )
                }
                else -> {
                    InfoHubScreen(
                        onBack = { showInfoHub = false },
                        onNavigateToAppGuide = { currentInfoScreen = "app_guide" },
                        onNavigateToCrowdsourcing = { currentInfoScreen = "crowdsourcing" },
                        onNavigateToRainGuide = { currentInfoScreen = "rain_guide" },
                        onNavigateToWeatherData = { currentInfoScreen = "weather_data" },
                    )
                }
            }
        }

        // Nearby Reports Screen
        if (showNearbyReports) {
            val crowdsourceRepo = remember { CrowdsourceRepository(FirebaseFirestore.getInstance()) }
            NearbyReportsScreen(
                userLat = uiState.userLat,
                userLon = uiState.userLon,
                onBack = { showNearbyReports = false },
                onFetchReports = { lat, lon, radiusKm, minutes ->
                    crowdsourceRepo.getNearbyReports(lat, lon, radiusKm, minutes)
                }
            )
        }
        
        // Theme Selection Dialog
        if (showThemeDialog) {
            ThemeSelectionDialog(
                onDismiss = { showThemeDialog = false },
                onThemeSelected = { theme ->
                    // TODO: Implement theme persistence with DataStore
                    Toast.makeText(context, "Theme: $theme (a la ṭawng)", Toast.LENGTH_SHORT).show()
                    showThemeDialog = false
                }
            )
        }
    }
}

@Composable
private fun StatusBanner(
    isDay: Boolean,
    permissionDenied: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    // Loading / error / permission shown in one compact premium banner.
    val show = permissionDenied || isLoading || !errorMessage.isNullOrBlank()
    if (!show) return

    GlassCard(modifier = Modifier.fillMaxWidth(), isDay = isDay) {
        when {
            permissionDenied -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOff, contentDescription = "Location permission off", tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Location a in-off", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "GPS i on chuan i awmna hnaih ber a thlang ang a, a dik zawk ang.",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onRequestPermission, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("On rawh le!")
                    }
                    OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Settings")
                    }
                }
            }

            !errorMessage.isNullOrBlank() -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = "Error", tint = Color.White)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Dik lo a awm tlat", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(errorMessage, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
                    }
                }
            }

            isLoading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Khawchin a load mek…", color = Color.White.copy(alpha = 0.90f), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun HeroSection(
    weather: WeatherDoc,
    userLat: Double?,
    userLon: Double?,
    userPlaceName: String?,
    isDay: Boolean,
    heroGradient: Brush = DayGradient,
) {
    // Use helper to support both old and new data formats
    val current = weather.getCurrentWeather()
    val temp = current?.temp ?: 0.0
    val weatherCode = current?.weatherCode ?: 0

    // Custom hero card with weather gradient
    val shape = RoundedCornerShape(24.dp)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.2f),
            )
            .clip(shape)
            .background(heroGradient)
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f),
                    )
                ),
                shape = shape
            )
            .padding(20.dp)
    ) {
        Column {
        // NOTE: Marine alert is intentionally NOT shown in Hero (shown under header only).

        val locationLabel = userPlaceName
            ?: if (userLat != null && userLon != null) "I awmna hnaivai" else null

        if (locationLabel != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF06D6A0))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = locationLabel,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                // Vibrant colored temperature with shadow for clarity
                Text(
                    text = "${temp.toInt()}°",
                    color = getTemperatureColor(temp),
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 88.sp,
                    letterSpacing = (-3).sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.3f),
                            offset = Offset(2f, 4f),
                            blurRadius = 12f
                        )
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Feels Like",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${current?.feelsLike?.toInt() ?: 0}°",
                        color = getTemperatureColor(current?.feelsLike ?: 0.0),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(12.dp))

                val season = seasonLabelMizo(weather.marineEvidence?.season)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF8338EC).copy(alpha = 0.3f),
                                    Color(0xFF3A86FF).copy(alpha = 0.3f),
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF8338EC).copy(alpha = 0.5f),
                                    Color(0xFF3A86FF).copy(alpha = 0.5f),
                                )
                            ),
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(text = season, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            // Animated weather icon
            AnimatedWeatherIcon(
                weatherCode = weatherCode,
                isDay = isDay,
                modifier = Modifier.size(120.dp)
            )
        }
        } // Column
    } // Box
}

@Composable
fun WeatherSvgIcon(
    code: Int,
    modifier: Modifier = Modifier,
    isDay: Boolean = true,
) {
    val context = LocalContext.current
    val uri = getWeatherIcon(code = code, isDay = isDay)

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(uri)
            .decoderFactory(SvgDecoder.Factory())
            .crossfade(true)
            .build(),
        contentDescription = "Weather icon",
        modifier = modifier,
    )
}

@Composable
private fun HourlyForecast(weather: WeatherDoc, isDay: Boolean) {
    // Use helper to support both old and new data formats
    val hourly = weather.getHourlyForecast() ?: return
    if (hourly.time.isEmpty() || hourly.temp.isEmpty()) return

    // Get sunrise/sunset for isDay calculation per hour
    val daily = weather.daily
    val sunriseStr = daily?.sunrise?.firstOrNull()
    val sunsetStr = daily?.sunset?.firstOrNull()
    val sunrise = sunriseStr?.let { 
        runCatching { LocalTime.parse(extractTimeHHMM(it)) }.getOrNull() 
    }
    val sunset = sunsetStr?.let { 
        runCatching { LocalTime.parse(extractTimeHHMM(it)) }.getOrNull() 
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), isDay = isDay) {
        Text(
            text = "Darkar Tin Thlirlawkna",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )

        // Compute the safe count across arrays we actually read.
        val count = weather.getSafeHourlyCount()
        if (count <= 0) return@GlassCard

        // Find the current hour index (same as hero uses) to sync temperatures
        val currentHourIdx = weather.findCurrentHourIndex(hourly.time)
        
        // Show up to 24 hours starting from current hour
        val endIdx = minOf(currentHourIdx + 24, count)
        val itemsToShow = endIdx - currentHourIdx
        
        if (itemsToShow <= 0) return@GlassCard
        
        // Get precipitation probability if available
        val precipProb = hourly.precipitationProbability ?: emptyList()

        val rows = (currentHourIdx until endIdx).mapNotNull { idx ->
            // Safely get temperature - skip if null
            val tempValue = hourly.temp.getOrNull(idx) ?: return@mapNotNull null
            
            HourlyData(
                time = hourly.time[idx],
                temp = tempValue,
                weatherCode = hourly.weatherCode.getOrNull(idx) ?: 0,
                rainMm = hourly.rainMm.getOrNull(idx) ?: 0.0,
                rainProb = precipProb.getOrNull(idx) ?: 0,
                wind = hourly.wind.getOrNull(idx) ?: 0.0,
            )
        }
        
        // If no valid rows, don't show
        if (rows.isEmpty()) return@GlassCard
        
        // First item data for sticky "Now" pill
        val nowItem = rows.firstOrNull()
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Sticky "Now" pill - always visible
            nowItem?.let { item ->
                val hourTime = runCatching { 
                    LocalTime.parse(extractTimeHHMM(item.time)) 
                }.getOrNull()
                val isHourDay = if (hourTime != null && sunrise != null && sunset != null) {
                    hourTime.isAfter(sunrise) && hourTime.isBefore(sunset)
                } else {
                    isDay
                }
                
                val sub = buildString {
                    if (item.rainProb > 0) append("${item.rainProb}%")
                    if (item.rainMm > 0.0) {
                        if (isNotEmpty()) append(" ")
                        append("${"%.1f".format(item.rainMm)}mm")
                    }
                }.ifBlank { null }
                
                HourlyPill(
                    label = "Tunah",
                    temp = item.temp,
                    code = item.weatherCode,
                    highlighted = true,
                    subLabel = sub,
                    isDay = isHourDay,
                )
            }
            
            // Scrollable remaining hours (skip first since it's shown as "Now")
            val remainingRows = if (rows.size > 1) rows.drop(1) else emptyList()
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(remainingRows) { idx, item ->
                    val rawTime = item.time
                    // Parse time using helper that handles both "2026-01-16T14:00" and "2026-01-16T14:00:00"
                    val label = if (rawTime.contains("T") || (rawTime.length == 5 && rawTime.contains(":"))) {
                        extractTimeHHMM(rawTime)
                    } else {
                        // Fallback: calculate hour based on index (starting from next hour)
                        val hour = (java.time.LocalTime.now().hour + 1 + idx) % 24
                        "%02d:00".format(hour)
                    }

                    // Calculate isDay for this specific hour
                    val hourTime = runCatching { LocalTime.parse(label) }.getOrNull()
                    val isHourDay = if (hourTime != null && sunrise != null && sunset != null) {
                        hourTime.isAfter(sunrise) && hourTime.isBefore(sunset)
                    } else {
                        isDay // fallback to current isDay
                    }

                    val rainMm = item.rainMm
                    val rainProb = item.rainProb
                    val wind = item.wind

                    // Sub-label shows rain probability %, rain mm, and wind km/h
                    val sub = buildString {
                        if (rainProb > 0) append("$rainProb%")
                        if (rainMm > 0.0) {
                            if (isNotEmpty()) append(" ")
                            append("${"%.1f".format(rainMm)}mm")
                        }
                        if (wind > 0.0) {
                            if (isNotEmpty()) append("  ")
                            append("${wind.toInt()}km/h")
                        }
                    }.ifBlank { null }

                    HourlyPill(
                        label = label,
                        temp = item.temp,
                        code = item.weatherCode,
                        highlighted = false,
                        subLabel = sub,
                        isDay = isHourDay,
                    )
                }
            }
        }
    }
}

/** Data class for hourly weather data */
private data class HourlyData(
    val time: String,
    val temp: Double,
    val weatherCode: Int,
    val rainMm: Double,
    val rainProb: Int,
    val wind: Double,
)

private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

@Composable
private fun HourlyPill(
    label: String,
    temp: Double,
    code: Int,
    highlighted: Boolean,
    subLabel: String? = null,
    isDay: Boolean = true,
) {
    val shape = RoundedCornerShape(20.dp)
    val tempColor = getTemperatureColor(temp)

    // SOLID dark backgrounds for maximum contrast
    val bg = if (highlighted) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF6B21A8),  // Solid purple
                Color(0xFF4C1D95),  // Dark purple
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0F172A),  // Very dark navy - solid
                Color(0xFF1E293B),  // Dark slate - solid
            ),
        )
    }

    val borderBrush = if (highlighted) {
        Brush.verticalGradient(
            listOf(
                Color(0xFFA855F7),  // Bright purple
                Color(0xFF7C3AED),  // Vivid violet
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFF64748B),  // Visible slate border
                Color(0xFF475569),  // Darker slate
            )
        )
    }

    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(shape)
            .background(bg)
            .border(1.5.dp, borderBrush, shape)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Time label - pure white for visibility
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )

        WeatherSvgIcon(code = code, isDay = isDay, modifier = Modifier.size(32.dp))

        subLabel?.let {
            Text(
                text = it,
                color = Color(0xFF64FFDA), // Bright teal - visible against dark bg
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Temperature with color coding - ensure visibility
        Text(
            text = "${temp.toInt()}°",
            color = tempColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun CurrentConditionsGrid(weather: WeatherDoc, isDay: Boolean = true) {
    // Use helper to support both old and new data formats
    val current = weather.getCurrentWeather()
    val hourly = weather.getHourlyForecast()

    // Wind direction from current, or fallback to hourly first value
    val windDeg = current?.windDir ?: current?.windDirection ?: hourly?.windDir?.firstOrNull()
    val windDir = windDirLabel(windDeg)
    val windSpeed = (current?.wind ?: 0.0).toInt()

    // Pressure from current, marineEvidence, or hourly
    val pressure = current?.pressure ?: weather.marineEvidence?.pressure
    
    // Visibility (convert m to km)
    val visibilityM = hourly?.visibilityM?.firstOrNull() ?: hourly?.visibility?.firstOrNull()?.toDouble()
    val visibilityKm = visibilityM?.let { it / 1000.0 }
    
    // Dewpoint
    val dewpoint = hourly?.dewpointC?.firstOrNull() ?: hourly?.dewpoint?.firstOrNull() ?: current?.dewpoint
    
    // UV Index and Cloud Cover
    val uvIndex = hourly?.uvIndex?.firstOrNull() ?: current?.uvIndex
    val cloudCover = hourly?.cloudCoverPercent?.firstOrNull()?.toInt() ?: hourly?.cloudCover?.firstOrNull() ?: current?.cloudCover

    val shape = RoundedCornerShape(24.dp)

    // Premium container with gradient border
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = shape,
                spotColor = Color(0xFF8338EC).copy(alpha = 0.25f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.06f),
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF8338EC).copy(alpha = 0.5f),
                        Color(0xFF3A86FF).copy(alpha = 0.3f),
                        Color(0xFF06D6A0).copy(alpha = 0.5f),
                    )
                ),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Section title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = "📊",
                    fontSize = 16.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Current Conditions",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }

            // Row 1: Wind Card (Full Width - iOS Weather Style)
            WindDetailCard(
                windSpeed = windSpeed,
                windGust = (current?.windGust ?: hourly?.windGust?.firstOrNull())?.toInt(),
                windDirection = windDeg,
                windDirLabel = windDir,
            )

            // Row 2: Rainfall + Humidity
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Rainfall Card
                PremiumMetricCard(
                    title = "RAINFALL",
                    value = "${"%.1f".format(current?.rainMm ?: 0.0)}",
                    unit = "mm/hr",
                    iconRes = R.drawable.ic_rain_mm,
                    gradientColors = listOf(Color(0xFF3A86FF), Color(0xFF0066FF)),
                    modifier = Modifier.weight(1f),
                )
                
                // Humidity Card
                PremiumMetricCard(
                    title = "HUMIDITY",
                    value = "${current?.humidity ?: 0}",
                    unit = "%",
                    iconRes = R.drawable.ic_humidity_drop,
                    gradientColors = listOf(Color(0xFF06D6A0), Color(0xFF00B894)),
                    modifier = Modifier.weight(1f),
                )
            }

            // Row 3: Pressure + Visibility
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Pressure Card
                PremiumMetricCard(
                    title = "PRESSURE",
                    value = pressure?.let { "${"%.0f".format(it)}" } ?: "--",
                    unit = "hPa",
                    iconRes = R.drawable.ic_pressure,
                    gradientColors = listOf(Color(0xFFFF006E), Color(0xFFD6336C)),
                    modifier = Modifier.weight(1f),
                )
                
                // Visibility Card
                PremiumMetricCard(
                    title = "VISIBILITY",
                    value = visibilityKm?.let { "${"%.1f".format(it)}" } ?: "--",
                    unit = "km",
                    iconRes = R.drawable.ic_visibility,
                    gradientColors = listOf(Color(0xFF9B59B6), Color(0xFF8E44AD)),
                    modifier = Modifier.weight(1f),
                )
            }

            // Row 4: Dewpoint + Cloud Cover
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Dewpoint Card
                if (dewpoint != null) {
                    PremiumMetricCard(
                        title = "DEWPOINT",
                        value = "${dewpoint.toInt()}",
                        unit = "°C",
                        iconRes = R.drawable.ic_dewpoint,
                        gradientColors = listOf(Color(0xFF1ABC9C), Color(0xFF16A085)),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Cloud Cover Card
                if (cloudCover != null) {
                    PremiumMetricCard(
                        title = "CLOUD COVER",
                        value = "$cloudCover",
                        unit = "%",
                        iconRes = R.drawable.ic_cloud,
                        gradientColors = listOf(Color(0xFF78909C), Color(0xFF546E7A)),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            
            // Row 5: UV Index (when available - important for daytime)
            if (uvIndex != null && uvIndex > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumMetricCard(
                        title = "UV INDEX",
                        value = "${"%.1f".format(uvIndex)}",
                        unit = getUvLevel(uvIndex),
                        iconRes = R.drawable.ic_sun,
                        gradientColors = getUvGradientColors(uvIndex),
                        modifier = Modifier.weight(1f),
                    )
                    // Spacer for balanced layout
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/*
 * TODO: Air Quality Index Card - Uncomment when airQuality field is added to WeatherDoc
 * Shows AQI, PM2.5, PM10, NO2, O3, SO2
 *
@Composable
private fun AirQualityCard(weather: WeatherDoc, isDay: Boolean = true) {
    val aqi = weather.airQuality ?: return // Don't show if no AQI data
    
    val aqiColor = Color(aqi.getCategoryColor())
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = shape,
                spotColor = aqiColor.copy(alpha = 0.3f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.06f),
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        aqiColor.copy(alpha = 0.6f),
                        aqiColor.copy(alpha = 0.3f),
                    )
                ),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header with AQI Value
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_air_quality),
                        contentDescription = null,
                        tint = aqiColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Air Quality",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = aqi.getCategoryMizo(),
                            color = aqiColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                        )
                    }
                }
                
                // Large AQI Value
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(aqiColor.copy(alpha = 0.2f))
                        .border(1.dp, aqiColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${aqi.aqi}",
                            color = aqiColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 28.sp,
                        )
                        Text(
                            text = "AQI",
                            color = aqiColor.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            
            // Health Advice
            Text(
                text = aqi.getHealthAdvice(),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
            
            // 2x3 Grid of pollutants
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: PM2.5 and PM10
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AqiPollutantCard(
                        name = "PM2.5",
                        value = aqi.pm25,
                        unit = "µg/m³",
                        isPrimary = aqi.dominantPollutant == "pm2_5",
                        modifier = Modifier.weight(1f),
                    )
                    AqiPollutantCard(
                        name = "PM10",
                        value = aqi.pm10,
                        unit = "µg/m³",
                        isPrimary = aqi.dominantPollutant == "pm10",
                        modifier = Modifier.weight(1f),
                    )
                }
                
                // Row 2: O3 and NO2
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AqiPollutantCard(
                        name = "O₃",
                        value = aqi.o3,
                        unit = "µg/m³",
                        isPrimary = aqi.dominantPollutant == "o3",
                        modifier = Modifier.weight(1f),
                    )
                    AqiPollutantCard(
                        name = "NO₂",
                        value = aqi.no2,
                        unit = "µg/m³",
                        isPrimary = aqi.dominantPollutant == "no2",
                        modifier = Modifier.weight(1f),
                    )
                }
                
                // Row 3: SO2 and CO
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AqiPollutantCard(
                        name = "SO₂",
                        value = aqi.so2,
                        unit = "µg/m³",
                        isPrimary = aqi.dominantPollutant == "so2",
                        modifier = Modifier.weight(1f),
                    )
                    AqiPollutantCard(
                        name = "CO",
                        value = aqi.co,
                        unit = "µg/m³",
                        isPrimary = aqi.dominantPollutant == "co",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// Individual pollutant card in AQI grid
@Composable
private fun AqiPollutantCard(
    name: String,
    value: Double,
    unit: String,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (isPrimary) Color(0xFFFF9800) else Color.White.copy(alpha = 0.15f)
    
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, borderColor, shape)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    color = if (isPrimary) Color(0xFFFF9800) else Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium,
                )
                if (isPrimary) {
                    Text(
                        text = "●",
                        color = Color(0xFFFF9800),
                        fontSize = 8.sp,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "%.1f".format(value),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = unit,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}
*/

/** iOS Weather Style Wind Card - Full Width with Compass */
@Composable
private fun WindDetailCard(
    windSpeed: Int,
    windGust: Int?,
    windDirection: Int?,
    windDirLabel: String?,
) {
    val shape = RoundedCornerShape(16.dp)
    val accentColor = Color(0xFF00D4FF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A1A2E).copy(alpha = 0.8f),
                        Color(0xFF16213E).copy(alpha = 0.7f),
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.6f),
                        accentColor.copy(alpha = 0.3f),
                    )
                ),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left Side - Wind Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Title Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF00D4FF), Color(0xFF0099CC)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_wind),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "WIND",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                    )
                }

                // Wind Speed - Main Value
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$windSpeed",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 42.sp,
                        style = TextStyle(
                            shadow = Shadow(
                                color = accentColor.copy(alpha = 0.5f),
                                offset = Offset(0f, 2f),
                                blurRadius = 8f
                            )
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = "km/h",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // Gust Row
                if (windGust != null && windGust > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF6B6B).copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "💨",
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Wind Gust",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$windGust km/h",
                            color = Color(0xFFFF6B6B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Right Side - Compass with Direction
            if (windDirection != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(start = 16.dp),
                ) {
                    // Compass Circle
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(2.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Compass cardinal directions
                        Text(
                            text = "N",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 4.dp),
                        )
                        Text(
                            text = "S",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp),
                        )
                        Text(
                            text = "E",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 6.dp),
                        )
                        Text(
                            text = "W",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 6.dp),
                        )
                        
                        // Direction Arrow
                        Text(
                            text = "↑",
                            color = accentColor,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = windDirection.toFloat()
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Direction Label
                    Text(
                        text = windDirLabel ?: "N",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${windDirection}°",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

/** Premium metric card with wind direction support */
@Composable
private fun PremiumMetricCard(
    title: String,
    value: String,
    unit: String,
    iconRes: Int,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    windDirection: Int? = null,
    windDirLabel: String? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val accentColor = gradientColors.first()

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A1A2E).copy(alpha = 0.8f),
                        Color(0xFF16213E).copy(alpha = 0.7f),
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.6f),
                        accentColor.copy(alpha = 0.3f),
                    )
                ),
                shape = shape
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Icon row with title
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(gradientColors)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    color = Color.White,  // Pure white for visibility
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                )
            }

            // Value with unit - WHITE for maximum visibility
            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = value,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = accentColor.copy(alpha = 0.5f),
                            offset = Offset(0f, 2f),
                            blurRadius = 8f
                        )
                    )
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = unit,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }

            // Wind direction display (only for wind card)
            if (windDirection != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    // Rotating arrow based on wind direction
                    Text(
                        text = "↑",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = windDirection.toFloat()
                        }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = windDirLabel ?: "N",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyForecastCard(weather: WeatherDoc, isDay: Boolean) {
    val daily = weather.daily ?: return

    val count = listOf(
        daily.time.size,
        daily.tempMax.size,
        daily.tempMin.size,
    ).minOrNull() ?: 0
    if (count == 0) return

    // Show 7 or 10 days based on what backend provides
    val daysToShow = minOf(count, if (count >= 10) 10 else 7)
    
    // Get confidence data from daily or meta (supports both simple numbers and complex objects)
    data class ConfidenceInfo(val overall: Double, val label: String, val precip: Int, val temp: Int)
    
    val confidenceData: List<ConfidenceInfo> = run {
        val rawList = daily.confidence ?: weather.meta?.confidenceByDay ?: emptyList()
        rawList.mapNotNull { item ->
            when (item) {
                // Simple number format
                is Number -> ConfidenceInfo(item.toDouble(), getConfidenceLabel(item.toDouble()), 0, 0)
                // Complex object format from backend v86+
                is Map<*, *> -> {
                    val overall = (item["overall"] as? Number)?.toDouble()?.div(100) ?: 0.5
                    val label = (item["label"] as? String) ?: getConfidenceLabel(overall)
                    val precip = (item["precip"] as? Number)?.toInt() ?: 0
                    val temp = (item["temp"] as? Number)?.toInt() ?: 0
                    ConfidenceInfo(overall, label, precip, temp)
                }
                else -> null
            }
        }
    }
    
    // Extract just the overall values for backward compatibility
    val confidenceLevels: List<Double> = confidenceData.map { it.overall }

    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                spotColor = Color(0xFF3A86FF).copy(alpha = 0.2f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A1A2E).copy(alpha = 0.85f),
                        Color(0xFF16213E).copy(alpha = 0.75f),
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF3A86FF).copy(alpha = 0.5f),
                        Color(0xFF3A86FF).copy(alpha = 0.2f),
                    )
                ),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = "📅",
                    fontSize = 18.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (daysToShow >= 10) "Ni 10 Chhung Forecast" else "Kar Khat Chhung Khawchin",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.weight(1f))
                // Confidence legend
                if (confidenceLevels.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🎯",
                            fontSize = 10.sp,
                        )
                        Text(
                            text = "A Rintlak Dan",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            // Daily rows
            for (i in 0 until daysToShow) {
                val confData = confidenceData.getOrNull(i)
                val confidence = confData?.overall ?: getDefaultConfidence(i)
                val confidenceLabel = confData?.label ?: getConfidenceLabel(confidence)
                val rainMm = daily.precipitationSum.getOrNull(i) ?: 0.0
                val maxTemp = daily.tempMax.getOrNull(i) ?: continue
                val minTemp = daily.tempMin.getOrNull(i) ?: continue
                PremiumForecastRow(
                    dateIso = daily.time[i],
                    max = maxTemp,
                    min = minTemp,
                    rainProb = daily.rainProb.getOrNull(i) ?: 0,
                    rainMm = rainMm,
                    iconCode = daily.weatherCode.getOrNull(i) ?: 0,
                    confidence = confidence,
                    confidenceLabel = confidenceLabel,
                )
            }
            
            // Confidence legend
            Spacer(Modifier.height(8.dp))
            ConfidenceLegend()
        }
    }
}

/** Legend for confidence levels - responsive for small screens */
@Composable
private fun ConfidenceLegend() {
    val shape = RoundedCornerShape(8.dp)
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    
    // Responsive sizing
    val isSmallScreen = screenWidthDp < 360
    val titleFontSize = if (isSmallScreen) 10.sp else 12.sp
    val itemFontSize = if (isSmallScreen) 9.sp else 11.sp
    val dotSize = if (isSmallScreen) 10.dp else 12.dp
    val padding = if (isSmallScreen) 8.dp else 12.dp
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF0F172A).copy(alpha = 0.8f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
            .padding(horizontal = padding, vertical = padding - 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header
            Text(
                text = "🎯 A Rintlak Dan:",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = titleFontSize,
                fontWeight = FontWeight.SemiBold,
            )
            // Legend items - wrap if needed on small screens
            if (isSmallScreen) {
                // Two rows for small screens
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        EnhancedConfidenceLegendItem(Color(0xFF4CAF50), "Tha Tak", itemFontSize, dotSize)
                        EnhancedConfidenceLegendItem(Color(0xFF8BC34A), "Tha", itemFontSize, dotSize)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        EnhancedConfidenceLegendItem(Color(0xFFFFC107), "Tha Tawk", itemFontSize, dotSize)
                        EnhancedConfidenceLegendItem(Color(0xFFE57373), "Beisei Theih", itemFontSize, dotSize)
                    }
                }
            } else {
                // Single row for normal screens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EnhancedConfidenceLegendItem(Color(0xFF4CAF50), "Tha Tak", itemFontSize, dotSize)
                    EnhancedConfidenceLegendItem(Color(0xFF8BC34A), "Tha", itemFontSize, dotSize)
                    EnhancedConfidenceLegendItem(Color(0xFFFFC107), "Tha Tawk", itemFontSize, dotSize)
                    EnhancedConfidenceLegendItem(Color(0xFFE57373), "Beisei Theih", itemFontSize, dotSize)
                }
            }
        }
    }
}

/** Enhanced confidence legend item with larger dot and visible text */
@Composable
private fun EnhancedConfidenceLegendItem(
    color: Color, 
    label: String,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    dotSize: Dp = 12.dp
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .shadow(2.dp, CircleShape, spotColor = color)
                .background(color, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
        )
    }
}


/** Get default confidence based on day index (0-based) */
private fun getDefaultConfidence(dayIndex: Int): Double {
    return when (dayIndex) {
        0, 1 -> 0.95  // Day 1-2: Very reliable
        2 -> 0.85     // Day 3: Reliable
        3 -> 0.75     // Day 4: Good
        4 -> 0.60     // Day 5: Moderate
        5 -> 0.50     // Day 6: Fair
        6 -> 0.40     // Day 7: Less reliable
        else -> 0.30  // Day 8+: Low
    }
}

/** Get confidence color based on confidence value */
@Composable
private fun getConfidenceColor(confidence: Double): Color {
    return when {
        confidence >= 0.85 -> Color(0xFF4CAF50) // Green - very reliable
        confidence >= 0.70 -> Color(0xFF8BC34A) // Light green - reliable
        confidence >= 0.55 -> Color(0xFFFFC107) // Yellow - moderate
        confidence >= 0.40 -> Color(0xFFFF9800) // Orange - fair
        else -> Color(0xFFE57373) // Red - less reliable
    }
}

/** Get Mizo confidence label */
private fun getConfidenceLabel(confidence: Double): String {
    return when {
        confidence >= 0.85 -> "Tha Tak"      // Very reliable
        confidence >= 0.70 -> "Tha"          // Good
        confidence >= 0.55 -> "Tha Tawk"     // Moderate
        confidence >= 0.40 -> "Beisei Theih" // Fair
        else -> "Ngaihdan"                   // Less reliable
    }
}

/** Get UV level label */
private fun getUvLevel(uv: Double): String {
    return when {
        uv >= 11 -> "Extreme"
        uv >= 8 -> "Very High"
        uv >= 6 -> "High"
        uv >= 3 -> "Moderate"
        else -> "Low"
    }
}

/** Get UV gradient colors based on UV index */
private fun getUvGradientColors(uv: Double): List<Color> {
    return when {
        uv >= 11 -> listOf(Color(0xFFE91E63), Color(0xFFC2185B)) // Purple - Extreme
        uv >= 8 -> listOf(Color(0xFFFF5722), Color(0xFFE64A19))  // Red - Very High
        uv >= 6 -> listOf(Color(0xFFFF9800), Color(0xFFF57C00))  // Orange - High
        uv >= 3 -> listOf(Color(0xFFFFC107), Color(0xFFFFA000))  // Yellow - Moderate
        else -> listOf(Color(0xFF4CAF50), Color(0xFF388E3C))     // Green - Low
    }
}

@Composable
private fun PremiumForecastRow(
    dateIso: String,
    max: Double,
    min: Double,
    rainProb: Int,
    rainMm: Double = 0.0,
    iconCode: Int,
    confidence: Double = 0.95,
    confidenceLabel: String = "",
) {
    val dayNameMizo = dayNameMizo(dateIso)
    val maxColor = getTemperatureColor(max)
    val minColor = getTemperatureColor(min)
    val confidenceColor = getConfidenceColor(confidence)
    val displayLabel = confidenceLabel.ifBlank { getConfidenceLabel(confidence) }
    
    val shape = RoundedCornerShape(12.dp)
    
    // Responsive sizing
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isSmallScreen = screenWidthDp < 360
    
    val dayFontSize = if (isSmallScreen) 13.sp else 15.sp
    val tempMaxFontSize = if (isSmallScreen) 15.sp else 18.sp
    val tempMinFontSize = if (isSmallScreen) 13.sp else 16.sp
    val rainFontSize = if (isSmallScreen) 9.sp else 11.sp
    val iconSize = if (isSmallScreen) 24.dp else 28.dp
    val confidenceDotSize = if (isSmallScreen) 12.dp else 14.dp
    val horizontalPadding = if (isSmallScreen) 10.dp else 14.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF1E293B),  // Solid dark slate
                        Color(0xFF0F172A),  // Very dark navy
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF475569).copy(alpha = 0.6f),
                        Color(0xFF334155).copy(alpha = 0.4f),
                    )
                ),
                shape = shape
            )
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Confidence indicator - color only, larger and more visible
        Box(
            modifier = Modifier
                .size(confidenceDotSize)
                .shadow(4.dp, CircleShape, spotColor = confidenceColor)
                .background(
                    Brush.radialGradient(
                        listOf(
                            confidenceColor,
                            confidenceColor.copy(alpha = 0.7f)
                        )
                    ),
                    CircleShape
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )
        
        Spacer(Modifier.width(if (isSmallScreen) 6.dp else 10.dp))
        
        // Day name - pure white bold
        Text(
            text = dayNameMizo,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = dayFontSize,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Rain info: probability % AND amount mm
        if (rainProb > 0 || rainMm > 0.1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = if (isSmallScreen) 6.dp else 10.dp)
            ) {
                // Rain probability %
                if (rainProb > 0) {
                    Text(
                        text = "💧",
                        fontSize = if (isSmallScreen) 8.sp else 10.sp,
                    )
                    Text(
                        text = "$rainProb%",
                        color = Color(0xFF64FFDA), // Teal for probability
                        fontSize = rainFontSize,
                        fontWeight = FontWeight.Bold,
                    )
                }
                
                // Rain amount mm
                if (rainMm > 0.1) {
                    if (rainProb > 0) {
                        Spacer(Modifier.width(if (isSmallScreen) 2.dp else 4.dp))
                    }
                    Text(
                        text = "🌧️",
                        fontSize = if (isSmallScreen) 8.sp else 10.sp,
                    )
                    Text(
                        text = "${"%.1f".format(rainMm)}",
                        color = Color(0xFF3A86FF), // Blue for mm amount
                        fontSize = rainFontSize,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // Weather icon
        WeatherSvgIcon(code = iconCode, isDay = true, modifier = Modifier.size(iconSize))

        Spacer(Modifier.width(if (isSmallScreen) 6.dp else 12.dp))

        // Temperature range - larger and bolder
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${max.toInt()}°",
                color = maxColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = tempMaxFontSize,
            )
            Text(
                text = " / ",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = if (isSmallScreen) 11.sp else 14.sp,
            )
            Text(
                text = "${min.toInt()}°",
                color = minColor,
                fontWeight = FontWeight.Bold,
                fontSize = tempMinFontSize,
            )
        }
    }
}

// Enhanced Seasonal Outlook Section - REDESIGNED for clarity
@Composable
private fun SeasonalForecastSection(weather: WeatherDoc, isDay: Boolean) {
    val seasonal = weather.seasonalOutlook
    
    // Check if we have enhanced seasonal data (backend v86+)
    val currentMonth = seasonal?.currentMonth
    val nextMonth = seasonal?.nextMonth
    val monthlyForecasts = seasonal?.monthlyForecasts
    
    // If no seasonal data at all, don't show
    if (currentMonth == null && nextMonth == null && seasonal?.text.isNullOrBlank()) {
        return
    }
    
    val shape = RoundedCornerShape(28.dp)
    
    // Premium gradient colors
    val primaryGradient = listOf(
        Color(0xFF1a1a2e),
        Color(0xFF16213e),
        Color(0xFF0f3460)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = shape,
                spotColor = Color(0xFF6366F1).copy(alpha = 0.4f),
                ambientColor = Color(0xFF8B5CF6).copy(alpha = 0.2f),
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = primaryGradient,
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF6366F1).copy(alpha = 0.6f),
                        Color(0xFF8B5CF6).copy(alpha = 0.4f),
                        Color(0xFFEC4899).copy(alpha = 0.3f),
                    )
                ),
                shape = shape
            )
    ) {
        // Decorative glow effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF6366F1).copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Premium Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Animated icon container
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFF8B5CF6)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📅", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Seasonal Outlook",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.3).sp,
                        )
                        Text(
                            text = "Past vs Upcoming",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                        )
                    }
                }
                // Model badge
                seasonal?.forecastModel?.let { model ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF10B981).copy(alpha = 0.3f),
                                        Color(0xFF059669).copy(alpha = 0.2f)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                Color(0xFF10B981).copy(alpha = 0.5f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = model,
                            color = Color(0xFF34D399),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            
            // Current & Next Month Comparison Cards
            currentMonth?.let { month ->
                PremiumMonthComparisonCard(
                    title = month.monthName,
                    climatology = month.climatology,
                    seasonalForecast = month.seasonalForecast,
                    season = month.season,
                    isCurrent = true,
                )
            }
            
            nextMonth?.let { month ->
                PremiumMonthComparisonCard(
                    title = month.monthName,
                    climatology = month.climatology,
                    seasonalForecast = month.seasonalForecast,
                    season = null,
                    isCurrent = false,
                )
            }
            
            // 6-Month Forecast Grid
            if (!monthlyForecasts.isNullOrEmpty() && monthlyForecasts.size > 2) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📊", fontSize = 14.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "6-Month Outlook",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                            )
                        }
                        // Legend
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF97316))
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("°C", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B82F6))
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("mm", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            }
                        }
                    }
                    
                    // Premium horizontal scroll cards
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(monthlyForecasts.drop(2)) { forecast ->
                            PremiumMonthlyMiniCard(forecast = forecast)
                        }
                    }
                }
            }
        }
    }
}

/** Premium Month Comparison Card - Shows clear Past vs Forecast with inline differences */
@Composable
private fun PremiumMonthComparisonCard(
    title: String,
    climatology: com.mapuia.khawchinthlirna.data.model.ClimatologyData?,
    seasonalForecast: com.mapuia.khawchinthlirna.data.model.SeasonalForecastData? = null,
    season: String? = null,
    isCurrent: Boolean = false,
) {
    val cardGradient = if (isCurrent) {
        listOf(
            Color(0xFF1E3A5F).copy(alpha = 0.9f),
            Color(0xFF0D2137).copy(alpha = 0.7f)
        )
    } else {
        listOf(
            Color(0xFF2D1B4E).copy(alpha = 0.9f),
            Color(0xFF1A0F2E).copy(alpha = 0.7f)
        )
    }
    
    val accentColor = if (isCurrent) Color(0xFF3B82F6) else Color(0xFF8B5CF6)
    
    // Calculate differences for comparison
    val avgTemp = climatology?.avgTempMax
    val forecastTemp = seasonalForecast?.predictedTempMax?.toInt()
    val tempDiff = if (avgTemp != null && forecastTemp != null) forecastTemp - avgTemp else null
    
    val avgRain = climatology?.avgRainMm
    val forecastRain = seasonalForecast?.predictedRainMm?.toInt()
    val rainDiff = if (avgRain != null && avgRain > 0 && forecastRain != null) {
        ((forecastRain - avgRain) * 100 / avgRain)
    } else null
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(cardGradient))
            .border(
                1.dp,
                accentColor.copy(alpha = 0.4f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.4f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isCurrent) "📅" else "🔮",
                        fontSize = 14.sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(
                        text = if (isCurrent) "Current Month" else "Next Month",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                    )
                }
            }
            season?.let { s ->
                val (seasonEmoji, seasonColor) = when (s.uppercase()) {
                    "DRY" -> Pair("❄️", Color(0xFF60A5FA))
                    "MONSOON", "MONSOON_PEAK" -> Pair("🌧️", Color(0xFF38BDF8))
                    "PRE_MONSOON", "WARMING" -> Pair("☀️", Color(0xFFFBBF24))
                    "POST_MONSOON", "MONSOON_RETREAT" -> Pair("🍂", Color(0xFF4ADE80))
                    else -> Pair("🌤️", Color.White.copy(alpha = 0.7f))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(seasonColor.copy(alpha = 0.15f))
                        .border(1.dp, seasonColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = seasonEmoji, fontSize = 14.sp)
                }
            }
        }
        
        // Temperature Comparison Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFB923C).copy(alpha = 0.1f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🌡️", fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Temp",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            
            // Values: Average → Forecast (diff)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                // Average
                Text(
                    text = "${avgTemp ?: "--"}°",
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                
                // Arrow
                Text(
                    text = " → ",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                )
                
                // Forecast with inline difference
                val tempColor = when {
                    (tempDiff ?: 0) >= 2 -> Color(0xFFFF7043)
                    (tempDiff ?: 0) <= -2 -> Color(0xFF4FC3F7)
                    else -> Color(0xFF4ADE80)
                }
                
                Text(
                    text = "${forecastTemp ?: "--"}°",
                    color = tempColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                )
                
                // Difference badge inline
                tempDiff?.let { diff ->
                    Spacer(Modifier.width(6.dp))
                    val sign = if (diff >= 0) "+" else ""
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tempColor.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$sign$diff°",
                            color = tempColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        
        // Rain Comparison Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF3B82F6).copy(alpha = 0.1f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "💧", fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Rain",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            
            // Values: Average → Forecast (diff%)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                // Average
                Text(
                    text = "${avgRain ?: "--"}mm",
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                
                // Arrow
                Text(
                    text = " → ",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                )
                
                // Forecast with inline difference
                val rainColor = when {
                    (rainDiff ?: 0) >= 20 -> Color(0xFF38BDF8)
                    (rainDiff ?: 0) <= -20 -> Color(0xFFFBBF24)
                    else -> Color(0xFF4ADE80)
                }
                
                Text(
                    text = "${forecastRain ?: "--"}mm",
                    color = rainColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                )
                
                // Difference badge inline (percentage)
                rainDiff?.let { diff ->
                    Spacer(Modifier.width(6.dp))
                    val sign = if (diff >= 0) "+" else ""
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(rainColor.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$sign$diff%",
                            color = rainColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/** Premium Mini Card for 6-month forecast - shows value with inline diff */
@Composable
private fun PremiumMonthlyMiniCard(forecast: com.mapuia.khawchinthlirna.data.model.MonthlyForecast) {
    // Temperature color based on anomaly
    val tempAnom = forecast.tempAnomalyC ?: 0.0
    val tempColor = when {
        tempAnom >= 2.0 -> Color(0xFFFF7043)
        tempAnom >= 1.0 -> Color(0xFFFFA726)
        tempAnom <= -2.0 -> Color(0xFF4FC3F7)
        tempAnom <= -1.0 -> Color(0xFF81D4FA)
        else -> Color(0xFF4ADE80)
    }
    
    // Rain color based on change
    val rainPct = forecast.precipPctChange ?: 0.0
    val rainColor = when {
        rainPct >= 30 -> Color(0xFF42A5F5)
        rainPct >= 15 -> Color(0xFF64B5F6)
        rainPct <= -30 -> Color(0xFFFFA726)
        rainPct <= -15 -> Color(0xFFFFCC80)
        else -> Color(0xFF4ADE80)
    }
    
    Column(
        modifier = Modifier
            .widthIn(min = 82.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(16.dp)
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Month name
        Text(
            text = forecast.monthName.take(3),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
        
        // Temperature with inline diff
        forecast.predictedTempMax?.let { temp ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${temp.toInt()}°",
                    color = tempColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                )
                forecast.tempAnomalyC?.let { anom ->
                    val sign = if (anom >= 0) "+" else ""
                    Text(
                        text = "(${sign}${anom.toInt()}°)",
                        color = tempColor.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        
        // Rain with inline diff %
        forecast.predictedRainMm?.let { rain ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(rainColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💧", fontSize = 9.sp)
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "${rain.toInt()}mm",
                        color = rainColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                }
                forecast.precipPctChange?.let { pct ->
                    val sign = if (pct >= 0) "+" else ""
                    Text(
                        text = "(${sign}${pct.toInt()}%)",
                        color = rainColor.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/** Data source and accuracy info section */
@Composable
private fun DataSourceInfo(weather: WeatherDoc, isDay: Boolean) {
    val modelWeights = weather.meta?.modelWeights
    val modelsUsed = weather.modelsUsed
    val generated = weather.generated
    
    // Calculate data freshness
    val dataAgeHours = remember(generated) {
        try {
            if (generated != null) {
                val instant = java.time.Instant.parse(generated)
                val hours = java.time.Duration.between(instant, java.time.Instant.now()).toHours()
                hours
            } else null
        } catch (_: Exception) { null }
    }
    val isStale = dataAgeHours != null && dataAgeHours > 6 // More than 6 hours old
    
    // If no model info, don't show
    if (modelsUsed.isNullOrEmpty() && modelWeights.isNullOrEmpty()) return
    
    val shape = RoundedCornerShape(16.dp)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF1A1A2E).copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = shape
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header - bigger and clearer
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "📡",
                    fontSize = 14.sp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Data Source",
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
            
            // Model weights visualization - BIGGER and CLEARER
            if (modelWeights != null && modelWeights.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    modelWeights.forEach { (model, weightAny) ->
                        val weight = (weightAny as? Number)?.toDouble() ?: 0.0
                        val displayName = when (model.lowercase()) {
                            "ecmwf" -> "ECMWF"
                            "gfs" -> "GFS"
                            "icon" -> "ICON"
                            else -> model.uppercase()
                        }
                        val color = when (model.lowercase()) {
                            "ecmwf" -> Color(0xFF66BB6A) // Green - primary model (brighter)
                            "gfs" -> Color(0xFF42A5F5)   // Blue (brighter)
                            "icon" -> Color(0xFFFFB74D)  // Orange (brighter)
                            else -> Color.White
                        }
                        val percentage = (weight * 100).toInt()
                        
                        // Model chip with background
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(color.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(color, CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "$displayName",
                                    color = color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "$percentage%",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }
            } else if (!modelsUsed.isNullOrEmpty()) {
                // Fallback to just showing model names
                Text(
                    text = modelsUsed.joinToString(" • ") { it.uppercase() },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            
            // Generation time - convert to local timezone
            if (generated != null) {
                val timeStr = try {
                    // Parse ISO timestamp and convert to local timezone
                    val utcOffsetSeconds = weather.utcOffsetSeconds ?: (5 * 3600 + 30 * 60) // Default IST +5:30
                    val instant = java.time.Instant.parse(generated)
                    val localTime = instant.plusSeconds(utcOffsetSeconds.toLong())
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("MM/dd HH:mm")
                        .withZone(java.time.ZoneOffset.UTC)
                    formatter.format(localTime)
                } catch (_: Exception) {
                    // Fallback: simple parse
                    try {
                        val parts = generated.split("T")
                        if (parts.size >= 2) {
                            val time = parts[1].take(5)
                            val date = parts[0].takeLast(5).replace("-", "/")
                            "$date $time"
                        } else generated
                    } catch (_: Exception) { generated }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Staleness warning indicator
                    if (isStale) {
                        Text(
                            text = "⚠️",
                            fontSize = 10.sp,
                        )
                    }
                    Text(
                        text = if (isStale) "Hlui: $timeStr" else "Updated: $timeStr",
                        color = if (isStale) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontWeight = if (isStale) FontWeight.Medium else FontWeight.Normal,
                    )
                    if (isStale && dataAgeHours != null) {
                        Text(
                            text = "(${dataAgeHours}h hlui)",
                            color = Color(0xFFFFB74D).copy(alpha = 0.7f),
                            fontSize = 8.sp,
                        )
                    }
                }
            }
            
            // Confidence legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                ConfidenceLegendItem(Color(0xFF4CAF50), "Rintlak")
                ConfidenceLegendItem(Color(0xFFFFC107), "Pangngai")
                ConfidenceLegendItem(Color(0xFFE57373), "Pawm ve theih")
            }
        }
    }
}

@Composable
private fun ConfidenceLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
        )
    }
}

private fun dayNameMizo(dateIso: String): String {
    return try {
        val date = LocalDate.parse(dateIso.take(10))
        when (date.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "Thawhtanni"
            java.time.DayOfWeek.TUESDAY -> "Thawhlehni"
            java.time.DayOfWeek.WEDNESDAY -> "Nilaini"
            java.time.DayOfWeek.THURSDAY -> "Ningani"
            java.time.DayOfWeek.FRIDAY -> "Zirtawpni"
            java.time.DayOfWeek.SATURDAY -> "Inrinni"
            java.time.DayOfWeek.SUNDAY -> "Pathianni"
        }
    } catch (_: Throwable) {
        dateIso.take(10)
    }
}

/** Wrapper for SunriseSunsetArc with premium styling */
@Composable
private fun SunriseSunsetCard(weather: WeatherDoc, isDay: Boolean) {
    // Just call the existing arc implementation
    SunriseSunsetArc(weather, isDay)
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun SunriseSunsetArc(weather: WeatherDoc, isDayParam: Boolean) {
    val daily = weather.daily ?: return
    val sunriseStr = daily.sunrise.firstOrNull() ?: return
    val sunsetStr = daily.sunset.firstOrNull() ?: return
    val now = LocalTime.now()

    val sunrise = LocalTime.parse(extractTimeHHMM(sunriseStr))
    val sunset = LocalTime.parse(extractTimeHHMM(sunsetStr))

    val totalMinutes = (sunset.toSecondOfDay() - sunrise.toSecondOfDay()) / 60
    val elapsedMinutes = (now.toSecondOfDay() - sunrise.toSecondOfDay()) / 60
    val progress = (elapsedMinutes.toFloat() / totalMinutes).coerceIn(0f, 1f)

    val isDay = now.isAfter(sunrise) && now.isBefore(sunset)
    
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                spotColor = Color(0xFFFFB347).copy(alpha = 0.3f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A1A2E).copy(alpha = 0.85f),
                        Color(0xFF16213E).copy(alpha = 0.75f),
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFFFFB347).copy(alpha = 0.5f),
                        Color(0xFFFF6F00).copy(alpha = 0.3f),
                    )
                ),
                shape = shape
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "☀️", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SUNRISE & SUNSET",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }

            // Arc Canvas - professional design
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val stroke = 8.dp.toPx()
                val horizontalPadding = 24.dp.toPx()
                val arcWidth = size.width - (horizontalPadding * 2)
                val arcHeight = 80.dp.toPx()
                val arcRect = Size(arcWidth, arcHeight * 2)
                val topLeft = Offset(horizontalPadding, size.height - 20.dp.toPx() - arcHeight)

                // Dashed background line (horizon)
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(horizontalPadding, size.height - 20.dp.toPx()),
                    end = Offset(size.width - horizontalPadding, size.height - 20.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )

                // Base arc (unfilled part) - gray
                drawArc(
                    color = Color.White.copy(alpha = 0.15f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcRect,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )

                // Progress arc with gradient
                if (progress > 0f) {
                    drawArc(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFFD700),  // Gold
                                Color(0xFFFF8C00),  // Dark Orange
                                Color(0xFFFF4500),  // Red-Orange
                            )
                        ),
                        startAngle = 180f,
                        sweepAngle = 180f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcRect,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }

                // Sun position on arc
                val angleDeg = 180 + (180 * progress)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val cx = topLeft.x + arcRect.width / 2
                val cy = topLeft.y + arcRect.height
                val r = arcRect.width / 2 - stroke / 2
                val iconX = (cx + r * cos(angleRad)).toFloat()
                val iconY = (cy + r * sin(angleRad)).toFloat()

                // Outer glow
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            if (isDay) Color(0xFFFFD700).copy(alpha = 0.6f) else Color(0xFFE8E8E8).copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        center = Offset(iconX, iconY),
                        radius = 24.dp.toPx()
                    ),
                    radius = 24.dp.toPx(),
                    center = Offset(iconX, iconY),
                )

                // Sun/Moon circle
                drawCircle(
                    color = if (isDay) Color(0xFFFFD700) else Color(0xFFF5F5F5),
                    radius = 12.dp.toPx(),
                    center = Offset(iconX, iconY),
                )
                
                // Inner highlight
                drawCircle(
                    color = if (isDay) Color(0xFFFFFFB0) else Color.White,
                    radius = 6.dp.toPx(),
                    center = Offset(iconX - 2.dp.toPx(), iconY - 2.dp.toPx()),
                )
            }

            // Time labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Sunrise
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "🌅 Sunrise",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                    )
                    Text(
                        text = sunrise.format(DateTimeFormatter.ofPattern("h:mm a")),
                        color = Color(0xFFFFD700),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                
                // Duration
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val daylightHours = totalMinutes / 60
                    val daylightMins = totalMinutes % 60
                    Text(
                        text = "Daylight",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                    )
                    Text(
                        text = "${daylightHours}h ${daylightMins}m",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                
                // Sunset
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "🌇 Sunset",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                    )
                    Text(
                        text = sunset.format(DateTimeFormatter.ofPattern("h:mm a")),
                        color = Color(0xFFFF8C00),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// RadarWebView removed - RainViewer has no coverage in this region

@Composable
private fun NativeAdAndroidView(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            NativeAdView(ctx).apply {
                val title = TextView(ctx).apply {
                    setTextColor(0xFFFFFFFF.toInt())
                    setTypeface(typeface, Typeface.BOLD)
                    textSize = 16f
                    id = View.generateViewId()
                }

                val body = TextView(ctx).apply {
                    setTextColor(0xCCFFFFFF.toInt())
                    textSize = 13f
                    id = View.generateViewId()
                }

                val media = MediaView(ctx).apply { id = View.generateViewId() }

                val root = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(24, 24, 24, 24)
                    addView(title)
                    addView(body)
                    addView(
                        media,
                        android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            320,
                        ),
                    )
                }

                addView(root)

                headlineView = title
                bodyView = body
                mediaView = media
            }
        },
        update = { view ->
            val title = view.headlineView as TextView
            val body = view.bodyView as TextView
            title.text = nativeAd.headline
            body.text = nativeAd.body ?: ""
            view.setNativeAd(nativeAd)
        },
    )
}

// NOTE: RadarMap and RadarWebView removed - RainViewer has no coverage in Mizoram/Chin Hills region

@Composable
private fun NativeAdCard(modifier: Modifier = Modifier, isDay: Boolean) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    // Ensure we don't leak ads.
    DisposableEffect(nativeAd) {
        onDispose {
            nativeAd?.destroy()
        }
    }

    val nativeAdUnitId = remember { context.getString(R.string.admob_native_unit_id) }

    LaunchedEffect(Unit) {
        val loader = AdLoader.Builder(context, nativeAdUnitId)
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setRequestMultipleImages(false)
                    .build(),
            )
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
            }
            .build()

        loader.loadAd(AdRequest.Builder().build())
    }

    // Ad placement polish: keep a consistent placeholder height to avoid jumpy layout.
    GlassCard(modifier = modifier, isDay = isDay) {
        Text(text = "Sponsored", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

        val ad = nativeAd
        if (ad == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Loading ad…", color = Color.White.copy(alpha = 0.75f))
            }
        } else {
            NativeAdAndroidView(
                nativeAd = ad,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
            )
        }
    }
}

@Composable
private fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bannerAdUnitId = remember { context.getString(R.string.admob_banner_unit_id) }
    
    // Calculate adaptive banner size based on screen width
    val density = context.resources.displayMetrics.density
    val screenWidthDp = (context.resources.displayMetrics.widthPixels / density).toInt()

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(bottom = 8.dp),
        factory = { ctx ->
            AdView(ctx).apply {
                // Use adaptive banner for better fit across devices
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, screenWidthDp))
                adUnitId = bannerAdUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}

@Composable
private fun GlassHeaderBar(
    onReport: () -> Unit,
    onInfoClick: () -> Unit = {},
    onMenuItemClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(0.dp, 0.dp, 28.dp, 28.dp)
    
    // Animated shimmer effect for premium feel
    val infiniteTransition = rememberInfiniteTransition(label = "headerShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .shadow(
                elevation = 24.dp,
                shape = shape,
                spotColor = Color(0xFF8338EC).copy(alpha = 0.4f),
                ambientColor = Color(0xFF3A86FF).copy(alpha = 0.2f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1a1a2e).copy(alpha = 0.95f),
                        Color(0xFF16213e).copy(alpha = 0.9f),
                        Color(0xFF0f3460).copy(alpha = 0.85f),
                    )
                )
            )
            .drawBehind {
                // Subtle animated gradient overlay
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF8338EC).copy(alpha = 0.08f),
                            Color(0xFF3A86FF).copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                        startX = size.width * (shimmerOffset - 0.3f),
                        endX = size.width * (shimmerOffset + 0.3f)
                    )
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.08f),
                        Color(0xFF8338EC).copy(alpha = 0.15f),
                    )
                ),
                shape = shape
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // App branding with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Weather icon with glow
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(8.dp, CircleShape, spotColor = Color(0xFF3A86FF).copy(alpha = 0.5f))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF3A86FF),
                                    Color(0xFF8338EC),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⛅",
                        fontSize = 20.sp,
                    )
                }
                
                Column {
                    Text(
                        text = "Khawchin Thlirna",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.3).sp,
                    )
                }
            }

            // Premium Hamburger Menu Button
            Box {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = Color(0xFF8338EC).copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.White.copy(alpha = 0.05f),
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.1f),
                                )
                            ),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { showMenu = true }
                        .semantics { contentDescription = "Open menu" },
                    contentAlignment = Alignment.Center
                ) {
                    // Premium hamburger icon with gradient lines
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.5.dp)
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFF3A86FF), Color.White)),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(2.5.dp)
                                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(2.5.dp)
                                .background(
                                    Brush.horizontalGradient(listOf(Color.White, Color(0xFF8338EC))),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
                
                // Premium Dropdown Menu
                PremiumDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    onReport = {
                        showMenu = false
                        onReport()
                    },
                    onMenuItemClick = { item ->
                        showMenu = false
                        onMenuItemClick(item)
                    }
                )
            }
        }
    }
}

/** Premium Dropdown Menu */
@Composable
private fun PremiumDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onReport: () -> Unit,
    onMenuItemClick: (String) -> Unit,
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .width(280.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460),
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f),
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF8338EC).copy(alpha = 0.2f),
                            Color(0xFF3A86FF).copy(alpha = 0.2f),
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "☰", fontSize = 20.sp)
                Text(
                    text = "Menu",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Weather Report - Primary Action with accent
        PremiumMenuItem(
            icon = "📝",
            iconBg = Color(0xFF06D6A0),
            text = "Khawchin reportna",
            subtitle = "Submit weather report",
            isPrimary = true,
            onClick = onReport
        )
        
        PremiumMenuDivider()
        
        // App Guide
        PremiumMenuItem(
            icon = "📖",
            iconBg = Color(0xFF3A86FF),
            text = "App hman dan",
            subtitle = "How to use this app",
            onClick = { onMenuItemClick("app_guide") }
        )
        
        // How Crowdsourcing Works
        PremiumMenuItem(
            icon = "👥",
            iconBg = Color(0xFF8338EC),
            text = "Report a ṭangkai dan",
            subtitle = "How reports help",
            onClick = { onMenuItemClick("crowdsourcing") }
        )
        
        // Rain Intensity Guide
        PremiumMenuItem(
            icon = "🌧️",
            iconBg = Color(0xFF00B4D8),
            text = "Ruah Sur Tam Dan Hrilhfiahna",
            subtitle = "Rain intensity guide",
            onClick = { onMenuItemClick("rain_guide") }
        )
        
        // Weather Data Explained
        PremiumMenuItem(
            icon = "🌡️",
            iconBg = Color(0xFFFF6B6B),
            text = "Khawchin data chiang zâwk",
            subtitle = "Understanding weather data",
            onClick = { onMenuItemClick("weather_data") }
        )
        
        PremiumMenuDivider()
        
        // Theme Selection
        PremiumMenuItem(
            icon = "🎨",
            iconBg = Color(0xFFFFBE0B),
            text = "Theme thlanna",
            subtitle = "Change app theme",
            onClick = { onMenuItemClick("theme") }
        )
        
        Spacer(Modifier.height(12.dp))
    }
}

/** Premium menu item with icon background and subtitle */
@Composable
private fun PremiumMenuItem(
    icon: String,
    iconBg: Color,
    text: String,
    subtitle: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "menuItemScale"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isPrimary) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(
                                iconBg.copy(alpha = 0.15f),
                                Color.Transparent,
                            )
                        )
                    )
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon with colored background
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = iconBg.copy(alpha = 0.5f))
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.radialGradient(
                        listOf(
                            iconBg,
                            iconBg.copy(alpha = 0.8f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 18.sp,
            )
        }
        
        Spacer(Modifier.width(14.dp))
        
        Column {
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            )
        }
        
        Spacer(Modifier.weight(1f))
        
        // Arrow indicator
        Text(
            text = "›",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Light,
        )
    }
}

/** Premium divider for menu */
@Composable
private fun PremiumMenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent,
                    )
                )
            )
    )
}

/**
 * Weather Systems Alert Card - Shows Bay of Bengal cyclone alerts with contextual messages
 * that explain the impact relative to the user's location
 * Now collapsible to save screen space
 */
@Composable
private fun WeatherSystemsAlertCard(
    weather: WeatherDoc,
    userLat: Double?,
    userLon: Double?,
) {
    val weatherSystems = weather.weatherSystems ?: return
    val bob = weatherSystems.bayOfBengal ?: return
    
    // Only show if there are active cyclones
    if (!bob.cycloneActive || bob.cyclones.isEmpty()) return
    
    val shape = RoundedCornerShape(16.dp)
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        bob.cyclones.forEach { cyclone ->
            // Collapsible state for each cyclone card
            var isExpanded by remember { mutableStateOf(false) }
            
            val impact = cyclone.impactAssessment
            val willImpact = impact?.willImpact == true
            val probability = impact?.impactProbability ?: 0
            // Calculate actual distance from user to cyclone (instead of using stored grid-cell value)
            val closestKm = if (userLat != null && userLon != null && cyclone.lat != 0.0 && cyclone.lon != 0.0) {
                calculateDistanceKm(userLat, userLon, cyclone.lat, cyclone.lon)
            } else {
                impact?.closestApproachKm ?: 0.0
            }
            val etaHours = impact?.etaHours ?: 0
            
            // Determine alert level and colors
            val (gradient, borderColor, emoji) = when {
                willImpact && probability >= 70 -> Triple(
                    Brush.horizontalGradient(listOf(Color(0xFFFF1744), Color(0xFFD50000))),
                    Color(0xFFFF1744),
                    "🌀"
                )
                willImpact || probability >= 40 -> Triple(
                    Brush.horizontalGradient(listOf(Color(0xFFFF6D00), Color(0xFFFF3D00))),
                    Color(0xFFFF6D00),
                    "🌀"
                )
                else -> Triple(
                    Brush.horizontalGradient(listOf(Color(0xFF3A86FF), Color(0xFF2563EB))),
                    Color(0xFF3A86FF),
                    "🌊"
                )
            }
            
            // Build contextual message in Mizo
            val contextualMessage = buildContextualCycloneMessage(
                cycloneName = cyclone.name,
                category = cyclone.category,
                categoryShort = cyclone.categoryShort,
                willImpact = willImpact,
                probability = probability,
                closestKm = closestKm.toInt(),
                etaHours = etaHours,
                cycloneLat = cyclone.lat,
                cycloneLon = cyclone.lon,
            )
            
            // Rotation animation for expand icon
            val rotationAngle by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = tween(200),
                label = "expandRotation"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = shape,
                        spotColor = borderColor.copy(alpha = 0.3f),
                    )
                    .clip(shape)
                    .background(gradient)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), shape)
                    .clickable { isExpanded = !isExpanded }
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Header - always visible, clickable to expand
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = emoji, fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Bay of Bengal Khawchin",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 8.sp,
                                )
                                Text(
                                    text = "Tropical Cyclone ${cyclone.name.ifBlank { "" }}".trim(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Category badge
                            if (cyclone.categoryShort.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cyclone.categoryShort,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            // Expand/Collapse icon
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(rotationAngle)
                            )
                        }
                    }
                    
                    // Short summary always visible
                    Text(
                        text = "📍 Km ${closestKm.toInt()} vela hnai",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                    )
                    
                    // Expanded content
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                        exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Spacer(Modifier.height(4.dp))
                            // Contextual message
                            Text(
                                text = contextualMessage,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                            )
                            
                            // Stats row
                            if (cyclone.windSpeedKmh > 0 || etaHours > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    if (cyclone.windSpeedKmh > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "💨", fontSize = 10.sp)
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                text = "${cyclone.windSpeedKmh.toInt()} km/h",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 10.sp,
                                            )
                                        }
                                    }
                                    if (etaHours > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "⏱️", fontSize = 10.sp)
                                            Spacer(Modifier.width(3.dp))
                                            Text(
                                                text = "~${etaHours}h hnuah",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 10.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculate distance between two coordinates using Haversine formula
 * Returns distance in kilometers
 */
private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    
    return earthRadiusKm * c
}

/**
 * Build contextual message about cyclone impact relative to user's location
 */
private fun buildContextualCycloneMessage(
    cycloneName: String,
    category: String,
    categoryShort: String,
    willImpact: Boolean,
    probability: Int,
    closestKm: Int,
    etaHours: Int,
    cycloneLat: Double,
    cycloneLon: Double,
): String {
    val name = cycloneName.ifBlank { "Thlipui" }
    
    // Determine location description
    val locationDesc = when {
        cycloneLon < 87 -> "Bay of Bengal khawthlang lamah" // Changed 'thlai' to 'khawthlang'
        cycloneLon < 90 -> "Bay of Bengal lai takah"        // Changed 'lailien' to 'lai takah'
        cycloneLon < 92 -> "Bangladesh chhim lam tuipuiah"  // More specific than just 'inkar'
        else -> "Andaman tuifinriatah"
    }
    
    return when {
        // High impact expected
        willImpact && probability >= 70 -> {
            "⚠️ $name ($categoryShort) chu $locationDesc a awm a, kan ram lam a rawn pan mek. " +
            (if (etaHours > 0) "Darkar $etaHours hnu velah a rawn thleng thei. " else "") +
            "Thli na tak leh ruahtui tam tak a thlen thei a, fimkhur hle tur!"
        }
        
        // Moderate impact possible
        willImpact || probability >= 40 -> {
            "$name ($categoryShort) chu $locationDesc a awm a, kan ram a rawn hnaih thei. " +
            "Km $closestKm vela hnaiin a rawn kal ang. Ruah leh thli a awm thei a, hriattirna ngaichang reng ang che."
        }
        
        // Low impact, but worth monitoring
        probability >= 20 -> {
            "$name chu $locationDesc a awm mek. I awmna a rawn thleng kher lo ang, " +
            "mahse thli leh ruah erawh a rawn thlen thei. Darkar 24 chhung chu ruah a sur thei."
        }
        
        // Far away, minimal impact
        else -> {
            "$name chu $locationDesc a awm a, i awmna a rawn thleng pha lo ang. " +
            "Km $closestKm vela hlaah a kal liam ang. Hriattirna erawh ngaichang reng rawh."
        }
    }
}

@Composable
private fun MarineAlertStrip(
    marineAlert: String,
    isDay: Boolean,
    modifier: Modifier = Modifier,
) {
    val level = marineAlert.trim().uppercase()
    if (level.isBlank()) return

    val (label, tint, gradient) = when (level) {
        "RED" -> Triple(
            "⚠️ Tuifinriat Ralveng: Thlipui Hlauhawm A Awm",
            Color(0xFFFF1744),
            Brush.horizontalGradient(listOf(Color(0xFFFF1744), Color(0xFFD50000)))
        )
        "ORANGE" -> Triple(
            "⚠️ Tuifinriat Ralveng: Thlipui A Intham Mek",
            Color(0xFFFF6D00),
            Brush.horizontalGradient(listOf(Color(0xFFFF6D00), Color(0xFFFF3D00)))
        )
        "YELLOW" -> Triple(
            "🌤️ Tuifinriat Ralveng: Low Pressure (Thlipui intham tan)",
            Color(0xFFFFD600),
            Brush.horizontalGradient(listOf(Color(0xFFFFD600), Color(0xFFFFC400)))
        )
        "GREEN" -> Triple(
            "✅ Tuifinriat Ralveng: A Thiang Tha E",
            Color(0xFF00E676),
            Brush.horizontalGradient(listOf(Color(0xFF00E676), Color(0xFF00C853)))
        )
        else -> return
    }

    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = tint.copy(alpha = 0.4f),
            )
            .clip(shape)
            .background(gradient)
            .border(
                1.dp,
                Color.White.copy(alpha = 0.3f),
                shape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                color = if (level == "YELLOW") Color.Black.copy(alpha = 0.9f) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun UpstreamRainAlertCard(weather: WeatherDoc) {
    val alert = weather.marineUpstreamRain ?: return
    if (alert.level.uppercase() != "HIGH" && alert.level.uppercase() != "MODERATE") return

    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF22D3EE), Color(0xFF2563EB)),
                ),
            )
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Upstream rain alert",
                    tint = Color.White,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Tuifinriat Lam Ralveng",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(text = alert.reason, color = Color.White)
        }
    }
}

@Composable
private fun ReportDialog(
    locationAvailable: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "I awmna hmun khaw dinhmun report rawh. GPS i on chuan a dik zawk ang.",
                )

                if (!locationAvailable) {
                    Text(
                        "GPS a off chuan report a thawn theih loh.",
                        color = Color(0xFFFFD166),
                        fontSize = 12.sp,
                    )
                }

                OutlinedButton(
                    onClick = { onSubmit("Ruah a sur") },
                    enabled = locationAvailable && !isSubmitting,
                ) { Text("Ruah a sur") }

                OutlinedButton(
                    onClick = { onSubmit("Khua a tha") },
                    enabled = locationAvailable && !isSubmitting,
                ) { Text("Khua a tha") }

                OutlinedButton(
                    onClick = { onSubmit("Thli a na") },
                    enabled = locationAvailable && !isSubmitting,
                ) { Text("Thli a na") }

                if (isSubmitting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Submitting…", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, enabled = !isSubmitting) { Text("Close") }
        },
    )
}

@Composable
private fun MiniMetricCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconRes: Int,
    accent: Brush,
) {
    val shape = RoundedCornerShape(20.dp)

    // Extract first color from gradient for glow effect
    val glowColor = when (title) {
        "WIND" -> Color(0xFF00D4FF)
        "RAINFALL" -> Color(0xFF3A86FF)
        "PRESSURE" -> Color(0xFFFF006E)
        "HUMIDITY" -> Color(0xFF06D6A0)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                spotColor = glowColor.copy(alpha = 0.3f),
                ambientColor = glowColor.copy(alpha = 0.15f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.08f),
                    ),
                ),
            )
            .background(
                // Subtle accent glow inside card
                Brush.radialGradient(
                    listOf(
                        glowColor.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    radius = 300f
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        glowColor.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.2f),
                        glowColor.copy(alpha = 0.15f),
                    )
                ),
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon with accent background
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent)
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Title column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            // Value row with prominent display
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = unit,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun NearbyReportsCard(
    onViewNearbyReports: () -> Unit,
    isDay: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val accentColors = listOf(Color(0xFF8338EC), Color(0xFF3A86FF))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = shape,
                spotColor = Color(0xFF8338EC).copy(alpha = 0.25f),
            )
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF8338EC).copy(alpha = 0.12f),
                        Color(0xFF3A86FF).copy(alpha = 0.08f),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(accentColors.map { it.copy(alpha = 0.4f) }),
                shape = shape
            )
            .clickable(onClick = onViewNearbyReports)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(accentColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📍",
                        fontSize = 22.sp,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Nearby Reports",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(
                        text = "I vela mi dangte report en rawh",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "→",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** Theme Selection Dialog */
@Composable
private fun ThemeSelectionDialog(
    onDismiss: () -> Unit,
    onThemeSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1a1a2e),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎨", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Theme Thlang",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Light Theme
                ThemeOptionButton(
                    icon = "☀️",
                    title = "Light",
                    description = "Eng eng a lang",
                    isSelected = false,
                    onClick = { onThemeSelected("light") },
                )
                
                // Dark Theme
                ThemeOptionButton(
                    icon = "🌙",
                    title = "Dark",
                    description = "Thim a lang",
                    isSelected = false,
                    onClick = { onThemeSelected("dark") },
                )
                
                // System Theme
                ThemeOptionButton(
                    icon = "⚙️",
                    title = "System",
                    description = "Phone setting zìr a zui ang",
                    isSelected = true, // Default
                    onClick = { onThemeSelected("system") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF06D6A0),
                ),
            ) {
                Text("Close", color = Color.White)
            }
        },
    )
}

/** Individual theme option button */
@Composable
private fun ThemeOptionButton(
    icon: String,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (isSelected) Color(0xFF06D6A0) else Color.White.copy(alpha = 0.2f)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (isSelected) Color(0xFF06D6A0).copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.05f)
            )
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
        }
        if (isSelected) {
            Text(
                text = "✓",
                color = Color(0xFF06D6A0),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
    }
}
