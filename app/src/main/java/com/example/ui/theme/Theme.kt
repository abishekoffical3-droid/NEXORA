package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NexoraDarkColorScheme = darkColorScheme(
    primary = NexoraPurplePrimary,
    onPrimary = NexoraTextWhite,
    primaryContainer = NexoraNeonPurpleGlow,
    onPrimaryContainer = NexoraTextWhite,
    secondary = NexoraPurpleHighlight,
    onSecondary = NexoraMainBackground,
    secondaryContainer = NexoraCardBackground,
    onSecondaryContainer = NexoraTextWhite,
    background = NexoraMainBackground,
    onBackground = NexoraTextWhite,
    surface = NexoraSecondaryBackground,
    onSurface = NexoraTextWhite,
    surfaceVariant = NexoraCardBackground,
    onSurfaceVariant = NexoraTextSecondary,
    outline = NexoraGlassCardBorder,
    error = NexoraErrorRed,
    onError = NexoraTextWhite
)

val NexoraLightColorScheme = lightColorScheme(
    primary = NexoraPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = NexoraPurpleDark,
    secondary = NexoraPurpleHighlight,
    onSecondary = NexoraLightTextPrimary,
    secondaryContainer = NexoraLightSurfaceVariant,
    onSecondaryContainer = NexoraLightTextPrimary,
    background = NexoraLightBackground,
    onBackground = NexoraLightTextPrimary,
    surface = NexoraLightSurface,
    onSurface = NexoraLightTextPrimary,
    surfaceVariant = NexoraLightSurfaceVariant,
    onSurfaceVariant = NexoraLightTextSecondary,
    outline = NexoraLightBorder,
    error = NexoraErrorRed,
    onError = Color.White
)

@Composable
fun NexoraTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemInDark
    }

    val colorScheme = if (isDark) NexoraDarkColorScheme else NexoraLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
