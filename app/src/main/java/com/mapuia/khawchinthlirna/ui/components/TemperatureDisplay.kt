package com.mapuia.khawchinthlirna.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset

/**
 * Vibrant temperature colors that pop
 */
fun getTemperatureColor(temp: Double): Color {
    return when {
        temp >= 40 -> Color(0xFFFF1744) // Extreme Hot - Bright Red
        temp >= 35 -> Color(0xFFFF5722) // Very Hot - Deep Orange
        temp >= 30 -> Color(0xFFFF9800) // Hot - Orange
        temp >= 25 -> Color(0xFFFFEB3B) // Warm - Yellow
        temp >= 20 -> Color(0xFF8BC34A) // Pleasant - Light Green
        temp >= 15 -> Color(0xFF00BCD4) // Cool - Cyan
        temp >= 10 -> Color(0xFF2196F3) // Cold - Blue
        temp >= 5 -> Color(0xFF673AB7) // Very Cold - Deep Purple
        else -> Color(0xFFE1F5FE) // Freezing - Ice Blue
    }
}

/**
 * Temperature gradient for backgrounds
 */
fun getTemperatureGradient(temp: Double): Brush {
    return when {
        temp >= 35 -> Brush.linearGradient(
            listOf(Color(0xFFFF512F), Color(0xFFDD2476)) // Hot red-pink
        )
        temp >= 30 -> Brush.linearGradient(
            listOf(Color(0xFFFF9966), Color(0xFFFF5E62)) // Orange-red
        )
        temp >= 25 -> Brush.linearGradient(
            listOf(Color(0xFFFDEB71), Color(0xFFF8D800)) // Golden yellow
        )
        temp >= 20 -> Brush.linearGradient(
            listOf(Color(0xFF11998E), Color(0xFF38EF7D)) // Teal-green
        )
        temp >= 15 -> Brush.linearGradient(
            listOf(Color(0xFF00D2FF), Color(0xFF3A7BD5)) // Cyan-blue
        )
        temp >= 10 -> Brush.linearGradient(
            listOf(Color(0xFF667eea), Color(0xFF764ba2)) // Purple
        )
        else -> Brush.linearGradient(
            listOf(Color(0xFFa8edea), Color(0xFFfed6e3)) // Ice pastel
        )
    }
}

/**
 * Get glow color for temperature-based effects
 */
fun getTemperatureGlow(temp: Double): Color {
    return when {
        temp >= 35 -> Color(0xFFFF5722)
        temp >= 30 -> Color(0xFFFF9800)
        temp >= 25 -> Color(0xFFFFEB3B)
        temp >= 20 -> Color(0xFF4CAF50)
        temp >= 15 -> Color(0xFF00BCD4)
        else -> Color(0xFF2196F3)
    }
}

@Composable
fun ColorfulTemperatureText(
    temperature: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(
        fontSize = 76.sp,
        fontWeight = FontWeight.ExtraBold
    ),
    showGlow: Boolean = true
) {
    val animatedColor by animateColorAsState(
        targetValue = getTemperatureColor(temperature),
        animationSpec = tween(500),
        label = "temp_color"
    )

    val glowColor = getTemperatureGlow(temperature)

    Text(
        text = "${temperature.toInt()}°",
        color = animatedColor,
        style = style.copy(
            shadow = if (showGlow) Shadow(
                color = glowColor.copy(alpha = 0.6f),
                offset = Offset(0f, 0f),
                blurRadius = 20f
            ) else null
        ),
        modifier = modifier
    )
}

/**
 * Large hero temperature display with gradient effect
 */
@Composable
fun HeroTemperatureDisplay(
    temperature: Double,
    modifier: Modifier = Modifier
) {
    val tempColor = getTemperatureColor(temperature)
    val glowColor = getTemperatureGlow(temperature)

    Text(
        text = "${temperature.toInt()}°",
        color = tempColor,
        fontSize = 96.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-4).sp,
        style = TextStyle(
            shadow = Shadow(
                color = glowColor.copy(alpha = 0.5f),
                offset = Offset(0f, 4f),
                blurRadius = 24f
            )
        ),
        modifier = modifier
            .drawBehind {
                // Subtle glow behind text
                drawCircle(
                    color = glowColor.copy(alpha = 0.15f),
                    radius = size.minDimension * 0.8f
                )
            }
    )
}

/**
 * Compact temperature with min/max
 */
@Composable
fun TemperatureRangeDisplay(
    current: Double,
    min: Double,
    max: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        ColorfulTemperatureText(
            temperature = current,
            style = TextStyle(
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Text(
            text = " ${max.toInt()}° / ${min.toInt()}°",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.offset(y = (-12).dp)
        )
    }
}
