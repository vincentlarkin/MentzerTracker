package com.example.mentzertracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RedPrimary,
    onPrimary = TextOnDark,
    secondary = RedSecondary,
    onSecondary = TextOnDark,
    background = BlackBackground,
    onBackground = TextOnDark,
    surface = DarkSurface,
    onSurface = TextOnDark,
    error = RedSecondary,
    onError = TextOnDark
)

@Composable
fun MentzerTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography, // from Type.kt
        content = content
    )
}
