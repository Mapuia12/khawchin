package com.mapuia.khawchinthlirna.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

/**
 * Strict mapping model for Firestore document in collection `weather_v69_grid`.
 *
 * IMPORTANT:
 * - Keep field names and types aligned with Firestore to avoid runtime mapping crashes.
 * - Supports both old format (current/hourly) and new backend format (blended/times/ensemble)
 */
@IgnoreExtraProperties
data class WeatherDoc(
    val lat: Double = 0.0,
    val lon: Double = 0.0,

    // Backend v69 fields
    @get:PropertyName("grid_id")
    @set:PropertyName("grid_id")
    var gridId: String? = null,
    
    val generated: String? = null, // ISO timestamp when data was generated
    
    // Timezone info (for proper time display in Mizoram IST vs Myanmar MMT)
    val timezone: String? = null, // e.g. "Asia/Kolkata" or "Asia/Yangon"
    
    @get:PropertyName("utc_offset_seconds")
    @set:PropertyName("utc_offset_seconds")
    var utcOffsetSeconds: Long? = null, // e.g. 19800 for +5:30, 23400 for +6:30

    @get:PropertyName("updated_at")
    @set:PropertyName("updated_at")
    var updatedAt: Any? = null, // Timestamp or String
    
    // Models used for forecast (backend v69)
    @get:PropertyName("models_used")
    @set:PropertyName("models_used")
    var modelsUsed: List<String>? = null,

    // Core Weather Blocks (old format)
    val current: CurrentWeather? = null,
    val hourly: HourlyArrays? = null,
    val daily: DailyArrays? = null,
    
    // Marine data (backend v69 format)
    val marine: MarineData? = null,
    
    // Meta data (backend v69 format)
    val meta: MetaData? = null,

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
    
    // Weather systems tracking (Bay of Bengal cyclones, Western Disturbance, etc.)
    @get:PropertyName("weather_systems")
    @set:PropertyName("weather_systems")
    var weatherSystems: WeatherSystems? = null,
) {
    /**
     * Check if document has valid data (supports multiple formats)
     */
    fun isValid(): Boolean {
        // Valid coordinates check
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return false

        // Format 1: Old format with current block
        if (current != null && current.temp > -100.0 && current.temp < 100.0) {
            return true
        }

        // Format 2: New blended format
        if (blended != null && !blended.temperature2m.isNullOrEmpty()) {
            val firstTemp = blended.temperature2m?.firstOrNull() ?: return false
            return firstTemp > -100.0 && firstTemp < 100.0
        }

        // Format 3: Backend v69 format with hourly.temperature_c
        if (hourly != null && !hourly.temperatureC.isNullOrEmpty()) {
            val firstTemp = hourly.temperatureC?.firstOrNull() ?: return false
            return firstTemp > -100.0 && firstTemp < 100.0
        }

        // Format 4: Legacy hourly.temp format
        if (hourly != null && hourly.temp.isNotEmpty()) {
            val firstTemp = hourly.temp.firstOrNull() ?: return false
            return firstTemp > -100.0 && firstTemp < 100.0
        }

        return false
    }

    /**
     * Find the index for the current hour in the hourly time array.
     * Returns 0 if no matching hour is found (fallback to first hour).
     * Public so UI can use same logic as getCurrentWeather().
     */
    fun findCurrentHourIndex(timeList: List<String>): Int {
        if (timeList.isEmpty()) return 0
        
        // Get timezone from document (supports both Mizoram IST and Myanmar MMT)
        val tzName = timezone ?: "Asia/Kolkata"
        val zoneId = try { java.time.ZoneId.of(tzName) } catch (e: Exception) { java.time.ZoneId.of("Asia/Kolkata") }
        
        // Get current time in format matching backend: "2026-01-16T14:00"
        val now = java.time.ZonedDateTime.now(zoneId)
        val currentHour = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00"))
        
        // Find exact match (handle both "2026-01-16T14:00" and "2026-01-16T14:00:00" formats)
        val exactIndex = timeList.indexOfFirst { 
            it == currentHour || it.startsWith(currentHour) 
        }
        if (exactIndex >= 0) return exactIndex
        
        // Find closest hour that's not in the future
        val currentInstant = now.toInstant()
        var bestIndex = 0
        var bestDiff = Long.MAX_VALUE
        
        for (i in timeList.indices) {
            try {
                val timeStr = timeList[i]
                // Parse "2026-01-14T00:00" format (local time, use document timezone)
                val hourTime = java.time.LocalDateTime.parse(timeStr)
                    .atZone(zoneId)
                    .toInstant()
                
                val diff = currentInstant.epochSecond - hourTime.epochSecond
                // Only consider past or current hours
                if (diff >= 0 && diff < bestDiff) {
                    bestDiff = diff
                    bestIndex = i
                }
            } catch (e: Exception) {
                continue
            }
        }
        
        return bestIndex
    }
    
    /**
     * Calculate if it's currently day or night based on approximate sunrise/sunset
     */
    private fun calculateIsDay(): Int {
        val tzName = timezone ?: "Asia/Kolkata"
        val zoneId = try { java.time.ZoneId.of(tzName) } catch (e: Exception) { java.time.ZoneId.of("Asia/Kolkata") }
        val hour = java.time.ZonedDateTime.now(zoneId).hour
        // Approximate: Day is 6 AM to 6 PM
        return if (hour in 6..17) 1 else 0
    }
    
    /**
     * Get current weather - works with both old and new format.
     * Uses current hour index instead of always first hour.
     */
    fun getCurrentWeather(): CurrentWeather? {
        // If old format exists, use it
        if (current != null) return current

        // Try v69 hourly format - build current from matching hour
        val h = hourly
        if (h != null) {
            val temps = h.temperatureC ?: h.temp
            if (temps.isNotEmpty()) {
                val idx = findCurrentHourIndex(h.time)
                
                // Safe getter helper
                fun <T> List<T>.safeAt(index: Int): T? = getOrNull(index) ?: firstOrNull()
                
                return CurrentWeather(
                    temp = temps.safeAt(idx) ?: 0.0,
                    feelsLike = (h.apparentTemperatureC ?: h.apparentTemperature)?.safeAt(idx) ?: temps.safeAt(idx) ?: 0.0,
                    rainMm = (h.precipitationMm ?: h.rainMm).safeAt(idx) ?: 0.0,
                    humidity = (h.relativeHumidity?.safeAt(idx)?.toInt() ?: h.humidity?.safeAt(idx)) ?: 0,
                    wind = (h.windSpeedKmh ?: h.wind).safeAt(idx) ?: 0.0,
                    windGust = (h.windGustKmh ?: h.windGust)?.safeAt(idx),
                    windDir = h.windDirectionDeg?.safeAt(idx) ?: h.windDir.safeAt(idx),
                    isDay = calculateIsDay(),
                    weatherCode = h.weatherCode.safeAt(idx) ?: 0,
                    vis = (h.visibilityM?.safeAt(idx) ?: h.visibility?.safeAt(idx)?.toDouble()) ?: 0.0,
                    pressure = (h.pressureHpa ?: h.pressure)?.safeAt(idx),
                    cloudCover = (h.cloudCoverPercent?.safeAt(idx)?.toInt() ?: h.cloudCover?.safeAt(idx)),
                    uvIndex = h.uvIndex?.safeAt(idx),
                    dewpoint = (h.dewpointC ?: h.dewpoint)?.safeAt(idx),
                )
            }
        }

        // Build from blended format (fallback)
        val timeList = times ?: emptyList()
        val temps = blended?.temperature2m ?: return null
        val precips = blended?.precipitation ?: emptyList()

        if (temps.isEmpty()) return null
        
        val idx = findCurrentHourIndex(timeList)

        return CurrentWeather(
            temp = temps.getOrNull(idx) ?: temps.firstOrNull() ?: 0.0,
            feelsLike = temps.getOrNull(idx) ?: temps.firstOrNull() ?: 0.0,
            rainMm = precips.getOrNull(idx) ?: precips.firstOrNull() ?: 0.0,
            humidity = 0, // Not available in blended format
            wind = 0.0, // Not available in blended format
            isDay = calculateIsDay(),
            weatherCode = 0,
            vis = 0.0,
        )
    }

    /**
     * Get hourly forecast - works with both old and new format
     */
    fun getHourlyForecast(): HourlyArrays? {
        // If hourly exists (old or v69 format), return it
        if (hourly != null) {
            // For v69 format, ensure legacy fields are populated from new field names
            val h = hourly
            val temps = h.temperatureC ?: h.temp
            val rains = h.precipitationMm ?: h.rainMm
            val winds = h.windSpeedKmh ?: h.wind
            
            // If v69 fields exist but legacy fields are empty, create a merged copy
            if (h.temperatureC != null && h.temp.isEmpty()) {
                return h.copy(
                    temp = h.temperatureC ?: emptyList(),
                    rainMm = h.precipitationMm ?: h.rainMm,
                    wind = h.windSpeedKmh ?: h.wind,
                    apparentTemperature = h.apparentTemperatureC ?: h.apparentTemperature,
                    windGust = h.windGustKmh ?: h.windGust,
                    humidity = h.relativeHumidity?.map { it.toInt() } ?: h.humidity,
                    pressure = h.pressureHpa ?: h.pressure,
                    cloudCover = h.cloudCoverPercent?.map { it.toInt() } ?: h.cloudCover,
                    visibility = h.visibilityM?.map { it.toInt() } ?: h.visibility,
                    dewpoint = h.dewpointC ?: h.dewpoint,
                )
            }
            return hourly
        }

        // Build from blended format
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
        // Try hourly format (old or v69)
        val oldHourly = this.hourly
        if (oldHourly != null) {
            // Get temps from v69 or legacy field
            val temps = oldHourly.temperatureC ?: oldHourly.temp
            
            // Only require time and temps to have data (other fields are optional)
            val timeSize = oldHourly.time.size
            val tempSize = temps.size
            
            if (timeSize > 0 && tempSize > 0) {
                return minOf(timeSize, tempSize)
            }
            return 0
        }

        // Try blended format
        val timeList = times ?: return 0
        val temps = blended?.temperature2m ?: return 0
        return minOf(timeList.size, temps.size)
    }
}

