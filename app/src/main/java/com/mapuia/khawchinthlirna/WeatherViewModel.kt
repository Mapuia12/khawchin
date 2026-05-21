package com.mapuia.khawchinthlirna

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mapuia.khawchinthlirna.data.LocationProvider
import com.mapuia.khawchinthlirna.data.ReverseGeocoder
import com.mapuia.khawchinthlirna.data.WeatherRepository
import com.mapuia.khawchinthlirna.data.model.SkillReport
import com.mapuia.khawchinthlirna.data.model.WeatherDoc
import com.mapuia.khawchinthlirna.data.model.ImergDoc
import com.mapuia.khawchinthlirna.data.model.ForecastSnapshot
import com.mapuia.khawchinthlirna.data.WeatherConstants
import com.mapuia.khawchinthlirna.data.LoadingState
import com.mapuia.khawchinthlirna.data.preferences.PreferencesManager
import com.mapuia.khawchinthlirna.data.preferences.SelectedLocationMode
import com.mapuia.khawchinthlirna.util.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.*

private const val DEFAULT_GRID_ID = WeatherConstants.DEFAULT_GRID_ID

enum class LocationPermissionState { UNKNOWN, GRANTED, DENIED }

data class WeatherUiState(
    val weatherLoadingState: LoadingState = LoadingState.Idle,
    val locationLoadingState: LoadingState = LoadingState.Idle,
    val reportSubmissionState: LoadingState = LoadingState.Idle,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val gridId: String? = null,
    val userLat: Double? = null,
    val userLon: Double? = null,
    val userPlaceName: String? = null,
    val weather: WeatherDoc? = null,
    val imerg: ImergDoc? = null,
    val forecastSnapshot: ForecastSnapshot? = null,
    val skillReport: SkillReport? = null,
    val locationPermissionState: LocationPermissionState = LocationPermissionState.UNKNOWN,
    val selectedLocationMode: SelectedLocationMode = SelectedLocationMode.CURRENT,
    val selectedLocationName: String? = null,
)

