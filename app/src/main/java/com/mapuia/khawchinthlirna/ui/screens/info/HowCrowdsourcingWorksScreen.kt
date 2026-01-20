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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapuia.khawchinthlirna.ui.components.BannerAd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowCrowdsourcingWorksScreen(
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
                            text = "Mipui Tanhona (Crowdsourcing)",
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
                // Section 1: What is Crowdsourcing?
                InfoSection(
                    icon = Icons.Default.Group,
                    iconColor = Color(0xFF00D4FF),
                    title = "Eng nge Crowdsourcing?",
                    titleEnglish = "Mipui Tanhona Awmzia",
                    points = listOf(
                        "Community-based data - Mipui tangrualin khawchin kan khawnkhawmna a ni.",
                        "Real reports - Khawchin dinhmun dik tak, a hmun ngeia awm ten an report a ni.",
                        "Accuracy - Hei hian mi zawng zawng tana khawchin thlirlawkna (forecast) dik zawk siamna a tanpui a ni."
                    )
                )

                // Section 2: How Your Reports Help
                InfoSection(
                    icon = Icons.Default.Psychology,
                    iconColor = Color(0xFF8338EC),
                    title = "I Report Tangkai Dan",
                    titleEnglish = "I Report Hlutna",
                    points = listOf(
                        "Combination - I report leh weather station data te kha chawhpawlh a ni.",
                        "Machine Learning - AI hmangin user rintlak dan (reputation) a zirin report hi teh a ni.",
                        "Influence - Report dik zawk = Reputation sang zawk = I thu a tlang zawk."
                    )
                )

                // Section 3: Reputation System
                InfoSection(
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFFFFD166),
                    title = "Reputation System",
                    titleEnglish = "Rintlak Lam Tehfung",
                    points = listOf(
                        "Start - User thar te chu 50% reputation-ah an tan ang.",
                        "Increase - Report dik tak i thehluhin i reputation a sang zel ang.",
                        "Verify - I report leh weather station/midang report a inmilin score a sang thin.",
                        "Rewards - Reputation sang chuan Trust Level 4, badge leh leaderboard-ah hmun a chang thei."
                    )
                )

                // Section 4: Privacy
                InfoSection(
                    icon = Icons.Default.Lock,
                    iconColor = Color(0xFF06D6A0),
                    title = "Privacy",
                    titleEnglish = "Himna leh Zalenna",
                    points = listOf(
                        "Location - I awmna (Location) hi khawchin mapping atan chauh hman a ni.",
                        "Safe - Mimal chanchin engmah midang hmuh turin a lang lo.",
                        "Anonymous - Hming thup (Anonymous) pawhin a tel theih."
                    )
                )

                // Section 5: How Crowdsource Improves Forecast Accuracy
                InfoSection(
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFF00D4FF),
                    title = "Forecast Dik Zawk",
                    titleEnglish = "Mipui Report Tangkaina",
                    points = listOf(
                        "Nowcast - I report atangin khawchin dik lo a awmin minute 30 chhungin siamthat a ni.",
                        "Learning - AI chuan i awmna hmun (tlang/ruam) a zira khawchin danglam dan a zir zel.",
                        "Microclimate - Satellite-in a hmuh phak loh, i awmna hmun bik khawchin hriat nan a tangkai.",
                        "Blending - Station Weight 70% + Mipui Report 30% in data chawhpawlh a ni.",
                        "Trust - Report dik thawn thin mi te chuan forecast-ah influence an nei zawk."
                    )
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
    title: String,
    titleEnglish: String,
    points: List<String>,
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
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(
                        text = titleEnglish,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }
            }
        }
    }
}