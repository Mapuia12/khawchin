package com.mapuia.khawchinthlirna.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Strict mapping model for Firestore document in collection `weather_v69_grid`.
 *
 * IMPORTANT:
 * - Keep field names and types aligned with Firestore to avoid runtime mapping crashes.
 * - Supports both old format (current/hourly) and new backend format (blended/times/ensemble)
 */
data class WeatherDoc(
    val lat: Double = 0.0,
    val lon: Double = 0.0,

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Any? = null, // Timestamp or String

    // Core Weather Blocks (old format)
    val current: CurrentWeather? = null,
    val hourly: HourlyArrays? = null,
    val daily: DailyArrays? = null,

    // New backend format (blended ensemble)
    val times: List<String>? = null,
    val blended: BlendedData? = null,
    val ensemble: EnsembleData? = null,
    val bias: BiasData? = null,

    @get:PropertyName("radar_frames_available")
    @set:PropertyName("radar_frames_available")
    var radarFramesAvailable: Boolean? = null,

    /** Optional backend alert map (different from marine_*). */
    val alert: AlertBlock? = null,

    // Intelligence & Alerts
    @get:PropertyName("marine_alert")
    @set:PropertyName("marine_alert")
    var marineAlert: String = "GREEN", // RED, ORANGE, YELLOW, GREEN

    @get:PropertyName("marine_score")
    @set:PropertyName("marine_score")
    var marineScore: Double = 0.0,

    @get:PropertyName("marine_evidence")
    @set:PropertyName("marine_evidence")
    var marineEvidence: MarineEvidence? = null,

    @get:PropertyName("marine_upstream_rain")
    @set:PropertyName("marine_upstream_rain")
    var marineUpstreamRain: UpstreamRainAlert? = null,

    // Meta (Radar URL)
    val meta: MetaData? = null,

    // Fallback info
    @get:PropertyName("fallback_from")
    @set:PropertyName("fallback_from")
    var fallbackFrom: String? = null,

    @get:PropertyName("fallback_type")
    @set:PropertyName("fallback_type")
    var fallbackType: String? = null,

    // Optional: seasonal forecast / outlook (top-level docs or nested docs depending on backend)
    @get:PropertyName("seasonal_outlook")
    @set:PropertyName("seasonal_outlook")
    var seasonalOutlook: SeasonalOutlook? = null,

    @get:PropertyName("seasonal_outlook_monthly")
    @set:PropertyName("seasonal_outlook_monthly")
    var seasonalOutlookMonthly: SeasonalOutlookMonthly? = null,
) {
    /**
     * Check if document has valid data (either old or new format)
     */
    fun isValid(): Boolean {
        // Valid coordinates check
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return false

        // Old format: has current block with valid temp
        if (current != null && current.temp > -100.0 && current.temp < 100.0) {
            return true
        }

        // New format: has blended data with temperature
        if (blended != null && !blended.temperature2m.isNullOrEmpty()) {
            val firstTemp = blended.temperature2m?.firstOrNull() ?: return false
            return firstTemp > -100.0 && firstTemp < 100.0
        }

        return false
    }

    /**
     * Get current weather - works with both old and new format
     */
    fun getCurrentWeather(): CurrentWeather? {
        // If old format exists, use it
        if (current != null) return current

        // Build from new format
        val temps = blended?.temperature2m ?: return null
        val precips = blended?.precipitation ?: emptyList()

        if (temps.isEmpty()) return null

        return CurrentWeather(
            temp = temps.firstOrNull() ?: 0.0,
            feelsLike = temps.firstOrNull() ?: 0.0, // Use same as temp if no feels_like
            rainMm = precips.firstOrNull() ?: 0.0,
            humidity = 0, // Not available in new format
            wind = 0.0, // Not available in new format
            isDay = 1,
            weatherCode = 0,
            vis = 0.0,
        )
    }

    /**
     * Get hourly forecast - works with both old and new format
     */
    fun getHourlyForecast(): HourlyArrays? {
        // If old format exists, use it
        if (hourly != null) return hourly

        // Build from new format
        val timeList = times ?: return null
        val temps = blended?.temperature2m ?: return null
        val precips = blended?.precipitation ?: emptyList()

        if (timeList.isEmpty() || temps.isEmpty()) return null

        return HourlyArrays(
            time = timeList,
            temp = temps,
            rainMm = precips,
            wind = emptyList(),
            windDir = emptyList(),
            weatherCode = emptyList(),
        )
    }

    fun getSafeHourlyCount(): Int {
        // Try old format first
        val oldHourly = this.hourly
        if (oldHourly != null) {
            return listOf(
                oldHourly.time.size,
                oldHourly.temp.size,
                oldHourly.weatherCode.size,
                oldHourly.rainMm.size,
                oldHourly.wind.size
            ).minOrNull() ?: 0
        }

        // Try new format
        val timeList = times ?: return 0
        val temps = blended?.temperature2m ?: return 0
        return minOf(timeList.size, temps.size)
    }
}

