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
    NO_RAIN(0, "Ruah a sur lo", "No Rain", "Van a eng, thli a awm lo", "0 mm/hr"),
    DRIZZLE(1, "Ruah fa", "Drizzle", "A nuam, umbrella mamawh lo", "0.1-2.5 mm/hr"),
    LIGHT(2, "Ruah nuam", "Light Rain", "A nuam, ṭhiannu neih ṭha", "2.5-7.5 mm/hr"),
    MODERATE(3, "Ruah zau", "Moderate", "A zau, pawnah awm ṭha lo", "7.5-25 mm/hr"),
    HEAVY(4, "Ruah nasa", "Heavy", "A nasa, tui a lian thei", "25-50 mm/hr"),
    VERY_HEAVY(5, "Ruah nasa tak", "Very Heavy", "Nasa tak, chhuah harsa", "50-100 mm/hr"),
    EXTREME(6, "Ruah vanduai", "Extreme", "Vanduai ang, chhuah ngai lo", ">100 mm/hr");

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
    CLEAR(0, "Van a eng", "Clear"),
    PARTLY_CLOUDY(1, "Sum leh van", "Partly Cloudy"),
    MOSTLY_CLOUDY(2, "Van a dum zau", "Mostly Cloudy"),
    OVERCAST(3, "Van a dum vek", "Overcast"),
    FOG(4, "Mauva", "Fog");

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
    CALM(0, "A del", "Calm"),
    LIGHT(1, "Thli nuam", "Light"),
    MODERATE(2, "Thli zau", "Moderate"),
    STRONG(3, "Thli nasa", "Strong"),
    VERY_STRONG(4, "Thli vanduai", "Very Strong");

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

