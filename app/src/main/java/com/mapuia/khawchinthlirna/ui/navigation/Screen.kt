package com.mapuia.khawchinthlirna.ui.navigation

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    // Main screens
    data object Home : Screen("home")
    data object ReportWeather : Screen("report_weather")
    data object NearbyReports : Screen("nearby_reports")
    data object WeatherDetail : Screen("weather_detail")

    // Info screens
    data object HowCrowdsourcingWorks : Screen("info/crowdsourcing")
    data object RainIntensityGuide : Screen("info/rain_guide")
    data object WeatherDataExplained : Screen("info/weather_data")
    data object AppGuide : Screen("info/app_guide")

    // Info hub
    data object InfoHub : Screen("info")
}

/**
 * Navigation actions
 */
object NavActions {
    const val ARG_USER_LAT = "userLat"
    const val ARG_USER_LON = "userLon"
    const val ARG_USER_ID = "userId"
}