/** New backend blended data format */
@IgnoreExtraProperties
data class BlendedData(
    @get:PropertyName("temperature_2m")
    @set:PropertyName("temperature_2m")
    var temperature2m: List<Double>? = null,

    val precipitation: List<Double>? = null,
)

/** New backend ensemble data format */
@IgnoreExtraProperties
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
@IgnoreExtraProperties
data class BiasData(
    @get:PropertyName("rain_bias")
    @set:PropertyName("rain_bias")
    var rainBias: Double? = null,
)

@IgnoreExtraProperties
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

    /** Wind gusts km/h */
    @get:PropertyName("wind_gust")
    @set:PropertyName("wind_gust")
    var windGust: Double? = null,

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

    // NEW: Additional current weather variables
    val pressure: Double? = null, // Pressure hPa

    @get:PropertyName("cloud_cover")
    @set:PropertyName("cloud_cover")
    var cloudCover: Int? = null, // Cloud cover %

    @get:PropertyName("uv_index")
    @set:PropertyName("uv_index")
    var uvIndex: Double? = null, // UV index

    val dewpoint: Double? = null, // Dew point °C
)

/** Optional backend `alert` map. */
@IgnoreExtraProperties
data class AlertBlock(
    val level: String = "GREEN",
    val reasons: List<String> = emptyList(),
    val score: Double = 0.0,
)

