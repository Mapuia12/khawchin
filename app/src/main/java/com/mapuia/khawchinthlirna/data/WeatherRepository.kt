package com.mapuia.khawchinthlirna.data

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.mapuia.khawchinthlirna.data.model.WeatherDoc
import com.mapuia.khawchinthlirna.util.AppLog
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
 * - Write: `crowd_reports` (must match Firestore security rules)
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
        AppLog.d("WeatherRepo", "getWeatherByGridId called with: $gridId")
        
        // Validate input
        if (!gridId.isValidGridId()) {
            AppLog.e("WeatherRepo", "Invalid grid ID format: $gridId")
            throw IllegalArgumentException("Invalid grid ID format: $gridId")
        }

        // First try the exact grid ID
        AppLog.d("WeatherRepo", "Trying exact grid ID: $gridId")
        val exactDoc = getWeatherWithRetry(gridId)
        if (exactDoc != null) {
            AppLog.d("WeatherRepo", "Found exact document for: $gridId")
            return exactDoc
        }

        AppLog.d("WeatherRepo", "Exact doc not found, trying fallback search")
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
        
        AppLog.d("WeatherRepo", "Starting robust fallback search from: $originalGridId (user: $userLat, $userLon)")

        // Generate ALL possible nearby grid IDs sorted by distance (uses 0.50° radius = ~55km)
        val candidates = generateNearbyCandidates(userLat, userLon, maxRadiusDegrees = 0.50)
        AppLog.d("WeatherRepo", "Generated ${candidates.size} fallback candidates, first 10: ${candidates.take(10)}")

        // Query in batches of 10 (Firestore whereIn limit)
        val foundDocs = mutableListOf<Pair<WeatherDoc, Double>>() // doc to distance
        var batchesQueried = 0
        val maxBatches = 50 // Query up to 500 candidates before giving up
        
        for (batch in candidates.chunked(10)) {
            if (batch.isEmpty()) continue
            if (batchesQueried >= maxBatches) break
            batchesQueried++
            
            try {
                AppLog.d("WeatherRepo", "Querying batch $batchesQueried: ${batch.joinToString()}")
                
                // Use FieldPath.documentId() since document IDs ARE the grid IDs
                val snapshot = db.collection(WeatherConstants.WEATHER_COLLECTION)
                    .whereIn(FieldPath.documentId(), batch)
                    .get()
                    .await()
                
                AppLog.d("WeatherRepo", "Batch $batchesQueried returned ${snapshot.documents.size} documents")
                
                for (document in snapshot.documents) {
                    val doc = document.toObject(WeatherDoc::class.java)
                    if (doc != null && doc.isValid()) {
                        val distance = haversineKm(userLat, userLon, doc.lat, doc.lon)
                        foundDocs.add(Pair(doc, distance))
                        AppLog.d("WeatherRepo", "Found valid doc: ${doc.gridId ?: document.id} at ${String.format("%.1f", distance)}km")
                    }
                }
                
                // If we found any valid document, we can stop after checking a few more batches
                // to ensure we have the closest one
                if (foundDocs.isNotEmpty() && batchesQueried >= 5) {
                    AppLog.d("WeatherRepo", "Found ${foundDocs.size} docs after $batchesQueried batches, selecting closest")
                    break
                }
            } catch (e: Exception) {
                AppLog.e("WeatherRepo", "Batch $batchesQueried query failed: ${e.message}")
                // Continue with next batch
            }
        }
        
        AppLog.d("WeatherRepo", "Total found: ${foundDocs.size} documents after $batchesQueried batches")
        
        // Return closest valid document
        val closest = foundDocs.minByOrNull { it.second }?.first
        if (closest != null) {
            AppLog.d("WeatherRepo", "Fallback success! Using: ${closest.gridId} (distance: ${foundDocs.minByOrNull { it.second }?.second?.let { String.format("%.1f", it) }}km)")
            runCatching { cache?.save(gridId = originalGridId, doc = closest) }
            return closest
        }
        
        AppLog.w("WeatherRepo", "No fallback documents found after $batchesQueried batches, trying cache")
        return getCachedWeatherFallback(originalGridId)
    }

    /**
     * Generate nearby grid ID candidates sorted by distance.
     * Uses 0.01 degree step to ensure ALL possible 2-decimal grid IDs are covered.
     * Backend uses various grids (0.25 coarse, 0.10 refined, POI-based).
     * 
     * FIXED: Use finer step (0.01) and larger radius (0.50) for comprehensive coverage.
     */
    private fun generateNearbyCandidates(lat: Double, lon: Double, maxRadiusDegrees: Double): List<String> {
        val candidates = mutableSetOf<String>()
        
        // Use 0.01 degree step to cover ALL possible 2-decimal grid points
        // This ensures we don't miss any grid like 23.48_93.24
        // For 0.50° radius: generates ~10,000 candidates but Firestore batch queries handle it
        val step = 0.01
        val searchRadius = maxOf(maxRadiusDegrees, 0.50) // At least 0.50° (~55km) search radius
        
        var dLat = -searchRadius
        while (dLat <= searchRadius) {
            var dLon = -searchRadius
            while (dLon <= searchRadius) {
                // Round to 2 decimals exactly as backend stores them
                val gLat = ((lat + dLat) * 100).roundToInt() / 100.0
                val gLon = ((lon + dLon) * 100).roundToInt() / 100.0
                
                // Only add if within valid coordinate range (Mizoram + Myanmar area)
                if (gLat in 21.0..25.0 && gLon in 91.5..95.0) {
                    candidates.add(String.format(Locale.US, "%.2f_%.2f", gLat, gLon))
                }
                dLon += step
            }
            dLat += step
        }
        
        AppLog.d("WeatherRepo", "Generated ${candidates.size} total candidates for $lat, $lon")
        
        // Sort by distance from user location and take reasonable limit
        return candidates
            .map { id ->
                val parts = id.split("_")
                val gLat = parts[0].toDouble()
                val gLon = parts[1].toDouble()
                Pair(id, haversineKm(lat, lon, gLat, gLon))
            }
            .sortedBy { it.second }
            .take(500) // Limit to closest 500 to avoid excessive queries
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
        val cached = cache?.cachedWeather?.firstOrNull()
        if (cached == null) return null
        
        // Only use cache if it matches the requested gridId OR is very close
        // Don't use cache from unrelated locations
        if (!cached.isExpired()) {
            val cachedGridId = cached.gridId ?: return null
            val (reqLat, reqLon) = parseGridId(gridId) ?: return null
            val (cachedLat, cachedLon) = parseGridId(cachedGridId) ?: return null
            
            val distanceDegrees = kotlin.math.sqrt(
                (reqLat - cachedLat) * (reqLat - cachedLat) + 
                (reqLon - cachedLon) * (reqLon - cachedLon)
            )
            
            // Only use cache if within 0.1 degrees (~11km) of requested location
            if (distanceDegrees <= 0.1) {
                AppLog.d("WeatherRepo", "Using cached weather from $cachedGridId for $gridId (dist: ${"%.3f".format(distanceDegrees)}°, age: ${cached.getAgeMinutes()} min)")
                return cached.doc
            } else {
                AppLog.d("WeatherRepo", "Cache too far: $cachedGridId vs $gridId (dist: ${"%.3f".format(distanceDegrees)}°)")
                return null
            }
        }
        
        AppLog.d("WeatherRepo", "Cache expired (age: ${cached.getAgeMinutes()} min > ${WeatherConstants.CACHE_EXPIRY_MINUTES} min)")
        return null
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
            // Must use 'timestamp_auto' to match Firestore security rules
            "timestamp_auto" to Instant.now().toString(),
            "report_type" to optionMizo.sanitizeInput(),
            // Required by Firestore rules
            "user_id" to currentUser.uid,
            // rain_intensity is required by rules (use severity as approximation)
            "rain_intensity" to severityClamped,
        )

        if (!gridId.isNullOrBlank()) {
            data["grid_id"] = gridId
        }

        db.collection(WeatherConstants.REPORTS_COLLECTION).add(data).await()
    }
}
