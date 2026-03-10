package com.example.fluidcheck.ui.components.admin
 
import com.example.fluidcheck.ui.theme.*

import com.example.fluidcheck.ui.admin.*
import com.example.fluidcheck.model.*

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.example.fluidcheck.R
import com.example.fluidcheck.model.ChartData
import com.example.fluidcheck.model.FluidLog
import com.example.fluidcheck.model.UserRecord
import com.example.fluidcheck.util.*
import com.example.fluidcheck.repository.AuthRepository
import com.example.fluidcheck.ui.theme.*
import com.example.fluidcheck.ui.screens.TimeRangeTabs
import com.example.fluidcheck.ui.screens.DateNavigationBar
import com.example.fluidcheck.ui.screens.HydrationLineChart
import com.example.fluidcheck.ui.screens.getChartDataForRange
import com.example.fluidcheck.ui.screens.generateYLabels
import com.example.fluidcheck.ui.components.profile.EditField
import com.example.fluidcheck.ui.components.profile.PersonalRecordsContainer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp

private val PST_ZONE = ZoneId.of("GMT+8")

@Composable
fun UserProgressChart(
    allLogs: List<FluidLog>,
    dailyGoal: Int,
    accountCreatedAt: com.google.firebase.Timestamp? = null
) {
    var selectedTab by remember { mutableStateOf("Week") }
    var navOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab) {
        navOffset = 0
    }

    val (navLabel, chartData) = remember(selectedTab, navOffset, allLogs, dailyGoal) {
        getChartDataForRange(selectedTab, navOffset, allLogs, dailyGoal)
    }

    val creationDate = remember(accountCreatedAt) {
        accountCreatedAt?.toDate()?.toInstant()?.atZone(PST_ZONE)?.toLocalDate()
            ?: LocalDate.now(PST_ZONE)
    }

    val canGoNext = navOffset < 0
    val canGoPrevious = remember(selectedTab, navOffset, creationDate) {
        val today = LocalDate.now(PST_ZONE)
        when (selectedTab) {
            "Day" -> {
                val currentTargetDate = today.plusDays(navOffset.toLong())
                currentTargetDate.minusDays(1) >= creationDate
            }
            "Week" -> {
                val currentTargetWeek = today.plusWeeks(navOffset.toLong())
                val currentWeekStart = currentTargetWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                currentWeekStart.minusDays(1) >= creationDate
            }
            "Month" -> {
                val currentTargetMonth = today.plusMonths(navOffset.toLong())
                val currentMonthStart = currentTargetMonth.with(TemporalAdjusters.firstDayOfMonth())
                currentMonthStart.minusDays(1) >= creationDate
            }
            "Year" -> {
                val currentTargetYear = today.plusYears(navOffset.toLong())
                val currentYearStart = currentTargetYear.with(TemporalAdjusters.firstDayOfYear())
                currentYearStart.minusDays(1) >= creationDate
            }
            else -> true
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TimeRangeTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DateNavigationBar(
                label = navLabel,
                onPrevious = { navOffset-- },
                onNext = { navOffset++ },
                isPreviousEnabled = canGoPrevious,
                isNextEnabled = canGoNext
            )

            Spacer(modifier = Modifier.height(24.dp))

            HydrationLineChart(
                dataPoints = chartData.points,
                xOffsets = chartData.xOffsets,
                xLabels = chartData.xLabels,
                yLabels = chartData.yLabels,
                maxValue = chartData.maxValue,
                rangeType = selectedTab
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun InfoSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = MutedForeground,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun ReadOnlyField(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = GhostWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(16.dp),
                color = MutedForeground,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun AnalyticsGrid(
    totalUsers: String,
    moderators: String,
    avgGoal: String,
    totalLogs: String,
    avgStreak: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Row 1: Users | Moderators
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsCard(
                title = stringResource(R.string.total_users),
                value = totalUsers,
                icon = AppIcons.Group,
                iconColor = PrimaryBlue,
                modifier = Modifier.weight(1f)
            )
            AnalyticsCard(
                title = stringResource(R.string.moderators_label),
                value = moderators,
                icon = AppIcons.Security,
                iconColor = WarningOrange,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 2: Avg. Goal | Total Logs (Rings Closed)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsCard(
                title = stringResource(R.string.avg_goal_label),
                value = avgGoal,
                icon = AppIcons.Goal,
                iconColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
            AnalyticsCard(
                title = stringResource(R.string.total_rings_closed_label),
                value = totalLogs,
                icon = AppIcons.History,
                iconColor = PrimaryBlue,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Row 3: Avg. Streak (full width)
        AnalyticsCard(
            title = stringResource(R.string.avg_streak),
            value = avgStreak,
            icon = AppIcons.Progress,
            iconColor = PremiumPurple,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AnalyticsCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MutedForeground,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = TextDark
            )
        }
    }
}
