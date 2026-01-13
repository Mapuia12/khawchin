package com.mapuia.khawchinthlirna.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Small abstraction so the ViewModel isn't coupled directly to the fused client API. */
class LocationProvider(context: Context) {
    private val fused = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getLastBestLocation(): Location? {
        return try {
            withTimeoutOrNull(WeatherConstants.LOCATION_TIMEOUT_MS) {
                val last = fused.lastLocation.await()
                last ?: fused.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    null,
                ).await()
            }
        } catch (_: Throwable) {
            null
        }
    }
}

