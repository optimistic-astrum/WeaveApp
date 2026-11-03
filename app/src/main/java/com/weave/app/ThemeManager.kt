package com.weave.app
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
enum class WeaveTheme { DARK, LIGHT, OCEAN, SUNSET }
val DarkColors = darkColorScheme(primary = Color(0xFFFFD700), secondary = Color(0xFFFFB300), background = Color(0xFF121212), surface = Color(0xFF1E1E1E), surfaceVariant = Color(0xFF2A2A2A), onBackground = Color.White, onSurface = Color(0xFFE0E0E0), onSurfaceVariant = Color(0xFFB0B0B0))
val LightColors = lightColorScheme(primary = Color(0xFFFFB300), secondary = Color(0xFFFFD700), background = Color(0xFFFAFAFA), surface = Color.White, surfaceVariant = Color(0xFFF0F0F0), onBackground = Color.Black, onSurface = Color(0xFF2A2A2A), onSurfaceVariant = Color(0xFF6A6A6A))
val OceanColors = darkColorScheme(primary = Color(0xFF4DB6AC), secondary = Color(0xFF29B6F6), background = Color(0xFF061A2E), surface = Color(0xFF0F2A42), surfaceVariant = Color(0xFF1B3B5A), onBackground = Color.White, onSurface = Color(0xFFE0F7FA), onSurfaceVariant = Color(0xFFB2EBF2))
val SunsetColors = darkColorScheme(primary = Color(0xFFFF7043), secondary = Color(0xFFFFCA28), background = Color(0xFF2D1B1B), surface = Color(0xFF3D2626), surfaceVariant = Color(0xFF5A3A3A), onBackground = Color.White, onSurface = Color(0xFFFFE0B2), onSurfaceVariant = Color(0xFFFFCC80))