/** Arrays-based hourly forecast with additional weather variables. */
@IgnoreExtraProperties
data class HourlyArrays(
    val time: List<String> = emptyList(),
    
    // Legacy field name
    val temp: List<Double> = emptyList(),
    
    // Backend v69 field name: temperature_c
    @get:PropertyName("temperature_c")
    @set:PropertyName("temperature_c")
    var temperatureC: List<Double>? = null,

    @get:PropertyName("rain_mm")
    @set:PropertyName("rain_mm")
    var rainMm: List<Double> = emptyList(),
    
    // Backend v69 field name: precipitation_mm
    @get:PropertyName("precipitation_mm")
    @set:PropertyName("precipitation_mm")
    var precipitationMm: List<Double>? = null,

    val wind: List<Double> = emptyList(),
    
    // Backend v69 field name: wind_speed_kmh
    @get:PropertyName("wind_speed_kmh")
    @set:PropertyName("wind_speed_kmh")
    var windSpeedKmh: List<Double>? = null,

    /** Optional wind direction per hour if backend provides it. */
    @get:PropertyName("wind_dir")
    @set:PropertyName("wind_dir")
    var windDir: List<Int> = emptyList(),
    
    /** Wind direction in degrees from backend v86+ */
    @get:PropertyName("wind_direction_deg")
    @set:PropertyName("wind_direction_deg")
    var windDirectionDeg: List<Int>? = null,

    @get:PropertyName("weather_code")
    @set:PropertyName("weather_code")
    var weatherCode: List<Int> = emptyList(),

    // Backend v69 field name: apparent_temperature_c
    @get:PropertyName("apparent_temperature")
    @set:PropertyName("apparent_temperature")
    var apparentTemperature: List<Double>? = null,
    
    @get:PropertyName("apparent_temperature_c")
    @set:PropertyName("apparent_temperature_c")
    var apparentTemperatureC: List<Double>? = null,

    // Backend v69 field name: wind_gust_kmh
    @get:PropertyName("wind_gust")
    @set:PropertyName("wind_gust")
    var windGust: List<Double>? = null,
    
    @get:PropertyName("wind_gust_kmh")
    @set:PropertyName("wind_gust_kmh")
    var windGustKmh: List<Double>? = null,

    val humidity: List<Int>? = null,
    
    // Backend v69 field name: relative_humidity
    @get:PropertyName("relative_humidity")
    @set:PropertyName("relative_humidity")
    var relativeHumidity: List<Double>? = null,

    val pressure: List<Double>? = null,
    
    // Backend v69 field name: pressure_hpa
    @get:PropertyName("pressure_hpa")
    @set:PropertyName("pressure_hpa")
    var pressureHpa: List<Double>? = null,

    // Backend v69 field name: cloud_cover_percent
    @get:PropertyName("cloud_cover")
    @set:PropertyName("cloud_cover")
    var cloudCover: List<Int>? = null,
    
    @get:PropertyName("cloud_cover_percent")
    @set:PropertyName("cloud_cover_percent")
    var cloudCoverPercent: List<Double>? = null,

    val visibility: List<Int>? = null,
    
    // Backend v69 field name: visibility_m
    @get:PropertyName("visibility_m")
    @set:PropertyName("visibility_m")
    var visibilityM: List<Double>? = null,

    @get:PropertyName("uv_index")
    @set:PropertyName("uv_index")
    var uvIndex: List<Double>? = null,

    val dewpoint: List<Double>? = null,
    
    // Backend v69 field name: dewpoint_c
    @get:PropertyName("dewpoint_c")
    @set:PropertyName("dewpoint_c")
    var dewpointC: List<Double>? = null,

    @get:PropertyName("precipitation_probability")
    @set:PropertyName("precipitation_probability")
    var precipitationProbability: List<Int>? = null,
    
    // Ensemble uncertainty data (backend v86+)
    @get:PropertyName("precipitation_ensemble")
    @set:PropertyName("precipitation_ensemble")
    var precipitationEnsemble: EnsembleSpread? = null,
    
    @get:PropertyName("temperature_ensemble")
    @set:PropertyName("temperature_ensemble")
    var temperatureEnsemble: EnsembleSpread? = null,
)

