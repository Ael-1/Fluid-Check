package com.example.fluidcheck.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.R
import com.example.fluidcheck.ui.theme.AppIcons
import com.example.fluidcheck.ui.theme.LocalFluidCheckColors
import com.example.fluidcheck.util.MeasurementUtils
import com.example.fluidcheck.util.VolumeUnit
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos

enum class ProgressMeterStyle(val displayName: String) {
    RING("Progress Ring"),
    BOTTLE_FILL("Bottle Fill"),
    WAVE("Dynamic Wave"),
    LINE_GAUGE("Line Gauge"),
    BATTERY("Battery Segmented"),
    DOT_MATRIX("Dot Matrix"),
    DROP_COUNTER("Liquid Drop")
}

@Composable
fun ProgressMeterRouter(
    style: ProgressMeterStyle,
    progress: Float,
    totalIntake: Int,
    dailyGoal: Int,
    onEditGoalClick: () -> Unit,
    isInteractionEnabled: Boolean = true,
    onTapBackground: () -> Unit = {},
    volumeUnit: VolumeUnit
) {
    when (style) {
        ProgressMeterStyle.RING -> {
            ThemeAwareProgressRing(progress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, onTapBackground, volumeUnit)
        }
        ProgressMeterStyle.BOTTLE_FILL -> {
            BottleFillMeter(progress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, onTapBackground, volumeUnit)
        }
        ProgressMeterStyle.WAVE -> {
            WaveMeter(progress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, onTapBackground, volumeUnit)
        }
        ProgressMeterStyle.LINE_GAUGE -> {
            LineGaugeMeter(progress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, onTapBackground, volumeUnit)
        }
        ProgressMeterStyle.BATTERY -> {
            BatteryMeter(progress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, onTapBackground, volumeUnit)
        }
        ProgressMeterStyle.DOT_MATRIX -> {
            DotMatrixMeter(progress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, onTapBackground, volumeUnit)
        }
        ProgressMeterStyle.DROP_COUNTER -> {
            DropCounterMeter(progress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, onTapBackground, volumeUnit)
        }
    }
}

