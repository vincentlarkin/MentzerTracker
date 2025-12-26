package com.vincentlarkin.mentzertracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Nova dark theme - refined and sophisticated
private val DarkColorScheme = darkColorScheme(
    primary = NovaAccent,
    onPrimary = Color.White,
    primaryContainer = NovaAccent.copy(alpha = 0.15f),
    onPrimaryContainer = NovaAccent,
    secondary = NovaAccentDark,
    onSecondary = Color.White,
    secondaryContainer = NovaAccentDark.copy(alpha = 0.15f),
    onSecondaryContainer = NovaAccentDark,
    tertiary = NovaInfo,
    onTertiary = Color.White,
    background = NovaBlack,
    onBackground = NovaText,
    surface = NovaDarkSurface,
    onSurface = NovaText,
    surfaceVariant = NovaDarkSurface,
    onSurfaceVariant = NovaTextMuted,
    outline = NovaBorder,
    outlineVariant = NovaBorder.copy(alpha = 0.5f),
    error = Color(0xFFFF6B6B),
    onError = Color.White
)

// Light theme - clean and airy
private val LightColorScheme = lightColorScheme(
    primary = NovaAccent,
    onPrimary = Color.White,
    primaryContainer = NovaAccent.copy(alpha = 0.1f),
    onPrimaryContainer = NovaAccentDark,
    secondary = NovaAccentDark,
    onSecondary = Color.White,
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF1A1A1F),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1F),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF636366),
    outline = Color(0xFFD1D1D6),
    outlineVariant = Color(0xFFE5E5EA),
    error = Color(0xFFFF3B30),
    onError = Color.White
)

@Composable
fun MentzerTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
