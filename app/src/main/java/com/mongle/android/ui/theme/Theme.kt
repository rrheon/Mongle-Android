package com.mongle.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = MonglePrimary,
    onPrimary = MongleTextOnPrimary,
    primaryContainer = MonglePrimaryLight,
    onPrimaryContainer = MonglePrimaryDarker,
    secondary = MongleAccentOrange,
    onSecondary = MongleTextOnPrimary,
    background = pastelColor(0xFFF8FAF8),
    onBackground = MongleTextPrimary,
    surface = pastelColor(0xFFFDF8F5),
    onSurface = MongleTextPrimary,
    surfaceVariant = MongleCardBackgroundLight,
    onSurfaceVariant = MongleTextSecondary,
    outline = MongleBorder,
    error = MongleError,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = MonglePrimaryDark,
    onPrimary = MongleBackgroundDark,
    primaryContainer = MonglePrimaryLightDark,
    onPrimaryContainer = MonglePrimarySoftDark,
    secondary = MongleMonggleOrange,
    onSecondary = MongleBackgroundDark,
    background = MongleBackgroundDark,
    onBackground = Color.White,
    surface = MongleSurfaceDark,
    onSurface = Color.White,
    surfaceVariant = MongleCardBackgroundDark,
    onSurfaceVariant = pastelColor(0xFFBBBBBB),
    outline = MongleDividerDark,
    error = MongleError,
    onError = Color.White
)

@Composable
fun MongleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MongleTypography,
        content = content
    )
}
