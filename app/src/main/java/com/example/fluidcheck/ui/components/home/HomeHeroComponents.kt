package com.example.fluidcheck.ui.components.home

import com.example.fluidcheck.ui.screens.*

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.R
import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.model.QuickAddConfig
import com.example.fluidcheck.model.ALL_FLUID_TYPES
import com.example.fluidcheck.model.DEFAULT_QUICK_ADD_CONFIGS
import com.example.fluidcheck.model.getIconForFluidType
import com.example.fluidcheck.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WaveBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path1 = Path().apply {
            moveTo(0f, size.height * 0.2f)
            quadraticTo(size.width * 0.25f, size.height * 0.1f, size.width * 0.5f, size.height * 0.2f)
            quadraticTo(size.width * 0.75f, size.height * 0.3f, size.width, size.height * 0.2f)
        }
        drawPath(
            path = path1,
            color = Color.White.copy(alpha = 0.08f),
            style = Stroke(width = 40.dp.toPx(), cap = StrokeCap.Round)
        )
        
        val path2 = Path().apply {
            moveTo(0f, size.height * 0.4f)
            quadraticTo(size.width * 0.35f, size.height * 0.3f, size.width * 0.6f, size.height * 0.4f)
            quadraticTo(size.width * 0.85f, size.height * 0.5f, size.width, size.height * 0.4f)
        }
        drawPath(
            path = path2,
            color = Color.White.copy(alpha = 0.06f),
            style = Stroke(width = 60.dp.toPx(), cap = StrokeCap.Round)
        )

        val path3 = Path().apply {
            moveTo(0f, size.height * 0.7f)
            quadraticTo(size.width * 0.2f, size.height * 0.8f, size.width * 0.5f, size.height * 0.7f)
            quadraticTo(size.width * 0.8f, size.height * 0.6f, size.width, size.height * 0.7f)
        }
        drawPath(
            path = path3,
            color = Color.White.copy(alpha = 0.04f),
            style = Stroke(width = 50.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun HeroProgressRing(
    progress: Float,
    totalIntake: Int,
    dailyGoal: Int,
    onEditGoalClick: () -> Unit,
    isInteractionEnabled: Boolean = true,
    onTapBackground: () -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "ProgressAnimation"
    )

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

            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = innerSize.width / 2,
                center = center,
                style = Stroke(width = strokeWidthPx)
            )

            val lap1Color = Color(0xFFACE6FD)
            val lap2Color = Color(0xFF0284C7)

            val numFullLaps = animatedProgress.toInt()
            val currentLapProgress = animatedProgress % 1f

            if (numFullLaps > 0) {
                val baseColor = if (numFullLaps % 2 == 1) lap1Color else lap2Color
                drawCircle(
                    color = baseColor,
                    radius = innerSize.width / 2,
                    center = center,
                    style = Stroke(width = strokeWidthPx)
                )
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

                val radius = innerSize.width / 2
                val centerX = size.width / 2
                val centerY = size.height / 2
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
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                val displayedPercentage = (animatedProgress * 100).toInt()
                Text(
                    text = displayedPercentage.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, fontSize = 64.sp),
                    color = Color.White,
                    letterSpacing = (-1.28).sp
                )
                Text(
                    text = "%",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                modifier = Modifier.fillMaxWidth(0.45f),
                enabled = isInteractionEnabled
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.ml_count_format, totalIntake, dailyGoal),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
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
}

@Composable
fun StreakPill(days: Int, onClick: () -> Unit = {}) {
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        onClick = onClick,
        interactionSource = remember { MutableInteractionSource() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = AppIcons.Streak,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.streak_label, days),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MetricsGrid(remaining: Int, isClosed: Boolean, onTapBackground: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTapBackground() })
            },
        contentAlignment = Alignment.Center
    ) {
        MetricCard(
            label = if (isClosed) stringResource(R.string.progress_ring_closed) else stringResource(R.string.remaining_intake),
            value = if (isClosed) "0" else "%,d".format(remaining),
            unit = stringResource(R.string.ml_unit),
            isHighlighted = isClosed,
            modifier = Modifier.fillMaxWidth(0.6f)
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    unit: String? = null,
    icon: ImageVector? = null,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GlowTransition")
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    val glowSpread by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowSpread"
    )

    val glowColor = GlassBlue

    Surface(
        color = if (isHighlighted) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.then(
            if (isHighlighted) {
                Modifier.drawBehind {
                    drawIntoCanvas { canvas ->
                        val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
                            isAntiAlias = true
                            style = android.graphics.Paint.Style.FILL
                            color = android.graphics.Color.TRANSPARENT
                            setShadowLayer(
                                glowSpread.dp.toPx(),
                                0f, 0f,
                                glowColor.copy(alpha = glowAlpha).toArgb()
                            )
                        }
                        val rect = android.graphics.RectF(
                            0f, 0f,
                            size.width, size.height
                        )
                        canvas.nativeCanvas.drawRoundRect(
                            rect,
                            24.dp.toPx(), 24.dp.toPx(),
                            paint
                        )
                    }
                    
                    drawRoundRect(
                        color = glowColor.copy(alpha = glowAlpha * 0.5f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                        style = Stroke(width = (glowSpread / 2).dp.toPx())
                    )
                    
                    drawRoundRect(
                        color = Color.White.copy(alpha = glowAlpha),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            } else Modifier
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                color = if (isHighlighted) Color.White else Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Bottom).padding(bottom = 2.dp)
                    )
                }
                if (icon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
