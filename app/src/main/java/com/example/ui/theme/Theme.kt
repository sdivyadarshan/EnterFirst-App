package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkHighDensityPrimary,
    onPrimary = DarkHighDensityOnPrimary,
    primaryContainer = DarkHighDensityPrimaryContainer,
    onPrimaryContainer = DarkHighDensityOnPrimaryContainer,
    secondary = DarkHighDensitySecondaryContainer,
    onSecondary = DarkHighDensityOnSecondaryContainer,
    background = DarkHighDensityBackground,
    surface = DarkHighDensitySurface,
    surfaceVariant = DarkHighDensitySurfaceVariant,
    onBackground = DarkHighDensityOnBackground,
    onSurface = DarkHighDensityOnSurface,
    onSurfaceVariant = DarkHighDensityOnSurfaceVariant,
    error = Color(0xFFF2B8B5),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

private val LightColorScheme = lightColorScheme(
    primary = HighDensityPrimary,
    onPrimary = HighDensityOnPrimary,
    primaryContainer = HighDensityPrimaryContainer,
    onPrimaryContainer = HighDensityOnPrimaryContainer,
    secondary = HighDensitySecondaryContainer,
    onSecondary = HighDensityOnSecondaryContainer,
    background = HighDensityBackground,
    surface = HighDensitySurface,
    surfaceVariant = HighDensitySurfaceVariant,
    onBackground = HighDensityOnBackground,
    onSurface = HighDensityOnSurface,
    onSurfaceVariant = HighDensityOnSurfaceVariant,
    error = HighDensityError,
    outline = HighDensityOutline,
    outlineVariant = HighDensityOutlineVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
