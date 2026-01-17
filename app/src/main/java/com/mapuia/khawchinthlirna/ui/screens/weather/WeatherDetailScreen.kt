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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapuia.khawchinthlirna.data.model.GridMetaData
import com.mapuia.khawchinthlirna.data.model.GridWeatherDocument
import com.mapuia.khawchinthlirna.data.model.HourlyWeatherItem
import com.mapuia.khawchinthlirna.data.model.MarineRiskData
import com.mapuia.khawchinthlirna.data.model.formatTimestamp
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
    modifier: Modifier = Modifier
) {
    val hourlyList = weatherData.toHourlyWeatherList()
    val currentHour = hourlyList.firstOrNull()

    val backgroundGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F0C29),
            Color(0xFF302B63),
            Color(0xFF24243E),
        )
    )

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
                            text = "Weather Details",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToWeatherDataExplained) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Weather data help",
                                tint = Color.White
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
                        marine = weatherData.marine
                    )
                }

                // Hourly forecast title
                item {
                    Text(
                        text = "Hourly Forecast",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
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
                        text = "Detailed Forecast",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }

                items(hourlyList.take(24)) { hour ->
                    HourlyWeatherRow(hour = hour)
                }

                // Data source info
                item {
                    DataSourceInfo(
                        generated = weatherData.generated,
                        models = weatherData.modelsUsed
                    )
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
    marine: MarineRiskData
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                current?.feelsLike?.let { feelsLike ->
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Feels like",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                        Text(
                            text = "${feelsLike.roundToInt()}°",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // Condition description
            current?.let {
                Text(
                    text = it.getConditionDescription(),
                    color = Color.White.copy(alpha = 0.8f),
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
                    label = "Ruah"
                )

                // Wind with gust
                WeatherStat(
                    icon = Icons.Default.Air,
                    value = "${current?.windSpeed?.roundToInt() ?: 0} km/h",
                    label = current?.windGust?.let { "Gust: ${it.roundToInt()}" } ?: "Thli"
                )

                // Humidity
                WeatherStat(
                    icon = Icons.Default.WaterDrop,
                    value = "${current?.humidity ?: 0}%",
                    label = "Humidity"
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
                        label = "Pressure"
                    )
                }

                // Visibility
                current?.visibility?.let { _ ->
                    WeatherStat(
                        icon = Icons.Default.Visibility,
                        value = current.formatVisibility(),
                        label = "A lang dan"
                    )
                }

                // Cloud cover
                current?.cloudCover?.let { cloud ->
                    WeatherStat(
                        icon = Icons.Default.Cloud,
                        value = "$cloud%",
                        label = "Sum"
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
                    text = "📍 Elevation: ${meta.elevationM.roundToInt()}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                current?.dewpoint?.let { dewpoint ->
                    Text(
                        text = "💧 Dew point: ${dewpoint.roundToInt()}°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Marine risk (if significant)
            if (marine.isSignificant()) {
                Spacer(modifier = Modifier.height(12.dp))
                MarineRiskBanner(marine = marine)
            }
        }
    }
}

@Composable
fun WeatherStat(
    icon: ImageVector,
    value: String,
    label: String,
    valueColor: Color = Color.White,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
        )
    }
}

@Composable
fun MarineRiskBanner(marine: MarineRiskData) {
    val (backgroundColor, text) = when (marine.level) {
        "YELLOW" -> Color(0xFFFFF3CD) to "⚠️ Marine Caution - A fimkhur tur"
        "ORANGE" -> Color(0xFFFFE5CC) to "🟠 Marine Warning - Tuifinriat ah a him lo"
        "RED" -> Color(0xFFFFCCCC) to "🔴 Marine Danger - Tuifinriat ah chhuah ngai lo!"
        else -> Color.Transparent to ""
    }

    if (text.isNotEmpty()) {
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.8f),
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
                color = Color.White.copy(alpha = 0.6f),
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
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )

            // Feels like (smaller)
            hour.feelsLike?.let { fl ->
                Text(
                    text = "~${fl.roundToInt()}°",
                    color = Color.White.copy(alpha = 0.5f),
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
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
fun HourlyWeatherRow(hour: HourlyWeatherItem) {
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
                color = Color.White.copy(alpha = 0.8f),
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                hour.feelsLike?.let {
                    Text(
                        text = "~${it.roundToInt()}°",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
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
                    color = Color.White.copy(alpha = 0.8f),
                )
            }

            // Wind
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = "${hour.windSpeed.roundToInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }

            // UV (if daytime and > 0)
            hour.uvIndex?.takeIf { it > 0 }?.let { uv ->
                Text(
                    text = "UV ${uv.roundToInt()}",
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
    models: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Data Source",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Generated: ${formatTimestamp(generated)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
            )
            if (models.isNotEmpty()) {
                Text(
                    text = "Models: ${models.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
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
