package com.mapuia.khawchinthlirna.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium weather card with glowing accent border and animated shimmer
 */
@Composable
fun ColorfulWeatherCard(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF3A86FF),
    enableGlow: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_glow")
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shape = RoundedCornerShape(24.dp)
    
    Card(
        modifier = modifier
            .shadow(
                elevation = if (enableGlow) 16.dp else 8.dp,
                shape = shape,
                ambientColor = accentColor.copy(alpha = glowAlpha * 0.5f),
                spotColor = accentColor.copy(alpha = glowAlpha)
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.12f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
                // Subtle inner glow
                .background(
                    Brush.radialGradient(
                        listOf(
                            accentColor.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        radius = 500f
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = glowAlpha + 0.2f),
                            Color.White.copy(alpha = 0.25f),
                            accentColor.copy(alpha = glowAlpha * 0.5f)
                        )
                    ),
                    shape = shape
                )
                .padding(20.dp)
        ) {
            Column(content = content)
        }
    }
}

/**
 * Metric card with icon, value, and unit - vibrant edition
 */
@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: @Composable () -> Unit,
    accentBrush: Brush,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    
    // Extract color for glow (approximate)
    val glowColor = Color(0xFF3A86FF)

    Card(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                spotColor = glowColor.copy(alpha = 0.25f)
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.14f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with accent background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentBrush),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                // Accent indicator bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentBrush)
                )
            }
            
            // Title
            androidx.compose.material3.Text(
                text = title,
                color = Color.White.copy(alpha = 0.8f),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                letterSpacing = 1.sp
            )
            
            // Value and unit
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                androidx.compose.material3.Text(
                    text = value,
                    color = Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                androidx.compose.material3.Text(
                    text = unit,
                    color = Color.White.copy(alpha = 0.85f),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}