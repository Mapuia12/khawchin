package com.mapuia.khawchinthlirna.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mapuia.khawchinthlirna.R
import com.mapuia.khawchinthlirna.data.auth.Badges
import com.mapuia.khawchinthlirna.data.auth.UserProfile
import com.mapuia.khawchinthlirna.ui.components.BannerAd
import com.mapuia.khawchinthlirna.ui.theme.appIconTint
import com.mapuia.khawchinthlirna.ui.theme.appTextMuted
import com.mapuia.khawchinthlirna.ui.theme.appTextPrimary
import com.mapuia.khawchinthlirna.ui.theme.appTextSecondary
import com.mapuia.khawchinthlirna.ui.theme.appBackgroundGradient

private val AccentCyan = Color(0xFF06D6A0)
private val AccentPurple = Color(0xFF8338EC)
private val AccentGold = Color(0xFFFFD166)
private val GlassWhite = Color.White.copy(alpha = 0.12f)
private val GlassBorder = Color.White.copy(alpha = 0.2f)

/**
 * Premium User Profile Screen with glass-morphism design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userProfile: UserProfile?,
    isAnonymous: Boolean,
    onBackClick: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    isMizo: Boolean = true
) {
    val backgroundGradient = appBackgroundGradient()
    val textPrimary = appTextPrimary()
    val textSecondary = appTextSecondary(0.7f)
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
                            text = langString(
                                R.string.profile_title_mz,
                                R.string.profile_title_en,
                                isMizo
                            ),
                            color = textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = langString(
                                    R.string.profile_back_cd_mz,
                                    R.string.profile_back_cd_en,
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Profile Header
                item {
                    ProfileHeader(
                        userProfile = userProfile,
                        isAnonymous = isAnonymous,
                        onSignInClick = onSignInClick,
                        onSignOutClick = onSignOutClick,
                        isMizo = isMizo
                    )
                }

                // Stats Section
                if (userProfile != null) {
                    item {
                        StatsSection(userProfile = userProfile, isMizo = isMizo)
                    }

                    // Trust Level Card
                    item {
                        TrustLevelCard(userProfile = userProfile, isMizo = isMizo)
                    }

                    // Badges Section
                    item {
                        BadgesSection(userProfile = userProfile, isMizo = isMizo)
                    }

                    // Activity Summary
                    item {
                        ActivitySummaryCard(userProfile = userProfile, isMizo = isMizo)
                    }
                }

                // Sign out button for authenticated users
                if (userProfile != null && !isAnonymous) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        PremiumOutlinedButton(
                            onClick = onSignOutClick,
                            text = langString(
                                R.string.profile_sign_out_mz,
                                R.string.profile_sign_out_en,
                                isMizo
                            ),
                            icon = Icons.AutoMirrored.Filled.Logout,
                            isDanger = true
                        )
                    }
                }

                // Banner Ad
                item {
                    BannerAd(modifier = Modifier.fillMaxWidth())
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun PremiumOutlinedButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    isDanger: Boolean = false
) {
    val borderColor = if (isDanger) Color(0xFFFF6B6B) else AccentCyan

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = Brush.horizontalGradient(
                listOf(borderColor.copy(alpha = 0.6f), borderColor)
            )
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = borderColor
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = AccentPurple.copy(alpha = 0.25f),
                ambientColor = Color.Black.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(GlassWhite)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.3f),
                        AccentPurple.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun ProfileHeader(
    userProfile: UserProfile?,
    isAnonymous: Boolean,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    isMizo: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with glow effect
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Glow ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    AccentCyan.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Avatar
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(AccentCyan, AccentPurple)
                            )
                        )
                        .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (userProfile?.photoUrl != null) {
                        AsyncImage(
                            model = userProfile.photoUrl,
                            contentDescription = langString(
                                R.string.profile_photo_cd_mz,
                                R.string.profile_photo_cd_en,
                                isMizo
                            ),
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = appIconTint()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Name
            Text(
                text = userProfile?.displayName ?: langString(
                    R.string.profile_guest_name_mz,
                    R.string.profile_guest_name_en,
                    isMizo
                ),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = appTextPrimary()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Email or Anonymous status
            Text(
                text = if (isAnonymous) {
                    langString(
                        R.string.profile_anonymous_label_mz,
                        R.string.profile_anonymous_label_en,
                        isMizo
                    )
                } else {
                    userProfile?.email ?: ""
                },
                fontSize = 14.sp,
                color = appTextSecondary(0.7f)
            )

            // Trust Level Badge
            if (userProfile != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AccentGold.copy(alpha = 0.2f))
                        .border(1.dp, AccentGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = getTrustLevelEmoji(userProfile.trustLevel),
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isMizo) userProfile.trustLevelNameMz else userProfile.trustLevelName,
                            color = AccentGold,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Sign In button for anonymous users
            if (isAnonymous) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onSignInClick,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentCyan
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        tint = Color.Black // Intentionally black on cyan button
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = langString(
                            R.string.profile_sign_in_google_mz,
                            R.string.profile_sign_in_google_en,
                            isMizo
                        ),
                        color = Color.Black, // Intentionally black on cyan button
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsSection(userProfile: UserProfile, isMizo: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PremiumStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Cloud,
            value = userProfile.totalReports.toString(),
            label = langString(
                R.string.profile_reports_label_mz,
                R.string.profile_reports_label_en,
                isMizo
            ),
            accentColor = AccentCyan
        )
        PremiumStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CheckCircle,
            value = "${userProfile.accuracyPercent}%",
            label = langString(
                R.string.profile_accuracy_label_mz,
                R.string.profile_accuracy_label_en,
                isMizo
            ),
            accentColor = AccentPurple
        )
        PremiumStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Star,
            value = userProfile.points.toString(),
            label = langString(
                R.string.profile_points_label_mz,
                R.string.profile_points_label_en,
                isMizo
            ),
            accentColor = AccentGold
        )
    }
}

@Composable
private fun PremiumStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: Color
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = appTextPrimary()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = appTextSecondary(0.6f)
            )
        }
    }
}

@Composable
private fun TrustLevelCard(userProfile: UserProfile, isMizo: Boolean) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = langString(
                    R.string.profile_trust_level_label_mz,
                    R.string.profile_trust_level_label_en,
                    isMizo
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = appTextPrimary()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Trust Level Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (level in 1..5) {
                    TrustLevelIndicator(
                        level = level,
                        isActive = level <= userProfile.trustLevel,
                        isCurrent = level == userProfile.trustLevel
                    )
                    if (level < 5) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (level < userProfile.trustLevel)
                                        AccentCyan
                                    else
                                        Color.White.copy(alpha = 0.15f)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reputation bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = langString(
                        R.string.profile_reputation_label_mz,
                        R.string.profile_reputation_label_en,
                        isMizo
                    ),
                    fontSize = 14.sp,
                    color = appTextSecondary(0.7f)
                )
                Text(
                    text = "${userProfile.reputationPercent}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(userProfile.reputation.toFloat())
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AccentCyan, AccentPurple)
                            )
                        )
                )
            }

            // Next level hint
            if (userProfile.trustLevel < 5) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TipsAndUpdates,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getNextLevelHint(userProfile.trustLevel, isMizo),
                        fontSize = 12.sp,
                        color = appTextSecondary(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustLevelIndicator(level: Int, isActive: Boolean, isCurrent: Boolean) {
    val size = if (isCurrent) 36.dp else 28.dp
    val emoji = getTrustLevelEmoji(level)
    val textMuted = appTextMuted(0.5f)

    val bgBrush = when {
        isCurrent -> Brush.linearGradient(listOf(AccentCyan, AccentPurple))
        isActive -> Brush.linearGradient(listOf(AccentCyan.copy(alpha = 0.3f), AccentCyan.copy(alpha = 0.3f)))
        else -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.1f)))
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bgBrush)
            .then(
                if (isCurrent) Modifier.border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                else Modifier
            )
            .animateContentSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isCurrent) {
            Text(text = emoji, fontSize = 16.sp)
        } else if (isActive) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = appIconTint()
            )
        } else {
            Text(
                text = level.toString(),
                fontSize = 12.sp,
                color = textMuted
            )
        }
    }
}

@Composable
private fun BadgesSection(userProfile: UserProfile, isMizo: Boolean) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = langString(
                        R.string.profile_badges_title_mz,
                        R.string.profile_badges_title_en,
                        isMizo
                    ),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = appTextPrimary()
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentPurple.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${userProfile.badges.size}/${Badges.allBadges.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (userProfile.badges.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = langString(
                                R.string.profile_no_badges_title_mz,
                                R.string.profile_no_badges_title_en,
                                isMizo
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = appTextSecondary(0.8f)
                        )
                        Text(
                            text = langString(
                                R.string.profile_no_badges_subtitle_mz,
                                R.string.profile_no_badges_subtitle_en,
                                isMizo
                            ),
                            fontSize = 12.sp,
                            color = appTextMuted(0.5f)
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(userProfile.badges) { badgeId ->
                        BadgeItem(
                            badgeId = badgeId,
                            isEarned = true,
                            isMizo = isMizo
                        )
                    }
                }
            }

            // Show locked badges
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = langString(
                    R.string.profile_badges_to_earn_mz,
                    R.string.profile_badges_to_earn_en,
                    isMizo
                ),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = appTextSecondary(0.6f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            val lockedBadges = Badges.allBadges.filter { !userProfile.badges.contains(it) }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(lockedBadges.take(5)) { badgeId ->
                    BadgeItem(
                        badgeId = badgeId,
                        isEarned = false,
                        isMizo = isMizo
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeItem(badgeId: String, isEarned: Boolean, isMizo: Boolean) {
    val backgroundColor = if (isEarned)
        Brush.linearGradient(listOf(AccentGold.copy(alpha = 0.3f), AccentPurple.copy(alpha = 0.2f)))
    else
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.04f)))

    Box(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = if (isEarned) AccentGold.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = Badges.getEmoji(badgeId),
                fontSize = if (isEarned) 28.sp else 24.sp,
                modifier = if (!isEarned) Modifier else Modifier
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isMizo) Badges.getNameMz(badgeId) else Badges.getName(badgeId),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 12.sp,
                fontWeight = if (isEarned) FontWeight.Medium else FontWeight.Normal,
                color = if (isEarned) appTextPrimary() else appTextMuted(0.5f)
            )
        }
    }
}

@Composable
private fun ActivitySummaryCard(userProfile: UserProfile, isMizo: Boolean) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = AccentCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = langString(
                        R.string.profile_activity_summary_mz,
                        R.string.profile_activity_summary_en,
                        isMizo
                    ),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = appTextPrimary()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ActivityRow(
                icon = Icons.AutoMirrored.Filled.Send,
                label = langString(
                    R.string.profile_activity_total_reports_mz,
                    R.string.profile_activity_total_reports_en,
                    isMizo
                ),
                value = userProfile.totalReports.toString(),
                color = AccentCyan
            )
            ActivityRow(
                icon = Icons.Default.Verified,
                label = langString(
                    R.string.profile_activity_verified_mz,
                    R.string.profile_activity_verified_en,
                    isMizo
                ),
                value = userProfile.accurateReports.toString(),
                color = AccentGold
            )
            ActivityRow(
                icon = Icons.Default.EmojiEvents,
                label = langString(
                    R.string.profile_activity_badges_mz,
                    R.string.profile_activity_badges_en,
                    isMizo
                ),
                value = userProfile.badges.size.toString(),
                color = AccentPurple
            )
        }
    }
}

@Composable
private fun ActivityRow(icon: ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = appTextSecondary(0.8f)
            )
        }
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
}

private fun getTrustLevelEmoji(level: Int): String = when (level) {
    1 -> "🌱"
    2 -> "🌿"
    3 -> "🌳"
    4 -> "⭐"
    5 -> "👑"
    else -> "🌱"
}

@Composable
private fun getNextLevelHint(currentLevel: Int, isMizo: Boolean): String = when (currentLevel) {
    1 -> langString(R.string.profile_next_level_1_mz, R.string.profile_next_level_1_en, isMizo)
    2 -> langString(R.string.profile_next_level_2_mz, R.string.profile_next_level_2_en, isMizo)
    3 -> langString(R.string.profile_next_level_3_mz, R.string.profile_next_level_3_en, isMizo)
    4 -> langString(R.string.profile_next_level_4_mz, R.string.profile_next_level_4_en, isMizo)
    else -> ""
}

@Composable
private fun langString(
    @StringRes mizoRes: Int,
    @StringRes englishRes: Int,
    isMizo: Boolean,
): String {
    return stringResource(if (isMizo) mizoRes else englishRes)
}