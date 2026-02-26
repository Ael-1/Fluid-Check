package com.example.fluidcheck.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.R
import com.example.fluidcheck.model.ChartData
import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.ui.theme.*
import com.google.firebase.Timestamp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

private val PST_ZONE = ZoneId.of("GMT+8")

@Composable
fun ProgressScreen(
    allLogs: List<FluidLog>,
    dailyGoal: Int,
    accountCreatedAt: Timestamp? = null
) {
    var selectedTab by remember { mutableStateOf("Day") }
    var navOffset by remember { mutableIntStateOf(0) }

    // Reset offset when tab changes
    LaunchedEffect(selectedTab) {
        navOffset = 0
    }

    val (navLabel, chartData) = remember(selectedTab, navOffset, allLogs, dailyGoal) {
        getChartDataForRange(selectedTab, navOffset, allLogs, dailyGoal)
    }

    // Dynamic Left Bound: User's Account Creation Date
    val creationDate = remember(accountCreatedAt) {
        accountCreatedAt?.toDate()?.toInstant()?.atZone(PST_ZONE)?.toLocalDate()
            ?: LocalDate.now(PST_ZONE)
    }

    val canGoNext = navOffset < 0
    val canGoPrevious = remember(selectedTab, navOffset, creationDate) {
        val today = LocalDate.now(PST_ZONE)
        val currentTargetDate = today.plusDays(navOffset.toLong())
        
        when (selectedTab) {
            "Day" -> {
                // Can only go back if yesterday is still on or after account creation
                currentTargetDate.minusDays(1) >= creationDate
            }
            "Week" -> {
                // Can go back if the start of the PREVIOUS week is >= creationDate
                val currentWeekStart = currentTargetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                currentWeekStart.minusDays(1) >= creationDate
            }
            "Month" -> {
                // Can go back if the start of the PREVIOUS month is >= creationDate
                val currentMonthStart = currentTargetDate.with(TemporalAdjusters.firstDayOfMonth())
                currentMonthStart.minusDays(1) >= creationDate
            }
            "Year" -> {
                // Can go back if the start of the PREVIOUS year is >= creationDate
                val currentYearStart = currentTargetDate.with(TemporalAdjusters.firstDayOfYear())
                currentYearStart.minusDays(1) >= creationDate
            }
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 24.dp)
    ) {
        @Suppress("DEPRECATION")
        Spacer(modifier = Modifier.height(32.dp))

        @Suppress("DEPRECATION")
        Text(
            text = stringResource(R.string.your_progress),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                color = TextDark,
                letterSpacing = (-0.5).sp
            )
        )

        @Suppress("DEPRECATION")
        Text(
            text = stringResource(R.string.progress_subtitle),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MutedForeground
            ),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Progress Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(40.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time Range Tabs
                TimeRangeTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Date Navigation Bar
                DateNavigationBar(
                    label = navLabel,
                    onPrevious = { navOffset-- },
                    onNext = { navOffset++ },
                    isPreviousEnabled = canGoPrevious,
                    isNextEnabled = canGoNext
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Chart Components
                HydrationLineChart(
                    dataPoints = chartData.points,
                    xOffsets = chartData.xOffsets,
                    xLabels = chartData.xLabels,
                    yLabels = chartData.yLabels,
                    maxValue = chartData.maxValue,
                    rangeType = selectedTab
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TimeRangeTabs(selectedTab: String, onTabSelected: (String) -> Unit) {
    val tabs = listOf("Day", "Week", "Month", "Year")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF1F5F9))
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .padding(2.dp)
                        .then(
                            if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTabSelected(tab)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    @Suppress("DEPRECATION")
                    Text(
                        text = tab,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextDark else MutedForeground
                    )
                }
            }
        }
    }
}

