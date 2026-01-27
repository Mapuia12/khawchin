package com.mapuia.khawchinthlirna.ui.screens.report

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mapuia.khawchinthlirna.data.model.NearbyReport
import com.mapuia.khawchinthlirna.data.model.RainIntensity
import com.mapuia.khawchinthlirna.data.model.SkyCondition
import com.mapuia.khawchinthlirna.data.model.WindStrength
import com.mapuia.khawchinthlirna.ui.components.BannerAd
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Screen showing nearby weather reports from other users.
 * Uses a list view (map could be added later with Google Maps SDK).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyReportsScreen(
    userLat: Double?,
    userLon: Double?,
    onBack: () -> Unit,
    onFetchReports: suspend (lat: Double, lon: Double, radiusKm: Double, minutes: Int) -> List<NearbyReport>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var reports by remember { mutableStateOf<List<NearbyReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTimeFilter by remember { mutableIntStateOf(60) } // Default 1 hour
    var selectedReport by remember { mutableStateOf<NearbyReport?>(null) }

    val timeFilters = listOf(
        30 to "30 min",
        60 to "1 dar",
        120 to "2 dar",
        180 to "3 dar",
    )

    // Fetch reports when screen loads or filter changes
    LaunchedEffect(userLat, userLon, selectedTimeFilter) {
        if (userLat != null && userLon != null) {
            isLoading = true
            try {
                reports = onFetchReports(userLat, userLon, 15.0, selectedTimeFilter)
            } catch (e: Exception) {
                Toast.makeText(context, "Reports load a hlawh lo", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

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
                            text = "Nearby Reports",
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
                        IconButton(
                            onClick = {
                                if (userLat != null && userLon != null) {
                                    scope.launch {
                                        isLoading = true
                                        reports = onFetchReports(userLat, userLon, 15.0, selectedTimeFilter)
                                        isLoading = false
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
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
                    .fillMaxSize()
            ) {
                // Time filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.CenterVertically)
                    )
                    timeFilters.forEach { (minutes, label) ->
                        FilterChip(
                            selected = selectedTimeFilter == minutes,
                            onClick = { selectedTimeFilter = minutes },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF06D6A0),
                                selectedLabelColor = Color.White,
                                containerColor = Color.White.copy(alpha = 0.1f),
                                labelColor = Color.White.copy(alpha = 0.7f),
                            )
                        )
                    }
                }

                // Location info
                if (userLat != null && userLon != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = Color(0xFF06D6A0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "15km radius • ${reports.size} reports",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                        )
                    }
                }

                // Content
                when {
                    userLat == null || userLon == null -> {
                        NoLocationView()
                    }
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF06D6A0))
                        }
                    }
                    reports.isEmpty() -> {
                        EmptyReportsView(selectedTimeFilter)
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(reports) { report ->
                                ReportCard(
                                    report = report,
                                    isSelected = selectedReport?.id == report.id,
                                    onClick = {
                                        selectedReport = if (selectedReport?.id == report.id) null else report
                                    }
                                )
                            }
                            // Banner Ad
                            item { 
                                BannerAd(modifier = Modifier.fillMaxWidth())
                            }
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoLocationView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "GPS Required",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Text(
                text = "Nearby reports en tur chuan GPS on rawh",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun EmptyReportsView(minutes: Int) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📍",
                fontSize = 48.sp,
            )
            Text(
                text = "Report a awm lo",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Text(
                text = "15km chhung leh $minutes minute chhung-ah report a awm lo.\nI report thawn tur i duh em?",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ReportCard(
    report: NearbyReport,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val rainIntensity = RainIntensity.fromLevel(report.rainIntensity)
    val backgroundColor = when (report.rainIntensity) {
        0 -> Color(0xFF06D6A0)
        1 -> Color(0xFF4ECDC4)
        2 -> Color(0xFF00B4DB)
        3 -> Color(0xFF3A86FF)
        4 -> Color(0xFFFF6B6B)
        5 -> Color(0xFFFF3D00)
        6 -> Color(0xFFD50000)
        else -> Color(0xFF3A86FF)
    }

    val emoji = when (report.rainIntensity) {
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                backgroundColor.copy(alpha = 0.3f)
            } else {
                Color.White.copy(alpha = 0.1f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Rain intensity indicator
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(backgroundColor.copy(alpha = 0.2f))
                            .border(2.dp, backgroundColor, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                    }

                    Column {
                        Text(
                            text = rainIntensity.labelMizo,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Text(
                            text = rainIntensity.labelEnglish,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                        )
                    }
                }

                // Distance
                Column(horizontalAlignment = Alignment.End) {
                    report.distanceKm?.let { distance ->
                        Text(
                            text = String.format("%.1f km", distance),
                            color = Color(0xFF06D6A0),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                    Text(
                        text = formatTimeAgo(report.timestampAuto),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                    )
                }
            }

            // Expanded details
            AnimatedVisibility(visible = isSelected) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Location name
                    report.locationName?.let { name ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = name,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                            )
                        }
                    }

                    // Sky condition
                    report.skyCondition?.let { level ->
                        val sky = SkyCondition.fromLevel(level)
                        DetailRow(
                            label = "Van:",
                            value = "${sky.labelMizo} (${sky.labelEnglish})"
                        )
                    }

                    // Wind strength
                    report.windStrength?.let { level ->
                        val wind = WindStrength.fromLevel(level)
                        DetailRow(
                            label = "Thli:",
                            value = "${wind.labelMizo} (${wind.labelEnglish})"
                        )
                    }

                    // User reputation
                    report.userReputation?.let { rep ->
                        val repPercent = (rep * 100).toInt()
                        Text(
                            text = "⭐ Reporter reputation: $repPercent%",
                            color = if (repPercent >= 70) Color(0xFF06D6A0) else Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
        )
    }
}

/**
 * Format timestamp to relative time (e.g., "5 min ago")
 */
private fun formatTimeAgo(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val duration = Duration.between(instant, Instant.now())
        val minutes = duration.toMinutes()

        when {
            minutes < 1 -> "Tun chauh"
            minutes < 60 -> "$minutes min hmasa"
            minutes < 120 -> "1 dar hmasa"
            minutes < 180 -> "${minutes / 60} dar hmasa"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("h:mm a")
                    .withZone(ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (e: Exception) {
        isoTimestamp
    }
}