// Helper drawing functions for radiating effects
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadiatingSparkles(
    radius: Float,
    centerX: Float,
    centerY: Float,
    shineRotation: Float,
    sparkleScale: Float
) {
    val sparkleCount = 8
    for (i in 0 until sparkleCount) {
        val angle = (i * (360f / sparkleCount) + (shineRotation * 0.5f)) % 360f
        val angleRad = (angle * (PI / 180f)).toFloat()
        val sX = centerX + radius * cos(angleRad)
        val sY = centerY + radius * sin(angleRad)
        val individualScale = if (i % 2 == 0) sparkleScale else (1.7f - sparkleScale)
        
        drawCircle(
            color = Color.White.copy(alpha = 0.8f * individualScale),
            radius = 3.dp.toPx() * individualScale,
            center = Offset(sX, sY)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 1. Theme Aware Progress Ring (Default)
// -------------------------------------------------------------------------------------------------
@Composable
fun ThemeAwareProgressRing(
    progress: Float,
    totalIntake: Int,
    dailyGoal: Int,
    onEditGoalClick: () -> Unit,
    isInteractionEnabled: Boolean,
    onTapBackground: () -> Unit,
    volumeUnit: VolumeUnit
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "ProgressAnim")
    val fluidColors = LocalFluidCheckColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "ShineTransition")
    val shineRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShineRotation"
    )
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SparkleScale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) {
                    detectTapGestures(onTap = { onTapBackground() })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 22.dp.toPx()
            val innerSize = size.copy(width = size.width - strokeWidthPx, height = size.height - strokeWidthPx)
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

            val lap1Color = fluidColors.ringLap1
            val lap2Color = fluidColors.ringLap2

            val numFullLaps = animatedProgress.toInt()
            val currentLapProgress = animatedProgress % 1f

            val activeColor = if (numFullLaps % 2 == 0) lap1Color else lap2Color
            val sweepAngle = 360f * currentLapProgress

            drawIntoCanvas { canvas ->
                // Draw background circle with a subtle dark shadow for high contrast on light/pastel backgrounds
                val bgPaint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = strokeWidthPx
                    color = Color.White.copy(alpha = 0.2f).toArgb()
                    setShadowLayer(
                        2.dp.toPx(),
                        0f, 1.dp.toPx(),
                        android.graphics.Color.argb(40, 0, 0, 0)
                    )
                }
                canvas.nativeCanvas.drawCircle(
                    center.x, center.y,
                    innerSize.width / 2,
                    bgPaint
                )

                if (numFullLaps > 0) {
                    val baseColor = if (numFullLaps % 2 == 1) lap1Color else lap2Color
                    val basePaint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = strokeWidthPx
                        color = baseColor.toArgb()
                        setShadowLayer(
                            3.dp.toPx(),
                            0f, 1.5.dp.toPx(),
                            android.graphics.Color.argb(50, 0, 0, 0)
                        )
                    }
                    canvas.nativeCanvas.drawCircle(
                        center.x, center.y,
                        innerSize.width / 2,
                        basePaint
                    )
                }

                if (sweepAngle > 0f) {
                    val activePaint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = strokeWidthPx
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        color = activeColor.toArgb()
                        setShadowLayer(
                            3.dp.toPx(),
                            0f, 1.5.dp.toPx(),
                            android.graphics.Color.argb(50, 0, 0, 0)
                        )
                    }
                    val rect = android.graphics.RectF(
                        topLeft.x, topLeft.y,
                        topLeft.x + innerSize.width,
                        topLeft.y + innerSize.height
                    )
                    canvas.nativeCanvas.drawArc(
                        rect,
                        -90f,
                        sweepAngle,
                        false,
                        activePaint
                    )
                }
            }

            // Radiating Effect when 100% completed or more
            if (animatedProgress >= 1f) {
                rotate(shineRotation) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            0.5f to Color.White.copy(alpha = 0.6f),
                            0.55f to Color.Transparent,
                            1f to Color.Transparent
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = innerSize,
                        style = Stroke(width = strokeWidthPx + 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                drawRadiatingSparkles(
                    radius = innerSize.width / 2,
                    centerX = size.width / 2,
                    centerY = size.height / 2,
                    shineRotation = shineRotation,
                    sparkleScale = sparkleScale
                )
            }
        }
        MeterCenterContent(animatedProgress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, volumeUnit)
    }
}

// -------------------------------------------------------------------------------------------------
// 2. Bottle Fill Meter
// -------------------------------------------------------------------------------------------------
@Composable
fun BottleFillMeter(
    progress: Float,
    totalIntake: Int,
    dailyGoal: Int,
    onEditGoalClick: () -> Unit,
    isInteractionEnabled: Boolean,
    onTapBackground: () -> Unit,
    volumeUnit: VolumeUnit
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "ProgressAnim")
    val fluidColors = LocalFluidCheckColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "ShineTransition")
    val shineRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShineRotation"
    )
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SparkleScale"
    )

    val lap1Color = fluidColors.ringLap1
    val lap2Color = fluidColors.ringLap2
    
    val numFullLaps = animatedProgress.toInt()
    val currentLapProgress = animatedProgress % 1f
    
    val baseColor = if (numFullLaps > 0) {
        if (numFullLaps % 2 == 1) lap1Color else lap2Color
    } else {
        androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
    }
    val activeColor = if (numFullLaps % 2 == 0) lap1Color else lap2Color
    val fillFraction = if (numFullLaps > 0) currentLapProgress else animatedProgress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.5f)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) detectTapGestures(onTap = { onTapBackground() })
            },
        contentAlignment = Alignment.Center
    ) {
        if (animatedProgress >= 1f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRadiatingSparkles(
                    radius = size.width * 0.45f,
                    centerX = size.width / 2,
                    centerY = size.height / 2,
                    shineRotation = shineRotation,
                    sparkleScale = sparkleScale
                )
            }
        }

        // Bottle shape
        Box(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .fillMaxWidth(0.8f)
                .background(baseColor, RoundedCornerShape(percent = 20))
                .border(4.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(percent = 20))
                .clip(RoundedCornerShape(percent = 20))
        ) {
            // Fill
            if (fillFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fillFraction)
                        .background(activeColor)
                        .align(Alignment.BottomCenter)
                )
            }
        }
        MeterCenterContent(animatedProgress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, volumeUnit)
    }
}

