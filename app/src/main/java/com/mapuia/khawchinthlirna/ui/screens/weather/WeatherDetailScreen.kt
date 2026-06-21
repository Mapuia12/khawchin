package com.mapuia.khawchinthlirna.ui.screens.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapuia.khawchinthlirna.R
import com.mapuia.khawchinthlirna.data.model.GridMetaData
import com.mapuia.khawchinthlirna.data.model.GridWeatherDocument
import com.mapuia.khawchinthlirna.data.model.HourlyWeatherItem
import com.mapuia.khawchinthlirna.data.model.MarineRiskData
import com.mapuia.khawchinthlirna.data.model.formatTimestamp
import com.mapuia.khawchinthlirna.ui.components.BannerAd
import com.mapuia.khawchinthlirna.ui.theme.appBackgroundGradient
import com.mapuia.khawchinthlirna.ui.theme.appIconTint
import com.mapuia.khawchinthlirna.ui.theme.appTextMuted
import com.mapuia.khawchinthlirna.ui.theme.appTextPrimary
import com.mapuia.khawchinthlirna.ui.theme.appTextSecondary
import kotlin.math.roundToInt

/**
 * Detailed weather view showing all available weather data including:
 * - Current conditions with feels like temperature
 * - UV index with color coding
 * - Visibility
 * - Marine risk
 * - Pressure trends
 * - Hourly forecast with all details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailScreen(
    weatherData: GridWeatherDocument,
    onBack: () -> Unit,
    onNavigateToWeatherDataExplained: () -> Unit = {},
    modifier: Modifier = Modifier,
    isMizo: Boolean = true,
) {
    val hourlyList = weatherData.toHourlyWeatherList()
    val currentHour = hourlyList.firstOrNull()

    val backgroundGradient = appBackgroundGradient()
    val textPrimary = appTextPrimary()
    val textSecondary = appTextSecondary(0.8f)
    val textMuted = appTextMuted(0.6f)
    val iconTint = appIconTint()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = langString(
                                R.string.weather_details_title_mz,
                                R.string.weather_details_title_en,
                                isMizo
                            ),
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = langString(
                                    R.string.ui_back_mz,
                                    R.string.ui_back_en,
                                    isMizo
                                ),
                                tint = iconTint
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToWeatherDataExplained) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = langString(
                                    R.string.weather_details_help_mz,
                                    R.string.weather_details_help_en,
                                    isMizo
                                ),
                                tint = iconTint
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current conditions card
                item {
                    CurrentConditionsCard(
                        current = currentHour,
                        meta = weatherData.meta,
                        marine = weatherData.marine,
                        isMizo = isMizo,
                    )
                }

                // Hourly forecast title
                item {
                    Text(
                        text = langString(
                            R.string.weather_hourly_forecast_mz,
                            R.string.weather_hourly_forecast_en,
                            isMizo
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Hourly forecast row
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(hourlyList.take(24)) { hour ->
                            HourlyWeatherCard(hour = hour)
                        }
                    }
                }

                // Detailed hourly list
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = langString(
                            R.string.weather_detailed_forecast_mz,
                            R.string.weather_detailed_forecast_en,
                            isMizo
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                items(hourlyList.take(24)) { hour ->
                    HourlyWeatherRow(hour = hour, isMizo = isMizo)
                }

                // Data source info
                item {
                    DataSourceInfo(
                        generated = weatherData.generated,
                        models = weatherData.modelsUsed,
                        isMizo = isMizo,
                    )
                }

                // Banner Ad
                item {
                    BannerAd(modifier = Modifier.fillMaxWidth())
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun CurrentConditionsCard(
    current: HourlyWeatherItem?,
    meta: GridMetaData,
    marine: MarineRiskData,
    isMizo: Boolean = true,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Temperature with feels like
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${current?.temperature?.roundToInt() ?: "--"}°",
                    style = MaterialTheme.typography.displayLarge,
                    color = appTextPrimary(),
                    fontWeight = FontWeight.Bold,
                )
                current?.feelsLike?.let { feelsLike ->
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = langString(
                                R.string.weather_feels_like_mz,
                                R.string.weather_feels_like_en,
                                isMizo
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = appTextSecondary(0.6f),
                        )
                        Text(
                            text = "${feelsLike.roundToInt()}°",
                            style = MaterialTheme.typography.titleLarge,
                            color = appTextPrimary(0.9f),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // Condition description
            current?.let {
                Text(
                    text = it.getConditionDescription(),
                    color = appTextSecondary(0.8f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weather details grid - Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Rain probability
                WeatherStat(
                    icon = Icons.Default.WaterDrop,
                    value = "${current?.precipitationProbability ?: 0}%",
                    label = langString(
                        R.string.weather_stat_rain_mz,
                        R.string.weather_stat_rain_en,
                        isMizo
                    )
                )

                // Wind with gust
                WeatherStat(
                    icon = Icons.Default.Air,
                    value = "${current?.windSpeed?.roundToInt() ?: 0} km/h",
                    label = current?.windGust?.let {
                        stringResource(
                            if (isMizo) R.string.weather_gust_label_mz else R.string.weather_gust_label_en,
                            it.roundToInt()
                        )
                    } ?: langString(
                        R.string.weather_stat_wind_mz,
                        R.string.weather_stat_wind_en,
                        isMizo
                    )
                )

                // Humidity
                WeatherStat(
                    icon = Icons.Default.WaterDrop,
                    value = "${current?.humidity ?: 0}%",
                    label = langString(
                        R.string.weather_stat_humidity_mz,
                        R.string.weather_stat_humidity_en,
                        isMizo
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weather details grid - Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // UV Index
                current?.uvIndex?.let { uv ->
                    WeatherStat(
                        icon = Icons.Default.WbSunny,
                        value = uv.roundToInt().toString(),
                        label = current.getUvLevelMizo(),
                        valueColor = Color(current.getUvColor())
                    )
                }

                // Pressure
                current?.pressure?.let { pressure ->
                    WeatherStat(
                        icon = Icons.Default.Speed,
                        value = "${pressure.roundToInt()} hPa",
                        label = langString(
                            R.string.weather_stat_pressure_mz,
                            R.string.weather_stat_pressure_en,
                            isMizo
                        )
                    )
                }

                // Visibility
                current?.visibility?.let { _ ->
                    WeatherStat(
                        icon = Icons.Default.Visibility,
                        value = current.formatVisibility(),
                        label = langString(
                            R.string.weather_stat_visibility_mz,
                            R.string.weather_stat_visibility_en,
                            isMizo
                        )
                    )
                }

                // Cloud cover
                current?.cloudCover?.let { cloud ->
                    WeatherStat(
                        icon = Icons.Default.Cloud,
                        value = "$cloud%",
                        label = langString(
                            R.string.weather_stat_clouds_mz,
                            R.string.weather_stat_clouds_en,
                            isMizo
                        )
                    )
                }
            }

            // Elevation info
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(
                        if (isMizo) R.string.weather_elevation_label_mz else R.string.weather_elevation_label_en,
                        meta.elevationM.roundToInt()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = appTextSecondary(0.6f)
                )
                current?.dewpoint?.let { dewpoint ->
                    Text(
                        text = stringResource(
                            if (isMizo) R.string.weather_dew_point_label_mz else R.string.weather_dew_point_label_en,
                            dewpoint.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = appTextSecondary(0.6f)
                    )
                }
            }

            // Marine risk (if significant)
            if (marine.isSignificant()) {
                Spacer(modifier = Modifier.height(12.dp))
                MarineRiskBanner(marine = marine, isMizo = isMizo)
            }
        }
    }
}

@Composable
fun WeatherStat(
    icon: ImageVector,
    value: String,
    label: String,
    valueColor: Color? = null,
) {
    val resolvedValueColor = valueColor ?: appTextPrimary()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = appIconTint(0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = resolvedValueColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
        Text(
            text = label,
            color = appTextMuted(0.5f),
            fontSize = 11.sp,
        )
    }
}

@Composable
fun MarineRiskBanner(
    marine: MarineRiskData,
    isMizo: Boolean = true,
) {
    val (backgroundColor, textRes) = when (marine.level) {
        "YELLOW" -> Color(0xFFFFF3CD) to if (isMizo) {
            R.string.weather_marine_yellow_mz
        } else {
            R.string.weather_marine_yellow_en
        }
        "ORANGE" -> Color(0xFFFFE5CC) to if (isMizo) {
            R.string.weather_marine_orange_mz
        } else {
            R.string.weather_marine_orange_en
        }
        "RED" -> Color(0xFFFFCCCC) to if (isMizo) {
            R.string.weather_marine_red_mz
        } else {
            R.string.weather_marine_red_en
        }
        else -> Color.Transparent to 0
    }

    if (textRes != 0) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(textRes),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black, // Intentionally black for yellow/orange/red warning backgrounds
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun HourlyWeatherCard(hour: HourlyWeatherItem) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = hour.formatHour(),
                color = appTextSecondary(0.6f),
                fontSize = 12.sp,
            )

            // Weather emoji based on conditions
            Text(
                text = getWeatherEmoji(hour),
                fontSize = 24.sp,
            )

            // Temperature
            Text(
                text = "${hour.temperature.roundToInt()}°",
                color = appTextPrimary(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )

            // Feels like (smaller)
            hour.feelsLike?.let { fl ->
                Text(
                    text = "~${fl.roundToInt()}°",
                    color = appTextMuted(0.5f),
                    fontSize = 11.sp,
                )
            }

            // Rain probability
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WaterDrop,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color(0xFF3A86FF)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "${hour.precipitationProbability}%",
                    color = appTextSecondary(0.7f),
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
fun HourlyWeatherRow(
    hour: HourlyWeatherItem,
    isMizo: Boolean = true,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.06f)
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time
            Text(
                text = hour.formatHour(),
                style = MaterialTheme.typography.bodyMedium,
                color = appTextSecondary(0.8f),
                modifier = Modifier.width(60.dp)
            )

            // Weather icon
            Text(
                text = getWeatherEmoji(hour),
                fontSize = 24.sp,
            )

            // Temperature with feels like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${hour.temperature.roundToInt()}°",
                    style = MaterialTheme.typography.bodyLarge,
                    color = appTextPrimary(),
                    fontWeight = FontWeight.Bold,
                )
                hour.feelsLike?.let {
                    Text(
                        text = "~${it.roundToInt()}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = appTextMuted(0.5f)
                    )
                }
            }

            // Rain
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFF2196F3)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "${hour.precipitationProbability}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = appTextSecondary(0.8f),
                )
            }

            // Wind
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = appIconTint(0.6f)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "${hour.windSpeed.roundToInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = appTextSecondary(0.7f),
                )
            }

            // UV (if daytime and > 0)
            hour.uvIndex?.takeIf { it > 0 }?.let { uv ->
                Text(
                    text = stringResource(
                        if (isMizo) R.string.weather_uv_label_mz else R.string.weather_uv_label_en,
                        uv.roundToInt()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(hour.getUvColor()),
                    fontWeight = FontWeight.Medium,
                )
            } ?: Spacer(Modifier.width(40.dp))
        }
    }
}

@Composable
fun DataSourceInfo(
    generated: String,
    models: List<String>,
    isMizo: Boolean = true,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🛰️", fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = langString(
                        R.string.weather_data_source_title_mz,
                        R.string.weather_data_source_title_en,
                        isMizo
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = appTextSecondary(0.6f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    if (isMizo) R.string.weather_data_source_generated_mz else R.string.weather_data_source_generated_en,
                    formatTimestamp(generated)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = appTextMuted(0.5f),
            )
            if (models.isNotEmpty()) {
                Text(
                    text = stringResource(
                        if (isMizo) R.string.weather_data_source_models_mz else R.string.weather_data_source_models_en,
                        models.joinToString(", ")
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = appTextMuted(0.5f),
                )
            }
        }
    }
}

/**
 * Get weather emoji based on conditions
 */
private fun getWeatherEmoji(hour: HourlyWeatherItem): String {
    val rainMm = hour.precipitation
    val cloud = hour.cloudCover ?: 0
    val uv = hour.uvIndex ?: 0.0

    return when {
        rainMm > 25 -> "⛈️"
        rainMm > 10 -> "🌧️"
        rainMm > 2.5 -> "🌧️"
        rainMm > 0 -> "🌦️"
        hour.visibility?.let { it < 1000 } == true -> "🌫️"
        cloud >= 80 -> "☁️"
        cloud >= 50 -> "🌥️"
        cloud >= 20 -> "⛅"
        uv > 0 -> "☀️"
        else -> "🌙"
    }
}

@Composable
private fun langString(
    @StringRes mizoRes: Int,
    @StringRes englishRes: Int,
    isMizo: Boolean,
): String {
    return stringResource(if (isMizo) mizoRes else englishRes)
}
