package com.mapuia.khawchinthlirna

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mapuia.khawchinthlirna.data.LocationProvider
import com.mapuia.khawchinthlirna.data.ReverseGeocoder
import com.mapuia.khawchinthlirna.data.WeatherRepository
import com.mapuia.khawchinthlirna.data.model.WeatherDoc
import com.mapuia.khawchinthlirna.data.WeatherConstants
import com.mapuia.khawchinthlirna.data.LoadingState
import com.mapuia.khawchinthlirna.util.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val locationPermissionState: LocationPermissionState = LocationPermissionState.UNKNOWN,
)

class WeatherViewModel(
    app: Application,
    private val repository: WeatherRepository,
    private val locationProvider: LocationProvider,
    private val reverseGeocoder: ReverseGeocoder,
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(
        WeatherUiState(isLoading = true, gridId = DEFAULT_GRID_ID)
    )
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        refresh(isUserInitiated = false)
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
            _uiState.update {
                it.copy(
                    isLoading = it.weather == null && !isUserInitiated,
                    isRefreshing = isUserInitiated,
                    errorMessage = null,
                )
            }

            try {
                val loc = safeGetLocationOrNull()

                val resolvedGridId = if (loc != null && uiState.value.locationPermissionState == LocationPermissionState.GRANTED) {
                    // Simply round user location to 2 decimals - no hardcoded grid list needed
                    // Firebase documents are stored as "23.20_94.02" format
                    val roundedLat = (loc.latitude * 100).roundToInt() / 100.0
                    val roundedLon = (loc.longitude * 100).roundToInt() / 100.0
                    val gridId = String.format(java.util.Locale.US, "%.2f_%.2f", roundedLat, roundedLon)
                    AppLog.d("WeatherVM", "Location: ${loc.latitude}, ${loc.longitude}")
                    AppLog.d("WeatherVM", "Generated grid ID: $gridId")
                    gridId
                } else {
                    AppLog.d("WeatherVM", "Using DEFAULT_GRID_ID: $DEFAULT_GRID_ID")
                    DEFAULT_GRID_ID
                }
                AppLog.d("WeatherVM", "Resolved grid ID: $resolvedGridId")

                // Resolve human-friendly place name using DYNAMIC reverse geocoding
                // Uses user's ACTUAL GPS coordinates (not grid coordinates) for accuracy
                val placeName = if (loc != null && uiState.value.locationPermissionState == LocationPermissionState.GRANTED) {
                    // Use actual GPS coordinates for reverse geocoding - NOT the rounded grid coordinates
                    // This gives us the real location name (e.g., "Aizawl" instead of "23.20_93.96")
                    reverseGeocoder.getPlaceName(loc.latitude, loc.longitude)
                } else {
                    null
                }

                _uiState.update {
                    it.copy(
                        gridId = resolvedGridId,
                        userLat = loc?.latitude,
                        userLon = loc?.longitude,
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

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        weather = doc,
                        errorMessage = if (doc == null) {
                            "Dik lo a awm tlat, khawchin data a awm lo ($resolvedGridId). Internet i check ang u."
                        } else null,
                    )
                }
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

                // Block if we still don't have coordinates (backend clustering needs them).
                val resolvedLat = loc?.latitude ?: lat
                val resolvedLon = loc?.longitude ?: lon
                if (resolvedLat == null || resolvedLon == null) {
                    onDone(false, "Missing GPS location")
                    return@launch
                }

                // Map the UI options to a 1..5 integer severity scale for backend clustering.
                val severity = when (optionMizo) {
                    "Ruah a sur" -> 4
                    "Thli a na" -> 4
                    "Khua a tha" -> 2
                    else -> 3
                }

                repository.submitCrowdReport(
                    optionMizo = optionMizo,
                    gridId = gridId,
                    userLat = resolvedLat,
                    userLon = resolvedLon,
                    accuracyMeters = accuracy,
                    severity = severity,
                )

                onDone(true, null)
            } catch (t: Throwable) {
                onDone(false, t.message)
            }
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
