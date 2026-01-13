package com.mapuia.khawchinthlirna

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mapuia.khawchinthlirna.data.LocationProvider
import com.mapuia.khawchinthlirna.data.MIZORAM_GRID_POINTS
import com.mapuia.khawchinthlirna.data.ReverseGeocoder
import com.mapuia.khawchinthlirna.data.WeatherRepository
import com.mapuia.khawchinthlirna.data.nearestGridPointId
import com.mapuia.khawchinthlirna.data.model.WeatherDoc
import com.mapuia.khawchinthlirna.data.WeatherConstants
import com.mapuia.khawchinthlirna.data.LoadingState
import com.mapuia.khawchinthlirna.data.isValidGridId
import com.mapuia.khawchinthlirna.data.sanitizeInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
                    val nearest = nearestGridPointId(loc.latitude, loc.longitude, MIZORAM_GRID_POINTS)
                    nearest ?: DEFAULT_GRID_ID
                } else {
                    DEFAULT_GRID_ID
                }

                // Resolve human-friendly place name (best-effort). Keep old value if resolution fails.
                val placeName = if (loc != null && uiState.value.locationPermissionState == LocationPermissionState.GRANTED) {
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

                var doc = repository.getWeatherByGridId(resolvedGridId)

                // If still no doc and we didn't already try default, try it as last resort
                if (doc == null && resolvedGridId != DEFAULT_GRID_ID) {
                    doc = repository.getWeatherByGridId(DEFAULT_GRID_ID)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        weather = doc,
                        errorMessage = if (doc == null) {
                            "Khawchin data a awm lo ($resolvedGridId). Internet i check ang u."
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

                // Map the UI options to a 1..5 severity scale for backend clustering.
                val severity = when (optionMizo) {
                    "Ruah a sur" -> 4.0
                    "Thli a na" -> 4.0
                    "Khua a tha" -> 2.0
                    else -> 3.0
                }

                repository.submitCrowdReport(
                    optionMizo = optionMizo,
                    gridId = gridId,
                    userLat = resolvedLat,
                    userLon = resolvedLon,
                    accuracyMeters = accuracy,
                    severity = severity,
                    hasPhoto = false,
                    photoPath = null,
                )

                onDone(true, null)
            } catch (t: Throwable) {
                onDone(false, t.message)
            }
        }
    }
}
