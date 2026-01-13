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
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
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

private val DayGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF667eea), // Indigo
        Color(0xFF764ba2), // Purple
    ),
)

// Dynamic weather-based gradients
private fun getAppBackground(weatherCode: Int, isDay: Boolean, hour: Int): Brush {
    return when {
        !isDay -> NightGradient
        weatherCode == 0 -> Brush.verticalGradient(
            listOf(
                Color(0xFF00B4DB), // Bright Cyan
                Color(0xFF0083B0), // Deep Ocean Blue
            )
        )
        weatherCode in 51..67 || weatherCode in 80..82 -> Brush.verticalGradient(
            listOf(
                Color(0xFF0F2027),
                Color(0xFF203A43),
                Color(0xFF2C5364),
            )
        )
        weatherCode in 95..99 -> Brush.verticalGradient(
            listOf(
                Color(0xFF141E30),
                Color(0xFF243B55),
                Color(0xFF6441A5),
            )
        )
        weatherCode in 1..3 -> Brush.verticalGradient(
            listOf(
                Color(0xFF4776E6),
                Color(0xFF8E54E9),
            )
        )
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

    var showReport by remember { mutableStateOf(false) }
    var reportSubmitting by remember { mutableStateOf(false) }

    // One-shot toast feedback counter
    var reportToastKey by remember { mutableIntStateOf(0) }

    // Get weather code for dynamic background
    val weatherCode = uiState.weather?.getCurrentWeather()?.weatherCode ?: 0
    val currentHour = remember { java.time.LocalTime.now().hour }

    // Dynamic background based on weather conditions
    val backgroundBrush = getAppBackground(weatherCode, isDay, currentHour)

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
                    onReport = { showReport = true },
                )
            },
            bottomBar = {
                BannerAd(modifier = Modifier.fillMaxWidth())
            },
        ) { paddingValues ->
            PullToRefreshBox(
                state = pullState,
                isRefreshing = uiState.isRefreshing,
                onRefresh = { vm.refresh(isUserInitiated = true) },
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
                        // Marine alert (primary backend alert) directly under header.
                        MarineAlertStrip(marineAlert = weather.marineAlert, isDay = isDay)

                        UpstreamRainAlertCard(weather)

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
                            )
                        }

                        HourlyForecast(weather, isDay = isDay)

                        NativeAdCard(modifier = Modifier.fillMaxWidth(), isDay = isDay)

                        CurrentConditionsGrid(weather)
                        SevenDayForecast(weather, isDay = isDay)
                        SunriseSunsetArc(weather, isDay)
                        RadarMap(weather, isDay = isDay)
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
                            val text = if (ok) "Ka lawm e! Report a hlawn a." else (msg ?: "Report submit a hlawh lo")
                            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                            reportToastKey++
                        },
                    )
                },
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
                        Text("Location off", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "GPS i phal chuan a hnaih ber grid a thlan thei a, a dik zawk ang.",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onRequestPermission, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Request")
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
                        Text("Something went wrong", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(errorMessage, color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
                    }
                }
            }

            isLoading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Loading weather…", color = Color.White.copy(alpha = 0.90f), fontWeight = FontWeight.SemiBold)
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
) {
    // Use helper to support both old and new data formats
    val current = weather.getCurrentWeather()
    val temp = current?.temp ?: 0.0

    GlassCard(modifier = Modifier.fillMaxWidth(), isDay = isDay) {
        // NOTE: Marine alert is intentionally NOT shown in Hero (shown under header only).

        val locationLabel = userPlaceName
            ?: if (userLat != null && userLon != null) "Near you" else null

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
                // Vibrant colored temperature
                Text(
                    text = "${temp.toInt()}°",
                    color = getTemperatureColor(temp),
                    fontSize = 84.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 84.sp,
                    letterSpacing = (-2).sp,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "A lum dan",
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
                weatherCode = current?.weatherCode ?: 0,
                isDay = isDay,
                modifier = Modifier.size(120.dp)
            )
        }
    }
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

    GlassCard(modifier = Modifier.fillMaxWidth(), isDay = isDay) {
        Text(
            text = "Hourly Forecast",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )

        // Compute the safe count across arrays we actually read.
        val count = weather.getSafeHourlyCount()
        if (count <= 0) return@GlassCard

        // Show up to 24 (one-day) if available.
        val itemsToShow = minOf(24, count)

        val rows = (0 until itemsToShow).map { idx ->
            Quint(
                hourly.time[idx],
                hourly.temp[idx],
                hourly.weatherCode.getOrElse(idx) { 0 },
                hourly.rainMm.getOrElse(idx) { 0.0 },
                hourly.wind.getOrElse(idx) { 0.0 },
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rows) { item ->
                val rawTime = item.first
                val label = if (rawTime.contains("T")) rawTime.takeLast(5) else rawTime

                val rainMm = item.fourth
                val wind = item.fifth

                // Sub-label shows both rain mm and wind km/h.
                val sub = buildString {
                    if (rainMm > 0.0) append("${"%.1f".format(rainMm)}mm")
                    if (wind > 0.0) {
                        if (isNotEmpty()) append("  ")
                        append("${wind.toInt()}km/h")
                    }
                }.ifBlank { null }

                HourlyPill(
                    label = label,
                    temp = item.second,
                    code = item.third,
                    highlighted = false,
                    subLabel = sub,
                )
            }
        }
    }
}

