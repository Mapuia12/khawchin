package com.mapuia.khawchinthlirna.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapuia.khawchinthlirna.ui.theme.PremiumColors

/**
 * Shared GlassCard component for consistent glass-morphism styling across the app.
 * Use this instead of defining local GlassCard composables in each screen.
 */
@Composable
fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    backgroundColor: Color = PremiumColors.GlassWhite,
    borderColor: Color = PremiumColors.GlassBorder,
    contentPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .padding(contentPadding),
        content = content
    )
}

/**
 * Simple glass card variant with default padding.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    PremiumGlassCard(
        modifier = modifier,
        contentPadding = 4.dp,
        content = content
    )
}

/**
 * Accent-colored glass card for highlighting important content.
 */
@Composable
fun AccentGlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = PremiumColors.AccentCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    PremiumGlassCard(
        modifier = modifier,
        backgroundColor = accentColor.copy(alpha = 0.12f),
        borderColor = accentColor.copy(alpha = 0.3f),
        content = content
    )
}
