package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FrostedDarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = FrostedGlassBackgroundActive,
    onPrimaryContainer = IndigoAccent,
    secondary = IndigoAccent,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0x306366F1),
    onSecondaryContainer = IndigoAccent,
    tertiary = IndigoAccent,
    onTertiary = Color.White,
    background = FrostedCanvas,
    onBackground = TextLight,
    surface = FrostedGlassBackground,
    onSurface = TextLight,
    surfaceVariant = FrostedGlassBackground,
    onSurfaceVariant = TextSlate,
    surfaceContainerHigh = FrostedGlassSurfaceLight,
    surfaceContainerHighest = Color(0x20FFFFFF),
    outline = FrostedGlassBorder,
    outlineVariant = Color(0x12FFFFFF)
)

// In the Frosted Glass theme, both light and dark systems adhere to the dark glass canvas
// as designed in the specification for eye comfort and ambient glow.
private val FrostedLightColorScheme = FrostedDarkColorScheme

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = FrostedDarkColorScheme,
        typography = Typography,
        content = content
    )
}
