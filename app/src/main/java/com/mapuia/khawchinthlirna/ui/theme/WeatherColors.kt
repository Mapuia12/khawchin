package com.mapuia.khawchinthlirna.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Vibrant, eye-catching weather color schemes
 * Premium weather app design - bold and colorful
 */
object WeatherColorSchemes {
    
    // Sunny/Clear Day - Brilliant warm gradient
    val SunnyGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF00B4DB), // Bright Cyan
            Color(0xFF0083B0), // Deep Ocean Blue
            Color(0xFF48C6EF), // Sky Blue
        )
    )

    // Sunny Premium - Golden hour vibes
    val SunnyPremiumGradient = Brush.verticalGradient(
        listOf(
            Color(0xFFFF512F), // Vibrant Orange
            Color(0xFFDD2476), // Hot Pink
        )
    )
    
    // Rainy Weather - Deep blue moody
    val RainyGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F2027), // Deep Dark
            Color(0xFF203A43), // Ocean Dark
            Color(0xFF2C5364), // Steel Blue
        )
    )
    
    // Cloudy Weather - Soft purple-blue
    val CloudyGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF4776E6), // Royal Blue
            Color(0xFF8E54E9), // Purple
        )
    )
    
    // Night Time - Deep cosmic purple
    val NightGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F0C29), // Deep Dark Purple
            Color(0xFF302B63), // Cosmic Purple
            Color(0xFF24243E), // Dark Indigo
        )
    )
    
    // Night Premium - Aurora vibes
    val NightPremiumGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0B486B), // Deep Ocean
            Color(0xFFF56217), // Sunset Orange (accent at bottom)
        )
    )

    // Stormy Weather - Dramatic purple-teal
    val StormyGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF141E30), // Dark Blue Black
            Color(0xFF243B55), // Storm Blue
            Color(0xFF6441A5), // Thunder Purple
        )
    )
    
    // Misty/Foggy - Soft elegant grey-blue
    val MistyGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF2C3E50), // Wet Asphalt
            Color(0xFF4CA1AF), // Teal
        )
    )

    // Snow/Cold - Icy blue-white
    val SnowGradient = Brush.verticalGradient(
        listOf(
            Color(0xFFE6DADA), // Light Pink Grey
            Color(0xFF274046), // Cold Dark Teal
        )
    )

    // Premium App Background - Default vibrant
    val PremiumDayGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF667eea), // Indigo
            Color(0xFF764ba2), // Purple
        )
    )

    val PremiumNightGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F2027), // Deep Dark
            Color(0xFF203A43), // Ocean Dark
            Color(0xFF2C5364), // Steel Blue
        )
    )

    // Accent Colors for UI elements
    object Accents {
        val Cyan = Color(0xFF00D4FF)
        val Orange = Color(0xFFFF6B35)
        val Pink = Color(0xFFFF006E)
        val Purple = Color(0xFF8338EC)
        val Green = Color(0xFF06D6A0)
        val Yellow = Color(0xFFFFBE0B)
        val Red = Color(0xFFFF4757)
        val Blue = Color(0xFF3A86FF)
    }

    // Card Accent Gradients
    val WindAccent = Brush.linearGradient(listOf(Color(0xFF00D4FF), Color(0xFF00B4DB)))
    val RainAccent = Brush.linearGradient(listOf(Color(0xFF3A86FF), Color(0xFF0066FF)))
    val PressureAccent = Brush.linearGradient(listOf(Color(0xFFFF006E), Color(0xFF8338EC)))
    val HumidityAccent = Brush.linearGradient(listOf(Color(0xFF06D6A0), Color(0xFF00B894)))
    val TempHotAccent = Brush.linearGradient(listOf(Color(0xFFFF512F), Color(0xFFDD2476)))
    val TempColdAccent = Brush.linearGradient(listOf(Color(0xFF00D4FF), Color(0xFF7F7FD5)))
}

