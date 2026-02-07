package com.mapuia.khawchinthlirna.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Primary text color - fully visible against app background
 * Dark mode: White text for dark backgrounds
 * Light mode: Dark text (not used since our app always has dark gradient bg)
 */
@Composable
fun appTextPrimary(alpha: Float = 1f): Color {
    // Always use white text since our app background is always a dark gradient
    val base = Color.White
    return base.copy(alpha = alpha)
}

/**
 * Secondary text color - slightly muted but still clearly visible
 */
@Composable
fun appTextSecondary(alpha: Float = 1f): Color {
    // Always use white text since our app background is always a dark gradient
    val base = Color.White
    return base.copy(alpha = alpha)
}

/**
 * Muted text color - for less important information
 */
@Composable
fun appTextMuted(alpha: Float = 1f): Color {
    // Always use white text since our app background is always a dark gradient
    val base = Color.White
    return base.copy(alpha = alpha)
}

/**
 * Icon tint color - matches text for consistency
 */
@Composable
fun appIconTint(alpha: Float = 1f): Color = appTextPrimary(alpha)

/**
 * Text color specifically for use on colored buttons (like green accent buttons)
 * This ensures text is readable on colored backgrounds
 */
@Composable
fun appTextOnAccent(): Color {
    return Color.Black
}