class WeatherViewModel(
    app: Application,
    private val repository: WeatherRepository,
    private val locationProvider: LocationProvider,
    private val reverseGeocoder: ReverseGeocoder,
    private val preferencesManager: PreferencesManager,
) : AndroidViewModel(app) {

    private val nonInteractiveRefreshIntervalMs = 10 * 60 * 1000L
    private var lastSuccessfulRefreshMs: Long = 0L

    private val _uiState = MutableStateFlow(
        WeatherUiState(isLoading = true, gridId = DEFAULT_GRID_ID)
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.selectedLocationFlow.collect { selected ->
                _uiState.update {
                    it.copy(
                        selectedLocationMode = selected.mode,
                        selectedLocationName = selected.gridName,
                    )
                }
            }
        }
    }

    fun onLocationPermissionGranted() {
        _uiState.update { it.copy(locationPermissionState = LocationPermissionState.GRANTED) }
        refresh(isUserInitiated = false)
    }

    fun onLocationPermissionDenied() {
        _uiState.update { it.copy(locationPermissionState = LocationPermissionState.DENIED) }
        // Still okay: app will use DEFAULT_GRID_ID and cached fallback.
        refresh(isUserInitiated = false)
    }

    /** Manual refresh trigger (pull-to-refresh). */
    fun refresh(isUserInitiated: Boolean = true) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (!isUserInitiated && lastSuccessfulRefreshMs > 0L) {
                val elapsed = now - lastSuccessfulRefreshMs
                if (elapsed < nonInteractiveRefreshIntervalMs) {
                    AppLog.d("WeatherVM", "Skipping non-user refresh; elapsed=${elapsed}ms")
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = it.weather == null && !isUserInitiated,
                    isRefreshing = isUserInitiated,
                    errorMessage = null,
                )
            }

            try {
                val selectedLocation = preferencesManager.selectedLocationFlow.first()
                val useManualLocation =
                    selectedLocation.mode == SelectedLocationMode.MANUAL &&
                        selectedLocation.lat != null &&
                        selectedLocation.lng != null

                val loc = if (useManualLocation) null else safeGetLocationOrNull()

                val resolvedGridId = when {
                    useManualLocation && !selectedLocation.gridId.isNullOrBlank() -> {
                        selectedLocation.gridId
                    }
                    useManualLocation -> {
                        val roundedLat = (selectedLocation.lat!! * 100).roundToInt() / 100.0
                        val roundedLon = (selectedLocation.lng!! * 100).roundToInt() / 100.0
                        String.format(java.util.Locale.US, "%.2f_%.2f", roundedLat, roundedLon)
                    }
                    loc != null && uiState.value.locationPermissionState == LocationPermissionState.GRANTED -> {
                        val roundedLat = (loc.latitude * 100).roundToInt() / 100.0
                        val roundedLon = (loc.longitude * 100).roundToInt() / 100.0
                        val gridId = String.format(java.util.Locale.US, "%.2f_%.2f", roundedLat, roundedLon)
                        AppLog.d("WeatherVM", "Location: ${loc.latitude}, ${loc.longitude}")
                        AppLog.d("WeatherVM", "Generated grid ID: $gridId")
                        gridId
                    }
                    else -> {
                        AppLog.d("WeatherVM", "Using DEFAULT_GRID_ID: $DEFAULT_GRID_ID")
                        DEFAULT_GRID_ID
                    }
                }
                AppLog.d("WeatherVM", "Resolved grid ID: $resolvedGridId")

                val resolvedLat = when {
                    useManualLocation -> selectedLocation.lat
                    else -> loc?.latitude
                }
                val resolvedLon = when {
                    useManualLocation -> selectedLocation.lng
                    else -> loc?.longitude
                }

                val placeName = when {
                    useManualLocation -> selectedLocation.gridName
                    loc != null && uiState.value.locationPermissionState == LocationPermissionState.GRANTED -> {
                        reverseGeocoder.getPlaceName(loc.latitude, loc.longitude)
                    }
                    else -> null
                }

                if (loc != null && uiState.value.locationPermissionState == LocationPermissionState.GRANTED) {
                    runCatching {
                        preferencesManager.setLastLocation(loc.latitude, loc.longitude)
                    }
                }

                _uiState.update {
                    it.copy(
                        gridId = resolvedGridId,
                        userLat = resolvedLat,
                        userLon = resolvedLon,
                        userPlaceName = placeName ?: it.userPlaceName,
                    )
                }

                // Repository already has robust fallback - finds nearest available grid within ~55km
                var doc = repository.getWeatherByGridId(resolvedGridId)
                AppLog.d("WeatherVM", "Primary fetch for $resolvedGridId returned: ${if (doc != null) "found ${doc.gridId}" else "null"}")

                // FINAL FALLBACK: If repository couldn't find any nearby data,
                // try DEFAULT_GRID_ID as absolute last resort (better than no data)
                if (doc == null && resolvedGridId != DEFAULT_GRID_ID) {
                    AppLog.d("WeatherVM", "Trying DEFAULT_GRID_ID as final fallback: $DEFAULT_GRID_ID")
                    doc = repository.getWeatherByGridId(DEFAULT_GRID_ID)
                    if (doc != null) {
                        AppLog.d("WeatherVM", "Final fallback succeeded with ${doc.gridId}")
                    }
                }

                val skillReport = repository.getLatestSkillReport()
                AppLog.d(
                    "WeatherVM",
                    "Skill report fetched: ${skillReport != null} sample=${skillReport?.sampleCount} perModelMae=${skillReport?.perModelMae?.size} perModelCount=${skillReport?.perModelCount?.size}"
                )
                AppLog.d("WeatherVM", "Skill report fetched: ${skillReport != null}, sampleCount=${skillReport?.sampleCount}, perModelMae=${skillReport?.perModelMae?.keys}")

                // Satellite IMERG + forecast snapshot (same grid as the weather doc)
                val dataGridId = (doc?.gridId ?: resolvedGridId).takeIf { it.isNotBlank() }
                val imerg = if (doc != null && dataGridId != null) repository.getImergByGridId(dataGridId) else null
                val snapshot = if (doc != null && dataGridId != null) repository.getForecastSnapshotByGridId(dataGridId) else null

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        weather = doc,
                        imerg = imerg,
                        forecastSnapshot = snapshot,
                        skillReport = skillReport,
                        errorMessage = if (doc == null) {
                            "Dik lo a awm tlat, khawchin data a awm lo ($resolvedGridId). Internet i check ang u."
                        } else null,
                    )
                }
                lastSuccessfulRefreshMs = System.currentTimeMillis()
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = t.message ?: "Unknown error",
                    )
                }
            }
        }
    }

    private suspend fun safeGetLocationOrNull(): Location? {
        return runCatching { locationProvider.getLastBestLocation() }.getOrNull()
    }

    fun submitCrowdReport(
        optionMizo: String,
        onDone: (success: Boolean, message: String?) -> Unit = { _, _ -> },
    ) {
        val gridId = uiState.value.gridId
        val lat = uiState.value.userLat
        val lon = uiState.value.userLon

        viewModelScope.launch {
            try {
                // Use a fresh location read to grab accuracy too.
                val loc = safeGetLocationOrNull()
                val accuracy = loc?.accuracy?.toDouble() ?: 150.0

                // Crowd reports must come from real GPS, not from a manually switched forecast view.
                val resolvedLat = loc?.latitude
                val resolvedLon = loc?.longitude
                if (resolvedLat == null || resolvedLon == null) {
                    onDone(false, "Missing GPS location")
                    return@launch
                }

                // Map quick-report options to structured fields for better backend use.
                val severity: Int
                val rainIntensity: Int?
                val windStrength: Int?
                val skyCondition: Int?
                when (optionMizo) {
                    "Ruah a sur" -> {
                        severity = 4
                        rainIntensity = 4
                        windStrength = null
                        skyCondition = null
                    }
                    "Thli a na" -> {
                        severity = 4
                        rainIntensity = 0
                        windStrength = 4
                        skyCondition = null
                    }
                    "Khua a tha" -> {
                        severity = 1
                        rainIntensity = 0
                        windStrength = 0
                        skyCondition = 0
                    }
                    else -> {
                        severity = 3
                        rainIntensity = 1
                        windStrength = null
                        skyCondition = null
                    }
                }

                repository.submitCrowdReport(
                    optionMizo = optionMizo,
                    gridId = gridId,
                    userLat = resolvedLat,
                    userLon = resolvedLon,
                    accuracyMeters = accuracy,
                    severity = severity,
                    rainIntensity = rainIntensity,
                    windStrength = windStrength,
                    skyCondition = skyCondition,
                    reportSource = "quick_dialog",
                )

                onDone(true, null)
            } catch (t: Throwable) {
                onDone(false, t.message)
            }
        }
    }

    fun switchToCurrentLocation() {
        viewModelScope.launch {
            preferencesManager.setSelectedLocationCurrent()
            refresh(isUserInitiated = true)
        }
    }

    fun selectManualLocation(
        gridId: String,
        gridName: String,
        lat: Double,
        lon: Double,
    ) {
        viewModelScope.launch {
            preferencesManager.setSelectedManualLocation(
                gridId = gridId,
                gridName = gridName,
                lat = lat,
                lng = lon,
            )
            refresh(isUserInitiated = true)
        }
    }

    companion object {
        /**
         * Calculate distance between two coordinates using Haversine formula.
         * Returns distance in kilometers.
         */
        fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371.0 // Earth radius in km
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2.0) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2.0)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }

        /**
         * Parse grid ID like "22.00_92.15" into (lat, lon) pair.
         */
        fun parseGridId(gridId: String): Pair<Double, Double>? {
            return try {
                val parts = gridId.split("_")
                if (parts.size == 2) {
                    Pair(parts[0].toDouble(), parts[1].toDouble())
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
