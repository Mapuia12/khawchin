package com.mapuia.khawchinthlirna.data

object WeatherConstants {
    // Default grid ID: Aizawl area (snapped to 0.20 step grid)
    // Backend uses 0.20 step from 22.30-24.50 lat, 92.40-94.40 lon
    const val DEFAULT_GRID_ID = "23.70_92.80"
    const val MAX_HOURLY_ITEMS = 24
    const val CACHE_EXPIRY_MINUTES = 30L
    const val MAX_RETRY_ATTEMPTS = 3
    const val LOCATION_TIMEOUT_MS = 10000L
    const val DEFAULT_ACCURACY_METERS = 150.0
    
    // Firestore collections
    const val WEATHER_COLLECTION = "weather_v69_grid"
    const val REPORTS_COLLECTION = "crowd_reports"
    
    // Grid validation pattern (supports both 0.10 and 0.20 step grids)
    val GRID_ID_PATTERN = Regex("^\\d{2}\\.\\d{2}_\\d{2}\\.\\d{2}$")
}

fun String.isValidGridId(): Boolean = WeatherConstants.GRID_ID_PATTERN.matches(this)

fun String.sanitizeInput(): String = this.trim().take(100)