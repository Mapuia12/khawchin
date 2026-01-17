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
                            text = "Crowdsourcing Tangkai Dan",
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
                // Section 1: What is Crowdsourcing?
                InfoSection(
                    icon = Icons.Default.Group,
                    iconColor = Color(0xFF00D4FF),
                    title = "Crowdsourcing chu eng nge ni?",
                    titleEnglish = "What is Crowdsourcing?",
                    points = listOf(
                        "Community-based weather data collection - Mipui zawng zawng ten khawchin thu kan sawi khawm a ni",
                        "Real weather conditions from real people in real locations - Hmun dang dang atanga mihriem ten an hmuh tak tak report an thawn a ni",
                        "Helps improve forecast accuracy for everyone - Helpa mi zawng zawng tan khawchin thlirna a dik zawk theih nan a tangkai"
                    )
                )

                // Section 2: How Your Reports Help
                InfoSection(
                    icon = Icons.Default.Psychology,
                    iconColor = Color(0xFF8338EC),
                    title = "I report tangkai dan",
                    titleEnglish = "How Your Reports Help",
                    points = listOf(
                        "Your reports are combined with official weather stations - I report chu official weather station data nen a inkawp a ni",
                        "Machine learning weights reports by user reputation - AI/Machine learning in user reputation azirin report a pawimawh dan a ngaihtuah a ni",
                        "More accurate = higher reputation = more influence - A dik zawk chuan reputation a sang zawk a, influence a nei zawk"
                    )
                )

                // Section 3: Reputation System
                InfoSection(
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFFFFD166),
                    title = "Reputation System",
                    titleEnglish = "How Reputation Works",
                    points = listOf(
                        "New users start at 50% reputation - User thar te hi 50% reputation-ah an tan a ni",
                        "Accurate reports increase reputation - Report dik thawn chuan reputation a sang a ni",
                        "Reports matching nearby stations/users boost score - Report hnai leh user dangte nen a inang chuan score a sang a ni",
                        "High reputation = Trust Level 4, badges, leaderboard - Reputation sang chuan Trust Level 4, badges leh leaderboard-ah i awm thei"
                    )
                )

                // Section 4: Privacy
                InfoSection(
                    icon = Icons.Default.Lock,
                    iconColor = Color(0xFF06D6A0),
                    title = "Privacy",
                    titleEnglish = "Your Privacy Matters",
                    points = listOf(
                        "Location used only for weather mapping - I location chu khawchin mapping tan chauh a hman a ni",
                        "No personal data shared publicly - Personal data engmah public-ah a share a ni lo",
                        "Anonymous participation allowed - Hming lo thei chuan i tel thei bawk"
                    )
                )

                // Section 5: How Crowdsource Improves Forecast Accuracy
                InfoSection(
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFF00D4FF),
                    title = "Forecast Accuracy Dik Zawk",
                    titleEnglish = "How Crowdsource Improves Forecast",
                    points = listOf(
                        "Nowcast Correction - I report atanga current weather a dik lo chu minutes 30 chhungin a update nghal a ni",
                        "Bias Learning - I hmun tlangval/tlangzawl emaw tui hnai etc. attribute te chu AI in a zir a, forecast a improve a ni",
                        "Local Microclimate - Model global 25km grid a hmang a, i hmun micro-climate chu mipuite report-ah chauh ka hria a ni",
                        "Station Weight 70% + Crowdsource 30% - I report chu ground-truth weather station nen a blend a ni",
                        "Trust Level sang zawk = Influence sang zawk - Report dik thawn thin mi te chu forecast-ah influence an nei zawk"
                    )
                )

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

