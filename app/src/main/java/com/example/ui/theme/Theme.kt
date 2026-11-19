package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val QuantitativeDarkColorScheme = darkColorScheme(
    primary = EmeraldCall,
    onPrimary = ObsidianBg,
    primaryContainer = EmeraldCallDim,
    onPrimaryContainer = EmeraldCall,
    secondary = CyberCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = CyberCyanDim,
    onSecondaryContainer = CyberCyan,
    tertiary = SolarAmber,
    onTertiary = ObsidianBg,
    tertiaryContainer = SolarAmberDim,
    onTertiaryContainer = SolarAmber,
    error = CrimsonPut,
    onError = TextPrimary,
    errorContainer = CrimsonPutDim,
    onErrorContainer = CrimsonPut,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = SlateCard,
    surfaceContainerHigh = SlateCardElevated,
    outline = BorderHairline,
    outlineVariant = BorderHairline
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun IqTradingBotTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = QuantitativeDarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
