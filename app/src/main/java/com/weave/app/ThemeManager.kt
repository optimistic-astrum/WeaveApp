package com.weave.app

import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color

enum class WeaveTheme {
    DARK, LIGHT, AMOLED, CYBERPUNK
}

val DarkColors = darkColorScheme(
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFF7C4DFF),
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFF2A2A2A),
    onBackground = Color.White,
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFB0B0B0)
)

val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    secondary = Color(0xFF6A1B9A),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F0F0),
    onBackground = Color.Black,
    onSurface = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFF6A6A6A)
)

val AmoledColors = darkColorScheme(
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFF7C4DFF),
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF151515),
    onBackground = Color.White,
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFB0B0B0)
)

val CyberpunkColors = darkColorScheme(
    primary = Color(0xFFFF006E),
    secondary = Color(0xFF00F5D4),
    background = Color(0xFF0D0221),
    surface = Color(0xFF1A0B2E),
    surfaceVariant = Color(0xFF2A1B3E),
    onBackground = Color.White,
    onSurface = Color(0xFFE0D0FF),
    onSurfaceVariant = Color(0xFFB0A0D0)
)
