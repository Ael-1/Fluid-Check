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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.R
import com.example.fluidcheck.model.ChartData
import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.ui.theme.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.firebase.Timestamp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

import java.time.YearMonth
import java.time.temporal.ChronoUnit

private val PST_ZONE = ZoneId.of("GMT+8")

@Composable
fun ProgressScreen(
    userId: String,
    firestoreRepository: com.example.fluidcheck.repository.FirestoreRepository,
    dailyGoal: Int,
    accountCreatedAt: Timestamp? = null,
    allLogs: List<FluidLog>,
    measurementPreferences: com.example.fluidcheck.util.MeasurementPreferences = com.example.fluidcheck.util.MeasurementPreferences(),
    userRole: String = "FREE USER",
    userRecord: com.example.fluidcheck.model.UserRecord? = null,
    isConnected: Boolean,
    onPremiumFeatureClick: () -> Unit = {},
    onAcceptMission: suspend (String) -> Unit = {},
    onAbortMission: suspend (String) -> Unit = {},
    onCompleteMission: suspend (String) -> Unit = {},
    onNavigateToInventory: () -> Unit = {}
) {
    val themeColors = com.example.fluidcheck.ui.theme.LocalFluidCheckColors.current
    var selectedTab by remember { mutableStateOf("Day") }
    var navOffset by remember { mutableIntStateOf(0) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Reset offset when tab changes
    LaunchedEffect(selectedTab) {
        navOffset = 0
    }

    val (navLabel, chartData) = remember(selectedTab, navOffset, allLogs, dailyGoal, measurementPreferences) {
        getChartDataForRange(selectedTab, navOffset, allLogs, dailyGoal, measurementPreferences)
    }

    // Dynamic Left Bound: User's Account Creation Date
    val creationDate = remember(accountCreatedAt) {
        accountCreatedAt?.toDate()?.toInstant()?.atZone(PST_ZONE)?.toLocalDate()
            ?: LocalDate.now(PST_ZONE)
    }

    val today = remember { LocalDate.now(PST_ZONE) }

    val canGoNext = navOffset < 0
    val canGoPrevious = remember(selectedTab, navOffset, creationDate) {
        val todayNow = LocalDate.now(PST_ZONE)
        
        when (selectedTab) {
            "Day" -> {
                val currentTargetDate = todayNow.plusDays(navOffset.toLong())
                currentTargetDate.minusDays(1) >= creationDate
            }
            "Week" -> {
                val currentTargetWeek = todayNow.plusWeeks(navOffset.toLong())
                val currentWeekStart = currentTargetWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                currentWeekStart.minusDays(1) >= creationDate
            }
            "Month" -> {
                val currentTargetMonth = todayNow.plusMonths(navOffset.toLong())
                val currentMonthStart = currentTargetMonth.with(TemporalAdjusters.firstDayOfMonth())
                currentMonthStart.minusDays(1) >= creationDate
            }
            "Year" -> {
                val currentTargetYear = todayNow.plusYears(navOffset.toLong())
                val currentYearStart = currentTargetYear.with(TemporalAdjusters.firstDayOfYear())
                currentYearStart.minusDays(1) >= creationDate
            }
            else -> true
        }
    }

    val selectedLocalDate = remember(selectedTab, navOffset) {
        val todayNow = LocalDate.now(PST_ZONE)
        val calculated = when (selectedTab) {
            "Day" -> todayNow.plusDays(navOffset.toLong())
            "Week" -> todayNow.plusWeeks(navOffset.toLong())
            "Month" -> todayNow.plusMonths(navOffset.toLong())
            "Year" -> todayNow.plusYears(navOffset.toLong())
            else -> todayNow
        }
        // Coerce within bounds
        if (calculated < creationDate) creationDate
        else if (calculated > todayNow) todayNow
        else calculated
    }

    if (showDatePicker) {
        HydrationDatePickerDialog(
            initialDate = selectedLocalDate,
            creationDate = creationDate,
            allLogs = allLogs,
            dailyGoal = dailyGoal,
            userRole = userRole,
            onDateSelected = { pickedDate ->
                val todayNow = LocalDate.now(PST_ZONE)
                navOffset = when (selectedTab) {
                    "Day" -> ChronoUnit.DAYS.between(todayNow, pickedDate).toInt()
                    "Week" -> {
                        val todayMonday = todayNow.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        val pickedMonday = pickedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        ChronoUnit.WEEKS.between(todayMonday, pickedMonday).toInt()
                    }
                    "Month" -> {
                        val todayMonthStart = todayNow.with(TemporalAdjusters.firstDayOfMonth())
                        val pickedMonthStart = pickedDate.with(TemporalAdjusters.firstDayOfMonth())
                        ChronoUnit.MONTHS.between(todayMonthStart, pickedMonthStart).toInt()
                    }
                    "Year" -> {
                        val todayYearStart = todayNow.with(TemporalAdjusters.firstDayOfYear())
                        val pickedYearStart = pickedDate.with(TemporalAdjusters.firstDayOfYear())
                        ChronoUnit.YEARS.between(todayYearStart, pickedYearStart).toInt()
                    }
                    else -> 0
                }
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }

    val weeklyGradeAndFeedback = remember(allLogs, dailyGoal) {
        val todayNow = LocalDate.now(PST_ZONE)
        var metGoalDays = 0
        for (i in 0..6) {
            val date = todayNow.minusDays(i.toLong())
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
            val intake = allLogs.filter { it.date == dateStr }.sumOf { it.amount }
            if (intake >= dailyGoal) {
                metGoalDays++
            }
        }
        
        val grade = when (metGoalDays) {
            7 -> "A+"
            6 -> "A"
            5 -> "B+"
            4 -> "B"
            3 -> "C+"
            2 -> "C"
            1 -> "D"
            else -> "F"
        }
        
        val feedback = when (metGoalDays) {
            7 -> "Outstanding! You met your daily hydration goal every single day this week. Your consistency is perfect, keep up the excellent work!"
            6 -> "Excellent job! You reached your goal 6 out of 7 days. Just one minor slip, but you are maintaining great overall habits."
            5 -> "Great effort! Meeting your goal 5 out of 7 days shows a strong habit. A little more focus on the weekend could secure a perfect score."
            4 -> "Good job! You met your goal 4 out of 7 days. You are on the right track, but consistency can be improved by setting mid-day reminders."
            3 -> "Fair week. You met your goal 3 out of 7 days. Try to keep a water bottle near your desk or set smart notifications to boost your intake."
            2 -> "You reached your hydration goal 2 days this week. Let's aim for 4 days next week! Increasing your morning intake can help set a good pace."
            1 -> "You met your goal only 1 day this week. Consistent hydration improves energy and focus. Try using the AI Coach to adjust your target."
            else -> "No daily goals met this week. Don't worry, every week is a fresh start! Try starting with small frequent sips and use AI recommendations."
        }
        
        Pair(grade, feedback)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        @Suppress("DEPRECATION")
        Spacer(modifier = Modifier.height(32.dp))

        @Suppress("DEPRECATION")
        Text(
            text = stringResource(R.string.your_progress),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
        )

        @Suppress("DEPRECATION")
        Text(
            text = stringResource(R.string.progress_subtitle),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
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
            color = themeColors.cardBackground,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.cardBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Time Range Tabs
                TimeRangeTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { tab -> 
                        if (userRole == "FREE USER" && tab != "Day") {
                            onPremiumFeatureClick()
                        } else {
                            selectedTab = tab
                        }
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Date Navigation Bar
                DateNavigationBar(
                    label = navLabel,
                    onPrevious = { 
                        if (userRole == "FREE USER" && selectedTab == "Day" && navOffset <= -6) {
                            onPremiumFeatureClick()
                        } else {
                            navOffset-- 
                        }
                    },
                    onNext = { navOffset++ },
                    isPreviousEnabled = canGoPrevious,
                    isNextEnabled = canGoNext,
                    onLabelClick = { showDatePicker = true }
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

        Spacer(modifier = Modifier.height(24.dp))

        // AI Weekly Scorecard Card
        val (grade, feedback) = weeklyGradeAndFeedback
        val gradeBgColor = when {
            grade.startsWith("A") -> Color(0xFFE2FBE9)
            grade.startsWith("B") -> Color(0xFFFFF4E5)
            grade.startsWith("C") -> Color(0xFFFFFBE6)
            grade.startsWith("D") -> Color(0xFFFFF2F2)
            else -> androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
        }
        val gradeTextColor = when {
            grade.startsWith("A") -> Color(0xFF1B803A)
            grade.startsWith("B") -> Color(0xFFB76E00)
            grade.startsWith("C") -> Color(0xFFD97706)
            grade.startsWith("D") -> Color(0xFFEF4444)
            else -> Color(0xFF64748B)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable { if (userRole == "FREE USER") onPremiumFeatureClick() },
            shape = RoundedCornerShape(32.dp),
            color = if (userRole == "FREE USER") themeColors.cardBackground.copy(alpha = 0.5f) else themeColors.cardBackground,
            shadowElevation = if (userRole == "FREE USER") 2.dp else 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, themeColors.cardBorder)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.AICoach,
                            contentDescription = null,
                            tint = if (userRole == "FREE USER") Color.LightGray else androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Weekly Scorecard",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (userRole == "FREE USER") Color.Gray else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    if (userRole != "FREE USER") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(gradeBgColor)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = grade,
                                color = gradeTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Icon(
                            imageVector = AppIcons.Premium,
                            contentDescription = "Premium",
                            tint = com.example.fluidcheck.ui.theme.Gold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (userRole != "FREE USER") {
                    Text(
                        text = feedback,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    )
                } else {
                    Text(
                        text = "Unlock Premium to get detailed AI analysis on your weekly hydration habits, personalized tips, and a weekly grade.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 22.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // GAMIFICATION: Mission Board Section
        if (userRole != "FREE USER" && userRecord != null) {
            com.example.fluidcheck.ui.components.MissionBoardSection(
                userRole = userRole,
                boardMissionIds = userRecord.boardMissionIds,
                activeMissions = userRecord.activeMissions,
                completedMissionsToday = userRecord.completedMissionsToday,
                abortedMissionsToday = userRecord.abortedMissionsToday,
                isConnected = isConnected,
                onAcceptMission = onAcceptMission,
                onAbortMission = onAbortMission,
                onCompleteMission = onCompleteMission,
                onNavigateToInventory = onNavigateToInventory
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TimeRangeTabs(selectedTab: String, onTabSelected: (String) -> Unit) {
    val tabs = listOf("Day", "Week", "Month", "Year")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
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
                        .background(if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.surface else Color.Transparent)
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
                        color = if (isSelected) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else MutedForeground
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
    isNextEnabled: Boolean,
    onLabelClick: () -> Unit
) {
    Surface(
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
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
                    tint = if (isPreviousEnabled) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else Color(0xFF94A3B8).copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onLabelClick() }
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
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
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
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
                    tint = if (isNextEnabled) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else Color(0xFF94A3B8).copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun HydrationDatePickerDialog(
    initialDate: LocalDate,
    creationDate: LocalDate,
    allLogs: List<FluidLog>,
    dailyGoal: Int,
    userRole: String,
    onDateSelected: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    val today = remember { LocalDate.now(PST_ZONE) }
    val completedGoalColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = { onDateSelected(selectedDate) }
            ) {
                Text(stringResource(R.string.confirm), color = androidx.compose.material3.MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel), color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        title = {
            Text(
                text = "Select Date",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Month Selector Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val prevEnabled = currentMonth.minusMonths(1).atEndOfMonth().isAfter(creationDate.minusDays(1))
                    val nextEnabled = currentMonth.plusMonths(1).atDay(1).isBefore(today.plusDays(1))

                    IconButton(
                        onClick = { currentMonth = currentMonth.minusMonths(1) },
                        enabled = prevEnabled
                    ) {
                        Icon(
                            imageVector = AppIcons.ArrowLeft,
                            contentDescription = "Previous Month",
                            tint = if (prevEnabled) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else Color(0xFF94A3B8).copy(alpha = 0.3f)
                        )
                    }

                    Text(
                        text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = { currentMonth = currentMonth.plusMonths(1) },
                        enabled = nextEnabled
                    ) {
                        Icon(
                            imageVector = AppIcons.ArrowRight,
                            contentDescription = "Next Month",
                            tint = if (nextEnabled) androidx.compose.material3.MaterialTheme.colorScheme.onSurface else Color(0xFF94A3B8).copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Days of the Week Header
                Row(modifier = Modifier.fillMaxWidth()) {
                    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar Grid
                val firstOfMonth = currentMonth.atDay(1)
                val dayOfWeekOffset = firstOfMonth.dayOfWeek.value - 1 // 0 for Monday, 6 for Sunday
                val daysInMonth = currentMonth.lengthOfMonth()
                
                val totalCells = daysInMonth + dayOfWeekOffset
                val rows = (totalCells + 6) / 7

                Column {
                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - dayOfWeekOffset + 1
                                
                                if (dayNum in 1..daysInMonth) {
                                    val cellDate = currentMonth.atDay(dayNum)
                                    val maxPastDate = if (userRole == "FREE USER") today.minusDays(7) else creationDate
                                    val isSelectable = cellDate >= maxPastDate && cellDate <= today
                                    val isSelected = cellDate == selectedDate
                                    
                                    // Calculate progress percentage
                                    val dateStr = cellDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                                    val dayLogs = allLogs.filter { it.date == dateStr }
                                    val totalIntake = dayLogs.sumOf { it.amount }.toFloat()
                                    val progressPct = if (dailyGoal > 0) (totalIntake / dailyGoal).coerceIn(0f, 1f) else 0f

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(enabled = isSelectable) {
                                                selectedDate = cellDate
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelectable) {
                                            // Progress background representation (pie chart style)
                                            val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                            val bgColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                                            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                                                val size = this.size
                                                val radius = size.minDimension / 2
                                                
                                                // Background soft progress circle (using androidx.compose.material3.MaterialTheme.colorScheme.background color)
                                                drawCircle(
                                                    color = bgColor,
                                                    radius = radius
                                                )
                                                
                                                // Filled progress sector (pie slice)
                                                if (progressPct > 0f) {
                                                    val sweepAngle = progressPct * 360f
                                                    val arcColor = completedGoalColor
                                                    drawArc(
                                                        color = arcColor,
                                                        startAngle = -90f,
                                                        sweepAngle = sweepAngle,
                                                        useCenter = true
                                                    )
                                                }

                                                // Clean border ring highlight for the selected date
                                                if (isSelected) {
                                                    drawCircle(
                                                        color = primaryColor,
                                                        radius = radius - 1.dp.toPx(),
                                                        style = Stroke(width = 2.5f.dp.toPx())
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = dayNum.toString(),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            color = when {
                                                !isSelectable -> MutedForeground.copy(alpha = 0.3f)
                                                isSelected -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                                                else -> androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    )
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
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Chart Area
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Grid Lines
                val gridColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val height = size.height
                    val lines = if (yLabels.size > 1) yLabels.size - 1 else 1
                    for (i in 0..lines) {
                        val y = height - (i * (height / lines))
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                // Line Chart Visuals
                val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
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
                            color = primaryColor,
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
                            color = primaryColor,
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
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
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
    dailyGoal: Int,
    measurementPreferences: com.example.fluidcheck.util.MeasurementPreferences
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
            
            val targetDateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
            val dayLogs = allLogs.filter { it.date == targetDateStr }
            
            val sortedLogs = dayLogs.map { log ->
                val time = try {
                    val cleanTime = log.time.trim().uppercase(Locale.US)
                    val formats = listOf(
                        DateTimeFormatter.ofPattern("h:mm a", Locale.US),
                        DateTimeFormatter.ofPattern("hh:mm a", Locale.US),
                        DateTimeFormatter.ofPattern("H:mm", Locale.US),
                        DateTimeFormatter.ofPattern("HH:mm", Locale.US),
                        DateTimeFormatter.ofPattern("h:mm:ss a", Locale.US),
                        DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.US)
                    )
                    var parsedTime: LocalTime? = null
                    for (formatter in formats) {
                        try {
                            parsedTime = LocalTime.parse(cleanTime, formatter)
                            break
                        } catch (e: Exception) {
                            // continue
                        }
                    }
                    parsedTime ?: run {
                        val parts = cleanTime.split(":")
                        if (parts.isNotEmpty()) {
                            val hour = parts[0].filter { it.isDigit() }.toIntOrNull() ?: 12
                            val minute = if (parts.size > 1) parts[1].filter { it.isDigit() }.toIntOrNull() ?: 0 else 0
                            val isPm = cleanTime.contains("PM")
                            val adjustedHour = when {
                                isPm && hour < 12 -> hour + 12
                                !isPm && hour == 12 -> 0
                                else -> hour
                            }
                            LocalTime.of(adjustedHour.coerceIn(0, 23), minute.coerceIn(0, 59))
                        } else {
                            LocalTime.of(12, 0)
                        }
                    }
                } catch (e: Exception) {
                    LocalTime.of(12, 0)
                }
                log to time
            }.sortedBy { it.second }

            // Cumulative Sum Logic
            var runningTotal = 0f
            val points = mutableListOf<Float>()
            val xOffsets = mutableListOf<Float>()
            
            // Start from midnight with 0 volume
            points.add(0f)
            xOffsets.add(0f)

            sortedLogs.forEach { (log, time) ->
                runningTotal += log.amount
                points.add(runningTotal)
                val offsetInDay = time.toSecondOfDay().toFloat() / (24 * 60 * 60)
                xOffsets.add(offsetInDay)
            }
            
            // If it's today and we have logs, extend the line to current time? 
            // Or just leave it at the last log. Let's extend to "now" if it's today to show current progress.
            if (offset == 0 && sortedLogs.isNotEmpty()) {
                val nowTime = LocalTime.now(PST_ZONE)
                val nowOffset = nowTime.toSecondOfDay().toFloat() / (24 * 60 * 60)
                if (nowOffset > (xOffsets.lastOrNull() ?: 0f)) {
                    points.add(runningTotal)
                    xOffsets.add(nowOffset)
                }
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
                    yLabels = generateYLabels(chartMax, measurementPreferences),
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
                val dayStr = currentDay.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                val total = allLogs.filter { it.date == dayStr }.sumOf { it.amount }.toFloat()
                weekPoints.add(total)
            }

            val limitIndex = if (offset == 0) today.dayOfWeek.value - 1 else 6
            
            val filteredPoints = mutableListOf<Float>()
            val xOffsets = mutableListOf<Float>()
            // Always show from Monday to today/Sunday
            for (i in 0..limitIndex) {
                filteredPoints.add(weekPoints[i])
                xOffsets.add(i / 6f)
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
                    yLabels = generateYLabels(chartMax, measurementPreferences),
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
            
            val filteredPoints = mutableListOf<Float>()
            val xOffsets = mutableListOf<Float>()
            // Show from Week 1 to current week
            for (i in 0..limitIndex) {
                filteredPoints.add(monthPoints[i])
                xOffsets.add(i / 3f)
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
                    yLabels = generateYLabels(chartMax, measurementPreferences),
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
            val filteredPoints = mutableListOf<Float>()
            val xOffsets = mutableListOf<Float>()
            // Show from January to current month
            for (i in 0..limitIndex) {
                filteredPoints.add(yearPoints[i])
                xOffsets.add(i / 11f)
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
                    yLabels = generateYLabels(chartMax, measurementPreferences),
                    maxValue = chartMax
                )
            )
        }
    }
}

fun generateYLabels(maxValue: Float, measurementPreferences: com.example.fluidcheck.util.MeasurementPreferences): List<String> {
    val steps = 4
    if (maxValue <= 0f) {
        return (0..steps).map { i -> 
            val v = i * (1000f / steps)
            com.example.fluidcheck.util.MeasurementUtils.formatChartLabel(v.toInt(), measurementPreferences.volume)
        }
    }
    val interval = maxValue / steps
    return (0..steps).map { i ->
        val value = i * interval
        com.example.fluidcheck.util.MeasurementUtils.formatChartLabel(value.toInt(), measurementPreferences.volume)
    }
}