/** New backend blended data format */
data class BlendedData(
    @get:PropertyName("temperature_2m")
    @set:PropertyName("temperature_2m")
    var temperature2m: List<Double>? = null,

    val precipitation: List<Double>? = null,
)

/** New backend ensemble data format */
data class EnsembleData(
    val members: List<String>? = null,

    @get:PropertyName("precip_spread")
    @set:PropertyName("precip_spread")
    var precipSpread: List<Double>? = null,

    @get:PropertyName("temp_spread")
    @set:PropertyName("temp_spread")
    var tempSpread: List<Double>? = null,

    val probabilities: Map<String, List<Double>>? = null,
)

/** New backend bias data format */
data class BiasData(
    @get:PropertyName("rain_bias")
    @set:PropertyName("rain_bias")
    var rainBias: Double? = null,
)

data class CurrentWeather(
    val temp: Double = 0.0,

    @get:PropertyName("feels_like")
    @set:PropertyName("feels_like")
    var feelsLike: Double = 0.0,

    @get:PropertyName("rain_mm")
    @set:PropertyName("rain_mm")
    var rainMm: Double = 0.0,

    // Per your contract (int). Firestore typically stores numbers without decimals.
    val humidity: Int = 0,

    // In your schema this is km/h already.
    val wind: Double = 0.0,

    /** Some docs provide wind direction in degrees. */
    @get:PropertyName("wind_dir")
    @set:PropertyName("wind_dir")
    var windDir: Int? = null,

    /** Some docs provide wind direction as degrees under a different key. */
    @get:PropertyName("wind_direction")
    @set:PropertyName("wind_direction")
    var windDirection: Int? = null,

    @get:PropertyName("is_day")
    @set:PropertyName("is_day")
    var isDay: Int = 1,

    // WMO Code (0-99). Backend uses `code`.
    @get:PropertyName("code")
    @set:PropertyName("code")
    var weatherCode: Int = 0,

    /** Alias: some pipelines write `visibility` while UI expects `vis`. */
    @get:PropertyName("visibility")
    @set:PropertyName("visibility")
    var vis: Double = 0.0,
)

/** Optional backend `alert` map. */
data class AlertBlock(
    val level: String = "GREEN",
    val reasons: List<String> = emptyList(),
    val score: Double = 0.0,
)

/** Arrays-based hourly forecast. */
data class HourlyArrays(
    val time: List<String> = emptyList(),
    val temp: List<Double> = emptyList(),

    @get:PropertyName("rain_mm")
    @set:PropertyName("rain_mm")
    var rainMm: List<Double> = emptyList(),

    val wind: List<Double> = emptyList(),

    /** Optional wind direction per hour if backend provides it. */
    @get:PropertyName("wind_dir")
    @set:PropertyName("wind_dir")
    var windDir: List<Int> = emptyList(),

    @get:PropertyName("weather_code")
    @set:PropertyName("weather_code")
    var weatherCode: List<Int> = emptyList(),
)

data class DailyArrays(
    val time: List<String> = emptyList(),

    @get:PropertyName("temp_max")
    @set:PropertyName("temp_max")
    var tempMax: List<Double> = emptyList(),

    @get:PropertyName("temp_min")
    @set:PropertyName("temp_min")
    var tempMin: List<Double> = emptyList(),

    val sunrise: List<String> = emptyList(),
    val sunset: List<String> = emptyList(),

    @get:PropertyName("rain_prob")
    @set:PropertyName("rain_prob")
    var rainProb: List<Int> = emptyList(),
)

data class MarineEvidence(
    val season: String? = "NEUTRAL", // "MONSOON", "PRE_POST", "NEUTRAL"

    @get:PropertyName("local_pressure_hpa")
    @set:PropertyName("local_pressure_hpa")
    var pressure: Double? = null,
)

data class UpstreamRainAlert(
    val level: String = "NONE", // "HIGH", "MODERATE"
    val reason: String = "", // Mizo text reason
)

data class MetaData(
    @get:PropertyName("radar_url")
    @set:PropertyName("radar_url")
    var radarUrl: String? = null, // Website URL
)

// --- Seasonal forecast models (best-effort mapping; optional in Firestore) ---

data class SeasonalOutlook(
    /** Backend may provide a headline/summary string. */
    val text: String? = null,
    val level: String? = null,
)

data class SeasonalOutlookMonthly(
    /** Backend may provide month-wise strings. Keep generic to avoid mapping crashes. */
    val text: String? = null,
    val months: List<String> = emptyList(),
)
