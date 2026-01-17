package com.mapuia.khawchinthlirna.ui.screens.info

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDataExplainedScreen(
    onBack: () -> Unit,
) {
    val backgroundGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F0C29),
            Color(0xFF302B63),
            Color(0xFF24243E),
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Weather Data Explained",
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Temperature
                WeatherMetricCard(
                    icon = Icons.Default.Thermostat,
                    iconColor = Color(0xFFFF6B6B),
                    title = "Temperature (°C)",
                    titleMizo = "A lum/vawh dan",
                    description = "Actual air temperature measured at your location.",
                    descriptionMizo = "I awmnaa boruak a lum/vawh dan dik tak a ni.",
                    tip = "\"Feels like\" includes wind chill and humidity effects - a lum dan tak i hre tur a ni."
                )

                // Precipitation Probability
                WeatherMetricCard(
                    icon = Icons.Default.WaterDrop,
                    iconColor = Color(0xFF3A86FF),
                    title = "Precipitation Probability (%)",
                    titleMizo = "Ruah sur thei chance",
                    description = "The chance of rain occurring in that hour.",
                    descriptionMizo = "Darkar khat chhunga ruah sur thei chance a ni.",
                    tip = "80%+ = Nihliap ken a ngai! Umbrella bring!"
                )

                // Wind Speed & Gusts
                WeatherMetricCard(
                    icon = Icons.Default.Air,
                    iconColor = Color(0xFF00D4FF),
                    title = "Wind Speed & Gusts",
                    titleMizo = "Thli chak dan",
                    description = "Average wind speed vs maximum sudden gusts.",
                    descriptionMizo = "Thli a chak dan pangngai leh a thawk huk inkar a ni.",
                    tip = "Strong winds affect outdoor activities - pawna chet chhuahna tliin harsatna a thlen thei."
                )

                // Humidity
                WeatherMetricCard(
                    icon = Icons.Default.WaterDrop,
                    iconColor = Color(0xFF06D6A0),
                    title = "Humidity (%)",
                    titleMizo = "Boruak hnawng dan",
                    description = "Amount of moisture in the air.",
                    descriptionMizo = "Boruak-ah tui awm zat a ni.",
                    tip = "High humidity = khua a ti lum zual - feels hotter than actual temperature."
                )

                // UV Index
                WeatherMetricCard(
                    icon = Icons.Default.WbSunny,
                    iconColor = Color(0xFFFFD166),
                    title = "UV Index",
                    titleMizo = "Ni eng chak dan",
                    description = "Measures sun's UV radiation strength.",
                    descriptionMizo = "Ni eng atanga UV radiation chak dan a ni.",
                    tip = "6+ = sunscreen hman ang, ni eng hnuaiah rei tak awm suh."
                )

                // UV Index Color Guide
                UVIndexGuide()

                // Visibility
                WeatherMetricCard(
                    icon = Icons.Default.Visibility,
                    iconColor = Color(0xFF8338EC),
                    title = "Visibility",
                    titleMizo = "A lang dan/hmu theih dan",
                    description = "How far you can see clearly.",
                    descriptionMizo = "A hlat zawng zawng in i hmuh theih dan a ni.",
                    tip = "Low visibility = mauva/fog, ruah nasa - drive fimkhur ang!"
                )

                // Pressure
                WeatherMetricCard(
                    icon = Icons.Default.Compress,
                    iconColor = Color(0xFFFF006E),
                    title = "Pressure (hPa)",
                    titleMizo = "Boruak pressure",
                    description = "Atmospheric pressure at your location.",
                    descriptionMizo = "I hmuna boruak a thlum dan/pressure a ni.",
                    tip = "Rising = khawchin a ṭha zawk ang; Falling = ruah sur thei."
                )

                // Marine Risk
                MarineRiskGuide()

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun WeatherMetricCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    titleMizo: String,
    description: String,
    descriptionMizo: String,
    tip: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = titleMizo,
                        color = iconColor.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                    )
                }
            }

            Text(
                text = description,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )

            Text(
                text = descriptionMizo,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )

            // Tip box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.15f))
                    .padding(12.dp)
            ) {
                Row {
                    Text(
                        text = "💡",
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = tip,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun UVIndexGuide() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "UV Index Color Guide",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UVLevelBox(range = "0-2", label = "Low", color = Color(0xFF06D6A0), modifier = Modifier.weight(1f))
                UVLevelBox(range = "3-5", label = "Moderate", color = Color(0xFFFFD166), modifier = Modifier.weight(1f))
                UVLevelBox(range = "6-7", label = "High", color = Color(0xFFFF9F1C), modifier = Modifier.weight(1f))
                UVLevelBox(range = "8+", label = "Very High", color = Color(0xFFFF3D00), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun UVLevelBox(
    range: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.3f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = range,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun MarineRiskGuide() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00B4DB).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sailing,
                        contentDescription = null,
                        tint = Color(0xFF00B4DB),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Marine Risk",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = "Tuifinriat Ralveng (for coastal areas)",
                        color = Color(0xFF00B4DB).copy(alpha = 0.8f),
                        fontSize = 12.sp,
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MarineRiskLevel(
                    level = "GREEN",
                    label = "Safe - A him, pawnah i che thei",
                    color = Color(0xFF06D6A0)
                )
                MarineRiskLevel(
                    level = "YELLOW",
                    label = "Caution - Fimkhur ang, thil tih tur chiang takin ngaihtuah ang",
                    color = Color(0xFFFFD166)
                )
                MarineRiskLevel(
                    level = "ORANGE",
                    label = "Dangerous - Ralveng a awm, experience nei lo ten chhuah lo ang",
                    color = Color(0xFFFF9F1C)
                )
                MarineRiskLevel(
                    level = "RED",
                    label = "Do not go out - Chhuah ngai lo, a him lo!",
                    color = Color(0xFFFF3D00)
                )
            }
        }
    }
}

@Composable
private fun MarineRiskLevel(
    level: String,
    label: String,
    color: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = level,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
            )
        }
    }
}

