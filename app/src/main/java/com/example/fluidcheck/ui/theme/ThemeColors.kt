package com.example.fluidcheck.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class FluidCheckColors(
    val heroGradientStart: Color,
    val heroGradientMid: Color,
    val heroGradientEnd: Color,
    val ringLap1: Color,
    val ringLap2: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val streakColor: Color,
    val isDark: Boolean
)

val LocalFluidCheckColors = staticCompositionLocalOf {
    FluidCheckColors(
        heroGradientStart = Color.Transparent,
        heroGradientMid = Color.Transparent,
        heroGradientEnd = Color.Transparent,
        ringLap1 = Color.Transparent,
        ringLap2 = Color.Transparent,
        cardBackground = Color.Transparent,
        cardBorder = Color.Transparent,
        streakColor = Color.Transparent,
        isDark = false
    )
}
