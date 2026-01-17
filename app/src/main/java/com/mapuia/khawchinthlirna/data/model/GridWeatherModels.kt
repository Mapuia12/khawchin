package com.mapuia.khawchinthlirna.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlin.math.roundToInt

/**
 * Represents a single grid point's weather data from Firebase
 * Collection: weather_v69_grid
 * 
 * This model maps EXACTLY to the new backend format with blended/ensemble data.
 */
@IgnoreExtraProperties
data class GridWeatherDocument(
    @get:PropertyName("grid_id")
    @set:PropertyName("grid_id")
    var gridId: String = "",
    
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    
    val generated: String = "",
    
    val hourly: HourlyData = HourlyData(),
    val meta: GridMetaData = GridMetaData(),
    val marine: MarineRiskData = MarineRiskData(),
    
    @get:PropertyName("models_used")
    @set:PropertyName("models_used")
    var modelsUsed: List<String> = emptyList()
) {
    /**
     * Check if document has valid data
     */
    fun isValid(): Boolean {
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return false
        if (hourly.time.isEmpty() || hourly.temperatureC.isEmpty()) return false
        return true
    }
    
    /**
     * Get hourly weather list for UI display
     */
    fun toHourlyWeatherList(): List<HourlyWeatherItem> {
        return hourly.toHourlyWeatherList()
    }
    
    /**
     * Get current weather (first hour)
     */
    fun getCurrentHour(): HourlyWeatherItem? {
        return toHourlyWeatherList().firstOrNull()
    }
}

/**
 * Hourly weather arrays - ALL fields from Firebase weather_v69_grid
 */
@IgnoreExtraProperties
data class HourlyData(
    val time: List<String> = emptyList(),
    
    @get:PropertyName("precipitation_mm")
    @set:PropertyName("precipitation_mm")
    var precipitationMm: List<Double?> = emptyList(),
    
    @get:PropertyName("precipitation_probability")
    @set:PropertyName("precipitation_probability")
    var precipitationProbability: List<Double?> = emptyList(),
    
    @get:PropertyName("temperature_c")
    @set:PropertyName("temperature_c")
    var temperatureC: List<Double?> = emptyList(),
    
    @get:PropertyName("apparent_temperature_c")
    @set:PropertyName("apparent_temperature_c")
    var apparentTemperatureC: List<Double?> = emptyList(),
    
    @get:PropertyName("wind_speed_kmh")
    @set:PropertyName("wind_speed_kmh")
    var windSpeedKmh: List<Double?> = emptyList(),
    
    @get:PropertyName("wind_gust_kmh")
    @set:PropertyName("wind_gust_kmh")
    var windGustKmh: List<Double?> = emptyList(),
    
    @get:PropertyName("relative_humidity")
    @set:PropertyName("relative_humidity")
    var relativeHumidity: List<Double?> = emptyList(),
    
    @get:PropertyName("pressure_hpa")
    @set:PropertyName("pressure_hpa")
    var pressureHpa: List<Double?> = emptyList(),
    
    @get:PropertyName("cloud_cover_percent")
    @set:PropertyName("cloud_cover_percent")
    var cloudCoverPercent: List<Double?> = emptyList(),
    
    @get:PropertyName("visibility_m")
    @set:PropertyName("visibility_m")
    var visibilityM: List<Double?> = emptyList(),
    
    @get:PropertyName("uv_index")
    @set:PropertyName("uv_index")
    var uvIndex: List<Double?> = emptyList(),
    
    @get:PropertyName("dewpoint_c")
    @set:PropertyName("dewpoint_c")
    var dewpointC: List<Double?> = emptyList()
) {
    /**
     * Convert arrays to list of HourlyWeatherItem for UI
     */
    fun toHourlyWeatherList(): List<HourlyWeatherItem> {
        return time.mapIndexed { index, timeStr ->
            HourlyWeatherItem(
                time = timeStr,
                temperature = temperatureC.getOrNull(index) ?: 0.0,
                feelsLike = apparentTemperatureC.getOrNull(index),
                precipitation = precipitationMm.getOrNull(index) ?: 0.0,
                precipitationProbability = precipitationProbability.getOrNull(index)?.toInt() ?: 0,
                windSpeed = windSpeedKmh.getOrNull(index) ?: 0.0,
                windGust = windGustKmh.getOrNull(index),
                humidity = relativeHumidity.getOrNull(index)?.toInt() ?: 0,
                pressure = pressureHpa.getOrNull(index),
                cloudCover = cloudCoverPercent.getOrNull(index)?.toInt(),
                visibility = visibilityM.getOrNull(index)?.toInt(),
                uvIndex = uvIndex.getOrNull(index),
                dewpoint = dewpointC.getOrNull(index)
            )
        }
    }
}

