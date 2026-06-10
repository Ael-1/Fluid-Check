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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
            // Using the new theme-aware colors, so we duplicate the core of HeroProgressRing here
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
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000))
    val fluidColors = LocalFluidCheckColors.current
    
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

            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = innerSize.width / 2,
                center = center,
                style = Stroke(width = strokeWidthPx)
            )

            val lap1Color = fluidColors.ringLap1
            val lap2Color = fluidColors.ringLap2

            val numFullLaps = animatedProgress.toInt()
            val currentLapProgress = animatedProgress % 1f

            if (numFullLaps > 0) {
                val baseColor = if (numFullLaps % 2 == 1) lap1Color else lap2Color
                drawCircle(color = baseColor, radius = innerSize.width / 2, center = center, style = Stroke(width = strokeWidthPx))
            }

            val activeColor = if (numFullLaps % 2 == 0) lap1Color else lap2Color
            val sweepAngle = 360f * currentLapProgress

            if (sweepAngle > 0f) {
                drawArc(
                    color = activeColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = innerSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
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
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000))
    val fluidColors = LocalFluidCheckColors.current

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.5f)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) detectTapGestures(onTap = { onTapBackground() })
            },
        contentAlignment = Alignment.Center
    ) {
        // Bottle shape
        Box(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .fillMaxWidth(0.8f)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(percent = 20))
                .border(4.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(percent = 20))
                .clip(RoundedCornerShape(percent = 20))
        ) {
            // Fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(animatedProgress.coerceIn(0f, 1f))
                    .background(fluidColors.ringLap1)
                    .align(Alignment.BottomCenter)
            )
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
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000))
    val fluidColors = LocalFluidCheckColors.current
    
    val infiniteTransition = rememberInfiniteTransition()
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(4.dp, Color.White.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) detectTapGestures(onTap = { onTapBackground() })
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progressHeight = size.height * (1f - animatedProgress.coerceIn(0f, 1f))
            
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
                
                drawPath(path, fluidColors.ringLap1)
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
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000))
    val fluidColors = LocalFluidCheckColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) detectTapGestures(onTap = { onTapBackground() })
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MeterCenterContent(animatedProgress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, volumeUnit)
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(fluidColors.ringLap1, RoundedCornerShape(12.dp))
            )
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
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000))
    val fluidColors = LocalFluidCheckColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) detectTapGestures(onTap = { onTapBackground() })
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MeterCenterContent(animatedProgress, totalIntake, dailyGoal, onEditGoalClick, isInteractionEnabled, volumeUnit)
        Spacer(modifier = Modifier.height(16.dp))
        
        val segments = 10
        val filledSegments = (animatedProgress * segments).toInt().coerceIn(0, segments)
        
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
                            if (i < filledSegments) fluidColors.ringLap1 else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(2.dp)
                        )
                )
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
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000))
    val fluidColors = LocalFluidCheckColors.current

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
            val filledDots = (animatedProgress * totalDots).toInt().coerceIn(0, totalDots)
            
            val spacingX = size.width / dotsPerRow
            val spacingY = size.height / dotsPerCol
            val radius = minOf(spacingX, spacingY) * 0.3f
            
            var count = 0
            for (y in (dotsPerCol - 1) downTo 0) {
                for (x in 0 until dotsPerRow) {
                    val cx = x * spacingX + spacingX / 2
                    val cy = y * spacingY + spacingY / 2
                    
                    drawCircle(
                        color = if (count < filledDots) fluidColors.ringLap1 else Color.White.copy(alpha = 0.15f),
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                    count++
                }
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
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(1000))
    val fluidColors = LocalFluidCheckColors.current

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
            drawPath(path, Color.White.copy(alpha = 0.15f))
            
            // Filled drop
            clipRect(top = h * (1f - animatedProgress.coerceIn(0f, 1f)), bottom = h) {
                drawPath(path, fluidColors.ringLap1)
            }
            
            // Outline
            drawPath(path, Color.White.copy(alpha = 0.4f), style = Stroke(4.dp.toPx()))
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
                letterSpacing = (-1.28).sp
            )
            @Suppress("DEPRECATION")
            Text(
                text = "%",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
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
                    overflow = TextOverflow.Ellipsis
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