/** Ensemble spread data for uncertainty quantification */
@IgnoreExtraProperties
data class EnsembleSpread(
    val p10: List<Double>? = null, // 10th percentile (optimistic)
    val p50: List<Double>? = null, // 50th percentile (median/best estimate)
    val p90: List<Double>? = null, // 90th percentile (pessimistic)
)

@IgnoreExtraProperties
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
    
    @get:PropertyName("precipitation_sum")
    @set:PropertyName("precipitation_sum")
    var precipitationSum: List<Double> = emptyList(),
    
    @get:PropertyName("weather_code")
    @set:PropertyName("weather_code")
    var weatherCode: List<Int> = emptyList(),
    
    // Forecast confidence per day (backend v86+)
    // Values: 0.95 (day 1-2), 0.85 (day 3), 0.75 (day 4), 0.60 (day 5), etc.
    // Can be List of Doubles or List of complex objects (HashMap) from backend
    var confidence: List<Any>? = null,
)

@IgnoreExtraProperties
data class MarineEvidence(
    val season: String? = "NEUTRAL", // "MONSOON", "PRE_POST", "NEUTRAL"

    @get:PropertyName("local_pressure_hpa")
    @set:PropertyName("local_pressure_hpa")
    var pressure: Double? = null,
)

@IgnoreExtraProperties
data class UpstreamRainAlert(
    val level: String = "NONE", // "HIGH", "MODERATE"
    val reason: String = "", // Mizo text reason
)

