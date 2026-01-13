package com.mapuia.khawchinthlirna.data

import android.content.Context
import android.location.Geocoder
import android.os.Build
import java.util.Locale

/**
 * Best-effort reverse geocoder.
 *
 * Contract:
 * - Input: latitude/longitude
 * - Output: a short human-readable place name (e.g. "Aizawl") or null
 * - Never throws (callers can rely on null on failure)
 */
class ReverseGeocoder(private val context: Context) {

    @Suppress("DEPRECATION")
    suspend fun getPlaceName(lat: Double, lon: Double): String? {
        return runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(lat, lon, 1) { list ->
                        val a = list.firstOrNull()
                        val name = a?.locality
                            ?: a?.subAdminArea
                            ?: a?.adminArea
                            ?: a?.featureName
                        cont.resume(name, onCancellation = null)
                    }
                }
            } else {
                val list = geocoder.getFromLocation(lat, lon, 1)
                val a = list?.firstOrNull()
                a?.locality ?: a?.subAdminArea ?: a?.adminArea ?: a?.featureName
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}

