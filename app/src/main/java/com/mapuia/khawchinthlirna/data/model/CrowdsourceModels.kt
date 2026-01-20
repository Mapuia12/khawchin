package com.mapuia.khawchinthlirna.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Request model for submitting crowdsource weather reports.
 * Matches backend API: POST /api/v1/reports
 */
data class ReportRequest(
    // Required fields
    @get:PropertyName("user_id")
    @set:PropertyName("user_id")
    var userId: String = "",

    val lat: Double = 0.0,
    val lon: Double = 0.0,

    @get:PropertyName("rain_intensity")
    @set:PropertyName("rain_intensity")
    var rainIntensity: Int = 0, // 0-6

    // Optional fields
    @get:PropertyName("sky_condition")
    @set:PropertyName("sky_condition")
    var skyCondition: Int? = null, // 0-4

    @get:PropertyName("wind_strength")
    @set:PropertyName("wind_strength")
    var windStrength: Int? = null, // 0-4

    val notes: String? = null, // max 500 chars

    @get:PropertyName("location_name")
    @set:PropertyName("location_name")
    var locationName: String? = null,

    // Additional metadata
    @get:PropertyName("accuracy_m")
    @set:PropertyName("accuracy_m")
    var accuracyMeters: Double = 150.0,

    @get:PropertyName("timestamp_auto")
    @set:PropertyName("timestamp_auto")
    var timestampAuto: String = "",

    @get:PropertyName("grid_id")
    @set:PropertyName("grid_id")
    var gridId: String? = null,
)

/**
 * Rain intensity levels with Mizo labels
 */
enum class RainIntensity(
    val level: Int,
    val labelMizo: String,
    val labelEnglish: String,
    val description: String,
    val mmPerHour: String,
) {
    NO_RAIN(0, "Ruah Sur Lo", "No Rain", "Khua a thiang, ruah a sur lo", "0 mm/hr"),
    DRIZZLE(1, "Ruah Phingphisiau", "Drizzle", "Ruah mal, a hmi te te a tla", "0.1-2.5 mm/hr"),
    LIGHT(2, "Ruah Tlem", "Light Rain", "Ruah a sur cherh cherh", "2.5-7.5 mm/hr"),
    MODERATE(3, "Ruah Sur Pangngai", "Moderate", "Ruah a sur ve deuh, nihliap mamawh", "7.5-25 mm/hr"),
    HEAVY(4, "Ruah Nasa", "Heavy", "Ruah a tam, tui a lian thei", "25-50 mm/hr"),
    VERY_HEAVY(5, "Ruah Nasa Tak", "Very Heavy", "Ruah a sur buan buan, pawn chhuah a harsa", "50-100 mm/hr"),
    EXTREME(6, "Ruahpui / Hlauhawm", "Extreme", "A hlauhawm thei, in chhungah awm rawh", ">100 mm/hr");

    companion object {
        fun fromLevel(level: Int): RainIntensity = entries.find { it.level == level } ?: NO_RAIN
    }
}

/**
 * Sky condition levels with Mizo labels
 */
enum class SkyCondition(
    val level: Int,
    val labelMizo: String,
    val labelEnglish: String,
) {
    CLEAR(0, "Van a thiang", "Clear"),
    PARTLY_CLOUDY(1, "Chhum awm pheuh pheuh", "Partly Cloudy"),
    MOSTLY_CLOUDY(2, "Chhum tam tak a awm", "Mostly Cloudy"),
    OVERCAST(3, "Chhumin a khat vek", "Overcast"),
    FOG(4, "Tiauchhum a zing", "Fog");

    companion object {
        fun fromLevel(level: Int): SkyCondition = entries.find { it.level == level } ?: CLEAR
    }
}

/**
 * Wind strength levels with Mizo labels
 */
enum class WindStrength(
    val level: Int,
    val labelMizo: String,
    val labelEnglish: String,
) {
    CALM(0, "Thli Thaw Lo", "Calm"),
    LIGHT(1, "Thli Thaw Heuh Heuh", "Light"),
    MODERATE(2, "Thli Thaw Pangngai", "Moderate"),
    STRONG(3, "Thli Na", "Strong"),
    VERY_STRONG(4, "Thli Na Tak / Thlipui", "Very Strong");

    companion object {
        fun fromLevel(level: Int): WindStrength = entries.find { it.level == level } ?: CALM
    }
}

/**
 * Nearby report model from backend
 */
data class NearbyReport(
    val id: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,

    @get:PropertyName("rain_intensity")
    @set:PropertyName("rain_intensity")
    var rainIntensity: Int = 0,

    @get:PropertyName("sky_condition")
    @set:PropertyName("sky_condition")
    var skyCondition: Int? = null,

    @get:PropertyName("wind_strength")
    @set:PropertyName("wind_strength")
    var windStrength: Int? = null,

    @get:PropertyName("location_name")
    @set:PropertyName("location_name")
    var locationName: String? = null,

    @get:PropertyName("timestamp_auto")
    @set:PropertyName("timestamp_auto")
    var timestampAuto: String = "",

    @get:PropertyName("user_reputation")
    @set:PropertyName("user_reputation")
    var userReputation: Double? = null,

    @get:PropertyName("distance_km")
    @set:PropertyName("distance_km")
    var distanceKm: Double? = null,
)

/**
 * Marine risk level model
 */
data class MarineRisk(
    val score: Double = 0.0,
    val level: String = "GREEN", // GREEN, YELLOW, ORANGE, RED
)

