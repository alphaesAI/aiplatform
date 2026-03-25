package com.healthpipeline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    // Primary - Black for main actions
    primary = PureBlack,
    onPrimary = PureWhite,
    primaryContainer = Gray200,
    onPrimaryContainer = PureBlack,
    
    // Secondary - Gray for secondary actions
    secondary = Gray700,
    onSecondary = PureWhite,
    secondaryContainer = Gray300,
    onSecondaryContainer = Gray900,
    
    // Tertiary - Nothing Red for accents
    tertiary = NothingRed,
    onTertiary = PureWhite,
    tertiaryContainer = NothingRedLight,
    onTertiaryContainer = NothingRedDark,
    
    // Background & Surface
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    
    // Outline & Border
    outline = BorderLight,
    outlineVariant = Gray200,
    
    // Error
    error = StatusError,
    onError = PureWhite,
    errorContainer = NothingRedLight,
    onErrorContainer = NothingRedDark
)

private val DarkColorScheme = darkColorScheme(
    // Primary - White for main actions
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = Gray800,
    onPrimaryContainer = PureWhite,
    
    // Secondary - Gray for secondary actions
    secondary = Gray400,
    onSecondary = PureBlack,
    secondaryContainer = Gray700,
    onSecondaryContainer = Gray200,
    
    // Tertiary - Nothing Red for accents
    tertiary = NothingRed,
    onTertiary = PureWhite,
    tertiaryContainer = NothingRedDark,
    onTertiaryContainer = NothingRedLight,
    
    // Background & Surface
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    
    // Outline & Border
    outline = BorderDark,
    outlineVariant = Gray900,
    
    // Error
    error = StatusError,
    onError = PureWhite,
    errorContainer = NothingRedDark,
    onErrorContainer = NothingRedLight
)

@Composable
fun HealthPipelineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
