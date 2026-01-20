package com.mapuia.khawchinthlirna.data

import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.mapuia.khawchinthlirna.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL
import java.util.Locale

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
 * Contract:
 * - Input: latitude/longitude
 * - Output: a short human-readable place name (e.g. "Letpanchaung") or null
 * - Never throws (callers can rely on null on failure)
 */
class ReverseGeocoder(private val context: Context) {

    companion object {
        private const val TAG = "ReverseGeocoder"
        // Nominatim API (free, no API key needed)
        // Note: Requires User-Agent header per Nominatim usage policy
        private const val NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse"
    }

    /**
     * Get place name for coordinates using Nominatim API first, then Android Geocoder fallback.
     */
    suspend fun getPlaceName(lat: Double, lon: Double): String? {
        // Try Nominatim first (better for rural areas)
        val nominatimResult = getNominatimPlaceName(lat, lon)
        if (!nominatimResult.isNullOrBlank()) {
            AppLog.d(TAG, "Nominatim result: $nominatimResult")
            return nominatimResult
        }
        
        // Fallback to Android Geocoder
        return getAndroidGeocoderPlaceName(lat, lon)
    }

    /**
     * Fetch place name from OpenStreetMap Nominatim API.
     * Returns village/town/city name, not administrative regions.
     */
    private suspend fun getNominatimPlaceName(lat: Double, lon: Double): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("$NOMINATIM_URL?format=json&lat=$lat&lon=$lon&zoom=18&addressdetails=1")
                val connection = url.openConnection().apply {
                    setRequestProperty("User-Agent", "KhawchinThlirna/1.0 (Mizoram Weather App)")
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

