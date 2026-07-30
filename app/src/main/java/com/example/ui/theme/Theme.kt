package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = Color.Black,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldContainer,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = GoldContainer,
    background = DarkBackground,
    onBackground = Color(0xFFECFDF5),
    surface = DarkSurface,
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = Color(0xFF133E31),
    onSurfaceVariant = Color(0xFFA7F3D0)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldDark,
    secondary = GoldAccent,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = Color(0xFF78350F),
    background = LightBackground,
    onBackground = Color(0xFF064E3B),
    surface = LightSurface,
    onSurface = Color(0xFF064E3B),
    surfaceVariant = Color(0xFFE6F4EA),
    onSurfaceVariant = Color(0xFF047857)
)

@Composable
fun SmartNamazTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
