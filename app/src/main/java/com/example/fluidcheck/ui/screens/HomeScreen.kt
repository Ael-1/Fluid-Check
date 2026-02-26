package com.example.fluidcheck.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun HomeScreen(
    dailyGoal: Int,
    totalIntake: Int,
    logs: List<FluidLog>, // Today's logs
    allLogs: List<FluidLog>,
    streakDays: Int,
    quickAddConfigs: List<QuickAddConfig>?, // Nullable from UserRecord
    onUpdateGoal: (Int) -> Unit,
    onEditLog: (FluidLog) -> Unit,
    onQuickAdd: (QuickAddConfig) -> Unit,
    onUpdateQuickAdd: (List<QuickAddConfig>) -> Unit
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    var showAchievementDialog by rememberSaveable { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var hasShownAchievementThisSession by rememberSaveable { mutableStateOf(false) }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedConfigs by remember { mutableStateOf(setOf<QuickAddConfig>()) }

    val actualConfigs = quickAddConfigs ?: DEFAULT_QUICK_ADD_CONFIGS

    // Helper to dismiss selection mode
    val dismissSelection = {
        if (selectionMode) {
            selectionMode = false
            selectedConfigs = emptySet()
        }
    }

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
        LogHistoryDialog(
            logs = allLogs, 
            onDismiss = { showHistoryDialog = false },
            onEdit = { 
                onEditLog(it)
                showHistoryDialog = false
            }
        )
    }

    if (showQuickAddDialog) {
        AddQuickAddDialog(
            onDismiss = { showQuickAddDialog = false },
            onSave = { config ->
                onUpdateQuickAdd(actualConfigs + config)
                showQuickAddDialog = false
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(selectionMode) {
                if (selectionMode) {
                    detectTapGestures(onTap = { dismissSelection() })
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        ) {
            item {
                HeroSection(
                    totalIntake = totalIntake,
                    dailyGoal = dailyGoal,
                    streakDays = streakDays,
                    quickAddConfigs = quickAddConfigs,
                    selectionMode = selectionMode,
                    onSelectionModeChange = { selectionMode = it },
                    selectedConfigs = selectedConfigs,
                    onSelectedConfigsChange = { selectedConfigs = it },
                    onQuickAdd = onQuickAdd,
                    onUpdateQuickAdd = onUpdateQuickAdd,
                    onAddClick = { if (!selectionMode) showQuickAddDialog = true },
                    onEditGoalClick = { if (!selectionMode) showGoalDialog = true },
                    onTapBackground = dismissSelection
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(R.string.recent_logs),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = selectionMode,
                            onClick = dismissSelection
                        ),
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
                            .padding(48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = selectionMode,
                                onClick = dismissSelection
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        @Suppress("DEPRECATION")
                        Text(
                            text = stringResource(R.string.no_logs_yet),
                            color = MutedForeground,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(logs.reversed().take(5)) { log ->
                    RecentLogItem(
                        log = log, 
                        onEdit = { if (!selectionMode) onEditLog(log) },
                        isInteractionEnabled = !selectionMode,
                        onTapBackground = dismissSelection
                    )
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
                    @Suppress("DEPRECATION")
                    TextButton(
                        onClick = { if (!selectionMode) showHistoryDialog = true else dismissSelection() },
                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryBlue),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = true
                    ) {
                        Icon(AppIcons.History, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        @Suppress("DEPRECATION")
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
                Spacer(
                    modifier = Modifier
                        .height(120.dp)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = selectionMode,
                            onClick = dismissSelection
                        )
                )
            }
        }
    }
}

@Composable
fun LazyItemScope.HeroSection(
    totalIntake: Int,
    dailyGoal: Int,
    streakDays: Int,
    quickAddConfigs: List<QuickAddConfig>?,
    selectionMode: Boolean,
    onSelectionModeChange: (Boolean) -> Unit,
    selectedConfigs: Set<QuickAddConfig>,
    onSelectedConfigsChange: (Set<QuickAddConfig>) -> Unit,
    onQuickAdd: (QuickAddConfig) -> Unit,
    onUpdateQuickAdd: (List<QuickAddConfig>) -> Unit,
    onAddClick: () -> Unit,
    onEditGoalClick: () -> Unit,
    onTapBackground: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(GradientStart, GradientMid, GradientEnd)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillParentMaxHeight(1f)
            .background(
                brush = gradient,
                shape = RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp)
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        WaveBackground(modifier = Modifier.matchParentSize())

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            val progress = if (dailyGoal > 0) totalIntake.toFloat() / dailyGoal.toFloat() else 0f
            
            Spacer(modifier = Modifier.weight(0.15f))

            Box(
                modifier = Modifier
                    .weight(1.4f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HeroProgressRing(
                    progress = progress,
                    totalIntake = totalIntake,
                    dailyGoal = dailyGoal,
                    onEditGoalClick = onEditGoalClick,
                    isInteractionEnabled = !selectionMode,
                    onTapBackground = onTapBackground
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.weight(0.2f))

                StreakPill(days = streakDays, onClick = { if (selectionMode) onTapBackground() })
                
                Spacer(modifier = Modifier.weight(0.2f))
                
                MetricsGrid(
                    remaining = (dailyGoal - totalIntake).coerceAtLeast(0), 
                    isClosed = totalIntake >= dailyGoal,
                    onTapBackground = onTapBackground
                )
                
                Spacer(modifier = Modifier.weight(0.3f))
                
                HeroQuickAdd(
                    configs = quickAddConfigs,
                    selectionMode = selectionMode,
                    onSelectionModeChange = onSelectionModeChange,
                    selectedConfigs = selectedConfigs,
                    onSelectedConfigsChange = onSelectedConfigsChange,
                    onQuickAdd = onQuickAdd,
                    onUpdateQuickAdd = onUpdateQuickAdd,
                    onAddClick = onAddClick
                )
                
                Spacer(modifier = Modifier.weight(0.15f))
            }
        }
    }
}

@Composable
fun HeroQuickAdd(
    configs: List<QuickAddConfig>?,
    selectionMode: Boolean,
    onSelectionModeChange: (Boolean) -> Unit,
    selectedConfigs: Set<QuickAddConfig>,
    onSelectedConfigsChange: (Set<QuickAddConfig>) -> Unit,
    onQuickAdd: (QuickAddConfig) -> Unit,
    onUpdateQuickAdd: (List<QuickAddConfig>) -> Unit,
    onAddClick: () -> Unit
) {
    val actualConfigs = configs ?: DEFAULT_QUICK_ADD_CONFIGS

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .pointerInput(selectionMode) {
                if (selectionMode) {
                    detectTapGestures(onTap = {
                        onSelectionModeChange(false)
                        onSelectedConfigsChange(emptySet())
                    })
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            @Suppress("DEPRECATION")
            Text(
                text = "QUICK ADD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.sp,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = selectionMode,
                    onClick = {
                        onSelectionModeChange(false)
                        onSelectedConfigsChange(emptySet())
                    }
                )
            )
            
            if (selectionMode) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val newConfigs = actualConfigs.filter { it !in selectedConfigs }
                        onUpdateQuickAdd(newConfigs)
                        onSelectionModeChange(false)
                        onSelectedConfigsChange(emptySet())
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Delete,
                        contentDescription = "Delete selected",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        
        val rows = actualConfigs.chunked(3)
        
        if (actualConfigs.isEmpty()) {
            if (!selectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    QuickAddPlusButton(
                        modifier = Modifier.weight(1f),
                        onClick = onAddClick
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        } else {
            rows.forEachIndexed { rowIndex, rowConfigs ->
                if (rowIndex > 0) Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowConfigs.forEach { config ->
                        HeroQuickAddButton(
                            config = config,
                            onClick = {
                                if (selectionMode) {
                                    val newSelected = if (config in selectedConfigs) {
                                        selectedConfigs - config
                                    } else {
                                        selectedConfigs + config
                                    }
                                    onSelectedConfigsChange(newSelected)
                                } else {
                                    onQuickAdd(config)
                                }
                            },
                            onLongClick = {
                                if (!selectionMode) {
                                    onSelectionModeChange(true)
                                    onSelectedConfigsChange(selectedConfigs + config)
                                }
                            },
                            isSelected = config in selectedConfigs,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    if (!selectionMode && rowIndex == rows.size - 1 && rowConfigs.size < 3) {
                        QuickAddPlusButton(
                            modifier = Modifier.weight(1f),
                            onClick = onAddClick
                        )
                        repeat(3 - rowConfigs.size - 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    } else if (selectionMode && rowIndex == rows.size - 1 && rowConfigs.size < 3) {
                        repeat(3 - rowConfigs.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            if (!selectionMode && rows.isNotEmpty() && rows.last().size == 3 && actualConfigs.size < 6) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    QuickAddPlusButton(
                        modifier = Modifier.weight(1f),
                        onClick = onAddClick
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun QuickAddPlusButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .drawBehind {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(), 
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = AppIcons.Add, 
            contentDescription = null, 
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroQuickAddButton(
    config: QuickAddConfig,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(64.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) Color.White else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                imageVector = getIconForFluidType(config.type),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            @Suppress("DEPRECATION")
            Text(
                text = "${config.amount}ml",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuickAddDialog(
    onDismiss: () -> Unit,
    onSave: (QuickAddConfig) -> Unit
) {
    var selectedType by remember { mutableStateOf("Water") }
    var amountText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toIntOrNull() ?: 0
                    if (amount > 0) {
                        onSave(QuickAddConfig(amount, selectedType))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Configure Quick Add") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        modifier = Modifier.menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        ALL_FLUID_TYPES.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type.name
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    label = { Text("Amount (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        containerColor = Color.White
    )
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
            val lap2Color = Color(0xFF40E6FD)

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
                @Suppress("DEPRECATION")
                Text(
                    text = displayedPercentage.toString(),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1.28).sp
                )
                @Suppress("DEPRECATION")
                Text(
                    text = "%",
                    fontSize = 24.sp,
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
                modifier = Modifier.fillMaxWidth(0.45f),
                enabled = isInteractionEnabled
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = "%,d / %,d ml".format(totalIntake, dailyGoal),
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
                @Suppress("DEPRECATION")
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
            @Suppress("DEPRECATION")
            Text(
                stringResource(R.string.goal_achieved_title),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            @Suppress("DEPRECATION")
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
fun LogHistoryDialog(logs: List<FluidLog>, onDismiss: () -> Unit, onEdit: (FluidLog) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            @Suppress("DEPRECATION")
            TextButton(onClick = onDismiss) {
                @Suppress("DEPRECATION")
                Text(stringResource(R.string.close), fontWeight = FontWeight.Bold, color = PrimaryBlue)
            }
        },
        title = {
            @Suppress("DEPRECATION")
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
                                        @Suppress("DEPRECATION")
                                        Text(
                                            log.type, 
                                            fontWeight = FontWeight.Bold, 
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        @Suppress("DEPRECATION")
                                        Text("${log.time} - ${log.date}", fontSize = 12.sp, color = MutedForeground)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        @Suppress("DEPRECATION")
                                        Text(
                                            "${log.amount}ml", 
                                            fontWeight = FontWeight.Bold, 
                                            color = PrimaryBlue,
                                            maxLines = 1,
                                            overflow = TextOverflow.Visible
                                        )
                                        if (log.isEditable) {
                                            IconButton(onClick = { onEdit(log) }, modifier = Modifier.size(20.dp)) {
                                                Icon(AppIcons.Edit, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
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
            @Suppress("DEPRECATION")
            Text(
                text = stringResource(R.string.streak_label, days),
                fontSize = 13.sp,
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
            @Suppress("DEPRECATION")
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
                label = { 
                    @Suppress("DEPRECATION")
                    Text(stringResource(R.string.daily_goal_field_label), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) 
                },
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
                @Suppress("DEPRECATION")
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
        @Suppress("DEPRECATION")
        MetricCard(
            label = if (isClosed) stringResource(R.string.progress_ring_closed) else stringResource(R.string.remaining_intake),
            value = if (isClosed) "0" else "%,d".format(remaining),
            unit = stringResource(R.string.ml_unit),
            isHighlighted = isClosed,
            modifier = Modifier.fillMaxWidth(0.4f)
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

    val glowColor = Color(0xFF40E6FD)

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
            @Suppress("DEPRECATION")
            Text(
                text = label.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
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
                @Suppress("DEPRECATION")
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
                    @Suppress("DEPRECATION")
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
fun RecentLogItem(
    log: FluidLog, 
    onEdit: () -> Unit, 
    isInteractionEnabled: Boolean = true,
    onTapBackground: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) {
                    detectTapGestures(onTap = { onTapBackground() })
                }
            },
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
                @Suppress("DEPRECATION")
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
                    @Suppress("DEPRECATION")
                    Text(
                        text = log.time,
                        fontSize = 12.sp,
                        color = MutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                @Suppress("DEPRECATION")
                Text(
                    text = "${log.amount}ml",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PrimaryBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
                if (log.isEditable) {
                    IconButton(
                        onClick = onEdit, 
                        modifier = Modifier.size(24.dp),
                        enabled = isInteractionEnabled
                    ) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = if (isInteractionEnabled) Color(0xFFCBD5E1) else Color(0xFFCBD5E1).copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
