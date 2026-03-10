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
import java.util.Locale
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
fun GoalAchievedDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.awesome), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        icon = {
            Icon(
                AppIcons.Goal,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                stringResource(R.string.goal_achieved_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                stringResource(R.string.goal_achieved_msg),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogHistoryDialog(logs: List<FluidLog>, onDismiss: () -> Unit, onEdit: (FluidLog) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = PrimaryBlue)
            }
        },
        title = {
            Text(
                stringResource(R.string.log_history),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextDark
            )
        },
        text = {
            val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
            val groupedLogs = remember(logs) {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getTimeZone("GMT+8")
                }
                val now = java.util.Date()
                val todayStr = sdf.format(now)
                val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+8"))
                calendar.time = now
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
                val yesterdayStr = sdf.format(calendar.time)

                // Define order labels
                val todayLabel = "Today" 
                val yesterdayLabel = "Yesterday"
                val previousLabel = "Previous Logs"
                
                val order = listOf(todayLabel, yesterdayLabel, previousLabel)
                
                logs.groupBy { log ->
                    when (log.date) {
                        todayStr -> todayLabel
                        yesterdayStr -> yesterdayLabel
                        else -> previousLabel
                    }
                }.let { groups ->
                    order.mapNotNull { key ->
                        groups[key]?.let { key to it }
                    }
                }
            }

            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                if (logs.isEmpty()) {
                    Text(stringResource(R.string.no_logs_found), style = MaterialTheme.typography.bodyMedium, color = MutedForeground)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        groupedLogs.forEach { (header, logItems) ->
                            stickyHeader {
                                Surface(
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val displayHeader = when(header) {
                                        "Today" -> stringResource(R.string.today_label)
                                        "Yesterday" -> stringResource(R.string.yesterday_label)
                                        else -> stringResource(R.string.previous_logs_label)
                                    }
                                    Text(
                                        text = displayHeader.uppercase(), 
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                                        color = PrimaryBlue.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                }
                            }
                            items(logItems.reversed()) { log ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = GhostWhite),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = log.isEditable) { onEdit(log) }
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
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val formattedDate = remember(log.date, screenWidth) {
                                                try {
                                                    val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                                    val date = parser.parse(log.date)
                                                    val formatPattern = if (screenWidth < 360) "MMM d, yyyy" else "MMMM d, yyyy"
                                                    val formatter = java.text.SimpleDateFormat(formatPattern, java.util.Locale.getDefault())
                                                    if (date != null) formatter.format(date) else log.date
                                                } catch (e: Exception) {
                                                    log.date
                                                }
                                            }
                                            Text(
                                                if (header == "Previous Logs") "${log.time} - $formattedDate" else log.time, 
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MutedForeground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "${log.amount}ml", 
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
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
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
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
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            if (showError) {
                Text(
                    text = stringResource(R.string.invalid_goal_msg),
                    color = ErrorRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            OutlinedTextField(
                value = goalText,
                onValueChange = { if (it.all { char -> char.isDigit() }) goalText = it },
                label = { 
                    Text(stringResource(R.string.daily_goal_field_label), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) 
                },
                placeholder = { Text(stringResource(R.string.goal_placeholder), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val goal = goalText.trim().toIntOrNull()
                    if (goal != null && goal > 0) {
                        onSave(goal)
                    } else {
                        showError = true
                    }
                }),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = CardBorder
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val goal = goalText.trim().toIntOrNull()
                    if (goal != null && goal > 0) {
                        onSave(goal)
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.save_changes).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
