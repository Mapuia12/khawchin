package com.mapuia.khawchinthlirna.ui.theme

import android.app.Activity
import android.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color as ComposeColor

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    // Ensure dialogs have proper dark surfaces with readable text
    surface = ComposeColor(0xFF1C1B2E),
    surfaceVariant = ComposeColor(0xFF2A2940),
    onSurface = ComposeColor.White,
    onSurfaceVariant = ComposeColor.White.copy(alpha = 0.8f),
)

// Light mode also uses dark surfaces since our app always has dark gradient backgrounds
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    // Use darker surfaces even in light mode for consistency with our dark gradient backgrounds
    surface = ComposeColor(0xFF1C1B2E),
    surfaceVariant = ComposeColor(0xFF2A2940),
    onSurface = ComposeColor.White,
    onSurfaceVariant = ComposeColor.White.copy(alpha = 0.8f),
)

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun KhawchinThlirnaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Do NOT lock icon appearance here.
            // Each screen (day/night gradient) should control light/dark icons.
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
