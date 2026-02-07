package com.mapuia.khawchinthlirna.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val LightAppGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF0F0C29),
        Color(0xFF302B63),
        Color(0xFF24243E),
    )
)

private val DarkAppGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF0A0A1A),
        Color(0xFF1B1638),
        Color(0xFF120C24),
    )
)

@Composable
fun appBackgroundGradient(): Brush {
    return if (LocalDarkTheme.current) DarkAppGradient else LightAppGradient
}
