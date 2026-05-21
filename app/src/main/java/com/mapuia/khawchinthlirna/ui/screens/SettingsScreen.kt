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

private val AccentCyan = Color(0xFF06D6A0)
private val AccentPurple = Color(0xFF8338EC)
private val AccentGold = Color(0xFFFFD166)
private val AccentRed = Color(0xFFEF476F)
private val GlassWhite = Color.White.copy(alpha = 0.12f)
private val GlassBorder = Color.White.copy(alpha = 0.2f)

// Dark dialog colors - consistent across light/dark mode
private val DialogContainerColor = Color(0xFF1C1B2E)
private val DialogTitleColor = Color.White
private val DialogTextColor = Color.White.copy(alpha = 0.87f)

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
    appVersionName: String,
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
    val backgroundGradient = appBackgroundGradient()
    val textPrimary = appTextPrimary()
    val textSecondary = appTextSecondary(0.8f)
    val textMuted = appTextMuted(0.6f)
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
                            langString(
                                R.string.settings_title_mz,
                                R.string.settings_title_en,
                                isMizo
                            ),
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
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
                                    contentDescription = langString(
                                        R.string.ui_back_mz,
                                        R.string.ui_back_en,
                                        isMizo
                                    ),
                                    tint = iconTint
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
                        title = langString(
                            R.string.settings_section_language_display_mz,
                            R.string.settings_section_language_display_en,
                            isMizo
                        ),
                        icon = Icons.Default.Palette
                    )
                }

                item {
                    GlassCard {
                        SettingsItem(
                            icon = Icons.Default.Language,
                            title = langString(
                                R.string.settings_language_title_mz,
                                R.string.settings_language_title_en,
                                isMizo
                            ),
                            subtitle = if (currentLanguage == "mz") {
                                langString(
                                    R.string.settings_language_mizo_mz,
                                    R.string.settings_language_mizo_en,
                                    isMizo
                                )
                            } else {
                                langString(
                                    R.string.settings_language_english_mz,
                                    R.string.settings_language_english_en,
                                    isMizo
                                )
                            },
                            onClick = { showLanguageDialog = true },
                            accentColor = AccentCyan
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = appTextMuted(0.1f)
                        )

                        SettingsItem(
                            icon = Icons.Default.DarkMode,
                            title = langString(
                                R.string.settings_theme_title_mz,
                                R.string.settings_theme_title_en,
                                isMizo
                            ),
                            subtitle = when (darkModeEnabled) {
                                true -> langString(
                                    R.string.settings_theme_dark_mz,
                                    R.string.settings_theme_dark_en,
                                    isMizo
                                )
                                false -> langString(
                                    R.string.settings_theme_light_mz,
                                    R.string.settings_theme_light_en,
                                    isMizo
                                )
                                null -> langString(
                                    R.string.settings_theme_system_mz,
                                    R.string.settings_theme_system_en,
                                    isMizo
                                )
                            },
                            onClick = { showThemeDialog = true },
                            accentColor = AccentPurple
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = appTextMuted(0.1f)
                        )

                        SettingsItem(
                            icon = Icons.Default.Thermostat,
                            title = langString(
                                R.string.settings_temp_unit_title_mz,
                                R.string.settings_temp_unit_title_en,
                                isMizo
                            ),
                            subtitle = if (temperatureUnit == "celsius") {
                                langString(
                                    R.string.settings_temp_unit_celsius_mz,
                                    R.string.settings_temp_unit_celsius_en,
                                    isMizo
                                )
                            } else {
                                langString(
                                    R.string.settings_temp_unit_fahrenheit_mz,
                                    R.string.settings_temp_unit_fahrenheit_en,
                                    isMizo
                                )
                            },
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
                        title = langString(
                            R.string.settings_section_notifications_mz,
                            R.string.settings_section_notifications_en,
                            isMizo
                        ),
                        icon = Icons.Default.Notifications
                    )
                }

                item {
                    GlassCard {
                        SettingsToggleItem(
                            icon = Icons.Default.Notifications,
                            title = langString(
                                R.string.settings_notifications_title_mz,
                                R.string.settings_notifications_title_en,
                                isMizo
                            ),
                            subtitle = langString(
                                R.string.settings_notifications_subtitle_mz,
                                R.string.settings_notifications_subtitle_en,
                                isMizo
                            ),
                            isChecked = notificationsEnabled,
                            onToggle = onNotificationsToggle,
                            accentColor = AccentCyan
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = appTextMuted(0.1f)
                        )

                        SettingsToggleItem(
                            icon = Icons.Default.Warning,
                            title = langString(
                                R.string.settings_severe_alerts_title_mz,
                                R.string.settings_severe_alerts_title_en,
                                isMizo
                            ),
                            subtitle = langString(
                                R.string.settings_severe_alerts_subtitle_mz,
                                R.string.settings_severe_alerts_subtitle_en,
                                isMizo
                            ),
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
                        title = langString(
                            R.string.settings_section_data_storage_mz,
                            R.string.settings_section_data_storage_en,
                            isMizo
                        ),
                        icon = Icons.Default.Storage
                    )
                }

                item {
                    GlassCard {
                        SettingsItem(
                            icon = Icons.Default.DeleteSweep,
                            title = langString(
                                R.string.settings_clear_cache_title_mz,
                                R.string.settings_clear_cache_title_en,
                                isMizo
                            ),
                            subtitle = langString(
                                R.string.settings_clear_cache_subtitle_mz,
                                R.string.settings_clear_cache_subtitle_en,
                                isMizo
                            ),
                            onClick = { showClearCacheDialog = true },
                            accentColor = AccentPurple
                        )
                    }
                }

                // About Section
                item {
                    SettingsSectionHeader(
                        title = langString(
                            R.string.settings_section_about_mz,
                            R.string.settings_section_about_en,
                            isMizo
                        ),
                        icon = Icons.Default.Info
                    )
                }

                item {
                    GlassCard {
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = langString(
                                R.string.settings_about_title_mz,
                                R.string.settings_about_title_en,
                                isMizo
                            ),
                            subtitle = stringResource(
                                if (isMizo) {
                                    R.string.settings_about_subtitle_mz
                                } else {
                                    R.string.settings_about_subtitle_en
                                },
                                appVersionName
                            ),
                            onClick = onAboutClick,
                            accentColor = AccentCyan
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = appTextMuted(0.1f)
                        )

                        SettingsItem(
                            icon = Icons.Default.PrivacyTip,
                            title = langString(
                                R.string.settings_privacy_title_mz,
                                R.string.settings_privacy_title_en,
                                isMizo
                            ),
                            onClick = onPrivacyPolicyClick,
                            accentColor = AccentPurple
                        )
                    }
                }

                // Danger Zone
                item {
                    SettingsSectionHeader(
                        title = langString(
                            R.string.settings_section_account_mz,
                            R.string.settings_section_account_en,
                            isMizo
                        ),
                        icon = Icons.Default.Security,
                        isDanger = true
                    )
                }

                item {
                    GlassCard {
                        SettingsItem(
                            icon = Icons.Default.DeleteForever,
                            title = langString(
                                R.string.settings_delete_account_title_mz,
                                R.string.settings_delete_account_title_en,
                                isMizo
                            ),
                            subtitle = langString(
                                R.string.settings_delete_account_subtitle_mz,
                                R.string.settings_delete_account_subtitle_en,
                                isMizo
                            ),
                            onClick = { showDeleteAccountDialog = true },
                            isDanger = true,
                            accentColor = AccentRed
                        )
                    }
                }

                // Banner Ad
                item {
                    BannerAd(modifier = Modifier.fillMaxWidth())
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
            containerColor = DialogContainerColor,
            titleContentColor = DialogTitleColor,
            textContentColor = DialogTextColor,
            title = {
                Text(
                    langString(
                        R.string.settings_language_dialog_title_mz,
                        R.string.settings_language_dialog_title_en,
                        isMizo
                    )
                )
            },
            text = {
                Column {
                    LanguageOption(
                        name = langString(
                            R.string.settings_language_mizo_mz,
                            R.string.settings_language_mizo_en,
                            isMizo
                        ),
                        isSelected = currentLanguage == "mz",
                        onClick = {
                            onLanguageChange("mz")
                            showLanguageDialog = false
                        }
                    )
                    LanguageOption(
                        name = langString(
                            R.string.settings_language_english_mz,
                            R.string.settings_language_english_en,
                            isMizo
                        ),
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
                    Text(
                        langString(
                            R.string.settings_cancel_mz,
                            R.string.settings_cancel_en,
                            isMizo
                        )
                    )
                }
            }
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = DialogContainerColor,
            titleContentColor = DialogTitleColor,
            textContentColor = DialogTextColor,
            title = {
                Text(
                    langString(
                        R.string.settings_theme_dialog_title_mz,
                        R.string.settings_theme_dialog_title_en,
                        isMizo
                    )
                )
            },
            text = {
                Column {
                    ThemeOption(
                        name = langString(
                            R.string.settings_theme_light_mz,
                            R.string.settings_theme_light_en,
                            isMizo
                        ),
                        isSelected = darkModeEnabled == false,
                        onClick = {
                            onDarkModeToggle(false)
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        name = langString(
                            R.string.settings_theme_dark_mz,
                            R.string.settings_theme_dark_en,
                            isMizo
                        ),
                        isSelected = darkModeEnabled == true,
                        onClick = {
                            onDarkModeToggle(true)
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        name = langString(
                            R.string.settings_theme_system_mz,
                            R.string.settings_theme_system_en,
                            isMizo
                        ),
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
                    Text(
                        langString(
                            R.string.settings_cancel_mz,
                            R.string.settings_cancel_en,
                            isMizo
                        )
                    )
                }
            }
        )
    }

    // Clear Cache Confirmation
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = DialogContainerColor,
            titleContentColor = DialogTitleColor,
            textContentColor = DialogTextColor,
            title = {
                Text(
                    langString(
                        R.string.settings_clear_cache_dialog_title_mz,
                        R.string.settings_clear_cache_dialog_title_en,
                        isMizo
                    )
                )
            },
            text = {
                Text(
                    langString(
                        R.string.settings_clear_cache_dialog_body_mz,
                        R.string.settings_clear_cache_dialog_body_en,
                        isMizo
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearCache()
                    showClearCacheDialog = false
                }) {
                    Text(
                        langString(
                            R.string.settings_clear_cache_confirm_mz,
                            R.string.settings_clear_cache_confirm_en,
                            isMizo
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(
                        langString(
                            R.string.settings_cancel_mz,
                            R.string.settings_cancel_en,
                            isMizo
                        )
                    )
                }
            }
        )
    }

    // Delete Account Confirmation
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            containerColor = DialogContainerColor,
            titleContentColor = DialogTitleColor,
            textContentColor = DialogTextColor,
            title = {
                Text(
                    langString(
                        R.string.settings_delete_account_dialog_title_mz,
                        R.string.settings_delete_account_dialog_title_en,
                        isMizo
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    langString(
                        R.string.settings_delete_account_dialog_body_mz,
                        R.string.settings_delete_account_dialog_body_en,
                        isMizo
                    )
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
                    Text(
                        langString(
                            R.string.settings_delete_account_confirm_mz,
                            R.string.settings_delete_account_confirm_en,
                            isMizo
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(
                        langString(
                            R.string.settings_cancel_mz,
                            R.string.settings_cancel_en,
                            isMizo
                        )
                    )
                }
            }
        )
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
            color = if (isDanger) AccentRed else appTextSecondary(0.7f)
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
                color = if (isDanger) AccentRed else appTextPrimary()
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = appTextMuted(0.5f)
                )
            }
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = appIconTint(0.6f)
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
                color = if (enabled) appTextPrimary() else appTextMuted(0.5f)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = appTextMuted(if (enabled) 0.5f else 0.3f)
                )
            }
        }
        
        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = appTextPrimary(),
                checkedTrackColor = accentColor,
                uncheckedThumbColor = appTextSecondary(0.8f),
                uncheckedTrackColor = appTextMuted(0.2f),
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
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = AccentCyan,
                unselectedColor = Color.White.copy(alpha = 0.7f)
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = name, color = Color.White)
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
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = AccentCyan,
                unselectedColor = Color.White.copy(alpha = 0.7f)
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = name, color = Color.White)
    }
}
