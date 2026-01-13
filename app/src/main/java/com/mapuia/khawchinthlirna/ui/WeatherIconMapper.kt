package com.mapuia.khawchinthlirna.ui

/**
 * Maps WMO weather codes to Basmilius SVG asset file URIs.
 *
 * Your SVGs live in: app/src/main/res/assets
 * Android runtime path for assets is: file:///android_asset/<filename>
 */
fun getWeatherIcon(code: Int, isDay: Boolean = true): String {
    val file = when (code) {
        0 -> if (isDay) "clear-day.svg" else "clear-night.svg"
        1, 2 -> if (isDay) "partly-cloudy-day.svg" else "partly-cloudy-night.svg"
        3 -> "cloudy.svg"
        45, 48 -> "fog.svg"
        in 51..57 -> "drizzle.svg"
        in 61..67 -> "rain.svg"
        in 71..77 -> "snow.svg"
        80, 81, 82 -> "rain.svg"
        85, 86 -> "snow.svg"
        95, 96, 99 -> "thunderstorms.svg"
        else -> "not-available.svg"
    }
    return "file:///android_asset/$file"
}

fun seasonLabelMizo(season: String?): String {
    return when (season?.uppercase()) {
        "MONSOON" -> "Fur Lai"
        "PRE_POST" -> "Thlipui Hun"
        else -> "Hun Pangngai"
    }
}
