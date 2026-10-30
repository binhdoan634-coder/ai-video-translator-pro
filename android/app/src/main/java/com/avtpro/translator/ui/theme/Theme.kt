package com.avtpro.translator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AvtProColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonGreen,
    tertiary = NeonCyan,
    background = NavyBackground,
    surface = NavySurface,
    surfaceVariant = NavySurfaceVariant,
    onPrimary = NavyBackground,
    onSecondary = NavyBackground,
    onBackground = WhiteText,
    onSurface = WhiteText,
    onSurfaceVariant = SilverText,
    error = ErrorRed
)

/** Always dark (Dark Mode) brand theme, as specified for this app. */
@Composable
fun AvtProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AvtProColorScheme,
        typography = AvtProTypography,
        content = content
    )
}
