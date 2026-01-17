package com.mapuia.khawchinthlirna.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Premium color palette
private val PremiumGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF0F0C29),
        Color(0xFF302B63),
        Color(0xFF24243E),
    )
)

private val AccentCyan = Color(0xFF06D6A0)
private val AccentPurple = Color(0xFF8338EC)
private val AccentGold = Color(0xFFFFD166)
private val AccentRed = Color(0xFFEF476F)
private val GlassWhite = Color.White.copy(alpha = 0.12f)
private val GlassBorder = Color.White.copy(alpha = 0.2f)

// Glass Card Composable
@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(4.dp),
        content = content
    )
}

/**
 * Premium Settings Screen with glass-morphism design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentLanguage: String, // "mz" or "en"
    onLanguageChange: (String) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    severeWeatherAlertsEnabled: Boolean,
    onSevereWeatherAlertsToggle: (Boolean) -> Unit,
    darkModeEnabled: Boolean?,
    onDarkModeToggle: (Boolean?) -> Unit, // null = system
    temperatureUnit: String, // "celsius" or "fahrenheit"
    onTemperatureUnitChange: (String) -> Unit,
    onClearCache: () -> Unit,
    onDeleteAccount: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onAboutClick: () -> Unit,
    onBackClick: () -> Unit,
    isMizo: Boolean = true
) {
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            if (isMizo) "Settings" else "Settings",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GlassWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack, 
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Language & Appearance Section
                item {
                    SettingsSectionHeader(
                        title = if (isMizo) "Ṭawng leh Display" else "Language & Appearance",
                        icon = Icons.Default.Palette
                    )
                }

                item {
                    GlassCard {
                        SettingsItem(
                            icon = Icons.Default.Language,
                            title = if (isMizo) "Ṭawng" else "Language",
                            subtitle = if (currentLanguage == "mz") "Mizo" else "English",
                            onClick = { showLanguageDialog = true },
                            accentColor = AccentCyan
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        )

                        SettingsItem(
                            icon = Icons.Default.DarkMode,
                            title = if (isMizo) "Theme" else "Theme",
                            subtitle = when (darkModeEnabled) {
                                true -> if (isMizo) "Thim" else "Dark"
                                false -> if (isMizo) "Eng" else "Light"
                                null -> if (isMizo) "System angin" else "System default"
                            },
                            onClick = { showThemeDialog = true },
                            accentColor = AccentPurple
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        )

                        SettingsItem(
                            icon = Icons.Default.Thermostat,
                            title = if (isMizo) "Temperature Unit" else "Temperature Unit",
                            subtitle = if (temperatureUnit == "celsius") "Celsius (°C)" else "Fahrenheit (°F)",
                            onClick = {
                                onTemperatureUnitChange(if (temperatureUnit == "celsius") "fahrenheit" else "celsius")
                            },
                            accentColor = AccentGold
                        )
                    }
                }

                // Notifications Section
                item {
                    SettingsSectionHeader(
                        title = if (isMizo) "Hriattirna" else "Notifications",
                        icon = Icons.Default.Notifications
                    )
                }

                item {
                    GlassCard {
                        SettingsToggleItem(
                            icon = Icons.Default.Notifications,
                            title = if (isMizo) "Hriattirna" else "Notifications",
                            subtitle = if (isMizo) "Weather updates leh report status" else "Weather updates and report status",
                            isChecked = notificationsEnabled,
                            onToggle = onNotificationsToggle,
                            accentColor = AccentCyan
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        )

                        SettingsToggleItem(
                            icon = Icons.Default.Warning,
                            title = if (isMizo) "Khaw chin ṭha lo Hriattirna" else "Severe Weather Alerts",
                            subtitle = if (isMizo) "Thlipui, ruahtui lian, etc." else "Storms, heavy rain, etc.",
                            isChecked = severeWeatherAlertsEnabled,
                            onToggle = onSevereWeatherAlertsToggle,
                            enabled = notificationsEnabled,
                            accentColor = AccentGold
                        )
                    }
                }

                // Data & Storage Section
                item {
                    SettingsSectionHeader(
                        title = if (isMizo) "Data & Storage" else "Data & Storage",
                        icon = Icons.Default.Storage
                    )
                }

                item {
                    GlassCard {
                        SettingsItem(
                            icon = Icons.Default.DeleteSweep,
                            title = if (isMizo) "Cache ṭhiat" else "Clear Cache",
                            subtitle = if (isMizo) "Cached weather data paih rawh" else "Clear cached weather data",
                            onClick = { showClearCacheDialog = true },
                            accentColor = AccentPurple
                        )
                    }
                }

                // About Section
                item {
                    SettingsSectionHeader(
                        title = if (isMizo) "App chungchang" else "About",
                        icon = Icons.Default.Info
                    )
                }

                item {
                    GlassCard {
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = if (isMizo) "App chungchang" else "About",
                            subtitle = "Khawchin v1.0",
                            onClick = onAboutClick,
                            accentColor = AccentCyan
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        )

                        SettingsItem(
                            icon = Icons.Default.PrivacyTip,
                            title = if (isMizo) "Privacy Policy" else "Privacy Policy",
                            onClick = onPrivacyPolicyClick,
                            accentColor = AccentPurple
                        )
                    }
                }

                // Danger Zone
                item {
                    SettingsSectionHeader(
                        title = if (isMizo) "Account" else "Account",
                        icon = Icons.Default.Security,
                        isDanger = true
                    )
                }

                item {
                    GlassCard {
                        SettingsItem(
                            icon = Icons.Default.DeleteForever,
                            title = if (isMizo) "Account paih" else "Delete Account",
                            subtitle = if (isMizo) "I data zawng zawng paih rawh" else "Remove all your data",
                            onClick = { showDeleteAccountDialog = true },
                            isDanger = true,
                            accentColor = AccentRed
                        )
                    }
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(if (isMizo) "Ṭawng thlan rawh" else "Select Language") },
            text = {
                Column {
                    LanguageOption(
                        name = "Mizo",
                        isSelected = currentLanguage == "mz",
                        onClick = {
                            onLanguageChange("mz")
                            showLanguageDialog = false
                        }
                    )
                    LanguageOption(
                        name = "English",
                        isSelected = currentLanguage == "en",
                        onClick = {
                            onLanguageChange("en")
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(if (isMizo) "Cancel" else "Cancel")
                }
            }
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(if (isMizo) "Theme thlan rawh" else "Select Theme") },
            text = {
                Column {
                    ThemeOption(
                        name = if (isMizo) "Eng" else "Light",
                        isSelected = darkModeEnabled == false,
                        onClick = {
                            onDarkModeToggle(false)
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        name = if (isMizo) "Thim" else "Dark",
                        isSelected = darkModeEnabled == true,
                        onClick = {
                            onDarkModeToggle(true)
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        name = if (isMizo) "System angin" else "System default",
                        isSelected = darkModeEnabled == null,
                        onClick = {
                            onDarkModeToggle(null)
                            showThemeDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(if (isMizo) "Cancel" else "Cancel")
                }
            }
        )
    }

    // Clear Cache Confirmation
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(if (isMizo) "Cache ṭhiat?" else "Clear Cache?") },
            text = {
                Text(
                    if (isMizo) "Cached weather data a paih dawn a, offline mode hman theih ni tawh lo ang."
                    else "This will clear cached weather data. Offline mode won't work until data is refreshed."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearCache()
                    showClearCacheDialog = false
                }) {
                    Text(if (isMizo) "Paih rawh" else "Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(if (isMizo) "Cancel" else "Cancel")
                }
            }
        )
    }

    // Delete Account Confirmation
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text(
                    if (isMizo) "⚠️ Account paih?" else "⚠️ Delete Account?",
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    if (isMizo) "I account leh i data zawng zawng a paih vek dawn. Hei hi ruahman theih a ni lo!"
                    else "This will permanently delete your account and all your data. This action cannot be undone!"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAccount()
                        showDeleteAccountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isMizo) "Paih rawh" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(if (isMizo) "cancel" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String, 
    icon: ImageVector,
    isDanger: Boolean = false
) {
    Row(
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isDanger) AccentRed else AccentCyan
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = if (isDanger) AccentRed else Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    isDanger: Boolean = false,
    accentColor: Color = AccentCyan
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isDanger) AccentRed else accentColor
            )
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDanger) AccentRed else Color.White
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean = true,
    accentColor: Color = AccentCyan
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = if (enabled) 0.15f else 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) accentColor else accentColor.copy(alpha = 0.4f)
            )
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = if (enabled) 0.5f else 0.3f)
                )
            }
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun LanguageOption(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = name)
    }
}

@Composable
private fun ThemeOption(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = name)
    }
}
