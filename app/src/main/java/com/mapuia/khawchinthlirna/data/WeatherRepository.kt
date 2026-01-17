package com.mapuia.khawchinthlirna.data

import com.google.firebase.firestore.FirebaseFirestore
import com.mapuia.khawchinthlirna.data.model.WeatherDoc
import kotlinx.coroutines.delay
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Locale
import kotlin.math.*

/**
 * Repository that encapsulates Firestore access.
 *
 * Contract:
 * - Read: `weather_v69_grid/{gridId}`
 * - Write: `crowdsource_reports` (must match backend function field names)
 * 
 * ROBUST FALLBACK STRATEGY:
 * 1. Try exact grid ID from user location (rounded to 2 decimals)
 * 2. If not found, use Firestore whereIn query with batch of nearby candidates
 * 3. Return the closest valid document by distance
 */
class WeatherRepository(
    private val db: FirebaseFirestore,
    private val cache: WeatherCache? = null,
) {

    /**
     * Get weather by grid ID. If document doesn't exist, automatically
     * searches for nearest available grid point in Firestore using batch queries.
     */
    suspend fun getWeatherByGridId(gridId: String): WeatherDoc? {
        android.util.Log.d("WeatherRepo", "getWeatherByGridId called with: $gridId")
        
        // Validate input
        if (!gridId.isValidGridId()) {
            android.util.Log.e("WeatherRepo", "Invalid grid ID format: $gridId")
            throw IllegalArgumentException("Invalid grid ID format: $gridId")
        }

        // First try the exact grid ID
        android.util.Log.d("WeatherRepo", "Trying exact grid ID: $gridId")
        val exactDoc = getWeatherWithRetry(gridId)
        if (exactDoc != null) {
            android.util.Log.d("WeatherRepo", "Found exact document for: $gridId")
            return exactDoc
        }

        android.util.Log.d("WeatherRepo", "Exact doc not found, trying fallback search")
        // Document not found - try robust fallback search
        return findNearestAvailableWeather(gridId)
    }

    /**
     * ROBUST FALLBACK: Find nearest available weather document.
     * 
     * Strategy:
     * 1. Generate candidate grid IDs in expanding rings (0.01 to 0.30 degree radius)
     * 2. Use Firestore whereIn queries (batch of 10) to efficiently check multiple IDs
     * 3. Return the closest valid document by geographic distance
     * 
     * This handles the case where user is at 23.19_94.01 but Firebase has 23.19_94.05
     */
    private suspend fun findNearestAvailableWeather(originalGridId: String): WeatherDoc? {
        val (userLat, userLon) = parseGridId(originalGridId) ?: return getCachedWeatherFallback(originalGridId)
        
        android.util.Log.d("WeatherRepo", "Starting robust fallback search from: $originalGridId")

        // Generate ALL possible nearby grid IDs sorted by distance
        val candidates = generateNearbyCandidates(userLat, userLon, maxRadiusDegrees = 0.30)
        android.util.Log.d("WeatherRepo", "Generated ${candidates.size} fallback candidates")

        // Query in batches of 10 (Firestore whereIn limit)
        val foundDocs = mutableListOf<Pair<WeatherDoc, Double>>() // doc to distance
        
        for (batch in candidates.chunked(10)) {
            if (batch.isEmpty()) continue
            
            try {
                android.util.Log.d("WeatherRepo", "Querying batch: ${batch.take(3)}...")
                
                val snapshot = db.collection(WeatherConstants.WEATHER_COLLECTION)
                    .whereIn("grid_id", batch)
                    .get()
                    .await()
                
                for (document in snapshot.documents) {
                    val doc = document.toObject(WeatherDoc::class.java)
                    if (doc != null && doc.isValid()) {
                        val distance = haversineKm(userLat, userLon, doc.lat, doc.lon)
                        foundDocs.add(Pair(doc, distance))
                        android.util.Log.d("WeatherRepo", "Found valid doc: ${doc.gridId} at ${distance}km")
                    }
                }
                
                // If we found at least one valid document in first 3 batches (closest 30 candidates), 
                // we can stop early for efficiency
                if (foundDocs.isNotEmpty() && candidates.indexOf(batch.first()) < 30) {
                    android.util.Log.d("WeatherRepo", "Early exit with ${foundDocs.size} docs found")
                    break
                }
            } catch (e: Exception) {
                android.util.Log.e("WeatherRepo", "Batch query failed: ${e.message}")
                // Continue with next batch
            }
        }
        
        // Return closest valid document
        val closest = foundDocs.minByOrNull { it.second }?.first
        if (closest != null) {
            android.util.Log.d("WeatherRepo", "Fallback success! Using: ${closest.gridId}")
            runCatching { cache?.save(gridId = originalGridId, doc = closest) }
            return closest
        }
        
        android.util.Log.w("WeatherRepo", "No fallback documents found, trying cache")
        return getCachedWeatherFallback(originalGridId)
    }

    /**
     * Generate nearby grid ID candidates sorted by distance.
     * Uses fine-grained search to catch all possible grid points (both coarse 0.25 and refined 0.1 grids).
     */
    private fun generateNearbyCandidates(lat: Double, lon: Double, maxRadiusDegrees: Double): List<String> {
        val candidates = mutableSetOf<String>()
        
        // Generate grid points in 0.01 degree increments to catch ALL possible Firebase documents
        // Backend uses 0.25 coarse + 0.1 refined, so 0.01 step guarantees we hit every possible ID
        var dLat = -maxRadiusDegrees
        while (dLat <= maxRadiusDegrees) {
            var dLon = -maxRadiusDegrees
            while (dLon <= maxRadiusDegrees) {
                val gLat = ((lat + dLat) * 100).roundToInt() / 100.0
                val gLon = ((lon + dLon) * 100).roundToInt() / 100.0
                
                // Only add if within valid coordinate range
                if (gLat in 20.0..26.0 && gLon in 90.0..96.0) {
                    candidates.add(String.format(Locale.US, "%.2f_%.2f", gLat, gLon))
                }
                dLon += 0.01
            }
            dLat += 0.01
        }
        
        // Sort by distance from user location
        return candidates
            .map { id ->
                val parts = id.split("_")
                val gLat = parts[0].toDouble()
                val gLon = parts[1].toDouble()
                Pair(id, haversineKm(lat, lon, gLat, gLon))
            }
            .sortedBy { it.second }
            .map { it.first }
    }

    /**
     * Haversine formula for distance between two coordinates in km
     */
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + 
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * 
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
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
     * - timestamp_auto (iso string)
     */
    suspend fun submitCrowdReport(
        optionMizo: String,
        gridId: String?,
        userLat: Double?,
        userLon: Double?,
        accuracyMeters: Double = 150.0,
        severity: Int = 3,
    ) {
        // Keep payload minimal and aligned with backend function.
        // Validate inputs
        if (userLat == null || userLon == null) {
            throw IllegalArgumentException("Location coordinates required")
        }
        if (userLat !in -90.0..90.0 || userLon !in -180.0..180.0) {
            throw IllegalArgumentException("Invalid coordinates")
        }

        // Enforce authentication because Firestore rules require request.auth != null for creating reports
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            throw IllegalStateException("Authentication required to submit reports")
        }

        val severityClamped = severity.coerceIn(1, 5)

        val data = hashMapOf<String, Any>(
            "lat" to userLat,
            "lon" to userLon,
            "accuracy_m" to accuracyMeters.coerceIn(1.0, 10000.0),
            // Firestore rule expects severity to be integer
            "severity" to severityClamped,
            // Firestore rule expects timestamp_utc field name
            "timestamp_utc" to Instant.now().toString(),
            "report_type" to optionMizo.sanitizeInput(),
        )

        if (!gridId.isNullOrBlank()) {
            data["grid_id"] = gridId
        }

        db.collection(WeatherConstants.REPORTS_COLLECTION).add(data).await()
    }
}