object WeatherPalette {
    val SkyCloudyDay = listOf(Color(0xFF3A4E6B), Color(0xFF4F6485), Color(0xFF667BA0))
    val SkyClearDay = listOf(Color(0xFF0B4F9E), Color(0xFF1A8EDB), Color(0xFF3BB6EE))
    val SkyWarmDay = listOf(Color(0xFF1A5276), Color(0xFF2980B9), Color(0xFF5DADE2))
    val SkyHotDay = listOf(Color(0xFF172033), Color(0xFF7C2D12), Color(0xFFC2410C), Color(0xFF1D4ED8))
    val SkyColdDay = listOf(Color(0xFF1B2A4A), Color(0xFF2E5077), Color(0xFF7FBBDA))
    val SkyRain = listOf(Color(0xFF1C2B4A), Color(0xFF253762), Color(0xFF2E4880))
    val SkyStorm = listOf(Color(0xFF100B2C), Color(0xFF1D1450), Color(0xFF2E1E78))
    val SkyFog = listOf(Color(0xFF2C3E50), Color(0xFF3D5166), Color(0xFF4CA1AF))
    val SkySunrise = listOf(Color(0xFF312E81), Color(0xFF9A3412), Color(0xFFEA580C), Color(0xFF2563EB))
    val SkySunset = listOf(Color(0xFF1A0A2E), Color(0xFF8338EC), Color(0xFFFF6B35))
    val SkyNight = listOf(Color(0xFF050B24), Color(0xFF17206A), Color(0xFF3B2B86), Color(0xFF0F766E))
    val SkyDeepNight = listOf(Color(0xFF020617), Color(0xFF0B1B46), Color(0xFF25135F), Color(0xFF0B5F6A))
}

fun getWeatherGradient(weatherCode: Int, isDay: Boolean): Brush {
    return when {
        !isDay -> WeatherColorSchemes.NightGradient
        weatherCode == 0 -> WeatherColorSchemes.SunnyGradient // Clear
        weatherCode in 1..3 -> WeatherColorSchemes.CloudyGradient // Partly cloudy
        weatherCode in 45..48 -> WeatherColorSchemes.MistyGradient // Fog
        weatherCode in 51..67 -> WeatherColorSchemes.RainyGradient // Rain
        weatherCode in 71..77 -> WeatherColorSchemes.SnowGradient // Snow
        weatherCode in 85..86 -> WeatherColorSchemes.SnowGradient // Snow showers
        weatherCode in 95..99 -> WeatherColorSchemes.StormyGradient // Thunderstorm
        else -> WeatherColorSchemes.CloudyGradient
    }
}

fun getFullAppGradient(
    weatherCode: Int,
    isDay: Boolean,
    tempC: Double,
    hourOfDay: Int,
): Brush {
    if (!isDay) {
        return Brush.verticalGradient(
            if (hourOfDay in 22..23 || hourOfDay in 0..3) {
                WeatherPalette.SkyDeepNight
            } else {
                WeatherPalette.SkyNight
            }
        )
    }

    if (hourOfDay in 5..7) return Brush.verticalGradient(WeatherPalette.SkySunrise)
    if (hourOfDay in 17..19) return Brush.verticalGradient(WeatherPalette.SkySunset)

    return when {
        weatherCode in 95..99 -> Brush.verticalGradient(WeatherPalette.SkyStorm)
        weatherCode in 51..82 -> Brush.verticalGradient(WeatherPalette.SkyRain)
        weatherCode in 45..48 -> Brush.verticalGradient(WeatherPalette.SkyFog)
        weatherCode in 1..3 -> Brush.verticalGradient(WeatherPalette.SkyCloudyDay)
        tempC >= 35.0 -> Brush.verticalGradient(WeatherPalette.SkyHotDay)
        tempC <= 14.0 -> Brush.verticalGradient(WeatherPalette.SkyColdDay)
        tempC in 15.0..24.0 -> Brush.verticalGradient(WeatherPalette.SkyClearDay)
        else -> Brush.verticalGradient(WeatherPalette.SkyWarmDay)
    }
}

/**
 * Get dynamic background based on weather and time
 */
fun getDynamicBackground(weatherCode: Int, isDay: Boolean, hour: Int = 12): Brush {
    // Golden hour effect (6-7am and 5-7pm)
    val isGoldenHour = hour in 6..7 || hour in 17..19

    return when {
        !isDay -> WeatherColorSchemes.NightPremiumGradient
        isGoldenHour && weatherCode == 0 -> WeatherColorSchemes.SunnyPremiumGradient
        else -> getWeatherGradient(weatherCode, isDay)
    }
}
