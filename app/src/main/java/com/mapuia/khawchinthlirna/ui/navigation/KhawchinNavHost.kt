package com.mapuia.khawchinthlirna.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mapuia.khawchinthlirna.MainScreen
import com.mapuia.khawchinthlirna.ui.screens.info.AppGuideScreen
import com.mapuia.khawchinthlirna.ui.screens.info.HowCrowdsourcingWorksScreen
import com.mapuia.khawchinthlirna.ui.screens.info.InfoHubScreen
import com.mapuia.khawchinthlirna.ui.screens.info.RainIntensityGuideScreen
import com.mapuia.khawchinthlirna.ui.screens.info.WeatherDataExplainedScreen
import com.mapuia.khawchinthlirna.ui.screens.report.NearbyReportsScreen
import com.mapuia.khawchinthlirna.ui.screens.report.ReportWeatherScreen
import com.mapuia.khawchinthlirna.data.model.NearbyReport

/**
 * Main navigation graph for the app
 */
@Composable
fun KhawchinNavHost(
    navController: NavHostController,
    userLat: Double?,
    userLon: Double?,
    userId: String,
    onSubmitReport: suspend (
        rainIntensity: Int,
        skyCondition: Int?,
        windStrength: Int?,
        notes: String?,
        locationName: String?,
    ) -> Result<Unit>,
    onFetchNearbyReports: suspend (lat: Double, lon: Double, radiusKm: Double, minutes: Int) -> List<NearbyReport>,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        // Main weather screen
        composable(Screen.Home.route) {
            MainScreen()
        }

        // Report weather screen
        composable(Screen.ReportWeather.route) {
            ReportWeatherScreen(
                userLat = userLat,
                userLon = userLon,
                userId = userId,
                onBack = { navController.popBackStack() },
                onSubmit = onSubmitReport,
                onNavigateToRainGuide = { navController.navigate(Screen.RainIntensityGuide.route) }
            )
        }

        // Nearby reports screen
        composable(Screen.NearbyReports.route) {
            NearbyReportsScreen(
                userLat = userLat,
                userLon = userLon,
                onBack = { navController.popBackStack() },
                onFetchReports = onFetchNearbyReports,
            )
        }

        // Info hub
        composable(Screen.InfoHub.route) {
            InfoHubScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAppGuide = { navController.navigate(Screen.AppGuide.route) },
                onNavigateToCrowdsourcing = { navController.navigate(Screen.HowCrowdsourcingWorks.route) },
                onNavigateToRainGuide = { navController.navigate(Screen.RainIntensityGuide.route) },
                onNavigateToWeatherData = { navController.navigate(Screen.WeatherDataExplained.route) },
            )
        }

        // App guide
        composable(Screen.AppGuide.route) {
            AppGuideScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // How crowdsourcing works
        composable(Screen.HowCrowdsourcingWorks.route) {
            HowCrowdsourcingWorksScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Rain intensity guide
        composable(Screen.RainIntensityGuide.route) {
            RainIntensityGuideScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Weather data explained
        composable(Screen.WeatherDataExplained.route) {
            WeatherDataExplainedScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Extension function to navigate and clear back stack
 */
fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

/**
 * Extension function for navigation actions
 */
fun NavHostController.navigateToReportWeather() = navigate(Screen.ReportWeather.route)
fun NavHostController.navigateToNearbyReports() = navigate(Screen.NearbyReports.route)
fun NavHostController.navigateToInfoHub() = navigate(Screen.InfoHub.route)
fun NavHostController.navigateToRainGuide() = navigate(Screen.RainIntensityGuide.route)
fun NavHostController.navigateToWeatherDataExplained() = navigate(Screen.WeatherDataExplained.route)