/**
 * Metadata about the grid point
 */
@IgnoreExtraProperties
data class GridMetaData(
    @get:PropertyName("elevation_m")
    @set:PropertyName("elevation_m")
    var elevationM: Double = 0.0,
    
    @get:PropertyName("bias_factor")
    @set:PropertyName("bias_factor")
    var biasFactor: Double = 1.0,
    
    @get:PropertyName("orographic_factor")
    @set:PropertyName("orographic_factor")
    var orographicFactor: Double = 1.0
)

/**
 * Marine/water body risk assessment
 */
@IgnoreExtraProperties
data class MarineRiskData(
    val score: Double = 0.0,
    val level: String = "GREEN" // GREEN, YELLOW, ORANGE, RED
) {
    fun isSignificant(): Boolean = level != "GREEN"
    
    fun getDisplayColor(): Long {
        return when (level) {
            "GREEN" -> 0xFF06D6A0
            "YELLOW" -> 0xFFFFD166
            "ORANGE" -> 0xFFFF9F1C
            "RED" -> 0xFFFF3D00
            else -> 0xFF06D6A0
        }
    }
}

/**
 * Single hour weather data (for UI display)
 */
data class HourlyWeatherItem(
    val time: String,
    val temperature: Double,
    val feelsLike: Double?,
    val precipitation: Double,
    val precipitationProbability: Int,
    val windSpeed: Double,
    val windGust: Double?,
    val humidity: Int,
    val pressure: Double?,
    val cloudCover: Int?,
    val visibility: Int?,
    val uvIndex: Double?,
    val dewpoint: Double?
) {
    /**
     * Format time for display (e.g., "2026-01-14T13:00" -> "1 PM")
     */
    fun formatHour(): String {
        return try {
            val hour = time.substringAfter("T").substringBefore(":").toInt()
            when {
                hour == 0 -> "12 AM"
                hour < 12 -> "$hour AM"
                hour == 12 -> "12 PM"
                else -> "${hour - 12} PM"
            }
        } catch (e: Exception) {
            time
        }
    }
    
    /**
     * Get UV color based on index
     */
    fun getUvColor(): Long {
        val uv = uvIndex ?: return 0xFF4CAF50
        return when {
            uv <= 2 -> 0xFF4CAF50  // Green - Low
            uv <= 5 -> 0xFFFFC107  // Yellow - Moderate
            uv <= 7 -> 0xFFFF9800  // Orange - High
            uv <= 10 -> 0xFFF44336 // Red - Very High
            else -> 0xFF9C27B0     // Purple - Extreme
        }
    }
    
    /**
     * Get UV level description in Mizo
     */
    fun getUvLevelMizo(): String {
        val uv = uvIndex ?: return "A hniam"
        return when {
            uv <= 2 -> "A hniam (Low)"
            uv <= 5 -> "Moderate"
            uv <= 7 -> "A sang (High)"
            uv <= 10 -> "A sang tak (Very High)"
            else -> "Extreme"
        }
    }
    
    /**
     * Format visibility for display
     */
    fun formatVisibility(): String {
        val vis = visibility ?: return "--"
        return when {
            vis >= 10000 -> "10+ km"
            vis >= 1000 -> "${vis / 1000} km"
            else -> "$vis m"
        }
    }
    
    /**
     * Get weather condition description based on available data
     */
    fun getConditionDescription(): String {
        val rainMm = precipitation
        val cloud = cloudCover ?: 0
        
        return when {
            rainMm > 50 -> "Ruah vanduai"
            rainMm > 25 -> "Ruah nasa tak"
            rainMm > 10 -> "Ruah nasa"
            rainMm > 2.5 -> "Ruah zau"
            rainMm > 0.5 -> "Ruah nuam"
            rainMm > 0 -> "Ruah fa"
            cloud >= 80 -> "Van a dum vek"
            cloud >= 50 -> "Van a dum zau"
            cloud >= 20 -> "Sum leh van"
            else -> "Van a eng"
        }
    }
}

/**
 * Extension to format timestamp
 */
fun formatTimestamp(iso: String): String {
    return try {
        val date = iso.substringBefore("T")
        val time = iso.substringAfter("T").substringBefore(".")
        "$date $time"
    } catch (e: Exception) {
        iso
    }
}