@Composable
fun DateNavigationBar(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isPreviousEnabled: Boolean,
    isNextEnabled: Boolean
) {
    Surface(
        color = AccentBlue.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(32.dp),
                enabled = isPreviousEnabled
            ) {
                Icon(
                    imageVector = AppIcons.ArrowLeft,
                    contentDescription = stringResource(R.string.previous),
                    tint = if (isPreviousEnabled) TextDark else Color(0xFF94A3B8).copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.viewing),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp
                )
                Text(
                    text = label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(32.dp),
                enabled = isNextEnabled
            ) {
                Icon(
                    imageVector = AppIcons.ArrowRight,
                    contentDescription = stringResource(R.string.next),
                    tint = if (isNextEnabled) TextDark else Color(0xFF94A3B8).copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun HydrationLineChart(
    dataPoints: List<Float>,
    xOffsets: List<Float>?,
    xLabels: List<String>,
    yLabels: List<String>,
    maxValue: Float,
    rangeType: String
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    Row(modifier = Modifier
        .fillMaxWidth()
        .height(280.dp)) {
        // Y-Axis Labels
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(44.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            yLabels.reversed().forEach { label ->
                @Suppress("DEPRECATION")
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = MutedForeground,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Chart Area
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Grid Lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val height = size.height
                    val lines = if (yLabels.size > 1) yLabels.size - 1 else 1
                    for (i in 0..lines) {
                        val y = height - (i * (height / lines))
                        drawLine(
                            color = Color(0xFFF1F5F9),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                // Line Chart Visuals
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (dataPoints.isEmpty()) return@Canvas

                    val width = size.width
                    val height = size.height
                    val effectiveMax = if (maxValue <= 0f) 1f else maxValue

                    val allOffsets = dataPoints.mapIndexed { index, value ->
                        val x = if (xOffsets != null && index < xOffsets.size) {
                            xOffsets[index] * width
                        } else {
                            val spacing = if (dataPoints.size > 1) width / (dataPoints.size - 1) else 0f
                            index * spacing
                        }
                        Offset(x = x, y = height - (value / effectiveMax * height))
                    }

                    if (allOffsets.size >= 2) {
                        val path = Path().apply {
                            moveTo(allOffsets.first().x, allOffsets.first().y)
                            for (i in 0 until allOffsets.size - 1) {
                                val p1 = allOffsets[i]
                                val p2 = allOffsets[i + 1]
                                // Smooth curve
                                val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                                val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                                cubicTo(
                                    controlPoint1.x, controlPoint1.y,
                                    controlPoint2.x, controlPoint2.y,
                                    p2.x, p2.y
                                )
                            }
                        }

                        drawPath(
                            path = path,
                            color = PrimaryBlue,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }

                    // Draw nodes (dots) for EVERY point in the provided data
                    allOffsets.forEach { point ->
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            color = PrimaryBlue,
                            radius = 4.dp.toPx(),
                            center = point,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // X-Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                xLabels.forEach { label ->
                    val processedLabel = if (rangeType == "Year" && screenWidth < 450) {
                        label.take(1)
                    } else {
                        label
                    }

                    @Suppress("DEPRECATION")
                    Text(
                        text = processedLabel,
                        fontSize = 11.sp,
                        color = MutedForeground,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(IntrinsicSize.Min)
                    )
                }
            }
        }
    }
}

fun getChartDataForRange(
    range: String,
    offset: Int,
    allLogs: List<FluidLog>,
    dailyGoal: Int
): Pair<String, ChartData> {
    val today = LocalDate.now(PST_ZONE)
    return when (range) {
        "Day" -> {
            val date = today.plusDays(offset.toLong())
            val label = when (offset) {
                0 -> "Today"
                -1 -> "Yesterday"
                else -> date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault()))
            }
            
            val targetDateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()))
            val dayLogs = allLogs.filter { it.date == targetDateStr }
            
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
            val sortedLogs = dayLogs.mapNotNull { log ->
                try {
                    val time = LocalTime.parse(log.time, timeFormatter)
                    log to time
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.second }

            // Cumulative Sum Logic
            var runningTotal = 0f
            val points = mutableListOf<Float>()
            val xOffsets = mutableListOf<Float>()
            
            sortedLogs.forEach { (log, time) ->
                runningTotal += log.amount
                points.add(runningTotal)
                val offsetInDay = time.toSecondOfDay().toFloat() / (24 * 60 * 60)
                xOffsets.add(offsetInDay)
            }

            val chartMax = if (runningTotal > dailyGoal) {
                (runningTotal * 1.2f).coerceAtLeast(100f)
            } else {
                dailyGoal.toFloat()
            }

            Pair(
                label,
                ChartData(
                    points = points,
                    xOffsets = xOffsets,
                    xLabels = listOf("12AM", "4AM", "8AM", "12PM", "4PM", "8PM", "11PM"),
                    yLabels = generateYLabels(chartMax),
                    maxValue = chartMax
                )
            )
        }
        "Week" -> {
            val targetWeek = today.plusWeeks(offset.toLong())
            val monday = targetWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val sunday = monday.plusDays(6)
            
            val label = if (offset == 0) "This Week" 
                       else "${monday.format(DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault()))} - ${sunday.format(DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault()))}"
            
            val weekPoints = mutableListOf<Float>()
            for (i in 0..6) {
                val currentDay = monday.plusDays(i.toLong())
                val dayStr = currentDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault()))
                val total = allLogs.filter { it.date == dayStr }.sumOf { it.amount }.toFloat()
                weekPoints.add(total)
            }

            val limitIndex = if (offset == 0) today.dayOfWeek.value - 1 else 6
            val firstActive = weekPoints.indexOfFirst { it > 0 }
            
            val filteredPoints = mutableListOf<Float>()
            val xOffsets = mutableListOf<Float>()
            if (firstActive != -1 && firstActive <= limitIndex) {
                for (i in firstActive..limitIndex) {
                    filteredPoints.add(weekPoints[i])
                    xOffsets.add(i / 6f)
                }
            }

            val maxInChart = if (filteredPoints.isNotEmpty()) filteredPoints.maxOrNull() ?: 0f else 0f
            val chartMax = if (maxInChart > dailyGoal) {
                (maxInChart * 1.2f).coerceAtLeast(100f)
            } else {
                dailyGoal.toFloat()
            }

            Pair(
                label,
                ChartData(
                    points = filteredPoints,
                    xOffsets = xOffsets,
                    xLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                    yLabels = generateYLabels(chartMax),
                    maxValue = chartMax
                )
            )
        }
        "Month" -> {
            val targetMonth = today.plusMonths(offset.toLong())
            val label = if (offset == 0) "This Month" 
                       else targetMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            
            val lastDay = targetMonth.with(TemporalAdjusters.lastDayOfMonth())
            val monthPoints = mutableListOf<Float>()
            val weekRanges = listOf(1..7, 8..14, 15..21, 22..lastDay.dayOfMonth)
            
            weekRanges.forEach { range ->
                val totalForWeek = allLogs.filter { log ->
                    try {
                        val logDate = LocalDate.parse(log.date)
                        logDate.month == targetMonth.month && 
                        logDate.year == targetMonth.year &&
                        logDate.dayOfMonth in range
                    } catch (e: Exception) {
                        false
                    }
                }.sumOf { it.amount }.toFloat()
                monthPoints.add(totalForWeek)
            }

            val limitIndex = if (offset == 0) {
                when (today.dayOfMonth) {
                    in 1..7 -> 0
                    in 8..14 -> 1
                    in 15..21 -> 2
                    else -> 3
                }
            } else 3
            
            val firstActive = monthPoints.indexOfFirst { it > 0 }
            val filteredPoints = mutableListOf<Float>()
            val xOffsets = mutableListOf<Float>()
            if (firstActive != -1 && firstActive <= limitIndex) {
                for (i in firstActive..limitIndex) {
                    filteredPoints.add(monthPoints[i])
                    xOffsets.add(i / 3f)
                }
            }

            val maxInChart = if (filteredPoints.isNotEmpty()) filteredPoints.maxOrNull() ?: 0f else 0f
            val chartMax = if (maxInChart > (dailyGoal * 7)) {
                (maxInChart * 1.2f).coerceAtLeast(100f)
            } else {
                (dailyGoal * 7).toFloat()
            }

            Pair(
                label,
                ChartData(
                    points = filteredPoints,
                    xOffsets = xOffsets,
                    xLabels = listOf("Week 1", "Week 2", "Week 3", "Week 4"),
                    yLabels = generateYLabels(chartMax),
                    maxValue = chartMax
                )
            )
        }
        else -> { // Year
            val targetYear = today.plusYears(offset.toLong())
            val label = if (offset == 0) "This Year" else targetYear.year.toString()
            
            val yearPoints = mutableListOf<Float>()
            for (month in 1..12) {
                val totalForMonth = allLogs.filter { log ->
                    try {
                        val logDate = LocalDate.parse(log.date)
                        logDate.year == targetYear.year && logDate.monthValue == month
                    } catch (e: Exception) {
                        false
                    }
                }.sumOf { it.amount }.toFloat()
                yearPoints.add(totalForMonth)
            }

            val limitIndex = if (offset == 0) today.monthValue - 1 else 11
            val firstActive = yearPoints.indexOfFirst { it > 0 }
            val filteredPoints = mutableListOf<Float>()
            val xOffsets = mutableListOf<Float>()
            if (firstActive != -1 && firstActive <= limitIndex) {
                for (i in firstActive..limitIndex) {
                    filteredPoints.add(yearPoints[i])
                    xOffsets.add(i / 11f)
                }
            }

            val maxInChart = if (filteredPoints.isNotEmpty()) filteredPoints.maxOrNull() ?: 0f else 0f
            val chartMax = if (maxInChart > (dailyGoal * 30)) {
                (maxInChart * 1.2f).coerceAtLeast(100f)
            } else {
                (dailyGoal * 30).toFloat()
            }

            Pair(
                label,
                ChartData(
                    points = filteredPoints,
                    xOffsets = xOffsets,
                    xLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
                    yLabels = generateYLabels(chartMax),
                    maxValue = chartMax
                )
            )
        }
    }
}

fun generateYLabels(maxValue: Float): List<String> {
    val steps = 4
    if (maxValue <= 0f) return listOf("0", "250", "500", "750", "1000")
    val interval = maxValue / steps
    return (0..steps).map { i ->
        val value = i * interval
        when {
            value >= 1000f -> "${String.format("%.1f", value / 1000f)}k"
            else -> value.toInt().toString()
        }
    }
}
