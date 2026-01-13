package com.mapuia.khawchinthlirna.ui.theme

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class GlassTokens(
    val cornerRadius: Dp = 24.dp,
    val padding: PaddingValues = PaddingValues(16.dp),
    val borderAlpha: Float = 0.35f,
    val surfaceAlpha: Float = 0.18f,
    val elevation: Dp = 16.dp,
    val blurRadius: Float = 24f,
    val glowAlpha: Float = 0.12f,
)

val PremiumGlassTokens = GlassTokens()

// More vibrant glass tints
fun glassTint(isDay: Boolean): Color {
    return if (isDay) Color(0xFFFFFFFF) else Color(0xFFE8EFFF)
}

// Gradient border for premium look
fun glassGradientBorder(isDay: Boolean): Brush {
    return if (isDay) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.5f),
                Color(0xFF00D4FF).copy(alpha = 0.3f),
                Color.White.copy(alpha = 0.2f),
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.4f),
                Color(0xFF8338EC).copy(alpha = 0.3f),
                Color.White.copy(alpha = 0.15f),
            )
        )
    }
}

// Inner glow gradient for depth
fun glassInnerGlow(isDay: Boolean): Brush {
    val baseColor = if (isDay) Color(0xFF00D4FF) else Color(0xFF8338EC)
    return Brush.verticalGradient(
        listOf(
            baseColor.copy(alpha = 0.08f),
            Color.Transparent,
            Color.Transparent,
            baseColor.copy(alpha = 0.04f),
        )
    )
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    isDay: Boolean,
    tokens: GlassTokens = PremiumGlassTokens,
    enableGlow: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(tokens.cornerRadius)
    val tint = glassTint(isDay)

    // Subtle animated shimmer effect
    val infiniteTransition = rememberInfiniteTransition(label = "glass_shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = tokens.elevation,
                shape = shape,
                spotColor = if (isDay) Color(0xFF00D4FF).copy(alpha = 0.25f)
                           else Color(0xFF8338EC).copy(alpha = 0.25f),
                ambientColor = Color.Black.copy(alpha = 0.15f),
            )
            .clip(shape)
            .background(tint.copy(alpha = tokens.surfaceAlpha))
            .then(
                if (enableGlow) {
                    Modifier.background(glassInnerGlow(isDay))
                } else Modifier
            )
            .border(
                width = 1.5.dp,
                brush = glassGradientBorder(isDay),
                shape = shape
            )
    ) {
        // Subtle top highlight for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = shimmerAlpha),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = 100f
                    )
                )
        )

        Column(
            modifier = Modifier.padding(tokens.padding),
            content = content,
        )
    }
}

/**
 * Accent Glass Card with colored glow
 */
@Composable
fun AccentGlassSurface(
    modifier: Modifier = Modifier,
    accentColor: Color,
    tokens: GlassTokens = PremiumGlassTokens,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(tokens.cornerRadius)

    Box(
        modifier = modifier
            .shadow(
                elevation = tokens.elevation,
                shape = shape,
                spotColor = accentColor.copy(alpha = 0.35f),
                ambientColor = accentColor.copy(alpha = 0.15f),
            )
            .clip(shape)
            .background(Color.White.copy(alpha = 0.12f))
            .background(
                Brush.verticalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.15f),
                        Color.Transparent,
                        accentColor.copy(alpha = 0.05f),
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.3f),
                        accentColor.copy(alpha = 0.2f),
                    )
                ),
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier.padding(tokens.padding),
            content = content,
        )
    }
}

