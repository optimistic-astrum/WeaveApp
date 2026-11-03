package com.weave.app
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
enum class WeaveTheme { GOLDEN, DARK, LIGHT, OCEAN, SUNSET, FOREST, ROSE, CYBERPUNK, LAVENDER, MONOCHROME }
val GoldenColors = darkColorScheme(primary = Color(0xFFFFD700), secondary = Color(0xFFFFB300), background = Color(0xFF121212), surface = Color(0xFF1E1E1E), surfaceVariant = Color(0xFF2A2A2A), onBackground = Color.White, onSurface = Color(0xFFE0E0E0), onSurfaceVariant = Color(0xFFB0B0B0))
val DarkColors = darkColorScheme(primary = Color(0xFF00E5FF), secondary = Color(0xFF7C4DFF), background = Color(0xFF0A0A0A), surface = Color(0xFF1A1A1A), surfaceVariant = Color(0xFF2A2A2A), onBackground = Color.White, onSurface = Color(0xFFE0E0E0), onSurfaceVariant = Color(0xFFB0B0B0))
val LightColors = lightColorScheme(primary = Color(0xFF1565C0), secondary = Color(0xFF6A1B9A), background = Color(0xFFFAFAFA), surface = Color.White, surfaceVariant = Color(0xFFF0F0F0), onBackground = Color.Black, onSurface = Color(0xFF2A2A2A), onSurfaceVariant = Color(0xFF6A6A6A))
val OceanColors = darkColorScheme(primary = Color(0xFF4DB6AC), secondary = Color(0xFF29B6F6), background = Color(0xFF061A2E), surface = Color(0xFF0F2A42), surfaceVariant = Color(0xFF1B3B5A), onBackground = Color.White, onSurface = Color(0xFFE0F7FA), onSurfaceVariant = Color(0xFFB2EBF2))
val SunsetColors = darkColorScheme(primary = Color(0xFFFF7043), secondary = Color(0xFFFFCA28), background = Color(0xFF2D1B1B), surface = Color(0xFF3D2626), surfaceVariant = Color(0xFF5A3A3A), onBackground = Color.White, onSurface = Color(0xFFFFE0B2), onSurfaceVariant = Color(0xFFFFCC80))
val ForestColors = darkColorScheme(primary = Color(0xFF81C784), secondary = Color(0xFFAED581), background = Color(0xFF0B1B11), surface = Color(0xFF152A1D), surfaceVariant = Color(0xFF234A33), onBackground = Color.White, onSurface = Color(0xFFC8E6C9), onSurfaceVariant = Color(0xFFA5D6A7))
val RoseColors = darkColorScheme(primary = Color(0xFFF06292), secondary = Color(0xFFBA68C8), background = Color(0xFF2D1B1B), surface = Color(0xFF3D2626), surfaceVariant = Color(0xFF5A3A3A), onBackground = Color.White, onSurface = Color(0xFFF8BBD0), onSurfaceVariant = Color(0xFFF48FB1))
val CyberpunkColors = darkColorScheme(primary = Color(0xFFFF006E), secondary = Color(0xFF00F5D4), background = Color(0xFF0D0221), surface = Color(0xFF1A0B2E), surfaceVariant = Color(0xFF2A1B3E), onBackground = Color.White, onSurface = Color(0xFFE0D0FF), onSurfaceVariant = Color(0xFFB0A0D0))
val LavenderColors = lightColorScheme(primary = Color(0xFF9C27B0), secondary = Color(0xFF673AB7), background = Color(0xFFF3E5F5), surface = Color.White, surfaceVariant = Color(0xFFE1BEE7), onBackground = Color.Black, onSurface = Color(0xFF4A148C), onSurfaceVariant = Color(0xFF7B1FA2))
val MonochromeColors = darkColorScheme(primary = Color(0xFFFFFFFF), secondary = Color(0xFFBDBDBD), background = Color(0xFF000000), surface = Color(0xFF121212), surfaceVariant = Color(0xFF212121), onBackground = Color.White, onSurface = Color(0xFFE0E0E0), onSurfaceVariant = Color(0xFF9E9E9E))
fun getThemeColors(theme: WeaveTheme) = when (theme) {
    WeaveTheme.GOLDEN -> GoldenColors; WeaveTheme.DARK -> DarkColors; WeaveTheme.LIGHT -> LightColors
    WeaveTheme.OCEAN -> OceanColors; WeaveTheme.SUNSET -> SunsetColors; WeaveTheme.FOREST -> ForestColors
    WeaveTheme.ROSE -> RoseColors; WeaveTheme.CYBERPUNK -> CyberpunkColors; WeaveTheme.LAVENDER -> LavenderColors
    WeaveTheme.MONOCHROME -> MonochromeColors
}
