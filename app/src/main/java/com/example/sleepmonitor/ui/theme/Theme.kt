package com.example.sleepmonitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = DeepOcean,
    onPrimary = Foam,
    primaryContainer = Sand,
    onPrimaryContainer = Midnight,
    secondary = Aurora,
    onSecondary = Foam,
    secondaryContainer = ColorTokens.SecondarySoft,
    onSecondaryContainer = Midnight,
    tertiary = Coral,
    onTertiary = Foam,
    tertiaryContainer = ColorTokens.TertiarySoft,
    onTertiaryContainer = Midnight,
    background = Foam,
    onBackground = Midnight,
    surface = ColorTokens.Surface,
    onSurface = Midnight,
    surfaceContainerHigh = ColorTokens.Card,
    surfaceContainerLow = ColorTokens.SurfaceLow,
    surfaceBright = ColorTokens.SurfaceBright,
    error = Coral
)

private val DarkColors = darkColorScheme(
    primary = Sand,
    onPrimary = Midnight,
    primaryContainer = DeepOcean,
    onPrimaryContainer = Foam,
    secondary = Aurora,
    onSecondary = Midnight,
    secondaryContainer = NightCard,
    onSecondaryContainer = Foam,
    tertiary = Coral,
    onTertiary = Midnight,
    tertiaryContainer = ColorTokens.DarkTertiarySoft,
    onTertiaryContainer = Foam,
    background = Midnight,
    onBackground = Foam,
    surface = NightSurface,
    onSurface = Foam,
    surfaceContainerHigh = NightCard,
    surfaceContainerLow = DeepOcean,
    surfaceBright = NightCard,
    error = Coral
)

@Composable
fun SleepMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}

private object ColorTokens {
    val SecondarySoft = Color(0xFFD8F1EE)
    val TertiarySoft = Color(0xFFF6D9CF)
    val Surface = Color(0xFFF6FBFD)
    val SurfaceLow = Color(0xFFF0F6F8)
    val SurfaceBright = Color(0xFFFFFFFF)
    val Card = Color(0xFFF9FCFD)
    val DarkTertiarySoft = Color(0xFF4A2B25)
}