/** Marine data from backend v69 */
@IgnoreExtraProperties
data class MarineData(
    val level: String? = null, // e.g., "GREEN", "YELLOW", "ORANGE", "RED"
    val score: Double? = null,
    val reasons: List<String>? = null,
)

@IgnoreExtraProperties
data class MetaData(
    @get:PropertyName("radar_url")
    @set:PropertyName("radar_url")
    var radarUrl: String? = null, // Website URL (deprecated - no radar coverage)
    
    // Backend v69 fields
    @get:PropertyName("bias_factor")
    @set:PropertyName("bias_factor")
    var biasFactor: Double? = null,
    
    @get:PropertyName("elevation_m")
    @set:PropertyName("elevation_m")
    var elevationM: Double? = null,
    
    @get:PropertyName("orographic_factor")
    @set:PropertyName("orographic_factor")
    var orographicFactor: Double? = null,
    
    // Accuracy features (backend v86+)
    @get:PropertyName("model_weights")
    @set:PropertyName("model_weights")
    var modelWeights: Map<String, Any>? = null, // e.g. {"ecmwf": 0.75, "gfs": 0.10, "icon": 0.15}
    
    // confidence_by_day can be List of doubles or List of complex objects
    @get:PropertyName("confidence_by_day")
    @set:PropertyName("confidence_by_day")
    var confidenceByDay: List<Any>? = null, // Accept any format from backend
)

// --- Seasonal forecast models (backend v86+ enhanced format) ---

/** Climatology data for a month */
@IgnoreExtraProperties
data class ClimatologyData(
    @get:PropertyName("avg_rain_mm")
    @set:PropertyName("avg_rain_mm")
    var avgRainMm: Int = 0,
    
    @get:PropertyName("avg_temp_max")
    @set:PropertyName("avg_temp_max")
    var avgTempMax: Int = 0,
    
    @get:PropertyName("avg_temp_min")
    @set:PropertyName("avg_temp_min")
    var avgTempMin: Int = 0,
    
    @get:PropertyName("rain_days")
    @set:PropertyName("rain_days")
    var rainDays: Int = 0,
)

/** Current month outlook */
@IgnoreExtraProperties
data class MonthOutlook(
    val month: Int = 0,
    
    @get:PropertyName("month_name")
    @set:PropertyName("month_name")
    var monthName: String = "",
    
    val text: String = "",
    val level: String = "",
    val season: String? = null,
    val climatology: ClimatologyData? = null,
)

/** Upcoming season outlook */
@IgnoreExtraProperties
data class SeasonOutlook(
    val season: String = "",
    val text: String = "",
    val level: String = "",
    
    @get:PropertyName("months_away")
    @set:PropertyName("months_away")
    var monthsAway: Int = 0,
)

/** Enhanced seasonal outlook from backend v86+ */
@IgnoreExtraProperties
data class SeasonalOutlook(
    /** Backend may provide a headline/summary string (legacy). */
    val text: String? = null,
    val level: String? = null,
    
    // Enhanced format from v86+
    @get:PropertyName("current_month")
    @set:PropertyName("current_month")
    var currentMonth: MonthOutlook? = null,
    
    @get:PropertyName("next_month")
    @set:PropertyName("next_month")
    var nextMonth: MonthOutlook? = null,
    
    @get:PropertyName("upcoming_season")
    @set:PropertyName("upcoming_season")
    var upcomingSeason: SeasonOutlook? = null,
    
    val alerts: List<Map<String, Any>>? = null,
    
    @get:PropertyName("generated_at")
    @set:PropertyName("generated_at")
    var generatedAt: String? = null,
)

