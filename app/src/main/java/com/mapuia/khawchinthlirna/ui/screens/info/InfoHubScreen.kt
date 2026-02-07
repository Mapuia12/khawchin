package com.mapuia.khawchinthlirna.ui.screens.info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.WaterDrop
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
fun InfoHubScreen(
    onBack: () -> Unit,
    onNavigateToAppGuide: () -> Unit,
    onNavigateToCrowdsourcing: () -> Unit,
    onNavigateToRainGuide: () -> Unit,
    onNavigateToWeatherData: () -> Unit,
    isMizo: Boolean = true,
) {
    val backgroundGradient = appBackgroundGradient()
    val textPrimary = appTextPrimary()
    val textSecondary = appTextSecondary(0.8f)
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
                                R.string.info_hub_title_mz,
                                R.string.info_hub_title_en,
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = langString(
                        R.string.info_hub_desc_mz,
                        R.string.info_hub_desc_en,
                        isMizo
                    ),
                    color = textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )

                Spacer(modifier = Modifier.height(8.dp))

                InfoMenuItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    iconColor = Color(0xFF00D4FF),
                    title = langString(
                        R.string.info_menu_app_title_mz,
                        R.string.info_menu_app_title_en,
                        isMizo
                    ),
                    subtitle = langString(
                        R.string.info_menu_app_subtitle_mz,
                        R.string.info_menu_app_subtitle_en,
                        isMizo
                    ),
                    onClick = onNavigateToAppGuide,
                    isMizo = isMizo,
                )

                InfoMenuItem(
                    icon = Icons.Default.Group,
                    iconColor = Color(0xFF8338EC),
                    title = langString(
                        R.string.info_menu_crowd_title_mz,
                        R.string.info_menu_crowd_title_en,
                        isMizo
                    ),
                    subtitle = langString(
                        R.string.info_menu_crowd_subtitle_mz,
                        R.string.info_menu_crowd_subtitle_en,
                        isMizo
                    ),
                    onClick = onNavigateToCrowdsourcing,
                    isMizo = isMizo,
                )

                InfoMenuItem(
                    icon = Icons.Default.WaterDrop,
                    iconColor = Color(0xFF3A86FF),
                    title = langString(
                        R.string.info_menu_rain_title_mz,
                        R.string.info_menu_rain_title_en,
                        isMizo
                    ),
                    subtitle = langString(
                        R.string.info_menu_rain_subtitle_mz,
                        R.string.info_menu_rain_subtitle_en,
                        isMizo
                    ),
                    onClick = onNavigateToRainGuide,
                    isMizo = isMizo,
                )

                InfoMenuItem(
                    icon = Icons.Default.Cloud,
                    iconColor = Color(0xFF06D6A0),
                    title = langString(
                        R.string.info_menu_weather_title_mz,
                        R.string.info_menu_weather_title_en,
                        isMizo
                    ),
                    subtitle = langString(
                        R.string.info_menu_weather_subtitle_mz,
                        R.string.info_menu_weather_subtitle_en,
                        isMizo
                    ),
                    onClick = onNavigateToWeatherData,
                    isMizo = isMizo,
                )

                // Banner Ad
                BannerAd(modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun InfoMenuItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isMizo: Boolean = true,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = appTextPrimary(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(
                    text = subtitle,
                    color = appTextMuted(0.6f),
                    fontSize = 12.sp,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = langString(
                    R.string.ui_go_mz,
                    R.string.ui_go_en,
                    isMizo
                ),
                tint = appIconTint(0.5f),
                modifier = Modifier.size(20.dp)
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