private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

@Composable
private fun HourlyPill(
    label: String,
    temp: Double,
    code: Int,
    highlighted: Boolean,
    subLabel: String? = null,
) {
    val shape = RoundedCornerShape(20.dp)
    val tempColor = getTemperatureColor(temp)

    val bg = if (highlighted) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF8338EC).copy(alpha = 0.7f),
                Color(0xFF3A86FF).copy(alpha = 0.5f),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.06f),
            ),
        )
    }

    val borderBrush = if (highlighted) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF8338EC).copy(alpha = 0.6f),
                Color(0xFF3A86FF).copy(alpha = 0.4f),
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.1f),
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
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )

        WeatherSvgIcon(code = code, isDay = true, modifier = Modifier.size(32.dp))

        subLabel?.let {
            Text(
                text = it,
                color = Color(0xFF00D4FF).copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        // Temperature with color coding
        Text(
            text = "${temp.toInt()}°",
            color = tempColor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun CurrentConditionsGrid(weather: WeatherDoc) {
    // Use helper to support both old and new data formats
    val current = weather.getCurrentWeather()

    // Some docs provide wind direction in `current.wind_dir` or `current.wind_direction`.
    val windDeg = current?.windDir ?: current?.windDirection
    val windDir = windDirLabel(windDeg)

    val pressure = weather.marineEvidence?.pressure

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MiniMetricCard(
            title = "WIND",
            value = "${(current?.wind ?: 0.0).toInt()}",
            unit = if (windDeg != null) "km/h ${windDir ?: ""}" else "km/h",
            iconRes = R.drawable.ic_wind,
            accent = Brush.linearGradient(listOf(Color(0xFF00D4FF), Color(0xFF00B4DB))), // Vibrant Cyan
            modifier = Modifier.weight(1f),
        )
        MiniMetricCard(
            title = "RAINFALL",
            value = "${"%.1f".format(current?.rainMm ?: 0.0)}",
            unit = "mm",
            subtitle = "darh khat chhung",
            iconRes = R.drawable.ic_rain_mm,
            accent = Brush.linearGradient(listOf(Color(0xFF3A86FF), Color(0xFF0066FF))), // Vibrant Blue
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MiniMetricCard(
            title = "PRESSURE",
            value = pressure?.let { "${"%.0f".format(it)}" } ?: "-",
            unit = "hPa",
            iconRes = R.drawable.ic_pressure,
            accent = Brush.linearGradient(listOf(Color(0xFFFF006E), Color(0xFF8338EC))), // Vibrant Pink-Purple
            modifier = Modifier.weight(1f),
        )
        MiniMetricCard(
            title = "HUMIDITY",
            value = "${current?.humidity ?: 0}",
            unit = "%",
            iconRes = R.drawable.ic_humidity_drop,
            accent = Brush.linearGradient(listOf(Color(0xFF06D6A0), Color(0xFF00B894))), // Vibrant Teal
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SevenDayForecast(weather: WeatherDoc, isDay: Boolean) {
    val daily = weather.daily ?: return

    val count = listOf(
        daily.time.size,
        daily.tempMax.size,
        daily.tempMin.size,
        daily.rainProb.size,
    ).minOrNull() ?: 0
    if (count == 0) return

    // If Firestore provides 10 days, show 10; otherwise show 7.
    val daysToShow = if (count >= 10) 10 else 7

    GlassCard(modifier = Modifier.fillMaxWidth(), isDay = isDay) {
        Text(text = if (daysToShow == 10) "10-Day" else "7-Day", color = Color.White, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (i in 0 until minOf(daysToShow, count)) {
                ForecastRow(
                    dateIso = daily.time[i],
                    max = daily.tempMax[i],
                    min = daily.tempMin[i],
                iconCode = weather.getCurrentWeather()?.weatherCode ?: 0,
                )
            }
        }
    }

    // Seasonal forecast section (only if Firestore provides it)
    SeasonalForecastSection(weather = weather, isDay = isDay)
}

@Composable
private fun SeasonalForecastSection(weather: WeatherDoc, isDay: Boolean) {
    val text = weather.seasonalOutlook?.text
        ?: weather.seasonalOutlookMonthly?.text
        ?: weather.seasonalOutlookMonthly?.months?.joinToString("\n")

    if (text.isNullOrBlank()) return

    GlassCard(modifier = Modifier.fillMaxWidth(), isDay = isDay) {
        Text(
            text = "Seasonal Outlook",
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ForecastRow(dateIso: String, max: Double, min: Double, iconCode: Int) {
    val dayNameMizo = dayNameMizo(dateIso)
    val maxColor = getTemperatureColor(max)
    val minColor = getTemperatureColor(min)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = dayNameMizo,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        WeatherSvgIcon(code = iconCode, isDay = true, modifier = Modifier.size(28.dp))

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${max.toInt()}°",
                color = maxColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            Text(
                text = " / ",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 14.sp,
            )
            Text(
                text = "${min.toInt()}°",
                color = minColor,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
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

@Composable
@Suppress("UNUSED_PARAMETER")
private fun SunriseSunsetArc(weather: WeatherDoc, isDayParam: Boolean) {
    // isDayParam currently not used; kept for API compatibility and future tuning.

    val daily = weather.daily ?: return
    val sunriseStr = daily.sunrise.firstOrNull() ?: return
    val sunsetStr = daily.sunset.firstOrNull() ?: return
    val now = LocalTime.now()

    val sunrise = LocalTime.parse(sunriseStr.takeLast(5))
    val sunset = LocalTime.parse(sunsetStr.takeLast(5))

    val totalMinutes = (sunset.toSecondOfDay() - sunrise.toSecondOfDay()) / 60
    val elapsedMinutes = (now.toSecondOfDay() - sunrise.toSecondOfDay()) / 60
    val progress = (elapsedMinutes.toFloat() / totalMinutes).coerceIn(0f, 1f)

    val isDay = now.isAfter(sunrise) && now.isBefore(sunset)

    GlassCard(modifier = Modifier.fillMaxWidth(), isDay = isDay) {
        Text(text = "SUNRISE & SUNSET", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = sunrise.format(DateTimeFormatter.ofPattern("h:mm a")),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = sunset.format(DateTimeFormatter.ofPattern("h:mm a")),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.06f)),
        ) {
            val stroke = 8.dp.toPx()
            val padding = 18.dp.toPx()
            val arcRect = Size(size.width - padding * 2, (size.height - padding * 2) * 1.6f)
            val topLeft = Offset(padding, size.height - padding - arcRect.height)

            // Base arc
            drawArc(
                color = Color.White.copy(alpha = 0.16f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcRect,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // Progress arc
            if (progress > 0f) {
                drawArc(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFFD166), Color(0xFFF97316))
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcRect,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }

            // Sun/Moon icon
            val angleDeg = 180 + (180 * progress)
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val cx = topLeft.x + arcRect.width / 2
            val cy = topLeft.y + arcRect.height
            val r = arcRect.width / 2
            val iconX = (cx + r * cos(angleRad)).toFloat()
            val iconY = (cy + r * sin(angleRad)).toFloat()

            drawCircle(
                color = if (isDay) Color(0xFFFFD166) else Color(0xFFF1F5F9),
                radius = stroke,
                center = Offset(iconX, iconY),
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                radius = stroke,
                center = Offset(iconX, iconY + 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RadarWebView(
    url: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                loadUrl(url)
            }
        },
        update = { it.loadUrl(url) },
    )
}

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

@Composable
@Suppress("UNCHECKED_CAST")
private fun RadarMap(weather: WeatherDoc, isDay: Boolean) {
    val url = weather.meta?.radarUrl ?: return

    GlassCard(modifier = Modifier.fillMaxWidth(), isDay = isDay) {
        Text(text = "Radar", color = Color.White, fontWeight = FontWeight.Bold)

        RadarWebView(
            url = url,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
    }
}

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

    AndroidView(
        modifier = modifier
            .background(Color.Transparent)
            .padding(bottom = 8.dp),
        factory = {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = bannerAdUnitId
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}

@Composable
private fun GlassHeaderBar(
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(0.dp, 0.dp, 24.dp, 24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .shadow(
                elevation = 16.dp,
                shape = shape,
                spotColor = Color(0xFF8338EC).copy(alpha = 0.3f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.08f),
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // App icon/logo placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF8338EC),
                                    Color(0xFF3A86FF),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "☁",
                        fontSize = 20.sp,
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = "Khawchin Thlirna",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp,
                )
            }

            // Report button with gradient
            PremiumPressable(
                onClick = onReport,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF06D6A0).copy(alpha = 0.8f),
                                Color(0xFF00B894).copy(alpha = 0.8f),
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.3f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .semantics { contentDescription = "Report weather" },
            ) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Report",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
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
            "⚠️ Khawvel Ralveng: Chak lutuk",
            Color(0xFFFF1744),
            Brush.horizontalGradient(listOf(Color(0xFFFF1744), Color(0xFFD50000)))
        )
        "ORANGE" -> Triple(
            "⚠️ Khawvel Ralveng: Fimkhur tur",
            Color(0xFFFF6D00),
            Brush.horizontalGradient(listOf(Color(0xFFFF6D00), Color(0xFFFF3D00)))
        )
        "YELLOW" -> Triple(
            "🌤️ Khawvel Ralveng: Thlawh sen lo",
            Color(0xFFFFD600),
            Brush.horizontalGradient(listOf(Color(0xFFFFD600), Color(0xFFFFC400)))
        )
        "GREEN" -> Triple(
            "✅ Khawvel Ralveng: A that",
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
                    "I thil a thleng dan report rawh. GPS accuracy a awm chuan report hi a rinawm zawk ang.",
                )

                if (!locationAvailable) {
                    Text(
                        "GPS a off a nih chuan report submit a theih lo.",
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
