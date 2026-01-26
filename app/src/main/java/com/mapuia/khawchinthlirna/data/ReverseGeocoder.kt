package com.mapuia.khawchinthlirna.data

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.LruCache
import com.mapuia.khawchinthlirna.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Best-effort reverse geocoder using multiple sources.
 *
 * PRIORITY ORDER:
 * 1. OpenStreetMap Nominatim API (free, covers rural areas worldwide)
 * 2. Android Geocoder (Google's backend, fallback)
 *
 * This approach is similar to how Google Maps works - using a real geocoding API
 * instead of hardcoded location databases. Nominatim has excellent coverage of
 * rural areas in Myanmar and India.
 *
 * NOMINATIM USAGE POLICY COMPLIANCE:
 * - Rate limited to max 1 request per second
 * - Results cached to reduce API calls
 * - User-Agent includes app name and contact email
 * - See: https://operations.osmfoundation.org/policies/nominatim/
 *
 * Contract:
 * - Input: latitude/longitude
 * - Output: a short human-readable place name (e.g. "Letpanchaung") or null
 * - Never throws (callers can rely on null on failure)
 */
class ReverseGeocoder(private val context: Context) {

    companion object {
        private const val TAG = "ReverseGeocoder"
        // Nominatim API (free, no API key needed)
        private const val NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse"
        
        // Rate limiting: max 1 request per second per Nominatim policy
        private const val MIN_REQUEST_INTERVAL_MS = 1100L // 1.1 seconds to be safe
        
        // Cache settings
        private const val CACHE_SIZE = 100
        private const val COORDINATE_PRECISION = 3 // ~111m precision for cache key
    }
    
    // Simple in-memory cache to reduce API calls
    private val cache = LruCache<String, String>(CACHE_SIZE)
    
    // Track last request time for rate limiting
    private val lastRequestTime = AtomicLong(0)
    
    /**
     * Generate cache key from coordinates (rounded for reasonable precision)
     */
    private fun cacheKey(lat: Double, lon: Double): String {
        val latRounded = "%.${COORDINATE_PRECISION}f".format(lat)
        val lonRounded = "%.${COORDINATE_PRECISION}f".format(lon)
        return "$latRounded,$lonRounded"
    }

    /**
     * Get place name for coordinates using Nominatim API first, then Android Geocoder fallback.
     * Results are cached to reduce API calls.
     */
    suspend fun getPlaceName(lat: Double, lon: Double): String? {
        // Check cache first
        val key = cacheKey(lat, lon)
        cache.get(key)?.let { cached ->
            AppLog.d(TAG, "Cache hit: $cached")
            return cached
        }
        
        // Try Nominatim first (better for rural areas)
        val nominatimResult = getNominatimPlaceName(lat, lon)
        if (!nominatimResult.isNullOrBlank()) {
            AppLog.d(TAG, "Nominatim result: $nominatimResult")
            cache.put(key, nominatimResult)
            return nominatimResult
        }
        
        // Fallback to Android Geocoder
        val androidResult = getAndroidGeocoderPlaceName(lat, lon)
        if (!androidResult.isNullOrBlank()) {
            cache.put(key, androidResult)
        }
        return androidResult
    }

    /**
     * Fetch place name from OpenStreetMap Nominatim API.
     * Returns village/town/city name, not administrative regions.
     * 
     * Respects Nominatim usage policy:
     * - Max 1 request per second
     * - Proper User-Agent with contact info
     */
    private suspend fun getNominatimPlaceName(lat: Double, lon: Double): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                // Rate limiting - wait if needed
                val now = System.currentTimeMillis()
                val elapsed = now - lastRequestTime.get()
                if (elapsed < MIN_REQUEST_INTERVAL_MS) {
                    delay(MIN_REQUEST_INTERVAL_MS - elapsed)
                }
                lastRequestTime.set(System.currentTimeMillis())
                
                val url = URL("$NOMINATIM_URL?format=json&lat=$lat&lon=$lon&zoom=18&addressdetails=1")
                val connection = url.openConnection().apply {
                    // User-Agent with app name and contact per Nominatim policy
                    // Replace email with your actual contact for production
                    setRequestProperty("User-Agent", "KhawchinThlirna/1.0 (Mizoram Weather App; contact@example.com)")
                    setRequestProperty("Accept-Language", "en")
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                
                val response = connection.getInputStream().bufferedReader().readText()
                
                // Parse JSON response
                val json = org.json.JSONObject(response)
                val address = json.optJSONObject("address")
                
                // Priority: village > hamlet > town > city > suburb > county
                // This gives us the most specific local name
                val placeName = address?.optString("village")?.takeIf { it.isNotBlank() }
                    ?: address?.optString("hamlet")?.takeIf { it.isNotBlank() }
                    ?: address?.optString("town")?.takeIf { it.isNotBlank() }
                    ?: address?.optString("city")?.takeIf { it.isNotBlank() }
                    ?: address?.optString("suburb")?.takeIf { it.isNotBlank() }
                    ?: address?.optString("municipality")?.takeIf { it.isNotBlank() }
                    ?: address?.optString("county")?.takeIf { it.isNotBlank() }
                
                placeName
            }.onFailure { e ->
                AppLog.w(TAG, "Nominatim API failed: ${e.message}")
            }.getOrNull()
        }
    }

    /**
     * Fallback to Android's built-in Geocoder (uses Google's backend).
     */
    @Suppress("DEPRECATION")
    private suspend fun getAndroidGeocoderPlaceName(lat: Double, lon: Double): String? {
        return runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lon, 1) { list ->
                        val a = list.firstOrNull()
                        val name = a?.locality
                            ?: a?.subLocality
                            ?: a?.subAdminArea
                            ?: a?.adminArea
                            ?: a?.featureName
                        cont.resume(name, onCancellation = null)
                    }
                }
            } else {
                val list = geocoder.getFromLocation(lat, lon, 1)
                val a = list?.firstOrNull()
                a?.locality ?: a?.subLocality ?: a?.subAdminArea ?: a?.adminArea ?: a?.featureName
            }
        }.onFailure { e ->
            AppLog.w(TAG, "Android Geocoder failed: ${e.message}")
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}