// -------------------------------------------------------------------------------------------------
// 3. Dynamic Wave Meter
// -------------------------------------------------------------------------------------------------
@Composable
fun WaveMeter(
    progress: Float,
    totalIntake: Int,
    dailyGoal: Int,
    onEditGoalClick: () -> Unit,
    isInteractionEnabled: Boolean,
    onTapBackground: () -> Unit,
    volumeUnit: VolumeUnit
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "ProgressAnim")
    val fluidColors = LocalFluidCheckColors.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "WaveTransition")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    val shineRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShineRotation"
    )
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SparkleScale"
    )

    val lap1Color = fluidColors.ringLap1
    val lap2Color = fluidColors.ringLap2
    val numFullLaps = animatedProgress.toInt()
    val currentLapProgress = animatedProgress % 1f
    
    val baseColor = if (numFullLaps % 2 == 1) lap1Color else lap2Color
    val activeColor = if (numFullLaps % 2 == 0) lap1Color else lap2Color
    val fillFraction = if (numFullLaps > 0) currentLapProgress else animatedProgress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
            .border(4.dp, Color.White.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) detectTapGestures(onTap = { onTapBackground() })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (numFullLaps > 0) {
                drawCircle(color = baseColor, radius = size.width / 2, center = center)
            }

            val progressHeight = size.height * (1f - fillFraction)
            
            clipRect(top = progressHeight, bottom = size.height) {
                val path = Path()
                path.moveTo(0f, size.height)
                path.lineTo(0f, progressHeight)
                
                val waveLength = size.width
                val amplitude = 15.dp.toPx()
                
                for (x in 0..size.width.toInt() step 5) {
                    val y = progressHeight + sin((x / waveLength + waveOffset) * 2 * PI).toFloat() * amplitude
                    path.lineTo(x.toFloat(), y)
                }
                
                path.lineTo(size.width, size.height)
                path.close()
                
                drawPath(path, activeColor)
            }

            // Radiating effect
            if (animatedProgress >= 1f) {
                val strokeWidthPx = 4.dp.toPx()
                val innerSize = size.copy(width = size.width - strokeWidthPx, height = size.height - strokeWidthPx)
                val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

                rotate(shineRotation) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            0.5f to Color.White.copy(alpha = 0.6f),
                            0.55f to Color.Transparent,
                            1f to Color.Transparent
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = innerSize,
                        style = Stroke(width = strokeWidthPx + 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                drawRadiatingSparkles(
                    radius = innerSize.width / 2,
                    centerX = size.width / 2,
                    centerY = size.height / 2,
                    shineRotation = shineRotation,
                    sparkleScale = sparkleScale
                )
            }
        }
        MeterCenterContent(animatedProgress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, volumeUnit)
    }
}

