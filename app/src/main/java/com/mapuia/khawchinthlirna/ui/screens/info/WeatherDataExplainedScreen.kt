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
import com.mapuia.khawchinthlirna.ui.components.BannerAd

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
                            text = "Khawchin Data Hrilhfiahna", // Weather Data Explained
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kirna",
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
                    titleMizo = "Khaw Lum/Vawh Lam",
                    description = "Actual air temperature measured at your location.",
                    descriptionMizo = "I awmna hmuna boruak lum leh vawh dan dik tak.",
                    tip = "\"Feels like\" (A lan dan) hian thli leh boruak hnawng a huam tel a, a vawh dan tak tak a ni."
                )

                // Precipitation Probability
                WeatherMetricCard(
                    icon = Icons.Default.WaterDrop,
                    iconColor = Color(0xFF3A86FF),
                    title = "Precipitation Probability (%)",
                    titleMizo = "Ruah Sur Thei Dinhmun",
                    description = "The chance of rain occurring in that hour.",
                    descriptionMizo = "Darkar 1 chhunga ruah sur thei chance zat.",
                    tip = "80%+ a nih chuan nihliap ken a ngai ang!"
                )

                // Wind Speed & Gusts
                WeatherMetricCard(
                    icon = Icons.Default.Air,
                    iconColor = Color(0xFF00D4FF),
                    title = "Wind Speed & Gusts",
                    titleMizo = "Thli Tleh Chak Lam",
                    description = "Average wind speed vs maximum sudden gusts.",
                    descriptionMizo = "Thli tleh dan pangngai leh a thawk thut (Gust) chak dan.",
                    tip = "Thli a na chuan pawn chhuah fimkhur a ngai."
                )

                // Humidity
                WeatherMetricCard(
                    icon = Icons.Default.WaterDrop,
                    iconColor = Color(0xFF06D6A0),
                    title = "Humidity (%)",
                    titleMizo = "Boruak Hnawng Lam",
                    description = "Amount of moisture in the air.",
                    descriptionMizo = "Boruaka tui (moisture) awm zat.",
                    tip = "Hnawng a tam chuan khua a lum zualin a hriat."
                )

                // UV Index
                WeatherMetricCard(
                    icon = Icons.Default.WbSunny,
                    iconColor = Color(0xFFFFD166),
                    title = "UV Index",
                    titleMizo = "Ni Zung Chak Lam (UV)",
                    description = "Measures sun's UV radiation strength.",
                    descriptionMizo = "Ni zung hlauhawm (UV radiation) chak lam tehna.",
                    tip = "6+ a nih chuan ni sa hnuaiah rei tak awm loh tur."
                )

                // UV Index Color Guide
                UVIndexGuide()

                // Visibility
                WeatherMetricCard(
                    icon = Icons.Default.Visibility,
                    iconColor = Color(0xFF8338EC),
                    title = "Visibility",
                    titleMizo = "Khaw Hmuh Theih Chin",
                    description = "How far you can see clearly.",
                    descriptionMizo = "Khaw hla lam hmuh theih chin (Km in).",
                    tip = "A tlem chuan motor khalh fimkhur tur (Meikhu/Ruah vang)."
                )

                // Pressure
                WeatherMetricCard(
                    icon = Icons.Default.Compress,
                    iconColor = Color(0xFFFF006E),
                    title = "Pressure (hPa)",
                    titleMizo = "Boruak Rit Lam (Pressure)",
                    description = "Atmospheric pressure at your location.",
                    descriptionMizo = "I awmnaa boruak rit lam (Atmospheric pressure).",
                    tip = "A san chuan khua a tha ang; a hniam chuan ruah a sur thei."
                )

                // Marine Risk
                MarineRiskGuide()

                BannerAd(modifier = Modifier.fillMaxWidth())

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
                UVLevelBox(range = "0-2", label = "Hniam", color = Color(0xFF06D6A0), modifier = Modifier.weight(1f))
                UVLevelBox(range = "3-5", label = "Pangngai", color = Color(0xFFFFD166), modifier = Modifier.weight(1f))
                UVLevelBox(range = "6-7", label = "Sang", color = Color(0xFFFF9F1C), modifier = Modifier.weight(1f))
                UVLevelBox(range = "8+", label = "Sang Lutuk", color = Color(0xFFFF3D00), modifier = Modifier.weight(1f))
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
                        text = "Tuifinriat Ralveng", // Marine Risk
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = "Tuipui hnaih a awmte tan bik",
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
                    label = "Him - Tuifinriat chung a thiang a, hlauhawm a awm lo",
                    color = Color(0xFF06D6A0)
                )
                MarineRiskLevel(
                    level = "YELLOW",
                    label = "Fimkhur - Fimkhur a ngai, lawng te chhuah loh tur",
                    color = Color(0xFFFFD166)
                )
                MarineRiskLevel(
                    level = "ORANGE",
                    label = "Hlauhawm - Experience nei lo tan chhuah a him lo",
                    color = Color(0xFFFF9F1C)
                )
                MarineRiskLevel(
                    level = "RED",
                    label = "Chhuah Loh Tur - A hlauhawm hle, chhuah loh tawp tur!",
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