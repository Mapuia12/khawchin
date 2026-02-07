package com.mapuia.khawchinthlirna.ui.screens.info

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapuia.khawchinthlirna.R
import com.mapuia.khawchinthlirna.data.model.RainIntensity
import com.mapuia.khawchinthlirna.ui.components.BannerAd
import com.mapuia.khawchinthlirna.ui.theme.appBackgroundGradient
import com.mapuia.khawchinthlirna.ui.theme.appIconTint
import com.mapuia.khawchinthlirna.ui.theme.appTextMuted
import com.mapuia.khawchinthlirna.ui.theme.appTextPrimary
import com.mapuia.khawchinthlirna.ui.theme.appTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RainIntensityGuideScreen(
    onBack: () -> Unit,
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
                                R.string.rain_guide_title_mz,
                                R.string.rain_guide_title_en,
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
                        R.string.rain_guide_intro_mz,
                        R.string.rain_guide_intro_en,
                        isMizo
                    ),
                    color = textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )

                Spacer(modifier = Modifier.height(8.dp))

                RainIntensity.entries.forEach { intensity ->
                    RainLevelCard(intensity = intensity, isMizo = isMizo)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tips section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF06D6A0).copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = langString(
                                R.string.rain_guide_tips_title_mz,
                                R.string.rain_guide_tips_title_en,
                                isMizo
                            ),
                            color = Color(0xFF06D6A0),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Text(
                            text = langString(
                                R.string.rain_guide_tip1_mz,
                                R.string.rain_guide_tip1_en,
                                isMizo
                            ),
                            color = appTextSecondary(0.8f),
                            fontSize = 13.sp,
                        )
                        Text(
                            text = langString(
                                R.string.rain_guide_tip2_mz,
                                R.string.rain_guide_tip2_en,
                                isMizo
                            ),
                            color = appTextSecondary(0.8f),
                            fontSize = 13.sp,
                        )
                    }
                }

                BannerAd(modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun RainLevelCard(
    intensity: RainIntensity,
    isMizo: Boolean = true,
) {
    val backgroundColor = when (intensity.level) {
        0 -> Color(0xFF06D6A0)
        1 -> Color(0xFF4ECDC4)
        2 -> Color(0xFF00B4DB)
        3 -> Color(0xFF3A86FF)
        4 -> Color(0xFFFF6B6B)
        5 -> Color(0xFFFF3D00)
        6 -> Color(0xFFD50000)
        else -> Color(0xFF3A86FF)
    }

    val emoji = when (intensity.level) {
        0 -> "☀️"
        1 -> "🌦️"
        2 -> "🌧️"
        3 -> "🌧️🌧️"
        4 -> "⛈️"
        5 -> "⛈️⛈️"
        6 -> "🌊⛈️"
        else -> "🌧️"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Level indicator
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor.copy(alpha = 0.3f))
                    .border(2.dp, backgroundColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 24.sp,
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            if (isMizo) R.string.rain_guide_level_mz else R.string.rain_guide_level_en,
                            intensity.level
                        ),
                        color = backgroundColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = intensity.mmPerHour,
                        color = appTextMuted(0.5f),
                        fontSize = 11.sp,
                    )
                }

                Text(
                    text = if (isMizo) intensity.labelMizo else intensity.labelEnglish,
                    color = appTextPrimary(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )

                if (isMizo) {
                    Text(
                        text = intensity.labelEnglish,
                        color = appTextSecondary(0.7f),
                        fontSize = 13.sp,
                    )
                }

                Text(
                    text = if (isMizo) intensity.description else intensity.descriptionEnglish,
                    color = appTextMuted(0.5f),
                    fontSize = 12.sp,
                )
            }
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