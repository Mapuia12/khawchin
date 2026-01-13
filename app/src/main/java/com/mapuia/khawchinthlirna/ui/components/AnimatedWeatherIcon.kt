package com.mapuia.khawchinthlirna.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.mapuia.khawchinthlirna.WeatherSvgIcon

@Composable
fun AnimatedWeatherIcon(
    weatherCode: Int,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_animation")
    
    // Sun rotation - slow majestic spin
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_rotation"
    )
    
    // Pulse effect for glow
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Breathing glow alpha
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(modifier = modifier) {
        when {
            // Clear day - rotating sun with glow
            weatherCode == 0 && isDay -> {
                // Glow layer
                Canvas(modifier = Modifier.fillMaxSize().alpha(glowAlpha)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                Color(0xFFFFD700).copy(alpha = 0.6f),
                                Color(0xFFFF8C00).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension * 0.6f
                    )
                }

                Box(
                    modifier = Modifier
                        .rotate(rotation)
                        .scale(pulse)
                ) {
                    WeatherSvgIcon(code = weatherCode, isDay = isDay, modifier = Modifier.fillMaxSize())
                }
            }

            // Clear night - twinkling stars effect
            weatherCode == 0 && !isDay -> {
                TwinklingStars(modifier = Modifier.fillMaxSize())
                WeatherSvgIcon(code = weatherCode, isDay = isDay, modifier = Modifier.fillMaxSize())
            }

            // Rain - animated droplets
            weatherCode in 51..67 || weatherCode in 80..82 -> {
                Box {
                    WeatherSvgIcon(code = weatherCode, isDay = isDay, modifier = Modifier.fillMaxSize())
                    RainDroplets(modifier = Modifier.fillMaxSize(), intensity = if (weatherCode >= 65) 1.5f else 1f)
                }
            }

            // Snow - floating snowflakes
            weatherCode in 71..77 || weatherCode in 85..86 -> {
                Box {
                    WeatherSvgIcon(code = weatherCode, isDay = isDay, modifier = Modifier.fillMaxSize())
                    Snowflakes(modifier = Modifier.fillMaxSize())
                }
            }

            // Thunderstorm - lightning flash
            weatherCode in 95..99 -> {
                Box {
                    LightningFlash(modifier = Modifier.fillMaxSize())
                    WeatherSvgIcon(code = weatherCode, isDay = isDay, modifier = Modifier.fillMaxSize())
                    RainDroplets(modifier = Modifier.fillMaxSize(), intensity = 1.8f)
                }
            }

            // Cloudy - gentle float
            weatherCode in 1..3 || weatherCode in 45..48 -> {
                val floatY by infiniteTransition.animateFloat(
                    initialValue = -3f,
                    targetValue = 3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "cloud_float"
                )

                Box(modifier = Modifier.offset(y = floatY.dp)) {
                    WeatherSvgIcon(code = weatherCode, isDay = isDay, modifier = Modifier.fillMaxSize())
                }
            }

            // Default
            else -> {
                WeatherSvgIcon(code = weatherCode, isDay = isDay, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun RainDroplets(
    modifier: Modifier = Modifier,
    intensity: Float = 1f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((1200 / intensity).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_progress"
    )
    
    Canvas(modifier = modifier) {
        drawRainDrops(animationProgress, intensity)
    }
}

private fun DrawScope.drawRainDrops(progress: Float, intensity: Float) {
    val dropColor = Color(0xFF00D4FF).copy(alpha = 0.7f)
    val dropCount = (10 * intensity).toInt()

    repeat(dropCount) { i ->
        val x = size.width * (i + 1) / (dropCount + 1)
        val startY = -30f
        val endY = size.height + 30f
        val speed = 0.8f + (i % 3) * 0.1f
        val currentY = startY + (endY - startY) * ((progress * speed + i * 0.08f) % 1f)

        // Draw elongated droplet
        drawOval(
            color = dropColor,
            topLeft = androidx.compose.ui.geometry.Offset(x - 2f, currentY - 8f),
            size = androidx.compose.ui.geometry.Size(4f, 16f)
        )
    }
}

@Composable
fun Snowflakes(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "snow")

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_fall"
    )

    val sway by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    Canvas(modifier = modifier) {
        val flakeCount = 8
        repeat(flakeCount) { i ->
            val baseX = size.width * (i + 1) / (flakeCount + 1)
            val x = baseX + sway * 10f * (if (i % 2 == 0) 1 else -1)
            val startY = -20f
            val endY = size.height + 20f
            val speed = 0.6f + (i % 4) * 0.1f
            val currentY = startY + (endY - startY) * ((progress * speed + i * 0.1f) % 1f)

            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 4f + (i % 3) * 2f,
                center = androidx.compose.ui.geometry.Offset(x, currentY)
            )
        }
    }
}

@Composable
fun TwinklingStars(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")

    val twinkle1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle1"
    )

    val twinkle2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle2"
    )

    Canvas(modifier = modifier) {
        // Star positions (relative)
        val stars = listOf(
            Triple(0.2f, 0.15f, twinkle1),
            Triple(0.8f, 0.25f, twinkle2),
            Triple(0.35f, 0.7f, twinkle2),
            Triple(0.7f, 0.8f, twinkle1),
            Triple(0.15f, 0.5f, twinkle1),
        )

        stars.forEach { (xRatio, yRatio, alpha) ->
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = 3f,
                center = androidx.compose.ui.geometry.Offset(
                    size.width * xRatio,
                    size.height * yRatio
                )
            )
        }
    }
}

@Composable
fun LightningFlash(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "lightning")

    val flash by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                0f at 0
                0f at 2000
                1f at 2050
                0f at 2100
                0.7f at 2150
                0f at 2200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "flash"
    )

    Canvas(modifier = modifier.alpha(flash * 0.4f)) {
        drawRect(
            color = Color(0xFFFFFFFF)
        )
    }
}