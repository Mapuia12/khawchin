package com.mapuia.khawchinthlirna.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Shared premium color palette for consistent theming across the app.
 * Use these colors instead of defining local color constants.
 */
object PremiumColors {
    // Accent colors
    val AccentCyan = Color(0xFF06D6A0)
    val AccentPurple = Color(0xFF8338EC)
    val AccentGold = Color(0xFFFFD166)
    val AccentRed = Color(0xFFEF476F)
    val AccentBlue = Color(0xFF118AB2)
    
    // Glass colors
    val GlassWhite = Color.White.copy(alpha = 0.12f)
    val GlassBorder = Color.White.copy(alpha = 0.2f)
    val GlassHighlight = Color.White.copy(alpha = 0.08f)
    
    // Text colors for dark backgrounds
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.8f)
    val TextTertiary = Color.White.copy(alpha = 0.6f)
    val TextMuted = Color.White.copy(alpha = 0.4f)
    
    // Background gradients
    val PremiumGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F0C29), // Deep Dark Purple
            Color(0xFF302B63), // Cosmic Purple
            Color(0xFF24243E), // Dark Indigo
        )
    )
    
    val NightGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F0C29),
            Color(0xFF302B63),
            Color(0xFF24243E),
        )
    )
    
    val DayGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF667eea),
            Color(0xFF764ba2),
        )
    )
    
    val ClearSkyGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF00B4DB),
            Color(0xFF0083B0),
        )
    )
    
    val RainyGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF0F2027),
            Color(0xFF203A43),
            Color(0xFF2C5364),
        )
    )
    
    val StormGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF141E30),
            Color(0xFF243B55),
            Color(0xFF6441A5),
        )
    )
    
    val CloudyGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF4776E6),
            Color(0xFF8E54E9),
        )
    )
}
