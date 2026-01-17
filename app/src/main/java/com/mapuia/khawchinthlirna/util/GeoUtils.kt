package com.mapuia.khawchinthlirna.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geographic utility functions.
 * Provides consistent distance calculation across the app.
 */
object GeoUtils {
    private const val EARTH_RADIUS_KM = 6371.0
    
    /**
     * Calculate distance between two geographic points using Haversine formula.
     * @param lat1 Latitude of first point in degrees
     * @param lon1 Longitude of first point in degrees
     * @param lat2 Latitude of second point in degrees
     * @param lon2 Longitude of second point in degrees
     * @return Distance in kilometers
     */
    fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + 
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * 
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_KM * c
    }
    
    /**
     * Calculate bounding box around a point for efficient geospatial queries.
     * @param lat Center latitude
     * @param lon Center longitude
     * @param radiusKm Radius in kilometers
     * @return BoundingBox with min/max lat/lon values
     */
    fun getBoundingBox(lat: Double, lon: Double, radiusKm: Double): BoundingBox {
        // Approximate: 111km per degree latitude
        val latDelta = radiusKm / 111.0
        // Adjust for longitude based on latitude
        val lonDelta = radiusKm / (111.0 * cos(Math.toRadians(lat)))
        
        return BoundingBox(
            minLat = lat - latDelta,
            maxLat = lat + latDelta,
            minLon = lon - lonDelta,
            maxLon = lon + lonDelta
        )
    }
    
    /**
     * Check if a point is within Mizoram bounds.
     */
    fun isInMizoramRegion(lat: Double, lon: Double): Boolean {
        return lat in 21.0..26.0 && lon in 91.0..96.0
    }
}

/**
 * Represents a geographic bounding box.
 */
data class BoundingBox(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
) {
    fun contains(lat: Double, lon: Double): Boolean {
        return lat in minLat..maxLat && lon in minLon..maxLon
    }
}
