package com.mapuia.khawchinthlirna.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Shared shape constants for consistent styling across the app.
 * Use these instead of creating local RoundedCornerShape instances.
 * 
 * Benefits:
 * - Consistent corner radii throughout the UI
 * - Single source of truth for design system
 * - Reduced object creation (shapes are cached)
 * - Easier to update app-wide styling
 */
object AppShapes {
    // Extra Large - Hero cards, modals
    val ExtraLarge = RoundedCornerShape(24.dp)
    
    // Large - Primary cards, bottom sheets
    val Large = RoundedCornerShape(20.dp)
    
    // Medium - Secondary cards, dialogs
    val Medium = RoundedCornerShape(16.dp)
    
    // Small - Chips, tags, buttons
    val Small = RoundedCornerShape(12.dp)
    
    // Extra Small - Compact elements
    val ExtraSmall = RoundedCornerShape(8.dp)
    
    // Pill - Fully rounded ends (for badges, pills)
    val Pill = RoundedCornerShape(50)
    
    // Bottom Sheet - Only top corners rounded
    val BottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    
    // Top Rounded - Only bottom corners rounded
    val TopRounded = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
}
