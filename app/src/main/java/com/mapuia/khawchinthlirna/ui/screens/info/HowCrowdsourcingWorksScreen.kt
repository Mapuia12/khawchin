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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.res.stringArrayResource
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
fun HowCrowdsourcingWorksScreen(
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
                                R.string.crowd_title_mz,
                                R.string.crowd_title_en,
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
                // Section 1: What is Crowdsourcing?
                InfoSection(
                    icon = Icons.Default.Group,
                    iconColor = Color(0xFF00D4FF),
                    titleMz = stringResource(R.string.crowd_section1_title_mz),
                    titleEn = stringResource(R.string.crowd_section1_title_en),
                    pointsMz = stringArrayResource(R.array.crowd_section1_points_mz).toList(),
                    pointsEn = stringArrayResource(R.array.crowd_section1_points_en).toList(),
                    isMizo = isMizo,
                )

                // Section 2: How Your Reports Help
                InfoSection(
                    icon = Icons.Default.Psychology,
                    iconColor = Color(0xFF8338EC),
                    titleMz = stringResource(R.string.crowd_section2_title_mz),
                    titleEn = stringResource(R.string.crowd_section2_title_en),
                    pointsMz = stringArrayResource(R.array.crowd_section2_points_mz).toList(),
                    pointsEn = stringArrayResource(R.array.crowd_section2_points_en).toList(),
                    isMizo = isMizo,
                )

                // Section 3: Reputation System
                InfoSection(
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFFFFD166),
                    titleMz = stringResource(R.string.crowd_section3_title_mz),
                    titleEn = stringResource(R.string.crowd_section3_title_en),
                    pointsMz = stringArrayResource(R.array.crowd_section3_points_mz).toList(),
                    pointsEn = stringArrayResource(R.array.crowd_section3_points_en).toList(),
                    isMizo = isMizo,
                )

                // Section 4: Privacy
                InfoSection(
                    icon = Icons.Default.Lock,
                    iconColor = Color(0xFF06D6A0),
                    titleMz = stringResource(R.string.crowd_section4_title_mz),
                    titleEn = stringResource(R.string.crowd_section4_title_en),
                    pointsMz = stringArrayResource(R.array.crowd_section4_points_mz).toList(),
                    pointsEn = stringArrayResource(R.array.crowd_section4_points_en).toList(),
                    isMizo = isMizo,
                )

                // Section 5: How Crowdsource Improves Forecast Accuracy
                InfoSection(
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFF00D4FF),
                    titleMz = stringResource(R.string.crowd_section5_title_mz),
                    titleEn = stringResource(R.string.crowd_section5_title_en),
                    pointsMz = stringArrayResource(R.array.crowd_section5_points_mz).toList(),
                    pointsEn = stringArrayResource(R.array.crowd_section5_points_en).toList(),
                    isMizo = isMizo,
                )

                BannerAd(modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun InfoSection(
    icon: ImageVector,
    iconColor: Color,
    titleMz: String,
    titleEn: String,
    pointsMz: List<String>,
    pointsEn: List<String>,
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
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isMizo) titleMz else titleEn,
                        color = appTextPrimary(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    if (isMizo) {
                        Text(
                            text = titleEn,
                            color = appTextMuted(0.6f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val points = if (isMizo) pointsMz else pointsEn
                points.forEach { point ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "•",
                            color = iconColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = point,
                            color = appTextSecondary(0.85f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }
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