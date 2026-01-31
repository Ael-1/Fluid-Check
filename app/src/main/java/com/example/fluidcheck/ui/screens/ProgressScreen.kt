package com.example.fluidcheck.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fluidcheck.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Composable
fun ProgressScreen() {
    var selectedTab by remember { mutableStateOf("Day") }
    var navOffset by remember { mutableIntStateOf(0) }

    // Reset offset when tab changes (Spec 5.1)
    LaunchedEffect(selectedTab) {
        navOffset = 0
    }

    val (navLabel, chartData) = remember(selectedTab, navOffset) {
        getChartDataForRange(selectedTab, navOffset)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Your Progress",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                color = TextDark,
                letterSpacing = (-0.5).sp
            )
        )

        Text(
            text = "View your hydration trends over time.",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MutedForeground
            ),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Progress Card (Spec 5 Card background is white with shadow)
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
                // 5.1 Time Range Tabs
                TimeRangeTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 5.2 Date Navigation Bar
                DateNavigationBar(
                    label = navLabel,
                    onPrevious = { navOffset-- },
                    onNext = { navOffset++ }
                )

                Spacer(modifier = Modifier.height(40.dp))

                // 5.3 Chart Components
                HydrationLineChart(
                    dataPoints = chartData.points,
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
fun DateNavigationBar(label: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Surface(
        color = AccentBlue.copy(alpha = 0.3f), // Spec 5.2
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
            IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "VIEWING",
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

            IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun HydrationLineChart(
    dataPoints: List<Float>,
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
                    val lines = yLabels.size - 1
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

                // 5.3 Line Chart Visuals
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (dataPoints.isEmpty()) return@Canvas

                    val width = size.width
                    val height = size.height
                    val spacing = width / (dataPoints.size - 1)

                    val points = dataPoints.mapIndexed { index, value ->
                        Offset(
                            x = index * spacing,
                            y = height - (value / maxValue * height)
                        )
                    }

                    // Monotone Curve Implementation
                    val path = Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                                val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                                cubicTo(
                                    controlPoint1.x, controlPoint1.y,
                                    controlPoint2.x, controlPoint2.y,
                                    p2.x, p2.y
                                )
                            }
                        }
                    }

                    drawPath(
                        path = path,
                        color = PrimaryBlue,
                        style = Stroke(width = 3.dp.toPx()) // 3.dp as per spec
                    )

                    // 5.3 Points: 4.dp radius white dots with blue borders
                    points.forEach { point ->
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
                    // Increased threshold from 380 to 450 to accommodate more devices
                    val processedLabel = if (rangeType == "Year" && screenWidth < 450) {
                        label.take(1)
                    } else {
                        label
                    }

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

data class ChartData(
    val points: List<Float>,
    val xLabels: List<String>,
    val yLabels: List<String>,
    val maxValue: Float
)

fun getChartDataForRange(range: String, offset: Int): Pair<String, ChartData> {
    val random = Random(offset + range.hashCode())
    return when (range) {
        "Day" -> {
            val date = LocalDate.now().plusDays(offset.toLong())
            val label = when (offset) {
                0 -> "Today"
                -1 -> "Yesterday"
                else -> date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            }
            // Cumulative growth for day view as seen in image 1
            var current = 0f
            val points = List(7) { 
                current += random.nextInt(100, 600).toFloat()
                current
            }.map { if (it > 2800f) 2800f else it }
            
            Pair(
                label,
                ChartData(
                    points = points,
                    xLabels = listOf("12AM", "4AM", "8AM", "12PM", "4PM", "8PM", "11PM"),
                    yLabels = listOf("0", "700", "1400", "2100", "2800"),
                    maxValue = 2800f
                )
            )
        }
        "Week" -> {
            val label = if (offset == 0) "This Week" else "Last Week"
            val points = List(7) { random.nextInt(1500, 3201).toFloat() }
            Pair(
                label,
                ChartData(
                    points = points,
                    xLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                    yLabels = listOf("0", "800", "1600", "2400", "3200"),
                    maxValue = 3200f
                )
            )
        }
        "Month" -> {
            val label = if (offset == 0) "This Month" else "Last Month"
            val points = List(4) { random.nextInt(5000, 18001).toFloat() }
            Pair(
                label,
                ChartData(
                    points = points,
                    xLabels = listOf("Week 1", "Week 2", "Week 3", "Week 4"),
                    yLabels = listOf("0", "5k", "10k", "15k", "20k"),
                    maxValue = 20000f
                )
            )
        }
        else -> { // Year
            val label = if (offset == 0) "This Year" else (LocalDate.now().year + offset).toString()
            val points = List(12) { random.nextInt(30000, 60001).toFloat() }
            Pair(
                label,
                ChartData(
                    points = points,
                    xLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
                    yLabels = listOf("0", "15k", "30k", "45k", "60k"),
                    maxValue = 60000f
                )
            )
        }
    }
}
