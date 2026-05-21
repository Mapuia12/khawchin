package com.mapuia.khawchinthlirna.data

import java.util.Locale

data class FocusLocation(
    val id: String,
    val name: String,
    val nameMz: String,
    val lat: Double,
    val lon: Double,
    val category: String,
) {
    val gridId: String
        get() = String.format(Locale.US, "%.2f_%.2f", lat, lon)
}

object FocusLocations {
    val all: List<FocusLocation> = listOf(
        FocusLocation("aizawl", "Aizawl", "Aizawl", 23.73, 92.72, "District"),
        FocusLocation("lunglei", "Lunglei", "Lunglei", 22.88, 92.73, "District"),
        FocusLocation("champhai", "Champhai", "Champhai", 23.47, 93.33, "District"),
        FocusLocation("serchhip", "Serchhip", "Serchhip", 23.30, 92.85, "District"),
        FocusLocation("kolasib", "Kolasib", "Kolasib", 24.22, 92.68, "District"),
        FocusLocation("lawngtlai", "Lawngtlai", "Lawngtlai", 22.53, 92.90, "District"),
        FocusLocation("mamit", "Mamit", "Mamit", 23.92, 92.49, "District"),
        FocusLocation("saitual", "Saitual", "Saitual", 23.56, 92.92, "District"),
        FocusLocation("hnahthial", "Hnahthial", "Hnahthial", 22.70, 92.78, "District"),
        FocusLocation("saiha", "Saiha", "Saiha", 22.49, 92.98, "District"),
        FocusLocation("khawzawl", "Khawzawl", "Khawzawl", 23.38, 93.15, "District"),
        FocusLocation("vairengte", "Vairengte", "Vairengte", 24.49, 92.76, "Town"),
        FocusLocation("rihkhawdar", "Rih Khawdar", "Rih Khawdar", 23.31, 93.39, "Border POI"),
        FocusLocation("kalemyo", "Kalemyo", "Kalemyo", 23.19, 94.05, "City"),
        FocusLocation("tahan", "Tahan", "Tahan", 23.20, 94.02, "POI"),
        FocusLocation("tamu", "Tamu", "Tamu", 24.22, 94.30, "Border Town"),
        FocusLocation("letpanchaung", "Letpanchaung", "Letpanchaung", 23.33, 94.03, "POI"),
        FocusLocation("hmuntha", "Hmuntha", "Hmuntha", 23.67, 94.14, "POI"),
        FocusLocation("kanan", "Kanan", "Kanan", 23.81, 94.15, "POI"),
        FocusLocation("khawmawi", "Khawmawi", "Khawmawi", 23.36, 93.39, "POI"),
        FocusLocation("melbuk", "Melbuk", "Melbuk", 23.39, 93.38, "POI"),
        FocusLocation("new_haimual", "New Haimual", "New Haimual", 23.38, 93.41, "POI"),
    )

    fun search(query: String, isMizo: Boolean): List<FocusLocation> {
        val normalized = query.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return all
        return all.filter { location ->
            val primary = if (isMizo) location.nameMz else location.name
            primary.lowercase(Locale.ROOT).contains(normalized) ||
                location.name.lowercase(Locale.ROOT).contains(normalized) ||
                location.nameMz.lowercase(Locale.ROOT).contains(normalized) ||
                location.category.lowercase(Locale.ROOT).contains(normalized)
        }
    }
}
