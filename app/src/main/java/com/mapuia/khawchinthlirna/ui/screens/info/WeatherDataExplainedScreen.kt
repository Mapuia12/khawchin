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
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapuia.khawchinthlirna.R
import com.mapuia.khawchinthlirna.ui.components.BannerAd
import com.mapuia.khawchinthlirna.ui.theme.appBackgroundGradient
import com.mapuia.khawchinthlirna.ui.theme.appIconTint
import com.mapuia.khawchinthlirna.ui.theme.appTextMuted
import com.mapuia.khawchinthlirna.ui.theme.appTextPrimary
import com.mapuia.khawchinthlirna.ui.theme.appTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDataExplainedScreen(
    onBack: () -> Unit,
    isMizo: Boolean = true,
) {
    val backgroundGradient = appBackgroundGradient()
    val textPrimary = appTextPrimary()
    val iconTint = appIconTint()

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
                            text = langString(
                                R.string.weather_data_title_mz,
                                R.string.weather_data_title_en,
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
                    titleEn = stringResource(R.string.wd_temp_title_en),
                    titleMz = stringResource(R.string.wd_temp_title_mz),
                    descriptionEn = stringResource(R.string.wd_temp_desc_en),
                    descriptionMz = stringResource(R.string.wd_temp_desc_mz),
                    tipEn = stringResource(R.string.wd_temp_tip_en),
                    tipMz = stringResource(R.string.wd_temp_tip_mz),
                    isMizo = isMizo,
                )

                // Precipitation Probability
                WeatherMetricCard(
                    icon = Icons.Default.WaterDrop,
                    iconColor = Color(0xFF3A86FF),
                    titleEn = stringResource(R.string.wd_precip_title_en),
                    titleMz = stringResource(R.string.wd_precip_title_mz),
                    descriptionEn = stringResource(R.string.wd_precip_desc_en),
                    descriptionMz = stringResource(R.string.wd_precip_desc_mz),
                    tipEn = stringResource(R.string.wd_precip_tip_en),
                    tipMz = stringResource(R.string.wd_precip_tip_mz),
                    isMizo = isMizo,
                )

                // Wind Speed & Gusts
                WeatherMetricCard(
                    icon = Icons.Default.Air,
                    iconColor = Color(0xFF00D4FF),
                    titleEn = stringResource(R.string.wd_wind_title_en),
                    titleMz = stringResource(R.string.wd_wind_title_mz),
                    descriptionEn = stringResource(R.string.wd_wind_desc_en),
                    descriptionMz = stringResource(R.string.wd_wind_desc_mz),
                    tipEn = stringResource(R.string.wd_wind_tip_en),
                    tipMz = stringResource(R.string.wd_wind_tip_mz),
                    isMizo = isMizo,
                )

                // Humidity
                WeatherMetricCard(
                    icon = Icons.Default.WaterDrop,
                    iconColor = Color(0xFF06D6A0),
                    titleEn = stringResource(R.string.wd_humidity_title_en),
                    titleMz = stringResource(R.string.wd_humidity_title_mz),
                    descriptionEn = stringResource(R.string.wd_humidity_desc_en),
                    descriptionMz = stringResource(R.string.wd_humidity_desc_mz),
                    tipEn = stringResource(R.string.wd_humidity_tip_en),
                    tipMz = stringResource(R.string.wd_humidity_tip_mz),
                    isMizo = isMizo,
                )

                // UV Index
                WeatherMetricCard(
                    icon = Icons.Default.WbSunny,
                    iconColor = Color(0xFFFFD166),
                    titleEn = stringResource(R.string.wd_uv_title_en),
                    titleMz = stringResource(R.string.wd_uv_title_mz),
                    descriptionEn = stringResource(R.string.wd_uv_desc_en),
                    descriptionMz = stringResource(R.string.wd_uv_desc_mz),
                    tipEn = stringResource(R.string.wd_uv_tip_en),
                    tipMz = stringResource(R.string.wd_uv_tip_mz),
                    isMizo = isMizo,
                )

                // UV Index Color Guide
                UVIndexGuide(isMizo = isMizo)

                // Visibility
                WeatherMetricCard(
                    icon = Icons.Default.Visibility,
                    iconColor = Color(0xFF8338EC),
                    titleEn = stringResource(R.string.wd_visibility_title_en),
                    titleMz = stringResource(R.string.wd_visibility_title_mz),
                    descriptionEn = stringResource(R.string.wd_visibility_desc_en),
                    descriptionMz = stringResource(R.string.wd_visibility_desc_mz),
                    tipEn = stringResource(R.string.wd_visibility_tip_en),
                    tipMz = stringResource(R.string.wd_visibility_tip_mz),
                    isMizo = isMizo,
                )

                // Pressure
                WeatherMetricCard(
                    icon = Icons.Default.Compress,
                    iconColor = Color(0xFFFF006E),
                    titleEn = stringResource(R.string.wd_pressure_title_en),
                    titleMz = stringResource(R.string.wd_pressure_title_mz),
                    descriptionEn = stringResource(R.string.wd_pressure_desc_en),
                    descriptionMz = stringResource(R.string.wd_pressure_desc_mz),
                    tipEn = stringResource(R.string.wd_pressure_tip_en),
                    tipMz = stringResource(R.string.wd_pressure_tip_mz),
                    isMizo = isMizo,
                )

                // Marine Risk
                MarineRiskGuide(isMizo = isMizo)

                // Satellite IMERG
                WeatherMetricCard(
                    icon = Icons.Default.WaterDrop,
                    iconColor = Color(0xFF7C3AED),
                    titleEn = stringResource(R.string.wd_satellite_title_en),
                    titleMz = stringResource(R.string.wd_satellite_title_mz),
                    descriptionEn = stringResource(R.string.wd_satellite_desc_en),
                    descriptionMz = stringResource(R.string.wd_satellite_desc_mz),
                    tipEn = stringResource(R.string.wd_satellite_tip_en),
                    tipMz = stringResource(R.string.wd_satellite_tip_mz),
                    isMizo = isMizo,
                )

                // Bias Correction
                WeatherMetricCard(
                    icon = Icons.Default.Compress,
                    iconColor = Color(0xFF06D6A0),
                    titleEn = stringResource(R.string.wd_bias_title_en),
                    titleMz = stringResource(R.string.wd_bias_title_mz),
                    descriptionEn = stringResource(R.string.wd_bias_desc_en),
                    descriptionMz = stringResource(R.string.wd_bias_desc_mz),
                    tipEn = stringResource(R.string.wd_bias_tip_en),
                    tipMz = stringResource(R.string.wd_bias_tip_mz),
                    isMizo = isMizo,
                )

                // Nowcast Sources
                WeatherMetricCard(
                    icon = Icons.Default.Air,
                    iconColor = Color(0xFF00D4FF),
                    titleEn = stringResource(R.string.wd_nowcast_title_en),
                    titleMz = stringResource(R.string.wd_nowcast_title_mz),
                    descriptionEn = stringResource(R.string.wd_nowcast_desc_en),
                    descriptionMz = stringResource(R.string.wd_nowcast_desc_mz),
                    tipEn = stringResource(R.string.wd_nowcast_tip_en),
                    tipMz = stringResource(R.string.wd_nowcast_tip_mz),
                    isMizo = isMizo,
                )

                // Weather Systems
                WeatherMetricCard(
                    icon = Icons.Default.Sailing,
                    iconColor = Color(0xFFFF6D00),
                    titleEn = stringResource(R.string.wd_systems_title_en),
                    titleMz = stringResource(R.string.wd_systems_title_mz),
                    descriptionEn = stringResource(R.string.wd_systems_desc_en),
                    descriptionMz = stringResource(R.string.wd_systems_desc_mz),
                    tipEn = stringResource(R.string.wd_systems_tip_en),
                    tipMz = stringResource(R.string.wd_systems_tip_mz),
                    isMizo = isMizo,
                )

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
    titleEn: String,
    titleMz: String,
    descriptionEn: String,
    descriptionMz: String,
    tipEn: String,
    tipMz: String,
    isMizo: Boolean = true,
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
                        text = if (isMizo) titleMz else titleEn,
                        color = appTextPrimary(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = if (isMizo) titleEn else titleMz,
                        color = iconColor.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                    )
                }
            }

            Text(
                text = if (isMizo) descriptionMz else descriptionEn,
                color = appTextSecondary(0.85f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )

            if (isMizo) {
                Text(
                    text = descriptionEn,
                    color = appTextMuted(0.6f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }

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
                        text = if (isMizo) tipMz else tipEn,
                        color = appTextSecondary(0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun UVIndexGuide(isMizo: Boolean = true) {
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
                text = langString(
                    R.string.wd_uv_guide_title_mz,
                    R.string.wd_uv_guide_title_en,
                    isMizo
                ),
                color = appTextPrimary(),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UVLevelBox(
                    range = stringResource(R.string.wd_uv_range_0_2),
                    label = langString(R.string.wd_uv_low_mz, R.string.wd_uv_low_en, isMizo),
                    color = Color(0xFF06D6A0),
                    modifier = Modifier.weight(1f)
                )
                UVLevelBox(
                    range = stringResource(R.string.wd_uv_range_3_5),
                    label = langString(R.string.wd_uv_moderate_mz, R.string.wd_uv_moderate_en, isMizo),
                    color = Color(0xFFFFD166),
                    modifier = Modifier.weight(1f)
                )
                UVLevelBox(
                    range = stringResource(R.string.wd_uv_range_6_7),
                    label = langString(R.string.wd_uv_high_mz, R.string.wd_uv_high_en, isMizo),
                    color = Color(0xFFFF9F1C),
                    modifier = Modifier.weight(1f)
                )
                UVLevelBox(
                    range = stringResource(R.string.wd_uv_range_8_plus),
                    label = langString(R.string.wd_uv_very_high_mz, R.string.wd_uv_very_high_en, isMizo),
                    color = Color(0xFFFF3D00),
                    modifier = Modifier.weight(1f)
                )
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
            color = appTextPrimary(),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Text(
            text = label,
            color = appTextSecondary(0.8f),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun MarineRiskGuide(isMizo: Boolean = true) {
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
                        text = langString(
                            R.string.wd_marine_title_mz,
                            R.string.wd_marine_title_en,
                            isMizo
                        ),
                        color = appTextPrimary(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = langString(
                            R.string.wd_marine_subtitle_mz,
                            R.string.wd_marine_subtitle_en,
                            isMizo
                        ),
                        color = Color(0xFF00B4DB).copy(alpha = 0.8f),
                        fontSize = 12.sp,
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MarineRiskLevel(
                    level = stringResource(R.string.wd_marine_level_green),
                    label = langString(
                        R.string.wd_marine_green_mz,
                        R.string.wd_marine_green_en,
                        isMizo
                    ),
                    color = Color(0xFF06D6A0)
                )
                MarineRiskLevel(
                    level = stringResource(R.string.wd_marine_level_yellow),
                    label = langString(
                        R.string.wd_marine_yellow_mz,
                        R.string.wd_marine_yellow_en,
                        isMizo
                    ),
                    color = Color(0xFFFFD166)
                )
                MarineRiskLevel(
                    level = stringResource(R.string.wd_marine_level_orange),
                    label = langString(
                        R.string.wd_marine_orange_mz,
                        R.string.wd_marine_orange_en,
                        isMizo
                    ),
                    color = Color(0xFFFF9F1C)
                )
                MarineRiskLevel(
                    level = stringResource(R.string.wd_marine_level_red),
                    label = langString(
                        R.string.wd_marine_red_mz,
                        R.string.wd_marine_red_en,
                        isMizo
                    ),
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
                color = appTextSecondary(0.8f),
                fontSize = 12.sp,
            )
        }
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