// -------------------------------------------------------------------------------------------------
// 4. Line Gauge Meter
// -------------------------------------------------------------------------------------------------
@Composable
fun LineGaugeMeter(
    progress: Float,
    totalIntake: Int,
    dailyGoal: Int,
    onEditGoalClick: () -> Unit,
    isInteractionEnabled: Boolean,
    onTapBackground: () -> Unit,
    volumeUnit: VolumeUnit
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "ProgressAnim")
    val fluidColors = LocalFluidCheckColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "ShineTransition")
    val shineRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShineRotation"
    )
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SparkleScale"
    )

    val lap1Color = fluidColors.ringLap1
    val lap2Color = fluidColors.ringLap2
    val numFullLaps = animatedProgress.toInt()
    val currentLapProgress = animatedProgress % 1f
    
    val baseColor = if (numFullLaps > 0) {
        if (numFullLaps % 2 == 1) lap1Color else lap2Color
    } else {
        androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
    }
    val activeColor = if (numFullLaps % 2 == 0) lap1Color else lap2Color
    val fillFraction = if (numFullLaps > 0) currentLapProgress else animatedProgress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) detectTapGestures(onTap = { onTapBackground() })
            },
        contentAlignment = Alignment.Center
    ) {
        if (animatedProgress >= 1f) {
            Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 36.dp)) {
                drawRadiatingSparkles(
                    radius = size.width * 0.4f,
                    centerX = size.width / 2,
                    centerY = size.height / 2,
                    shineRotation = shineRotation,
                    sparkleScale = sparkleScale
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            MeterCenterContent(animatedProgress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, volumeUnit)
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(baseColor, RoundedCornerShape(12.dp))
            ) {
                if (fillFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fillFraction)
                            .fillMaxHeight()
                            .background(activeColor, RoundedCornerShape(12.dp))
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 5. Battery Segmented Meter
// -------------------------------------------------------------------------------------------------
@Composable
fun BatteryMeter(
    progress: Float,
    totalIntake: Int,
    dailyGoal: Int,
    onEditGoalClick: () -> Unit,
    isInteractionEnabled: Boolean,
    onTapBackground: () -> Unit,
    volumeUnit: VolumeUnit
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "ProgressAnim")
    val fluidColors = LocalFluidCheckColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "ShineTransition")
    val shineRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShineRotation"
    )
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SparkleScale"
    )

    val lap1Color = fluidColors.ringLap1
    val lap2Color = fluidColors.ringLap2
    val numFullLaps = animatedProgress.toInt()
    val currentLapProgress = animatedProgress % 1f
    
    val baseColor = if (numFullLaps % 2 == 1) lap1Color else lap2Color
    val activeColor = if (numFullLaps % 2 == 0) lap1Color else lap2Color
    
    val fillFraction = if (numFullLaps > 0) currentLapProgress else animatedProgress.coerceIn(0f, 1f)
    val segments = 10
    val filledSegments = (fillFraction * segments).toInt().coerceIn(0, segments)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) detectTapGestures(onTap = { onTapBackground() })
            },
        contentAlignment = Alignment.Center
    ) {
        if (animatedProgress >= 1f) {
            Canvas(modifier = Modifier.fillMaxSize().padding(bottom = 40.dp)) {
                drawRadiatingSparkles(
                    radius = size.width * 0.4f,
                    centerX = size.width / 2,
                    centerY = size.height / 2,
                    shineRotation = shineRotation,
                    sparkleScale = sparkleScale
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            MeterCenterContent(animatedProgress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, volumeUnit)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Color.Transparent)
                    .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 0 until segments) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (i < filledSegments) activeColor 
                                else if (numFullLaps > 0) baseColor 
                                else Color.White.copy(alpha = 0.1f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 6. Dot Matrix Meter
// -------------------------------------------------------------------------------------------------
@Composable
fun DotMatrixMeter(
    progress: Float,
    totalIntake: Int,
    dailyGoal: Int,
    onEditGoalClick: () -> Unit,
    isInteractionEnabled: Boolean,
    onTapBackground: () -> Unit,
    volumeUnit: VolumeUnit
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "ProgressAnim")
    val fluidColors = LocalFluidCheckColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "ShineTransition")
    val shineRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShineRotation"
    )
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SparkleScale"
    )

    val lap1Color = fluidColors.ringLap1
    val lap2Color = fluidColors.ringLap2
    val numFullLaps = animatedProgress.toInt()
    val currentLapProgress = animatedProgress % 1f
    
    val baseColor = if (numFullLaps % 2 == 1) lap1Color else lap2Color
    val activeColor = if (numFullLaps % 2 == 0) lap1Color else lap2Color
    
    val fillFraction = if (numFullLaps > 0) currentLapProgress else animatedProgress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) detectTapGestures(onTap = { onTapBackground() })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotsPerRow = 10
            val dotsPerCol = 10
            val totalDots = dotsPerRow * dotsPerCol
            val filledDots = (fillFraction * totalDots).toInt().coerceIn(0, totalDots)
            
            val spacingX = size.width / dotsPerRow
            val spacingY = size.height / dotsPerCol
            val radius = minOf(spacingX, spacingY) * 0.3f
            
            var count = 0
            for (y in (dotsPerCol - 1) downTo 0) {
                for (x in 0 until dotsPerRow) {
                    val cx = x * spacingX + spacingX / 2
                    val cy = y * spacingY + spacingY / 2
                    
                    val color = if (count < filledDots) activeColor 
                                else if (numFullLaps > 0) baseColor 
                                else Color.White.copy(alpha = 0.15f)
                    
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                    count++
                }
            }

            // Radiating effect
            if (animatedProgress >= 1f) {
                drawRadiatingSparkles(
                    radius = size.width * 0.5f,
                    centerX = size.width / 2,
                    centerY = size.height / 2,
                    shineRotation = shineRotation,
                    sparkleScale = sparkleScale
                )
            }
        }
        MeterCenterContent(animatedProgress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, volumeUnit)
    }
}

