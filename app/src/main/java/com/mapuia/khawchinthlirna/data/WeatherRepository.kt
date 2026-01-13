package com.mapuia.khawchinthlirna.data

import com.google.firebase.firestore.FirebaseFirestore
import com.mapuia.khawchinthlirna.data.model.WeatherDoc
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.time.Instant

/**
 * Repository that encapsulates Firestore access.
 *
 * Contract:
 * - Read: `weather_v69_grid/{gridId}`
 * - Write: `crowd_reports` (must match backend function field names)
 */
class WeatherRepository(
    private val db: FirebaseFirestore,
    private val cache: WeatherCache? = null,
) {

    /**
     * Get weather by grid ID. If document doesn't exist, automatically
     * searches for nearest available grid point in Firestore.
     */
    suspend fun getWeatherByGridId(gridId: String): WeatherDoc? {
        // Validate input
        if (!gridId.isValidGridId()) {
            throw IllegalArgumentException("Invalid grid ID format: $gridId")
        }

        // First try the exact grid ID
        val exactDoc = getWeatherWithRetry(gridId)
        if (exactDoc != null) {
            return exactDoc
        }

        // Document not found - try nearest grid points
        return findNearestAvailableWeather(gridId)
    }

    /**
     * Find nearest available weather document when exact grid ID doesn't exist.
     * Searches in expanding radius from the original grid point.
     * Uses the grid utilities to ensure we look for grid IDs that match backend's 0.20 step.
     */
    private suspend fun findNearestAvailableWeather(originalGridId: String): WeatherDoc? {
        val (lat, lon) = parseGridId(originalGridId) ?: return getCachedWeatherFallback(originalGridId)

        // Generate candidate grid IDs sorted by distance using grid-aligned utilities
        // Use a larger search radius (50km) for better coverage in sparse areas
        val candidates = getNearbyGridIds(lat, lon, maxDistanceKm = 50.0)

        for (candidateId in candidates) {
            if (candidateId == originalGridId) continue // Already tried

            try {
                val doc = db.collection(WeatherConstants.WEATHER_COLLECTION)
                    .document(candidateId)
                    .get()
                    .await()
                    .toObject(WeatherDoc::class.java)

                if (doc != null && doc.isValid()) {
                    // Found a valid nearby document
                    runCatching { cache?.save(gridId = originalGridId, doc = doc) }
                    return doc
                }
            } catch (e: Exception) {
                // Continue to next candidate
                continue
            }
        }

        // No nearby documents found, fall back to cache
        return getCachedWeatherFallback(originalGridId)
    }

    /**
     * Parse grid ID like "23.50_93.30" into (lat, lon) pair.
     */
    private fun parseGridId(gridId: String): Pair<Double, Double>? {
        return try {
            val parts = gridId.split("_")
            if (parts.size == 2) {
                Pair(parts[0].toDouble(), parts[1].toDouble())
            } else null
        } catch (e: Exception) {
            null
        }
    }


    private suspend fun getWeatherWithRetry(gridId: String, maxRetries: Int = WeatherConstants.MAX_RETRY_ATTEMPTS): WeatherDoc? {
        repeat(maxRetries) { attempt ->
            try {
                val doc = db.collection(WeatherConstants.WEATHER_COLLECTION)
                    .document(gridId)
                    .get()
                    .await()
                    .toObject(WeatherDoc::class.java)

                if (doc != null && doc.isValid()) {
                    // Cache successful, valid docs
                    runCatching { cache?.save(gridId = gridId, doc = doc) }
                    return doc
                } else {
                    // Document doesn't exist or invalid - return null to trigger fallback
                    return null
                }
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) {
                    // Last attempt failed, return null
                    return null
                }
                // Exponential backoff
                delay(1000L * (attempt + 1))
            }
        }
        return null
    }

    private suspend fun getCachedWeatherFallback(gridId: String): WeatherDoc? {
        return cache?.cachedWeather?.firstOrNull()?.takeIf { it.gridId == gridId }?.doc
            ?: cache?.cachedWeather?.firstOrNull()?.doc
    }

    /**
     * Writes a report document that matches the backend function contract.
     *
     * Backend reads:
     * - lat/lon
     * - accuracy_m (default 150)
     * - severity (default 3)
     * - has_photo or photo_path
     * - timestamp_auto (iso string)
     */
    suspend fun submitCrowdReport(
        optionMizo: String,
        gridId: String?,
        userLat: Double?,
        userLon: Double?,
        accuracyMeters: Double = 150.0,
        severity: Double = 3.0,
        hasPhoto: Boolean = false,
        photoPath: String? = null,
    ) {
        val finalHasPhoto = hasPhoto || !photoPath.isNullOrBlank()

        // Keep payload minimal and aligned with backend function.
        // Validate inputs
        if (userLat == null || userLon == null) {
            throw IllegalArgumentException("Location coordinates required")
        }
        if (userLat !in -90.0..90.0 || userLon !in -180.0..180.0) {
            throw IllegalArgumentException("Invalid coordinates")
        }

        val data = hashMapOf<String, Any>(
            "lat" to userLat,
            "lon" to userLon,
            "accuracy_m" to accuracyMeters.coerceIn(1.0, 10000.0),
            "severity" to severity.coerceIn(1.0, 5.0),
            "has_photo" to finalHasPhoto,
            "timestamp_auto" to Instant.now().toString(),
            "report_type" to optionMizo.sanitizeInput(),
        )

        // Optional: store photo_path only when present.
        if (!photoPath.isNullOrBlank()) {
            data["photo_path"] = photoPath
        }
        if (!gridId.isNullOrBlank()) {
            data["grid_id"] = gridId
        }

        db.collection(WeatherConstants.REPORTS_COLLECTION).add(data).await()
    }
}
