package com.mapuia.khawchinthlirna.ui.screens.report

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.koin.compose.koinInject

import com.mapuia.khawchinthlirna.data.model.RainIntensity
import com.mapuia.khawchinthlirna.data.model.SkyCondition
import com.mapuia.khawchinthlirna.data.model.WindStrength
import com.mapuia.khawchinthlirna.data.auth.AuthManager
import com.mapuia.khawchinthlirna.data.auth.UserProfile
import com.mapuia.khawchinthlirna.data.ReverseGeocoder
import com.mapuia.khawchinthlirna.ui.components.BannerAd
import kotlinx.coroutines.launch

/**
 * Full-featured Report Weather Screen.
 * Includes all fields supported by backend API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportWeatherScreen(
    userLat: Double?,
    userLon: Double?,
    userId: String,
    onBack: () -> Unit,
    onSubmit: suspend (
        rainIntensity: Int,
        skyCondition: Int?,
        windStrength: Int?,
        notes: String?,
        locationName: String?,
    ) -> Result<Unit>,
    onNavigateToRainGuide: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager: AuthManager = koinInject()
    val reverseGeocoder = remember { ReverseGeocoder(context) }

    // Auth state
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }

    // Load user profile
    LaunchedEffect(authManager.userId) {
        userProfile = authManager.getUserProfile()
    }

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            isSigningIn = true
            val signInResult = authManager.handleGoogleSignInResult(result.data)
            isSigningIn = false
            if (signInResult.isSuccess) {
                userProfile = authManager.getUserProfile()
                Toast.makeText(context, "Sign in a hlawhtling e!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Sign in a hlawhchham tlat.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Form state
    var rainIntensity by remember { mutableIntStateOf(0) }
    var skyCondition by remember { mutableStateOf<Int?>(null) }
    var windStrength by remember { mutableStateOf<Int?>(null) }
    var notes by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var isAutoDetectingLocation by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Auto-detect location name using ReverseGeocoder (handles API deprecation properly)
    LaunchedEffect(userLat, userLon) {
        if (userLat != null && userLon != null && locationName.isBlank()) {
            isAutoDetectingLocation = true
            val name = reverseGeocoder.getPlaceName(userLat, userLon)
            if (!name.isNullOrBlank() && locationName.isBlank()) {
                locationName = name
            }
            isAutoDetectingLocation = false
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
                            text = "Khawchin Report-na", // Changed from Report Weather
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
                    actions = {
                        IconButton(onClick = onNavigateToRainGuide) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Ruah sur dan Sawifiahna",
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // User Profile Card - Sign in to earn points
                UserProfileCard(
                    userProfile = userProfile,
                    isAnonymous = authManager.isAnonymous,
                    isSigningIn = isSigningIn,
                    onSignInClick = { signInLauncher.launch(authManager.getGoogleSignInIntent()) }
                )

                // Location indicator
                if (userLat != null && userLon != null) {
                    LocationCard(
                        locationName = locationName,
                        isLoading = isAutoDetectingLocation,
                        onLocationNameChange = { locationName = it }
                    )
                } else {
                    NoLocationCard()
                }

                // Rain Intensity Selector (Required)
                SectionCard(
                    title = "Ruah Sur Dan (Tih ngei ngei tur) *", // Rain Intensity
                    icon = Icons.Default.WaterDrop,
                ) {
                    RainIntensitySelector(
                        selected = rainIntensity,
                        onSelect = { rainIntensity = it }
                    )
                }

                // Sky Condition Selector (Optional)
                SectionCard(
                    title = "Van Dinhmun (Sky Condition)",
                    icon = Icons.Default.Cloud,
                    optional = true,
                ) {
                    SkyConditionSelector(
                        selected = skyCondition,
                        onSelect = { skyCondition = if (skyCondition == it) null else it }
                    )
                }

                // Wind Strength Selector (Optional)
                SectionCard(
                    title = "Thli Tleh Dan (Wind Strength)",
                    icon = Icons.Default.Cloud,
                    optional = true,
                ) {
                    WindStrengthSelector(
                        selected = windStrength,
                        onSelect = { windStrength = if (windStrength == it) null else it }
                    )
                }

                // Notes Section (Optional)
                SectionCard(
                    title = "Thil dang sawi duh (Notes)",
                    icon = Icons.Default.Cloud,
                    optional = true,
                ) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { if (it.length <= 500) notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Sawi belh duh i neih chuan hetah ziak rawh...",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF06D6A0),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color(0xFF06D6A0),
                        ),
                        maxLines = 4,
                        minLines = 2,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (userLat == null || userLon == null) {
                            Toast.makeText(
                                context,
                                "Report thehlut turin GPS a in-on a ngai",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        isSubmitting = true
                        scope.launch {
                            val result = onSubmit(
                                rainIntensity,
                                skyCondition,
                                windStrength,
                                notes.takeIf { it.isNotBlank() },
                                locationName.takeIf { it.isNotBlank() },
                            )

                            isSubmitting = false

                            result.fold(
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Ka lawm e! Report thehluh a hlawhtling.", // Fixed Mizo translation
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onBack()
                                },
                                onFailure = { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: "Report thehluh a hlawhchham tlat",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isSubmitting && userLat != null && userLon != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF06D6A0),
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isSubmitting) "Thehlut mek..." else "Report Ang",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }

                // Banner Ad
                BannerAd(modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    optional: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF00D4FF),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                if (optional) {
                    Text(
                        text = "Dah kher a ngai lo", // Optional
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun LocationCard(
    locationName: String,
    isLoading: Boolean,
    onLocationNameChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF06D6A0).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF06D6A0).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF06D6A0),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hmun Hming (Location)",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color(0xFF06D6A0),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "I awmna hmun zawn mek...",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = onLocationNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Hmun hming ziak lut rawh",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF06D6A0),
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = Color(0xFF06D6A0),
                        ),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoLocationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF6B6B).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "GPS On A Ngai",
                    color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    text = "Report thehlut turin khawngaihin GPS on rawh",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
fun RainIntensitySelector(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RainIntensity.entries.forEach { intensity ->
            RainIntensityChip(
                intensity = intensity,
                isSelected = selected == intensity.level,
                onClick = { onSelect(intensity.level) }
            )
        }
    }
}

@Composable
private fun RainIntensityChip(
    intensity: RainIntensity,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            when (intensity.level) {
                0 -> Color(0xFF06D6A0)
                1 -> Color(0xFF4ECDC4)
                2 -> Color(0xFF00B4DB)
                3 -> Color(0xFF3A86FF)
                4 -> Color(0xFFFF6B6B)
                5 -> Color(0xFFFF3D00)
                6 -> Color(0xFFD50000)
                else -> Color(0xFF3A86FF)
            }.copy(alpha = 0.8f)
        } else {
            Color.White.copy(alpha = 0.08f)
        },
        animationSpec = tween(200),
        label = "bg"
    )

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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { contentDescription = "${intensity.labelMizo} - ${intensity.labelEnglish}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = intensity.labelMizo,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "(${intensity.labelEnglish})",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                )
            }
            Text(
                text = intensity.description,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SkyConditionSelector(
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SkyCondition.entries.forEach { condition ->
            ConditionChip(
                label = condition.labelMizo,
                subLabel = condition.labelEnglish,
                isSelected = selected == condition.level,
                onClick = { onSelect(condition.level) },
                emoji = when (condition.level) {
                    0 -> "☀️"
                    1 -> "⛅"
                    2 -> "🌥️"
                    3 -> "☁️"
                    4 -> "🌫️"
                    else -> "☁️"
                }
            )
        }
    }
}

@Composable
fun WindStrengthSelector(
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WindStrength.entries.forEach { strength ->
            ConditionChip(
                label = strength.labelMizo,
                subLabel = strength.labelEnglish,
                isSelected = selected == strength.level,
                onClick = { onSelect(strength.level) },
                emoji = when (strength.level) {
                    0 -> "🍃"
                    1 -> "🌬️"
                    2 -> "💨"
                    3 -> "🌪️"
                    4 -> "🌊"
                    else -> "💨"
                }
            )
        }
    }
}

@Composable
private fun ConditionChip(
    label: String,
    subLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    emoji: String,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF3A86FF).copy(alpha = 0.7f)
        else Color.White.copy(alpha = 0.08f),
        animationSpec = tween(200),
        label = "bg"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { contentDescription = "$label - $subLabel" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subLabel,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * User profile card for sign-in and displaying user info in Report screen
 */
@Composable
private fun UserProfileCard(
    userProfile: UserProfile?,
    isAnonymous: Boolean,
    isSigningIn: Boolean,
    onSignInClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0xFF8338EC).copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF06D6A0), Color(0xFF8338EC))
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (userProfile?.photoUrl != null) {
                    AsyncImage(
                        model = userProfile.photoUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // User info or Sign in prompt
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (isAnonymous) {
                    Text(
                        text = "Sign in rawh",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Points leh badges i hlawh thei ang!",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = userProfile?.displayName ?: "User",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⭐ ${userProfile?.points ?: 0} points",
                            color = Color(0xFFFFD166),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "📊 ${userProfile?.totalReports ?: 0} reports",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Sign in button for anonymous users
            if (isAnonymous) {
                Button(
                    onClick = onSignInClick,
                    enabled = !isSigningIn,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF06D6A0)
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                ) {
                    if (isSigningIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Lut Rawh", // Sign In
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}