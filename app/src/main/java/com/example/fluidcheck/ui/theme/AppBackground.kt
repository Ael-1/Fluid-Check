package com.example.fluidcheck.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class BackgroundType(val displayName: String) {
    NONE("Theme Default"),
    // Solid colors
    SOLID_CREAM("Cream"),
    SOLID_SLATE("Slate"),
    SOLID_MIDNIGHT("Midnight"),
    SOLID_MINT("Mint"),
    // Gradients
    GRADIENT_DAWN("Dawn"),
    GRADIENT_ARCTIC("Arctic"),
    GRADIENT_LAVENDER("Lavender"),
    GRADIENT_SUNSET("Sunset"),
    // Patterns
    PATTERN_DOTS("Dots"),
    PATTERN_WAVES("Waves"),
    PATTERN_GEOMETRIC("Geometric"),
    PATTERN_LINES("Lines")
}

@Composable
fun AppBackgroundContainer(
    backgroundType: BackgroundType,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val themeBg = MaterialTheme.colorScheme.background
    
    Box(modifier = modifier.fillMaxSize().background(themeBg)) {
        when (backgroundType) {
            BackgroundType.NONE -> {
                // Just use the theme background applied above
            }
            BackgroundType.SOLID_CREAM -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBF7)))
            }
            BackgroundType.SOLID_SLATE -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF1F5F9)))
            }
            BackgroundType.SOLID_MIDNIGHT -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)))
            }
            BackgroundType.SOLID_MINT -> {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF0FDF4)))
            }
            BackgroundType.GRADIENT_DAWN -> {
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFFFFEDD5), Color(0xFFFDE047), Color(0xFFFEF08A)))
                ))
            }
            BackgroundType.GRADIENT_ARCTIC -> {
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFF7DD3FC)))
                ))
            }
            BackgroundType.GRADIENT_LAVENDER -> {
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFFF3E8FF), Color(0xFFE9D5FF), Color(0xFFD8B4FE)))
                ))
            }
            BackgroundType.GRADIENT_SUNSET -> {
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(Color(0xFFFDA4AF), Color(0xFFF43F5E), Color(0xFFBE123C)))
                ))
            }
            BackgroundType.PATTERN_DOTS -> {
                PatternDots()
            }
            BackgroundType.PATTERN_WAVES -> {
                PatternWaves()
            }
            BackgroundType.PATTERN_GEOMETRIC -> {
                PatternGeometric()
            }
            BackgroundType.PATTERN_LINES -> {
                PatternLines()
            }
        }
        
        // Content overlay
        content()
    }
}

@Composable
private fun PatternDots() {
    val dotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 40.dp.toPx()
        val radius = 2.dp.toPx()
        
        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(
                    color = dotColor,
                    radius = radius,
                    center = Offset(x, y)
                )
                y += spacing
            }
            x += spacing
        }
    }
}

@Composable
private fun PatternWaves() {
    val waveColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 100.dp.toPx()
        var y = 0f
        
        while (y < size.height + spacing) {
            val path = Path().apply {
                moveTo(0f, y)
                quadraticTo(size.width * 0.25f, y - 40f, size.width * 0.5f, y)
                quadraticTo(size.width * 0.75f, y + 40f, size.width, y)
            }
            drawPath(
                path = path,
                color = waveColor,
                style = Stroke(width = 2.dp.toPx())
            )
            y += spacing
        }
    }
}

@Composable
private fun PatternGeometric() {
    val lineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 80.dp.toPx()
        var x = 0f
        while (x < size.width + size.height) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(0f, x),
                strokeWidth = 1.dp.toPx()
            )
            x += spacing
        }
    }
}

@Composable
private fun PatternLines() {
    val lineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 60.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx()
            )
            y += spacing
        }
    }
}
