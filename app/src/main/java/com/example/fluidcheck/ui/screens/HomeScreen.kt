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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.R
import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.model.QuickAddConfig
import com.example.fluidcheck.model.ALL_FLUID_TYPES
import com.example.fluidcheck.model.DEFAULT_QUICK_ADD_CONFIGS
import com.example.fluidcheck.model.getIconForFluidType
import com.example.fluidcheck.ui.theme.*
import com.example.fluidcheck.ui.components.ProgressMeterRouter
import com.example.fluidcheck.ui.components.ProgressMeterStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch
import com.example.fluidcheck.util.VolumeUnit
import com.example.fluidcheck.util.MeasurementPreferences
import com.example.fluidcheck.util.MeasurementUtils

@Composable
fun HomeScreen(
    userId: String,
    firestoreRepository: com.example.fluidcheck.repository.FirestoreRepository,
    dailyGoal: Int,
    totalIntake: Int,
    logs: List<FluidLog>, // Today's logs
    streakDays: Int,
    quickAddConfigs: List<QuickAddConfig>?, // Nullable from UserRecord
    onUpdateGoal: (Int) -> Unit,
    onEditLog: (FluidLog) -> Unit,
    onQuickAdd: (QuickAddConfig) -> Unit,
    onUpdateQuickAdd: (List<QuickAddConfig>) -> Unit,
    measurementPreferences: com.example.fluidcheck.util.MeasurementPreferences = com.example.fluidcheck.util.MeasurementPreferences(),
    userRole: String = "FREE USER",
    onPremiumFeatureClick: () -> Unit = {},
    userRecord: com.example.fluidcheck.model.UserRecord? = null,
    onNavigateToProgress: () -> Unit = {},
    progressMeterStyle: String = "RING"
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val allLogsFlow = remember(userId) { firestoreRepository.getFluidLogsFlow(userId) }
    // Note: We only collect this when needed below to save reads
    
    var showGoalDialog by remember { mutableStateOf(false) }
    var showAchievementDialog by rememberSaveable { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val userPreferencesRepository = remember(context) { com.example.fluidcheck.repository.UserPreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    val lastShownDateFlow = remember(userId) { userPreferencesRepository.getLastShownCongratulationsDate(userId) }
    val lastShownDate by lastShownDateFlow.collectAsState(initial = null)

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
    val today = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("GMT+8")
        }.format(java.util.Date())
    }

    LaunchedEffect(totalIntake, dailyGoal, lastShownDate) {
        if (lastShownDate != null && totalIntake >= dailyGoal && dailyGoal > 0 && lastShownDate != today) {
            showAchievementDialog = true
        }
    }

    if (showGoalDialog) {
        UpdateDailyGoalDialog(
            currentGoal = dailyGoal,
            onDismiss = { showGoalDialog = false },
            onSave = { newGoal ->
                onUpdateGoal(newGoal)
                showGoalDialog = false
            },
            volumeUnit = measurementPreferences.volume
        )
    }

    if (showAchievementDialog) {
        GoalAchievedDialog(
            onDismiss = { 
                showAchievementDialog = false 
                scope.launch {
                    userPreferencesRepository.saveLastShownCongratulationsDate(userId, today)
                }
            }
        )
    }

    if (showHistoryDialog) {
        val allLogs by allLogsFlow.collectAsState(initial = null)
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        LogHistoryDialog(
            logs = allLogs, 
            onDismiss = { showHistoryDialog = false },
            onEdit = { 
                onEditLog(it)
            },
            onDeleteLogs = { selectedLogs ->
                scope.launch {
                    try {
                        val result = firestoreRepository.deleteFluidLogs(userId, selectedLogs)
                        if (result.isSuccess) {
                            android.widget.Toast.makeText(context, "Deleted ${selectedLogs.size} logs", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Error deleting logs", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Delete failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
            volumeUnit = measurementPreferences.volume
        )
    }

    if (showQuickAddDialog) {
        AddQuickAddDialog(
            onDismiss = { showQuickAddDialog = false },
            onSave = { config ->
                onUpdateQuickAdd(actualConfigs + config)
                showQuickAddDialog = false
            },
            volumeUnit = measurementPreferences.volume
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(selectionMode) {
                detectTapGestures(onTap = { 
                    focusManager.clearFocus()
                    if (selectionMode) dismissSelection() 
                })
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
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
                    onTapBackground = dismissSelection,
                    volumeUnit = measurementPreferences.volume,
                    userRole = userRole,
                    onPremiumFeatureClick = onPremiumFeatureClick,
                    userRecord = userRecord,
                    onNavigateToProgress = onNavigateToProgress,
                    progressMeterStyle = progressMeterStyle
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
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
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
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
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
                        onTapBackground = dismissSelection,
                        volumeUnit = measurementPreferences.volume
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
                        colors = ButtonDefaults.textButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
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
    onTapBackground: () -> Unit,
    volumeUnit: VolumeUnit,
    userRole: String = "FREE USER",
    onPremiumFeatureClick: () -> Unit = {},
    userRecord: com.example.fluidcheck.model.UserRecord? = null,
    onNavigateToProgress: () -> Unit = {},
    progressMeterStyle: String = "RING"
) {
    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current
    val gradient = Brush.verticalGradient(
        colors = listOf(themeColors.heroGradientStart, themeColors.heroGradientMid, themeColors.heroGradientEnd)
    )

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val actualConfigs = quickAddConfigs ?: com.example.fluidcheck.model.DEFAULT_QUICK_ADD_CONFIGS
    val hasTwoRows = actualConfigs.size > 3
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
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
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = 48.dp, top = 16.dp)
        ) {
            val progress = if (dailyGoal > 0) totalIntake.toFloat() / dailyGoal.toFloat() else 0f
            
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                val styleEnum = try { ProgressMeterStyle.valueOf(progressMeterStyle) } catch (e: Exception) { ProgressMeterStyle.RING }
                ProgressMeterRouter(
                    style = styleEnum,
                    progress = progress,
                    totalIntake = totalIntake,
                    dailyGoal = dailyGoal,
                    onEditGoalClick = onEditGoalClick,
                    isInteractionEnabled = !selectionMode,
                    onTapBackground = onTapBackground,
                    volumeUnit = volumeUnit
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 24.dp)
            ) {
                StreakPill(days = streakDays, onClick = { if (selectionMode) onTapBackground() })
                
                if (userRole != "FREE USER" && userRecord != null && userRecord.activeMissions.isNotEmpty()) {
                    com.example.fluidcheck.ui.components.ActiveMissionTracker(
                        activeMissions = userRecord.activeMissions,
                        userRole = userRole,
                        onNavigateToProgress = onNavigateToProgress
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                MetricsGrid(
                    remaining = (dailyGoal - totalIntake).coerceAtLeast(0), 
                    isClosed = totalIntake >= dailyGoal,
                    onTapBackground = onTapBackground,
                    volumeUnit = volumeUnit
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (userRole != "FREE USER") {
                    HeroQuickAdd(
                        configs = quickAddConfigs,
                        selectionMode = selectionMode,
                        onSelectionModeChange = onSelectionModeChange,
                        selectedConfigs = selectedConfigs,
                        onSelectedConfigsChange = onSelectedConfigsChange,
                        onQuickAdd = onQuickAdd,
                        onUpdateQuickAdd = onUpdateQuickAdd,
                        onAddClick = onAddClick,
                        volumeUnit = volumeUnit
                    )
                } else {
                    // Premium Upsell for Quick Add
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clickable { onPremiumFeatureClick() },
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(AppIcons.Premium, contentDescription = "Premium", tint = com.example.fluidcheck.ui.theme.Gold)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                @Suppress("DEPRECATION")
                                Text("Unlock Quick Add", fontWeight = FontWeight.Bold, color = Color.White)
                                @Suppress("DEPRECATION")
                                Text("Save favorite drinks for 1-tap logging", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
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
    onAddClick: () -> Unit,
    volumeUnit: VolumeUnit
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
                            modifier = Modifier.weight(1f),
                            volumeUnit = volumeUnit
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
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
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
    modifier: Modifier = Modifier,
    volumeUnit: VolumeUnit
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
                text = MeasurementUtils.formatVolumeCompact(LocalContext.current, config.amount, volumeUnit),
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
    onSave: (QuickAddConfig) -> Unit,
    volumeUnit: VolumeUnit = VolumeUnit.METRIC
) {
    var selectedType by remember { mutableStateOf("Water") }
    var amountText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val displayAmount = amountText.trim().toDoubleOrNull() ?: 0.0
                    if (displayAmount > 0) {
                        val amount = MeasurementUtils.convertVolumeToMl(displayAmount, volumeUnit)
                        onSave(QuickAddConfig(amount, selectedType))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary)
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
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amountText = it },
                    label = { Text(MeasurementUtils.volumeLabel(LocalContext.current, volumeUnit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val displayAmount = amountText.trim().toDoubleOrNull() ?: 0.0
                        if (displayAmount > 0) {
                            val amount = MeasurementUtils.convertVolumeToMl(displayAmount, volumeUnit)
                            onSave(QuickAddConfig(amount, selectedType))
                        }
                    }),
                    singleLine = true
                )
            }
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
    )
}

@Composable
fun WaveBackground(modifier: Modifier = Modifier) {
    val waveColorBase = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
    Canvas(modifier = modifier) {
        val path1 = Path().apply {
            moveTo(0f, size.height * 0.2f)
            quadraticTo(size.width * 0.25f, size.height * 0.1f, size.width * 0.5f, size.height * 0.2f)
            quadraticTo(size.width * 0.75f, size.height * 0.3f, size.width, size.height * 0.2f)
        }
        drawPath(
            path = path1,
            color = waveColorBase.copy(alpha = 0.08f),
            style = Stroke(width = 40.dp.toPx(), cap = StrokeCap.Round)
        )
        
        val path2 = Path().apply {
            moveTo(0f, size.height * 0.4f)
            quadraticTo(size.width * 0.35f, size.height * 0.3f, size.width * 0.6f, size.height * 0.4f)
            quadraticTo(size.width * 0.85f, size.height * 0.5f, size.width, size.height * 0.4f)
        }
        drawPath(
            path = path2,
            color = waveColorBase.copy(alpha = 0.06f),
            style = Stroke(width = 60.dp.toPx(), cap = StrokeCap.Round)
        )

        val path3 = Path().apply {
            moveTo(0f, size.height * 0.7f)
            quadraticTo(size.width * 0.2f, size.height * 0.8f, size.width * 0.5f, size.height * 0.7f)
            quadraticTo(size.width * 0.8f, size.height * 0.6f, size.width, size.height * 0.7f)
        }
        drawPath(
            path = path3,
            color = waveColorBase.copy(alpha = 0.04f),
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
    onTapBackground: () -> Unit = {},
    volumeUnit: VolumeUnit
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

            val lap1Color = Sky200
            val lap2Color = Sky700 // Darker Sky Blue for 2nd lap

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
}

@Composable
fun GoalAchievedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
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
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogHistoryDialog(
    logs: List<FluidLog>?,
    onDismiss: () -> Unit,
    onEdit: (FluidLog) -> Unit,
    onDeleteLogs: (List<FluidLog>) -> Unit,
    volumeUnit: VolumeUnit = VolumeUnit.METRIC
) {
    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedLogs by remember { mutableStateOf(setOf<FluidLog>()) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Logs", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete these ${selectedLogs.size} logs?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteLogs(selectedLogs.toList())
                        isSelectionMode = false
                        selectedLogs = emptySet()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
        )
    }

    AlertDialog(
        onDismissRequest = {
            isSelectionMode = false
            selectedLogs = emptySet()
            onDismiss()
        },
        confirmButton = {
            @Suppress("DEPRECATION")
            TextButton(
                onClick = {
                    isSelectionMode = false
                    selectedLogs = emptySet()
                    onDismiss()
                }
            ) {
                @Suppress("DEPRECATION")
                Text(stringResource(R.string.close), fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Text(
                        text = "${selectedLogs.size} selected",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                showDeleteConfirmDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = AppIcons.Delete,
                                contentDescription = "Delete Selected",
                                tint = Color.Red
                            )
                        }
                        TextButton(
                            onClick = {
                                isSelectionMode = false
                                selectedLogs = emptySet()
                            }
                        ) {
                            Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    Text(
                        stringResource(R.string.log_history),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        text = {
            val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
            val groupedLogs = remember(logs) {
                if (logs == null) return@remember emptyList()
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("GMT+8")
                }
                val now = java.util.Date()
                val today = sdf.format(now)
                val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+8"))
                calendar.time = now
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
                val yesterday = sdf.format(calendar.time)

                // Define order
                val order = listOf("Today", "Yesterday", "Previous Logs")
                
                logs.groupBy { log ->
                    when (log.date) {
                        today -> "Today"
                        yesterday -> "Yesterday"
                        else -> "Previous Logs"
                    }
                }.let { groups ->
                    order.mapNotNull { key ->
                        groups[key]?.let { key to it }
                    }
                }
            }

            val sdfToday = remember {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("GMT+8")
                }
            }
            val todayStr = remember { sdfToday.format(java.util.Date()) }

            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                if (logs == null) {
                    LogHistorySkeleton()
                } else if (logs.isEmpty()) {
                    Text("No logs found.", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        groupedLogs.forEach { (header, logItems) ->
                            stickyHeader {
                                Surface(
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    @Suppress("DEPRECATION")
                                    Text(
                                        text = header.uppercase(), 
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                }
                            }
                            items(logItems.reversed()) { log ->
                                val isToday = log.date == todayStr
                                val isLogSelected = selectedLogs.contains(log)
                                val cardBgColor = when {
                                    isLogSelected -> androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else -> themeColors.cardBackground
                                }

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                    border = if (isLogSelected) androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else androidx.compose.foundation.BorderStroke(1.dp, themeColors.cardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            enabled = isToday,
                                            onLongClick = {
                                                if (!isSelectionMode) {
                                                    isSelectionMode = true
                                                    selectedLogs = setOf(log)
                                                }
                                            },
                                            onClick = {
                                                if (isSelectionMode) {
                                                    if (selectedLogs.contains(log)) {
                                                        selectedLogs = selectedLogs - log
                                                        if (selectedLogs.isEmpty()) {
                                                            isSelectionMode = false
                                                        }
                                                    } else {
                                                        selectedLogs = selectedLogs + log
                                                    }
                                                } else {
                                                    onEdit(log)
                                                }
                                            }
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(log.icon, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
                                            val formattedDate = remember(log.date, screenWidth) {
                                                try {
                                                    val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                                    val date = parser.parse(log.date)
                                                    val formatPattern = if (screenWidth < 360) "MMM d, yyyy" else "MMMM d, yyyy"
                                                    val formatter = java.text.SimpleDateFormat(formatPattern, java.util.Locale.getDefault())
                                                    formatter.format(date!!)
                                                } catch (e: Exception) {
                                                    log.date
                                                }
                                            }
                                            @Suppress("DEPRECATION")
                                            Text(
                                                if (header == "Previous Logs") "${log.time} - $formattedDate" else log.time, 
                                                fontSize = 12.sp, 
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            @Suppress("DEPRECATION")
                                            Text(
                                                MeasurementUtils.formatVolumeCompact(LocalContext.current, log.amount, volumeUnit), 
                                                fontWeight = FontWeight.Bold, 
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Visible
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun LogHistorySkeleton() {
    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "SkeletonPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SkeletonAlpha"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.cardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color(0xFFE2E8F0).copy(alpha = alpha), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(14.dp)
                                .background(Color(0xFFE2E8F0).copy(alpha = alpha), RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(10.dp)
                                .background(Color(0xFFE2E8F0).copy(alpha = alpha), RoundedCornerShape(4.dp))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(14.dp)
                            .background(Color(0xFFE2E8F0).copy(alpha = alpha), RoundedCornerShape(4.dp))
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
    onSave: (Int) -> Unit,
    volumeUnit: VolumeUnit
) {
    val displayValue = MeasurementUtils.convertVolumeForDisplay(currentGoal, volumeUnit)
    val initialText = if (displayValue == displayValue.toLong().toDouble()) displayValue.toLong().toString() else "%.1f".format(displayValue)
    var goalText by remember { mutableStateOf(initialText) }
    var showError by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
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
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
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
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) goalText = it },
                label = { 
                    @Suppress("DEPRECATION")
                    Text(MeasurementUtils.dailyGoalLabel(LocalContext.current, volumeUnit), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) 
                },
                placeholder = { Text("e.g. 2500", fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val parsed = goalText.trim().toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        val goalMl = MeasurementUtils.convertVolumeToMl(parsed, volumeUnit)
                        onSave(goalMl)
                    } else {
                        showError = true
                    }
                }),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val parsed = goalText.trim().toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        val goalMl = MeasurementUtils.convertVolumeToMl(parsed, volumeUnit)
                        onSave(goalMl)
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
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
fun MetricsGrid(remaining: Int, isClosed: Boolean, onTapBackground: () -> Unit = {}, volumeUnit: VolumeUnit) {
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
            value = if (isClosed) "0" else MeasurementUtils.formatVolume(LocalContext.current, remaining, volumeUnit),
            unit = null, // formatVolume includes the unit
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

    val glowColor = androidx.compose.material3.MaterialTheme.colorScheme.primary

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
    onTapBackground: () -> Unit = {},
    volumeUnit: VolumeUnit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .pointerInput(isInteractionEnabled) {
                if (!isInteractionEnabled) {
                    detectTapGestures(onTap = { onTapBackground() })
                }
            }
            .then(
                if (isInteractionEnabled && log.isEditable) {
                    Modifier.clickable { onEdit() }
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = log.icon,
                        contentDescription = null,
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
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
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.Schedule,
                        contentDescription = null,
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    @Suppress("DEPRECATION")
                    Text(
                        text = log.time,
                        fontSize = 12.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                @Suppress("DEPRECATION")
                Text(
                    text = MeasurementUtils.formatVolumeCompact(LocalContext.current, log.amount, volumeUnit),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}
