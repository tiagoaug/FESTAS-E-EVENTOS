package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryVioletContainer,
    onPrimary = OnPrimaryVioletContainer,
    primaryContainer = PrimaryViolet,
    onPrimaryContainer = Color.White,
    secondary = SecondaryLavContainer,
    onSecondary = OnSecondaryLavContainer,
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
    surfaceVariant = Color(0xFF2B2832)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryViolet,
    onPrimary = Color.White,
    primaryContainer = PrimaryVioletContainer,
    onPrimaryContainer = OnPrimaryVioletContainer,
    secondary = SecondaryLav,
    onSecondary = Color.White,
    secondaryContainer = SecondaryLavContainer,
    onSecondaryContainer = OnSecondaryLavContainer,
    tertiary = TertiaryRose,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryRoseContainer,
    onTertiaryContainer = OnTertiaryRoseContainer,
    background = BackgroundVibrant,
    onBackground = OnBackgroundVibrant,
    surface = SurfaceWhite,
    onSurface = OnSurfaceVibrant,
    surfaceVariant = SurfaceVariantVibrant,
    onSurfaceVariant = OnSurfaceVariantVibrant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

