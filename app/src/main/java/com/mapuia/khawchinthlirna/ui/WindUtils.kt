package com.mapuia.khawchinthlirna.ui

/** Convert degrees (0..360) into 8-wind compass label. */
fun windDirLabel(deg: Int?): String? {
    val d = deg ?: return null
    val normalized = ((d % 360) + 360) % 360
    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val idx = ((normalized + 22) / 45) % 8
    return dirs[idx]
}

