package com.example.fluidcheck.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.ui.theme.*

@Composable
fun HomeScreen() {
    var dailyGoal by remember { mutableStateOf(3000) }
    var totalIntake by remember { mutableStateOf(1200) }
    var showGoalDialog by remember { mutableStateOf(false) }

    if (showGoalDialog) {
        UpdateDailyGoalDialog(
            currentGoal = dailyGoal,
            onDismiss = { showGoalDialog = false },
            onSave = { newGoal ->
                dailyGoal = newGoal
                showGoalDialog = false
            }
        )
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
                text = "Recent Logs",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp),
                color = TextDark
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        val mockLogs = listOf(
            FluidLog("Water", "9:05 AM", "500ml", Icons.Outlined.WaterDrop),
            FluidLog("Coffee", "11:30 AM", "240ml", Icons.Outlined.Coffee),
            FluidLog("Water", "2:15 PM", "300ml", Icons.Outlined.WaterDrop)
        )
        
        items(mockLogs) { log ->
            RecentLogItem(log)
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
            modifier = Modifier.fillMaxHeight().padding(vertical = 40.dp)
        ) {
            val progress = totalIntake.toFloat() / dailyGoal.toFloat()
            HeroProgressRing(
                progress = progress,
                totalIntake = totalIntake,
                dailyGoal = dailyGoal,
                onEditGoalClick = onEditGoalClick
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            StreakPill(days = 12)
            
            Spacer(modifier = Modifier.height(48.dp))
            MetricsGrid(remaining = (dailyGoal - totalIntake).coerceAtLeast(0))
        }
    }
}

@Composable
fun WaveBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path1 = Path().apply {
            moveTo(0f, size.height * 0.2f)
            quadraticBezierTo(size.width * 0.25f, size.height * 0.1f, size.width * 0.5f, size.height * 0.2f)
            quadraticBezierTo(size.width * 0.75f, size.height * 0.3f, size.width, size.height * 0.2f)
        }
        drawPath(
            path = path1,
            color = Color.White.copy(alpha = 0.08f),
            style = Stroke(width = 40.dp.toPx(), cap = StrokeCap.Round)
        )
        
        val path2 = Path().apply {
            moveTo(0f, size.height * 0.4f)
            quadraticBezierTo(size.width * 0.35f, size.height * 0.3f, size.width * 0.6f, size.height * 0.4f)
            quadraticBezierTo(size.width * 0.85f, size.height * 0.5f, size.width, size.height * 0.4f)
        }
        drawPath(
            path = path2,
            color = Color.White.copy(alpha = 0.06f),
            style = Stroke(width = 60.dp.toPx(), cap = StrokeCap.Round)
        )

        val path3 = Path().apply {
            moveTo(0f, size.height * 0.7f)
            quadraticBezierTo(size.width * 0.2f, size.height * 0.8f, size.width * 0.5f, size.height * 0.7f)
            quadraticBezierTo(size.width * 0.8f, size.height * 0.6f, size.width, size.height * 0.7f)
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

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Background ring: 15% alpha white, 18px stroke (approx 6dp)
            drawArc(
                color = Color.White.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Progress arc: Solid white, 18px stroke (approx 6dp)
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = (animatedProgress * 100).toInt().toString(),
                    fontSize = 84.sp, // Display Large spec: 84sp
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1.68).sp // -0.02em of 84sp
                )
                Text(
                    text = "%",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
            
            // Interactive Intake Pill
            Surface(
                onClick = onEditGoalClick,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "%,d / %,d ml".format(totalIntake, dailyGoal),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Goal",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
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
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "$days DAY STREAK",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
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
                text = "Update Daily Goal",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = goalText,
                onValueChange = { if (it.all { char -> char.isDigit() }) goalText = it },
                label = { Text("Daily Goal (ml)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val newGoal = goalText.toIntOrNull() ?: currentGoal
                    onSave(newGoal)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "SAVE CHANGES",
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
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        MetricCard(
            label = "REMAINING INTAKE",
            value = "%,d".format(remaining),
            unit = "ml",
            modifier = Modifier.fillMaxWidth(0.65f)
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
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Bottom).padding(bottom = 4.dp)
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
fun RecentLogItem(log: FluidLog) {
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
                    color = TextDark
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
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
                    text = log.amount,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PrimaryBlue
                )
                IconButton(onClick = { /* Delete */ }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

data class FluidLog(
    val type: String,
    val time: String,
    val amount: String,
    val icon: ImageVector
)
