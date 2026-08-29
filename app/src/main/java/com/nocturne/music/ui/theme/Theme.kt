package com.nocturne.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NocturneDarkColorScheme = darkColorScheme(
    primary = NocturnePurple,
    onPrimary = TextPrimary,
    primaryContainer = NocturnePurpleDark,
    onPrimaryContainer = TextPrimary,
    secondary = NocturneTeal,
    onSecondary = NocturneDarkBackground,
    background = NocturneDarkBackground,
    onBackground = TextPrimary,
    surface = NocturneDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = NocturneDarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = NocturneRed
)

@Composable
fun NocturneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Nocturne is dark-first by design, consistent with desktop Nocturne
    MaterialTheme(
        colorScheme = NocturneDarkColorScheme,
        typography = NocturneTypography,
        content = content
    )
}