data class SeasonalOutlookMonthly(
    /** Backend may provide month-wise strings. Keep generic to avoid mapping crashes. */
    val text: String? = null,
    val months: List<String> = emptyList(),
)

// --- Weather Systems Models (Bay of Bengal cyclones, Western Disturbance, etc.) ---

/** Cyclone impact assessment */
@IgnoreExtraProperties
data class CycloneImpactAssessment(
    @get:PropertyName("will_impact")
    @set:PropertyName("will_impact")
    var willImpact: Boolean = false,
    
    @get:PropertyName("impact_probability")
    @set:PropertyName("impact_probability")
    var impactProbability: Int = 0,
    
    @get:PropertyName("closest_approach_km")
    @set:PropertyName("closest_approach_km")
    var closestApproachKm: Double = 0.0,
    
    @get:PropertyName("eta_hours")
    @set:PropertyName("eta_hours")
    var etaHours: Int = 0,
    
    @get:PropertyName("impact_areas")
    @set:PropertyName("impact_areas")
    var impactAreas: List<Map<String, Any>>? = null,
    
    val trajectory: List<Map<String, Any>>? = null,
)

/** Cyclone information */
@IgnoreExtraProperties
data class CycloneInfo(
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    
    @get:PropertyName("wind_speed_kmh")
    @set:PropertyName("wind_speed_kmh")
    var windSpeedKmh: Double = 0.0,
    
    @get:PropertyName("pressure_hpa")
    @set:PropertyName("pressure_hpa")
    var pressureHpa: Double = 0.0,
    
    val category: String = "",
    
    @get:PropertyName("category_short")
    @set:PropertyName("category_short")
    var categoryShort: String = "",
    
    @get:PropertyName("movement_dir")
    @set:PropertyName("movement_dir")
    var movementDir: Double = 0.0,
    
    @get:PropertyName("movement_speed_kmh")
    @set:PropertyName("movement_speed_kmh")
    var movementSpeedKmh: Double = 0.0,
    
    val timestamp: String? = null,
    
    @get:PropertyName("forecast_track")
    @set:PropertyName("forecast_track")
    var forecastTrack: List<Map<String, Any>>? = null,
    
    @get:PropertyName("impact_assessment")
    @set:PropertyName("impact_assessment")
    var impactAssessment: CycloneImpactAssessment? = null,
)

/** Bay of Bengal monitoring status */
@IgnoreExtraProperties
data class BayOfBengalStatus(
    @get:PropertyName("cyclone_active")
    @set:PropertyName("cyclone_active")
    var cycloneActive: Boolean = false,
    
    val cyclones: List<CycloneInfo> = emptyList(),
    val alerts: List<Map<String, Any>>? = null,
    
    @get:PropertyName("checked_at")
    @set:PropertyName("checked_at")
    var checkedAt: String? = null,
)

/** Western Disturbance status */
@IgnoreExtraProperties
data class WesternDisturbanceStatus(
    val active: Boolean = false,
    val approaching: Boolean = false,
    val intensity: String = "none",
    
    @get:PropertyName("rain_expected")
    @set:PropertyName("rain_expected")
    var rainExpected: Boolean = false,
    
    @get:PropertyName("eta_hours")
    @set:PropertyName("eta_hours")
    var etaHours: Int? = null,
    
    val systems: List<Map<String, Any>>? = null,
)

/** Weather systems tracking from backend */
@IgnoreExtraProperties
data class WeatherSystems(
    @get:PropertyName("active_systems")
    @set:PropertyName("active_systems")
    var activeSystems: List<String> = emptyList(),
    
    val alerts: List<Map<String, Any>>? = null,
    
    @get:PropertyName("bay_of_bengal")
    @set:PropertyName("bay_of_bengal")
    var bayOfBengal: BayOfBengalStatus? = null,
    
    @get:PropertyName("western_disturbance")
    @set:PropertyName("western_disturbance")
    var westernDisturbance: WesternDisturbanceStatus? = null,
    
    val norwesters: Map<String, Any>? = null,
    
    val timestamp: String? = null,
)

/** Daily confidence info from backend v86+ */
@IgnoreExtraProperties
data class DayConfidence(
    val label: String = "",
    val overall: Int = 0,
    val precip: Int = 0,
    val temp: Int = 0,
)
