package com.example.fluidcheck.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.R
import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    dailyGoal: Int,
    totalIntake: Int,
    logs: List<FluidLog>,
    onUpdateGoal: (Int) -> Unit,
    onEditLog: (FluidLog) -> Unit
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    var showAchievementDialog by rememberSaveable { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var hasShownAchievementThisSession by rememberSaveable { mutableStateOf(false) }

    // Check if goal achieved to show dialog
    LaunchedEffect(totalIntake, dailyGoal) {
        if (totalIntake >= dailyGoal && dailyGoal > 0 && !hasShownAchievementThisSession) {
            showAchievementDialog = true
            hasShownAchievementThisSession = true
        } else if (totalIntake < dailyGoal) {
            hasShownAchievementThisSession = false
        }
    }

    if (showGoalDialog) {
        UpdateDailyGoalDialog(
            currentGoal = dailyGoal,
            onDismiss = { showGoalDialog = false },
            onSave = { newGoal ->
                onUpdateGoal(newGoal)
                showGoalDialog = false
            }
        )
    }

    if (showAchievementDialog) {
        GoalAchievedDialog(onDismiss = { showAchievementDialog = false })
    }

    if (showHistoryDialog) {
        LogHistoryDialog(logs = logs, onDismiss = { showHistoryDialog = false })
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        item {
            HeroSection(
                totalIntake = totalIntake,
                dailyGoal = dailyGoal,
                onEditGoalClick = { showGoalDialog = true }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.recent_logs),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp),
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (logs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_logs_yet),
                        color = MutedForeground,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(logs.take(5)) { log ->
                RecentLogItem(log, onEdit = { onEditLog(log) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = { showHistoryDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(AppIcons.History, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.view_full_history), 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun LazyItemScope.HeroSection(
    totalIntake: Int,
    dailyGoal: Int,
    onEditGoalClick: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(GradientStart, GradientMid, GradientEnd)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillParentMaxHeight()
            .background(
                brush = gradient,
                shape = RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp)
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        WaveBackground(modifier = Modifier.fillMaxSize())

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight().padding(vertical = 16.dp)
        ) {
            val progress = if (dailyGoal > 0) totalIntake.toFloat() / dailyGoal.toFloat() else 0f
            HeroProgressRing(
                progress = progress,
                totalIntake = totalIntake,
                dailyGoal = dailyGoal,
                onEditGoalClick = onEditGoalClick
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            StreakPill(days = 12)
            
            Spacer(modifier = Modifier.height(20.dp))
            MetricsGrid(remaining = (dailyGoal - totalIntake).coerceAtLeast(0))
        }
    }
}

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
    onEditGoalClick: () -> Unit
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
            .fillMaxWidth(0.80f)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 24.dp.toPx()
            val innerSize = size.copy(width = size.width - strokeWidthPx, height = size.height - strokeWidthPx)
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

            drawArc(
                color = Color.White.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = innerSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            val lap1Color = Color(0xFFACE6FD)
            drawArc(
                color = lap1Color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = innerSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            if (animatedProgress > 1f) {
                val overflowSweep = 360f * (animatedProgress - 1f).coerceIn(0f, 1f)
                val lap2Color = Color(0xFF40E6FD)

                drawArc(
                    color = lap2Color,
                    startAngle = -90f,
                    sweepAngle = overflowSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = innerSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                drawIntoCanvas { canvas ->
                    val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = strokeWidthPx
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        color = lap2Color.toArgb()
                        setShadowLayer(
                            12.dp.toPx(), 
                            0f, 0f, 
                            Color.Black.copy(alpha = 0.5f).toArgb()
                        )
                    }

                    val rect = android.graphics.RectF(
                        topLeft.x, topLeft.y, 
                        topLeft.x + innerSize.width, topLeft.y + innerSize.height
                    )

                    canvas.nativeCanvas.drawArc(
                        rect,
                        -90f + overflowSweep - 0.1f,
                        0.1f,
                        false,
                        paint
                    )
                }
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
                val displayedPercentage = (animatedProgress.coerceAtMost(1f) * 100).toInt()
                Text(
                    text = displayedPercentage.toString(),
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1.36).sp
                )
                Text(
                    text = "%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                onClick = onEditGoalClick,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "%,d / %,d ml".format(totalIntake, dailyGoal),
                        fontSize = 14.sp,
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
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GoalAchievedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.awesome))
            }
        },
        icon = {
            Icon(
                AppIcons.Goal,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                stringResource(R.string.goal_achieved_title),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                stringResource(R.string.goal_achieved_msg),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun LogHistoryDialog(logs: List<FluidLog>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), fontWeight = FontWeight.Bold, color = PrimaryBlue)
            }
        },
        title = {
            Text(
                stringResource(R.string.log_history),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextDark
            )
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                if (logs.isEmpty()) {
                    Text("No logs found.", color = MutedForeground)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(logs.reversed()) { log ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(log.icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            log.type, 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(log.time, fontSize = 12.sp, color = MutedForeground)
                                    }
                                    Text(
                                        "${log.amount}ml", 
                                        fontWeight = FontWeight.Bold, 
                                        color = PrimaryBlue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Visible
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun StreakPill(days: Int) {
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = AppIcons.Streak,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.streak_label, days),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDailyGoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var goalText by remember { mutableStateOf(currentGoal.toString()) }
    var showError by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.daily_goal_dialog_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            if (showError) {
                Text(
                    text = "Please input a valid goal.",
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            OutlinedTextField(
                value = goalText,
                onValueChange = { if (it.all { char -> char.isDigit() }) goalText = it },
                label = { Text(stringResource(R.string.daily_goal_field_label), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                placeholder = { Text("e.g. 2500", fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (goalText.isBlank()) {
                        showError = true
                    } else {
                        showError = false
                        val newGoal = goalText.toIntOrNull() ?: currentGoal
                        onSave(newGoal)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.save_changes).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun MetricsGrid(remaining: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        MetricCard(
            label = stringResource(R.string.remaining_intake),
            value = "%,d".format(remaining),
            unit = stringResource(R.string.ml_unit),
            modifier = Modifier.fillMaxWidth(0.9f)
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    unit: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.7f),
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
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
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

@Composable
fun RecentLogItem(log: FluidLog, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                color = PrimaryBlue.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = log.icon,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.type,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.Schedule,
                        contentDescription = null,
                        tint = MutedForeground,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = log.time,
                        fontSize = 12.sp,
                        color = MutedForeground
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${log.amount}ml",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PrimaryBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = AppIcons.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
