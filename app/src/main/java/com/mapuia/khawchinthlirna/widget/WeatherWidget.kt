package com.mapuia.khawchinthlirna.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.text.*
import com.mapuia.khawchinthlirna.MainActivity

/**
 * Weather Widget using Jetpack Glance
 * Shows current weather for the home location
 */
class WeatherWidget : GlanceAppWidget() {

    companion object {
        private val LOCATION_KEY = stringPreferencesKey("widget_location")
        private val TEMP_KEY = stringPreferencesKey("widget_temp")
        private val CONDITION_KEY = stringPreferencesKey("widget_condition")
        private val CONDITION_EMOJI_KEY = stringPreferencesKey("widget_emoji")
        private val HUMIDITY_KEY = stringPreferencesKey("widget_humidity")
        private val LAST_UPDATED_KEY = stringPreferencesKey("widget_last_updated")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WeatherWidgetContent()
        }
    }

    @Composable
    private fun WeatherWidgetContent() {
        val prefs = currentState<Preferences>()
        
        val location = prefs[LOCATION_KEY] ?: "Aizawl"
        val temp = prefs[TEMP_KEY] ?: "--"
        val condition = prefs[CONDITION_KEY] ?: "Loading..."
        val emoji = prefs[CONDITION_EMOJI_KEY] ?: "🌤️"
        val humidity = prefs[HUMIDITY_KEY] ?: "--"
        val lastUpdated = prefs[LAST_UPDATED_KEY] ?: ""

        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.background)
                    .appWidgetBackground()
                    .cornerRadius(16.dp)
                    .clickable(actionStartActivity<MainActivity>())
                    .padding(12.dp)
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    // Location header
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📍 $location",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Main weather display
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Weather emoji
                        Text(
                            text = emoji,
                            style = TextStyle(fontSize = 40.sp)
                        )

                        Spacer(modifier = GlanceModifier.width(12.dp))

                        // Temperature and condition
                        Column {
                            Text(
                                text = "${temp}°",
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = condition,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Humidity
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💧",
                            style = TextStyle(fontSize = 12.sp)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = "${humidity}%",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    // Last updated and refresh
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (lastUpdated.isNotEmpty()) {
                            Text(
                                text = lastUpdated,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            )
                            Spacer(modifier = GlanceModifier.width(8.dp))
                        }
                        
                        Box(
                            modifier = GlanceModifier
                                .size(24.dp)
                                .cornerRadius(12.dp)
                                .background(GlanceTheme.colors.primaryContainer)
                                .clickable(actionRunCallback<RefreshWeatherAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🔄",
                                style = TextStyle(fontSize = 12.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Widget receiver
 */
class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()
}

/**
 * Refresh weather action callback
 */
class RefreshWeatherAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // Trigger weather update
        WeatherWidgetUpdater.updateWidget(context)
    }
}

/**
 * Helper object to update widget from anywhere in the app
 */
object WeatherWidgetUpdater {
    private val LOCATION_KEY = stringPreferencesKey("widget_location")
    private val TEMP_KEY = stringPreferencesKey("widget_temp")
    private val CONDITION_KEY = stringPreferencesKey("widget_condition")
    private val CONDITION_EMOJI_KEY = stringPreferencesKey("widget_emoji")
    private val HUMIDITY_KEY = stringPreferencesKey("widget_humidity")
    private val LAST_UPDATED_KEY = stringPreferencesKey("widget_last_updated")

    suspend fun updateWidget(context: Context) {
        // This would be called from repository when weather data changes
        // For now, just trigger a refresh
        WeatherWidget().updateAll(context)
    }

    suspend fun updateWidgetData(
        context: Context,
        location: String,
        temperature: String,
        condition: String,
        conditionEmoji: String,
        humidity: String,
        lastUpdated: String
    ) {
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(WeatherWidget::class.java)
        
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[LOCATION_KEY] = location
                prefs[TEMP_KEY] = temperature
                prefs[CONDITION_KEY] = condition
                prefs[CONDITION_EMOJI_KEY] = conditionEmoji
                prefs[HUMIDITY_KEY] = humidity
                prefs[LAST_UPDATED_KEY] = lastUpdated
            }
            WeatherWidget().update(context, glanceId)
        }
    }

    /**
     * Get weather emoji based on condition code
     */
    fun getWeatherEmoji(conditionCode: Int?): String {
        return when (conditionCode) {
            1000 -> "☀️" // Sunny
            1003 -> "🌤️" // Partly cloudy
            1006 -> "☁️" // Cloudy
            1009 -> "☁️" // Overcast
            1030 -> "🌫️" // Mist
            1063, 1150, 1153, 1180, 1183 -> "🌦️" // Light rain
            1186, 1189, 1192, 1195 -> "🌧️" // Rain
            1087, 1273, 1276 -> "⛈️" // Thunderstorm
            1066, 1114, 1210, 1213, 1216, 1219, 1222, 1225 -> "🌨️" // Snow
            1135, 1147 -> "🌫️" // Fog
            else -> "🌤️"
        }
    }
}
