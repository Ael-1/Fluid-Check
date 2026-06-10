package com.example.fluidcheck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun FluidCheckTheme(
    themeId: AppThemeId = AppThemeId.LIGHT,
    content: @Composable () -> Unit
) {
    val themePalette = getThemePalette(themeId)

    CompositionLocalProvider(
        LocalFluidCheckColors provides themePalette.fluidCheckColors
    ) {
        MaterialTheme(
            colorScheme = themePalette.colorScheme,
            typography = Typography,
            content = content
        )
    }
}