// -------------------------------------------------------------------------------------------------
// 7. Drop Counter Meter
// -------------------------------------------------------------------------------------------------
@Composable
fun DropCounterMeter(
    progress: Float,
    totalIntake: Int,
    dailyGoal: Int,
    onEditGoalClick: () -> Unit,
    isInteractionEnabled: Boolean,
    onTapBackground: () -> Unit,
    volumeUnit: VolumeUnit
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000), label = "ProgressAnim")
    val fluidColors = LocalFluidCheckColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "ShineTransition")
    val shineRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShineRotation"
    )
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SparkleScale"
    )

    val lap1Color = fluidColors.ringLap1
    val lap2Color = fluidColors.ringLap2
    val numFullLaps = animatedProgress.toInt()
    val currentLapProgress = animatedProgress % 1f
    
    val baseColor = if (numFullLaps % 2 == 1) lap1Color else lap2Color
    val activeColor = if (numFullLaps % 2 == 0) lap1Color else lap2Color
    
    val fillFraction = if (numFullLaps > 0) currentLapProgress else animatedProgress.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) detectTapGestures(onTap = { onTapBackground() })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path()
            val w = size.width
            val h = size.height
            
            // Draw a teardrop shape
            path.moveTo(w / 2, 0f)
            path.cubicTo(w, h * 0.5f, w * 0.9f, h, w / 2, h)
            path.cubicTo(w * 0.1f, h, 0f, h * 0.5f, w / 2, 0f)
            
            // Background drop
            drawPath(path, if (numFullLaps > 0) baseColor else Color.White.copy(alpha = 0.15f))
            
            // Filled drop
            if (fillFraction > 0f) {
                clipRect(top = h * (1f - fillFraction), bottom = h) {
                    drawPath(path, activeColor)
                }
            }
            
            // Outline
            drawPath(path, Color.White.copy(alpha = 0.4f), style = Stroke(4.dp.toPx()))

            // Radiating effect
            if (animatedProgress >= 1f) {
                drawRadiatingSparkles(
                    radius = w * 0.5f,
                    centerX = w / 2,
                    centerY = h / 2,
                    shineRotation = shineRotation,
                    sparkleScale = sparkleScale
                )
            }
        }
        MeterCenterContent(animatedProgress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, volumeUnit)
    }
}

// -------------------------------------------------------------------------------------------------
// Shared Center Content
// -------------------------------------------------------------------------------------------------
@Composable
private fun MeterCenterContent(
    animatedProgress: Float,
    totalIntake: Int,
    dailyGoal: Int,
    onEditGoalClick: () -> Unit,
    isInteractionEnabled: Boolean,
    volumeUnit: VolumeUnit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            val displayedPercentage = (animatedProgress * 100).toInt()
            @Suppress("DEPRECATION")
            Text(
                text = displayedPercentage.toString(),
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-1.28).sp,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = Offset(2f, 2f),
                        blurRadius = 6f
                    )
                )
            )
            @Suppress("DEPRECATION")
            Text(
                text = "%",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp),
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        offset = Offset(2f, 2f),
                        blurRadius = 6f
                    )
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
            onClick = onEditGoalClick,
            color = Color.White.copy(alpha = 0.2f),
            shape = RoundedCornerShape(50.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            modifier = Modifier.wrapContentWidth(),
            enabled = isInteractionEnabled
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                @Suppress("DEPRECATION")
                Text(
                    text = MeasurementUtils.formatProgressRing(LocalContext.current, totalIntake, dailyGoal, volumeUnit),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.3f),
                            offset = Offset(1f, 1f),
                            blurRadius = 3f
                        )
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = AppIcons.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
