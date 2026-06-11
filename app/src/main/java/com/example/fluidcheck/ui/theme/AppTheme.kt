package com.example.fluidcheck.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AppThemeId(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    OCEAN_BLUE("Ocean Blue"),
    SUNSET_ORANGE("Sunset Orange"),
    FOREST_GREEN("Forest Green"),
    MIDNIGHT_PURPLE("Midnight Purple"),
    PASTEL_SPRING("Pastel Spring"),
    CLASSIC_SEPIA("Classic Sepia"),
    CYBERPUNK_NEON("Cyberpunk Neon")
}

data class ThemePalette(
    val colorScheme: ColorScheme,
    val fluidCheckColors: FluidCheckColors
)

fun getThemePalette(themeId: AppThemeId): ThemePalette {
    return when (themeId) {
        AppThemeId.LIGHT -> ThemePalette(
            colorScheme = lightColorScheme(
                primary = PrimaryBlue,
                secondary = AccentBlue,
                tertiary = SuccessGreen,
                background = Slate50, // More neutral white/gray instead of blue
                surface = Color.White,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color.Black,
                onSurface = Color.Black,
                surfaceVariant = Slate50,
                outline = Slate100,
                onSurfaceVariant = MutedForeground,
                error = ErrorRed
            ),
            fluidCheckColors = FluidCheckColors(
                heroGradientStart = GradientStart,
                heroGradientMid = GradientMid,
                heroGradientEnd = GradientEnd,
                ringLap1 = Sky200,
                ringLap2 = PrimaryBlue,
                cardBackground = Color.White,
                cardBorder = Slate100,
                streakColor = Color(0xFFFF9800),
                isDark = false
            )
        )
        AppThemeId.DARK -> ThemePalette(
            colorScheme = darkColorScheme(
                primary = PrimaryBlue,
                secondary = AccentBlue,
                tertiary = SuccessGreen,
                background = DarkBackground,
                surface = DarkSurface,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White,
                surfaceVariant = DarkSurfaceVariant,
                outline = DarkOutline,
                onSurfaceVariant = Color.LightGray,
                error = ErrorRed
            ),
            fluidCheckColors = FluidCheckColors(
                heroGradientStart = DarkGradientStart,
                heroGradientMid = DarkGradientMid,
                heroGradientEnd = DarkGradientEnd,
                ringLap1 = AccentBlue,
                ringLap2 = Blue600,
                cardBackground = DarkSurface,
                cardBorder = DarkOutline,
                streakColor = Gold,
                isDark = true
            )
        )
        AppThemeId.OCEAN_BLUE -> ThemePalette(
            colorScheme = lightColorScheme(
                primary = OceanPrimary,
                secondary = OceanSecondary,
                background = OceanBackground,
                surface = OceanSurface,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = OceanTextDark,
                onSurface = OceanTextDark,
                surfaceVariant = OceanSurfaceVariant,
                outline = OceanOutline,
                onSurfaceVariant = MutedForeground
            ),
            fluidCheckColors = FluidCheckColors(
                heroGradientStart = OceanPrimary,
                heroGradientMid = OceanSecondary,
                heroGradientEnd = OceanGradientEnd,
                ringLap1 = OceanRing1,
                ringLap2 = OceanPrimary,
                cardBackground = OceanSurface,
                cardBorder = OceanOutline,
                streakColor = OceanSecondary,
                isDark = false
            )
        )
        AppThemeId.SUNSET_ORANGE -> ThemePalette(
            colorScheme = lightColorScheme(
                primary = SunsetPrimary,
                secondary = SunsetSecondary,
                background = SunsetBackground,
                surface = Color.White,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = TextDark,
                onSurface = TextDark,
                surfaceVariant = SunsetSurfaceVariant,
                outline = SunsetOutline,
                onSurfaceVariant = MutedForeground
            ),
            fluidCheckColors = FluidCheckColors(
                heroGradientStart = SunsetPrimary,
                heroGradientMid = SunsetSecondary,
                heroGradientEnd = SunsetGradientEnd,
                ringLap1 = SunsetRing1,
                ringLap2 = SunsetPrimary,
                cardBackground = Color.White,
                cardBorder = SunsetOutline,
                streakColor = SunsetPrimary,
                isDark = false
            )
        )
        AppThemeId.FOREST_GREEN -> ThemePalette(
            colorScheme = lightColorScheme(
                primary = ForestPrimary,
                secondary = ForestSecondary,
                background = ForestBackground,
                surface = Color.White,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = TextDark,
                onSurface = TextDark,
                surfaceVariant = ForestSurfaceVariant,
                outline = ForestOutline,
                onSurfaceVariant = MutedForeground
            ),
            fluidCheckColors = FluidCheckColors(
                heroGradientStart = ForestPrimary,
                heroGradientMid = ForestSecondary,
                heroGradientEnd = ForestGradientEnd,
                ringLap1 = ForestRing1,
                ringLap2 = ForestPrimary,
                cardBackground = Color.White,
                cardBorder = ForestOutline,
                streakColor = ForestPrimary,
                isDark = false
            )
        )
        AppThemeId.MIDNIGHT_PURPLE -> ThemePalette(
            colorScheme = darkColorScheme(
                primary = MidnightPrimary,
                secondary = MidnightSecondary,
                background = MidnightBackground,
                surface = MidnightSurface,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White,
                surfaceVariant = MidnightSurfaceVariant,
                outline = MidnightOutline,
                onSurfaceVariant = Color.LightGray
            ),
            fluidCheckColors = FluidCheckColors(
                heroGradientStart = MidnightGradientStart,
                heroGradientMid = MidnightGradientMid,
                heroGradientEnd = MidnightGradientEnd,
                ringLap1 = MidnightRing1,
                ringLap2 = MidnightPrimary,
                cardBackground = MidnightSurface,
                cardBorder = MidnightOutline,
                streakColor = Gold,
                isDark = true
            )
        )
        AppThemeId.PASTEL_SPRING -> ThemePalette(
            colorScheme = lightColorScheme(
                primary = PastelPrimary,
                secondary = PastelSecondary,
                background = PastelBackground,
                surface = Color.White,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = TextDark,
                onSurface = TextDark,
                surfaceVariant = PastelSurfaceVariant,
                outline = PastelOutline,
                onSurfaceVariant = MutedForeground
            ),
            fluidCheckColors = FluidCheckColors(
                heroGradientStart = PastelPrimary,
                heroGradientMid = PastelSecondary,
                heroGradientEnd = PastelGradientEnd,
                ringLap1 = PastelRing1,
                ringLap2 = PastelPrimary,
                cardBackground = Color.White,
                cardBorder = PastelOutline,
                streakColor = PastelPrimary,
                isDark = false
            )
        )
        AppThemeId.CLASSIC_SEPIA -> ThemePalette(
            colorScheme = lightColorScheme(
                primary = SepiaPrimary,
                secondary = SepiaSecondary,
                background = SepiaBackground,
                surface = SepiaSurface,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = SepiaTextDark,
                onSurface = SepiaTextDark,
                surfaceVariant = SepiaSurfaceVariant,
                outline = SepiaOutline,
                onSurfaceVariant = Color(0xFF6B5C4F)
            ),
            fluidCheckColors = FluidCheckColors(
                heroGradientStart = SepiaPrimary,
                heroGradientMid = SepiaSecondary,
                heroGradientEnd = SepiaGradientEnd,
                ringLap1 = SepiaRing1,
                ringLap2 = SepiaPrimary,
                cardBackground = SepiaSurface,
                cardBorder = SepiaOutline,
                streakColor = SepiaPrimary,
                isDark = false
            )
        )
        AppThemeId.CYBERPUNK_NEON -> ThemePalette(
            colorScheme = darkColorScheme(
                primary = CyberpunkPrimary,
                secondary = CyberpunkSecondary,
                background = CyberpunkBackground,
                surface = CyberpunkSurface,
                onPrimary = Color.Black,
                onSecondary = Color.Black,
                onBackground = CyberpunkTextLight,
                onSurface = CyberpunkTextLight,
                surfaceVariant = CyberpunkSurfaceVariant,
                outline = CyberpunkOutline,
                onSurfaceVariant = Color.LightGray
            ),
            fluidCheckColors = FluidCheckColors(
                heroGradientStart = CyberpunkGradientStart,
                heroGradientMid = CyberpunkGradientMid,
                heroGradientEnd = CyberpunkGradientEnd,
                ringLap1 = CyberpunkPrimary,
                ringLap2 = CyberpunkSecondary,
                cardBackground = CyberpunkSurface,
                cardBorder = CyberpunkOutline,
                streakColor = CyberpunkSecondary,
                isDark = true
            )
        )
    }
}
