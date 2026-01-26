package com.mapuia.khawchinthlirna.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.mapuia.khawchinthlirna.data.model.NearbyReport
import com.mapuia.khawchinthlirna.data.model.RainIntensity
import com.mapuia.khawchinthlirna.data.model.ReportRequest
import kotlinx.coroutines.tasks.await
import java.time.Instant

/**
 * Repository for crowdsource weather reports.
 * Handles submitting reports and fetching nearby reports.
 */
class CrowdsourceRepository(
    private val db: FirebaseFirestore,
) {

    companion object {
        // Must match Firestore security rules: match /crowd_reports/{reportId}
        private const val REPORTS_COLLECTION = "crowd_reports"
    }

    /**
     * Submit a comprehensive weather report.
     * Matches backend API: POST /api/v1/reports
     */
    suspend fun submitReport(
        userId: String,
        lat: Double,
        lon: Double,
        rainIntensity: Int,
        skyCondition: Int? = null,
        windStrength: Int? = null,
        notes: String? = null,
        locationName: String? = null,
        accuracyMeters: Double = 150.0,
        gridId: String? = null,
    ) {
        // Validate inputs
        require(userId.isNotBlank()) { "User ID required" }
        require(lat in 21.0..26.0) { "Latitude must be between 21.0 and 26.0 (Mizoram region)" }
        require(lon in 91.0..96.0) { "Longitude must be between 91.0 and 96.0 (Mizoram region)" }
        require(rainIntensity in 0..6) { "Rain intensity must be 0-6" }
        skyCondition?.let { require(it in 0..4) { "Sky condition must be 0-4" } }
        windStrength?.let { require(it in 0..4) { "Wind strength must be 0-4" } }
        notes?.let { require(it.length <= 500) { "Notes must be max 500 characters" } }

        val data = hashMapOf<String, Any>(
            "user_id" to userId,
            "lat" to lat,
            "lon" to lon,
            "rain_intensity" to rainIntensity,
            "accuracy_m" to accuracyMeters.coerceIn(1.0, 10000.0),
            "timestamp_auto" to Instant.now().toString(),  // Must match Firestore rules field name
        )

        // Optional fields
        skyCondition?.let { data["sky_condition"] = it }
        windStrength?.let { data["wind_strength"] = it }
        notes?.takeIf { it.isNotBlank() }?.let { data["notes"] = it.take(500) }
        locationName?.takeIf { it.isNotBlank() }?.let { data["location_name"] = it }
        gridId?.takeIf { it.isNotBlank() }?.let { data["grid_id"] = it }

        // Map rain intensity to severity for backend clustering
        // Firestore rules require: severity >= 1 && severity <= 5 (integer values only)
        val severity = when (rainIntensity) {
            0 -> 1
            1 -> 1
            2 -> 2
            3 -> 3
            4 -> 4
            5 -> 5
            6 -> 5
            else -> 3
        }
        data["severity"] = severity

        // Map rain intensity to Mizo label for legacy compatibility
        val rainLabel = RainIntensity.fromLevel(rainIntensity)
        data["report_type"] = rainLabel.labelMizo

        try {

            android.util.Log.d("CROWD", "submitReport called. userId param=$userId, authUid=${com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid}")
            android.util.Log.d("CROWD", "payload preview: lat=${data["lat"]} lon=${data["lon"]} rain_intensity=${data["rain_intensity"]} timestamp_auto=${data["timestamp_auto"]}")
            db.collection(REPORTS_COLLECTION).add(data).await()
        } catch (e: FirebaseFirestoreException) {
            // Provide user-friendly error messages
            val userMessage = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> 
                    "Report submit theih loh - Sign in hmasa rawh le"
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Network connection a awm lo - Beih leh rawh"
                else -> 
                    "Report submit a hlawh lo: ${e.message}"
            }
            throw Exception(userMessage, e)
        }
    }

    /**
     * Fetch nearby reports.
     * Backend endpoint: GET /api/v1/reports/nearby?lat={lat}&lon={lon}&radius_km=15&minutes=60
     *
     * Note: Since we're using Firestore directly, we'll do a simplified query.
     * For production, consider using Firebase Cloud Functions or backend API.
     */
    suspend fun getNearbyReports(
        lat: Double,
        lon: Double,
        radiusKm: Double = 15.0,
        minutes: Int = 60,
    ): List<NearbyReport> {
        // Calculate time threshold
        val threshold = Instant.now().minusSeconds(minutes * 60L).toString()

        // Firestore doesn't support geospatial queries directly.
        // We'll fetch recent reports and filter by distance client-side.
        // For production, use Geohash or Cloud Functions.

        // Simple bounding box approximation
        val latDelta = radiusKm / 111.0 // ~111km per degree latitude
        val lonDelta = radiusKm / (111.0 * kotlin.math.cos(Math.toRadians(lat)))

        val minLat = lat - latDelta
        val maxLat = lat + latDelta
        val minLon = lon - lonDelta
        val maxLon = lon + lonDelta

        return try {
            val snapshot = db.collection(REPORTS_COLLECTION)
                .whereGreaterThanOrEqualTo("lat", minLat)
                .whereLessThanOrEqualTo("lat", maxLat)
                .whereGreaterThanOrEqualTo("timestamp_auto", threshold)  // Must match field name in Firestore
                .limit(100)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val reportLat = doc.getDouble("lat") ?: return@mapNotNull null
                    val reportLon = doc.getDouble("lon") ?: return@mapNotNull null

                    // Check longitude bounds
                    if (reportLon < minLon || reportLon > maxLon) return@mapNotNull null

                    // Calculate actual distance
                    val distance = haversineDistance(lat, lon, reportLat, reportLon)
                    if (distance > radiusKm) return@mapNotNull null

                    NearbyReport(
                        id = doc.id,
                        lat = reportLat,
                        lon = reportLon,
                        rainIntensity = doc.getLong("rain_intensity")?.toInt() ?: 0,
                        skyCondition = doc.getLong("sky_condition")?.toInt(),
                        windStrength = doc.getLong("wind_strength")?.toInt(),
                        locationName = doc.getString("location_name"),
                        timestampAuto = doc.getString("timestamp_auto") ?: "",  // Read from 'timestamp_auto'
                        userReputation = doc.getDouble("user_reputation"),
                        distanceKm = distance,
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.distanceKm }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return R * c
    }
}

