package com.mapuia.khawchinthlirna.data

import kotlin.math.*

/**
 * Grid points used to pick nearest Firestore document id from user location.
 *
 * NOTE: The backend uses a 0.20 degree step grid from 22.30-24.50 lat and 92.40-94.40 lon.
 * This file provides utilities to find the nearest grid point that exists in Firestore.
 */
data class GridPoint(
    val lat: Double,
    val lon: Double,
    /** Firestore document id, e.g. "22.30_92.40" */
    val id: String,
)

/**
 * Grid bounds and step matching the backend's generate_grid function.
 */
private object GridConfig {
    const val LAT_START = 22.30
    const val LAT_END = 24.50
    const val LON_START = 92.40
    const val LON_END = 94.40
    const val STEP = 0.20
}

/**
 * Priority points from backend - these are added in addition to the regular grid.
 * Must match PRIORITY_POINTS in backend_v83_merged.py
 */
private val PRIORITY_POINTS = listOf(
    // CORE / REGIONAL
    Pair(23.73, 92.72), Pair(22.88, 92.73), Pair(22.47, 92.90), Pair(23.47, 93.33),
    Pair(24.35, 92.68), Pair(23.50, 92.80), Pair(22.16, 92.80), Pair(23.80, 92.50),
    Pair(23.63, 93.13), Pair(23.23, 92.83), Pair(23.53, 93.37),
    // KEY TOWNS
    Pair(24.50, 92.73), Pair(24.07, 92.68), Pair(23.28, 92.75), Pair(23.45, 92.75),
    Pair(23.83, 92.63), Pair(23.73, 92.58), Pair(24.23, 92.53), Pair(23.95, 93.58),
    Pair(23.37, 93.13), Pair(23.13, 93.38), Pair(22.93, 92.50), Pair(22.63, 92.45),
    Pair(22.28, 93.03), Pair(22.58, 92.95),
    // BORDER & VILLAGES
    Pair(23.37, 93.38), Pair(23.37, 93.36), Pair(23.36, 93.39), Pair(23.42, 93.35),
    Pair(23.18, 93.35), Pair(23.10, 93.32), Pair(23.15, 93.30), Pair(23.22, 93.35),
    Pair(23.65, 93.45),
    // KALAY & KABAW
    Pair(23.19, 94.05), Pair(23.22, 94.02), Pair(23.15, 94.03), Pair(23.21, 94.00),
    Pair(23.32, 94.07), Pair(23.35, 94.08), Pair(23.28, 94.06), Pair(23.05, 94.02),
    Pair(23.68, 94.15), Pair(23.55, 94.12), Pair(23.75, 94.18), Pair(24.22, 94.30),
    Pair(23.95, 94.22), Pair(23.45, 94.10),
    // FILLERS
    Pair(22.91, 93.68), Pair(22.30, 92.40), Pair(23.40, 93.40),
)

/**
 * Generate grid points that match the backend's 0.20 step grid + priority points.
 * This is computed dynamically to stay in sync with backend.
 */
val MIZORAM_GRID_POINTS: List<GridPoint> by lazy {
    val uniquePoints = mutableSetOf<Pair<Double, Double>>()

    // Add regular grid (0.20 step)
    var lat = GridConfig.LAT_START
    while (lat <= GridConfig.LAT_END + 0.001) {
        var lon = GridConfig.LON_START
        while (lon <= GridConfig.LON_END + 0.001) {
            uniquePoints.add(Pair(lat.roundTo2Decimals(), lon.roundTo2Decimals()))
            lon = (lon + GridConfig.STEP).roundTo2Decimals()
        }
        lat = (lat + GridConfig.STEP).roundTo2Decimals()
    }

    // Add priority points (matching backend)
    for ((pLat, pLon) in PRIORITY_POINTS) {
        uniquePoints.add(Pair(pLat.roundTo2Decimals(), pLon.roundTo2Decimals()))
    }

    // Convert to GridPoint list
    uniquePoints.map { (lat, lon) ->
        val id = "%.2f_%.2f".format(lat, lon)
        GridPoint(lat, lon, id)
    }.sortedWith(compareBy({ it.lat }, { it.lon }))
}

/**
 * Snap a coordinate to the nearest grid point (0.20 step).
 * This ensures we look up grid IDs that actually exist in Firestore.
 *
 * Uses integer arithmetic to avoid floating point precision issues.
 */
fun snapToGrid(value: Double, start: Double, step: Double): Double {
    // Convert to integer arithmetic to avoid floating point errors
    val startCents = (start * 100).roundToInt()
    val valueCents = (value * 100).roundToInt()
    val stepCents = (step * 100).roundToInt()

    val offsetCents = valueCents - startCents
    val steps = (offsetCents.toDouble() / stepCents).roundToInt()
    val snappedCents = startCents + (steps * stepCents)

    return snappedCents / 100.0
}

/**
 * Get the grid ID for a snapped coordinate pair.
 */
fun getSnappedGridId(lat: Double, lon: Double): String {
    val snappedLat = snapToGrid(lat, GridConfig.LAT_START, GridConfig.STEP)
        .coerceIn(GridConfig.LAT_START, GridConfig.LAT_END)
    val snappedLon = snapToGrid(lon, GridConfig.LON_START, GridConfig.STEP)
        .coerceIn(GridConfig.LON_START, GridConfig.LON_END)
    return "%.2f_%.2f".format(snappedLat, snappedLon)
}

/**
 * Find the nearest grid point ID for a user location.
 * Searches all points (both regular grid and priority points) by distance.
 */
fun nearestGridPointId(userLat: Double, userLon: Double, points: List<GridPoint> = MIZORAM_GRID_POINTS): String? {
    if (points.isEmpty()) return null

    // Find the nearest point by distance - this handles both regular grid and priority points
    return points.minByOrNull { haversineKm(userLat, userLon, it.lat, it.lon) }?.id
}

/**
 * Get nearby grid IDs for fallback search, sorted by distance.
 * Returns grid IDs that should exist in Firestore (both regular grid and priority points).
 */
fun getNearbyGridIds(lat: Double, lon: Double, maxDistanceKm: Double = 50.0): List<String> {
    // Search all known grid points (regular + priority) within max distance
    return MIZORAM_GRID_POINTS
        .map { point -> Pair(point.id, haversineKm(lat, lon, point.lat, point.lon)) }
        .filter { (_, distance) -> distance <= maxDistanceKm }
        .sortedBy { it.second }
        .map { it.first }
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a =
        sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2)
            .pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

private fun Double.roundTo2Decimals(): Double = (this * 100).roundToInt() / 100.0
