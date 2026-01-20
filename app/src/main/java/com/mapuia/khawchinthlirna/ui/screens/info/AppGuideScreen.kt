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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Send
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
fun AppGuideScreen(
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
                            text = "App Hman Dan", // App Guide
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
                Text(
                    text = "Khawchin Thlirna app hman dan kaihhruaina (Guide).",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )

                // Section 1: Weather en dan
                GuideSection(
                    number = 1,
                    icon = Icons.Default.Home,
                    iconColor = Color(0xFF00D4FF),
                    title = "Khawchin En Dan",
                    titleEnglish = "Home Screen leh Forecast",
                    steps = listOf(
                        "Home screen-ah tun huna khawchin dinhmun a lang nghal ang.",
                        "A hnuai lamah scroll la, darkar tin leh ni tin thlirlawkna i hmu ang."
                    )
                )

                // Section 2: Report thawn dan
                GuideSection(
                    number = 2,
                    icon = Icons.Default.Send,
                    iconColor = Color(0xFF06D6A0),
                    title = "Report Thehluh Dan",
                    titleEnglish = "Khawchin Report Thawnna",
                    steps = listOf(
                        "'+' button emaw 'Report Weather' tih kha hmet rawh.",
                        "Ruah sur dan (Rain Intensity) thlang rawh - Tih ngei ngei tur.",
                        "Van awmdan, thli leh note te i duh chuan belh rawh - Tih kher a ngai lo.",
                        "A tawpah 'Submit' hmet rawh le!"
                    )
                )

                // Section 3: Points leh Badges
                GuideSection(
                    number = 3,
                    icon = Icons.Default.EmojiEvents,
                    iconColor = Color(0xFFFFD166),
                    title = "Points leh Badges",
                    titleEnglish = "Lawmman leh Chawimawina",
                    steps = listOf(
                        "Report i thehluh apiangin Points i hmu zel ang.",
                        "Achievement hrang hrang ti hlawhtling la, Badge la khawm rawh.",
                        "Leaderboard-ah midang nen inkhaikhin rawh u!"
                    )
                )

                // Section 4: Tips
                GuideSection(
                    number = 4,
                    icon = Icons.Default.Lightbulb,
                    iconColor = Color(0xFF8338EC),
                    title = "Thurawn (Tips)",
                    titleEnglish = "Hriat Tur Pawimawhte",
                    steps = listOf(
                        "I awmna hmun tak atangin report thehlut thin rawh.",
                        "Thu dik chiah report rawh - dawt report chuan i 'Trust Level' a ti hniam ang.",
                        "Report i thehluh ngun chuan i 'Trust Level' a sang zel ang."
                    )
                )

                BannerAd(modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun GuideSection(
    number: Int,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    titleEnglish: String,
    steps: List<String>,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Number badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                steps.forEachIndexed { index, step ->
                    Row {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(iconColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = iconColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = step,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}