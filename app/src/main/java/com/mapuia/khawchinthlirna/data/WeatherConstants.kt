package com.mapuia.khawchinthlirna.data

object WeatherConstants {
    // Default grid ID: Aizawl area (2 decimal format to match Firebase/backend)
    // Backend uses 2 decimal places: "23.73_92.72"
    const val DEFAULT_GRID_ID = "22.00_92.15"
    const val MAX_HOURLY_ITEMS = 24
    const val CACHE_EXPIRY_MINUTES = 30L
    const val MAX_RETRY_ATTEMPTS = 3
    const val LOCATION_TIMEOUT_MS = 10000L
    const val DEFAULT_ACCURACY_METERS = 150.0
    
    // Firestore collections (must match security rules)
    const val WEATHER_COLLECTION = "weather_v69_grid"
    const val REPORTS_COLLECTION = "crowd_reports"  // Match Firestore rules: match /crowd_reports/{reportId}
    
    // Grid validation pattern: 2 decimal places (e.g., "23.73_92.72")
    val GRID_ID_PATTERN = Regex("^\\d{2}\\.\\d{2}_\\d{2}\\.\\d{2}$")
}

fun String.isValidGridId(): Boolean = WeatherConstants.GRID_ID_PATTERN.matches(this)

fun String.sanitizeInput(): String = this.trim().take(100)