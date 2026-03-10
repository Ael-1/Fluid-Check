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

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val actualConfigs = quickAddConfigs ?: com.example.fluidcheck.model.DEFAULT_QUICK_ADD_CONFIGS
    
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
                verticalArrangement = Arrangement.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = 24.dp)
            ) {
                StreakPill(days = streakDays, onClick = { if (selectionMode) onTapBackground() })
                
                Spacer(modifier = Modifier.height(24.dp))
                
                MetricsGrid(
                    remaining = (dailyGoal - totalIntake).coerceAtLeast(0), 
                    isClosed = totalIntake >= dailyGoal,
                    onTapBackground = onTapBackground
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
            Text(
                text = stringResource(R.string.quick_add_label).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                color = Color.White.copy(alpha = 0.6f),
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
                        contentDescription = stringResource(R.string.delete_selected),
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
            Text(
                text = "${config.amount}ml",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
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
                    val amount = amountText.trim().toIntOrNull() ?: 0
                    if (amount > 0) {
                        onSave(QuickAddConfig(amount, selectedType))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(stringResource(R.string.add), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        title = { Text(stringResource(R.string.configure_quick_add), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
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
                        label = { Text(stringResource(R.string.type_label), style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = CardBorder
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        ALL_FLUID_TYPES.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name, style = MaterialTheme.typography.bodyLarge) },
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
                    label = { Text(stringResource(R.string.amount_label), style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("e.g. 250", style = MaterialTheme.typography.bodyLarge) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val amount = amountText.trim().toIntOrNull() ?: 0
                        if (amount > 0) {
                            onSave(QuickAddConfig(amount, selectedType))
                        }
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = CardBorder
                    )
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}
